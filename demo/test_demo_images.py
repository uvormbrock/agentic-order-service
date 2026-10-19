"""Offline seeding contracts: no database, application or network calls."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import uuid

spec = importlib.util.spec_from_file_location("demo_helper", Path(__file__).with_name("demo.py"))
demo = importlib.util.module_from_spec(spec)
spec.loader.exec_module(demo)


class DemoImagesTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.state = self.root / "state"
        self.state.mkdir()
        self.assets = self.root / "demo/assets/products"
        self.assets.mkdir(parents=True)
        self.products = [{"id": str(uuid.uuid4()), "name": name, "active": index != 7}
                         for index, (name, _) in enumerate(demo.IMAGE_ASSETS)]
        (self.state / "seed.json").write_text(json.dumps({"products": self.products}))
        for _, filename in demo.IMAGE_ASSETS:
            (self.assets / filename).write_bytes(b"offline-fixture")
        for name, value in (("ROOT", self.root), ("STATE", self.state)):
            mock = patch.object(demo, name, value)
            mock.start()
            self.addCleanup(mock.stop)
        for name in ("database", "app"):
            mock = patch.object(demo, name)
            mock.start()
            self.addCleanup(mock.stop)
        self.get = patch.object(demo, "request", side_effect=self.lookup).start()
        self.addCleanup(patch.stopall)
        self.upload = patch.object(demo, "upload_image").start()

    def lookup(self, method, path):
        self.assertEqual(method, "GET")
        return next(p for p in self.products if path == "/products/" + p["id"])

    def test_repeat_uses_recorded_ids_without_recreating_or_reactivating_products(self):
        with contextlib.redirect_stdout(io.StringIO()):
            demo.seed_images({})
            demo.seed_images({})
        self.assertEqual(self.upload.call_count, 16)
        first = self.upload.call_args_list[:8]
        self.assertEqual(first, self.upload.call_args_list[8:])
        self.assertEqual([call.args[0] for call in first], [p["id"] for p in self.products])
        self.assertEqual([call.args[1].name for call in first], [f for _, f in demo.IMAGE_ASSETS])
        self.assertIs(self.products[7]["active"], False)
        self.assertEqual(self.get.call_count, 16)

    def test_missing_asset_prevents_all_uploads(self):
        (self.assets / "legacy-item.jpg").unlink()
        with self.assertRaisesRegex(RuntimeError, "Required demo image missing"):
            demo.seed_images({})
        self.upload.assert_not_called()

    def test_product_name_mismatch_prevents_all_uploads(self):
        self.products[-1]["name"] = "Other product"
        with self.assertRaisesRegex(RuntimeError, "mismatch"):
            demo.seed_images({})
        self.upload.assert_not_called()

    def test_active_legacy_is_not_silently_modified(self):
        self.products[-1]["active"] = True
        with self.assertRaisesRegex(RuntimeError, "must remain inactive"):
            demo.seed_images({})
        self.upload.assert_not_called()

    def test_duplicate_ids_prevent_all_uploads(self):
        self.products[-1]["id"] = self.products[0]["id"]
        (self.state / "seed.json").write_text(json.dumps({"products": self.products}))
        with self.assertRaisesRegex(RuntimeError, "Duplicate"):
            demo.seed_images({})
        self.upload.assert_not_called()

    def test_upload_contains_actual_bytes_and_multipart_field(self):
        # Use the function wrapped by the mock so this test inspects the HTTP boundary.
        source = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(source)
        product_id = self.products[0]["id"]
        asset = self.assets / "keyboard.jpg"
        payload = b"\xff\xd8real-image-bytes\xff\xd9"
        asset.write_bytes(payload)
        response = unittest.mock.MagicMock()
        response.__enter__.return_value = response
        response.status = 204
        response.headers = {"X-Request-ID": "offline-request"}
        with patch.object(source.urllib.request, "urlopen", return_value=response) as call:
            source.upload_image(product_id, asset)
        request = call.call_args.args[0]
        self.assertEqual(request.method, "PUT")
        self.assertTrue(request.full_url.endswith("/products/" + product_id + "/image"))
        self.assertIn(b'name="file"', request.data)
        self.assertIn(b'filename="keyboard.jpg"', request.data)
        self.assertIn(payload, request.data)


if __name__ == "__main__":
    unittest.main()

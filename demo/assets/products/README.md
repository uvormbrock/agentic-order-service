# Demo Product Images

Generated with the built-in ImageGen tool on 2026-10-18. These are synthetic demo catalog illustrations, not photographs of specific inventory. The CH-002 demo seeder uploads the derived JPEGs through the product image API. PNG generation sources remain unchanged. The inactive legacy product also has an image.

The IDs below are historical example IDs, not stable seed identities. The seeder uses actual IDs in `target/local-demo/seed.json`.

| Historical example ID | Product | PNG source |
| --- | --- | --- |
| `c05615ce-5c15-488b-a942-e9cd971c6330` | Demo Mouse | [demo-mouse.png](demo-mouse.png) |
| `fbd4d4a1-b462-4c39-8d69-77f6c2c3d556` | Demo Notebook | [demo-notebook.png](demo-notebook.png) |
| `8082e4f0-491f-4dc1-a676-a2cce9c2397f` | Demo Monitor | [demo-monitor.png](demo-monitor.png) |
| `7bd4e512-8c52-4d44-92e4-064bf32e6e28` | Demo Sticker | [demo-sticker.png](demo-sticker.png) |
| `2ddfe1ef-e500-4af3-8297-cb05917eb0be` | Demo Desk | [demo-desk.png](demo-desk.png) |
| `1503b0c7-4c97-4e04-8028-db9863fb41e8` | Demo Pen | [demo-pen.png](demo-pen.png) |
| `2338fcd8-2204-437c-b20d-85caee19d34f` | Demo Keyboard v2 | [demo-keyboard-v2.png](demo-keyboard-v2.png) |
| `dfc2e04b-0480-40e8-8207-6d839942418f` | Demo Legacy Item | [demo-legacy-item.png](demo-legacy-item.png) |

## Generation prompts

Each subject below used this shared prompt, substituting the subject verbatim:

> Use case: product-mockup. Asset type: square e-commerce demo product image, PNG. Subject: {subject}. Style: photorealistic premium catalog product photography, physically credible materials and proportions. Backdrop: seamless very light neutral gray studio background with soft natural contact shadow. Composition: single isolated product centered, entire object visible, generous consistent margins, square frame. Lighting: soft diffused studio illumination, crisp detail, restrained neutral colors. Constraints: no people, no price tags, no captions, no watermark, no extra unrelated props. This is one image in a coherent eight-product computer accessories catalog.

- **Demo Mouse:** a modern ergonomic wireless computer mouse, graphite black, scroll wheel and two subtle side buttons
- **Demo Notebook:** an open silver MacBook Pro laptop computer, unmistakably a laptop with black keyboard, large trackpad, thin aluminum body, screen with a tasteful abstract blue desktop wallpaper; three-quarter front view; not a paper notebook
- **Demo Monitor:** a modern slim 27-inch computer monitor on a silver adjustable stand, screen displaying an abstract blue desktop wallpaper
- **Demo Sticker:** one premium die-cut laptop sticker with a playful stylized computer terminal icon and cyan angle brackets, lying nearly flat at a slight angle, visible white sticker border
- **Demo Desk:** a minimalist computer workstation desk, oak desktop, dark metal legs, discreet rear cable management tray, entire desk visible, no chair or devices
- **Demo Pen:** a graphite digital stylus pen for a graphics tablet, subtle side button and fine nib, diagonally positioned, unmistakably a computer input accessory
- **Demo Keyboard v2:** a modern compact wireless mechanical computer keyboard, graphite keys, silver aluminum case, subtle cyan accent escape key, three-quarter view
- **Demo Legacy Item:** a vintage beige wired computer mouse with two buttons and a rubber scroll wheel, coiled cable with old PS/2 connector, a clean well-kept retro computing accessory

## JPEG seed assets

Human-approved JPEG conversion uses macOS `sips` at quality 90, retaining 1254×1254
resolution. Originals uploaded to the backend are these JPEG bytes; no runtime
conversion of the uploaded original is permitted.

| Product | JPEG uploaded by demo seeder |
| --- | --- |
| Demo Mouse | [mouse.jpg](mouse.jpg) |
| Demo Notebook (laptop) | [notebook.jpg](notebook.jpg) |
| Demo Monitor | [monitor.jpg](monitor.jpg) |
| Demo Sticker | [sticker.jpg](sticker.jpg) |
| Demo Desk | [desk.jpg](desk.jpg) |
| Demo Pen (digital stylus) | [pen.jpg](pen.jpg) |
| Demo Keyboard v2 | [keyboard.jpg](keyboard.jpg) |
| Demo Legacy Item (inactive) | [legacy-item.jpg](legacy-item.jpg) |

Initial `make demo-seed` loads catalog data and images. Repeat only image replacement
with `python3 demo/demo.py seed-images`; it preflights all eight recorded product
identities, names and files and refuses a missing/mismatched seed. It never creates
products or changes activity. No automatic replay after an uncertain upload outcome.
Runtime storage is independently configured and Git-ignored, never this asset folder.

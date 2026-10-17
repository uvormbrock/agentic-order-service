#!/usr/bin/env python3
"""Isolated local demo commands; Python standard library only."""
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parent.parent
ENV_FILE = ROOT / "demo/.env.demo"
STATE = ROOT / "target/local-demo"
OWNER = hashlib.sha256(str(ROOT).encode()).hexdigest()
PROJECT = "agentic-order-service-demo"
BASE = "http://127.0.0.1:18080"
# Intentionally public credentials, exclusively for the isolated disposable demo.
DEMO_PASSWORD = "demo_only_local"
# Explicit environment prevents inherited Spring/Compose overrides redirecting the demo.
ENV = {k: v for k, v in os.environ.items() if k in (
    "HOME", "PATH", "JAVA_HOME", "TMPDIR", "LANG", "DOCKER_HOST", "DOCKER_CONTEXT",
    "DOCKER_CONFIG", "XDG_RUNTIME_DIR")}
COMPOSE = ["docker", "compose", "--project-name", PROJECT, "--env-file", str(ENV_FILE),
           "--env-file", str(ROOT / "config/postgres.properties"), "-f", str(ROOT / "demo/compose.yml")]


def run(args, capture=False, **kwargs):
    return subprocess.run(args, cwd=ROOT, env=ENV, check=True,
                          text=True, stdout=subprocess.PIPE if capture else None, **kwargs)


def java():
    home = ENV.get("JAVA_HOME")
    if not home:
        raise RuntimeError("Set JAVA_HOME to a Java 21 installation.")
    executable = str(Path(home) / "bin/java")
    info = run([executable, "-XshowSettings:properties", "-version"], True,
               stderr=subprocess.STDOUT).stdout
    if not re.search(r"java.specification.version = 21\s", info):
        raise RuntimeError("Java 21 is required.")
    return executable


def configuration(require_fixed=False):
    if not ENV_FILE.is_file() or ENV_FILE.is_symlink():
        raise RuntimeError("Missing demo credentials. Run make demo-setup.")
    values = dict(line.split("=", 1) for line in ENV_FILE.read_text().splitlines())
    password = values.get("DEMO_PASSWORD", "")
    legacy = bool(re.fullmatch(r"[0-9a-f]{48}", password))
    if values.get("DEMO_OWNER") != OWNER or not (password == DEMO_PASSWORD or legacy):
        raise RuntimeError("Unexpected demo environment; refusing to adopt or replace it.")
    if require_fixed and legacy:
        raise RuntimeError("Legacy demo credentials preserved. Follow the explicit credential-transition steps "
                           "in docs/06-operations/local-demo.md; no reset or credential replacement was performed.")
    return json.loads(run(COMPOSE + ["config", "--format", "json"], True).stdout)


def containers(config):
    """Validate every existing project container before any lifecycle action."""
    ids = run(["docker", "ps", "-aq", "--filter", "label=com.docker.compose.project=" + PROJECT], True).stdout.split()
    if len(ids) > 1:
        raise RuntimeError("Unexpected containers in demo project; no action taken.")
    for ident in ids:
        item = json.loads(run(["docker", "inspect", ident], True).stdout)[0]
        labels = item["Config"].get("Labels", {})
        ports = item["HostConfig"].get("PortBindings", {}).get("5432/tcp", [])
        mounts = item.get("Mounts", [])
        environment = dict(value.split("=", 1) for value in item["Config"].get("Env", []))
        expected = config["services"]["postgres"]["environment"]
        if (labels.get("org.agentic-order-service.demo-owner") != OWNER
                or labels.get("com.docker.compose.service") != "postgres"
                or item["Config"]["Image"] != config["services"]["postgres"]["image"]
                or any(environment.get(key) != expected[key]
                       for key in ("POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD"))
                or ports != [{"HostIp": "127.0.0.1", "HostPort": "15432"}]
                or any(m.get("Type") in ("bind", "volume") for m in mounts)
                or "/var/lib/postgresql/data" not in item["HostConfig"].get("Tmpfs", {})):
            raise RuntimeError("Demo ownership, image, port or temporary-storage check failed; no action taken.")
    return ids


def database(config):
    if not containers(config):
        raise RuntimeError("Demo database is absent. Run make demo-setup.")
    identity = sql("SELECT current_database() || ':' || current_user")
    if identity != "agentic_order_demo:demo":
        raise RuntimeError("Unexpected database identity; refusing operation.")


def sql(statement):
    return run(COMPOSE + ["exec", "-T", "postgres", "psql", "-X", "-U", "demo", "-d",
                          "agentic_order_demo", "-v", "ON_ERROR_STOP=1", "-Atc", statement], True).stdout.strip()


def app():
    path = STATE / "app.json"
    if not path.exists():
        raise RuntimeError("No demo-owned application is recorded. Start make demo-run in another terminal.")
    state = json.loads(path.read_text())
    pid = state["pid"]
    command = subprocess.run(["ps", "-p", str(pid), "-o", "command="], text=True,
                             stdout=subprocess.PIPE, check=False).stdout
    if state.get("owner") != OWNER or "-Dagentic.demo.owner=" + OWNER not in command:
        raise RuntimeError("Recorded demo application is absent or belongs to another process; no signal sent.")
    return pid


def available(port):
    with socket.socket() as sock:
        try:
            sock.bind(("127.0.0.1", port))
        except OSError as error:
            raise RuntimeError(f"Port {port} is already in use; no existing service will be replaced.") from error


def request(method, path, body=None, expected=200):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method,
                                 headers={"Content-Type": "application/json", "Accept": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            if response.status != expected:
                raise RuntimeError(f"Unexpected HTTP status for {method} {path}: {response.status}")
            if not response.headers.get("X-Request-ID"):
                raise RuntimeError("Missing X-Request-ID response header.")
            return json.loads(response.read()) if expected != 204 else None
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"HTTP {error.code} for {method} {path}; seeding stopped without replay.") from error


def setup():
    java()
    run(["docker", "info"], True)
    if not ENV_FILE.exists():
        # Exclusive creation never replaces existing credentials.
        fd = os.open(ENV_FILE, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as stream:
            stream.write(f"DEMO_OWNER={OWNER}\nDEMO_PASSWORD={DEMO_PASSWORD}\n")
    config = configuration(require_fixed=True)
    existing = containers(config)
    if not existing:
        available(15432)
    run(COMPOSE + ["up", "-d", "--wait", "--wait-timeout", "90", "--no-recreate"])
    database(config)
    print("Demo PostgreSQL ready on 127.0.0.1:15432; schema is initialized by demo-run.")


def start(config):
    executable = java()
    database(config)
    if (STATE / "app.json").exists():
        # A stale record also requires explicit inspection, never silent PID reuse.
        raise RuntimeError("Demo application record already exists; inspect demo-status and stop the previous run.")
    available(18080)
    run([str(ROOT / "mvnw"), "-B", "-ntp", "-DskipTests", "package"])
    STATE.mkdir(parents=True, exist_ok=True)
    values = dict(line.split("=", 1) for line in ENV_FILE.read_text().splitlines())
    env = dict(ENV, SPRING_DATASOURCE_PASSWORD=values["DEMO_PASSWORD"])
    command = [executable, "-Dagentic.demo.owner=" + OWNER, "-jar",
               str(ROOT / "target/agentic-order-service-0.0.1-SNAPSHOT.jar"),
               "--server.address=127.0.0.1", "--server.port=18080",
               "--spring.datasource.url=jdbc:postgresql://127.0.0.1:15432/agentic_order_demo",
               "--spring.datasource.username=demo", "--spring.flyway.enabled=true",
               "--spring.flyway.validate-on-migrate=true", "--spring.flyway.baseline-on-migrate=false",
               "--spring.flyway.clean-disabled=true"]
    process = subprocess.Popen(command, cwd=ROOT, env=env, start_new_session=True)
    state = STATE / "app.json"
    state.write_text(json.dumps({"pid": process.pid, "owner": OWNER}))
    try:
        print("Demo application starting at " + BASE + "; Ctrl-C stops only this JVM.", flush=True)
        return_code = process.wait()
        if return_code not in (0, -signal.SIGTERM, 128 + signal.SIGTERM):
            raise RuntimeError(f"Demo JVM exited with status {return_code}.")
    except KeyboardInterrupt:
        process.terminate()
        process.wait(timeout=30)
    finally:
        if process.poll() is not None:
            state.unlink(missing_ok=True)


def seed(config):
    database(config)
    app()
    if any(request("GET", "/" + resource) for resource in ("customers", "products", "orders")):
        raise RuntimeError("Demo database is not empty. Seed refused; existing data was not changed.")
    dataset = json.loads((ROOT / "demo/data/scenarios.json").read_text())
    customers = [request("POST", "/customers", row, 201) for row in dataset["customers"]]
    products = [request("POST", "/products", row, 201) for row in dataset["products"]]
    orders = []
    for scenario in dataset["orders"]:
        body = {"customerId": customers[scenario["customer"]]["id"], "items": [
            {"productId": products[index]["id"], "quantity": quantity} for index, quantity in scenario["items"]]}
        order = request("POST", "/orders", body, 201)
        actual = [order[key] for key in ("subtotal", "discount", "total")]
        if actual != scenario["expected"]:
            raise RuntimeError("Unexpected order calculation; stopped without replay.")
        orders.append(order)
    request("PUT", "/products/" + products[0]["id"], {"name": "Demo Keyboard v2", "price": 60})
    request("PATCH", "/products/" + products[7]["id"], {"active": False})
    request("PATCH", "/customers/" + customers[4]["id"], {"active": False})
    request("DELETE", "/customers/" + customers[3]["id"], expected=204)
    snapshot = request("GET", "/orders/" + orders[0]["id"])
    keyboard = next(item for item in snapshot["items"] if item["productId"] == products[0]["id"])
    if keyboard["productName"] != "Demo Keyboard" or keyboard["unitPrice"] != 50:
        raise RuntimeError("Historical product snapshot check failed.")
    counts = sql("SELECT (SELECT count(*) FROM customers) || ',' || (SELECT count(*) FROM products) || ',' || "
                 "(SELECT count(*) FROM orders) || ',' || (SELECT count(*) FROM order_items)")
    if counts != "5,8,4,8":
        raise RuntimeError("Unexpected demo record counts.")
    STATE.mkdir(parents=True, exist_ok=True)
    (STATE / "seed.json").write_text(json.dumps({"customers": customers, "products": products, "orders": orders}, indent=2) + "\n")
    print("Seed verified: 5 customers, 8 products, 4 orders, 8 order items. IDs: target/local-demo/seed.json")


def down(config):
    containers(config)
    state = STATE / "app.json"
    if state.exists():
        pid = app()
        os.kill(pid, signal.SIGTERM)
        for _ in range(30):
            time.sleep(1)
            if not state.exists():
                break
        else:
            raise RuntimeError("Application shutdown not confirmed; database left running.")
    run(COMPOSE + ["down"])
    print("Demo stopped. Temporary database data is discarded; development volumes were not touched.")


def main():
    action = sys.argv[1] if len(sys.argv) == 2 else "help"
    if action == "setup":
        setup()
        return
    config = configuration(require_fixed=action in ("run", "seed"))
    if action == "run":
        start(config)
    elif action == "seed":
        seed(config)
    elif action == "status":
        containers(config)
        run(COMPOSE + ["ps"])
        print("Application: " + (str(app()) if (STATE / "app.json").exists() else "not running"))
        if containers(config):
            database(config)
            print("Public tables: " + sql("SELECT string_agg(tablename, ', ' ORDER BY tablename) FROM pg_tables WHERE schemaname='public'"))
    elif action == "db":
        database(config)
        run(COMPOSE + ["exec", "postgres", "psql", "-X", "-U", "demo", "-d", "agentic_order_demo"])
    elif action == "down":
        down(config)
    else:
        raise RuntimeError("Use setup, run, seed, status, db or down.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError, ValueError) as error:
        # Avoid rendering command/env details, which may include resolved credentials.
        print("Demo command failed: " + (str(error) if isinstance(error, RuntimeError) else type(error).__name__), file=sys.stderr)
        sys.exit(1)

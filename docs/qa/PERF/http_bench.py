"""Closed-loop SSR/quote benchmark against the private PERF fixture."""
import concurrent.futures
import http.cookiejar
import json
import math
import os
import re
import statistics
import subprocess
import time
import urllib.parse
import urllib.request
from pathlib import Path

out = Path(os.environ["PERF_OUT"])
site = os.environ["PERF_SITE"]
assert site == "http://127.0.0.1:18516" and os.environ["PERF_DB_PORT"] == "33516"


def sql(statement):
    cmd = [os.environ["PERF_DB_CLI"], "--skip-ssl", "--protocol=tcp", "-h127.0.0.1",
           "-P33516", "-uroot", "--batch", "--raw", "--skip-column-names", "perf_test", "-e", statement]
    return subprocess.check_output(cmd, encoding="utf-8").strip()


def save(name, data):
    (out / name).write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")


class Client:
    def __init__(self, account):
        self.http = urllib.request.build_opener(urllib.request.ProxyHandler({}),
                        urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.account = account
        self.token = None
        if account:
            page = self.read("/login")[1]
            self.token = self.csrf(page)
            self.read("/login", {"_csrf": self.token, "username": f"perf{account}@example.invalid",
                                 "password": os.environ["CART01_FIXTURE_PASS"]})
            route = "/admin/catalog/products" if account > 6 else "/account/addresses"
            path, page = self.read(route)
            assert path == route, "Fixture login required"
            self.token = self.csrf(page)

    @staticmethod
    def csrf(page):
        return re.search(rb'name="_csrf"[^>]*value="([^"]+)"', page)[1].decode()

    def read(self, path, form=None):
        data = urllib.parse.urlencode(form).encode() if form else None
        with self.http.open(site + path, data, timeout=30) as response:
            return urllib.parse.urlsplit(response.url).path, response.read()

    def sample(self, path, marker):
        form = {"addressId": self.account, "_csrf": self.token} if path == "/checkout/quote" else None
        start = time.perf_counter()
        try:
            actual, page = self.read(path, form)
            ok = actual == path.split("?")[0] and marker in page
            return round((time.perf_counter()-start)*1000, 3), len(page), ok
        except (OSError, TimeoutError):
            return round((time.perf_counter()-start)*1000, 3), 0, False


def capture_selects(client, path, marker):
    # Record only one isolated request after timing. Credentials never enter exported logs.
    since = sql("SELECT NOW(6)")
    sql("SET GLOBAL log_output='TABLE'; SET GLOBAL general_log=ON")
    try:
        assert client.sample(path, marker)[2], path
    finally:
        sql("SET GLOBAL general_log=OFF")
    entries = sql(f"SELECT HEX(argument) FROM mysql.general_log WHERE event_time >= '{since}' "
                  "AND command_type IN ('Query','Execute') ORDER BY event_time")
    queries = [bytes.fromhex(h).decode() for h in entries.splitlines()]
    return [q for q in queries if re.match(r"\s*(?:/\*.*?\*/\s*)?(SELECT|WITH)\b", q, re.I | re.S)]


def trace(client, path, marker):
    result = []
    for q in capture_selects(client, path, marker):
        shape = re.sub(r"'(?:''|[^'])*'|\b\d+(?:\.\d+)?\b", "?", " ".join(q.split()))
        plan = sql("EXPLAIN " + q)
        result.append({"shape": shape, "explain": plan.splitlines()})
    return {"select_count": len(result), "queries": result}


def main():
    counts = sql("SELECT (SELECT COUNT(*) FROM san_pham),(SELECT COUNT(*) FROM bien_the_san_pham),"
                 "(SELECT COUNT(*) FROM danh_muc),(SELECT COUNT(*) FROM thuong_hieu),(SELECT COUNT(*) FROM don_hang)")
    assert counts.split() == ["1200","3600","24","40","480"], counts
    clients = {n: Client(n) for n in range(1, 9)}
    guests = [Client(None), Client(None)]
    checkout_ok = b'data-quote-valid="true"'
    routes = [
        ("catalog", "/products", guests, b"perf-"),
        ("catalog-page", "/products?page=4", guests, b"perf-"),
        ("search-filter", "/products?q=Serum&category=cat-1&sort=priceAsc&page=1", guests, b"perf-"),
        ("pdp", "/products/perf-1", guests, b"PF-1"),
        ("admin", "/admin/catalog/products?page=3", [clients[7], clients[8]], b"perf-"),
    ]
    for size, first in [(1,1),(10,3),(100,5)]:
        users = [clients[first], clients[first+1]]
        routes.extend([(f"cart-{size}", "/cart", users, b"data-cart-line="),
                       (f"checkout-{size}", "/checkout", users, checkout_ok)])
    routes.append(("quote-100", "/checkout/quote", [clients[5], clients[6]], checkout_ok))
    metrics, traces, raw = {}, {}, {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        for name, path, users, marker in routes:
            first = users[0].sample(path, marker)
            if not first[2]:
                raise RuntimeError(f"Invalid fixture response: {name} ({first[1]} bytes)")
            until = time.monotonic() + 2
            while time.monotonic() < until:
                assert users[0].sample(path, marker)[2], name
            start = time.perf_counter()
            batches = [executor.submit(lambda c: [c.sample(path, marker) for _ in range(50)], c) for c in users]
            samples = [s for batch in batches for s in batch.result()]
            duration = time.perf_counter() - start
            ms = sorted(s[0] for s in samples)
            metrics[name] = {"first_ms": first[0], "n": len(ms), "p50_ms": statistics.median(ms),
                             "p95_ms": ms[math.ceil(.95*len(ms))-1], "p99_ms": ms[math.ceil(.99*len(ms))-1],
                             "rps": round(len(ms)/duration, 2), "error_pct": 100*sum(not s[2] for s in samples)/len(ms),
                             "mean_bytes": round(statistics.mean(s[1] for s in samples))}
            raw[name] = samples
            traces[name] = trace(users[0], path, marker)
            metrics[name]["select_count"] = traces[name]["select_count"]
            save("latency.json", raw)
            save("plans.json", traces)
            save("summary.json", metrics)
            print(name, metrics[name], flush=True)
            assert not metrics[name]["error_pct"] and traces[name]["select_count"] > 0


if __name__ == "__main__":
    main()

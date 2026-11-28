"""Run after workload completion. MariaDB ANALYZE executes the captured read SQL."""
import json
import os
import statistics
import urllib.error
import urllib.request
from pathlib import Path

from http_bench import Client, capture_selects, save, site, sql

summary = {}
for name, path, account, marker in [
    ("catalog", "/products", None, b"perf-"), ("admin", "/admin/catalog/products?page=3", 7, b"perf-"),
    ("checkout-100", "/checkout", 5, b'data-quote-valid="true"')]:
    rows = []
    for q in capture_selects(Client(account), path, marker):
        result = sql(q)
        times = [json.loads(sql("ANALYZE FORMAT=JSON "+q))["query_block"]["r_total_time_ms"] for _ in range(7)]
        rows.append({"rows":len(result.splitlines()), "tsv_bytes":len(result.encode()),
                     "median_db_ms":statistics.median(times),"db_ms":times})
    summary[name] = rows

summary["fixture_counts"] = {}
for table in ["san_pham","bien_the_san_pham","danh_muc","thuong_hieu"]:
    summary["fixture_counts"][table] = sql(f"SELECT COUNT(*), SUM(dang_hoat_dong) FROM {table}").split()
summary["cart_rows"] = sql("SELECT COUNT(*) FROM chi_tiet_gio_hang GROUP BY ma_gio_hang ORDER BY ma_gio_hang").split()

# Replace the illustration of product 1 after timing, only in the new PERF fixture.
assert site == "http://127.0.0.1:18516"
directory = Path(os.environ["APP_UPLOADS_SKU_DIR"])
old = "00000000-0000-4000-8000-000000000001.png"
new = "00000000-0000-4000-8001-000000000001.png"
with (directory / new).open("xb") as dst:
    dst.write((directory / old).read_bytes())
sql(f"UPDATE bien_the_san_pham SET url_hinh_anh='/uploads/sku/{new}' WHERE ma_san_pham=1")
client = Client(None)
assert new.encode() in client.read("/products/perf-1")[1]
assert client.read("/uploads/sku/"+new)[1].startswith(b"\x89PNG")
try:
    client.read("/uploads/sku/"+old)
except urllib.error.HTTPError as error:
    assert error.code == 404
else:
    raise AssertionError("Old unreferenced image still public")
summary["replacement"] = {"pdp_new_url":True,"new":200,"old":404}
save("diagnostics.json",summary)

"""Print paired route measurements and verify the PERF read-count contract."""
import json
from pathlib import Path

qa = Path(__file__).resolve().parent


def load(folder, name):
    return json.loads((qa / folder / name).read_text(encoding="utf-8-sig"))


before = load("baseline", "summary.json")
after = load("candidate", "summary.json")
assert before.keys() == after.keys()
print("| Route | p50 ms | p95 ms | p99 ms | RPS | SELECT | HTML byte |")
print("|---|---:|---:|---:|---:|---:|---:|")
for name, a in before.items():
    b = after[name]
    assert a["n"] == b["n"] == 100 and a["error_pct"] == b["error_pct"] == 0
    saved = int(name.startswith(("cart-", "checkout-", "quote-")))
    assert a["select_count"] - b["select_count"] == saved, name
    pairs = []
    for key in ["p50_ms", "p95_ms", "p99_ms", "rps", "select_count", "mean_bytes"]:
        fmt = ".1f" if key not in ("select_count", "mean_bytes") else "d"
        pairs.append(f"{a[key]:{fmt}} → {b[key]:{fmt}}")
    print("| " + " | ".join([name, *pairs]) + " |")

for folder in ["baseline", "candidate"]:
    diagnostics = load(folder, "diagnostics.json")
    assert diagnostics["replacement"] == {"pdp_new_url": True, "new": 200, "old": 404}
    assert diagnostics["cart_rows"] == ["1", "1", "10", "10", "100", "100"]
    checks = load(folder, "cart-webkit.json")
    assert len(checks) == 6 and all(r["badge"] == r["count"] and r["status"] == 200 for r in checks)

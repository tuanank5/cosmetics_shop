"""Run from the QA checkout before committing. Only path and rule names are reported."""
import json
import re
import subprocess
import sys


def git(*args):
    return subprocess.check_output(["git", *args])


def staged(path):
    return git("show", ":" + path).decode("utf-8-sig")


def main():
    expected = set(staged("docs/qa/AUTH/final-review/scope.txt").splitlines())
    paths = set(git("diff", "--cached", "--name-only", "-z").decode().split("\0")) - {""}
    if expected != paths:
        sys.exit("Scope mismatch: " + ", ".join(sorted(expected ^ paths)))
    rules = {
        "private-key": r"-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----",
        "service-token": r"(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|AKIA[A-Z0-9]{16}|AIza[\w-]{35}|xox[baprs]-[\w-]{20,}|GOCSPX-[\w-]{20,})",
        "signed-token": r"\beyJ[\w-]{8,}\.eyJ[\w-]{8,}\.[\w-]{8,}",
        "embedded-credentials": r"://[^\s/:@]+:[^\s/@]+@",
        "bearer-cookie-reset": r"(?i)(?:bearer\s+|JSESSIONID=|reset-password\?token=)[A-Za-z0-9_./+=-]{16,}",
        "literal-secret": r'''(?i)(?:password|client.secret|api.key|access.token|secret.key)\s*[:=]\s*["']([^"'\s]{8,})["']''',
    }
    findings = []
    fixtures = 0
    for path in sorted(paths):
        content = staged(path)
        for name, pattern in rules.items():
            for match in re.finditer(pattern, content):
                if (name == "literal-secret" and path.startswith("src/test/") and
                        match.group(1) == "fixture-callback-password"):
                    fixtures += 1
                    continue
                findings.append({"file": path, "rule": name})
    print(json.dumps({"files": len(paths), "rules": list(rules), "test_fixture_literals": fixtures,
                      "findings": findings}, indent=2))
    return bool(findings)


if __name__ == "__main__":
    sys.exit(main())

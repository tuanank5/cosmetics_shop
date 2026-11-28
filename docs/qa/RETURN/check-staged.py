"""Check RETURN handoff against Git index bytes. Run from the prepared QA checkout."""
import hashlib
import json
import subprocess
import sys


def git(*args):
    return subprocess.check_output(["git", *args])


def staged(path):
    return git("show", ":" + path)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    prefix = "docs/qa/RETURN/"
    manifest_path = prefix + "manifest.sha256"
    source = json.loads(staged(prefix + "source.json"))
    if git("rev-parse", "HEAD").decode().strip() != source["base"]:
        sys.exit("HEAD differs from the reviewed baseline")
    manifest = {}
    for line in staged(manifest_path).decode().splitlines():
        digest, path = line.split("  ", 1)
        manifest[path] = digest
    changes = git("diff", "--cached", "--name-only", "-z").decode().split("\0")
    if set(filter(None, changes)) != set(manifest) | {manifest_path}:
        sys.exit("Staged paths differ from the delivery manifest")
    inputs = source["files"]
    expected_inputs = {p for p in git("ls-files", "-z", "--", "src", "pom.xml").decode().split("\0") if p}
    expected_inputs.update(prefix + name for name in ("browser.cjs", "run-fixture.ps1"))
    if set(inputs) != expected_inputs:
        sys.exit("Source file set differs from tested inputs")
    for files in (inputs, manifest):
        for path, digest in files.items():
            if sha(staged(path)) != digest:
                sys.exit("Staged byte mismatch: " + path)
    fingerprint = sha("".join(path + "\t" + inputs[path] + "\n" for path in sorted(inputs)).encode())
    if fingerprint != source["testedInputsSHA256"]:
        sys.exit("Source fingerprint mismatch")
    git("diff", "--cached", "--check")
    print(f"{len(inputs)} tested inputs, {len(changes)-1} staged paths, SHA-256 {fingerprint}")
    print("Git tree", git("write-tree").decode().strip())


if __name__ == "__main__":
    main()

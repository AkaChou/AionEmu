#!/usr/bin/env python3
"""Run every memory-bank gate in one shot: derived indexes, structure, freshness.

Run before a commit that touches `.agents/memory-bank/`:

    python3 -B .agents/memory-bank/verify_memory_bank.py

Steps:

1. `sync_memory_bank.py --check`  - derived indexes (index.jsonl, symptom-index.md, summary index)
2. `check_memory_bank.py`         - routing, metadata, links and evidence references
3. `memory_bank_stats.py --fail-over-days N` - CONFIRMED entries older than N days
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


BANK = Path(__file__).resolve().parent


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run all memory-bank gates.")
    parser.add_argument("--root", type=Path, default=BANK.parents[1])
    parser.add_argument(
        "--max-age-days",
        type=int,
        default=90,
        help="fail when a CONFIRMED entry was last verified more than N days ago (0 disables)",
    )
    return parser.parse_args()


def run_step(name: str, command: list[str], root: Path) -> bool:
    proc = subprocess.run(command, capture_output=True, text=True, check=False, cwd=root)
    output = (proc.stdout + proc.stderr).strip()
    print(f"=== {name} ===")
    if output:
        print(output)
    print(f"--- {name}: {'OK' if proc.returncode == 0 else 'FAILED'}")
    return proc.returncode == 0


def main() -> None:
    args = parse_args()
    root = args.root.resolve()
    steps = [
        (
            "derived-index",
            [sys.executable, "-B", str(BANK / "sync_memory_bank.py"), "--check", "--root", str(root)],
        ),
        (
            "structure",
            [sys.executable, "-B", str(BANK / "check_memory_bank.py"), "--root", str(root)],
        ),
    ]
    if args.max_age_days > 0:
        steps.append(
            (
                f"freshness<= {args.max_age_days}d",
                [
                    sys.executable,
                    "-B",
                    str(BANK / "memory_bank_stats.py"),
                    "--root",
                    str(root),
                    "--fail-over-days",
                    str(args.max_age_days),
                ],
            )
        )

    failed = [name for name, command in steps if not run_step(name, command, root)]
    print()
    if failed:
        print(f"MEMORY_BANK_VERIFY_FAILED STEPS={', '.join(failed)}")
        raise SystemExit(1)
    print(f"MEMORY_BANK_VERIFY_OK STEPS={len(steps)}")


if __name__ == "__main__":
    main()

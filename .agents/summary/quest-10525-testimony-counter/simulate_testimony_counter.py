#!/usr/bin/env python3
"""Reachability model of the 10525 four-agent testimony counter.

Mirrors the typed planner semantics: conditions are evaluated on the pre-increment
value, the increment is applied inside the transition, and the completion routes
require an exact pre-value (15 - delta).
"""
from __future__ import annotations

import collections
import sys

AGENTS = (("Este", 1, 14), ("Ovest", 2, 13), ("Meridies", 4, 11), ("Ceber", 8, 7))


def run(guarded: bool, maximum: int) -> tuple[set[int], set[int], bool]:
    """Returns (reachable states, completable states, any overflow)."""
    start = 0
    seen = {start}
    queue = collections.deque([start])
    completion = set()
    overflow = False
    while queue:
        value = queue.popleft()
        for _name, delta, completion_value in AGENTS:
            if value == completion_value:
                completion.add(value)
            if not guarded or value < completion_value:
                produced = value + delta
                if produced > maximum:
                    overflow = True
                    continue
                if produced not in seen:
                    seen.add(produced)
                    queue.append(produced)
    return seen, completion, overflow


def reachable_from(value: int, guarded: bool, maximum: int) -> set[int]:
    seen = {value}
    queue = collections.deque([value])
    while queue:
        current = queue.popleft()
        for _name, delta, completion_value in AGENTS:
            if not guarded or current < completion_value:
                produced = current + delta
                if produced > maximum:
                    continue
                if produced not in seen:
                    seen.add(produced)
                    queue.append(produced)
    return seen


def main() -> int:
    print("== current (unguarded) contract, declared max 31 ==")
    seen, completion, overflow = run(False, 31)
    print(f"states reachable from 0: {len(seen)}  max={max(seen)}  completion values hit: {sorted(completion)}")
    print(f"overflow beyond declared max: {overflow}  (first overflow value: "
          f"{min(v for v in seen if False) if False else ''}"
          f"{next((v for v in sorted(seen) if v + 8 > 31), '-')} + 8)")
    print("states that can still reach a completion value: "
          f"{sum(1 for v in sorted(seen) if completion & reachable_from(v, False, 31))}/{len(seen)}")

    print()
    print("== repaired (guarded) contract, declared max 15 ==")
    seen, completion, overflow = run(True, 15)
    print(f"states reachable from 0: {sorted(seen)}  max={max(seen)}  overflow: {overflow}")
    unresolved = [v for v in sorted(seen) if not completion & reachable_from(v, True, 15)]
    print(f"states that can still reach a completion value: {len(seen) - len(unresolved)}/{len(seen)} {unresolved}")
    for legacy in (6, 24, 386):
        value = (legacy >> 6) & 0x0F if legacy > 15 else legacy
        print(f"legacy packed {legacy} decodes to var1={value}; reachable completion: "
              f"{bool(completion & reachable_from(value, True, 15))}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

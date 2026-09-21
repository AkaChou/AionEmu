#!/usr/bin/env python3
"""审计：击杀自环路线在计数器已饱和后仍会命中（提交状态未变化的空事务并下发一次任务更新）。

判定：自环（source == target）击杀路线的动作把某字段 set 到常量 V，而条件里该字段只有下界
（variable-at-least <= V）且没有 variable-is / 上界把 V 排除，则存在一个计数器已等于 V 的状态
仍然满足条件 —— 该状态下的额外击杀不会改变任何状态，却会执行 sync-quest-state 并让客户端显示"任务更新"。
"""
from __future__ import annotations
import re
import pathlib

QUESTS = pathlib.Path(__file__).resolve().parents[3] / "src/main/resources/aion/data/static_data/quest_definition/quests"


def main() -> int:
    hits = []
    for file in sorted(QUESTS.glob("*.xml"), key=lambda path: int(path.stem)):
        text = file.read_text(encoding="utf-8")
        for match in re.finditer(r"<transition\b([^>]*)>(.*?)</transition>", text, re.S):
            attrs, body = match.group(1), match.group(2)
            source = re.search(r'source="([^"]*)"', attrs)
            target = re.search(r'target="([^"]*)"', attrs)
            if not source or not target or source.group(1) != target.group(1):
                continue
            if "<kill-npc" not in body:
                continue
            sets = {field: int(value) for field, value in
                    re.findall(r'<set-variable field="([^"]+)" value="(-?\d+)"', body)}
            if re.search(r'<increment-variable\b', body):
                continue
            at_least = {field: int(value) for field, value in
                        re.findall(r'<variable-at-least field="([^"]+)" value="(-?\d+)"', body)}
            below = {field: int(value) for field, value in
                     re.findall(r'<variable-below field="([^"]+)" value="(-?\d+)"', body)}
            exact = {field for field, _ in re.findall(r'<variable-is field="([^"]+)" value="(-?\d+)"', body)}
            for field, value in sets.items():
                if field in exact:
                    continue
                lower = at_least.get(field)
                upper = below.get(field)
                if lower is not None and lower <= value and (upper is None or upper > value):
                    hits.append((int(file.stem), source.group(1), field, value, lower, upper))
    for quest_id, source, field, value, lower, upper in hits:
        print(f"{quest_id}\t{source}\t{field}: set {value} while at-least {lower} below {upper}")
    print(f"TOTAL {len(hits)} saturated-but-matching kill self-loops in {len({h[0] for h in hits})} quests")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

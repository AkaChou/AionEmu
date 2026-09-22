#!/usr/bin/env python3
"""PREREQ 批次补丁（真端 finished_quest_cond 缺失前置 + 2641 迁移漂移修正）。

只做两种改动：
  1) ADD   : 仓库缺失的单前置分支 -> 插入 <metadata><prerequisites><quest id=X/></prerequisites>
  2) REPLACE: 2641 的 <start-conditions> finished 2640 -> <prerequisites> 2619
插入位置严格遵守 quest_definition.xsd 的 metadata 子元素顺序
(races/classes/gender/repeat/prerequisites/items/.../rewards/.../kills/start-conditions...)。
"""
from __future__ import annotations
import re, sys
from pathlib import Path

QUESTS = Path("src/main/resources/aion/data/static_data/quest_definition/quests")

# quest -> (prerequisite, 证据)
ADD = {
    15471: (15402, "retail quest.xml cond1=Q15402; quest_data.xml finished 15402"),
    15551: (15550, "retail+client cond1=Q15550; quest_data.xml finished 15550"),
    15552: (15551, "retail+client cond1=Q15551; quest_data.xml finished 15551"),
    15553: (15552, "retail+client cond1=Q15552; quest_data.xml finished 15552"),
    15554: (15553, "retail+client cond1=Q15553; quest_data.xml finished 15553"),
    15563: (15550, "retail+client cond1=Q15550; quest_data.xml finished 15550"),
    15595: (15550, "retail+client cond1=Q15550; quest_data.xml finished 15550"),
    15673: (15550, "retail+client cond1=Q15550; quest_data.xml finished 15550"),
    16823: (16822, "retail cond1=Q16822 (LEVEL_UP 迁移条件已含 16822，元数据缺失)"),
    16824: (16821, "retail+client cond1=Q16821"),
    16825: (16822, "retail+client cond1=Q16822"),
    18035: (18036, "retail cond1=Q18036; quest_data.xml finished 18036"),
    18821: (18830, "retail cond1=Q18830; quest_data.xml finished 18830"),
    18993: (18992, "retail cond1=Q18992"),
    21004: (21001, "retail cond1=Q21001"),
    21080: (21065, "retail cond1=Q21065"),
    21201: (21200, "retail cond1=Q21200; quest_data.xml finished 21200"),
    2533:  (2532, "retail cond1=Q2532"),
    26823: (26822, "retail cond1=Q26822 (LEVEL_UP 迁移条件已含 26822，元数据缺失)"),
    28035: (28036, "retail cond1=Q28036; quest_data.xml finished 28036"),
    3050:  (3049, "retail cond1=Q3049"),
    30719: (30708, "retail cond1=Q30708"),
    49004: (49003, "retail cond1=Q49003"),
    80343: (80341, "retail cond1=Q80341"),
}

# 2641: 仓库 start-conditions 引用 2640，真端/客户端/quest_data 三源一致为 2619
REPLACE = {2641: (2619, 2640, "retail+client cond1=Q2619; quest_data.xml finished 2619 (仓库误为 2640)")}

PRE_TAGS = ("races", "classes", "gender", "repeat")
POST_TAGS = ("items", "inventory-items", "work-items", "rewards", "reward-groups", "extended-rewards",
             "extended-reward-groups", "drops", "bonus", "bonuses", "kills", "start-conditions",
             "start-condition-groups", "class-rewards")


def block_span(text: str, tag: str, start: int = 0):
    open_m = re.compile(rf"<{tag}(?:\s[^>]*)?>").search(text, start)
    if not open_m:
        return None
    if open_m.group(0).endswith("/>"):
        return open_m.start(), open_m.end()
    close = re.compile(rf"</{tag}>").search(text, open_m.end())
    if not close:
        raise SystemExit(f"unterminated <{tag}>")
    return open_m.start(), close.end()


def insertion_offset(text: str, metadata_start: int, metadata_end: int) -> int:
    offset = text.index(">", metadata_start) + 1
    cursor = offset
    while True:
        m = re.compile(r"<([a-z-]+)").search(text, cursor)
        if not m or m.start() >= metadata_end:
            break
        tag = m.group(1)
        span = block_span(text, tag, m.start())
        if span is None:
            break
        if tag in PRE_TAGS:
            offset = span[1]
        elif tag in POST_TAGS:
            break
        else:
            raise SystemExit(f"unexpected metadata child <{tag}>")
        cursor = span[1]
    newline = text.find("\n", offset)
    if newline == -1:
        raise SystemExit("insertion point not before a newline")
    return newline + 1


def render_pre(prereq: int) -> str:
    return (f"    <prerequisites>\n"
            f"      <quest id=\"{prereq}\"/>\n"
            f"    </prerequisites>\n")


def apply_add(path: Path, prereq: int) -> str:
    text = path.read_text(encoding="utf-8")
    meta = block_span(text, "metadata")
    if meta is None:
        raise SystemExit(f"{path}: no metadata")
    if "<prerequisites>" in text[meta[0]:meta[1]]:
        raise SystemExit(f"{path}: already has prerequisites")
    if f'quest-id="{prereq}"' in text[meta[0]:meta[1]]:
        raise SystemExit(f"{path}: metadata already references {prereq}")
    off = insertion_offset(text, meta[0], meta[1])
    return text[:off] + render_pre(prereq) + text[off:]


def apply_replace(path: Path, prereq: int, drop: int) -> str:
    text = path.read_text(encoding="utf-8")
    meta = block_span(text, "metadata")
    if meta is None:
        raise SystemExit(f"{path}: no metadata")
    sc = block_span(text, "start-conditions", meta[0])
    if sc is None or sc[1] > meta[1]:
        raise SystemExit(f"{path}: expected a start-conditions block")
    block = text[sc[0]:sc[1]]
    if block.count("<condition") != 1 or f'quest-id="{drop}"' not in block:
        raise SystemExit(f"{path}: unexpected start-conditions block {block!r}")
    text = text[:sc[0]] + text[sc[1]:]
    text = re.sub(r"\n[ \t]+\n(\s*</metadata>)", r"\n\1", text, count=1)
    meta = block_span(text, "metadata")
    off = insertion_offset(text, meta[0], meta[1])
    return text[:off] + render_pre(prereq) + text[off:]


def main() -> int:
    changed = []
    for qid, (prereq, _ev) in sorted(ADD.items()):
        path = QUESTS / f"{qid}.xml"
        path.write_text(apply_add(path, prereq), encoding="utf-8")
        changed.append((qid, "ADD", prereq))
    for qid, (prereq, drop, _ev) in sorted(REPLACE.items()):
        path = QUESTS / f"{qid}.xml"
        path.write_text(apply_replace(path, prereq, drop), encoding="utf-8")
        changed.append((qid, "REPLACE", prereq))
    for row in changed:
        print(*row)
    print("changed files:", len(changed))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

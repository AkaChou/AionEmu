#!/usr/bin/env python3
"""只读扫描：客户端 quest_summary 末行与首行同文（末行=第 0 行重复行）的任务族，并对照本仓库 reward 投影。

客户端证据：Aion 5.8 客户端解包 data_unpacked/Dialogs/**/quest_q<id>.html 的 quest_summary。
仓库证据：src/main/resources/aion/data/static_data/quest/definitions/quests/<id>.xml 的节点投影。
"""
from __future__ import annotations

import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
UNPACK = Path(str(REPO.parent / "PycharmProjects" / "unpak"))
DIALOGS = UNPACK / "data_unpacked/Dialogs"

TOKEN_RE = re.compile(r"\[%[^\]]*\]")


def normalize(fragment: str) -> str:
    text = re.sub(r"<[^>]+>", "", fragment)
    text = TOKEN_RE.sub("", text)
    return re.sub(r"\s+", "", text)


def rows(quest_id: int) -> list[str] | None:
    for path in DIALOGS.glob(f"*/quest_q{quest_id}.html"):
        text = path.read_text(encoding="utf-8", errors="replace")
        m = re.search(r'<HtmlPage name="quest_summary".*?</HtmlPage>', text, re.S)
        if not m:
            return None
        return [normalize(step) for step in re.findall(r"<step>.*?</step>", m.group(0), re.S)]
    return None


def projections(quest_id: int) -> tuple[int | None, int | None, str]:
    path = QUESTS / f"{quest_id}.xml"
    if not path.exists():
        return None, None, "NO_XML"
    root = ET.parse(path).getroot()
    reward = None
    starts: list[int] = []
    for node in root.findall("./nodes/node"):
        label = node.get("label", "")
        status = node.get("status", "")
        value = None
        for var in node.findall("var"):
            if var.get("name") == "var0":
                value = int(var.get("value"))
        if status == "REWARD":
            reward = value
        elif status == "START" and label not in ("unaccepted",):
            if value is not None:
                starts.append(value)
    return reward, (max(starts) if starts else None), ""


def main() -> None:
    out = []
    for path in sorted(DIALOGS.glob("*/quest_q*.html")):
        m = re.search(r"quest_q(\d+)\.html$", path.name)
        if not m:
            continue
        quest_id = int(m.group(1))
        row_list = rows(quest_id)
        if not row_list or len(row_list) < 3:
            continue
        first, last = row_list[0], row_list[-1]
        if not first or first != last:
            continue
        reward, last_start, note = projections(quest_id)
        out.append((quest_id, len(row_list), reward, last_start, last_start, note))
    print("quest_id\trows\treward_var0\tlast_start_var0\tverdict")
    for quest_id, count, reward, last_start, _mirror, note in out:
        verdict = "OK" if (reward is not None and last_start is not None and reward == last_start) else "REWARD_EQ_LAST_ROW"
        print(f"{quest_id}\t{count}\t{reward}\t{last_start}\t{verdict}{(' ' + note) if note else ''}")
    (Path(__file__).resolve().parent / "duplicate-row0-family.tsv").write_text(
        "quest_id\trows\treward_var0\tlast_start_var0\tverdict\n"
        + "".join(
            f"{q}\t{c}\t{r}\t{l}\t{'OK' if (r is not None and l is not None and r == l) else 'REWARD_EQ_LAST_ROW'}\n"
            for q, c, r, l, _m, _n in out
        ),
        encoding="utf-8",
    )
    print(f"\nfamily size = {len(out)}", file=sys.stderr)


if __name__ == "__main__":
    main()

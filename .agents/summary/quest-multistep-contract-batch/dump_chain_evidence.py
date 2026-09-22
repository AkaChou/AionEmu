#!/usr/bin/env python3
"""逐个任务导出「客户端步骤链 ↔ retail 步骤 ↔ 当前 XML」三方证据，供批量修复前核对。"""
from __future__ import annotations

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
DIALOG = Path("/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs")
RETAIL = Path("/tmp/zz_retail.xml")

PAGE_RE = re.compile(r'<HtmlPage name="([^"]+)">(.*?)</HtmlPage>', re.S)
ACT_RE = re.compile(r'<Act href="([^"]*)">([^<]*)</Act>')
TEXT_RE = re.compile(r"<[^>]+>")


def npc_names() -> dict[int, str]:
    """NPC id -> 内部名（npc_template 的属性顺序不固定，必须整体解析属性）。"""
    names: dict[int, str] = {}
    for path in (REPO / "src/main/resources/aion/data/static_data/npcs").glob("*.xml"):
        raw = path.read_text(encoding="utf-8", errors="ignore")
        for tag in re.findall(r'<npc_template\b[^>]*>', raw):
            attrs = dict(re.findall(r'(\w+)="([^"]*)"', tag))
            npc_id = attrs.get("npc_id")
            if npc_id and npc_id.isdigit() and attrs.get("name"):
                names.setdefault(int(npc_id), attrs["name"])
    return names


def client(quest_id: int) -> tuple[list[str], dict[str, list[tuple[str, str]]], dict[str, str]]:
    """与 audit_reward_row_vs_client_steps.py 同口径：递归查找 quest_q<id>.html，优先非 unused、含 quest_summary 的副本。"""
    candidates: list[Path] = []
    for path in DIALOG.rglob("*.html"):
        if re.fullmatch(rf"(?i)quest_q{quest_id}\.html", path.name):
            candidates.append(path)
    if not candidates:
        return [], {}, {}
    candidates.sort(key=lambda path: ("unused" in path.parts, path.name.startswith("QUEST")))
    with_page = [path for path in candidates if '<HtmlPage name="quest_summary">' in
                 path.read_text(encoding="utf-8", errors="ignore")]
    path = (with_page or candidates)[0]
    text = path.read_text(encoding="utf-8", errors="replace")
    summary = re.search(r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', text, re.S)
    rows = [re.sub(r"\s+", " ", TEXT_RE.sub("", step)).strip()
            for step in re.findall(r"<step>(.*?)</step>", summary.group(1), re.S)] if summary else []
    pages: dict[str, list[tuple[str, str]]] = {}
    first_text: dict[str, str] = {}
    for match in PAGE_RE.finditer(text):
        name = match.group(1)
        entries = []
        for href, label in ACT_RE.findall(match.group(2)):
            tokens = re.findall(r"HACTION_([A-Z0-9_]+)", href)
            entries.append((tokens[-1] if tokens else href, label.strip()))
        pages[name] = entries
        first_text[name] = re.sub(r"\s+", " ", TEXT_RE.sub(" ", match.group(2))).strip()[:60]
    return rows, pages, first_text


def chains(pages: dict[str, list[tuple[str, str]]], first_text: dict[str, str]):
    """返回以推进动作结尾的完整链：(terminal, 页面序列, 首段文本)。"""
    reverse: dict[str, str] = {}
    for name, acts in pages.items():
        for action, _label in acts:
            if re.fullmatch(r"SELECT\d+_\d+(?:_\d+)*", action):
                reverse.setdefault(action.lower(), name)
    result = []
    for name, acts in pages.items():
        if not acts:
            continue
        for action, _label in acts:
            if not re.fullmatch(r"(SETPRO\d+|SET_SUCCEED|SELECT_QUEST_REWARD)", action):
                continue
            seq = [name]
            current = name
            guard = 0
            while guard < 12:
                guard += 1
                prev = reverse.get(current)
                if prev is None or prev in seq:
                    break
                seq.insert(0, prev)
                current = prev
            result.append((action, seq, first_text.get(seq[0], "")))
    order = {"SET_SUCCEED": 900, "SELECT_QUEST_REWARD": 950}
    result.sort(key=lambda item: (order.get(item[0], int(re.sub(r"\D", "", item[0]) or 0)), item[1][0]))
    return result


def retail_steps(quest_id: int) -> list[dict[str, str]]:
    if not RETAIL.exists():
        return []
    text = RETAIL.read_text(encoding="utf-8", errors="ignore")
    match = re.search(rf'<data_driven_quest id="{quest_id}"[^>]*>(.*?)</data_driven_quest>', text, re.S)
    if not match:
        return []
    return [dict(re.findall(r'(\w+)="([^"]*)"', step))
            for step in re.findall(r"<step\b([^>]*)/>", match.group(1))]


def retail_entry(quest_id: int) -> tuple[str, dict[str, str]] | None:
    """retail 脚本中该任务的任意类型条目（data_driven_quest/item_order/report_to...）。"""
    if not RETAIL.exists():
        return None
    text = RETAIL.read_text(encoding="utf-8", errors="ignore")
    match = re.search(rf'^  <(\w+) id="{quest_id}"([^>]*?)(?:/>|>.*?</\1>)', text, re.S | re.M)
    if not match:
        return None
    return match.group(1), dict(re.findall(r'(\w+)="([^"]*)"', match.group(2)))


def xml_facts(quest_id: int) -> dict[str, object]:
    path = QUESTS / f"{quest_id}.xml"
    raw = path.read_text(encoding="utf-8")
    root = ET.fromstring(raw)
    nodes_element = root.find("nodes")
    states = []
    for node in (nodes_element if nodes_element is not None else []):
        var0 = {v.get("name"): v.get("value") for v in node.findall("var")}.get("var0")
        if node.get("status") in ("START", "REWARD"):
            states.append(f"{node.get('label')}@{var0}")
    actions = sorted(set(re.findall(r'\bactions?="([^"]+)"', raw)))
    pages = sorted(set(re.findall(r'page="([A-Za-z0-9_]+)"', raw)))
    start_npcs = re.findall(r'<dialog type="NPC_START" npc-id="(\d+)"', raw)
    completes = re.findall(r'<npc-complete npc-id="(\d+)"', raw)
    return {"states": states, "actions": actions, "pages": pages,
            "start_npcs": start_npcs, "completes": completes, "file": str(path)}


def main() -> int:
    names = npc_names()
    for raw_id in sys.argv[1:]:
        quest_id = int(raw_id)
        rows, pages, first_text = client(quest_id)
        facts = xml_facts(quest_id)
        print(f"\n########## {quest_id} | NPC_START={facts['start_npcs']} completes={facts['completes']}")
        for index, row in enumerate(rows):
            print(f"  row{index}: {row}")
        print("  retail steps:", [(s.get("ids"), s.get("dialog_id"), s.get("give_item_id"), s.get("remove_item_id"))
                                  for s in retail_steps(quest_id)])
        entry = retail_entry(quest_id)
        print("  retail entry:", entry)
        for terminal, seq, text in chains(pages, first_text):
            print(f"  chain {terminal:20s} {' -> '.join(seq)}  | {text}")
        print("  xml states:", facts["states"])
        print("  xml actions:", facts["actions"])
        print("  xml pages:", facts["pages"])
        involved = sorted({int(v) for v in facts["start_npcs"] + facts["completes"]})
        print("  xml npc names:", {i: names.get(i, "?") for i in involved})
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

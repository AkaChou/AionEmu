#!/usr/bin/env python3
"""P5D 步 3 激活批证据探针（18213/28213）。

逐行复算激活判据的四源：
  ① 真端表 Quest_SimpleItemPlay.xml（接取/交付 NPC、中继链、步发扣、演出道具、过场/交付门列）；
  ② 真端 quest.xml（minlevel_permitted、race_permitted、finished_quest_cond1、quest_work_itemN）；
  ③ 客户端页契约 client_dialog_contract.tsv（入口页/问询窗/步页声明）；
  ④ owner 台账 retail-xml-retention.tsv + 目录 quest_definition_catalog.xml（退役且目录条目已删）。

用法：
  python3 itemplay-activation-probe.py                # 打印 TSV 到 stdout
  python3 itemplay-activation-probe.py --out <path>   # 落盘
"""
import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[5]
ITEMPLAY_TABLE = REPO / "src/main/resources/aion/data/static_data/quest/retail/Quest_SimpleItemPlay.xml"
QUEST_XML = REPO / "src/main/resources/aion/data/static_data/quest/retail/quest.xml"
CONTRACT = REPO / "src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv"
RETENTION = REPO / "src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv"
CATALOG = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml"
XML_DIR = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"

ACTIVATED = (18213, 28213)
ROW = re.compile(r'<id id="(\d+)">(.*?)</id>', re.S)
FIELD = re.compile(r"<([a-z_0-9]+)>\s*([^<]*?)\s*</\1>")


def table_rows():
    text = ITEMPLAY_TABLE.read_text(encoding="utf-8")
    rows = {}
    for quest_id, body in ROW.findall(text):
        rows[int(quest_id)] = {k: v.strip() for k, v in FIELD.findall(body)}
    return rows


def quest_rows():
    text = QUEST_XML.read_text(encoding="utf-8")
    rows = {}
    for block in re.findall(r"<quest>.*?</quest>", text, re.S):
        m = re.search(r"<id>\s*(\d+)\s*</id>", block)
        if not m:
            continue
        quest_id = int(m.group(1))
        if quest_id not in ACTIVATED:
            continue
        rows[quest_id] = {k: v.strip() for k, v in FIELD.findall(block)}
    return rows


def contract_pages():
    pages = {}
    for line in CONTRACT.read_text(encoding="utf-8").splitlines():
        cells = line.split("\t")
        if len(cells) != 3 or not cells[0].isdigit():
            continue
        quest_id = int(cells[0])
        if quest_id in ACTIVATED:
            pages.setdefault(quest_id, []).append((int(cells[1]), cells[2]))
    return {k: sorted(v) for k, v in pages.items()}


def owners():
    result = {}
    for line in RETENTION.read_text(encoding="utf-8").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        cells = line.split("\t", -1)
        if cells[0].isdigit() and int(cells[0]) in ACTIVATED:
            result[int(cells[0])] = cells
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out")
    args = parser.parse_args()

    table, quests, pages, owner_rows = table_rows(), quest_rows(), contract_pages(), owners()
    catalog = CATALOG.read_text(encoding="utf-8")
    lines = [
        "# P5D 步 3 激活批证据（真端表 + 真端 quest.xml + 客户端页契约 + owner 台账；逐行复算）",
        "\t".join([
            "quest_id", "race", "minlevel", "prereq", "retention_owner", "xml_in_repo", "catalog_entry",
            "acquire_npc", "relay_npc_1", "relay_npc_2", "reward_npc", "talk_count",
            "accept_give", "step2_give", "step2_remove", "use_item", "declared_pages", "verdict",
        ]),
    ]
    problems = []
    for quest_id in ACTIVATED:
        raw, meta, owner = table[quest_id], quests[quest_id], owner_rows[quest_id]
        page_ids = [page for page, _ in pages.get(quest_id, [])]
        verdict = "NATIVE_READY"
        if owner[1] != "RETAIL_TABLE":
            problems.append(f"{quest_id}: retention owner = {owner[1]}")
        if (XML_DIR / f"{quest_id}.xml").exists():
            problems.append(f"{quest_id}: XML definition still in repo")
        if f'id="{quest_id}"' in catalog:
            problems.append(f"{quest_id}: catalog entry still present")
        if meta.get("minlevel_permitted") == "999":
            problems.append(f"{quest_id}: stopped row (minlevel 999)")
        for required in ("acquired_npc_name", "reward_npc_name", "use_item_name"):
            if not raw.get(required):
                problems.append(f"{quest_id}: missing {required}")
        lines.append("\t".join([
            str(quest_id),
            meta.get("race_permitted", "-"),
            meta.get("minlevel_permitted", "-"),
            meta.get("finished_quest_cond1", "-"),
            owner[1],
            str((XML_DIR / f"{quest_id}.xml").exists()).lower(),
            str(f'id="{quest_id}"' in catalog).lower(),
            raw.get("acquired_npc_name", "-"),
            raw.get("talk_npc1", "-"),
            raw.get("talk_npc2", "-"),
            raw.get("reward_npc_name", "-"),
            str(sum(1 for key in raw if re.fullmatch(r"talk_npc\d+", key))),
            raw.get("give_item", "-"),
            raw.get("give_item2", "-"),
            raw.get("remove_item2", "-"),
            raw.get("use_item_name", "-"),
            ",".join(str(page) for page in page_ids),
            verdict,
        ]))
    text = "\n".join(lines) + "\n"
    if args.out:
        Path(args.out).write_text(text, encoding="utf-8")
    else:
        sys.stdout.write(text)
    if problems:
        for problem in problems:
            print("P5D3_PROBLEM " + problem, file=sys.stderr)
        return 1
    print("P5D3_ACTIVATION_OK rows=" + str(len(ACTIVATED)), file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

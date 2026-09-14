#!/usr/bin/env python3
"""批 A1：纯 NPC_REPORT 完成型任务改为显式 1009 物品检查对（客户端 fail/ok 页接线）。

Batch A1: replace single NPC_REPORT completion with explicit 1009 check pairs proven by
the client quest.xml check_item fields (names resolved via the retail Items.xml):
  (started, npc, 31)          -> SHOW DEFAULT_SUCCESS
  (started, npc, 1009) prio 0 -> has-item(id,count) remove + sync + SHOW reward window 1
  (started, npc, 1009) prio 1 -> SHOW CHECK_USER_ITEM_FAIL
  (started, npc, 1008)        -> close dialog
"""
from __future__ import annotations

import csv
import hashlib
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / ".agents/summary/quest-load-fail"))
from fix_notask_pages import client_model, quest_xml_path  # noqa: E402

UNPACK = Path("/Users/mc/IdeaProjects/58Server/Map/XML")
CLIENT_UNPACK = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked")
MANIFEST = ROOT / ".agents/summary/quest-load-fail/npc-report-check-manifest.csv"
AUDIT = ROOT / ".agents/summary/quest-load-fail/quest-order-audit-current.csv"


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def client_check_items() -> dict[str, list[tuple[str, int]]]:
    """quest.xml check_item/collect_item 名字 -> (item name, count)。"""
    out: dict[str, list[tuple[str, int]]] = {}
    cur = None
    for event, elem in ET.iterparse(CLIENT_UNPACK / "quest.xml"):
        if elem.tag == "quest":
            cur = (elem.findtext("id") or "").strip()
            continue
        if cur and elem.tag in ("check_item1_1", "check_item1_2", "collect_item1", "collect_item2") \
                and elem.text and elem.text.strip():
            parts = elem.text.strip().split()
            if len(parts) == 2:
                out.setdefault(cur, []).append((parts[0], int(parts[1])))
    return out


def item_name_to_id() -> dict[str, str]:
    found: dict[str, str] = {}
    cur = None
    for event, elem in ET.iterparse(UNPACK / "Items.xml"):
        if elem.tag == "item":
            elem.clear()
            cur = None
        elif elem.tag == "id" and elem.text:
            cur = elem.text.strip()
        elif elem.tag == "name" and elem.text and cur:
            found[elem.text.strip().lower()] = cur
    return found


def main() -> int:
    write = "--write" in sys.argv
    pages, actions = client_model()
    check_items = client_check_items()
    name_to_id = item_name_to_id()
    rows = read_csv(AUDIT)
    unr_quests = set()
    for r in rows:
        if r["audit_status"] != "CLIENT_PAGE_UNREACHED":
            continue
        m = re.match(r"^([^#]+)#(\S+?) page-order", r["evidence_source"])
        if m and m.group(2) in ("check_user_item_fail", "check_user_item_ok"):
            unr_quests.add(int(r["quest_id"]))

    manifest_rows = []
    edits: dict[Path, list[dict]] = defaultdict(list)
    for q in sorted(unr_quests):
        path = quest_xml_path(q)
        root = ET.parse(path).getroot()
        reports = [d for d in root.findall("./transitions/dialog") if d.get("type") == "NPC_REPORT"]
        if len(reports) != 1:
            continue  # 批 A1 只处理单 NPC_REPORT
        has39 = any(e.get("action") == "CHECK_USER_HAS_QUEST_ITEM"
                    for t in root.findall("./transitions/transition")
                    for e in t.findall("./event/dialog"))
        if has39 or root.findall("./transitions/npc-item-report"):
            continue
        # check_item 与 collect_item 常为同一物品的两种视图：按 id 合并取最大数量。
        # check_item and collect_item often mirror the same item: merge by id, keep max count.
        merged: dict[str, int] = {}
        for name, count in check_items.get(str(q), []):
            item_id = name_to_id.get(name.lower(), name)
            merged[item_id] = max(merged.get(item_id, 0), count)
        items = sorted(merged.items())
        if not items:
            manifest_rows.append({"quest_id": q, "decision": "skip: no check_item evidence"})
            continue
        d = reports[0]
        npc = d.get("npc-id")
        src = d.get("source")
        tgt = d.get("target")
        page = d.get("page")
        if not (npc and src and tgt and page):
            manifest_rows.append({"quest_id": q, "decision": "skip: malformed NPC_REPORT"})
            continue
        # 幂等：已有显式 1009 路由则跳过
        if any(any(e.get("npc-id") == npc and e.get("action") == "SELECT_QUEST_REWARD"
                   for e in t.findall("./event/dialog"))
               for t in root.findall("./transitions/transition")):
            manifest_rows.append({"quest_id": q, "decision": "skip: explicit 1009 exists"})
            continue
        items_desc = "+".join(f"{i}x{c}" for i, c in items)
        manifest_rows.append({"quest_id": q, "npc": npc, "src": src, "tgt": tgt, "page": page,
                              "items": items_desc, "decision": "replace-npc-report"})
        edits[path].append({"npc": npc, "src": src, "tgt": tgt, "page": page, "items": items})

    with MANIFEST.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["quest_id", "npc", "src", "tgt", "page", "items", "decision"])
        writer.writeheader()
        writer.writerows(manifest_rows)
    adds = sum(1 for r in manifest_rows if r["decision"] == "replace-npc-report")
    print(f"batch A1: candidates={adds} files={len(edits)} manifest={MANIFEST}")
    if not write:
        return 0

    has_cond = "".join(f'        <has-item item-id="{i}" count="{c}"/>\n' for i, c in items)
    for path, items_list in edits.items():
        before = path.read_bytes()
        s = before.decode("utf-8")
        for info in items_list:
            npc, src, tgt, page = info["npc"], info["src"], info["tgt"], info["page"]
            items = info["items"]
            has_cond = "".join(f'        <has-item item-id="{i}" count="{c}"/>\n' for i, c in items)
            rm_act = "".join(f'        <remove-item item-id="{i}" count="{c}"/>\n' for i, c in items)
            block = (
                f'\n    <!-- 客户端 select_success 按钮 1009 触发 check_item 检查：'
                f'集齐进 REWARD+奖励窗，不足显示 check_user_item_fail，FINISH_DIALOG 关闭。 -->\n'
                f'    <transition source="{src}" target="{src}">\n'
                f'      <event>\n'
                f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>\n'
                f'      </event>\n'
                f'      <after-commit>\n'
                f'        <dialog type="SHOW_QUEST_PAGE" page="{page}"/>\n'
                f'      </after-commit>\n'
                f'    </transition>\n'
                f'    <transition source="{src}" target="{tgt}" priority="0">\n'
                f'      <event>\n'
                f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SELECT_QUEST_REWARD"/>\n'
                f'      </event>\n'
                f'      <conditions>\n{has_cond}      </conditions>\n'
                f'      <actions>\n{rm_act}      </actions>\n'
                f'      <after-commit>\n'
                f'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
                f'        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>\n'
                f'      </after-commit>\n'
                f'    </transition>\n'
                f'    <transition source="{src}" target="{src}" priority="1">\n'
                f'      <event>\n'
                f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SELECT_QUEST_REWARD"/>\n'
                f'      </event>\n'
                f'      <after-commit>\n'
                f'        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>\n'
                f'      </after-commit>\n'
                f'    </transition>\n'
                f'    <transition source="{src}" target="{src}">\n'
                f'      <event>\n'
                f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="FINISH_DIALOG"/>\n'
                f'      </event>\n'
                f'      <after-commit>\n'
                f'        <close-dialog/>\n'
                f'      </after-commit>\n'
                f'    </transition>')
            # 删除 NPC_REPORT 块（自闭合）
            rep_pat = re.compile(
                r'\n[ \t]*<dialog type="NPC_REPORT" npc-id="%s" source="%s" target="%s" page="%s" */>' %
                tuple(re.escape(x) for x in (npc, src, tgt, page)))
            s2, n = rep_pat.subn("", s, count=1)
            if n != 1:
                raise RuntimeError(f"{path}: NPC_REPORT block not matched for npc {npc}")
            s = s2.replace("</transitions>", block + "</transitions>", 1)
        ET.fromstring(s)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change: {path}")
        path.write_text(s, encoding="utf-8")
    print(f"patched files: {len(edits)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

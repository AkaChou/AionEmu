#!/usr/bin/env python3
"""check_user_item_ok 确认页批：39 成功分支显示 CHECK_USER_ITEM_OK，确认页按钮接管后续。

Batch: make the 39 success branch SHOW CHECK_USER_ITEM_OK (client-proven confirmation page)
and wire the ok page's own button:
  finish  : ok button FINISH_DIALOG -> close dialog (reward claim via existing preview)
  reward  : ok button SELECT_QUEST_REWARD -> open reward window 1
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
sys.path.insert(0, str(ROOT / ".agent/summary/quest-load-fail"))
from fix_notask_pages import client_model, quest_xml_path  # noqa: E402

AUDIT = ROOT / ".agent/summary/quest-load-fail/quest-order-audit-current.csv"
MANIFEST = ROOT / ".agent/summary/quest-load-fail/okpage-manifest.csv"

FINISH_TMPL = (
    '\n    <!-- 客户端 check_user_item_ok 确认页：上交成功后显示确认，按钮结束对话；'
    '领奖经 REWARD 态 preview/QUEST_SELECT 打开。 -->\n'
    '    <transition source="{src}" target="{src}">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="FINISH_DIALOG"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <close-dialog/>\n'
    '      </after-commit>\n'
    '    </transition>')
REWARD_TMPL = (
    '\n    <!-- 客户端 check_user_item_ok 确认页：上交成功后显示确认，按钮 SELECT_QUEST_REWARD '
    '打开奖励窗口。 -->\n'
    '    <transition source="{src}" target="{src}">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SELECT_QUEST_REWARD"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>\n'
    '      </after-commit>\n'
    '    </transition>')


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def main() -> int:
    args = sys.argv[1:]
    write = "--write" in args
    pages, actions = client_model()
    rows = read_csv(AUDIT)
    # 每任务信息：ok 页按钮 + 39 成功/兜底路由所在 (npc, source)
    targets: dict[int, dict] = {}
    for r in rows:
        if r["audit_status"] != "CLIENT_PAGE_UNREACHED":
            continue
        m = re.match(r"^([^#]+)#(\S+?) page-order", r["evidence_source"])
        if not m or m.group(2) != "check_user_item_ok":
            continue
        q = int(r["quest_id"])
        btns = actions.get(q, {}).get(10000, set())
        if not btns:
            continue
        targets.setdefault(q, {"buttons": btns})

    edits: dict[Path, list[dict]] = defaultdict(list)
    manifest_rows = []
    for q, info in sorted(targets.items()):
        buttons = info["buttons"]
        if buttons == {"FINISH_DIALOG"}:
            family = "finish"
        elif buttons == {"SELECT_QUEST_REWARD"}:
            family = "reward"
        else:
            manifest_rows.append({"quest_id": q, "family": f"skip: buttons={'+'.join(sorted(buttons))}"})
            continue
        path = quest_xml_path(q)
        root = ET.parse(path).getroot()
        # 39 路由的 (npc, source)：取 CHECK_USER_HAS_QUEST_ITEM 事件
        routes = []
        for t in root.findall("./transitions/transition"):
            for e in t.findall("./event/dialog"):
                if e.get("action") == "CHECK_USER_HAS_QUEST_ITEM":
                    routes.append((t.get("source"), e.get("npc-id"), t.get("priority")))
        if not routes:
            manifest_rows.append({"quest_id": q, "family": "skip: no explicit 39 route"})
            continue
        # 成功分支 = 有 has-item 条件的那条（或唯一一条）；其 source/npc 用于新路由
        success = next((r for r in routes if r[2] == "0"), routes[0])
        src, npc = success[0], success[1]
        # 幂等：已有该 source 的 ok 页确认路由则跳过
        exists = any(
            t.get("source") == src
            and any(e.get("npc-id") == npc and e.get("action") == ("FINISH_DIALOG" if family == "finish"
                                                                  else "SELECT_QUEST_REWARD")
                    for e in t.findall("./event/dialog"))
            for t in root.findall("./transitions/transition"))
        if exists:
            manifest_rows.append({"quest_id": q, "family": "skip: confirmation route exists"})
            continue
        # 成功分支必须显示 ok 页
        ok_shown = any(
            t.get("source") == src and int(t.get("priority", -1)) == 0
            and any(e.get("npc-id") == npc and e.get("action") == "CHECK_USER_HAS_QUEST_ITEM"
                    for e in t.findall("./event/dialog"))
            and any(a.get("page") == "CHECK_USER_ITEM_OK" for a in t.findall("./after-commit/dialog"))
            for t in root.findall("./transitions/transition"))
        info.update({"family": family, "npc": npc, "src": src, "ok_shown": ok_shown})
        manifest_rows.append({"quest_id": q, "family": f"add-{family}", "npc": npc, "src": src,
                              "ok_shown": ok_shown})
        edits[path].append(info)

    with MANIFEST.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["quest_id", "family", "npc", "src", "ok_shown"])
        writer.writeheader()
        writer.writerows(manifest_rows)
    adds = sum(1 for r2 in manifest_rows if r2["family"].startswith("add"))
    print(f"ok-page batch: adds={adds} files={len(edits)} manifest={MANIFEST}")
    if not write:
        return 0

    for path, items in edits.items():
        before = path.read_bytes()
        s = before.decode("utf-8")
        root = ET.fromstring(s)
        for item in items:
            npc, src, family = item["npc"], item["src"], item["family"]
            if not item["ok_shown"]:
                # 在 39 成功分支 after-commit 的 sync 后插入 ok 页显示
                for t in root.findall("./transitions/transition"):
                    if t.get("source") != src or t.get("priority") != "0":
                        continue
                    if not any(e.get("npc-id") == npc and e.get("action") == "CHECK_USER_HAS_QUEST_ITEM"
                               for e in t.findall("./event/dialog")):
                        continue
                    after = t.find("./after-commit")
                    if after is None or any(a.get("page") == "CHECK_USER_ITEM_OK"
                                            for a in after.findall("./dialog")):
                        continue
                    # 原始文本块替换：在该 transition 的 after-commit 第一个 sync 元素后插入
                    block = ET.tostring(t, encoding="unicode")
                    new_block = block.replace(
                        "<after-commit>",
                        '<after-commit>\n        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>',
                        1)
                    old_text = s
                    # 用序列化块在原文中近似定位失败时退化为字符串模板查找
                    marker = f'<dialog type="TALK_TO_NPC" npc-id="{npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>'
                    midx = s.find(marker)
                    if midx < 0:
                        continue
                    tstart = s.rfind("<transition", 0, midx)
                    tend = s.find("</transition>", midx) + len("</transition>")
                    tb = s[tstart:tend]
                    if 'page="CHECK_USER_ITEM_OK"' in tb or "<after-commit>" not in tb:
                        continue
                    nb = tb.replace("<after-commit>",
                                    '<after-commit>\n        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>',
                                    1)
                    s = s[:tstart] + nb + s[tend:]
                    root = ET.fromstring(s)
                    break
            tmpl = FINISH_TMPL if family == "finish" else REWARD_TMPL
            s = s.replace("</transitions>",
                          tmpl.format(src=src, npc=npc) + "</transitions>", 1)
        ET.fromstring(s)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change: {path}")
        path.write_text(s, encoding="utf-8")
    print(f"patched files: {len(edits)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

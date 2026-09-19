#!/usr/bin/env python3
"""按 handover 计划把“1008 本地关闭确认页”换成同 NPC 的续接页。

Applies the hand-over plan: replaces the client-local-close confirmation page
(page CHECK_USER_ITEM_OK) with the same-NPC continuation page that the target node
already shows when the dialogue is re-opened.

用法 / Usage:
  python3 .agents/summary/quest-10501-handover-continuation/apply_handover_fix.py <plan.tsv> [--check]

  <plan.tsv> 由 HandoverFixPlan 生成（列: quest source target npc dialog dialog_action to_page to_page_name）。
  --check 只校验，不写文件。
"""
from __future__ import annotations

import os
import re
import subprocess
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
XML_DIR = os.path.join(REPO, "src", "main", "resources", "aion", "data", "static_data",
                       "quest_definition", "quests")
OK_PAGE = "CHECK_USER_ITEM_OK"
TRANSITION_RE = re.compile(r"<transition\b[^>]*>.*?</transition>", re.S)


def attribute(tag: str, name: str) -> str | None:
    match = re.search(rf'\b{name}="([^"]*)"', tag)
    return match.group(1) if match else None


def plan_rows(path: str) -> list[dict[str, str]]:
    rows = []
    for line in open(path, encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        fields = line.rstrip("\n").split("\t")
        if len(fields) != 8 or not fields[0].isdigit():
            continue
        rows.append(dict(zip(
            ["quest", "source", "target", "npc", "dialog", "dialog_action", "to_page", "to_page_name"], fields)))
    return rows


def main() -> int:
    check_only = "--check" in sys.argv
    plan_file = [arg for arg in sys.argv[1:] if not arg.startswith("--")][0]
    rows = plan_rows(plan_file)
    applied = 0
    already = 0
    touched: dict[str, int] = {}
    for row in rows:
        path = os.path.join(XML_DIR, row["quest"] + ".xml")
        text = open(path, encoding="utf-8").read()
        matched = []
        for block in TRANSITION_RE.finditer(text):
            body = block.group(0)
            opening = body[:body.index(">") + 1]
            if attribute(opening, "source") != row["source"] or attribute(opening, "target") != row["target"]:
                continue
            event = re.search(r"<event>.*?</event>", body, re.S)
            if event is None:
                continue
            event_dialog = re.search(r"<dialog\b[^>]*>", event.group(0))
            if event_dialog is None:
                continue
            tag = event_dialog.group(0)
            if attribute(tag, "npc-id") != row["npc"] or attribute(tag, "action") != row["dialog_action"]:
                continue
            matched.append(block)
        ok_blocks = [block for block in matched if f'page="{OK_PAGE}"' in block.group(0)]
        done_blocks = [block for block in matched if f'page="{row["to_page_name"]}"' in block.group(0)
                       and f'page="{OK_PAGE}"' not in block.group(0)]
        if not ok_blocks:
            if done_blocks:
                already += 1
                continue
            raise SystemExit(f"quest {row['quest']}: no transition block matches {row}")
        for block in ok_blocks:
            body = block.group(0)
            if body.count(f'page="{OK_PAGE}"') != 1:
                raise SystemExit(f"quest {row['quest']}: ambiguous confirmation page count in {row['source']}->{row['target']}")
            patched = body.replace(f'page="{OK_PAGE}"', f'page="{row["to_page_name"]}"', 1)
            text = text.replace(body, patched, 1)
            applied += 1
        touched[row["quest"]] = touched.get(row["quest"], 0) + len(ok_blocks)
        if not check_only:
            open(path, "w", encoding="utf-8").write(text)
    print(f"PLAN_ROWS={len(rows)} APPLIED={applied} ALREADY={already} FILES={len(touched)}")
    if not check_only:
        for quest in sorted(touched):
            path = os.path.join(XML_DIR, quest + ".xml")
            subprocess.run(["xmllint", "--noout", path], check=True)
        print("XMLLINT_OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

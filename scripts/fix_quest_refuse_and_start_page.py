#!/usr/bin/env python3
"""Fix quest XML: start-page mismatches and missing refuse routes.

Evidence chain:
- quest-dialog-pages.csv / quest-dialog-action-details.csv: authoritative client page order and buttons.
- quest-order-audit.csv: CLIENT_PAGE_UNREACHED rows mark pages the compiled IR never emits.

Fix A (start-page): client first page is select_none/select1; XML start-page says the opposite.
Fix B (refuse): XML has no NPC_START block, client ask_quest_accept page carries refuse buttons,
  but the IR has no refuse route. Adds the refuse self-loop transitions (template: 28641.xml).
"""
from __future__ import annotations

import argparse
import csv
import re
from pathlib import Path

DEFAULT_PAGES = "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"
DEFAULT_DETAILS = "docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"
DEFAULT_AUDIT = "docs/quest/client-dialog-mapping/quest-order-audit.csv"
DEFAULT_QUEST_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"

REFUSE_TEMPLATE = """    <transition source="{source}" target="{source}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" actions="{actions}"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
"""


def load_audit_targets(audit_csv: Path, page_index: Path) -> tuple[set[int], set[int]]:
    """Returns (refuse_missing_quests, success_missing_quests) from UNREACHED rows."""
    page_names = {}
    for row in csv.DictReader(open(page_index, encoding="utf-8-sig")):
        if row["source_variant"] == "active":
            page_names[int(row["page_id"])] = row["html_page_name"]
    refuse, success = set(), set()
    for row in csv.DictReader(open(audit_csv, encoding="utf-8-sig")):
        if row["audit_status"] != "CLIENT_PAGE_UNREACHED":
            continue
        name = page_names.get(int(row["shown_page"]))
        if name == "quest_refuse_1":
            refuse.add(int(row["quest_id"]))
        elif name == "select_success":
            success.add(int(row["quest_id"]))
    return refuse, success


def client_first_page(details_csv: Path, pages_csv: Path) -> dict[int, str]:
    """quest_id -> first page name in client reading order."""
    order = {}
    for row in csv.DictReader(open(pages_csv, encoding="utf-8-sig")):
        if row["source_variant"] != "active":
            continue
        qid = int(row["quest_id"])
        key = (int(row["page_order"]), row["html_page_name"])
        if qid not in order or key[0] < order[qid][0]:
            order[qid] = key
    return {qid: name for qid, (_, name) in order.items()}


def client_refuse_actions(details_csv: Path) -> dict[int, set[str]]:
    """quest_id -> refuse action constants visible on the client accept window."""
    result: dict[int, set[str]] = {}
    for row in csv.DictReader(open(details_csv, encoding="utf-8-sig")):
        if row["source_variant"] != "active":
            continue
        if "REFUSE" not in row["action_constant"]:
            continue
        result.setdefault(int(row["quest_id"]), set()).add(row["action_constant"])
    return result


def fix_a(xml_path: Path, old: str, new: str) -> bool:
    text = xml_path.read_text(encoding="utf-8")
    pattern = f'start-page="{old}"'
    if pattern not in text:
        return False
    xml_path.write_text(text.replace(pattern, f'start-page="{new}"'), encoding="utf-8")
    return True


def fix_b(xml_path: Path, npc_id: str, source: str, actions: list[str]) -> bool:
    text = xml_path.read_text(encoding="utf-8")
    if "<dialog type=\"TALK_TO_NPC\"" not in text:
        return False
    block = REFUSE_TEMPLATE.format(source=source, npc=npc_id, actions=" ".join(actions))
    marker = "  </transitions>"
    if marker not in text:
        return False
    text = text.replace(marker, block + marker, 1)
    xml_path.write_text(text, encoding="utf-8")
    return True


def fix_b_merged(xml_path: Path) -> bool:
    """Split a merged refuse route: REFUSE_1 shows the refuse page, the rest close the dialog.

    Matches the expandNpcStart template (QuestXmlBlockExpander lines 186-191).
    """
    text = xml_path.read_text(encoding="utf-8")
    pattern = re.compile(
        r'<dialog type="TALK_TO_NPC" npc-id="(\d+)" actions="([^"]*QUEST_REFUSE[^"]*)"',
    )
    match = pattern.search(text)
    if not match:
        return False
    npc_id, actions = match.group(1), match.group(2)
    action_list = actions.split()
    if len(action_list) < 2 or "QUEST_REFUSE_1" not in action_list:
        return False
    rest = [a for a in action_list if a != "QUEST_REFUSE_1"]
    if not rest:
        return False
    # Replace the merged transition block (event + close after-commit) with split routes.
    old_block = re.compile(
        r'<transition source="([^"]+)" target="\1">\s*<event>\s*'
        + re.escape(match.group(0)) + r'\s*/>\s*</event>\s*<after-commit>\s*<close-dialog/>\s*</after-commit>\s*</transition>',
        re.S,
    )
    new_block = (
        '<transition source="\\1" target="\\1">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="QUEST_REFUSE_1"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        '        <dialog type="SHOW_QUEST_PAGE" page="QUEST_REFUSE_1"/>\n'
        '      </after-commit>\n'
        '    </transition>\n'
        '    <transition source="\\1" target="\\1">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" actions="{" ".join(rest)}"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        '        <close-dialog/>\n'
        '      </after-commit>\n'
        '    </transition>'
    )
    new_text, count = old_block.subn(new_block, text, count=1)
    if count == 0:
        return False
    xml_path.write_text(new_text, encoding="utf-8")
    return True


def fix_d(xml_path: Path, subpage: str) -> bool:
    """Add the missing detail subpage route.

    Client menu page X shows a button SELECT{n}_1; the IR emits X but has no route that opens
    X_1. Adds a self-loop per distinct source node that already shows X.

    Template matches expandNpcStart lines 170-172:
      SELECT{n}_1 -> ShowQuestDialog(SELECT{n}_1)
    """
    menu = re.sub(r"_1$", "", subpage)
    text = xml_path.read_text(encoding="utf-8")
    action = subpage.upper()
    if f'action="{action}"' in text:
        return False  # route already exists
    if f'<dialog type="SHOW_QUEST_PAGE" page="{menu.upper()}"/>' not in text:
        return False  # the menu page itself is never emitted; nothing to chain the subpage to
    # transitions that emit the menu page: capture source + npc
    menu_route = re.compile(
        r'<transition source="([^"]+)"(?: target="[^"]+")?[^>]*>\s*<event>\s*'
        r'<dialog type="TALK_TO_NPC" npc-id="(\d+)"[^>]*/>\s*</event>\s*'
        r'(?:<conditions>.*?</conditions>\s*)?'
        r'(?:<actions>.*?</actions>\s*)?'
        r'<after-commit>.*?<dialog type="SHOW_QUEST_PAGE" page="'
        + menu.upper() + r'"/>.*?</after-commit>\s*</transition>',
        re.S,
    )
    additions = []
    seen = set()
    for m in menu_route.finditer(text):
        source, npc_id = m.group(1), m.group(2)
        key = (source, npc_id)
        if key in seen:
            continue
        seen.add(key)
        additions.append(
            f'    <transition source="{source}" target="{source}">\n'
            f'      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="{action}"/>\n'
            f'      </event>\n'
            f'      <after-commit>\n'
            f'        <dialog type="SHOW_QUEST_PAGE" page="{subpage.upper()}"/>\n'
            f'      </after-commit>\n'
            f'    </transition>\n'
        )
    if not additions:
        return False
    marker = "  </transitions>"
    if marker not in text:
        return False
    text = text.replace(marker, "".join(additions) + marker, 1)
    xml_path.write_text(text, encoding="utf-8")
    return True


def npc_id_for_quest(xml_path: Path) -> str:
    """First TALK_TO_NPC npc id: the acquisition NPC owns the refuse route."""
    text = xml_path.read_text(encoding="utf-8")
    match = re.search(r'<dialog type="TALK_TO_NPC" npc-id="(\d+)"', text)
    if not match:
        match = re.search(r'<dialog type="NPC_START" npc-id="(\d+)"', text)
    return match.group(1) if match else ""


def page_names_from(page_index: Path) -> dict[int, str]:
    result = {}
    for row in csv.DictReader(open(page_index, encoding="utf-8-sig")):
        if row["source_variant"] == "active":
            result[int(row["page_id"])] = row["html_page_name"]
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pages", type=Path, default=Path(DEFAULT_PAGES))
    parser.add_argument("--details", type=Path, default=Path(DEFAULT_DETAILS))
    parser.add_argument("--audit", type=Path, default=Path(DEFAULT_AUDIT))
    parser.add_argument("--quest-dir", type=Path, default=Path(DEFAULT_QUEST_DIR))
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    refuse, success = load_audit_targets(args.audit, args.pages)
    first = client_first_page(args.details, args.pages)
    refuse_actions = client_refuse_actions(args.details)
    print(f"refuse 缺失: {len(refuse)}, success 缺失: {len(success)}")

    fixed_a = []
    fixed_b = []
    fixed_c = []
    fixed_d = []
    skipped_b = []
    for qid in sorted(refuse):
        path = args.quest_dir / f"{qid}.xml"
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        # Fix A: start-page mismatch on the client first page
        client_first = first.get(qid, "")
        if "NPC_START" in text:
            if client_first == "select_none" and 'start-page="SELECT1"' in text:
                if not args.dry_run:
                    fix_a(path, "SELECT1", "SELECT_NONE")
                fixed_a.append((qid, "SELECT1->SELECT_NONE"))
            elif client_first == "select1" and 'start-page="SELECT_NONE"' in text:
                if not args.dry_run:
                    fix_a(path, "SELECT_NONE", "SELECT1")
                fixed_a.append((qid, "SELECT_NONE->SELECT1"))
            continue
        # Fix C: merged refuse route -> split REFUSE_1 (shows refuse page) from the close actions
        if "QUEST_REFUSE" in text:
            if not args.dry_run:
                if fix_b_merged(path):
                    fixed_c.append(qid)
            else:
                import re as _re
                if _re.search(r'actions="[^"]*QUEST_REFUSE_1[^"]* [^"]+"', text):
                    fixed_c.append(qid)
            continue
        # Fix B: no NPC_START -> add refuse self-loop if client shows refuse buttons
        actions = refuse_actions.get(qid)
        if not actions:
            skipped_b.append((qid, "no client refuse button"))
            continue
        npc = npc_id_for_quest(path)
        if not npc:
            skipped_b.append((qid, "no NPC found"))
            continue
        action_ids = [a.replace("HACTION_", "") for a in sorted(actions)]
        if not args.dry_run:
            fix_b(path, npc, "unaccepted", action_ids)
        fixed_b.append((qid, npc, action_ids))

    # Fix D: subpage routes (select{n}_1 missing) — only after A/B/C so chain roots exist
    page_names = page_names_from(args.pages)
    subpage_targets = set()
    for r in csv.DictReader(open(args.audit, encoding="utf-8-sig")):
        if r["audit_status"] != "CLIENT_PAGE_UNREACHED":
            continue
        page_id = int(r["shown_page"])
        name = page_names.get(page_id)
        if name and re.match(r"^select\d+_1$", name):
            subpage_targets.add((int(r["quest_id"]), name.upper()))
    for qid, subpage in sorted(subpage_targets):
        path = args.quest_dir / f"{qid}.xml"
        if not path.exists():
            continue
        if not args.dry_run:
            if fix_d(path, subpage):
                fixed_d.append((qid, subpage))
        else:
            fixed_d.append((qid, subpage))

    print(f"Fix A (start-page): {len(fixed_a)}")
    for qid, change in fixed_a[:10]:
        print(f"  {qid} {change}")
    print(f"Fix B (refuse self-loop): {len(fixed_b)}")
    for qid, npc, acts in fixed_b[:8]:
        print(f"  {qid} npc={npc} actions={acts}")
    print(f"Fix C (split merged refuse): {len(fixed_c)}")
    print(f"Fix D (subpage routes): {len(fixed_d)}")
    print(f"Fix B 跳过: {len(skipped_b)}")
    for qid, reason in skipped_b[:8]:
        print(f"  {qid}: {reason}")


if __name__ == "__main__":
    main()

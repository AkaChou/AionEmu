#!/usr/bin/env python3
"""Generate executable XML chains for 71 metadata-only item_collecting quests.

Classifies each quest by its client page/button shape (docs/quest/client-dialog-mapping):
  - full      : ask_quest_accept page present, select5 button CHECK_USER_HAS_QUEST_ITEM (39)
  - simple    : select1 buttons QUEST_ACCEPT_SIMPLE/QUEST_REFUSE_SIMPLE, select5 button SIMPLE (20002)
  - selectable: reward-groups contain SELECTABLE_ITEM -> npc-complete choices
  - setpro1   : select1 has only SETPRO1 (10000) -> report happens on the start dialog

Templates follow the migrated reference quests 11024 (full), 1103/11039 (simple),
1001/13806 (selectable) and the compact NPC_START / npc-item-report / npc-complete
expander blocks used by the latest migration batch (ab42c75bf).
"""
import csv
import os
import re
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
QUESTS = os.path.join(ROOT, "src/main/resources/aion/data/static_data/quest_definition/quests")
CATALOG = os.path.join(ROOT, "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml")
PAGES_CSV = os.path.join(ROOT, "docs/quest/client-dialog-mapping/quest-dialog-pages.csv")
DETAILS_CSV = os.path.join(ROOT, "docs/quest/client-dialog-mapping/quest-dialog-action-details.csv")
CONTRACTS_CSV = os.path.join(ROOT, "docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv")

# ---------------------------------------------------------------------------
# data loading
# ---------------------------------------------------------------------------

def load_rows(path):
    with open(path, encoding="utf-8-sig") as f:
        return list(csv.DictReader(f))

contracts = defaultdict(list)
for r in load_rows(CONTRACTS_CSV):
    contracts[r["quest_id"]].append(r)

pages = defaultdict(list)
for r in load_rows(PAGES_CSV):
    if r["source_variant"] == "active" and r["page_mapping"] == "exact":
        pages[r["quest_id"]].append(r)

details = defaultdict(list)
for r in load_rows(DETAILS_CSV):
    if r["source_variant"] == "active" and r["page_mapping"] == "exact":
        details[r["quest_id"]].append(r)

def page_id_by_name(qid, name):
    for p in pages.get(qid, []):
        if p["html_page_name"] == name:
            return p["page_id"]
    return None

def page_actions(qid, page_id):
    return sorted({int(d["action_id"]) for d in details.get(qid, []) if d["page_id"] == page_id})

def classify(qid):
    """Return shape: full | simple | setpro1 | unknown, plus select1_1 flag."""
    by_name = {}
    for p in pages.get(qid, []):
        by_name.setdefault(p["html_page_name"], []).append(p)
    sel1_id = page_id_by_name(qid, "select1")
    sel1 = page_actions(qid, sel1_id) if sel1_id else []
    has_ask = "ask_quest_accept" in by_name
    if sel1 == [10000] and not has_ask:
        return "setpro1"
    if has_ask:
        return "full"
    if 20000 in sel1 or 20001 in sel1:
        return "simple"
    return "unknown"

def selectable_indices(qid, content):
    m = re.search(r"<reward-groups>(.*?)</reward-groups>", content, re.S)
    if not m:
        # rewardless quests: no fixed rewards, no selectable choices
        return [], []
    groups = re.findall(r"<group>(.*?)</group>", m.group(1), re.S)
    if len(groups) != 1:
        return [], len(groups)
    rewards = re.findall(r'<reward\s+kind="(\w+)"\s+id="(\d+)"\s+amount="(\d+)"', groups[0])
    fixed = [i for i, (kind, _, _) in enumerate(rewards) if kind != "SELECTABLE_ITEM"]
    sel = [i for i, (kind, _, _) in enumerate(rewards) if kind == "SELECTABLE_ITEM"]
    return fixed, sel

# ---------------------------------------------------------------------------
# XML builders
# ---------------------------------------------------------------------------

I = "  "
def esc(v):
    return v

def bit_field():
    return ('  <progress>\n'
            '    <bit-field name="var0" offset="0" width="1" min="0" max="1" persistence="PERSISTENT" scope="LOCAL"/>\n'
            '  </progress>\n')

def nodes():
    return ('  <nodes>\n'
            '    <node label="unaccepted" status="NONE">\n'
            '      <var name="var0" value="0"/>\n'
            '    </node>\n'
            '    <node label="started" status="START">\n'
            '      <var name="var0" value="0"/>\n'
            '    </node>\n'
            '    <node label="reward" status="REWARD">\n'
            '      <var name="var0" value="1"/>\n'
            '    </node>\n'
            '    <node label="complete" status="COMPLETE">\n'
            '      <var name="var0" value="0"/>\n'
            '    </node>\n'
            '  </nodes>\n')

def npc_start(npc):
    return f'    <dialog type="NPC_START" npc-id="{npc}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1"/>\n'

def quest_select(npc, page):
    return (f'    <transition source="started" target="started">\n'
            f'      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>\n'
            f'      </event>\n'
            f'      <after-commit>\n'
            f'        <dialog type="SHOW_QUEST_PAGE" page="{page}"/>\n'
            f'      </after-commit>\n'
            f'    </transition>\n')

def npc_item_report(npc, item_id, count):
    return f'    <npc-item-report npc-id="{npc}" source="started" target="reward" item-id="{item_id}" required="{count}"/>\n'

def manual_item_check(npc, action, items, fail_page=None):
    """Manual ok/fail routes for multi-item quests (1870 form)."""
    conds = "\n".join(f'        <has-item item-id="{iid}" count="{cnt}"/>' for iid, cnt in items)
    removes = "\n".join(f'        <remove-item item-id="{iid}" count="{cnt}"/>' for iid, cnt in items)
    if fail_page:
        fail = f'        <dialog type="SHOW_QUEST_PAGE" page="{fail_page}"/>\n'
    else:
        fail = f'        <close-dialog/>\n'
    return (f'    <transition source="started" target="reward" priority="0">\n'
            f'      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="{action}"/>\n'
            f'      </event>\n'
            f'      <conditions>\n{conds}\n'
            f'      </conditions>\n'
            f'      <actions>\n{removes}\n'
            f'      </actions>\n'
            f'      <after-commit>\n'
            f'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
            f'        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>\n'
            f'      </after-commit>\n'
            f'    </transition>\n'
            f'    <transition source="started" target="started" priority="1">\n'
            f'      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="{action}"/>\n'
            f'      </event>\n'
            f'      <after-commit>\n{fail}'
            f'      </after-commit>\n'
            f'    </transition>\n')

def setpro1_report(npc, item_id, count):
    # NPC_START does not generate a SETPRO1 route; shape-A quests start via the
    # SETPRO1 button on select1, so the accept route must be spelled out (11024 form).
    return (f'    <transition source="unaccepted" target="started">\n'
            f'      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SETPRO1"/>\n'
            f'      </event>\n'
            f'      <conditions>\n'
            f'        <start-eligible/>\n'
            f'      </conditions>\n'
            f'      <after-commit>\n'
            f'        <sync-quest-state mode="VISIBILITY_REFRESH"/>\n'
            f'        <close-dialog/>\n'
            f'      </after-commit>\n'
            f'    </transition>\n'
            f'    <transition source="started" target="reward" priority="0">\n'
            f'      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SETPRO1"/>\n'
            f'      </event>\n'
            f'      <conditions>\n'
            f'        <has-item item-id="{item_id}" count="{count}"/>\n'
            f'      </conditions>\n'
            f'      <actions>\n'
            f'        <remove-item item-id="{item_id}" count="{count}"/>\n'
            f'      </actions>\n'
            f'      <after-commit>\n'
            f'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
            f'        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>\n'
            f'      </after-commit>\n'
            f'    </transition>\n'
            f'    <transition source="started" target="started" priority="1">\n'
            f'      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SETPRO1"/>\n'
            f'      </event>\n'
            f'      <after-commit>\n'
            f'        <dialog type="SHOW_QUEST_PAGE" page="QUEST_FAILED_1"/>\n'
            f'      </after-commit>\n'
            f'    </transition>\n')

def set_succeed(npc):
    return (f'    <transition source="started" target="reward">\n'
            f'      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SET_SUCCEED"/>\n'
            f'      </event>\n'
            f'      <after-commit>\n'
            f'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
            f'        <close-dialog/>\n'
            f'      </after-commit>\n'
            f'    </transition>\n')

def npc_complete(npc, fixed_indices, selectable):
    fixed_attr = f' fixed-reward-indices="{" ".join(map(str, fixed_indices))}"' if fixed_indices else ""
    if selectable:
        choices = []
        for i, sel_idx in enumerate(selectable, start=1):
            choices.append(f'      <choice action="SELECTED_QUEST_REWARD{i}" reward-index="{sel_idx}"/>')
        choice_block = "\n".join(choices) + "\n"
        actions_attr = ""
    else:
        choice_block = ""
        actions_attr = ' actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD"'
    return (f'    <npc-complete npc-id="{npc}" source="reward" target="complete"{fixed_attr}'
            f' complete-reward-index="0" finish="SELECTION_DIALOG"{actions_attr}>\n'
            f'{choice_block}'
            f'      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>\n'
            f'    </npc-complete>\n')

# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------

def build(qid):
    path = os.path.join(QUESTS, qid + ".xml")
    content = open(path, encoding="utf-8").read()
    # Re-running is safe: metadata is preserved verbatim, transitions are deterministic.

    rows = contracts.get(qid, [])
    if not rows:
        raise SystemExit(f"{qid}: no contract row")
    row = rows[0]
    npc = row["start_npc_ids"].strip().split()[0]

    items_block = re.search(r"<items>(.*?)</items>", content, re.S).group(1)
    items = re.findall(r'<item\s+id="(\d+)"\s+count="(\d+)"', items_block)
    if not items:
        raise SystemExit(f"{qid}: no items")

    shape = classify(qid)
    fixed, selectable = selectable_indices(qid, content)
    if isinstance(selectable, int):
        raise SystemExit(f"{qid}: unsupported multi-group reward layout ({selectable} groups)")

    t = []
    t.append(npc_start(npc))

    if shape == "setpro1":
        # report happens on the start dialog page (select1 has only SETPRO1)
        t.append(quest_select(npc, "SELECT1"))
        t.append(setpro1_report(npc, items[0][0], items[0][1]))
        t.append(set_succeed(npc))
    else:
        t.append(quest_select(npc, "SELECT5"))
        if len(items) == 1:
            t.append(npc_item_report(npc, items[0][0], items[0][1]))
        else:
            action = "CHECK_USER_HAS_QUEST_ITEM_SIMPLE" if shape == "simple" else "CHECK_USER_HAS_QUEST_ITEM"
            fail_page = None if shape == "simple" else "SELECT6"
            t.append(manual_item_check(npc, action, items, fail_page=fail_page))
        t.append(set_succeed(npc))

    t.append(npc_complete(npc, fixed, selectable))
    transitions = "<transitions>\n" + "".join(t) + "  </transitions>\n"

    # rebuild the file: metadata verbatim, then progress/nodes/transitions
    m = re.search(r"<metadata.*?</metadata>", content, re.S)
    if not m:
        raise SystemExit(f"{qid}: no metadata block")
    metadata = m.group(0)
    out = ('<?xml version="1.0" encoding="UTF-8"?>\n'
           f'<quest-definition id="{qid}" version="1">\n'
           f'{metadata}\n'
           f'{bit_field()}'
           f'{nodes()}'
           f'{transitions}'
           f'</quest-definition>\n')
    with open(path, "w", encoding="utf-8") as f:
        f.write(out)
    return shape


def main():
    targets = ["11152","11268","11269","11270","11271","11272","11273","11274","1554","1685","1848","19049",
               "19079","19080","19081","21268","21269","21270","21271","21272","21275","21276","29049","29079",
               "29080","29081","3117","3119","3548","4116","4118","50035","50036","50037","50044","50045","50046",
               "50049","50050","50051","51035","51036","51037","51044","51045","51046","51049","51050","51051",
               "80007","80017","80019","80136","80555","80559","80561","80562","80563","80564","80614","80716",
               "80717","80718","80720","80738","80762","80767","9562","9563","9564","9565"]
    counts = defaultdict(int)
    for qid in targets:
        shape = build(qid)
        counts[shape] += 1
        print(f"{qid}: {shape}")
    print("shapes:", dict(counts))

    # catalog flip
    with open(CATALOG, encoding="utf-8") as f:
        catalog = f.read()
    for qid in targets:
        pattern = f'<definition id="{qid}" resource="aion/data/static_data/quest_definition/quests/{qid}.xml" mode="METADATA_ONLY" />'
        new = pattern.replace('mode="METADATA_ONLY"', 'mode="EXECUTABLE"')
        if pattern not in catalog:
            raise SystemExit(f"catalog row not found for {qid}")
        catalog = catalog.replace(pattern, new)
    with open(CATALOG, "w", encoding="utf-8") as f:
        f.write(catalog)
    print("catalog updated")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""近似核查 BT (world 300170000) 相关 NPC 的 retail pattern 静态 supports 判定。

Audit helper for the Macunbello conditional-spawn repair. Parses each compact
npc_ai_pattern block with ElementTree, collects the event tags, plus the direct
child tags of every <conditions>/<actions> rule, and diffs them against the
SUPPORTED_* sets parsed out of RetailPatternAI2.java. Parameter-level checks
inside supportsAction are NOT reproduced - structural whitelist approximation.

用法 / Usage:
    python3 pattern_support_audit.py [repo_root]
"""

import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

NPCS = [216206, 216207, 216208, 216209, 216210, 216211, 216212, 216213,
        216583, 216245, 216164, 216733, 216734, 216735, 216736, 216737, 216738,
        281696, 281759, 281760]


def java_set(source, name):
    match = re.search(r'Set<String> ' + name + r' = Set\.of\((.*?)\);', source, re.S)
    return set(re.findall(r'"([^"]+)"', match.group(1))) if match else None


def audit_pattern(block, events_ok, actions_ok, conditions_ok, categories_ok, target_events):
    root = ET.fromstring(block)
    handlers = root.find("event_handlers")
    events, actions, conditions, categories, event_target_uses = set(), set(), set(), set(), []
    if handlers is None:
        return events, actions, conditions, categories, event_target_uses
    for event in handlers:
        events.add(event.tag)
        for rule in event.findall("pattern"):
            category = rule.findtext("action_category", "").strip()
            if category:
                categories.add(category)
            conds = rule.find("conditions")
            if conds is not None:
                conditions.update(child.tag for child in conds)
            acts = rule.find("actions")
            if acts is not None:
                actions.update(child.tag for child in acts)
                for node in acts.iter():
                    if (node.text or "").strip() == "OBJI_EVENT_TARGET" and event.tag not in target_events:
                        event_target_uses.append((event.tag, node.tag))
    return events, actions, conditions, categories, event_target_uses


def main(root="."):
    ai_dir = os.path.join(root, "src/main/resources/aion/definitions/compact/ai")
    java = open(os.path.join(root, "src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java"),
                encoding="utf-8").read()
    events_ok = java_set(java, "SUPPORTED_EVENTS")
    actions_ok = java_set(java, "SUPPORTED_ACTIONS")
    conditions_ok = java_set(java, "SUPPORTED_CONDITIONS")
    categories_ok = java_set(java, "SUPPORTED_RULE_CATEGORIES")
    target_events = java_set(java, "TARGET_EVENTS")
    print(f"whitelist sizes: events={len(events_ok)} actions={len(actions_ok)} "
          f"conditions={len(conditions_ok)} categories={len(categories_ok)}")

    npc_ai = {}
    for path in glob.glob(os.path.join(ai_dir, "npc-ai-parts", "*.xml")):
        text = open(path, encoding="utf-8").read()
        for match in re.finditer(r'<npc id="(\d+)"[^>]*?\bai="([^"]+)"', text):
            npc_ai[int(match.group(1))] = match.group(2)

    pattern_files = {}
    for path in sorted(glob.glob(os.path.join(ai_dir, "npcaipatterns*.xml"))):
        text = open(path, encoding="utf-8").read()
        for match in re.finditer(r'<npc_ai_pattern><name>([^<]+)</name>', text):
            pattern_files.setdefault(match.group(1).lower(), (path, match.start()))

    audited, failures = set(), 0
    for npc in NPCS:
        ai = npc_ai.get(npc, "?")
        located = pattern_files.get(ai.lower())
        if located is None:
            print(f"npc {npc}: ai={ai} -> PATTERN NOT FOUND")
            failures += 1
            continue
        if ai.lower() in audited:
            continue
        audited.add(ai.lower())
        path, start = located
        text = open(path, encoding="utf-8").read()
        end = text.find("</npc_ai_pattern>", start)
        block = text[start:end + len("</npc_ai_pattern>") if end >= 0 else len(text)]

        events, actions, conditions, categories, event_target_uses = audit_pattern(
            block, events_ok, actions_ok, conditions_ok, categories_ok, target_events)

        unknown_events = sorted(events - events_ok)
        unknown_actions = sorted(actions - actions_ok)
        unknown_conditions = sorted(conditions - conditions_ok)
        unknown_categories = sorted(categories - categories_ok)
        print(f"\nnpc {npc}: ai={ai} file={os.path.basename(path)}")
        print(f"  events     = {sorted(events)}")
        print(f"  actions    = {sorted(actions)}")
        print(f"  conditions = {sorted(conditions)}")
        if unknown_events:
            failures += 1
            print(f"  !! UNSUPPORTED events: {unknown_events}")
        if unknown_actions:
            failures += 1
            print(f"  !! UNSUPPORTED actions: {unknown_actions}")
        if unknown_conditions:
            failures += 1
            print(f"  !! UNSUPPORTED conditions: {unknown_conditions}")
        if unknown_categories:
            failures += 1
            print(f"  !! UNSUPPORTED categories: {unknown_categories}")
        if event_target_uses:
            failures += 1
            print(f"  !! event-target use on non-target events: {event_target_uses}")
        if not (unknown_events or unknown_actions or unknown_conditions or unknown_categories
                or event_target_uses):
            print("  -> structural whitelist OK")

    print(f"\nfan-out: {len(audited)} patterns audited, {failures} problem(s)")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")

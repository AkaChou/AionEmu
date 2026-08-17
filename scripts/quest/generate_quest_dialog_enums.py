#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import xml.etree.ElementTree as ET
from pathlib import Path

from quest_dialog_symbols import LEGACY_ACTION_ALIASES, action_expression, attributes, load_maps


TAG_PATTERN = re.compile(r"<(?:dialog|talk-to-npc|quest-dialog|show-quest-dialog|show-quest-selection-dialog|npc-start|npc-report|npc-complete|choice|preview|fallback|category|equipment|reward-group)\b[^>]*>")
STANDARD_ACTIONS = {
    "QUEST_SELECT", "SELECT1_1", "ASK_QUEST_ACCEPT", "QUEST_ACCEPT_1", "QUEST_ACCEPT_SIMPLE",
    "QUEST_REFUSE_1", "QUEST_REFUSE_2", "QUEST_REFUSE_SIMPLE", "FINISH_DIALOG",
    "SELECT_QUEST_REWARD", "CHECK_USER_HAS_QUEST_ITEM", "CHECK_USER_HAS_QUEST_ITEM_SIMPLE",
}
STANDARD_PAGES = {
    "SELECT1", "SELECT1_1", "SHOW_ASK_QUEST_ACCEPT_WINDOW", "QUEST_ACCEPT_1", "QUEST_REFUSE_1",
    "SELECT_QUEST", "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT2", "SELECT5", "SELECT6",
    "DEFAULT_SUCCESS", "CHECK_USER_ITEM_OK", "CHECK_USER_ITEM_FAIL",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate typed quest dialog enums from client data and XML references.")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--check", action="store_true", help="Fail when generated sources differ; do not write.")
    return parser.parse_args()


def collect(
    root: Path,
    actions_by_id: dict[int, str],
    actions_by_name: dict[str, int],
    pages_by_id: dict[int, str],
) -> tuple[set[str], set[str]]:
    actions = set(STANDARD_ACTIONS)
    pages = set(STANDARD_PAGES)
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    for path in sorted(quest_dir.glob("*.xml")):
        source = path.read_text(encoding="utf-8")
        for match in TAG_PATTERN.finditer(source):
            tag = match.group(0)
            attrs = attributes(tag)
            name = tag[1:].split(None, 1)[0].rstrip(">")
            if name == "dialog":
                if attrs.get("type") in {"TALK_TO_NPC", "QUEST_ACTION"}:
                    add_symbol_expression(actions, attrs.get("action") or attrs.get("actions", ""),
                                          actions_by_id, actions_by_name)
                elif attrs.get("type") in {"SHOW_QUEST_PAGE", "SHOW_SELECTION_PAGE", "NPC_REPORT"}:
                    pages.add(attrs["page"])
                elif attrs.get("type") == "NPC_START":
                    pages.add(attrs.get("start-page", "SELECT1"))
            elif name in {"talk-to-npc", "quest-dialog"}:
                if "dialog" in attrs:
                    actions.add(LEGACY_ACTION_ALIASES[attrs["dialog"]])
                for key in ("dialog-id", "dialog-ids"):
                    if key in attrs:
                        add_symbol_expression(actions, action_expression(attrs[key], actions_by_id),
                                              actions_by_id, actions_by_name)
            elif name in {"show-quest-dialog", "show-quest-selection-dialog"}:
                pages.add(pages_by_id[int(attrs["dialog-id"])])
            elif name == "npc-start":
                pages.add(pages_by_id[int(attrs["start-dialog-id"])] if "start-dialog-id" in attrs else "SELECT1")
            elif name == "npc-report":
                pages.add(pages_by_id[int(attrs["page"])])
            elif name == "npc-complete":
                if "dialog-ids" in attrs:
                    add_symbol_expression(actions, action_expression(attrs["dialog-ids"], actions_by_id),
                                          actions_by_id, actions_by_name)
                if "preview-dialog-ids" in attrs:
                    add_symbol_expression(actions, action_expression(attrs["preview-dialog-ids"], actions_by_id),
                                          actions_by_id, actions_by_name)
                add_symbol_expression(actions, attrs.get("actions", ""), actions_by_id, actions_by_name)
            elif name in {"choice", "preview", "fallback"}:
                if "dialog-id" in attrs:
                    actions.add(actions_by_id[int(attrs["dialog-id"])])
                if "dialog-ids" in attrs:
                    add_symbol_expression(actions, action_expression(attrs["dialog-ids"], actions_by_id),
                                          actions_by_id, actions_by_name)
                add_symbol_expression(actions, attrs.get("action") or attrs.get("actions", ""),
                                      actions_by_id, actions_by_name)
            elif name in {"category", "equipment", "reward-group"} and "action" in attrs:
                actions.add(attrs["action"])
                if "page" in attrs:
                    pages.add(attrs["page"])
        add_reported_reward_symbols(source, actions, actions_by_id, actions_by_name)
    return actions, pages


def add_reported_reward_symbols(
    source: str, actions: set[str], actions_by_id: dict[int, str], actions_by_name: dict[str, int]
) -> None:
    transitions = ET.fromstring(source).find("transitions")
    if transitions is None:
        return
    mode = transitions.get("reported-reward-mode")
    if mode is None:
        return
    if mode == "FIXED":
        actions.add(actions_by_id[108])
        return
    if mode == "CLASS":
        actions.add(actions_by_id[8])
        actions.add(actions_by_id[110])
        return
    if mode != "CHOICE":
        raise ValueError(f"unknown reported reward mode {mode!r}")

    ordinary_ids: set[int] = set()
    for choice in transitions.findall(".//choice"):
        symbols: set[str] = set()
        add_symbol_expression(symbols, choice.get("action") or choice.get("actions", ""),
                              actions_by_id, actions_by_name)
        ordinary_ids.update(actions_by_name[symbol] for symbol in symbols
                            if 8 <= actions_by_name[symbol] <= 22)
    expected = list(range(8, 8 + len(ordinary_ids)))
    if sorted(ordinary_ids) != expected or len(ordinary_ids) < 2:
        raise ValueError(f"CHOICE reported reward slots must be contiguous from action 8: {sorted(ordinary_ids)}")
    for ordinary_id in expected:
        actions.add(actions_by_id[ordinary_id])
        actions.add(actions_by_id[110 + ordinary_id - 8])


def add_symbol_expression(
    target: set[str], raw: str, actions_by_id: dict[int, str], actions_by_name: dict[str, int]
) -> None:
    for token in raw.split():
        if ".." in token:
            endpoints = token.split("..")
            if len(endpoints) != 2 or not all(endpoints):
                raise ValueError(f"invalid dialog action range {token!r}")
            try:
                first, last = (actions_by_name[name] for name in endpoints)
            except KeyError as error:
                raise ValueError(f"unknown dialog action in range {token!r}") from error
            if first > last or last - first >= 256:
                raise ValueError(f"invalid dialog action range {token!r}")
            for action_id in range(first, last + 1):
                try:
                    target.add(actions_by_id[action_id])
                except KeyError as error:
                    raise ValueError(f"non-contiguous dialog action range {token!r} at {action_id}") from error
        elif token:
            target.add(token)


def enum_source(package: str, class_name: str, doc: str, symbols: set[str], by_name: dict[str, int]) -> str:
    missing = sorted(symbols - by_name.keys())
    if missing:
        raise ValueError(f"{class_name} references missing client symbols: {missing}")
    constants = sorted(((by_name[name], name) for name in symbols), key=lambda item: (item[0], item[1]))
    rows = ",\n".join(f"\t{name}({item_id})" for item_id, name in constants) + ";"
    return f"""package {package};

import java.util.Arrays;

/**
 * 由 Aion 5.8 客户端 {doc} 与活动任务 XML 引用生成。
 * Generated from Aion 5.8 client {doc} and active quest XML references.
 */
public enum {class_name} {{
{rows}

\tprivate final int id;

\t{class_name}(int id) {{
\t\tthis.id = id;
\t}}

\tpublic int id() {{
\t\treturn id;
\t}}

\tpublic static {class_name} fromId(int id) {{
\t\treturn Arrays.stream(values()).filter(value -> value.id == id).findFirst()
\t\t\t.orElseThrow(() -> new IllegalArgumentException("unknown {class_name} id " + id));
\t}}
}}
"""


def main() -> None:
    args = parse_args()
    root = args.root.resolve()
    actions_by_id, actions_by_name, pages_by_id, pages_by_name = load_maps(root)
    actions, pages = collect(root, actions_by_id, actions_by_name, pages_by_id)
    output_dir = root / "src/main/java/com/aionemu/gameserver/questEngine/definition"
    outputs = {
        output_dir / "QuestDialogAction.java": enum_source(
            "com.aionemu.gameserver.questEngine.definition", "QuestDialogAction", "HyperLinks.xml", actions, actions_by_name
        ),
        output_dir / "QuestDialogPage.java": enum_source(
            "com.aionemu.gameserver.questEngine.definition", "QuestDialogPage", "HtmlPages.xml", pages, pages_by_name
        ),
    }
    changed = [path for path, content in outputs.items() if not path.exists() or path.read_text(encoding="utf-8") != content]
    if args.check and changed:
        raise SystemExit("generated quest dialog enums are stale: " + ", ".join(str(path) for path in changed))
    for path in changed:
        path.write_text(outputs[path], encoding="utf-8")
    print(f"actions={len(actions)} pages={len(pages)} changed={len(changed)}")


if __name__ == "__main__":
    main()

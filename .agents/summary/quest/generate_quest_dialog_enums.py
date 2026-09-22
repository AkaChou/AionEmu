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
    "QUEST_1_2", "QUEST_REFUSE_4", "SELECT_QUEST", "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT2", "SELECT5",
    "SELECT6",
    "DEFAULT_SUCCESS", "CHECK_USER_ITEM_OK", "CHECK_USER_ITEM_FAIL",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate typed quest dialog enums from client data and XML references.")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[3])
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


ACTION_EXTRA_IMPLEMENTATION = """

\t/**
\t * 判断是否为客户端奖励窗口的确认动作（SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD）。
\t * Returns whether the id is a client reward-window confirmation action
\t * (SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD).
\t *
\t * <p>这些动作由全局奖励窗口发出，客户端可能携带上一个交互对象；服务端必须按
\t * questId + action 解析，不能把该对象当作完成 NPC 绑定。</p>
\t * <p>These actions come from the global reward window and may carry the previously
\t * interacted object; the server must resolve them by questId + action instead of treating
\t * that object as the completion NPC binding.</p>
\t *
\t * @param actionId 对话动作 ID / dialog action id
\t * @return 是否为奖励窗口确认动作 / whether it is a reward-window confirmation action
\t */
\tpublic static boolean isRewardWindowAction(int actionId) {
\t\treturn actionId >= SELECTED_QUEST_REWARD1.id() && actionId <= SELECTED_QUEST_NOREWARD.id();
\t}
"""

PAGE_EXTRA_IMPLEMENTATION = """

\t/**
\t * 第 N 档（0 基）奖励在客户端渲染的奖励窗口页面。
\t * Client reward-window page rendering the zero-based Nth reward tier.
\t *
\t * <p>客户端只声明 6 档奖励窗口：第 1~4 档对应页面 5..8，第 5/6 档对应页面 45/46；
\t * 超过 6 档时没有可下发页面，调用方必须显式降级或拒绝，禁止再用线性偏移猜测页面号。
\t * The client declares exactly six reward windows: tiers 1..4 map to pages 5..8 and tiers
\t * 5/6 to pages 45/46. Tiers beyond six have no client page, so callers must degrade or
\t * reject explicitly instead of extrapolating a page number.</p>
\t */
\tpublic static Optional<QuestDialogPage> rewardWindowForTier(int tier) {
\t\treturn switch (tier) {
\t\t\tcase 0 -> Optional.of(SHOW_SELECT_QUEST_REWARD_WINDOW1);
\t\t\tcase 1 -> Optional.of(SHOW_SELECT_QUEST_REWARD_WINDOW2);
\t\t\tcase 2 -> Optional.of(SHOW_SELECT_QUEST_REWARD_WINDOW3);
\t\t\tcase 3 -> Optional.of(SHOW_SELECT_QUEST_REWARD_WINDOW4);
\t\t\tcase 4 -> Optional.of(SHOW_SELECT_QUEST_REWARD_WINDOW5);
\t\t\tcase 5 -> Optional.of(SHOW_SELECT_QUEST_REWARD_WINDOW6);
\t\t\tdefault -> Optional.empty();
\t\t};
\t}
"""


def enum_source(
    package: str,
    class_name: str,
    doc: str,
    symbols: set[str],
    by_name: dict[str, int],
    imports: str,
    extra_implementation: str,
    extra_before_lookup: bool,
) -> str:
    missing = sorted(symbols - by_name.keys())
    if missing:
        raise ValueError(f"{class_name} references missing client symbols: {missing}")
    constants = sorted(((by_name[name], name) for name in symbols), key=lambda item: (item[0], item[1]))
    rows = ",\n".join(f"\t{name}({item_id})" for item_id, name in constants) + ";"
    trimmed_extra = extra_implementation.strip("\n")
    extra_before = f"{trimmed_extra}\n\n" if extra_before_lookup else ""
    extra_after = f"\n\n{trimmed_extra}\n" if not extra_before_lookup else ""
    return f"""package {package};

{imports}

/**
 * 由 Aion 5.8 客户端 {doc} 与活动任务 XML 引用生成。
 * Generated from Aion 5.8 client {doc} and active quest XML references.
 */
public enum {class_name} {{
{rows}

\t// 按 id 的静态查找表：fromId 在任务目录编译期被高频调用，早先的 Arrays.stream(values())
\t// 每次调用都会克隆整个枚举数组并分配 Stream 管线（一次启动窗口内实测约 200MB 分配）。
\t// Static id lookup: fromId is hot during catalog compilation, and the previous
\t// Arrays.stream(values()) cloned the whole enum array and allocated a Stream pipeline per call
\t// (~200MB of allocation inside one startup window).
\tprivate static final Map<Integer, {class_name}> BY_ID = buildById();

\tprivate final int id;

\t{class_name}(int id) {{
\t\tthis.id = id;
\t}}

\tpublic int id() {{
\t\treturn id;
\t}}

\tpublic static {class_name} fromId(int id) {{
\t\t{class_name} value = BY_ID.get(id);
\t\tif (value == null) {{
\t\t\tthrow new IllegalArgumentException("unknown {class_name} id " + id);
\t\t}}
\t\treturn value;
\t}}

{extra_before}\tprivate static Map<Integer, {class_name}> buildById() {{
\t\tMap<Integer, {class_name}> byId = new HashMap<>();
\t\tfor ({class_name} value : values()) {{
\t\t\t// 同一枚举不允许重复 id；保留 putIfAbsent 以固定"先声明者优先"的语义。
\t\t\t// Duplicate ids are not expected; putIfAbsent keeps the first-declared-wins semantics.
\t\t\tbyId.putIfAbsent(value.id, value);
\t\t}}
\t\treturn Map.copyOf(byId);
\t}}
{extra_after}}}
"""


def main() -> None:
    args = parse_args()
    root = args.root.resolve()
    actions_by_id, actions_by_name, pages_by_id, pages_by_name = load_maps(root)
    actions, pages = collect(root, actions_by_id, actions_by_name, pages_by_id)
    output_dir = root / "src/main/java/com/aionemu/gameserver/questEngine/definition"
    outputs = {
        output_dir / "QuestDialogAction.java": enum_source(
            "com.aionemu.gameserver.questEngine.definition", "QuestDialogAction", "HyperLinks.xml", actions, actions_by_name,
            "import java.util.HashMap;\nimport java.util.Map;", ACTION_EXTRA_IMPLEMENTATION, True
        ),
        output_dir / "QuestDialogPage.java": enum_source(
            "com.aionemu.gameserver.questEngine.definition", "QuestDialogPage", "HtmlPages.xml", pages, pages_by_name,
            "import java.util.HashMap;\nimport java.util.Map;\nimport java.util.Optional;", PAGE_EXTRA_IMPLEMENTATION, False
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

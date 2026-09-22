#!/usr/bin/env python3
"""块级保留的「纯对话多步链」重建器 v2。

与 v1 的差别：v1 重新渲染整个文件，会丢掉块内子元素（npc-complete 的 choice、
has-item 条件、remove-item 动作、其它 NPC 的入口路由等）。v2 按**原始文本块**处理：

- 只删除四类块：步骤 NPC 的 SETPRO 路由、步骤/领奖 NPC 的 QUEST_SELECT 与翻页路由、
  领奖 NPC 的 SELECT_QUEST_REWARD 路由（重新生成，条件与动作原样搬运）、
  非领奖 NPC 的 npc-complete 与重复提交路由；
- 其余块（NPC_START、unaccepted 路由、enter-world 自愈边、击杀/采集事件等）**原样保留**；
- 节点块在原有状态序列已满足「每行一个状态」时原样保留（如 1319 的 stage1..stage7）；
- 输出的页面名与动作名必须命中 QuestDialogPage / QuestDialogAction 枚举，否则该任务整体 SKIP。

用法：
  python3 apply_multistep_chains_v2.py 1319 1483 ...          # dry-run，写 v2-<id>.xml 并打印块级审计
  python3 apply_multistep_chains_v2.py --apply 1319 1483 ...  # 写回生产 XML
"""
from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

BASE = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE))
import dump_chain_evidence as D  # noqa: E402

REPO = BASE.parents[2]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
ENUM_DIR = REPO / "src/main/java/com/aionemu/gameserver/questEngine/definition"
REWARD_WINDOW = "SHOW_SELECT_QUEST_REWARD_WINDOW1"
CHAIN_COMMENT = ("    <!-- 客户端逐行对话链合同：每行一个 START/REWARD 状态，每步使用该行客户端的页面与按钮动作。 "
                 "Client step-chain contract: one state per quest_summary row, each step uses that row's client page/action. -->")


class Skip(Exception):
    """该任务无法自动重建（需要人工核对）。"""


def enum_constants(path: Path) -> set[str]:
    return {m.group(1) for m in
            re.finditer(r"^\t([A-Z][A-Z0-9_]*)\(\s*-?\d+\s*\)\s*[,;]?\s*$", path.read_text(encoding="utf-8"), re.M)}


ENUM_PAGES = enum_constants(ENUM_DIR / "QuestDialogPage.java")
ENUM_ACTIONS = enum_constants(ENUM_DIR / "QuestDialogAction.java")


def enum_ids(path: Path) -> dict[str, int]:
    return {match.group(1): int(match.group(2)) for match in
            re.finditer(r"^\t([A-Z][A-Z0-9_]*)\(\s*(-?\d+)\s*\)\s*[,;]?\s*$", path.read_text(encoding="utf-8"), re.M)}


PAGE_IDS = enum_ids(ENUM_DIR / "QuestDialogPage.java")
CLIENT_PAGE_INDEX = REPO / "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"


def client_page_ids() -> dict[int, set[int]]:
    """任务 → 客户端 HTML 实际声明的页面 id 集合（docs/quest/client-dialog-mapping 索引）。"""
    index: dict[int, set[int]] = {}
    if not CLIENT_PAGE_INDEX.exists():
        return index
    with CLIENT_PAGE_INDEX.open(encoding="utf-8-sig") as handle:
        next(handle, None)
        for line in handle:
            fields = line.rstrip("\n").split(",")
            if len(fields) < 7 or not fields[0].isdigit() or not fields[6].isdigit():
                continue
            index.setdefault(int(fields[0]), set()).add(int(fields[6]))
    return index


CLIENT_PAGES = client_page_ids()


def shown_page_id(block: str) -> int | None:
    match = re.search(r'<dialog type="SHOW_QUEST_PAGE" page="([A-Z0-9_]+)"', block)
    return PAGE_IDS.get(match.group(1)) if match else None


def resolve_page(name: str, fallback: str | None = None) -> str:
    upper = (name or "").upper()
    if upper in ENUM_PAGES:
        return upper
    if fallback and fallback.upper() in ENUM_PAGES:
        return fallback.upper()
    raise Skip(f"未知客户端页面 {name!r} (fallback={fallback!r})")


def resolve_action(name: str) -> str:
    upper = (name or "").upper()
    if upper not in ENUM_ACTIONS:
        raise Skip(f"未知客户端动作 {name!r}")
    return upper


def element_end(text: str, start: int) -> int:
    depth = 0
    cursor = start
    while cursor < len(text):
        nxt = text.find("<", cursor)
        if nxt < 0:
            raise ValueError("unterminated element")
        if text.startswith("<!--", nxt):
            closing = text.find("-->", nxt)
            if closing < 0:
                raise ValueError("unterminated comment")
            cursor = closing + 3
            continue
        close = text.find(">", nxt)
        if close < 0:
            raise ValueError("unterminated tag")
        token = text[nxt:close + 1]
        if token.startswith("</"):
            depth -= 1
            if depth == 0:
                return close + 1
        elif token.startswith("<?"):
            pass
        elif token.endswith("/>"):
            if depth == 0:
                return close + 1
        else:
            depth += 1
        cursor = close + 1
    raise ValueError("unterminated element")


def split_blocks(text: str) -> list[str]:
    blocks: list[str] = []
    cursor = 0
    while cursor < len(text):
        start = text.find("<", cursor)
        if start < 0:
            break
        if text.startswith("<!--", start):
            closing = text.find("-->", start)
            if closing < 0:
                break
            line_start = text.rfind("\n", 0, start) + 1
            indent = text[line_start:start]
            blocks.append((indent if not indent.strip() else "") + text[start:closing + 3].rstrip())
            cursor = closing + 3
            continue
        match = re.match(r"<([A-Za-z_][\w.-]*)", text[start:])
        if not match:
            cursor = start + 1
            continue
        end = element_end(text, start)
        line_start = text.rfind("\n", 0, start) + 1
        indent = text[line_start:start]
        blocks.append((indent if not indent.strip() else "") + text[start:end].rstrip())
        cursor = end
    return blocks


def container_inner(raw: str, tag: str) -> str:
    open_match = re.search(rf"<{tag}\b[^>]*>", raw)
    if not open_match:
        raise Skip(f"缺少 <{tag}> 块")
    close = raw.rfind(f"</{tag}>")
    if close < 0:
        raise Skip(f"<{tag}> 未闭合")
    return raw[open_match.end():close]


def rewrite_root_attribute(raw: str, name: str, value: str) -> str:
    close = raw.index(">")
    head, tail = raw[:close], raw[close:]
    pattern = rf'(\s{name}=")[^"]*(")'
    if re.search(pattern, head):
        return re.sub(pattern, rf"\g<1>{value}\g<2>", head, count=1) + tail
    stripped = head.rstrip()
    self_closing = stripped.endswith("/")
    if self_closing:
        stripped = stripped[:-1].rstrip()
    return f'{stripped} {name}="{value}"' + ("/>" if self_closing else ">") + tail


def drop_attribute(raw: str, name: str) -> str:
    close = raw.index(">")
    head, tail = raw[:close], raw[close:]
    head = re.sub(rf'\s{name}="[^"]*"', "", head, count=1)
    return head + tail


def rewrite_page(raw: str, page: str) -> str:
    return re.sub(r'(<dialog type="SHOW_QUEST_PAGE" page=")[^"]+(")', rf"\g<1>{page}\g<2>", raw, count=1)


def set_variable(raw: str, value: int) -> str:
    if re.search(r'(<set-variable field="var0" value=")\d+(")', raw):
        return re.sub(r'(<set-variable field="var0" value=")\d+(")', rf"\g<1>{value}\g<2>", raw, count=1)
    return raw


def transition_block(source: str | None, target: str, event: str, conditions: list[str] = (),
                     actions: list[str] = (), after: list[str] = ()) -> str:
    attributes = f' source="{source}"' if source else ""
    lines = [f'    <transition{attributes} target="{target}">', "      <event>", f"        {event}", "      </event>"]
    if conditions:
        lines += ["      <conditions>"] + [f"        {c}" for c in conditions] + ["      </conditions>"]
    if actions:
        lines += ["      <actions>"] + [f"        {a}" for a in actions] + ["      </actions>"]
    if after:
        lines += ["      <after-commit>"] + [f"        {a}" for a in after] + ["      </after-commit>"]
    lines.append("    </transition>")
    return "\n".join(lines)


def talk_block(source: str, target: str, npc: int, action: str, page: str | None = None,
               after_extra: list[str] = ()) -> str:
    after = [f'<dialog type="SHOW_QUEST_PAGE" page="{page}"/>'] if page else []
    after.extend(after_extra)
    return transition_block(source, target, f'<dialog type="TALK_TO_NPC" npc-id="{npc}" action="{action}"/>',
                            after=after)


@dataclass
class Step:
    npc: int
    pages: list[str]
    terminal: str
    source: str
    target: str


@dataclass
class Plan:
    quest_id: int
    rows: int
    steps: list[Step]
    reward_npc: int
    reward_pages: list[str]
    reward_npc_select_page: str
    reward_row_source: str
    nodes_raw: str | None
    kept: list[str]
    dropped: list[tuple[str, str]]
    warnings: list[str] = field(default_factory=list)


def npc_lookup() -> dict[str, int]:
    return {name.lower(): npc_id for npc_id, name in D.npc_names().items()}


def fuzzy_npc(key: str, candidates: set[int], lookup: dict[str, int]) -> int | None:
    lowered = key.lower()
    exact = lookup.get(lowered)
    if exact in candidates:
        return exact
    for name, npc_id in lookup.items():
        if npc_id in candidates and len(name) >= 5 and (name in lowered or lowered in name):
            return npc_id
    return None


def row_keys(row: str) -> list[str]:
    return re.findall(r"STR_DIC_N_([A-Za-z0-9_]+)", row)


def node_sequence(root: ET.Element, rows: int) -> list[str] | None:
    """原文件状态序列已满足「每行一个状态」时返回其标签序列，否则 None。"""
    nodes = root.find("nodes")
    if nodes is None:
        return None
    unaccepted = [n.get("label") for n in nodes if n.get("status") == "NONE"]
    starts = sorted((int((n.find("var") is not None and n.find("var").get("value")) or 0), n.get("label"))
                    for n in nodes if n.get("status") == "START")
    rewards = [(int((n.find("var") is not None and n.find("var").get("value")) or 0), n.get("label"))
               for n in nodes if n.get("status") == "REWARD"]
    if len(unaccepted) != 1 or len(starts) != rows or len(rewards) != 1:
        return None
    if [value for value, _label in starts] != list(range(rows)):
        return None
    if rewards[0][0] != rows - 1:
        return None
    return [unaccepted[0]] + [label for _value, label in starts] + [rewards[0][1]]


def synth_nodes(rows: int) -> str:
    labels = [("unaccepted", "NONE", 0), ("started", "START", 0)]
    labels += [(f"s{index}", "START", index) for index in range(1, rows)]
    labels += [("reward", "REWARD", rows - 1), ("complete", "COMPLETE", 0)]
    lines = ["  <nodes>"]
    for label, status, var0 in labels:
        lines += [f'    <node label="{label}" status="{status}">',
                  f'      <var name="var0" value="{var0}"/>', "    </node>"]
    lines.append("  </nodes>")
    return "\n".join(lines)


def parse_event(block: ET.Element) -> tuple[str, int | None, str | None]:
    event = block.find("event")
    if event is None or len(event) != 1:
        return ("", None, None)
    element = list(event)[0]
    npc = element.get("npc-id")
    return (element.tag, int(npc) if npc and npc.isdigit() else None, element.get("action"))


def build_plan(quest_id: int, raw: str) -> Plan:
    root = ET.fromstring(raw)
    rows, pages, first_text = D.client(quest_id)
    chain_list = D.chains(pages, first_text)
    step_chains = [chain for chain in chain_list if chain[0].startswith("SETPRO")]
    reward_chains = [chain for chain in chain_list if chain[0] in ("SELECT_QUEST_REWARD", "SET_SUCCEED")]
    if not rows or not step_chains or not reward_chains:
        raise Skip(f"客户端链不完整 rows={len(rows)} steps={len(step_chains)} reward={len(reward_chains)}")
    if len(step_chains) != len(rows) - 1:
        raise Skip(f"客户端链 {len(step_chains)} != 行数-1 {len(rows) - 1}")

    transitions_raw = container_inner(raw, "transitions")
    blocks = split_blocks(transitions_raw)
    parsed: list[ET.Element] = []
    for block in blocks:
        parsed.append(None if block.lstrip().startswith("<!--") else ET.fromstring(block))  # type: ignore[arg-type]

    xml_starts = [int(node.get("npc-id")) for node, block in zip(parsed, blocks)
                  if node is not None and node.tag == "dialog" and node.get("type") == "NPC_START"]
    completes = [int(node.get("npc-id")) for node in parsed
                 if node is not None and node.tag == "npc-complete"]
    entry = D.retail_entry(quest_id)
    tag, attrs = entry if entry else ("", {})
    retail_npcs: list[int] = []
    if tag == "data_driven_quest":
        retail_npcs = [int(step["ids"]) for step in D.retail_steps(quest_id) if step.get("ids", "").isdigit()]
    elif tag == "item_order":
        index = 1
        while f"talk_npc_id{index}" in attrs:
            retail_npcs.append(int(attrs[f"talk_npc_id{index}"]))
            index += 1

    lookup = npc_lookup()
    reward_pool = set(xml_starts + completes)
    reward_npc = None
    if tag == "data_driven_quest" and attrs.get("end_npc_ids", "").split():
        reward_npc = int(attrs["end_npc_ids"].split()[0])
    elif tag == "item_order" and attrs.get("end_npc_id"):
        reward_npc = int(attrs["end_npc_id"])
    if reward_npc is None:
        for key in (row_keys(rows[-1]) if rows else []):
            reward_npc = fuzzy_npc(key, reward_pool, lookup)
            if reward_npc is not None:
                break
    if reward_npc is None:
        reward_npc = completes[0] if completes else (xml_starts[-1] if xml_starts else None)
    if reward_npc is None:
        raise Skip("无法确定领奖 NPC")

    candidates = {npc for npc in xml_starts + completes + retail_npcs if npc != reward_npc}
    resolved: dict[int, int] = {}
    used: set[int] = set()
    for index in range(len(step_chains)):
        for key in row_keys(rows[index]):
            hit = fuzzy_npc(key, candidates - used, lookup)
            if hit is not None:
                resolved[index] = hit
                used.add(hit)
                break
    missing = [index for index in range(len(step_chains)) if index not in resolved]
    remaining = [npc for npc in (retail_npcs or xml_starts) if npc in candidates - used]
    if len(missing) == len(remaining):
        for index, npc in zip(missing, remaining):
            resolved[index] = npc
    if len(resolved) != len(step_chains):
        raise Skip(f"步骤 NPC 无法逐行解析 resolved={resolved} candidates={sorted(candidates)}")
    npcs = [resolved[index] for index in range(len(step_chains))]
    if len(set(npcs)) != len(npcs):
        raise Skip(f"步骤 NPC 重复 {npcs}")
    if reward_npc in npcs:
        raise Skip(f"领奖 NPC {reward_npc} 同时出现在步骤链")
    if retail_npcs and len(retail_npcs) == len(step_chains) and retail_npcs != npcs:
        raise Skip(f"retail 顺序 {retail_npcs} 与客户端行解析 {npcs} 冲突")

    sequence = node_sequence(root, len(rows)) or (["unaccepted", "started"]
                                                  + [f"s{index}" for index in range(1, len(rows))] + ["reward"])
    reward_row_source = sequence[len(rows)]
    steps: list[Step] = []
    for index, (terminal, seq, _text) in enumerate(step_chains):
        steps.append(Step(npc=npcs[index],
                          pages=[resolve_page(page) for page in seq],
                          terminal=resolve_action(terminal),
                          source=sequence[index + 1],
                          target=sequence[index + 2]))

    reward_page_fallback = None
    for node in parsed:
        if node is not None and node.tag == "dialog" and node.get("type") == "NPC_REPORT" \
                and int(node.get("npc-id", "0")) == reward_npc:
            reward_page_fallback = node.get("page")
    reward_pages = [resolve_page(page, reward_page_fallback) for page in reward_chains[0][1]]
    reward_npc_select_page = reward_pages[0]

    chain_actions: set[str] = {action for chain in chain_list for action in
                               [chain[0], *[page.upper() for page in chain[1]]]}

    kept: list[str] = []
    dropped: list[tuple[str, str]] = []
    captured_quest_select: dict[int, str] = {}
    captured_setpro: dict[int, str] = {}
    captured_select_reward: dict[int, list[str]] = {}
    step_npc_set = set(npcs)
    for block, node in zip(blocks, parsed):
        if node is None:
            dropped.append(("注释（由生成器统一重写）", block))
            continue
        if node.tag == "npc-complete":
            npc = int(node.get("npc-id", "0"))
            if npc == reward_npc:
                kept.append(block)
            else:
                dropped.append((f"非领奖 NPC {npc} 的 npc-complete", block))
            continue
        if node.tag == "dialog":
            if node.get("type") == "NPC_START":
                kept.append(block)
                continue
            npc = int(node.get("npc-id", "0"))
            dropped.append((f"NPC_REPORT {npc} 的旧上报页（改由领奖行页面承担）", block))
            continue
        event_tag, npc, action = parse_event(node)
        if event_tag != "dialog" or npc is None or not action:
            if event_tag == "enter-world":
                kept.append(set_variable(block, len(rows) - 1))
            else:
                kept.append(block)
            continue
        if action.startswith("SETPRO"):
            if npc in step_npc_set:
                captured_setpro.setdefault(npc, block)
                dropped.append((f"步骤 {npc} 的旧 SETPRO 路由（保留动作后按行重排）", block))
            else:
                dropped.append((f"非步骤 NPC {npc} 的 SETPRO 路由", block))
            continue
        if action == "SELECT_QUEST_REWARD":
            if npc == reward_npc:
                captured_select_reward.setdefault(npc, []).append(block)
                dropped.append((f"领奖 NPC {npc} 的提交路由（保留条件与动作后重排到领奖状态）", block))
            else:
                dropped.append((f"步骤 NPC {npc} 的重复提交路由", block))
            continue
        if action == "QUEST_SELECT":
            if npc in step_npc_set or npc == reward_npc:
                captured_quest_select.setdefault(npc, block)
                dropped.append((f"NPC {npc} 的入口页路由（按行重排到对应状态）", block))
            else:
                kept.append(block)
            continue
        if node.get("source") == "unaccepted":
            kept.append(block)
            continue
        if (npc in step_npc_set or npc == reward_npc) and action in chain_actions:
            dropped.append((f"NPC {npc} 的翻页路由 {action}（按客户端链重排）", block))
        else:
            kept.append(block)

    if not captured_select_reward.get(reward_npc):
        captured_select_reward[reward_npc] = []

    plan = Plan(quest_id=quest_id, rows=len(rows), steps=steps, reward_npc=reward_npc,
                reward_pages=reward_pages, reward_npc_select_page=reward_npc_select_page,
                reward_row_source=reward_row_source, nodes_raw=None, kept=kept, dropped=dropped)
    plan.captured_quest_select = captured_quest_select  # type: ignore[attr-defined]
    plan.captured_setpro = captured_setpro  # type: ignore[attr-defined]
    plan.captured_select_reward = captured_select_reward  # type: ignore[attr-defined]
    nodes_raw = container_inner(raw, "nodes")
    plan.nodes_raw = f"  <nodes>\n{nodes_raw.strip()}\n  </nodes>" if sequence_reusable(raw, len(rows)) else None
    return plan


def sequence_reusable(raw: str, rows: int) -> bool:
    root = ET.fromstring(raw)
    return node_sequence(root, rows) is not None


def render(plan: Plan) -> str:
    original = (QUESTS / f"{plan.quest_id}.xml").read_text(encoding="utf-8")
    header = original[:original.index("  <nodes")] if plan.nodes_raw else original[:original.index("  <nodes")]
    nodes = plan.nodes_raw or synth_nodes(plan.rows)
    blocks: list[str] = [CHAIN_COMMENT]
    blocks.extend(plan.kept)
    for step in plan.steps:
        blocks.append(f"    <!-- 步骤 {step.npc}：{' -> '.join(step.pages)} → {step.terminal} "
                      f"(client {step.pages[0].lower()}) -->")
        captured = getattr(plan, "captured_quest_select", {}).get(step.npc)
        if captured:
            entry = rewrite_page(rewrite_root_attribute(captured, "source", step.source), step.pages[0])
            entry = rewrite_root_attribute(entry, "target", step.source)
            blocks.append(entry)
        else:
            blocks.append(talk_block(step.source, step.source, step.npc, "QUEST_SELECT", step.pages[0]))
        for position in range(len(step.pages) - 1):
            blocks.append(talk_block(step.source, step.source, step.npc,
                                     step.pages[position + 1], step.pages[position + 1]))
        captured_step = getattr(plan, "captured_setpro", {}).get(step.npc)
        if captured_step:
            advance = rewrite_root_attribute(captured_step, "source", step.source)
            advance = rewrite_root_attribute(advance, "target", step.target)
            advance = re.sub(r'(<dialog type="TALK_TO_NPC" npc-id="\d+" action=")[A-Z0-9_]+(")',
                             rf"\g<1>{step.terminal}\g<2>", advance, count=1)
            blocks.append(advance)
        else:
            blocks.append(talk_block(step.source, step.target, step.npc, step.terminal,
                                     after_extra=["<sync-quest-state mode=\"LEVEL_AND_VISIBILITY_REFRESH\"/>",
                                                  "<close-dialog/>"]))
    reward_npc = plan.reward_npc
    reward_row = plan.reward_row_source
    blocks.append(f"    <!-- 领奖行 {reward_npc}：客户端领奖页 → 领奖窗口（进入 REWARD 由本行自己的客户端动作触发） -->")
    captured_reward_select = getattr(plan, "captured_quest_select", {}).get(reward_npc)
    if captured_reward_select:
        entry = rewrite_page(rewrite_root_attribute(captured_reward_select, "source", reward_row),
                             plan.reward_npc_select_page)
        blocks.append(rewrite_root_attribute(entry, "target", reward_row))
    else:
        blocks.append(talk_block(reward_row, reward_row, reward_npc, "QUEST_SELECT",
                                 plan.reward_npc_select_page))
    captures = getattr(plan, "captured_select_reward", {}).get(reward_npc) or []
    if captures:
        for capture in captures:
            # 页面必须在本任务的客户端 HTML 中真实存在；历史迁移遗留的跨任务页（如 3966/3968 的 SELECT6）
            # 会让客户端找不到页面，直接丢弃该分支。
            page = shown_page_id(capture)
            known = CLIENT_PAGES.get(plan.quest_id)
            if page is not None and known and page not in known:
                plan.warnings.append(f"丢弃领奖行分支：页面 {page} 不在本任务客户端页面索引（{sorted(known)}）")
                continue
            # 集齐分支进入 REWARD；未集齐回落分支留在领奖行状态，二者靠 priority/条件区分。
            original_target = ET.fromstring(capture).get("target")
            branch = rewrite_root_attribute(capture, "source", reward_row)
            blocks.append(rewrite_root_attribute(branch, "target",
                                                 "reward" if original_target == "reward" else reward_row))
    else:
        blocks.append(talk_block(reward_row, "reward", reward_npc, "SELECT_QUEST_REWARD",
                                 after_extra=["<sync-quest-state mode=\"LEVEL_AND_VISIBILITY_REFRESH\"/>",
                                              f'<dialog type="SHOW_QUEST_PAGE" page="{REWARD_WINDOW}"/>']))
    transitions = "\n".join(blocks)
    return f"{header}{nodes}\n  <transitions>\n{transitions}\n  </transitions>\n</quest-definition>\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--verbose", action="store_true")
    parser.add_argument("ids", nargs="+", type=int)
    args = parser.parse_args()
    ok, skipped = [], []
    for quest_id in args.ids:
        path = QUESTS / f"{quest_id}.xml"
        raw = path.read_text(encoding="utf-8")
        try:
            plan = build_plan(quest_id, raw)
            output = render(plan)
            ET.fromstring(output)
        except (Skip, ValueError, ET.ParseError) as error:
            skipped.append(quest_id)
            print(f"{quest_id}: SKIP {error}")
            continue
        print(f"{quest_id}: rows={plan.rows} steps={[step.npc for step in plan.steps]} reward={plan.reward_npc} "
              f"nodes={'reused' if plan.nodes_raw else 'synth'} kept={len(plan.kept)} dropped={len(plan.dropped)}")
        for warning in plan.warnings:
            print(f"    ! {warning}")
        for reason, block in plan.dropped:
            if args.verbose:
                print(f"    - DROP {reason}: {re.sub(r'\s+', ' ', block)[:110]}")
        if args.apply:
            path.write_text(output, encoding="utf-8")
        else:
            (BASE / f"v2-{quest_id}.xml").write_text(output, encoding="utf-8")
        ok.append(quest_id)
    print(f"可自动重建 {len(ok)}：{ok}")
    print(f"需人工 {len(skipped)}：{skipped}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

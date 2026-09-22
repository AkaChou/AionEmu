#!/usr/bin/env python3
"""按客户端逐行对话链重建「纯对话多步链」任务的节点与路由。

合同（与 1192 / QE-004 / QE-005 一致）：
- 客户端 quest_summary 有 n 行 → started(var0=0) + s1..s_{n-2} + reward(var0=n-1)；
- 第 i 步 NPC 使用客户端第 i 条链（以 SETPRO{i+1} 结尾），点击推进动作后进入第 i+1 行状态；
- 领奖行 NPC 独占 npc-complete，并下发 SELECT_QUEST_REWARD 链/领奖窗口；
- 接取链（NPC_START / unaccepted 路由）、旧存档自愈边（enter-world + REWARD + var0=0）
  与其它非对话事件原样保留。

用法：
  python3 apply_multistep_chains.py 1319 1483 ...            # dry-run，写 dry-run-<id>.xml
  python3 apply_multistep_chains.py --apply 1319 1483 ...    # 写回生产 XML
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
REWARD_WINDOW = "SHOW_SELECT_QUEST_REWARD_WINDOW1"


@dataclass
class Step:
    npc: int
    pages: list[str]
    terminal: str
    source: str
    target: str
    actions: list[str] = field(default_factory=list)


@dataclass
class Spec:
    quest_id: int
    rows: int
    steps: list[Step]
    reward_npc: int
    reward_pages: list[str]
    reward_complete_attrs: dict[str, str]
    accept_blocks: list[str]
    extras: list[str]
    metadata: str
    progress: str


def pretty(element: ET.Element, level: int = 2) -> str:
    pad = "  " * level
    attributes = "".join(f' {key}="{value}"' for key, value in element.attrib.items())
    children = list(element)
    if not children:
        return f"{pad}<{element.tag}{attributes}/>"
    lines = [f"{pad}<{element.tag}{attributes}>"]
    for child in children:
        lines.append(pretty(child, level + 1))
    lines.append(f"{pad}</{element.tag}>")
    return "\n".join(lines)


def transition(source: str | None, target: str, event: str, conditions: list[str] = (),
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


def talk(source: str, target: str, npc: int, action: str, page: str | None = None,
         actions: list[str] = (), after_extra: list[str] = ()) -> str:
    after = [f'<dialog type="SHOW_QUEST_PAGE" page="{page}"/>'] if page else []
    after.extend(after_extra)
    return transition(source, target, f'<dialog type="TALK_TO_NPC" npc-id="{npc}" action="{action}"/>',
                      actions=list(actions), after=after)



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


def state_labels(rows: int) -> list[tuple[str, str, int]]:
    labels = [("unaccepted", "NONE", 0), ("started", "START", 0)]
    for index in range(1, rows - 1):
        labels.append((f"s{index}", "START", index))
    labels.append(("reward", "REWARD", rows - 1))
    labels.append(("complete", "COMPLETE", 0))
    return labels


def nodes_block(rows: int) -> str:
    lines = ["  <nodes>"]
    for label, status, var0 in state_labels(rows):
        lines += [f'    <node label="{label}" status="{status}">',
                  f'      <var name="var0" value="{var0}"/>', "    </node>"]
    lines.append("  </nodes>")
    return "\n".join(lines)


def existing_step_actions(root: ET.Element) -> dict[int, list[str]]:
    result: dict[int, list[str]] = {}
    for item in root.find("transitions") or []:
        event = item.find("event")
        if event is None:
            continue
        for element in event:
            if element.tag != "dialog" or not (element.get("action") or "").startswith("SETPRO"):
                continue
            actions = item.find("actions")
            result.setdefault(int(element.get("npc-id")), []).extend(
                ET.tostring(child, encoding="unicode").strip() for child in (actions if actions is not None else []))
    return result


def item(action: str, item_id: str, count: str | None) -> str:
    return f'<{action}-item item-id="{item_id}" count="{count or 1}"/>'


def build_spec(quest_id: int, root: ET.Element, raw: str) -> Spec:
    rows, pages, first_text = D.client(quest_id)
    chain_list = D.chains(pages, first_text)
    step_chains = [c for c in chain_list if c[0].startswith("SETPRO")]
    reward_chains = [c for c in chain_list if c[0] == "SELECT_QUEST_REWARD"]
    if not rows or not step_chains or not reward_chains:
        raise SystemExit(f"{quest_id}: 客户端链不完整 rows={len(rows)} steps={len(step_chains)} reward={len(reward_chains)}")
    if len(step_chains) != len(rows) - 1:
        raise SystemExit(f"{quest_id}: 客户端链 {len(step_chains)} != 行数-1 {len(rows) - 1}")

    metadata = re.search(r"  <metadata\b[^>]*>.*?\n  </metadata>", raw, re.S).group(0)
    progress_match = re.search(r"  <progress\b[^>]*>.*?\n  </progress>", raw, re.S)
    progress = progress_match.group(0) if progress_match else ""

    entry = D.retail_entry(quest_id)
    tag, attrs = entry if entry else ("", {})
    xml_starts = [int(v) for v in re.findall(r'<dialog type="NPC_START" npc-id="(\d+)"', raw)]
    completes = [int(v) for v in re.findall(r'<npc-complete npc-id="(\d+)"', raw)]
    retail_npcs: list[int] = []
    if tag == "data_driven_quest":
        retail_npcs = [int(s["ids"]) for s in D.retail_steps(quest_id) if s.get("ids", "").isdigit()]
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
        raise SystemExit(f"{quest_id}: 无法确定领奖 NPC")

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
    missing_rows = [index for index in range(len(step_chains)) if index not in resolved]
    remaining = [npc for npc in (retail_npcs or xml_starts) if npc in candidates - used]
    if len(missing_rows) == len(remaining):
        for index, npc in zip(missing_rows, remaining):
            resolved[index] = npc
    if len(resolved) != len(step_chains):
        raise SystemExit(f"{quest_id}: 步骤 NPC 无法逐行解析 resolved={resolved} "
                         f"keys={[row_keys(rows[i]) for i in range(len(step_chains))]} candidates={sorted(candidates)}")

    npcs = [resolved[index] for index in range(len(step_chains))]
    if retail_npcs and len(retail_npcs) == len(step_chains) and retail_npcs != npcs:
        raise SystemExit(f"{quest_id}: retail 顺序 {retail_npcs} 与客户端行解析 {npcs} 冲突")
    if len(set(npcs)) != len(npcs):
        raise SystemExit(f"{quest_id}: 步骤 NPC 重复 {npcs}")
    if reward_npc in npcs:
        raise SystemExit(f"{quest_id}: 领奖 NPC {reward_npc} 同时出现在步骤链")

    accept_blocks = [pretty(block) for block in root.findall("transitions/dialog")
                     if block.get("type") == "NPC_START"]
    reward_complete = next((block for block in root.findall("transitions/npc-complete")
                            if block.get("npc-id") == str(reward_npc)), None)

    current_actions = existing_step_actions(root)
    accept_given = {found for block in accept_blocks
                    for found in re.findall(r'<give-item item-id="(\d+)"', block)}
    retail_map = {int(s["ids"]): s for s in D.retail_steps(quest_id) if s.get("ids", "").isdigit()}
    steps: list[Step] = []
    for index, (terminal, seq, _text) in enumerate(step_chains):
        npc = npcs[index]
        actions: list[str] = []
        if npc in retail_map and (retail_map[npc].get("give_item_id") or retail_map[npc].get("remove_item_id")):
            step_retail = retail_map[npc]
            if step_retail.get("give_item_id") and step_retail["give_item_id"] not in accept_given:
                actions.append(item("give", step_retail["give_item_id"], step_retail.get("give_item_count")))
            if step_retail.get("remove_item_id"):
                actions.append(item("remove", step_retail["remove_item_id"], step_retail.get("remove_item_count")))
        else:
            actions = current_actions.get(npc, [])
        steps.append(Step(npc=npc, pages=[page.upper() for page in seq], terminal=terminal,
                          source=f"s{index}" if index else "started",
                          target=f"s{index + 1}" if index + 1 < len(rows) - 1 else "reward",
                          actions=actions))

    extras: list[str] = []
    recovery_seen = False
    for item_el in root.find("transitions") or []:
        if item_el.tag in ("npc-complete", "dialog"):
            continue
        event = item_el.find("event")
        kinds = [element.tag for element in (event if event is not None else [])]
        if any(kind == "dialog" for kind in kinds):
            if item_el.get("source") != "unaccepted":
                continue
        elif kinds == ["enter-world"]:
            conditions = item_el.find("conditions")
            if any(condition.tag == "status-is" and condition.get("status") == "REWARD"
                   for condition in (conditions if conditions is not None else [])):
                for action in (item_el.find("actions") if item_el.find("actions") is not None else []):
                    if action.tag == "set-variable" and action.get("field") == "var0":
                        action.set("value", str(len(rows) - 1))
                recovery_seen = True
        extras.append(pretty(item_el))
    if not recovery_seen and len(rows) >= 2:
        extras.append(transition(None, "reward", "<enter-world/>",
                                 conditions=['<status-is status="REWARD"/>',
                                             '<variable-is field="var0" value="0"/>'],
                                 actions=[f'<set-variable field="var0" value="{len(rows) - 1}"/>'],
                                 after=['<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>']))

    return Spec(quest_id=quest_id, rows=len(rows), steps=steps, reward_npc=reward_npc,
                reward_pages=[page.upper() for page in reward_chains[0][1]],
                reward_complete_attrs=dict(reward_complete.attrib) if reward_complete is not None else {},
                accept_blocks=accept_blocks, extras=extras, metadata=metadata, progress=progress)


def render(spec: Spec) -> str:
    transitions: list[str] = [
        "    <!-- 客户端逐行对话链合同：每行一个 START/REWARD 状态，每步使用该行客户端的页面与按钮动作。 "
        "Client step-chain contract: one state per quest_summary row, each step uses that row's client page/action. -->"]
    transitions.extend(spec.accept_blocks)
    for step in spec.steps:
        transitions.append(f"    <!-- 步骤 {step.npc}：{' -> '.join(step.pages)} → {step.terminal} "
                           f"(client {step.pages[0].lower()}) -->")
        transitions.append(talk(step.source, step.source, step.npc, "QUEST_SELECT", step.pages[0]))
        for position in range(len(step.pages) - 1):
            transitions.append(talk(step.source, step.source, step.npc,
                                    step.pages[position + 1], step.pages[position + 1]))
        transitions.append(talk(step.source, step.target, step.npc, step.terminal,
                                actions=step.actions,
                                after_extra=['<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
                                             "<close-dialog/>"]))
    transitions.append(f"    <!-- 领奖行 {spec.reward_npc}：客户端领奖页 → 领奖窗口 -->")
    transitions.append(talk("reward", "reward", spec.reward_npc, "QUEST_SELECT", spec.reward_pages[0]))
    transitions.append(talk("reward", "reward", spec.reward_npc, "SELECT_QUEST_REWARD",
                            after_extra=['<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
                                         f'<dialog type="SHOW_QUEST_PAGE" page="{REWARD_WINDOW}"/>']))
    attributes = dict(spec.reward_complete_attrs) or {
        "npc-id": str(spec.reward_npc), "source": "reward", "target": "complete",
        "fixed-reward-indices": "0 1", "actions": "SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD",
        "complete-reward-index": "0", "finish": "SELECTION_DIALOG"}
    attributes["npc-id"] = str(spec.reward_npc)
    rendered = " ".join(f'{key}="{value}"' for key, value in attributes.items())
    transitions.append("    <npc-complete " + rendered + ">\n"
                       '      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>\n    </npc-complete>')
    transitions.extend(spec.extras)

    blocks = [f'<?xml version="1.0" encoding="UTF-8"?>\n<quest-definition id="{spec.quest_id}" version="1">',
              spec.metadata]
    if spec.progress:
        blocks.append(spec.progress)
    blocks.append(nodes_block(spec.rows))
    blocks.append("  <transitions>\n" + "\n".join(transitions) + "\n  </transitions>")
    blocks.append("</quest-definition>")
    return "\n".join(blocks) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("ids", nargs="+", type=int)
    args = parser.parse_args()
    ok, skipped = [], []
    for quest_id in args.ids:
        path = QUESTS / f"{quest_id}.xml"
        raw = path.read_text(encoding="utf-8")
        root = ET.fromstring(raw)
        try:
            spec = build_spec(quest_id, root, raw)
        except SystemExit as error:
            skipped.append(quest_id)
            print(f"{quest_id}: SKIP {error}")
            continue
        output = render(spec)
        try:
            ET.fromstring(output)
        except ET.ParseError as error:
            skipped.append(quest_id)
            print(f"{quest_id}: SKIP xml {error}")
            continue
        if args.apply:
            path.write_text(output, encoding="utf-8")
            print(f"{quest_id}: applied rows={spec.rows} steps={[s.npc for s in spec.steps]} reward={spec.reward_npc}")
        else:
            (BASE / f"dry-run-{quest_id}.xml").write_text(output, encoding="utf-8")
            print(f"{quest_id}: dry-run rows={spec.rows} steps={[s.npc for s in spec.steps]} reward={spec.reward_npc} "
                  f"chains={[('>'.join(s.pages), s.terminal) for s in spec.steps]}")
        ok.append(quest_id)
    print(f"可自动重建 {len(ok)}：{ok}")
    print(f"需人工 {len(skipped)}：{skipped}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

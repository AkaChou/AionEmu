#!/usr/bin/env python3
"""Analyze and byte-rewrite strictly equivalent quest XML domain blocks."""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import subprocess
import xml.etree.ElementTree as ET
import xml.parsers.expat
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable


ROOT = Path(__file__).resolve().parents[2]
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
DEFAULT_REPORT = ROOT / "docs/quest/xml-block-migration-report.json"


@dataclass(frozen=True)
class Span:
	tag: str
	start: int
	end: int


@dataclass
class Match:
	start_index: int
	end_index: int
	block_type: str
	replacement: str
	details: dict[str, Any]


def direct_transition_spans(data: bytes) -> list[Span]:
	parser = xml.parsers.expat.ParserCreate()
	depth = 0
	transitions_depth: int | None = None
	active: tuple[str, int, int] | None = None
	result: list[Span] = []

	def start(name: str, _attrs: dict[str, str]) -> None:
		nonlocal depth, transitions_depth, active
		if name == "transitions" and transitions_depth is None:
			transitions_depth = depth
		elif transitions_depth is not None and depth == transitions_depth + 1:
			active = (name, parser.CurrentByteIndex, depth)
		depth += 1

	def end(name: str) -> None:
		nonlocal depth, transitions_depth, active
		depth -= 1
		if active is not None and depth == active[2] and name == active[0]:
			index = parser.CurrentByteIndex
			if data[index:index + 2] == b"</":
				closing = data.find(b">", index)
				if closing < 0:
					raise ValueError(f"unterminated closing tag for {name}")
				end_index = closing + 1
			else:
				end_index = index
			result.append(Span(active[0], active[1], end_index))
			active = None
		if name == "transitions" and transitions_depth == depth:
			transitions_depth = None

	parser.StartElementHandler = start
	parser.EndElementHandler = end
	parser.Parse(data, True)
	return result


def children(element: ET.Element, tag: str | None = None) -> list[ET.Element]:
	return [child for child in list(element) if tag is None or child.tag == tag]


def child(element: ET.Element, tag: str) -> ET.Element | None:
	return next((item for item in children(element, tag)), None)


def only_child(element: ET.Element) -> ET.Element:
	items = children(element)
	if len(items) != 1:
		raise ValueError(f"{element.tag} must have exactly one child")
	return items[0]


def parse_dialog_ids(raw: str) -> list[int]:
	result: list[int] = []
	seen: set[int] = set()
	for token in raw.replace(",", " ").split():
		if ".." not in token:
			values = [int(token)]
		else:
			parts = token.split("..")
			if len(parts) != 2:
				raise ValueError(f"invalid dialog range {token}")
			first, last = map(int, parts)
			if first > last or last - first >= 256:
				raise ValueError(f"invalid dialog range {token}")
			values = list(range(first, last + 1))
		for value in values:
			if value in seen:
				raise ValueError(f"duplicate dialog id {value}")
			seen.add(value)
			result.append(value)
	return result


def join_ids(values: list[int]) -> str:
	return " ".join(str(value) for value in values)


def compact_dialog_ids(values: list[int]) -> str:
	if not values:
		return ""
	parts: list[str] = []
	start = previous = values[0]
	for value in values[1:] + [None]:
		if value is not None and value == previous + 1:
			previous = value
			continue
		parts.append(str(start) if start == previous else f"{start}..{previous}")
		if value is not None:
			start = previous = value
	return " ".join(parts)


def canon_element(element: ET.Element) -> dict[str, Any]:
	attrs = dict(element.attrib)
	if element.tag == "grant-reward":
		attrs.setdefault("amount-mode", "EXACT")
	if element.tag == "kill-npc" and "npc-ids" in attrs:
		attrs["npc-ids"] = " ".join(str(value) for value in sorted(map(int, attrs["npc-ids"].split())))
	return {
		"tag": element.tag,
		"attrs": dict(sorted(attrs.items())),
		"children": [canon_element(item) for item in children(element)],
	}


def canon(tag: str, **attrs: Any) -> dict[str, Any]:
	return {"tag": tag, "attrs": {key: str(value) for key, value in sorted(attrs.items())}, "children": []}


def wrapper_items(transition: ET.Element, name: str) -> list[dict[str, Any]]:
	wrapper = child(transition, name)
	return [] if wrapper is None else [canon_element(item) for item in children(wrapper)]


def event_elements(event: ET.Element) -> list[dict[str, Any]]:
	if event.tag != "talk-to-npc" or "dialog-ids" not in event.attrib:
		return [canon_element(event)]
	attrs = dict(event.attrib)
	dialogs = parse_dialog_ids(attrs.pop("dialog-ids"))
	return [canon("talk-to-npc", **attrs, **{"dialog-id": dialog_id}) for dialog_id in dialogs]


def normalized_transition(transition: ET.Element) -> list[dict[str, Any]]:
	event = only_child(child(transition, "event"))
	result: list[dict[str, Any]] = []
	for normalized_event in event_elements(event):
		result.append({
			"source": transition.get("source"),
			"target": transition.get("target"),
			"priority": int(transition.get("priority")) if transition.get("priority") is not None else None,
			"event": normalized_event,
			"conditions": wrapper_items(transition, "conditions"),
			"actions": wrapper_items(transition, "actions"),
			"after_commit": wrapper_items(transition, "after-commit"),
		})
	return result


def transition_ir(source: str, target: str, event: dict[str, Any],
		conditions: list[dict[str, Any]] | None = None,
		actions: list[dict[str, Any]] | None = None,
		after_commit: list[dict[str, Any]] | None = None,
		priority: int | None = None) -> dict[str, Any]:
	return {
		"source": source,
		"target": target,
		"priority": priority,
		"event": event,
		"conditions": conditions or [],
		"actions": actions or [],
		"after_commit": after_commit or [],
	}


def talk_event(npc_id: int, dialog_id: int) -> dict[str, Any]:
	return canon("talk-to-npc", **{"npc-id": npc_id, "dialog-id": dialog_id})


def metadata_rewards(root: ET.Element) -> list[dict[str, Any]]:
	metadata = child(root, "metadata")
	rewards = child(metadata, "rewards") if metadata is not None else None
	if rewards is None:
		return []
	return [dict(item.attrib) for item in children(rewards, "reward")]


def reward_action(reward: dict[str, str]) -> dict[str, Any]:
	kind = reward["kind"].upper()
	action_kind = "ITEM" if kind == "SELECTABLE_ITEM" else kind
	mode = "QUEST_BASE" if action_kind in {"GOLD", "KINAH", "EXP", "AP", "GP"} else "EXACT"
	return canon("grant-reward", kind=action_kind, id=reward["id"], amount=reward["amount"], **{"amount-mode": mode})


def expand_npc_start_ir(block: ET.Element) -> list[dict[str, Any]]:
	npc_id = int(block.get("npc-id"))
	source = block.get("source")
	target = block.get("target")
	selection_sources = block.get("selection-sources", "").split()
	actions_element = child(block, "accept-actions")
	accept_actions = [] if actions_element is None else [canon_element(item) for item in children(actions_element)]
	start_eligible = [canon("start-eligible")]
	visibility = canon("sync-quest-state", mode="VISIBILITY_REFRESH")
	result = [
		transition_ir(source, source, talk_event(npc_id, 31), after_commit=[canon("show-quest-dialog", **{"dialog-id": 1011})]),
		transition_ir(source, source, talk_event(npc_id, 1007), after_commit=[canon("show-quest-dialog", **{"dialog-id": 4})]),
		transition_ir(source, target, talk_event(npc_id, 1002), start_eligible, accept_actions,
			[visibility, canon("show-quest-dialog", **{"dialog-id": 1003})]),
		transition_ir(source, target, talk_event(npc_id, 20000), start_eligible, accept_actions,
			[visibility, canon("close-dialog")]),
	]
	for dialog_id in (1003, 1004, 20001):
		result.append(transition_ir(source, source, talk_event(npc_id, dialog_id), after_commit=[canon("close-dialog")]))
	for selection_source in selection_sources:
		result.append(transition_ir(selection_source, selection_source, talk_event(npc_id, 1008),
			after_commit=[canon("show-quest-selection-dialog", **{"dialog-id": 10})]))
	return result


def expand_counter_ir(block: ET.Element) -> list[dict[str, Any]]:
	source = block.get("source")
	target = block.get("target")
	field = block.get("field")
	required = int(block.get("required"))
	events = event_elements(only_child(child(block, "event")))
	conditions_element = child(block, "conditions")
	shared = [] if conditions_element is None else [canon_element(item) for item in children(conditions_element)]
	result: list[dict[str, Any]] = []
	for event in events:
		result.append(transition_ir(source, source, event,
			shared + [canon("variable-below", field=field, value=required - 1)],
			[canon("increment-variable", field=field, delta=1)],
			[canon("sync-quest-state", mode="PACKET_ONLY")], 1))
		result.append(transition_ir(source, target, event,
			shared + [canon("variable-is", field=field, value=required - 1)],
			[canon("increment-variable", field=field, delta=1)],
			[canon("sync-quest-state", mode="PACKET_ONLY")], 0))
	return result


def expand_kill_chain_ir(block: ET.Element) -> list[dict[str, Any]]:
	nodes = block.get("nodes").split()
	events = event_elements(only_child(child(block, "event")))
	conditions_element = child(block, "conditions")
	conditions = [] if conditions_element is None else [canon_element(item) for item in children(conditions_element)]
	result: list[dict[str, Any]] = []
	for source, target in zip(nodes[:-1], nodes[1:], strict=True):
		for event in events:
			result.append(transition_ir(source, target, event, conditions=conditions,
				after_commit=[canon("sync-quest-state", mode="PACKET_ONLY")]))
	return result


def expand_npc_complete_ir(block: ET.Element, rewards: list[dict[str, Any]]) -> list[dict[str, Any]]:
	npc_id = int(block.get("npc-id"))
	source = block.get("source")
	target = block.get("target")
	fixed_indices = [int(value) for value in block.get("fixed-reward-indices", "").split()]
	fixed = [reward_action(rewards[index]) for index in fixed_indices]
	complete_index = int(block.get("complete-reward-index"))
	finish = block.get("finish")
	after = [canon("refresh-player-stats"), canon("sync-quest-state", mode="COMPLETION")]
	if finish == "SELECTION_DIALOG":
		after.append(canon("show-quest-selection-dialog", **{"dialog-id": 10}))
	elif finish == "CLOSE_DIALOG":
		after.append(canon("close-dialog"))
	result = [transition_ir(source, source, talk_event(npc_id, dialog_id),
		after_commit=[canon("show-quest-dialog", **{"dialog-id": 5})])
		for dialog_id in parse_dialog_ids(block.get("preview-dialog-ids"))]
	if block.get("dialog-ids"):
		for dialog_id in parse_dialog_ids(block.get("dialog-ids")):
			result.append(transition_ir(source, target, talk_event(npc_id, dialog_id), actions=fixed + [
				canon("complete-quest", **{"reward-index": complete_index})], after_commit=after))
	for choice in children(block, "choice"):
		actions = fixed + [reward_action(rewards[int(choice.get("reward-index"))]),
			canon("complete-quest", **{"reward-index": complete_index})]
		result.append(transition_ir(source, target, talk_event(npc_id, int(choice.get("dialog-id"))),
			actions=actions, after_commit=after))
	fallback = child(block, "fallback")
	if fallback is not None:
		for dialog_id in parse_dialog_ids(fallback.get("dialog-ids")):
			result.append(transition_ir(source, target, talk_event(npc_id, dialog_id), actions=fixed + [
				canon("complete-quest", **{"reward-index": complete_index})], after_commit=after))
	return result


def semantic_summary(data: bytes) -> dict[str, Any]:
	root = ET.fromstring(data)
	transitions = child(root, "transitions")
	rewards = metadata_rewards(root)
	result: list[dict[str, Any]] = []
	if transitions is not None:
		for element in children(transitions):
			if element.tag == "transition":
				result.extend(normalized_transition(element))
			elif element.tag == "npc-start":
				result.extend(expand_npc_start_ir(element))
			elif element.tag == "counter":
				result.extend(expand_counter_ir(element))
			elif element.tag == "kill-chain":
				result.extend(expand_kill_chain_ir(element))
			elif element.tag == "npc-complete":
				result.extend(expand_npc_complete_ir(element, rewards))
			else:
				raise ValueError(f"unsupported transitions child {element.tag}")
	payload = json.dumps(result, ensure_ascii=True, sort_keys=True, separators=(",", ":")).encode()
	return {"sha256": hashlib.sha256(payload).hexdigest(), "transition_count": len(result)}


def transition_parts(element: ET.Element) -> dict[str, Any]:
	events = event_elements(only_child(child(element, "event")))
	return {
		"source": element.get("source"),
		"target": element.get("target"),
		"priority": int(element.get("priority")) if element.get("priority") is not None else None,
		"events": events,
		"conditions": wrapper_items(element, "conditions"),
		"actions": wrapper_items(element, "actions"),
		"after": wrapper_items(element, "after-commit"),
	}


def talk_info(parts: dict[str, Any]) -> tuple[int, list[int]] | None:
	if not parts["events"] or any(event["tag"] != "talk-to-npc" for event in parts["events"]):
		return None
	npc_ids = {int(event["attrs"]["npc-id"]) for event in parts["events"]}
	if len(npc_ids) != 1 or any("dialog-id" not in event["attrs"] for event in parts["events"]):
		return None
	return next(iter(npc_ids)), [int(event["attrs"]["dialog-id"]) for event in parts["events"]]


def is_talk(parts: dict[str, Any], source: str, target: str, npc_id: int, dialogs: list[int],
		conditions: list[dict[str, Any]], actions: list[dict[str, Any]],
		after: list[dict[str, Any]], priority: int | None = None) -> bool:
	return (parts["source"] == source and parts["target"] == target and parts["priority"] == priority
		and talk_info(parts) == (npc_id, dialogs) and parts["conditions"] == conditions
		and parts["actions"] == actions and parts["after"] == after)


def node_context(root: ET.Element) -> tuple[dict[str, str], dict[str, dict[str, int]], dict[str, int]]:
	statuses: dict[str, str] = {}
	variables: dict[str, dict[str, int]] = {}
	nodes = child(root, "nodes")
	if nodes is not None:
		for node in children(nodes, "node"):
			projection = child(node, "project")
			statuses[node.get("label")] = projection.get("status")
			vars_element = child(projection, "vars")
			variables[node.get("label")] = {} if vars_element is None else {
				item.get("name"): int(item.get("value")) for item in children(vars_element, "var")}
	fields: dict[str, int] = {}
	progress = child(root, "progress")
	if progress is not None:
		fields = {item.get("name"): int(item.get("max")) for item in children(progress, "bit-field")}
	return statuses, variables, fields


def xml_text(element: ET.Element) -> str:
	return ET.tostring(element, encoding="unicode", short_empty_elements=True).strip()


def attrs_text(values: list[tuple[str, Any]]) -> str:
	return " ".join(f'{name}="{html.escape(str(value), quote=True)}"' for name, value in values)


def render_npc_start(npc_id: int, source: str, target: str, selection_sources: list[str],
		actions: list[ET.Element], indent: str) -> str:
	attrs = [("npc-id", npc_id), ("source", source), ("target", target)]
	if selection_sources:
		attrs.append(("selection-sources", " ".join(selection_sources)))
	opening = f"<npc-start {attrs_text(attrs)}"
	if not actions:
		return opening + "/>"
	lines = [opening + ">", indent + "  <accept-actions>"]
	lines.extend(indent + "    " + xml_text(action) for action in actions)
	lines.extend([indent + "  </accept-actions>", indent + "</npc-start>"])
	return "\n".join(lines)


def match_npc_start(elements: list[ET.Element], index: int, statuses: dict[str, str], indent: str) -> Match | None:
	if index + 5 > len(elements) or any(item.tag != "transition" for item in elements[index:index + 5]):
		return None
	parts = [transition_parts(item) for item in elements[index:index + 5]]
	first_talk = talk_info(parts[0])
	if first_talk is None or first_talk[1] != [31]:
		return None
	npc_id = first_talk[0]
	source = parts[0]["source"]
	target = parts[2]["target"]
	if not source or not target or statuses.get(source) != "NONE" or statuses.get(target) != "START":
		return None
	show1011 = [canon("show-quest-dialog", **{"dialog-id": 1011})]
	show4 = [canon("show-quest-dialog", **{"dialog-id": 4})]
	eligible = [canon("start-eligible")]
	visibility = canon("sync-quest-state", mode="VISIBILITY_REFRESH")
	accept_actions = parts[2]["actions"]
	if not (
		is_talk(parts[0], source, source, npc_id, [31], [], [], show1011)
		and is_talk(parts[1], source, source, npc_id, [1007], [], [], show4)
		and is_talk(parts[2], source, target, npc_id, [1002], eligible, accept_actions,
			[visibility, canon("show-quest-dialog", **{"dialog-id": 1003})])
		and is_talk(parts[3], source, target, npc_id, [20000], eligible, accept_actions,
			[visibility, canon("close-dialog")])
		and is_talk(parts[4], source, source, npc_id, [1003, 1004, 20001], [], [], [canon("close-dialog")])
	):
		return None
	end = index + 5
	selection_sources: list[str] = []
	selection_after = [canon("show-quest-selection-dialog", **{"dialog-id": 10})]
	while end < len(elements) and elements[end].tag == "transition":
		candidate = transition_parts(elements[end])
		selection_source = candidate["source"]
		if not selection_source or statuses.get(selection_source) is None:
			break
		if not is_talk(candidate, selection_source, selection_source, npc_id, [1008], [], [], selection_after):
			break
		if selection_source in selection_sources:
			return None
		selection_sources.append(selection_source)
		end += 1
	actions_element = child(elements[index + 2], "actions")
	action_nodes = [] if actions_element is None else children(actions_element)
	return Match(index, end, "npc-start",
		render_npc_start(npc_id, source, target, selection_sources, action_nodes, indent),
		{"npc_id": npc_id, "source": source, "target": target, "selection_sources": selection_sources})


def same_event(left: dict[str, Any], right: dict[str, Any]) -> bool:
	return left["events"] == right["events"]


def single_action(parts: dict[str, Any], tag: str, **attrs: Any) -> bool:
	return parts["actions"] == [canon(tag, **attrs)]


def packet_only(parts: dict[str, Any]) -> bool:
	return parts["after"] == [canon("sync-quest-state", mode="PACKET_ONLY")]


def match_counter(elements: list[ET.Element], index: int, variables: dict[str, dict[str, int]],
		fields: dict[str, int], indent: str) -> Match | None:
	if index + 2 > len(elements) or any(item.tag != "transition" for item in elements[index:index + 2]):
		return None
	left, right = map(transition_parts, elements[index:index + 2])
	if (left["source"] != left["target"] or left["source"] != right["source"]
			or left["priority"] != 1 or right["priority"] != 0 or not same_event(left, right)
			or not packet_only(left) or not packet_only(right) or not left["conditions"] or not right["conditions"]):
		return None
	below = left["conditions"][-1]
	exact = right["conditions"][-1]
	if below["tag"] != "variable-below" or exact["tag"] != "variable-is":
		return None
	field = below["attrs"].get("field")
	if exact["attrs"].get("field") != field:
		return None
	required = int(below["attrs"]["value"]) + 1
	if int(exact["attrs"].get("value", "-1")) != required - 1:
		return None
	if left["conditions"][:-1] != right["conditions"][:-1]:
		return None
	if not single_action(left, "increment-variable", field=field, delta=1):
		return None
	if not single_action(right, "increment-variable", field=field, delta=1):
		return None
	if field not in fields or required > fields[field] or field in variables.get(left["source"], {}):
		return None
	target_value = variables.get(right["target"], {}).get(field)
	if target_value is not None and target_value != required:
		return None
	event = only_child(child(elements[index], "event"))
	shared = children(child(elements[index], "conditions"))[:-1]
	lines = [f'<counter {attrs_text([("source", left["source"]), ("target", right["target"]), ("field", field), ("required", required)])}>',
		indent + "  <event>" + xml_text(event) + "</event>"]
	if shared:
		lines.append(indent + "  <conditions>" + "".join(xml_text(item) for item in shared) + "</conditions>")
	lines.append(indent + "</counter>")
	return Match(index, index + 2, "counter", "\n".join(lines),
		{"source": left["source"], "target": right["target"], "field": field, "required": required})


def match_kill_chain(elements: list[ET.Element], index: int, indent: str) -> Match | None:
	if index + 2 > len(elements) or elements[index].tag != "transition":
		return None
	first = transition_parts(elements[index])
	if (not first["source"] or not first["target"] or first["source"] == first["target"]
			or first["priority"] is not None or len(first["events"]) != 1
			or first["events"][0]["tag"] != "kill-npc" or first["actions"] or not packet_only(first)):
		return None
	nodes = [first["source"], first["target"]]
	seen = set(nodes)
	if len(seen) != 2:
		return None
	end = index + 1
	while end < len(elements) and elements[end].tag == "transition":
		candidate = transition_parts(elements[end])
		if (candidate["source"] != nodes[-1] or not candidate["target"]
				or candidate["priority"] is not None or candidate["events"] != first["events"]
				or candidate["conditions"] != first["conditions"] or candidate["actions"]
				or not packet_only(candidate) or candidate["target"] in seen):
			break
		nodes.append(candidate["target"])
		seen.add(candidate["target"])
		end += 1
	if end - index < 2:
		return None
	event = only_child(child(elements[index], "event"))
	conditions_element = child(elements[index], "conditions")
	condition_nodes = [] if conditions_element is None else children(conditions_element)
	lines = [f'<kill-chain nodes="{" ".join(nodes)}">', indent + "  <event>" + xml_text(event) + "</event>"]
	if condition_nodes:
		lines.append(indent + "  <conditions>" + "".join(xml_text(item) for item in condition_nodes) + "</conditions>")
	lines.append(indent + "</kill-chain>")
	return Match(index, end, "kill-chain", "\n".join(lines),
		{"nodes": nodes, "event": first["events"][0]})


def review_legacy_counter(elements: list[ET.Element], index: int) -> dict[str, Any] | None:
	if index + 2 > len(elements) or any(item.tag != "transition" for item in elements[index:index + 2]):
		return None
	left, right = map(transition_parts, elements[index:index + 2])
	if (left["source"] != left["target"] or left["source"] != right["source"]
			or left["priority"] != 1 or right["priority"] != 0 or not same_event(left, right)
			or not packet_only(left) or not packet_only(right) or not left["conditions"] or not right["conditions"]):
		return None
	below = left["conditions"][-1]
	at_least = right["conditions"][-1]
	if below["tag"] != "variable-below" or at_least["tag"] != "variable-at-least":
		return None
	field = below["attrs"].get("field")
	required = int(below["attrs"].get("value", "-1"))
	if (at_least["attrs"].get("field") != field or int(at_least["attrs"].get("value", "-2")) != required
			or left["conditions"][:-1] != right["conditions"][:-1]
			or not single_action(left, "increment-variable", field=field, delta=1)
			or not single_action(right, "set-variable", field=field, value=required)):
		return None
	return {"source": left["source"], "target": right["target"], "field": field, "required": required,
		"reason": "legacy threshold routes require a later event and may be locked by node projections"}


def expected_reward_index(action: dict[str, Any], rewards: list[dict[str, Any]], selectable: bool,
		used: set[int]) -> int | None:
	for index, reward in enumerate(rewards):
		if index in used or (reward["kind"].upper() == "SELECTABLE_ITEM") != selectable:
			continue
		if reward_action(reward) == action:
			return index
	return None


def finish_mode(after: list[dict[str, Any]]) -> str | None:
	prefix = [canon("refresh-player-stats"), canon("sync-quest-state", mode="COMPLETION")]
	if after == prefix:
		return "NONE"
	if after == prefix + [canon("close-dialog")]:
		return "CLOSE_DIALOG"
	if after == prefix + [canon("show-quest-selection-dialog", **{"dialog-id": 10})]:
		return "SELECTION_DIALOG"
	return None


def match_npc_complete(elements: list[ET.Element], index: int, statuses: dict[str, str],
		rewards: list[dict[str, Any]], indent: str) -> Match | None:
	if index + 2 > len(elements) or elements[index].tag != "transition":
		return None
	preview = transition_parts(elements[index])
	talk = talk_info(preview)
	if talk is None:
		return None
	npc_id, preview_dialogs = talk
	source = preview["source"]
	if (not source or preview["target"] != source or statuses.get(source) != "REWARD"
			or preview["priority"] is not None or preview["conditions"] or preview["actions"]
			or preview["after"] != [canon("show-quest-dialog", **{"dialog-id": 5})]):
		return None
	route_parts: list[dict[str, Any]] = []
	end = index + 1
	target: str | None = None
	finish: str | None = None
	while end < len(elements) and elements[end].tag == "transition":
		parts = transition_parts(elements[end])
		candidate_talk = talk_info(parts)
		candidate_finish = finish_mode(parts["after"])
		if (candidate_talk is None or candidate_talk[0] != npc_id or parts["source"] != source
				or parts["priority"] is not None or parts["conditions"] or candidate_finish is None
				or not parts["actions"] or parts["actions"][-1]["tag"] != "complete-quest"):
			break
		if target is None:
			target = parts["target"]
			finish = candidate_finish
		if parts["target"] != target or candidate_finish != finish:
			break
		route_parts.append(parts)
		end += 1
	if not route_parts or target is None or statuses.get(target) != "COMPLETE":
		return None
	complete_indices = {int(parts["actions"][-1]["attrs"]["reward-index"]) for parts in route_parts}
	if len(complete_indices) != 1:
		return None
	grant_lists = [parts["actions"][:-1] for parts in route_parts]
	if any(any(action["tag"] != "grant-reward" for action in grants) for grants in grant_lists):
		return None
	used_fixed: set[int] = set()
	fixed_indices: list[int] = []
	position = 0
	while all(position < len(grants) for grants in grant_lists):
		action = grant_lists[0][position]
		if any(grants[position] != action for grants in grant_lists[1:]):
			break
		reward_index = expected_reward_index(action, rewards, False, used_fixed)
		if reward_index is None:
			break
		used_fixed.add(reward_index)
		fixed_indices.append(reward_index)
		position += 1
	choices: list[tuple[int, int]] = []
	fallback_dialogs: list[int] = []
	seen_dialogs = set(preview_dialogs)
	for parts, grants in zip(route_parts, grant_lists, strict=True):
		route_talk = talk_info(parts)
		dialogs = route_talk[1]
		if any(dialog_id in seen_dialogs for dialog_id in dialogs):
			return None
		seen_dialogs.update(dialogs)
		tail = grants[position:]
		if not tail:
			fallback_dialogs.extend(dialogs)
			continue
		if len(tail) != 1 or len(dialogs) != 1:
			return None
		reward_index = expected_reward_index(tail[0], rewards, True, set())
		if reward_index is None:
			return None
		choices.append((dialogs[0], reward_index))
	attrs = [("npc-id", npc_id), ("source", source), ("target", target)]
	if fixed_indices:
		attrs.append(("fixed-reward-indices", join_ids(fixed_indices)))
	if fallback_dialogs and not choices:
		attrs.append(("dialog-ids", compact_dialog_ids(fallback_dialogs)))
	attrs.extend([("complete-reward-index", next(iter(complete_indices))),
		("preview-dialog-ids", compact_dialog_ids(preview_dialogs)), ("finish", finish)])
	if not choices:
		replacement = f"<npc-complete {attrs_text(attrs)}/>"
	else:
		lines = [f"<npc-complete {attrs_text(attrs)}>"]
		lines.extend(indent + f'  <choice dialog-id="{dialog_id}" reward-index="{reward_index}"/>'
			for dialog_id, reward_index in choices)
		if fallback_dialogs:
			lines.append(indent + f'  <fallback dialog-ids="{compact_dialog_ids(fallback_dialogs)}"/>')
		lines.append(indent + "</npc-complete>")
		replacement = "\n".join(lines)
	return Match(index, end, "npc-complete", replacement,
		{"npc_id": npc_id, "source": source, "target": target, "fixed_reward_indices": fixed_indices,
			"choices": [{"dialog_id": dialog, "reward_index": reward} for dialog, reward in choices],
			"fallback_dialog_ids": fallback_dialogs, "finish": finish})


def whitespace_between(data: bytes, spans: list[Span], start: int, end: int) -> bool:
	return all(data[spans[index].end:spans[index + 1].start].strip() == b""
		for index in range(start, end - 1))


def line_indent(data: bytes, position: int) -> str:
	line_start = data.rfind(b"\n", 0, position) + 1
	return data[line_start:position].decode("utf-8")


def modified_paths() -> set[str]:
	output = subprocess.run(["git", "status", "--porcelain=v1", "-z", "--untracked-files=all"],
		cwd=ROOT, check=True, capture_output=True).stdout.decode("utf-8", errors="replace")
	result: set[str] = set()
	entries = output.split("\0")
	index = 0
	while index < len(entries) and entries[index]:
		entry = entries[index]
		status = entry[:2]
		path = entry[3:]
		result.add(path)
		if "R" in status or "C" in status:
			index += 1
			if index < len(entries):
				result.add(entries[index])
		index += 1
	return result


def analyze_file(path: Path, write: bool) -> tuple[dict[str, Any], list[dict[str, Any]]]:
	data = path.read_bytes()
	root = ET.fromstring(data)
	quest_id = int(root.get("id"))
	transitions = child(root, "transitions")
	if transitions is None:
		return {"quest_id": quest_id, "path": str(path.relative_to(ROOT)), "classification": "no_transitions"}, []
	elements = children(transitions)
	spans = direct_transition_spans(data)
	if len(elements) != len(spans):
		raise ValueError(f"direct-child span mismatch: XML={len(elements)} byte-spans={len(spans)}")
	statuses, variables, fields = node_context(root)
	rewards = metadata_rewards(root)
	matches: list[Match] = []
	reviews: list[dict[str, Any]] = []
	index = 0
	while index < len(elements):
		review = review_legacy_counter(elements, index)
		if review is not None:
			reviews.append({"quest_id": quest_id, "path": str(path.relative_to(ROOT)), **review})
		indent = line_indent(data, spans[index].start)
		matchers: list[Callable[[], Match | None]] = [
			lambda: match_npc_start(elements, index, statuses, indent),
			lambda: match_counter(elements, index, variables, fields, indent),
			lambda: match_kill_chain(elements, index, indent),
			lambda: match_npc_complete(elements, index, statuses, rewards, indent),
		]
		match = next((candidate for matcher in matchers if (candidate := matcher()) is not None), None)
		if match is None or not whitespace_between(data, spans, match.start_index, match.end_index):
			index += 1
			continue
		matches.append(match)
		index = match.end_index
	if not matches:
		return {"quest_id": quest_id, "path": str(path.relative_to(ROOT)),
			"classification": "behavior_review" if reviews else "no_strict_match",
			"behavior_review_count": len(reviews)}, reviews
	before = semantic_summary(data)
	updated = data
	for match in reversed(matches):
		start = spans[match.start_index].start
		end = spans[match.end_index - 1].end
		updated = updated[:start] + match.replacement.encode("utf-8") + updated[end:]
	after = semantic_summary(updated)
	if before != after:
		return {"quest_id": quest_id, "path": str(path.relative_to(ROOT)),
			"classification": "ir_mismatch_rolled_back", "before_ir": before, "after_ir": after,
			"replacements": [match.block_type for match in matches]}, reviews
	if write:
		path.write_bytes(updated)
	return {"quest_id": quest_id, "path": str(path.relative_to(ROOT)),
		"classification": "migrated" if write else "strict_match",
		"before_ir": before, "after_ir": after,
		"replacements": [{"type": match.block_type, **match.details} for match in matches]}, reviews


def main() -> int:
	parser = argparse.ArgumentParser()
	mode = parser.add_mutually_exclusive_group()
	mode.add_argument("--write", action="store_true", help="write clean files after equal IR summaries")
	mode.add_argument("--include-dirty", action="store_true",
		help="analyze dirty quest XML too; only valid without --write")
	mode.add_argument("--write-including-dirty", action="store_true",
		help="write clean and dirty files after equal IR summaries; preserves bytes outside matched spans")
	parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
	args = parser.parse_args()
	write = args.write or args.write_including_dirty
	include_dirty = args.include_dirty or args.write_including_dirty
	dirty = modified_paths()
	results: list[dict[str, Any]] = []
	reviews: list[dict[str, Any]] = []
	for path in sorted(QUEST_DIR.glob("*.xml")):
		relative = str(path.relative_to(ROOT))
		if relative in dirty and not include_dirty:
			results.append({"path": relative, "classification": "dirty_skipped"})
			continue
		try:
			result, file_reviews = analyze_file(path, write)
			results.append(result)
			reviews.extend(file_reviews)
		except Exception as error:  # fail one file closed and retain it
			results.append({"path": relative, "classification": "analysis_error",
				"error": f"{type(error).__name__}: {error}"})
	counts: dict[str, int] = {}
	blocks: dict[str, int] = {}
	for result in results:
		classification = result["classification"]
		counts[classification] = counts.get(classification, 0) + 1
		for replacement in result.get("replacements", []):
			block_type = replacement if isinstance(replacement, str) else replacement["type"]
			blocks[block_type] = blocks.get(block_type, 0) + 1
	report = {
		"schema_version": 1,
		"mode": "write-including-dirty" if args.write_including_dirty else "write" if args.write else "report-only",
		"quest_directory": str(QUEST_DIR.relative_to(ROOT)),
		"scanned_files": len(results),
		"dirty_paths_at_start": sorted(path for path in dirty if path.startswith(str(QUEST_DIR.relative_to(ROOT)))),
		"classification_counts": dict(sorted(counts.items())),
		"block_counts": dict(sorted(blocks.items())),
		"behavior_review": reviews,
		"files": [result for result in results if result["classification"] not in {"no_strict_match", "no_transitions"}],
	}
	args.report.parent.mkdir(parents=True, exist_ok=True)
	args.report.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
	print(json.dumps({key: report[key] for key in ("mode", "scanned_files", "classification_counts", "block_counts")},
		ensure_ascii=False, sort_keys=True))
	return 1 if counts.get("analysis_error", 0) or counts.get("ir_mismatch_rolled_back", 0) else 0


if __name__ == "__main__":
	raise SystemExit(main())

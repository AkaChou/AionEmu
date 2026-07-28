#!/usr/bin/env python3
from __future__ import annotations

import argparse
import base64
import hashlib
import html.entities
import json
import os
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Iterable


SCHEMA_VERSION = 1
SPEC_ID = "SPEC-001"
SPEC_REVISION = 2
SPEC_STATUS = "APPROVED"
QUEST_DIALOG_FILE = re.compile(r"(?i)^quest_q(\d+)\.html$")
NAMED_ENTITY = re.compile(r"&([A-Za-z][A-Za-z0-9]+);")
BARE_AMPERSAND = re.compile(r"&(?!#\d+;|#x[0-9A-Fa-f]+;|[A-Za-z][A-Za-z0-9]+;)")
OPAQUE_CDATA_CONTENTS = re.compile(
    r"(<Contents\b(?=[^>]*\bcdata\s*=\s*['\"]true['\"])[^>]*>)(.*?)(</Contents\s*>)",
    re.IGNORECASE | re.DOTALL,
)
XML_ENTITIES = {"amp", "apos", "gt", "lt", "quot"}
DIALOG_STANDARD_FAMILY = "dialog-standard"
QUEST_EVIDENCE_CACHE_VERSION = "quest-evidence-v2"
NPC_INDEX_CACHE_VERSION = "npc-index-v1"
ITEM_INDEX_CACHE_VERSION = "item-index-v1"
DIALOG_STANDARD_ACTION_METHODS = {
    "broadcastPacket",
    "changeQuestStep",
    "checkQuestItems",
    "checkQuestItemsSimple",
    "closeDialogWindow",
    "collectItemCheck",
    "defaultCloseDialog",
    "giveQuestItem",
    "getObjectId",
    "getQuestId",
    "removeQuestItem",
    "sendQuestDialog",
    "sendQuestEndDialog",
    "sendQuestSelectionDialog",
    "sendPacket",
    "sendQuestStartDialog",
    "setQuestVar",
    "setQuestVarById",
    "setStatus",
    "updateQuestStatus",
}
DIALOG_REWARD_CHOICES = tuple(f"SELECTED_QUEST_REWARD{index}" for index in range(1, 16)) + ("SELECT_NO_REWARD",)
TARGET_EQUALITY = re.compile(r"\b(?:(?:env\.)?getTargetId\(\)|getNpcId\(\)|targetId|npcId)\)*\s*==\s*(\d+)")
DIALOG_EQUALITY = re.compile(r"QuestDialog\.([A-Z][A-Z0-9_]*)")
DIALOG_ID_EQUALITY = re.compile(r"\b(?:env\.)?getDialogId\(\)\s*==\s*(-?\d+)")
DIALOG_ID_NAMES = {
    -1: "USE_OBJECT",
    **{index + 7: f"SELECTED_QUEST_REWARD{index}" for index in range(1, 16)},
    23: "SELECT_NO_REWARD",
    31: "START_DIALOG",
    39: "CHECK_COLLECTED_ITEMS",
    1002: "ACCEPT_QUEST",
    1003: "REFUSE_QUEST",
    1004: "REFUSE_QUEST_2",
    1007: "ASK_ACCEPTION",
    1008: "FINISH_DIALOG",
    1009: "SELECT_REWARD",
    20000: "ACCEPT_QUEST_SIMPLE",
    20001: "REFUSE_QUEST_SIMPLE",
    20002: "CHECK_COLLECTED_ITEMS_SIMPLE",
    20003: "SETPRO_NEXT",
    20004: "CHECK_AP",
    20005: "CHECK_GOLD",
    **{10000 + index - 1: f"STEP_TO_{index}" for index in range(1, 22)},
    10029: "STEP_TO_30",
    10030: "STEP_TO_31",
    10039: "STEP_TO_40",
    10040: "STEP_TO_41",
    10255: "SET_REWARD",
    **{value: f"SELECT_ACTION_{value}" for value in (
        1011, 1012, 1013, 1014, 1097, 1182, 1352, 1353, 1354, 1355, 1356, 1375, 1396, 1438, 1439,
        1609, 1693, 1694, 1695, 1696, 1697, 1779, 1780, 1864, 1865, 1949, 1950, 2034, 2035, 2036,
        2037, 2038, 2120, 2292, 2375, 2376, 2377, 2378, 2379, 2461, 2546, 2716, 2717, 2718, 2720,
        3058, 3143, 3399, 3400, 3739, 3740, 3741, 4081, 4166, 4763, 6501, 6503, 6842, 6844, 7183, 7524,
    )},
}
INVENTORY_COMPARISON = re.compile(
    r"getItemCountByItemId\((\d+)\)\s*(>=|<=|==|!=|>|<)\s*(\d+)"
)
QUEST_VARIABLE_COMPARISON = re.compile(
    r"getQuestVarById\((\d+)\)\)*\s*(>=|<=|==|!=|>|<)\s*(-?\d+)"
)
MESSAGE_CONSTRUCTOR = re.compile(
    r'new\s+SM_MESSAGE\([^,]+,\s*("(?:\\.|[^"\\])*")\s*,\s*ChatType\.([A-Z][A-Z0-9_]*)\)'
)
DIALOG_WINDOW_PACKET = re.compile(r"new\s+SM_DIALOG_WINDOW\((.+),\s*(-?\d+)\)")
DIALOG_IMPLEMENTED_CAPABILITIES = {
    "ACTION_CHANGE_STEP",
    "ACTION_CLOSE_DIALOG",
    "ACTION_COLLECT_QUEST_ITEMS",
    "ACTION_DEFAULT_CLOSE_DIALOG",
    "ACTION_FINISH_REWARD",
    "ACTION_GIVE_QUEST_ITEM",
    "ACTION_REMOVE_QUEST_ITEM",
    "ACTION_SEND_DIALOG",
    "ACTION_SEND_PLAYER_MESSAGE",
    "ACTION_SET_PACKED_QUEST_VARIABLES",
    "ACTION_SET_QUEST_STATUS",
    "ACTION_SET_QUEST_VARIABLE",
    "ACTION_SHOW_QUEST_LIST",
    "ACTION_STANDARD_START",
    "ACTION_START_WITH_INITIAL_ITEM",
    "ACTION_SYNC_QUEST_STATUS",
    "CONDITION_PLAYER_INVENTORY",
    "CONDITION_QUEST_VARIABLE",
    "CONDITION_QUEST_STATUS",
    "CONDITION_REPEAT_AVAILABLE",
    "CONTROL_CONDITIONAL",
    "CONTROL_BOUNDED_REGISTRATION_LOOP",
    "EVENT_DIALOG_ACTION",
    "EVENT_DIALOG_TARGET",
    "PROTOCOL_DIALOG_WINDOW_PACKET",
    "PROTOCOL_QUEST_ID_METADATA",
    "PROTOCOL_TARGET_OBJECT_ID_METADATA",
    "REFERENCE_NPC",
    "ROUTING_DIALOG",
    "STATE_PLAYER_CONTEXT",
    "PROVEN_INHERITED_NOOP_REGISTRATION",
}
DIALOG_SIGNATURE_CAPABILITIES = {
    "register": {
        "addOnQuestStart": "ROUTING_DIALOG",
        "addOnTalkEvent": "ROUTING_DIALOG",
        "registerQuestNpc": "ROUTING_DIALOG",
        "registerOnLevelUp": "PROVEN_INHERITED_NOOP_REGISTRATION",
    },
    "helper": {
        "changeQuestStep": "ACTION_CHANGE_STEP",
        "checkQuestItems": "ACTION_COLLECT_QUEST_ITEMS",
        "checkQuestItemsSimple": "ACTION_COLLECT_QUEST_ITEMS",
        "closeDialogWindow": "ACTION_CLOSE_DIALOG",
        "defaultCloseDialog": "ACTION_DEFAULT_CLOSE_DIALOG",
        "giveQuestItem/3": "ACTION_GIVE_QUEST_ITEM",
        "getQuestId": "PROTOCOL_QUEST_ID_METADATA",
        "removeQuestItem/3": "ACTION_REMOVE_QUEST_ITEM",
        "sendQuestDialog": "ACTION_SEND_DIALOG",
        "sendQuestEndDialog": "ACTION_FINISH_REWARD",
        "sendQuestSelectionDialog": "ACTION_SHOW_QUEST_LIST",
        "sendQuestStartDialog/3": "ACTION_START_WITH_INITIAL_ITEM",
        "sendQuestStartDialog": "ACTION_STANDARD_START",
        "updateQuestStatus": "ACTION_SYNC_QUEST_STATUS",
    },
    "state_read": {
        "canRepeat": "CONDITION_REPEAT_AVAILABLE",
        "getQuestId": "PROTOCOL_QUEST_ID_METADATA",
        "getQuestState": "CONDITION_QUEST_STATUS",
        "getQuestStateList": "CONDITION_QUEST_STATUS",
        "getQuestVarById": "CONDITION_QUEST_VARIABLE",
        "getStatus": "CONDITION_QUEST_STATUS",
    },
    "state_write": {
        "changeQuestStep": "ACTION_CHANGE_STEP",
        "setQuestVar": "ACTION_SET_PACKED_QUEST_VARIABLES",
        "setQuestVarById": "ACTION_SET_QUEST_VARIABLE",
        "setStatus": "ACTION_SET_QUEST_STATUS",
        "updateQuestStatus": "ACTION_SYNC_QUEST_STATUS",
    },
    "service": {
        "collectItemCheck": "ACTION_COLLECT_QUEST_ITEMS",
    },
    "direct": {
        "broadcastPacket": "ACTION_SEND_PLAYER_MESSAGE",
        "getDialog": "EVENT_DIALOG_ACTION",
        "getDialogId": "EVENT_DIALOG_ACTION",
        "getInventory": "CONDITION_PLAYER_INVENTORY",
        "getItemCountByItemId": "CONDITION_PLAYER_INVENTORY",
        "getObjectId": "PROTOCOL_TARGET_OBJECT_ID_METADATA",
        "getNpcId": "EVENT_DIALOG_TARGET",
        "getPlayer": "STATE_PLAYER_CONTEXT",
        "getTargetId": "EVENT_DIALOG_TARGET",
        "getVisibleObject": "EVENT_DIALOG_TARGET",
        "sendPacket": "PROTOCOL_DIALOG_WINDOW_PACKET",
        "super": None,
    },
}
STATE_READ_METHODS = {
    "canRepeat",
    "getAllFinishedQuests",
    "getAllQuestState",
    "getCompleteCount",
    "getCompleteTime",
    "getNextRepeatTime",
    "getNormalQuestListSize",
    "getNormalQuests",
    "getPersistentState",
    "getQuestId",
    "getQuestState",
    "getQuestStateList",
    "getQuestVarById",
    "getQuestVars",
    "getQuests",
    "getReward",
    "getStatus",
    "hasQuest",
    "size",
}
STATE_WRITE_METHODS = {
    "addQuest",
    "changeQuestStatus",
    "changeQuestStep",
    "delQuest",
    "finishQuest",
    "questTimerEnd",
    "questTimerStart",
    "removeQuest",
    "setCompleteCount",
    "setCompleteTime",
    "setNextRepeatTime",
    "setPersistentState",
    "setQuestVar",
    "setQuestVarById",
    "setReward",
    "setStatus",
    "startQuest",
    "updateCompleteTime",
    "updateQuestStatus",
}
TASK_ACCESS_METHODS = STATE_READ_METHODS | STATE_WRITE_METHODS
CONTROL_OUTLIER_KINDS = {"DO_WHILE", "LAMBDA", "SYNCHRONIZED", "WHILE"}
EVENT_FAMILIES = {
    "onAddAggroListEvent": "AI_PERCEPTION",
    "onAtDistanceEvent": "PROXIMITY",
    "onAttackEvent": "COMBAT",
    "onBonusApplyEvent": "REWARD",
    "onDialogEvent": "DIALOG",
    "onDieEvent": "COMBAT",
    "onDredgionRewardEvent": "PVP_INSTANCE",
    "onEnterWindStreamEvent": "MOVEMENT",
    "onEnterWorldEvent": "WORLD_ZONE",
    "onEnterZoneEvent": "WORLD_ZONE",
    "onEquipItemEvent": "ITEM",
    "onFailCraftEvent": "CRAFT",
    "onGetItemEvent": "ITEM",
    "onHouseItemUseEvent": "HOUSING",
    "onItemUseEvent": "ITEM",
    "onKillEvent": "COMBAT",
    "onKillInWorldEvent": "COMBAT",
    "onKillRankedEvent": "PVP",
    "onLeaveZoneEvent": "WORLD_ZONE",
    "onLogOutEvent": "RECOVERY",
    "onLvlUpEvent": "LEVEL",
    "onMovieEndEvent": "MOVIE",
    "onNpcLostTargetEvent": "ESCORT_AI",
    "onNpcReachTargetEvent": "ESCORT_AI",
    "onPassFlyingRingEvent": "MOVEMENT",
    "onQuestTimerEndEvent": "TIME",
    "onUseSkillEvent": "SKILL",
    "onZoneMissionEndEvent": "WORLD_ZONE",
}
REGISTRATION_FAMILIES = {
    "addOnAddAggroListEvent/1": "AI_PERCEPTION",
    "addOnAtDistanceEvent/1": "PROXIMITY",
    "addOnAttackEvent/1": "COMBAT",
    "addOnKillEvent/1": "COMBAT",
    "addOnQuestStart/1": "DIALOG",
    "addOnTalkEvent/1": "DIALOG",
    "registerAddOnLostTargetEvent/1": "ESCORT_AI",
    "registerAddOnReachTargetEvent/1": "ESCORT_AI",
    "registerCanAct/2": "MOVEMENT",
    "registerGetingItem/2": "ITEM",
    "registerOnBonusApply/2": "REWARD",
    "registerOnDie/1": "COMBAT",
    "registerOnDredgionReward/1": "PVP_INSTANCE",
    "registerOnEnterWindStream/1": "MOVEMENT",
    "registerOnEnterWorld/1": "WORLD_ZONE",
    "registerOnEnterZone/2": "WORLD_ZONE",
    "registerOnEnterZoneMissionEnd/1": "WORLD_ZONE",
    "registerOnEquipItem/2": "ITEM",
    "registerOnFailCraft/2": "CRAFT",
    "registerOnKillInWorld/2": "COMBAT",
    "registerOnKillRanked/2": "PVP",
    "registerOnLeaveZone/2": "WORLD_ZONE",
    "registerOnLevelUp/1": "LEVEL",
    "registerOnLogOut/1": "RECOVERY",
    "registerOnMovieEndQuest/2": "MOVIE",
    "registerOnPassFlyingRings/2": "MOVEMENT",
    "registerOnQuestTimerEnd/1": "TIME",
    "registerQuestHouseItem/2": "HOUSING",
    "registerQuestItem/2": "ITEM",
    "registerQuestNpc/1": "ROUTING_TARGET",
    "registerQuestSkill/2": "SKILL",
}
REFERENCE_ARGUMENTS = {
    "addItem": ((1, "ITEM"),),
    "addNewSpawn": ((0, "WORLD"), (1, "INSTANCE"), (2, "NPC")),
    "abandonQuest": ((1, "QUEST"),),
    "applyEffectDirectly": ((0, "SKILL"),),
    "getNextAvailableInstance": ((0, "WORLD"),),
    "giveQuestItem": ((1, "ITEM"),),
    "playQuestMovie": ((1, "MOVIE"),),
    "registerGetingItem": ((0, "ITEM"),),
    "registerOnEnterZone": ((0, "ZONE"),),
    "registerOnEquipItem": ((0, "ITEM"),),
    "registerOnFailCraft": ((0, "RECIPE"),),
    "registerOnKillInWorld": ((0, "WORLD"),),
    "registerOnLeaveZone": ((0, "ZONE"),),
    "registerOnMovieEndQuest": ((0, "MOVIE"),),
    "registerOnPassFlyingRings": ((0, "FLYING_RING"),),
    "registerQuestHouseItem": ((0, "ITEM"),),
    "registerQuestItem": ((0, "ITEM"),),
    "registerQuestNpc": ((0, "NPC"),),
    "registerQuestSkill": ((0, "SKILL"),),
    "removeQuestItem": ((1, "ITEM"),),
    "startEventQuest": ((1, "QUEST"),),
    "teleportTo": ((1, "WORLD"),),
}
ROUTING_SOURCE_PATHS = (
    "src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java",
    "src/main/java/com/aionemu/gameserver/services/DialogService.java",
    "src/main/java/com/aionemu/gameserver/services/QuestService.java",
    "src/main/java/com/aionemu/gameserver/ai2/AI2Actions.java",
    "src/main/java/com/aionemu/gameserver/ai/QuestItemNpcAI2.java",
    "src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_DIALOG_SELECT.java",
    "src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_PLAY_MOVIE_END.java",
    "src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_USE_ITEM.java",
)
ROUTING_POLICIES = (
    {
        "policy": "FIRST_TRUE_CONSUMES",
        "methods": ["onCanAct", "onDialog", "onMovieEnd"],
        "callback_result": "BOOLEAN",
        "propagation": "continue on false; stop on true",
    },
    {
        "policy": "FIRST_NON_UNKNOWN_CONSUMES",
        "methods": ["onItemUseEvent"],
        "callback_result": "HANDLER_RESULT",
        "propagation": "continue on UNKNOWN; stop on FAILED or SUCCESS",
    },
    {
        "policy": "FIRST_REGISTERED_HANDLER",
        "methods": ["onBonusApplyEvent"],
        "callback_result": "HANDLER_RESULT",
        "propagation": "return first registered handler result, including UNKNOWN",
    },
    {
        "policy": "FANOUT_IGNORE_CALLBACK_RESULT",
        "methods": [
            "onAddAggroList", "onAtDistance", "onAttack", "onBastionReward", "onCreativityPoint",
            "onDie", "onDredgionReward", "onEnterWindStream", "onEnterWorld", "onEnterZone",
            "onEnterZoneMissionEnd", "onEquipItem", "onFailCraft", "onHouseItemUseEvent",
            "onInvisibleTimerEnd", "onItemGet", "onKamarReward", "onKill", "onKillInWorld",
            "onKillRanked", "onLeaveZone", "onLogOut", "onLvlUp", "onNpcLostTarget",
            "onNpcReachTarget", "onOphidanReward", "onPassFlyingRing", "onQuestTimerEnd",
            "onUseSkill", "rideAction",
        ],
        "callback_result": "IGNORED",
        "propagation": "visit registered handlers in registry order",
    },
)
ROUTING_CALLER_CONTRACTS = (
    {
        "entry": "onDialog",
        "consumer": "DialogService/CM_DIALOG_SELECT/AI2Actions",
        "contract": "true suppresses non-quest dialog fallback; false permits fallback",
    },
    {
        "entry": "onItemUseEvent",
        "consumer": "CM_USE_ITEM",
        "contract": "FAILED aborts native item use; UNKNOWN and SUCCESS continue native item actions",
    },
    {
        "entry": "onBonusApplyEvent",
        "consumer": "QuestService",
        "contract": "FAILED suppresses generic bonus; UNKNOWN and SUCCESS permit generic bonus",
    },
    {
        "entry": "onCanAct",
        "consumer": "QuestItemNpcAI2",
        "contract": "true permits action-item interaction; false rejects it",
    },
    {
        "entry": "onMovieEnd",
        "consumer": "CM_PLAY_MOVIE_END",
        "contract": "return value is ignored by caller; true only stops internal propagation",
    },
    {
        "entry": "fanout methods",
        "consumer": "gameplay event callers",
        "contract": "dispatcher return, when present, reports dispatch health rather than business consumption",
    },
)
EXECUTION_SURFACE_CONTRACTS = {
    "ADMIN_GM_ENTRYPOINT": {
        "role": "CONTROL_PLANE_ADAPTER",
        "ownership_impact": "NON_OWNER_RETAIN",
        "new_state_access": "GRAPH_COMMAND_API_ONLY",
    },
    "CLIENT_PACKET_ENTRYPOINT": {
        "role": "UNTRUSTED_EVENT_ADAPTER",
        "ownership_impact": "NON_OWNER_RETAIN",
        "new_state_access": "EVENT_OR_COMMAND_API_ONLY",
    },
    "GAMEPLAY_AI": {
        "role": "GAMEPLAY_EVENT_BRIDGE",
        "ownership_impact": "NON_OWNER_RETAIN",
        "new_state_access": "EVENT_AND_QUERY_API_ONLY",
    },
    "GAMEPLAY_CONTROLLER": {
        "role": "GAMEPLAY_QUERY_BRIDGE",
        "ownership_impact": "NON_OWNER_RETAIN",
        "new_state_access": "QUERY_API_ONLY",
    },
    "GAMEPLAY_MODEL": {
        "role": "PLAYER_STATE_PROJECTION",
        "ownership_impact": "NON_OWNER_RETAIN",
        "new_state_access": "PROJECTION_API_ONLY",
    },
    "GAMEPLAY_SERVICE": {
        "role": "GAMEPLAY_ACTION_BRIDGE",
        "ownership_impact": "NON_OWNER_RETAIN",
        "new_state_access": "GRAPH_COMMAND_QUERY_API_ONLY",
    },
    "INSTANCE_RUNTIME": {
        "role": "INSTANCE_CONTEXT_BRIDGE",
        "ownership_impact": "NON_OWNER_RETAIN",
        "new_state_access": "QUERY_EVENT_API_ONLY",
    },
    "LEGACY_XML_MODEL_RUNTIME": {
        "role": "LEGACY_XML_EXECUTION_MODEL",
        "ownership_impact": "LEGACY_OWNER_RETIRE",
        "new_state_access": "NONE_AFTER_SWITCH",
    },
    "LEGACY_XML_TEMPLATE_RUNTIME": {
        "role": "LEGACY_XML_EXECUTOR",
        "ownership_impact": "LEGACY_OWNER_RETIRE",
        "new_state_access": "NONE_AFTER_SWITCH",
    },
    "PERSISTENCE": {
        "role": "PLAYER_GRAPH_STATE_STORE",
        "ownership_impact": "STORE_OWNER_REPLACE",
        "new_state_access": "DAO_ONLY",
    },
    "PLAYER_COMMAND_ENTRYPOINT": {
        "role": "PLAYER_COMMAND_ADAPTER",
        "ownership_impact": "NON_OWNER_RETAIN",
        "new_state_access": "GRAPH_COMMAND_API_ONLY",
    },
    "PROTOCOL_SERIALIZATION": {
        "role": "CLIENT_STATE_PROJECTION",
        "ownership_impact": "NON_OWNER_RETAIN",
        "new_state_access": "READ_ONLY_PROJECTION",
    },
    "QUEST_ENGINE_CORE": {
        "role": "GRAPH_RUNTIME_OWNER",
        "ownership_impact": "OWNER_REPLACE_IN_PLACE",
        "new_state_access": "SOLE_TRANSITION_AUTHORITY",
    },
}


class AuditError(RuntimeError):
    pass


@dataclass(frozen=True)
class Config:
    project_root: Path
    java_root: Path
    java_handler_root: Path
    xml_handler_root: Path
    dialogs_root: Path
    report_dir: Path
    check: bool


@dataclass
class InventoryBundle:
    manifest: dict[str, Any]
    input_hash: str
    java_rows: list[dict[str, Any]]
    handler_report: dict[str, Any]
    external_report: dict[str, Any]


@dataclass
class DialogBundle:
    manifest: dict[str, Any]
    input_hash: str
    flow_report: dict[str, Any]
    action_report: dict[str, Any]
    conflict_report: dict[str, Any]


def canonical_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=True, indent=2, sort_keys=True) + "\n"


def content_hash(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def file_hash(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def tool_version() -> str:
    tool_dir = Path(__file__).resolve().parent
    digest = hashlib.sha256()
    for path in sorted((tool_dir / name for name in (
        "HandlerAstInventory.java", "QuestGraphCompilerGate.java", "quest_migration.py",
    ))):
        digest.update(path.name.encode("utf-8"))
        digest.update(b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\0")
    return digest.hexdigest()


def cache_path(config: Config, namespace: str, input_hash: str,
               implementation_hash: str | None = None) -> tuple[Path, str]:
    version = implementation_hash or tool_version()
    key = content_hash(f"{namespace}\0{version}\0{input_hash}".encode("utf-8"))
    return config.project_root / "target/quest-migration-cache" / namespace / f"{key}.json", key


def cached_json(config: Config, namespace: str, input_hash: str, builder: Callable[[], Any],
                implementation_hash: str | None = None) -> Any:
    path, key = cache_path(config, namespace, input_hash, implementation_hash)
    if path.is_file():
        try:
            cached = json.loads(path.read_text(encoding="utf-8"))
            if cached.get("cache_format") == 1 and cached.get("key") == key and "payload" in cached:
                return cached["payload"]
        except (OSError, json.JSONDecodeError, AttributeError):
            pass
    payload = builder()
    content = canonical_json({"cache_format": 1, "key": key, "payload": payload})
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w", encoding="utf-8", dir=path.parent, prefix=f".{path.name}.", delete=False,
        ) as stream:
            stream.write(content)
            temporary = Path(stream.name)
        os.replace(temporary, path)
    finally:
        if temporary is not None and temporary.exists():
            temporary.unlink()
    return payload


def relative_key(path: Path, root: Path, label: str) -> str:
    try:
        return path.relative_to(root).as_posix()
    except ValueError:
        return f"{label}/{path.name}"


def input_manifest(groups: dict[str, tuple[Path, Iterable[Path]]]) -> tuple[dict[str, Any], str]:
    entries: list[dict[str, str]] = []
    authorities: dict[str, str] = {}
    for label in sorted(groups):
        root, paths = groups[label]
        authorities[label] = str(root)
        for path in sorted(set(paths)):
            entries.append({
                "authority": label,
                "path": relative_key(path, root, label),
                "sha256": file_hash(path),
            })
    stable = {"authorities": authorities, "files": entries}
    digest_input = [{"authority": entry["authority"], "path": entry["path"], "sha256": entry["sha256"]}
                    for entry in entries]
    return stable, content_hash(canonical_json(digest_input).encode("utf-8"))


def base_report(input_hash: str) -> dict[str, Any]:
    return {
        "schema_version": SCHEMA_VERSION,
        "tool_version": tool_version(),
        "input_manifest_hash": input_hash,
        "spec_id": SPEC_ID,
        "spec_revision": SPEC_REVISION,
        "spec_status": SPEC_STATUS,
    }


def build_quest_graph_input_report(config: Config) -> dict[str, Any]:
    root = config.project_root / "src/main/resources/aion/data/static_data/quest_graph_data"
    entry = root / "quest_graph_data.xml"
    schema = root / "quest_graph_data.xsd"
    graph_root = root / "graphs"
    graph_files = sorted(graph_root.rglob("*.xml")) if graph_root.is_dir() else []
    missing = [path for path in (entry, schema) if not path.is_file()]
    if not graph_files:
        missing.append(graph_root / "*.xml")
    if missing:
        raise AuditError("Missing quest graph input: " + ", ".join(str(path) for path in missing))

    manifest, input_hash = input_manifest({"QUEST_GRAPH_DATA": (root, [entry, schema, *graph_files])})
    try:
        entry_root = ET.parse(entry).getroot()
    except ET.ParseError as error:
        raise AuditError(f"Invalid quest graph entry XML {entry}: {error}") from error
    imports = [child for child in entry_root if local_name(child.tag) == "import"]
    expected_imports = [path.relative_to(root).as_posix() for path in graph_files]
    actual_imports = [element.get("file") for element in imports]
    if local_name(entry_root.tag) != "quest_graphs" or actual_imports != expected_imports \
            or any(element.get("skipRoot") != "true" for element in imports):
        raise AuditError("quest_graph_data.xml imports must exactly match sorted graphs/*.xml with skipRoot=true")

    quest_ids: list[int] = []
    blockers: list[dict[str, Any]] = []
    for path in graph_files:
        try:
            graph_file_root = ET.parse(path).getroot()
        except ET.ParseError as error:
            raise AuditError(f"Invalid quest graph XML {path}: {error}") from error
        if local_name(graph_file_root.tag) != "quest_graphs":
            raise AuditError(f"Quest graph file root must be quest_graphs: {path}")
        for graph in graph_file_root:
            if local_name(graph.tag) != "quest_graph":
                continue
            try:
                quest_ids.append(int(graph.attrib["quest_id"]))
            except (KeyError, ValueError) as error:
                raise AuditError(f"Invalid quest_id in {path}") from error
    duplicates = sorted(quest_id for quest_id, count in Counter(quest_ids).items() if count > 1)
    if duplicates:
        blockers.append({"kind": "DUPLICATE_QUEST_GRAPH_OWNER", "count": len(duplicates), "quest_ids": duplicates})
    return {
        **base_report(input_hash),
        "authority": "CURRENT_QUEST_GRAPH_INPUT",
        "classification": "DERIVED",
        "manifest": manifest,
        "counts": {"files": len(graph_files), "quest_graphs": len(quest_ids)},
        "quest_ids": sorted(quest_ids),
        "blockers": blockers,
    }


def run_command(command: list[str], cwd: Path) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        ["rtk", "proxy", *command],
        cwd=cwd,
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    if result.returncode != 0:
        detail = result.stderr.strip() or result.stdout.strip() or f"exit {result.returncode}"
        raise AuditError(f"Command failed: {' '.join(command)}\n{detail}")
    return result


def run_java_inventory_uncached(config: Config) -> list[dict[str, Any]]:
    helper = Path(__file__).resolve().parent / "HandlerAstInventory.java"
    with tempfile.TemporaryDirectory(prefix="quest-handler-ast-") as directory:
        classes = Path(directory)
        run_command(["javac", "-d", str(classes), str(helper)], config.project_root)
        result = run_command([
            "java",
            "-cp",
            str(classes),
            "HandlerAstInventory",
            str(config.project_root),
            str(config.java_root),
        ], config.project_root)
    rows = []
    for line_number, line in enumerate(result.stdout.splitlines(), 1):
        if not line.strip():
            continue
        try:
            rows.append(json.loads(line))
        except json.JSONDecodeError as error:
            raise AuditError(f"Invalid Java helper JSON on line {line_number}: {error}") from error
    rows.sort(key=lambda row: row["path"])
    return rows


def run_java_inventory(config: Config) -> list[dict[str, Any]]:
    java_files = sorted(config.java_root.rglob("*.java"))
    _, input_hash = input_manifest({"project-java": (config.project_root, java_files)})
    helper_hash = file_hash(Path(__file__).resolve().parent / "HandlerAstInventory.java")
    return cached_json(config, "java-ast", input_hash, lambda: run_java_inventory_uncached(config), helper_hash)


def ensure_real_compiler_runtime(config: Config) -> tuple[str, str]:
    source_dir = config.project_root / "src/main/java/com/aionemu/gameserver/questEngine/graph"
    target_dir = config.project_root / "target/classes/com/aionemu/gameserver/questEngine/graph"
    marker = target_dir / "QuestGraphCompiler.class"
    sources = sorted(source_dir.glob("*.java"))
    cache_root = config.project_root / "target/quest-migration-cache/java-graph-compiler"
    source_digest = hashlib.sha256()
    for path in [config.project_root / "pom.xml", *sources]:
        source_digest.update(path.name.encode("utf-8"))
        source_digest.update(b"\0")
        source_digest.update(path.read_bytes())
        source_digest.update(b"\0")
    build_stamp = cache_root / f"runtime-{source_digest.hexdigest()}.ready"
    if not marker.is_file() or not build_stamp.is_file():
        run_command(["mvn", "-q", "-DskipTests", "compile"], config.project_root)
    if not marker.is_file():
        raise AuditError("MISSING_JAVA_QUEST_GRAPH_COMPILER_CLASSES")
    build_stamp.parent.mkdir(parents=True, exist_ok=True)
    build_stamp.write_text(file_hash(marker) + "\n", encoding="utf-8")

    pom_hash = file_hash(config.project_root / "pom.xml")
    classpath_file = cache_root / f"classpath-{pom_hash}.txt"
    if not classpath_file.is_file():
        classpath_file.parent.mkdir(parents=True, exist_ok=True)
        run_command([
            "mvn", "-q", "dependency:build-classpath", "-DincludeScope=runtime",
            f"-Dmdep.outputFile={classpath_file}",
        ], config.project_root)
    dependencies = classpath_file.read_text(encoding="utf-8").strip()
    if not dependencies:
        raise AuditError("EMPTY_JAVA_QUEST_GRAPH_COMPILER_CLASSPATH")

    helper = Path(__file__).resolve().parent / "QuestGraphCompilerGate.java"
    helper_hash = file_hash(helper)
    classes = cache_root / f"gate-{helper_hash}"
    compiled_helper = classes / "QuestGraphCompilerGate.class"
    runtime_classpath = os.pathsep.join((str(config.project_root / "target/classes"), dependencies))
    if not compiled_helper.is_file():
        classes.mkdir(parents=True, exist_ok=True)
        run_command(["javac", "-cp", runtime_classpath, "-d", str(classes), str(helper)], config.project_root)

    runtime_files = sorted(target_dir.glob("*.class"))
    schema = config.project_root / "src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd"
    implementation = hashlib.sha256()
    for path in [helper, schema, *runtime_files]:
        implementation.update(path.name.encode("utf-8"))
        implementation.update(b"\0")
        implementation.update(path.read_bytes())
        implementation.update(b"\0")
    return os.pathsep.join((str(classes), runtime_classpath)), implementation.hexdigest()


def parse_real_compiler_results(output: str, members: dict[str, str]) -> dict[str, str]:
    failures: dict[str, str] = {}
    seen: set[str] = set()
    for line_number, line in enumerate(output.splitlines(), 1):
        if not line.strip():
            continue
        fields = line.split("\t")
        if len(fields) < 2 or fields[1] not in members or fields[1] in seen:
            raise AuditError(f"Invalid Java compiler gate result on line {line_number}: {line}")
        status, filename = fields[:2]
        seen.add(filename)
        if status == "OK" and len(fields) == 4 and fields[2] == "1":
            continue
        if status == "FAIL" and len(fields) == 3:
            try:
                message = base64.b64decode(fields[2], validate=True).decode("utf-8")
            except (ValueError, UnicodeDecodeError) as error:
                raise AuditError(f"Invalid Java compiler gate failure on line {line_number}") from error
            failures[members[filename]] = f"JAVA_COMPILER_REJECTED:{message}"
            continue
        raise AuditError(f"Invalid Java compiler gate result on line {line_number}: {line}")
    for filename in sorted(set(members) - seen):
        failures[members[filename]] = "JAVA_COMPILER_REJECTED:missing member result"
    return failures


def run_real_member_compiler_uncached(config: Config, artifacts: list[dict[str, Any]],
                                      runtime_classpath: str) -> dict[str, str]:
    schema = config.project_root / "src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd"
    with tempfile.TemporaryDirectory(prefix="quest-graph-members-") as directory:
        member_dir = Path(directory)
        members: dict[str, str] = {}
        for index, artifact in enumerate(sorted(artifacts, key=lambda value: value["member"]["path"])):
            filename = f"{index:06d}.xml"
            (member_dir / filename).write_text(quest_graphs_xml([artifact["graph"]]), encoding="utf-8")
            members[filename] = artifact["member"]["path"]
        result = run_command([
            "java", "-cp", runtime_classpath, "QuestGraphCompilerGate", str(schema), str(member_dir),
        ], config.project_root)
    return parse_real_compiler_results(result.stdout, members)


def validate_member_graph_compiler(config: Config, artifacts: list[dict[str, Any]]) -> dict[str, str]:
    if not artifacts:
        return {}
    runtime_classpath, implementation_hash = ensure_real_compiler_runtime(config)
    inputs = [{
        "path": artifact["member"]["path"],
        "xml": quest_graphs_xml([artifact["graph"]]),
    } for artifact in sorted(artifacts, key=lambda value: value["member"]["path"])]
    input_hash = content_hash(canonical_json(inputs).encode("utf-8"))
    return cached_json(
        config,
        "java-graph-compiler-results",
        input_hash,
        lambda: run_real_member_compiler_uncached(config, artifacts, runtime_classpath),
        implementation_hash,
    )


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def build_start_condition_report(config: Config) -> dict[str, Any]:
    quest_data = config.project_root / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml"
    if not quest_data.is_file():
        raise AuditError(f"Missing quest data input: {quest_data}")
    manifest, input_hash = input_manifest({"CURRENT_QUEST_DATA": (quest_data.parent, [quest_data])})
    try:
        root = ET.parse(quest_data).getroot()
    except ET.ParseError as error:
        raise AuditError(f"Invalid quest data XML {quest_data}: {error}") from error
    if local_name(root.tag) != "quests":
        raise AuditError(f"Quest data root must be quests: {quest_data}")

    quests: dict[int, tuple[ET.Element, int, str]] = {}
    duplicate_ids: list[int] = []
    for element in root:
        if local_name(element.tag) != "quest":
            continue
        try:
            quest_id = int(element.attrib["id"])
            max_repeat_count = int(element.get("max_repeat_count", "1"))
        except (KeyError, ValueError) as error:
            raise AuditError(f"Invalid quest id/repeat count in {quest_data}") from error
        if quest_id <= 0 or max_repeat_count <= 0:
            raise AuditError(f"Quest {quest_id} has an invalid id/repeat count")
        if quest_id in quests:
            duplicate_ids.append(quest_id)
        quests[quest_id] = (element, max_repeat_count, element.get("category", "QUEST"))

    tag_counts: Counter[str] = Counter()
    reward_nonzero = 0
    repeatable_finished = 0
    missing_references: list[dict[str, int]] = []
    unknown_tags: Counter[str] = Counter()
    rows: list[dict[str, Any]] = []
    reward_exceptions = {1922, 2947}

    def parse_ids(element: ET.Element, owner_id: int, tag: str) -> list[int]:
        tokens = (element.text or "").split()
        try:
            values = [int(token) for token in tokens]
        except ValueError as error:
            raise AuditError(f"Quest {owner_id} has an invalid {tag} id list") from error
        if not values or any(value <= 0 for value in values):
            raise AuditError(f"Quest {owner_id} has an empty or invalid {tag} id list")
        return values

    for quest_id, (quest, _, category) in sorted(quests.items()):
        source_groups: list[dict[str, Any]] = []
        missing_targets: set[int] = set()
        for group_index, group in enumerate(child for child in quest if local_name(child.tag) == "start_conditions"):
            atoms: list[dict[str, Any]] = []
            for source in group:
                tag = local_name(source.tag)
                if tag == "finished":
                    try:
                        target_id = int(source.attrib["quest_id"])
                        reward_index = int(source.get("reward", "0"))
                    except (KeyError, ValueError) as error:
                        raise AuditError(f"Quest {quest_id} has an invalid finished condition") from error
                    if target_id <= 0 or reward_index < 0:
                        raise AuditError(f"Quest {quest_id} has an invalid finished condition")
                    tag_counts[tag] += 1
                    reward_nonzero += reward_index != 0
                    atoms.append({"type": "QUEST_STATUS", "quest_id": target_id, "op": "IN", "statuses": ["COMPLETE"]})
                    if target_id not in reward_exceptions:
                        atoms.append({"type": "QUEST_REWARD", "quest_id": target_id, "reward_index": reward_index})
                    target = quests.get(target_id)
                    if target is None:
                        missing_references.append({"owner_quest_id": quest_id, "referenced_quest_id": target_id})
                        missing_targets.add(target_id)
                    else:
                        target_repeat_count = target[1]
                        if target_repeat_count > 1:
                            repeatable_finished += 1
                            if target_repeat_count != 255:
                                atoms.append({
                                    "type": "QUEST_COMPLETION_COUNT",
                                    "quest_id": target_id,
                                    "op": "EQUAL",
                                    "count": target_repeat_count,
                                })
                    continue
                if tag not in {"unfinished", "noacquired", "acquired", "equipped"}:
                    unknown_tags[tag] += 1
                    continue
                for target_id in parse_ids(source, quest_id, tag):
                    tag_counts[tag] += 1
                    if tag == "equipped":
                        atoms.append({"type": "PLAYER_EQUIPPED", "item_id": target_id})
                        continue
                    if target_id not in quests:
                        missing_references.append({"owner_quest_id": quest_id, "referenced_quest_id": target_id})
                        missing_targets.add(target_id)
                    if tag == "unfinished":
                        atoms.append({"type": "QUEST_STATUS", "quest_id": target_id, "op": "NOT_IN", "statuses": ["COMPLETE"]})
                    elif tag == "noacquired":
                        atoms.append({"type": "QUEST_STATUS", "quest_id": target_id, "op": "IN", "statuses": ["NONE", "LOCKED"]})
                    else:
                        atoms.append({
                            "type": "QUEST_STATUS",
                            "quest_id": target_id,
                            "op": "IN",
                            "statuses": ["START", "REWARD", "COMPLETE"],
                        })
            source_groups.append({"index": group_index, "conditions": atoms})
        if not source_groups:
            continue
        explicit_branches = [group["conditions"] for group in source_groups]
        automatic_conditions = [condition for group in source_groups for condition in group["conditions"]
                                if condition["type"] != "PLAYER_EQUIPPED"]
        rows.append({
            "quest_id": quest_id,
            "category": category,
            "migration_ready": not missing_targets,
            "missing_referenced_quest_ids": sorted(missing_targets),
            "source_groups": source_groups,
            "explicit_acceptance": {"operator": "OR", "branches": explicit_branches},
            "automatic_mission": ({"operator": "AND", "conditions": automatic_conditions}
                                  if category == "MISSION" else None),
        })

    missing_references.sort(key=lambda value: (value["owner_quest_id"], value["referenced_quest_id"]))
    blockers: list[dict[str, Any]] = []
    if duplicate_ids:
        blockers.append({"kind": "DUPLICATE_QUEST_TEMPLATE", "count": len(set(duplicate_ids)), "quest_ids": sorted(set(duplicate_ids))})
    if unknown_tags:
        blockers.append({"kind": "UNKNOWN_START_CONDITION", "count": sum(unknown_tags.values()), "tags": dict(sorted(unknown_tags.items()))})
    migration_blockers: list[dict[str, Any]] = []
    if missing_references:
        migration_blockers.append({
            "kind": "MISSING_START_CONDITION_QUEST",
            "count": len(missing_references),
            "references": missing_references,
        })
    group_counts = [len(row["source_groups"]) for row in rows]
    return {
        **base_report(input_hash),
        "authority": "CURRENT_QUEST_HANDLER_DATA",
        "classification": "DERIVED",
        "manifest": manifest,
        "counts": {
            "quest_templates": len(quests),
            "quests_with_start_conditions": len(rows),
            "condition_groups": sum(group_counts),
            "quests_with_multiple_groups": sum(count > 1 for count in group_counts),
            "maximum_groups_per_quest": max(group_counts, default=0),
            "finished_reward_nonzero": reward_nonzero,
            "finished_repeatable": repeatable_finished,
            "missing_quest_references": len(missing_references),
            **{tag: tag_counts[tag] for tag in ("finished", "unfinished", "noacquired", "acquired", "equipped")},
        },
        "reward_index_exceptions": sorted(reward_exceptions),
        "quests": rows,
        "migration_blockers": migration_blockers,
        "blockers": blockers,
    }


def xml_owners(root: Path) -> tuple[list[dict[str, Any]], list[dict[str, str]]]:
    owners: list[dict[str, Any]] = []
    errors: list[dict[str, str]] = []
    for path in sorted(root.rglob("*.xml")):
        if path.name.endswith(".xsd"):
            continue
        try:
            document = ET.parse(path)
        except ET.ParseError as error:
            errors.append({"path": path.relative_to(root).as_posix(), "error": str(error)})
            continue
        for element in list(document.getroot()):
            if local_name(element.tag) == "import" or "id" not in element.attrib:
                continue
            try:
                quest_id = int(element.attrib["id"])
            except ValueError:
                errors.append({
                    "path": path.relative_to(root).as_posix(),
                    "error": f"non-integer owner id: {element.attrib['id']}",
                })
                continue
            owners.append({
                "quest_id": quest_id,
                "kind": "XML_HANDLER",
                "path": path.relative_to(root).as_posix(),
                "template": local_name(element.tag),
                "evidence": "PROVEN_SOURCE",
            })
    owners.sort(key=lambda owner: (owner["quest_id"], owner["path"], owner["template"]))
    return owners, errors


def find_owner_conflicts(owners: list[dict[str, Any]]) -> list[dict[str, Any]]:
    by_quest: dict[int, list[dict[str, Any]]] = defaultdict(list)
    for owner in owners:
        by_quest[owner["quest_id"]].append(owner)
    return [
        {"quest_id": quest_id, "owners": entries}
        for quest_id, entries in sorted(by_quest.items())
        if len(entries) > 1
    ]


def call_signature(call: dict[str, Any]) -> str:
    return f"{call['method']}/{len(call['arguments'])}"


def task_access_classification(row: dict[str, Any], call: dict[str, Any]) -> tuple[str, str]:
    path = row["path"]
    select = call["select"]
    receiver_types = [value for value in call.get("receiver_types", []) if value != "var"]
    if any(re.search(r"(?:^|[.$])QuestState(?:List)?(?:<|$)", value) for value in receiver_types):
        return "CONFIRMED_QUEST_ACCESS", "QUEST_STATE_RECEIVER_TYPE"
    if ".getQuestState" in select or ".getQuestStateList" in select or select.startswith("getQuestStateList()."):
        return "CONFIRMED_QUEST_ACCESS", "QUEST_STATE_ACCESSOR_CHAIN"
    if select.startswith("QuestService."):
        return "CONFIRMED_QUEST_ACCESS", "QUEST_SERVICE_STATIC_CALL"
    if ("/questEngine/handlers/" in path or path.endswith("/questEngine/model/QuestState.java")
            or path.endswith("/model/gameobjects/player/QuestStateList.java")):
        return "CONFIRMED_QUEST_ACCESS", "QUEST_RUNTIME_SOURCE"
    if path.endswith("/services/QuestService.java") and select == call["method"]:
        return "CONFIRMED_QUEST_ACCESS", "QUEST_SERVICE_SELF_CALL"
    if receiver_types:
        return "NON_QUEST_METHOD_COLLISION", "NON_QUEST_RECEIVER_TYPE:" + ",".join(receiver_types)
    if any(marker in select for marker in (
            ".getFriendList().", ".getNpcFactions().", "getEquipment().", "getNpcs(",
            "getOnDistanceEvent().", "tasksById.")):
        return "NON_QUEST_METHOD_COLLISION", "NON_QUEST_ACCESSOR_CHAIN"
    receiver = select.split(".", 1)[0]
    if re.fullmatch(r"[A-Z][A-Za-z0-9_$]*", receiver):
        return "NON_QUEST_METHOD_COLLISION", "NON_QUEST_STATIC_RECEIVER"
    if "getSiegeLocation()." in select:
        return "NON_QUEST_METHOD_COLLISION", "NON_QUEST_ACCESSOR_CHAIN"
    if select in {call["method"], f"this.{call['method']}"} and call["method"] in row["methods"]:
        return "NON_QUEST_METHOD_COLLISION", "NON_QUEST_SELF_METHOD"
    if select == call["method"] and row.get("superclasses"):
        return "NON_QUEST_METHOD_COLLISION", "NON_QUEST_INHERITED_SELF_METHOD"
    return "AMBIGUOUS_TASK_ACCESS", "NO_QUEST_RECEIVER_EVIDENCE"


def execution_surface(path: str) -> str:
    if "/questEngine/handlers/template/" in path:
        return "LEGACY_XML_TEMPLATE_RUNTIME"
    if "/questEngine/handlers/models/xmlQuest/" in path:
        return "LEGACY_XML_MODEL_RUNTIME"
    if "/questEngine/" in path:
        return "QUEST_ENGINE_CORE"
    if "/commands/admin/" in path or "/network/aion/gmhandler/" in path:
        return "ADMIN_GM_ENTRYPOINT"
    if "/commands/player/" in path:
        return "PLAYER_COMMAND_ENTRYPOINT"
    if "/network/aion/clientpackets/" in path:
        return "CLIENT_PACKET_ENTRYPOINT"
    if "/network/" in path and "/serverpackets/" in path:
        return "PROTOCOL_SERIALIZATION"
    if "/services/" in path:
        return "GAMEPLAY_SERVICE"
    if "/ai/" in path:
        return "GAMEPLAY_AI"
    if "/controllers/" in path:
        return "GAMEPLAY_CONTROLLER"
    if "/instance/" in path:
        return "INSTANCE_RUNTIME"
    if "/dao/" in path:
        return "PERSISTENCE"
    if "/model/" in path:
        return "GAMEPLAY_MODEL"
    return "UNCLASSIFIED_EXECUTION_SURFACE"


def build_external_access_report(rows: list[dict[str, Any]], input_hash: str) -> dict[str, Any]:
    accesses = []
    collisions = []
    ambiguous = []
    for row in rows:
        if row["handler_candidate"]:
            continue
        for call in row["calls"]:
            if call["method"] not in TASK_ACCESS_METHODS:
                continue
            classification, evidence = task_access_classification(row, call)
            access_kind = "READ" if call["method"] in STATE_READ_METHODS else "WRITE"
            item = {
                "path": row["path"],
                "enclosing_method": call["enclosing_method"],
                "line": call["line"],
                "method": call["method"],
                "signature": call_signature(call),
                "select": call["select"],
                "receiver_types": call.get("receiver_types", []),
                "access_kind": access_kind,
                "classification": classification,
                "evidence": evidence,
            }
            if classification == "CONFIRMED_QUEST_ACCESS":
                item["execution_surface"] = execution_surface(row["path"])
                accesses.append(item)
            elif classification == "NON_QUEST_METHOD_COLLISION":
                collisions.append(item)
            else:
                ambiguous.append(item)

    key = lambda item: (item["path"], item["line"], item["method"], item["select"])
    accesses.sort(key=key)
    collisions.sort(key=key)
    ambiguous.sort(key=key)
    readers = [item for item in accesses if item["access_kind"] == "READ"]
    writers = [item for item in accesses if item["access_kind"] == "WRITE"]
    by_surface: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in accesses:
        by_surface[item["execution_surface"]].append(item)
    owner_groups = []
    for surface, items in sorted(by_surface.items()):
        contract = EXECUTION_SURFACE_CONTRACTS.get(surface)
        owner_groups.append({
            "execution_surface": surface,
            "files": sorted({item["path"] for item in items}),
            "read_count": sum(item["access_kind"] == "READ" for item in items),
            "write_count": sum(item["access_kind"] == "WRITE" for item in items),
            "access_count": len(items),
            "observed_state_scope": "PLAYER_QUEST_STATE",
            "ownership_status": "DERIVED_REVIEWED" if contract else "PENDING_REVIEW",
            **(contract or {}),
            "classification": "DERIVED",
        })
    unclassified = [item for item in accesses if item["execution_surface"] == "UNCLASSIFIED_EXECUTION_SURFACE"]
    pending_surfaces = [group["execution_surface"] for group in owner_groups
                        if group["ownership_status"] == "PENDING_REVIEW"]
    blockers = []
    if ambiguous:
        blockers.append({"kind": "AMBIGUOUS_TASK_ACCESS", "count": len(ambiguous)})
    if unclassified:
        blockers.append({"kind": "UNCLASSIFIED_EXECUTION_SURFACE", "count": len(unclassified)})
    if pending_surfaces:
        blockers.append({"kind": "PENDING_EXECUTION_SURFACE_OWNERSHIP_REVIEW", "count": len(pending_surfaces)})
    return {
        **base_report(input_hash),
        "authority": "CURRENT_SOURCE_AST",
        "classification": "DERIVED",
        "count": len(writers),
        "counts": {
            "candidate_accesses": len(accesses) + len(collisions) + len(ambiguous),
            "confirmed_task_accesses": len(accesses),
            "confirmed_task_reads": len(readers),
            "confirmed_task_writes": len(writers),
            "non_quest_method_collisions": len(collisions),
            "ambiguous_accesses": len(ambiguous),
            "execution_surfaces": len(owner_groups),
            "reviewed_execution_surfaces": len(owner_groups) - len(pending_surfaces),
            "pending_execution_surfaces": len(pending_surfaces),
        },
        "owner_groups": owner_groups,
        "readers": readers,
        "writers": writers,
        "non_quest_method_collisions": collisions,
        "ambiguous_accesses": ambiguous,
        "blockers": blockers,
    }


def handler_class(row: dict[str, Any]) -> str:
    stem = Path(row["path"]).stem
    return f"{row['package']}.{stem}" if row["package"] else stem


def is_service_call(call: dict[str, Any]) -> bool:
    return bool(re.search(r"(?:^|\.)(?:[A-Za-z_$][A-Za-z0-9_$]*)(?:Service|Services)\d*\.", call["select"]))


def extract_references(calls: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], list[str]]:
    references = {}
    signatures = set()
    for call in calls:
        for argument_index, kind in REFERENCE_ARGUMENTS.get(call["method"], ()):
            if argument_index >= len(call["arguments"]):
                continue
            values = call.get("argument_values", [])
            value = values[argument_index] if argument_index < len(values) else None
            reference = {
                "kind": kind,
                "source_signature": call_signature(call),
                "argument_index": argument_index,
                "expression": call["arguments"][argument_index],
                "value": value,
                "classification": "DERIVED_API_ARGUMENT",
            }
            key = (kind, reference["source_signature"], argument_index, reference["expression"],
                   "" if value is None else str(value))
            references[key] = reference
            signatures.add(f"{kind}:{reference['source_signature']}:{argument_index}")
    return [references[key] for key in sorted(references)], sorted(signatures)


def summarize_handler(row: dict[str, Any], helper_methods: set[str]) -> dict[str, Any]:
    calls = row["calls"]
    def registration_call(call: dict[str, Any]) -> bool:
        return (call["enclosing_method"] == "register"
                and (call["method"].startswith("register") or call["method"].startswith("addOn")))

    register_calls = sorted({call_signature(call) for call in calls if registration_call(call)})
    def confirmed_state_call(call: dict[str, Any]) -> bool:
        classification, _ = task_access_classification(row, call)
        return classification == "CONFIRMED_QUEST_ACCESS" or call["method"] in helper_methods

    state_reads = sorted({
        call_signature(call) for call in calls
        if call["method"] in STATE_READ_METHODS and confirmed_state_call(call)
    })
    state_writes = sorted({
        call_signature(call) for call in calls
        if call["method"] in STATE_WRITE_METHODS and confirmed_state_call(call)
    })
    helper_calls = sorted({call_signature(call) for call in calls if call["method"] in helper_methods})
    service_calls = sorted({
        call_signature(call)
        for call in calls
        if is_service_call(call)
    })
    direct_call_sites = [{
        "signature": call_signature(call),
        "method": call["method"],
        "select": call["select"],
        "enclosing_method": call["enclosing_method"],
        "line": call["line"],
    } for call in calls
        if not registration_call(call)
        and not (call["method"] in TASK_ACCESS_METHODS and confirmed_state_call(call))
        and call["method"] not in helper_methods
        and not is_service_call(call)
    ]
    direct_call_sites.sort(key=lambda site: (site["signature"], site["enclosing_method"], site["line"], site["select"]))
    direct_calls = sorted({site["signature"] for site in direct_call_sites})
    event_methods = sorted({
        method for method in row["methods"]
        if method.startswith("on") and method.endswith("Event")
    })
    references, reference_signatures = extract_references(calls)
    quest_ids = row["quest_ids"]
    return {
        "path": row["path"],
        "class": handler_class(row),
        "quest_id": quest_ids[0] if len(quest_ids) == 1 else None,
        "quest_id_candidates": quest_ids,
        "quest_id_classification": "DERIVED",
        "dynamic_quest_id": len(quest_ids) != 1,
        "event_methods": event_methods,
        "register_calls": register_calls,
        "state_reads": state_reads,
        "state_writes": state_writes,
        "helper_calls": helper_calls,
        "service_calls": service_calls,
        "direct_calls": direct_calls,
        "direct_call_sites": direct_call_sites,
        "reference_signatures": reference_signatures,
        "references": references,
        "overrides": row["overrides"],
        "controls": row["controls"],
        "control_methods": row["control_methods"],
        "integer_literals": row["integer_literals"],
    }


def collect_inventory(config: Config) -> InventoryBundle:
    java_files = sorted(config.java_root.rglob("*.java"))
    xml_files = sorted(config.xml_handler_root.rglob("*.xml"))
    manifest, input_hash = input_manifest({
        "project-java": (config.project_root, java_files),
        "quest-script-xml": (config.xml_handler_root, xml_files),
    })
    rows = run_java_inventory(config)
    helper_methods = next((set(row["methods"]) for row in rows
                           if row["path"].endswith("/questEngine/handlers/QuestHandler.java")), set())
    handlers = [summarize_handler(row, helper_methods) for row in rows if row["handler_candidate"]]
    handlers.sort(key=lambda item: (item["quest_id"] is None, item["quest_id"] or 0, item["path"]))

    java_owners = [{
        "quest_id": handler["quest_id"],
        "kind": "JAVA_HANDLER",
        "path": handler["path"],
        "class": handler["class"],
        "evidence": "PROVEN_SOURCE",
    } for handler in handlers if handler["quest_id"] is not None]
    xml_owner_rows, xml_errors = xml_owners(config.xml_handler_root)
    owners = sorted(java_owners + xml_owner_rows,
                    key=lambda owner: (owner["quest_id"], owner["kind"], owner["path"]))
    by_quest = group_by(owners, "quest_id")
    owner_conflicts = find_owner_conflicts(owners)
    dynamic_handlers = [
        {
            "path": handler["path"],
            "class": handler["class"],
            "quest_id_candidates": handler["quest_id_candidates"],
        }
        for handler in handlers if handler["dynamic_quest_id"]
    ]

    blockers = []
    if xml_errors:
        blockers.append({"kind": "XML_PARSE_ERROR", "count": len(xml_errors)})
    if dynamic_handlers:
        blockers.append({"kind": "DYNAMIC_QUEST_ID", "count": len(dynamic_handlers)})
    if owner_conflicts:
        blockers.append({"kind": "DUPLICATE_OWNER", "count": len(owner_conflicts)})

    handler_report = {
        **base_report(input_hash),
        "authority": "CURRENT_HANDLER_BASELINE",
        "classification": "SOURCE_PROVEN_WITH_DERIVED_AST",
        "counts": {
            "java_handlers": len(handlers),
            "xml_handlers": len(xml_owner_rows),
            "unique_owned_quests": len(by_quest),
            "dynamic_java_handlers": len(dynamic_handlers),
            "owner_conflicts": len(owner_conflicts),
            "xml_errors": len(xml_errors),
        },
        "handlers": handlers,
        "xml_owners": xml_owner_rows,
        "dynamic_handlers": dynamic_handlers,
        "owner_conflicts": owner_conflicts,
        "xml_errors": xml_errors,
        "blockers": blockers,
    }
    external_report = build_external_access_report(rows, input_hash)
    return InventoryBundle(manifest, input_hash, rows, handler_report, external_report)


def dialog_file_rows(root: Path) -> list[tuple[int, Path]]:
    rows = []
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        match = QUEST_DIALOG_FILE.match(path.name)
        if match:
            rows.append((int(match.group(1)), path))
    return sorted(rows, key=lambda row: (row[0], row[1].relative_to(root).as_posix()))


def wrap_opaque_cdata_contents(source: str) -> tuple[str, int]:
    def wrap(match: re.Match[str]) -> str:
        body = match.group(2)
        if "]]>" in body:
            raise AuditError("Opaque client dialog Contents contains a CDATA terminator")
        return f"{match.group(1)}<![CDATA[{body}]]>{match.group(3)}"

    return OPAQUE_CDATA_CONTENTS.subn(wrap, source)


def parse_dialog(path: Path, root: Path, quest_id: int) -> dict[str, Any]:
    def replace_entity(match: re.Match[str]) -> str:
        name = match.group(1)
        if name in XML_ENTITIES:
            return match.group(0)
        replacement = html.entities.html5.get(f"{name};")
        if replacement is None:
            raise AuditError(f"Unknown named entity in client dialog {path}: &{name};")
        return replacement

    try:
        source = path.read_text(encoding="utf-8-sig")
        normalizations = []
        stripped = source.lstrip(" \t\r\n")
        if stripped != source and stripped.startswith("<?xml"):
            source = stripped
            normalizations.append("LEADING_WHITESPACE_BEFORE_XML_DECLARATION")
        named_entities = {
            match.group(1) for match in NAMED_ENTITY.finditer(source)
            if match.group(1) not in XML_ENTITIES
        }
        if named_entities:
            normalizations.append("HTML_NAMED_ENTITIES")
        source, bare_ampersands = BARE_AMPERSAND.subn("&amp;", source)
        if bare_ampersands:
            normalizations.append("BARE_AMPERSAND_ESCAPED")
        normalized_source = NAMED_ENTITY.sub(replace_entity, source)
        try:
            document = ET.ElementTree(ET.fromstring(normalized_source))
        except ET.ParseError as strict_error:
            recovered_source, recovered_blocks = wrap_opaque_cdata_contents(normalized_source)
            if not recovered_blocks:
                raise AuditError(f"Malformed client dialog XML: {path}: {strict_error}") from strict_error
            try:
                document = ET.ElementTree(ET.fromstring(recovered_source))
            except ET.ParseError as recovery_error:
                raise AuditError(
                    f"Malformed client dialog XML: {path}: strict={strict_error}; cdata-recovery={recovery_error}"
                ) from recovery_error
            normalizations.append("OPAQUE_CDATA_CONTENT_RECOVERY")
    except UnicodeError as error:
        raise AuditError(f"Invalid client dialog encoding: {path}: {error}") from error
    pages = []
    all_actions = []
    for page in document.getroot().iter():
        if local_name(page.tag) != "HtmlPage":
            continue
        page_name = page.attrib.get("name", "")
        actions = []
        voices = []
        for child in page.iter():
            name = local_name(child.tag)
            if name == "Act":
                href = child.attrib.get("href", "")
                action = {
                    "href": href,
                    "text": "".join(child.itertext()).strip(),
                }
                actions.append(action)
                all_actions.append({"page": page_name, **action})
            elif name == "Voice":
                voices.append(child.attrib.get("file", ""))
        pages.append({
            "name": page_name,
            "actions": sorted(actions, key=lambda item: (item["href"], item["text"])),
            "voices": sorted(filter(None, voices)),
        })
    pages.sort(key=lambda item: item["name"])
    all_actions.sort(key=lambda item: (item["page"], item["href"], item["text"]))
    return {
        "quest_id": quest_id,
        "path": path.relative_to(root).as_posix(),
        "sha256": file_hash(path),
        "normalizations": normalizations,
        "pages": pages,
        "actions": all_actions,
    }


def collect_dialogs(config: Config) -> DialogBundle:
    if not config.dialogs_root.is_dir():
        raise AuditError(f"Client dialog root does not exist: {config.dialogs_root}")
    all_file_rows = dialog_file_rows(config.dialogs_root)
    paths = [path for _, path in all_file_rows]
    ignored_sources = [
        {
            "quest_id": quest_id,
            "path": path.relative_to(config.dialogs_root).as_posix(),
            "reason": "CLIENT_PATH_UNUSED",
            "sha256": file_hash(path),
        }
        for quest_id, path in all_file_rows
        if "unused" in {part.lower() for part in path.relative_to(config.dialogs_root).parts}
    ]
    file_rows = [
        (quest_id, path)
        for quest_id, path in all_file_rows
        if "unused" not in {part.lower() for part in path.relative_to(config.dialogs_root).parts}
    ]
    manifest, input_hash = input_manifest({
        "client-dialogs": (config.dialogs_root, paths),
    })
    parsed = []
    parse_errors = []
    for quest_id, path in file_rows:
        try:
            parsed.append(parse_dialog(path, config.dialogs_root, quest_id))
        except AuditError as error:
            parse_errors.append({
                "quest_id": quest_id,
                "path": path.relative_to(config.dialogs_root).as_posix(),
                "error": str(error),
            })
    by_quest: dict[int, list[dict[str, Any]]] = defaultdict(list)
    for row in parsed:
        by_quest[row["quest_id"]].append(row)

    conflicts = []
    duplicates = []
    canonical = []
    for quest_id, rows in sorted(by_quest.items()):
        hashes = sorted({row["sha256"] for row in rows})
        if len(rows) > 1:
            duplicate = {
                "quest_id": quest_id,
                "paths": [row["path"] for row in rows],
                "hashes": hashes,
            }
            duplicates.append(duplicate)
            if len(hashes) > 1:
                conflicts.append(duplicate)
        canonical.append(rows[0])

    action_counts: Counter[str] = Counter()
    emotion_counts: Counter[str] = Counter()
    unknown_actions = []
    label_only_count = 0
    flow_quests = []
    for row in canonical:
        page_names = {page["name"].lower() for page in row["pages"]}
        actions = []
        for action in row["actions"]:
            href = action["href"]
            tokens = [token.strip() for token in href.split(";") if token.strip()]
            hactions = sorted(token for token in tokens if token.startswith("HACTION_"))
            emotions = sorted(token for token in tokens if token.startswith("PLAYEMOTION_"))
            unknown_tokens = sorted(set(tokens) - set(hactions) - set(emotions))
            for token in hactions:
                action_counts[token] += 1
            for token in emotions:
                emotion_counts[token] += 1
            if not tokens:
                label_only_count += 1
            select_actions = [token for token in hactions if token.startswith("HACTION_SELECT")]
            candidate = select_actions[0].removeprefix("HACTION_").lower() if select_actions else None
            actions.append({
                **action,
                "tokens": tokens,
                "hactions": hactions,
                "emotions": emotions,
                "unknown_tokens": unknown_tokens,
                "candidate_page": candidate,
                "candidate_page_exists": candidate in page_names if candidate else None,
            })
            for token in unknown_tokens:
                unknown_actions.append({
                    "quest_id": row["quest_id"],
                    "path": row["path"],
                    "page": action["page"],
                    "token": token,
                })
        flow_quests.append({
            "quest_id": row["quest_id"],
            "path": row["path"],
            "pages": row["pages"],
            "actions": actions,
            "classification": "CLIENT_AUXILIARY",
        })

    flow_report = {
        **base_report(input_hash),
        "authority": "CLIENT_DIALOG_AUXILIARY",
        "classification": "CLIENT_AUXILIARY",
        "counts": {
            "files": len(parsed),
            "quests": len(canonical),
            "pages": sum(len(row["pages"]) for row in canonical),
            "actions": sum(len(row["actions"]) for row in canonical),
            "duplicate_quest_ids": len(duplicates),
            "conflicting_quest_ids": len(conflicts),
            "parse_errors": len(parse_errors),
            "ignored_unused_files": len(ignored_sources),
            "normalized_files": sum(bool(row["normalizations"]) for row in canonical),
            "recovered_opaque_content_files": sum(
                "OPAQUE_CDATA_CONTENT_RECOVERY" in row["normalizations"] for row in canonical
            ),
        },
        "quests": flow_quests,
        "blockers": ([{"kind": "DIALOG_PARSE_ERROR", "count": len(parse_errors)}] if parse_errors else [])
                    + ([{"kind": "DIALOG_CONFLICT", "count": len(conflicts)}] if conflicts else []),
    }
    action_report = {
        **base_report(input_hash),
        "authority": "CLIENT_DIALOG_AUXILIARY",
        "classification": "CLIENT_AUXILIARY",
        "action_counts": [
            {"href": href, "count": count}
            for href, count in sorted(action_counts.items())
        ],
        "emotion_counts": [
            {"name": name, "count": count}
            for name, count in sorted(emotion_counts.items())
        ],
        "label_only_elements": label_only_count,
        "unknown_actions": sorted(unknown_actions,
                                  key=lambda item: (item["quest_id"], item["path"], item["page"], item["token"])),
        "blockers": ([{"kind": "UNKNOWN_DIALOG_ACTION", "count": len(unknown_actions)}]
                     if unknown_actions else []),
    }
    conflict_report = {
        **base_report(input_hash),
        "authority": "CLIENT_DIALOG_AUXILIARY",
        "duplicates": duplicates,
        "conflicts": conflicts,
        "parse_errors": parse_errors,
        "recovered_sources": [{
            "quest_id": row["quest_id"],
            "path": row["path"],
            "sha256": row["sha256"],
            "classification": "CLIENT_AUXILIARY_RECOVERED",
            "reason": "OPAQUE_CDATA_CONTENT_RECOVERY",
        } for row in canonical if "OPAQUE_CDATA_CONTENT_RECOVERY" in row["normalizations"]],
        "ignored_sources": ignored_sources,
        "blockers": ([{"kind": "DIALOG_PARSE_ERROR", "count": len(parse_errors)}] if parse_errors else [])
                    + ([{"kind": "DIALOG_CONFLICT", "count": len(conflicts)}] if conflicts else []),
    }
    return DialogBundle(manifest, input_hash, flow_report, action_report, conflict_report)


def event_family(methods: Iterable[str]) -> str:
    categories = {EVENT_FAMILIES.get(method, "UNCLASSIFIED_EVENT") for method in methods}
    return "+".join(sorted(categories)) if categories else "REGISTRATION_ONLY"


def helper_family(signature: str) -> str | None:
    method = signature.split("/", 1)[0]
    if method in {"changeQuestStep", "updateQuestStatus"}:
        return "STATE_TRANSITION"
    if method.startswith("check"):
        return "ITEM_CONDITION"
    if method in {"closeDialogWindow", "sendQuestDialog", "sendQuestEndDialog", "sendQuestNoneDialog",
                  "sendQuestRewardDialog", "sendQuestSelectionDialog", "sendQuestStartDialog"}:
        return "DIALOG_PROTOCOL"
    if method == "defaultCloseDialog":
        return "DIALOG_TRANSITION"
    if method in {"defaultFollowEndEvent", "defaultStartFollowEvent"}:
        return "ESCORT_AI"
    if method in {"defaultOnEnterZoneEvent", "defaultOnZoneMissionEndEvent"}:
        return "WORLD_ZONE"
    if method == "defaultOnGetItemEvent":
        return "ITEM"
    if method in {"defaultOnKillEvent", "defaultOnKillRankedEvent"}:
        return "COMBAT"
    if method == "defaultOnLvlUpEvent":
        return "LEVEL"
    if method in {"giveQuestItem", "removeQuestItem"}:
        return "ITEM"
    if method == "playQuestMovie":
        return "MOVIE"
    if method == "sendEmotion":
        return "EMOTION"
    if method in {"useQuestItem", "useQuestObject"}:
        return "INTERACTION"
    if method == "getQuestId":
        return "QUEST_METADATA"
    return None


def state_read_family(signature: str) -> str | None:
    method = signature.split("/", 1)[0]
    return {
        "canRepeat": "REPEAT_ELIGIBILITY",
        "getCompleteCount": "COMPLETION_COUNT",
        "getQuestId": "QUEST_METADATA",
        "getQuestState": "STATE_LOOKUP",
        "getQuestStateList": "STATE_LOOKUP",
        "getQuestVarById": "VARIABLE",
        "getQuestVars": "VARIABLE",
        "getStatus": "STATUS",
    }.get(method)


def state_write_family(signature: str) -> str | None:
    method = signature.split("/", 1)[0]
    return {
        "addQuest": "ACCEPT",
        "changeQuestStep": "PROGRESS",
        "finishQuest": "COMPLETE_REWARD",
        "questTimerEnd": "TIMER",
        "questTimerStart": "TIMER",
        "setCompleteCount": "COMPLETION_COUNT",
        "setQuestVar": "PROGRESS",
        "setQuestVarById": "PROGRESS",
        "setStatus": "STATUS_TRANSITION",
        "startQuest": "ACCEPT",
        "updateQuestStatus": "PROTOCOL_SYNC",
    }.get(method)


def service_family(signature: str) -> str | None:
    method = signature.split("/", 1)[0]
    if method in {"abandonQuest", "finishQuest", "startEventQuest", "startQuest"}:
        return "QUEST_LIFECYCLE"
    if method in {"addItem", "addQuestItems", "collectItemCheck"}:
        return "ITEM"
    if method in {"addNewSpawn", "spawnQuestNpc"}:
        return "SPAWN"
    if method in {"applyEffectDirectly", "skillEngine"}:
        return "SKILL_EFFECT"
    if method == "checkLevelRequirement":
        return "LEVEL_CONDITION"
    if method in {"checkQuestIsActive", "eventService"}:
        return "EVENT_QUEST"
    if method in {"getHouseByAddress", "getPlayerAddress", "getPlayerStudio", "housingService",
                  "registerPlayerStudio"}:
        return "HOUSING"
    if method in {"getNextAvailableInstance", "registerPlayerWithInstance"}:
        return "INSTANCE"
    if method in {"onEnterZoneMissionEnd", "questEngine"}:
        return "QUEST_ENGINE_SIGNAL"
    if method in {"questTimerEnd", "questTimerStart", "schedule", "threadPoolManager"}:
        return "TIME_ASYNC"
    if method == "setClass":
        return "PLAYER_CLASS"
    if method == "teleportTo":
        return "TELEPORT"
    if method in {"getNpcs", "world"}:
        return "WORLD_OBJECT"
    return None


def build_shapes(inventory: InventoryBundle) -> dict[str, Any]:
    groups: dict[str, dict[str, Any]] = {}
    for handler in inventory.handler_report["handlers"]:
        canonical = {
            "event_methods": handler["event_methods"],
            "overrides": handler.get("overrides", []),
            "register_calls": handler["register_calls"],
            "state_reads": handler["state_reads"],
            "state_writes": handler["state_writes"],
            "helper_calls": handler["helper_calls"],
            "service_calls": handler["service_calls"],
            "direct_calls": handler["direct_calls"],
            "reference_signatures": handler["reference_signatures"],
            "controls": handler["controls"],
            "control_methods": handler["control_methods"],
        }
        fingerprint = content_hash(canonical_json(canonical).encode("utf-8"))[:20]
        group = groups.setdefault(fingerprint, {
            "fingerprint": fingerprint,
            "capability_family": event_family(handler["event_methods"]),
            "classification": "DERIVED",
            "signature": canonical,
            "members": [],
            "outlier_reasons": [],
        })
        group["members"].append({
            "quest_id": handler["quest_id"],
            "class": handler["class"],
            "path": handler["path"],
        })
        bounded_registration_loop = (
            handler["control_methods"].get("WHILE") == ["register"]
            and {"iterator/0", "hasNext/0", "next/0"}.issubset(handler["direct_calls"])
        )
        if bounded_registration_loop:
            group.setdefault("mechanical_notes", []).append("BOUNDED_REGISTRATION_ITERATION")
        outlier_controls = sorted(
            kind for kind in set(handler["controls"]) & CONTROL_OUTLIER_KINDS
            if not (kind == "WHILE" and bounded_registration_loop)
        )
        if outlier_controls:
            group["outlier_reasons"] = sorted(set(group["outlier_reasons"]) | {
                f"CONTROL:{kind}" for kind in outlier_controls
            })
    shape_rows = []
    for group in groups.values():
        group["members"].sort(key=lambda member: (member["quest_id"] is None, member["quest_id"] or 0, member["path"]))
        group["member_count"] = len(group["members"])
        group["mechanical_classification"] = "REPEATED" if len(group["members"]) >= 2 else "UNIQUE_OUTLIER"
        if "mechanical_notes" in group:
            group["mechanical_notes"] = sorted(set(group["mechanical_notes"]))
        shape_rows.append(group)
    shape_rows.sort(key=lambda group: (-group["member_count"], group["fingerprint"]))
    return {
        **base_report(inventory.input_hash),
        "authority": "CURRENT_HANDLER_AST",
        "classification": "DERIVED",
        "counts": {
            "shapes": len(shape_rows),
            "repeated_shapes": sum(row["member_count"] >= 2 for row in shape_rows),
            "unique_outliers": sum(row["member_count"] == 1 for row in shape_rows),
            "handlers_in_repeated_shapes": sum(row["member_count"] for row in shape_rows if row["member_count"] >= 2),
        },
        "shapes": shape_rows,
        "blockers": [],
    }


def dialog_signature_capability(category: str, signature: str) -> str:
    method = signature.split("/", 1)[0]
    mapping = DIALOG_SIGNATURE_CAPABILITIES[category]
    if signature in mapping:
        return mapping[signature]
    if method in mapping:
        return mapping[method]
    return f"UNMAPPED_{category.upper()}:{signature}"


def dialog_compiler_error_family(message: str) -> str:
    prefixes = {
        "Ambiguous start helper group": "AMBIGUOUS_START_HELPER_GROUP",
        "Ambiguous defaultCloseDialog group": "AMBIGUOUS_DEFAULT_CLOSE_GROUP",
        "Ambiguous dialog page group": "AMBIGUOUS_DIALOG_PAGE_GROUP",
        "Ambiguous close-dialog group": "AMBIGUOUS_CLOSE_DIALOG_GROUP",
        "Ignored giveQuestItem has no explicit failure edge": "IGNORED_GIVE_WITHOUT_FAILURE_EDGE",
        "Quest item give result requires an exact success branch": "UNPROVEN_GIVE_RESULT_BRANCH",
        "quest item id is not a constant integer": "DYNAMIC_QUEST_ITEM_ARGUMENT",
        "quest item count is not a constant integer": "DYNAMIC_QUEST_ITEM_ARGUMENT",
        "starter item id is not a constant integer": "DYNAMIC_QUEST_ITEM_ARGUMENT",
        "starter item count is not a constant integer": "DYNAMIC_QUEST_ITEM_ARGUMENT",
        "removeQuestItem lacks a sufficient inventory guard": "REMOVE_WITHOUT_SUFFICIENT_INVENTORY_GUARD",
        "Quest item": "INVALID_QUEST_ITEM_ACTION",
        "State mutation has ambiguous target/dialog": "AMBIGUOUS_STATE_MUTATION_TARGET_OR_DIALOG",
        "defaultCloseDialog has ambiguous target/dialog": "AMBIGUOUS_DEFAULT_CLOSE_TARGET_OR_DIALOG",
        "defaultCloseDialog result is not returned": "UNCONSUMED_DEFAULT_CLOSE_RESULT",
        "defaultCloseDialog argument count is unsupported": "UNSUPPORTED_DEFAULT_CLOSE_ARGUMENTS",
        "defaultCloseDialog sameNpc dialog is unsupported": "UNSUPPORTED_DEFAULT_CLOSE_SAME_NPC_DIALOG",
        "defaultCloseDialog varNum is unsupported": "UNSUPPORTED_DEFAULT_CLOSE_VARIABLE_SLOT",
        "packed quest vars is not a constant integer": "DYNAMIC_PACKED_QUEST_VARIABLES",
        "Start helper arguments require unsupported capability": "UNSUPPORTED_START_HELPER_ARGUMENTS",
        "Unsupported quest variable expression": "UNSUPPORTED_QUEST_VARIABLE_EXPRESSION",
        "Mutation group has no proven action-only return": "UNPROVEN_ACTION_ONLY_RETURN",
        "Expected one dialog endpoint": "INVALID_DIALOG_ENDPOINT_GROUP",
        "Dialog page must be positive": "NON_POSITIVE_DIALOG_PAGE",
        "MISSING_HANDLER_AST": "MISSING_HANDLER_AST",
        "MISSING_QUEST_DATA": "MISSING_QUEST_DATA",
        "MISSING_NPC_REFERENCES": "MISSING_NPC_REFERENCES",
        "MISSING_ITEM_REFERENCES": "MISSING_ITEM_REFERENCES",
        "XSD_VALIDATION_FAILED": "XSD_VALIDATION_FAILED",
        "JAVA_COMPILER_REJECTED": "JAVA_COMPILER_REJECTION",
        "Unsupported sendPacket protocol endpoint": "UNSUPPORTED_PROTOCOL_ENDPOINT",
        "Unsupported SM_DIALOG_WINDOW target": "UNSUPPORTED_PROTOCOL_ENDPOINT",
        "Unbound getObjectId metadata": "UNBOUND_PROTOCOL_METADATA",
        "Unbound getQuestId metadata": "UNBOUND_PROTOCOL_METADATA",
    }
    for prefix, family in prefixes.items():
        if message.startswith(prefix):
            return family
    if "dialog targets are not registered" in message:
        return "UNREGISTERED_DIALOG_TARGET"
    if "generated empty non-terminal node" in message:
        return "EMPTY_NON_TERMINAL_NODE"
    if "reward group is not a constant integer" in message:
        return "NON_CONSTANT_REWARD_GROUP"
    return "COMPILER_REJECTION"


def build_dialog_shape_capability_report(shapes: dict[str, Any],
                                         compiler_readiness: dict[str, dict[str, Any]] | None = None) -> dict[str, Any]:
    rows = []
    missing_counts: Counter[str] = Counter()
    dialog_shapes = [shape for shape in shapes["shapes"] if shape["capability_family"] == "DIALOG"]
    for index, shape in enumerate(dialog_shapes):
        signature = shape["signature"]
        required = {"CONTROL_CONDITIONAL", "EVENT_DIALOG_ACTION", "EVENT_DIALOG_TARGET", "REFERENCE_NPC", "ROUTING_DIALOG"}
        signature_capabilities = []
        for category, field in (
            ("register", "register_calls"),
            ("helper", "helper_calls"),
            ("state_read", "state_reads"),
            ("state_write", "state_writes"),
            ("service", "service_calls"),
            ("direct", "direct_calls"),
        ):
            for call_signature in signature[field]:
                capability = dialog_signature_capability(category, call_signature)
                if capability is None:
                    continue
                required.add(capability)
                signature_capabilities.append({
                    "category": category,
                    "signature": call_signature,
                    "capability": capability,
                })
        normalized_controls = set(signature["controls"])
        if signature.get("control_methods", {}).get("ENHANCED_FOR") == ["register"]:
            normalized_controls.remove("ENHANCED_FOR")
            required.add("CONTROL_BOUNDED_REGISTRATION_LOOP")
        unsupported_controls = sorted(normalized_controls - {"ASSIGNMENT", "IF", "SWITCH"})
        required.update(f"UNMAPPED_CONTROL:{control}" for control in unsupported_controls)
        unsupported_overrides = sorted(set(signature.get("overrides", [])) - {"onDialogEvent", "register"})
        required.update(f"UNMAPPED_OVERRIDE:{override}" for override in unsupported_overrides)
        missing = sorted(required - DIALOG_IMPLEMENTED_CAPABILITIES)
        for capability in missing:
            missing_counts[capability] += shape["member_count"]
        capability_ready = not missing
        readiness = compiler_readiness.get(shape["fingerprint"]) if compiler_readiness is not None else None
        if not capability_ready:
            compiler_status = "NOT_CAPABILITY_READY"
        elif readiness is None:
            compiler_status = "NOT_ANALYZED"
        elif readiness["blocked_handlers"] == 0:
            compiler_status = "READY"
        elif readiness["ready_handlers"] == 0:
            compiler_status = "BLOCKED"
        else:
            compiler_status = "PARTIAL"
        failed_paths = {failure["path"] for failure in readiness["failures"]} if readiness is not None else set()
        generation_members = ([member for member in shape["members"] if member["path"] not in failed_paths]
                              if capability_ready and readiness is not None else [])
        rows.append({
            "rank": index + 1,
            "fingerprint": shape["fingerprint"],
            "member_count": shape["member_count"],
            "capability_ready": capability_ready,
            "compiler_status": compiler_status,
            "compiler_ready": compiler_status == "READY",
            "compiler_ready_handlers": readiness["ready_handlers"] if readiness is not None else 0,
            "compiler_blocked_handlers": readiness["blocked_handlers"] if readiness is not None else 0,
            "compiler_failures": readiness["failures"] if readiness is not None else [],
            "generation_selected": bool(generation_members),
            "generation_member_count": len(generation_members),
            "generation_members": generation_members,
            "required_capabilities": sorted(required),
            "missing_capabilities": missing,
            "signature_capabilities": sorted(signature_capabilities,
                                             key=lambda row: (row["category"], row["signature"])),
            "members": shape["members"],
        })
    selected = [row for row in rows if row["generation_selected"]]
    failure_families: dict[str, dict[str, Any]] = {}
    for row in rows:
        for failure in row["compiler_failures"]:
            family = failure["family"]
            aggregate = failure_families.setdefault(family, {
                "family": family,
                "handler_count": 0,
                "fingerprints": set(),
                "quest_ids": [],
            })
            aggregate["handler_count"] += 1
            aggregate["fingerprints"].add(row["fingerprint"])
            aggregate["quest_ids"].append(failure["quest_id"])
    failure_ranking = []
    for aggregate in failure_families.values():
        failure_ranking.append({
            "family": aggregate["family"],
            "handler_count": aggregate["handler_count"],
            "shape_count": len(aggregate["fingerprints"]),
            "fingerprints": sorted(aggregate["fingerprints"]),
            "quest_ids": sorted(aggregate["quest_ids"]),
        })
    failure_ranking.sort(key=lambda row: (-row["handler_count"], row["family"]))
    analyzed = compiler_readiness is not None
    return {
        **{key: shapes[key] for key in ("schema_version", "tool_version", "input_manifest_hash", "spec_id",
                                       "spec_revision", "spec_status")},
        "authority": ("CURRENT_HANDLER_AST_IMPLEMENTED_GRAPH_CAPABILITIES_AND_COMPILER"
                      if analyzed else "CURRENT_HANDLER_AST_AND_IMPLEMENTED_GRAPH_CAPABILITIES"),
        "classification": "DERIVED",
        "implemented_capabilities": sorted(DIALOG_IMPLEMENTED_CAPABILITIES),
        "counts": {
            "dialog_shapes": len(rows),
            "dialog_handlers": sum(row["member_count"] for row in rows),
            "capability_ready_shapes": sum(row["capability_ready"] for row in rows),
            "capability_ready_handlers": sum(row["member_count"] for row in rows if row["capability_ready"]),
            "compiler_analyzed_shapes": sum(row["compiler_status"] in {"READY", "PARTIAL", "BLOCKED"} for row in rows),
            "compiler_analyzed_handlers": sum(row["member_count"] for row in rows
                                               if row["compiler_status"] in {"READY", "PARTIAL", "BLOCKED"}),
            "fully_compiler_ready_shapes": sum(row["compiler_status"] == "READY" for row in rows),
            "handlers_in_fully_compiler_ready_shapes": sum(row["member_count"] for row in rows
                                                            if row["compiler_status"] == "READY"),
            "partially_compiler_ready_shapes": sum(row["compiler_status"] == "PARTIAL" for row in rows),
            "handlers_selected_from_partial_shapes": sum(row["generation_member_count"] for row in rows
                                                          if row["compiler_status"] == "PARTIAL"),
            "fully_compiler_blocked_shapes": sum(row["compiler_status"] == "BLOCKED" for row in rows),
            "compiler_ready_handlers": sum(row["compiler_ready_handlers"] for row in rows),
            "compiler_blocked_handlers": sum(row["compiler_blocked_handlers"] for row in rows),
            "generation_selected_shapes": len(selected),
            "generation_selected_handlers": sum(row["generation_member_count"] for row in selected),
        },
        "missing_capability_ranking": [
            {"capability": capability, "handler_count": count}
            for capability, count in sorted(missing_counts.items(), key=lambda item: (-item[1], item[0]))
        ],
        "compiler_failure_ranking": failure_ranking,
        "selected_shapes": selected,
        "shapes": rows,
        "migration_blockers": [],
        "blockers": [],
    }


def analyze_dialog_compiler_members(config: Config, inventory: InventoryBundle,
                                    shapes: dict[str, Any]) -> tuple[dict[str, Any], dict[str, dict[str, Any]]]:
    capability_report = build_dialog_shape_capability_report(shapes)
    capability_ready = [row for row in capability_report["shapes"] if row["capability_ready"]]
    rows_by_path = {row["path"]: row for row in inventory.java_rows}
    quest_ids = {member["quest_id"] for row in capability_ready for member in row["members"]
                 if member["quest_id"] is not None}
    quests = load_quest_evidence(config, quest_ids)
    npc_ai = load_npc_evidence(config)
    item_ids = load_item_ids(config)
    failures_by_shape: dict[str, list[dict[str, Any]]] = defaultdict(list)
    artifacts: dict[str, dict[str, Any]] = {}

    def reject(shape: dict[str, Any], member: dict[str, Any], message: str, stage: str) -> None:
        failures_by_shape[shape["fingerprint"]].append({
            "quest_id": member["quest_id"],
            "path": member["path"],
            "stage": stage,
            "family": dialog_compiler_error_family(message),
            "error": message,
        })

    for shape in capability_ready:
        for member in shape["members"]:
            quest_id = member["quest_id"]
            row = rows_by_path.get(member["path"])
            if row is None:
                reject(shape, member, "MISSING_HANDLER_AST", "COMPILER")
                continue
            quest = quests.get(quest_id)
            if quest is None:
                reject(shape, member, "MISSING_QUEST_DATA", "REFERENCE")
                continue
            try:
                graph, parity = compile_dialog_handler(row, shape["fingerprint"], quest, npc_ai)
            except AuditError as error:
                reject(shape, member, str(error), "COMPILER")
                continue
            npc_references = sorted({int(event.get("npc_id")) for event in graph.iter("dialog")})
            inventory_references = sorted({int(node.get("item_id")) for node in graph.iter("player-inventory")})
            action_item_references = graph_item_action_references(graph)
            missing_npcs = sorted(set(npc_references) - set(npc_ai))
            missing_items = sorted((set(quest["item_ids"]) | set(inventory_references)
                                    | set(action_item_references)) - item_ids)
            if missing_npcs:
                reject(shape, member, f"MISSING_NPC_REFERENCES:{missing_npcs}", "REFERENCE")
                continue
            if missing_items:
                reject(shape, member, f"MISSING_ITEM_REFERENCES:{missing_items}", "REFERENCE")
                continue
            artifacts[member["path"]] = {
                "fingerprint": shape["fingerprint"],
                "member": member,
                "quest": quest,
                "graph": graph,
                "parity": parity,
                "npc_references": npc_references,
                "inventory_references": inventory_references,
                "action_item_references": action_item_references,
            }

    schema_failures = validate_member_graph_schemas(config, list(artifacts.values()))
    shape_by_fingerprint = {shape["fingerprint"]: shape for shape in capability_ready}
    for path, message in schema_failures.items():
        artifact = artifacts.pop(path)
        reject(shape_by_fingerprint[artifact["fingerprint"]], artifact["member"], message, "XSD")

    compiler_failures = validate_member_graph_compiler(config, list(artifacts.values()))
    for path, message in compiler_failures.items():
        artifact = artifacts.pop(path)
        reject(shape_by_fingerprint[artifact["fingerprint"]], artifact["member"], message, "JAVA_COMPILER")

    readiness = {}
    for shape in capability_ready:
        failures = failures_by_shape[shape["fingerprint"]]
        ready_handlers = sum(member["path"] in artifacts for member in shape["members"])
        readiness[shape["fingerprint"]] = {
            "ready_handlers": ready_handlers,
            "blocked_handlers": len(failures),
            "failures": failures,
        }
    return build_dialog_shape_capability_report(shapes, readiness), artifacts


def analyze_dialog_shape_capabilities(config: Config, inventory: InventoryBundle,
                                      shapes: dict[str, Any]) -> dict[str, Any]:
    return analyze_dialog_compiler_members(config, inventory, shapes)[0]


def build_capability_dependency_graph(capability_report: dict[str, Any],
                                      inventory: InventoryBundle | None = None) -> dict[str, Any]:
    dependency_sets: dict[tuple[str, ...], dict[str, Any]] = {}
    node_handlers: Counter[str] = Counter()
    node_shapes: Counter[str] = Counter()
    single_unlocks: Counter[str] = Counter()
    edge_handlers: Counter[tuple[str, str]] = Counter()
    for shape in capability_report["shapes"]:
        missing = tuple(shape["missing_capabilities"])
        if not missing:
            continue
        aggregate = dependency_sets.setdefault(missing, {
            "capabilities": list(missing),
            "handler_count": 0,
            "shape_count": 0,
            "fingerprints": [],
        })
        aggregate["handler_count"] += shape["member_count"]
        aggregate["shape_count"] += 1
        aggregate["fingerprints"].append(shape["fingerprint"])
        for capability in missing:
            node_handlers[capability] += shape["member_count"]
            node_shapes[capability] += 1
        if len(missing) == 1:
            single_unlocks[missing[0]] += shape["member_count"]
        for index, left in enumerate(missing):
            for right in missing[index + 1:]:
                edge_handlers[(left, right)] += shape["member_count"]

    nodes = []
    for capability in sorted(node_handlers):
        co_required = []
        for (left, right), handler_count in edge_handlers.items():
            if capability == left:
                co_required.append({"capability": right, "handler_count": handler_count})
            elif capability == right:
                co_required.append({"capability": left, "handler_count": handler_count})
        co_required.sort(key=lambda row: (-row["handler_count"], row["capability"]))
        nodes.append({
            "capability": capability,
            "affected_handlers": node_handlers[capability],
            "affected_shapes": node_shapes[capability],
            "single_capability_unlock_handlers": single_unlocks[capability],
            "co_required": co_required,
        })
    nodes.sort(key=lambda row: (-row["single_capability_unlock_handlers"],
                               -row["affected_handlers"], row["capability"]))

    requirements = [(set(key), value["handler_count"]) for key, value in dependency_sets.items()]
    selected: set[str] = set()
    portfolio = []
    previously_unlocked = 0
    remaining = set(node_handlers)
    for step in range(1, min(10, len(remaining)) + 1):
        candidates = []
        for capability in remaining:
            proposed = selected | {capability}
            cumulative = sum(count for required, count in requirements if required <= proposed)
            immediate = cumulative - previously_unlocked
            progress = sum((count * 1000) // len(required - selected)
                           for required, count in requirements
                           if capability in required and not required <= selected)
            candidates.append((immediate, progress, node_handlers[capability], capability, cumulative))
        immediate, progress, affected, capability, cumulative = sorted(
            candidates, key=lambda row: (-row[0], -row[1], -row[2], row[3]))[0]
        selected.add(capability)
        remaining.remove(capability)
        portfolio.append({
            "step": step,
            "capability": capability,
            "projected_newly_capability_ready_handlers": immediate,
            "projected_cumulative_capability_ready_handlers": cumulative,
            "dependency_progress_score": progress,
            "affected_handlers": affected,
            "closing_set": sorted(selected),
        })
        previously_unlocked = cumulative

    counts = capability_report["counts"]
    total_owned_quests = (inventory.handler_report["counts"]["unique_owned_quests"]
                          if inventory is not None else None)
    return {
        **{key: capability_report[key] for key in ("schema_version", "tool_version", "input_manifest_hash",
                                                   "spec_id", "spec_revision", "spec_status")},
        "authority": "CURRENT_HANDLER_AST_CAPABILITY_HYPERGRAPH",
        "classification": "DERIVED_PLANNING_ONLY",
        "scope": DIALOG_STANDARD_FAMILY,
        "counts": {
            "total_owned_quests": total_owned_quests,
            "dialog_handlers": counts["dialog_handlers"],
            "capability_ready_handlers": counts["capability_ready_handlers"],
            "compiler_ready_handlers": counts["compiler_ready_handlers"],
            "generation_selected_handlers": counts["generation_selected_handlers"],
            "production_owner_switches": 0,
            "missing_capabilities": len(nodes),
            "dependency_sets": len(dependency_sets),
        },
        "nodes": nodes,
        "dependency_sets": sorted(dependency_sets.values(),
                                  key=lambda row: (-row["handler_count"], row["capabilities"])),
        "edges": [{"left": left, "right": right, "handler_count": count}
                  for (left, right), count in sorted(edge_handlers.items(),
                                                     key=lambda item: (-item[1], item[0]))],
        "recommended_portfolio": portfolio,
        "known_compiler_blockers": capability_report["compiler_failure_ranking"],
        "blockers": [],
    }


def call_integer(call: dict[str, Any], index: int, label: str) -> int:
    values = call.get("argument_values", [])
    value = values[index] if index < len(values) else None
    if type(value) is int:
        return value
    arguments = call.get("arguments", [])
    raw = arguments[index].strip() if index < len(arguments) else ""
    if re.fullmatch(r"-?\d+", raw):
        return int(raw)
    raise AuditError(f"{label} is not a constant integer at line {call.get('line')}: {raw}")


def call_integers(call: dict[str, Any], index: int, label: str) -> list[int]:
    value_sets = call.get("argument_value_sets", [])
    if index < len(value_sets) and value_sets[index]:
        return sorted(set(value_sets[index]))
    return [call_integer(call, index, label)]


def call_boolean(call: dict[str, Any], index: int, label: str) -> bool:
    arguments = call.get("arguments", [])
    raw = arguments[index].strip() if index < len(arguments) else ""
    if raw == "true":
        return True
    if raw == "false":
        return False
    raise AuditError(f"{label} is not a constant boolean at line {call.get('line')}: {raw}")


def control_phase(path: tuple[str, ...]) -> str | None:
    true_controls = "\n".join(part for part in path if part.startswith("IF_TRUE:"))
    if "QuestStatus.REWARD" in true_controls:
        return "reward"
    if "QuestStatus.START" in true_controls:
        return "active"
    if "QuestStatus.NONE" in true_controls or re.search(r"\bqs\s*==\s*null\b", true_controls):
        return "offer"
    return None


def control_targets(path: tuple[str, ...]) -> list[int]:
    targets = set()
    for part in path:
        if part.startswith("IF_TRUE:"):
            targets.update(int(value) for value in TARGET_EQUALITY.findall(part))
        elif part.startswith("SWITCH:") and any(marker in part for marker in
                                                  ("targetId", "npcId", "getTargetId", "getNpcId")):
            for value in part.rsplit("=", 1)[-1].split("|"):
                if value.isdigit():
                    targets.add(int(value))
    return sorted(targets)


def control_dialogs(path: tuple[str, ...]) -> list[str]:
    dialogs = set()
    for part in path:
        if part.startswith("SWITCH:") and "dialog" in part.lower():
            dialogs.update(value for value in part.rsplit("=", 1)[-1].split("|") if value != "default")
        elif part.startswith("IF_TRUE:") and "QuestDialog." in part:
            dialogs.update(DIALOG_EQUALITY.findall(part))
        elif part.startswith("IF_TRUE:") and "getDialogId()" in part:
            for value in DIALOG_ID_EQUALITY.findall(part):
                name = DIALOG_ID_NAMES.get(int(value))
                if name is not None:
                    dialogs.add(name)
    return sorted(dialogs)


def control_excludes_dialog(path: tuple[str, ...], dialog: str) -> bool:
    return any(part.startswith("IF_FALSE:") and f"QuestDialog.{dialog}" in part for part in path) \
        or any(part.startswith("SWITCH:") and "dialog" in part.lower() and part.endswith("=default") for part in path)


def inventory_condition(path: tuple[str, ...]) -> dict[str, Any] | None:
    conditions = inventory_conditions(path)
    return conditions[0] if conditions else None


def inventory_conditions(path: tuple[str, ...]) -> list[dict[str, Any]]:
    result = []
    for part in path:
        if not part.startswith("IF_TRUE:"):
            continue
        for match in INVENTORY_COMPARISON.finditer(part):
            operation = {
                ">=": "GREATER_EQUAL",
                "<=": "LESSER_EQUAL",
                "==": "EQUAL",
                "!=": "NOT_EQUAL",
                ">": "GREATER",
                "<": "LESSER",
            }[match.group(2)]
            result.append({"tag": "player-inventory", "attrs": {
                "item_id": str(int(match.group(1))),
                "op": operation,
                "count": str(int(match.group(3))),
            }})
    return result


def quest_variable_conditions(path: tuple[str, ...], variables: set[str]) -> list[dict[str, Any]]:
    result = []
    for part in path:
        if not part.startswith("IF_TRUE:"):
            continue
        for match in QUEST_VARIABLE_COMPARISON.finditer(part):
            name = variable_name(int(match.group(1)))
            variables.add(name)
            operation = {
                ">=": "GREATER_EQUAL",
                "<=": "LESSER_EQUAL",
                "==": "EQUAL",
                "!=": "NOT_EQUAL",
                ">": "GREATER",
                "<": "LESSER",
            }[match.group(2)]
            result.append(condition("quest-variable", variable=name, op=operation, value=int(match.group(3))))
    return result


def merge_conditions(*groups: Iterable[dict[str, Any]]) -> list[dict[str, Any]]:
    result = []
    seen = set()
    for group in groups:
        for value in group:
            key = (value["tag"], tuple(sorted(value["attrs"].items())))
            if key not in seen:
                seen.add(key)
                result.append(value)
    return result


def compiled_path_conditions(path: tuple[str, ...], variables: set[str]) -> list[dict[str, Any]]:
    return merge_conditions(quest_variable_conditions(path, variables), inventory_conditions(path))


def action_only_path_is_closed(path: tuple[str, ...]) -> bool:
    for part in path:
        if part.startswith("SWITCH:"):
            if control_targets((part,)) or control_dialogs((part,)):
                continue
            return False
        if not (part.startswith("IF_TRUE:") or part.startswith("IF_FALSE:")):
            return False
        expression = part.split(":", 1)[1]
        if part.startswith("IF_FALSE:"):
            if "QuestStatus." in expression or re.search(r"\bqs\s*(?:==|!=)\s*null\b", expression):
                continue
            return False
        if "QuestStatus." in expression or re.search(r"\bqs\s*(?:==|!=)\s*null\b", expression) \
                or ".canRepeat()" in expression:
            continue
        if control_targets((part,)) or control_dialogs((part,)):
            continue
        if INVENTORY_COMPARISON.search(expression) and "&&" not in expression and "||" not in expression:
            continue
        if QUEST_VARIABLE_COMPARISON.search(expression) and "&&" not in expression and "||" not in expression:
            continue
        return False
    return True


def has_explicit_boolean_return(boolean_returns: Iterable[dict[str, Any]], path: tuple[str, ...],
                                value: bool, after_position: int) -> bool:
    return any(returned.get("value") is value
               and tuple(returned.get("control_path", [])) == path
               and returned.get("source_position", -1) > after_position
               for returned in boolean_returns)


def sufficient_remove_guard(path: tuple[str, ...], item_id: int, count: int) -> dict[str, Any] | None:
    for value in inventory_conditions(path):
        attrs = value["attrs"]
        if int(attrs["item_id"]) != item_id:
            continue
        threshold = int(attrs["count"])
        if attrs["op"] == "EQUAL" and threshold >= count \
                or attrs["op"] == "GREATER_EQUAL" and threshold >= count \
                or attrs["op"] == "GREATER" and threshold + 1 >= count:
            return value
    return None


def conditional_give_success_branch(call: dict[str, Any]) -> str | None:
    if call.get("result_usage") != "CONDITION":
        return None
    arguments = ",".join(re.sub(r"\s+", "", value) for value in call.get("arguments", []))
    invocation = f"{call.get('select', call.get('method', 'giveQuestItem'))}({arguments})"
    context = re.sub(r"\s+", "", call.get("result_context", ""))
    if context in {invocation, f"({invocation})"}:
        return "TRUE"
    if context in {f"!{invocation}", f"(!{invocation})"}:
        return "FALSE"
    return None


def condition_give_matches_success_path(call: dict[str, Any], path: tuple[str, ...]) -> bool:
    branch = conditional_give_success_branch(call)
    base = tuple(call.get("control_path", []))
    marker = f"IF_{branch}:{call.get('result_context', '')}"
    if branch is not None and path == base + (marker,):
        return True
    return branch == "FALSE" and path == base \
        and call.get("condition_true_outcome", "NONE").startswith("RETURN_") \
        and call.get("condition_false_outcome") == "FALLTHROUGH"


def fold_positive_condition_gives(groups: dict[tuple[str, ...], list[dict[str, Any]]]) -> None:
    for path, calls in list(groups.items()):
        if len(calls) != 1:
            continue
        give = calls[0]
        if give.get("method") != "giveQuestItem" or conditional_give_success_branch(give) != "TRUE":
            continue
        if not give.get("condition_true_outcome", "NONE").startswith("RETURN_") \
                or give.get("condition_false_outcome") != "FALLTHROUGH":
            continue
        success_path = path + (f"IF_TRUE:{give.get('result_context', '')}",)
        if success_path not in groups or any(candidate.get("source_position", candidate["line"])
                                              <= give.get("source_position", give["line"])
                                              for candidate in groups[success_path]):
            continue
        groups[success_path].append(give)
        del groups[path]


def graph_item_action_references(graph: ET.Element | None) -> list[int]:
    if graph is None:
        return []
    return sorted({int(node.get("item_id")) for tag in ("give-quest-item", "remove-quest-item")
                   for node in graph.iter(tag)})


def player_message(call: dict[str, Any]) -> dict[str, Any]:
    arguments = call.get("arguments", [])
    source = next((argument for argument in arguments if "new SM_MESSAGE" in argument), "")
    match = MESSAGE_CONSTRUCTOR.search(source)
    if match is None:
        raise AuditError(f"Unsupported player message at line {call.get('line')}: {source}")
    try:
        text = json.loads(match.group(1))
    except json.JSONDecodeError as error:
        raise AuditError(f"Invalid player message literal at line {call.get('line')}: {error}") from error
    if match.group(2) != "BRIGHT_YELLOW_CENTER":
        raise AuditError(f"Unsupported player message channel at line {call.get('line')}: {match.group(2)}")
    return {"tag": "send-player-message", "attrs": {"text": text, "channel": match.group(2)}}


def dialog_window_packet_action(call: dict[str, Any]) -> dict[str, Any]:
    arguments = call.get("arguments", [])
    source = arguments[1].strip() if len(arguments) > 1 else ""
    match = DIALOG_WINDOW_PACKET.fullmatch(source)
    if match is None:
        raise AuditError(f"Unsupported sendPacket protocol endpoint at line {call.get('line')}: {source}")
    target = re.sub(r"\s+", "", match.group(1))
    if target != "env.getVisibleObject().getObjectId()":
        raise AuditError(f"Unsupported SM_DIALOG_WINDOW target at line {call.get('line')}: {match.group(1)}")
    dialog_id = int(match.group(2))
    return action("close-dialog") if dialog_id == 0 else send_dialog_action(dialog_id, "SM_DIALOG_WINDOW")


def validate_metadata_calls(calls: list[dict[str, Any]]) -> list[dict[str, Any]]:
    metadata_methods = {"getObjectId", "getQuestId"}
    metadata = [call for call in calls if call["method"] in metadata_methods]
    actions = [call for call in calls if call["method"] not in metadata_methods]
    for call in metadata:
        context = call.get("result_context", "")
        if call.get("result_usage") != "ARGUMENT" \
                or not any(candidate.get("result_context", "") == context for candidate in actions):
            raise AuditError(f"Unbound {call['method']} metadata at line {call.get('line')}")
    return actions


def condition(tag: str, **attrs: Any) -> dict[str, Any]:
    return {"tag": tag, "attrs": {key: str(value).lower() if isinstance(value, bool) else str(value)
                                     for key, value in attrs.items()}}


def action(tag: str, **attrs: Any) -> dict[str, Any]:
    return condition(tag, **attrs)


def send_dialog_action(dialog_id: int, label: str) -> dict[str, Any]:
    if dialog_id <= 0:
        raise AuditError(f"Dialog page must be positive for {label}: {dialog_id}")
    return action("send-dialog", dialog_id=dialog_id)


def status_condition(status: str) -> dict[str, Any]:
    return condition("quest-status", op="IN", values=status.upper())


def repeat_condition(quest: dict[str, Any], expected: bool) -> dict[str, Any]:
    return condition("quest-repeat-available", max_completions=quest["max_repeat_count"],
                     requires_deadline=quest["requires_deadline"], expected=expected)


def registration_targets(row: dict[str, Any]) -> dict[str, list[int]]:
    calls_by_site: dict[tuple[int, str], list[dict[str, Any]]] = defaultdict(list)
    for call in row["calls"]:
        if call["enclosing_method"] == "register":
            calls_by_site[(call["line"], call.get("result_context", ""))].append(call)
    result = {"start": [], "talk": []}
    for (line, _), calls in calls_by_site.items():
        registrations = [call for call in calls if call["method"] == "registerQuestNpc"]
        if not registrations:
            continue
        npc_ids = {npc_id for call in registrations for npc_id in call_integers(call, 0, "registered NPC")}
        for event_method, kind in (("addOnQuestStart", "start"), ("addOnTalkEvent", "talk")):
            if any(call["method"] == event_method for call in calls):
                if not npc_ids:
                    raise AuditError(f"Ambiguous {event_method} NPC registration at {row['path']}:{line}")
                result[kind].extend(npc_ids)
    return {kind: sorted(set(values)) for kind, values in result.items()}


def parse_repeat_deadline_policy(element: ET.Element) -> dict[str, Any] | None:
    quest_id = int(element.get("id", "0"))
    repeat_tokens = element.get("repeat_cycle", "").split()
    try:
        cooldown_seconds = int(element.get("quest_cooltime", "0"))
    except ValueError as error:
        raise AuditError(f"Quest {quest_id} has invalid quest_cooltime") from error
    if cooldown_seconds < 0:
        raise AuditError(f"Quest {quest_id} has negative quest_cooltime")
    if repeat_tokens and cooldown_seconds > 0:
        raise AuditError(f"Quest {quest_id} combines repeat_cycle and quest_cooltime")
    if cooldown_seconds > 0:
        return {
            "repeat_kind": "ANCHORED_COOLDOWN",
            "time_basis": "SERVER_LOCAL",
            "reset_hour": 9,
            "cooldown_seconds": cooldown_seconds,
        }
    if not repeat_tokens:
        return None
    valid_weekdays = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"}
    if repeat_tokens == ["ALL"]:
        return {"repeat_kind": "DAILY", "time_basis": "SERVER_LOCAL", "reset_hour": 9}
    if "ALL" in repeat_tokens or any(token not in valid_weekdays for token in repeat_tokens):
        raise AuditError(f"Quest {quest_id} has unknown/conflicting repeat_cycle {repeat_tokens}")
    if len(set(repeat_tokens)) != len(repeat_tokens):
        raise AuditError(f"Quest {quest_id} has duplicate repeat weekdays {repeat_tokens}")
    return {
        "repeat_kind": "WEEKLY",
        "time_basis": "SERVER_LOCAL",
        "reset_hour": 9,
        "weekdays": " ".join(repeat_tokens),
    }


def build_repeat_policy_report(config: Config) -> dict[str, Any]:
    path = config.project_root / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml"
    if not path.is_file():
        raise AuditError(f"Quest data does not exist: {path}")
    policies = []
    errors = []
    kind_counts: Counter[str] = Counter()
    cooldown_counts: Counter[int] = Counter()
    quest_count = 0
    for element in ET.parse(path).getroot():
        if local_name(element.tag) != "quest" or not element.get("id"):
            continue
        quest_count += 1
        quest_id = int(element.get("id"))
        try:
            policy = parse_repeat_deadline_policy(element)
        except AuditError as error:
            errors.append({"quest_id": quest_id, "error": str(error)})
            continue
        if policy is None:
            continue
        policies.append({"quest_id": quest_id, **policy})
        kind_counts[policy["repeat_kind"]] += 1
        if policy["repeat_kind"] == "ANCHORED_COOLDOWN":
            cooldown_counts[policy["cooldown_seconds"]] += 1
    return {
        **base_report(content_hash(path.read_bytes())),
        "authority": "CURRENT_QUEST_DATA_PLUS_CURRENT_QUEST_SERVICE_ALGORITHM",
        "classification": "DERIVED",
        "counts": {
            "quests": quest_count,
            "time_based_quests": len(policies),
            "daily": kind_counts["DAILY"],
            "weekly": kind_counts["WEEKLY"],
            "anchored_cooldown": kind_counts["ANCHORED_COOLDOWN"],
            "unknown_or_conflicting": len(errors),
        },
        "cooldown_seconds": [{"seconds": seconds, "quests": count} for seconds, count in sorted(cooldown_counts.items())],
        "policies": policies,
        "errors": errors,
        "blockers": ([{"kind": "UNKNOWN_OR_CONFLICTING_REPEAT_POLICY", "count": len(errors)}] if errors else []),
    }


def load_quest_evidence(config: Config, quest_ids: set[int]) -> dict[int, dict[str, Any]]:
    path = config.project_root / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml"
    if not path.is_file():
        raise AuditError(f"Quest data does not exist: {path}")

    def build() -> list[dict[str, Any]]:
        result = []
        for element in ET.parse(path).getroot():
            if local_name(element.tag) != "quest" or not element.get("id"):
                continue
            quest_id = int(element.get("id"))
            collect_items = sorted({int(child.get("item_id")) for child in element.iter()
                                    if local_name(child.tag) == "collect_item" and child.get("item_id")})
            work_items = sorted({int(child.get("item_id")) for child in element.iter()
                                 if local_name(child.tag) == "quest_work_item" and child.get("item_id")})
            item_ids = sorted({int(child.get("item_id")) for child in element.iter() if child.get("item_id")})
            rewards = [child for child in element if local_name(child.tag) == "rewards"]
            max_repeat_count = int(element.get("max_repeat_count", "1"))
            repeat_policy = parse_repeat_deadline_policy(element)
            result.append({
                "quest_id": quest_id,
                "max_repeat_count": max_repeat_count,
                "requires_deadline": repeat_policy is not None,
                "repeat_policy": repeat_policy,
                "collect_item_ids": collect_items,
                "work_item_ids": work_items,
                "item_ids": item_ids,
                "reward_groups": len(rewards),
            })
        return result

    rows = cached_json(config, "quest-evidence", file_hash(path), build, QUEST_EVIDENCE_CACHE_VERSION)
    return {row["quest_id"]: row for row in rows if row["quest_id"] in quest_ids}


def load_npc_evidence(config: Config) -> dict[int, str]:
    path = config.project_root / "src/main/resources/aion/data/static_data/npcs/npc_template.xml"
    if not path.is_file():
        raise AuditError(f"NPC data does not exist: {path}")
    def build() -> list[list[Any]]:
        result = []
        for _, element in ET.iterparse(path, events=("end",)):
            if local_name(element.tag) == "npc_template" and element.get("npc_id"):
                result.append([int(element.get("npc_id")), element.get("ai", "")])
            element.clear()
        result.sort(key=lambda value: value[0])
        return result

    return {int(npc_id): ai for npc_id, ai in cached_json(
        config, "npc-index", file_hash(path), build, NPC_INDEX_CACHE_VERSION,
    )}


def load_item_ids(config: Config) -> set[int]:
    root = config.project_root / "src/main/resources/aion/data/static_data/items/item"
    paths = sorted(root.glob("*.xml"))
    if not paths:
        raise AuditError(f"Item template data does not exist: {root}")
    _, input_hash = input_manifest({"item-templates": (root, paths)})

    def build() -> list[int]:
        result = set()
        for path in paths:
            for _, element in ET.iterparse(path, events=("end",)):
                if local_name(element.tag) == "item_template" and element.get("id"):
                    result.add(int(element.get("id")))
                element.clear()
        return sorted(result)

    return set(cached_json(config, "item-index", input_hash, build, ITEM_INDEX_CACHE_VERSION))


def plan_transition(plans: list[dict[str, Any]], node: str, target: str, npc_id: int, dialog: str,
                    conditions: list[dict[str, Any]], actions: list[dict[str, Any]], call: dict[str, Any], kind: str,
                    registration: str, order: int = 0) -> None:
    if npc_id <= 0 or not dialog or not actions:
        raise AuditError(f"Invalid {kind} transition at line {call.get('line')}")
    plans.append({
        "node": node,
        "target": target,
        "npc_id": npc_id,
        "dialog": dialog,
        "conditions": conditions,
        "actions": actions,
        "source_line": call.get("line", 0),
        "source_method": call["method"],
        "kind": kind,
        "registration": registration,
        "order": order,
    })


def endpoint_actions(calls: list[dict[str, Any]], dialog: str) -> list[dict[str, Any]]:
    result = []
    pages = [call for call in calls if call["method"] == "sendQuestDialog"]
    closes = [call for call in calls if call["method"] == "closeDialogWindow"]
    ends = [call for call in calls if call["method"] == "sendQuestEndDialog"]
    selections = [call for call in calls if call["method"] == "sendQuestSelectionDialog"]
    packets = [call for call in calls if call["method"] == "sendPacket"]
    if len(pages) + len(closes) + len(ends) + len(selections) + len(packets) != 1:
        raise AuditError(f"Expected one dialog endpoint, got {[call['method'] for call in calls]}")
    if pages:
        result.append(send_dialog_action(call_integer(pages[0], 1, "dialog page"), "dialog endpoint"))
    elif closes:
        result.append(action("close-dialog"))
    elif selections:
        result.append(action("show-quest-list"))
    elif packets:
        result.append(dialog_window_packet_action(packets[0]))
    elif dialog == "SELECT_REWARD":
        result.append(send_dialog_action(5, "reward selection"))
    else:
        raise AuditError(f"sendQuestEndDialog cannot handle active dialog {dialog}")
    return result


def variable_name(index: int) -> str:
    if index < 0 or index > 5:
        raise AuditError(f"Quest variable index is outside legacy slots: {index}")
    return f"var{index}"


def packed_quest_variable_actions(call: dict[str, Any], variables: set[str]) -> list[dict[str, Any]]:
    packed = call_integer(call, 0, "packed quest vars")
    result = []
    for index in range(6):
        name = variable_name(index)
        variables.add(name)
        result.append(action("set-quest-variable", variable=name, value=packed & 0x3F))
        packed >>= 6
    return result


def compile_mutation_group(plans: list[dict[str, Any]], calls: list[dict[str, Any]], path: tuple[str, ...],
                           variables: set[str], boolean_returns: Iterable[dict[str, Any]] = ()) -> None:
    if control_phase(path) != "active":
        raise AuditError(f"State mutation is outside START at line {calls[0].get('line')}")
    targets = control_targets(path)
    dialogs = control_dialogs(path)
    if not targets or len(dialogs) != 1:
        raise AuditError(f"State mutation has ambiguous target/dialog at line {calls[0].get('line')}")
    dialog = dialogs[0]
    conditions = merge_conditions([status_condition("START")], compiled_path_conditions(path, variables))
    state_actions = []
    item_actions = []
    becomes_reward = False
    change_calls = [call for call in calls if call["method"] == "changeQuestStep"]
    if len(change_calls) > 1:
        raise AuditError(f"Multiple changeQuestStep calls at line {calls[0].get('line')}")
    if change_calls:
        change = change_calls[0]
        step = call_integer(change, 1, "current quest step")
        next_step = call_integer(change, 2, "next quest step")
        reward = change["arguments"][3].strip() == "true"
        index = call_integer(change, 4, "quest variable") if len(change["arguments"]) > 4 else 0
        name = variable_name(index)
        variables.add(name)
        conditions.append(condition("quest-variable", variable=name, op="EQUAL", value=step))
        if reward:
            state_actions.append(action("set-quest-status", status="REWARD"))
            becomes_reward = True
        elif next_step != step:
            state_actions.append(action("set-quest-variable", variable=name, value=next_step))
    set_variable_calls = [call for call in calls if call["method"] == "setQuestVarById"]
    if len(set_variable_calls) > 1:
        raise AuditError(f"Multiple setQuestVarById calls at line {calls[0].get('line')}")
    if set_variable_calls:
        set_call = set_variable_calls[0]
        index = call_integer(set_call, 0, "quest variable")
        name = variable_name(index)
        variables.add(name)
        expression = set_call["arguments"][1].strip()
        increment = re.fullmatch(rf"\(?\s*qs\.getQuestVarById\({index}\)\s*\)?\s*\+\s*(-?\d+)", expression)
        if increment:
            state_actions.append(action("add-quest-variable", variable=name, delta=int(increment.group(1))))
        elif re.fullmatch(r"-?\d+", expression):
            state_actions.append(action("set-quest-variable", variable=name, value=int(expression)))
        else:
            raise AuditError(f"Unsupported quest variable expression at line {set_call.get('line')}: {expression}")
    packed_variable_calls = [call for call in calls if call["method"] == "setQuestVar"]
    if len(packed_variable_calls) > 1:
        raise AuditError(f"Multiple setQuestVar calls at line {calls[0].get('line')}")
    if packed_variable_calls:
        if change_calls or set_variable_calls:
            raise AuditError(f"Packed quest variable assignment is mixed with another variable write at line "
                             f"{packed_variable_calls[0].get('line')}")
        state_actions.extend(packed_quest_variable_actions(packed_variable_calls[0], variables))
    statuses = [call for call in calls if call["method"] == "setStatus"]
    if statuses:
        if len(statuses) != 1 or statuses[0]["arguments"] != ["QuestStatus.REWARD"] or becomes_reward:
            raise AuditError(f"Unsupported explicit quest status mutation at line {statuses[0].get('line')}")
        state_actions.append(action("set-quest-status", status="REWARD"))
        becomes_reward = True
    state_positions = [call.get("source_position", call["line"]) for call in calls
                       if call["method"] in {"changeQuestStep", "setQuestVar", "setQuestVarById", "setStatus"}]
    for item_call in sorted((call for call in calls if call["method"] in {"giveQuestItem", "removeQuestItem"}),
                            key=lambda call: call.get("source_position", call["line"])):
        item_id = call_integer(item_call, 1, "quest item id")
        count = call_integer(item_call, 2, "quest item count")
        if item_id <= 0 or count <= 0:
            raise AuditError(f"Quest item id/count must be positive at line {item_call.get('line')}")
        if item_call["method"] == "giveQuestItem":
            if item_call.get("result_usage") == "IGNORED":
                raise AuditError(f"Ignored giveQuestItem has no explicit failure edge at line {item_call.get('line')}")
            if item_call.get("result_usage") == "CONDITION" and not condition_give_matches_success_path(item_call, path):
                raise AuditError(f"Quest item give result requires an exact success branch at line {item_call.get('line')}")
            if item_call.get("result_usage") not in {"RETURNED", "CONDITION"}:
                raise AuditError(f"Quest item give result requires an exact success branch at line {item_call.get('line')}")
            if state_positions and item_call.get("source_position", item_call["line"]) > min(state_positions):
                raise AuditError(f"Quest item give occurs after a state mutation at line {item_call.get('line')}")
            item_actions.append(action("give-quest-item", item_id=item_id, count=count, mode="TOP_UP_TO"))
        else:
            guard = sufficient_remove_guard(path, item_id, count)
            if guard is None:
                raise AuditError(f"removeQuestItem lacks a sufficient inventory guard at line {item_call.get('line')}")
            conditions = merge_conditions(conditions, [guard])
            item_actions.append(action("remove-quest-item", item_id=item_id, count=count, mode="EXACT"))
    if not state_actions and not item_actions:
        raise AuditError(f"Mutation group has no state or item write at line {calls[0].get('line')}")
    endpoints = [call for call in calls
                 if call["method"] in {"sendQuestDialog", "closeDialogWindow", "sendQuestEndDialog",
                                       "sendQuestSelectionDialog", "sendPacket"}]
    if state_actions and not endpoints:
        last_position = max(call.get("source_position", call["line"]) for call in calls)
        if not action_only_path_is_closed(path) \
                or not has_explicit_boolean_return(boolean_returns, path, True, last_position):
            raise AuditError(f"Mutation group has no proven action-only return at line {calls[0].get('line')}")
    if not state_actions and not endpoints and any(call.get("result_usage") != "RETURNED"
                                                   for call in calls if call["method"] in {"giveQuestItem", "removeQuestItem"}):
        raise AuditError(f"Quest item mutation has no explicit endpoint at line {calls[0].get('line')}")
    protocol = ([action("sync-quest-status")] if state_actions else [])
    if endpoints:
        protocol.extend(endpoint_actions(calls, dialog))
    for target in targets:
        plan_transition(plans, "active", "reward" if becomes_reward else "active", target, dialog,
                        list(conditions), state_actions + item_actions + protocol, calls[0], "advance", "talk")


def default_close_arguments(call: dict[str, Any]) -> dict[str, Any]:
    arity = len(call.get("arguments", []))
    if arity not in {3, 5, 6, 7, 9, 10, 11}:
        raise AuditError(f"defaultCloseDialog argument count is unsupported at line {call.get('line')}: {arity}")
    result = {
        "step": call_integer(call, 1, "defaultCloseDialog step"),
        "next_step": call_integer(call, 2, "defaultCloseDialog next step"),
        "reward": False,
        "same_npc": False,
        "reward_id": 0,
        "give_item_id": 0,
        "give_item_count": 0,
        "remove_item_id": 0,
        "remove_item_count": 0,
        "var_num": 0,
    }
    if arity in {5, 6, 9, 10, 11}:
        result["reward"] = call_boolean(call, 3, "defaultCloseDialog reward")
        result["same_npc"] = call_boolean(call, 4, "defaultCloseDialog sameNpc")
    if arity == 6:
        result["reward_id"] = call_integer(call, 5, "defaultCloseDialog reward id")
    elif arity == 7:
        result.update({
            "give_item_id": call_integer(call, 3, "defaultCloseDialog give item id"),
            "give_item_count": call_integer(call, 4, "defaultCloseDialog give item count"),
            "remove_item_id": call_integer(call, 5, "defaultCloseDialog remove item id"),
            "remove_item_count": call_integer(call, 6, "defaultCloseDialog remove item count"),
        })
    elif arity == 9:
        result.update({
            "give_item_id": call_integer(call, 5, "defaultCloseDialog give item id"),
            "give_item_count": call_integer(call, 6, "defaultCloseDialog give item count"),
            "remove_item_id": call_integer(call, 7, "defaultCloseDialog remove item id"),
            "remove_item_count": call_integer(call, 8, "defaultCloseDialog remove item count"),
        })
    elif arity in {10, 11}:
        result.update({
            "reward_id": call_integer(call, 5, "defaultCloseDialog reward id"),
            "give_item_id": call_integer(call, 6, "defaultCloseDialog give item id"),
            "give_item_count": call_integer(call, 7, "defaultCloseDialog give item count"),
            "remove_item_id": call_integer(call, 8, "defaultCloseDialog remove item id"),
            "remove_item_count": call_integer(call, 9, "defaultCloseDialog remove item count"),
        })
        if arity == 11:
            result["var_num"] = call_integer(call, 10, "defaultCloseDialog quest variable")
    return result


def compile_default_close_group(plans: list[dict[str, Any]], calls: list[dict[str, Any]], path: tuple[str, ...],
                                variables: set[str], npc_ai: dict[int, str]) -> None:
    helpers = [call for call in calls if call["method"] == "defaultCloseDialog"]
    if len(helpers) != 1:
        raise AuditError(f"Ambiguous defaultCloseDialog group at line {calls[0].get('line')}")
    helper = helpers[0]
    helper_position = helper.get("source_position", helper["line"])
    prefix = [call for call in calls if call is not helper
              and call.get("source_position", call["line"]) < helper_position]
    suffix = [call for call in calls if call is not helper
              and call.get("source_position", call["line"]) >= helper_position]
    if suffix or any(call["method"] not in {"setQuestVar", "setQuestVarById", "giveQuestItem", "removeQuestItem"}
                     for call in prefix):
        raise AuditError(f"Ambiguous defaultCloseDialog group at line {calls[0].get('line')}")
    if helper.get("result_usage") != "RETURNED":
        raise AuditError(f"defaultCloseDialog result is not returned at line {helper.get('line')}")
    if control_phase(path) != "active":
        raise AuditError(f"defaultCloseDialog is outside START at line {helper.get('line')}")
    targets = control_targets(path)
    dialogs = control_dialogs(path)
    if not targets or len(dialogs) != 1:
        raise AuditError(f"defaultCloseDialog has ambiguous target/dialog at line {helper.get('line')}")
    values = default_close_arguments(helper)
    if values["var_num"] != 0:
        raise AuditError(f"defaultCloseDialog varNum is unsupported at line {helper.get('line')}: "
                         f"{values['var_num']}")

    prefix_state_actions = []
    prefix_item_actions = []
    prefix_conditions = []
    known_variables: dict[str, int] = {}
    for call in prefix:
        if call["method"] == "setQuestVar":
            packed_actions = packed_quest_variable_actions(call, variables)
            prefix_state_actions.extend(packed_actions)
            known_variables.update({value["attrs"]["variable"]: int(value["attrs"]["value"])
                                    for value in packed_actions})
            continue
        if call["method"] in {"giveQuestItem", "removeQuestItem"}:
            item_id = call_integer(call, 1, "quest item id")
            count = call_integer(call, 2, "quest item count")
            if item_id <= 0 or count <= 0:
                raise AuditError(f"Quest item id/count must be positive at line {call.get('line')}")
            if call["method"] == "giveQuestItem":
                if call.get("result_usage") == "IGNORED":
                    raise AuditError(f"Ignored giveQuestItem has no explicit failure edge at line {call.get('line')}")
                if call.get("result_usage") != "CONDITION" or not condition_give_matches_success_path(call, path):
                    raise AuditError(f"Quest item give result requires an exact success branch at line {call.get('line')}")
                prefix_item_actions.append(action("give-quest-item", item_id=item_id, count=count, mode="TOP_UP_TO"))
            else:
                guard = sufficient_remove_guard(path, item_id, count)
                if guard is None:
                    raise AuditError(f"removeQuestItem lacks a sufficient inventory guard at line {call.get('line')}")
                if guard not in prefix_conditions:
                    prefix_conditions.append(guard)
                prefix_item_actions.append(action("remove-quest-item", item_id=item_id, count=count, mode="EXACT"))
            continue
        index = call_integer(call, 0, "quest variable")
        name = variable_name(index)
        raw_value = call.get("arguments", ["", ""])[1].strip()
        if not re.fullmatch(r"-?\d+", raw_value):
            raise AuditError(f"Unsupported quest variable expression at line {call.get('line')}: {raw_value}")
        value = int(raw_value)
        variables.add(name)
        known_variables[name] = value
        prefix_state_actions.append(action("set-quest-variable", variable=name, value=value))

    name = variable_name(0)
    variables.add(name)
    conditions = [status_condition("START"), *prefix_conditions]
    known_step = known_variables.get(name)
    if known_step is None:
        conditions.append(condition("quest-variable", variable=name, op="EQUAL", value=values["step"]))
    elif known_step != values["step"]:
        raise AuditError(f"defaultCloseDialog prefix sets {name} to {known_step}, expected {values['step']} "
                         f"at line {helper.get('line')}")
    item_actions = []
    if values["give_item_id"] != 0 and values["give_item_count"] != 0:
        if values["give_item_id"] < 0 or values["give_item_count"] < 0:
            raise AuditError(f"Quest item id/count must be positive at line {helper.get('line')}")
        item_actions.append(action("give-quest-item", item_id=values["give_item_id"],
                                   count=values["give_item_count"], mode="TOP_UP_TO"))
    if values["remove_item_id"] != 0 and values["remove_item_count"] != 0:
        if values["remove_item_id"] < 0 or values["remove_item_count"] < 0:
            raise AuditError(f"Quest item id/count must be positive at line {helper.get('line')}")
        guard = sufficient_remove_guard(path, values["remove_item_id"], values["remove_item_count"])
        if guard is None:
            raise AuditError(f"removeQuestItem lacks a sufficient inventory guard at line {helper.get('line')}")
        if guard not in conditions:
            conditions.append(guard)
        item_actions.append(action("remove-quest-item", item_id=values["remove_item_id"],
                                   count=values["remove_item_count"], mode="EXACT"))

    state_actions = []
    target_node = "active"
    if values["reward"]:
        state_actions.append(action("set-quest-status", status="REWARD"))
        target_node = "reward"
    elif values["next_step"] != values["step"]:
        state_actions.append(action("set-quest-variable", variable=name, value=values["next_step"]))
    protocol = [action("sync-quest-status")] if state_actions else []
    dialog = dialogs[0]
    for target in targets:
        if values["same_npc"]:
            if not values["reward"] or dialog not in {"SELECT_REWARD", "USE_OBJECT"}:
                raise AuditError(f"defaultCloseDialog sameNpc dialog is unsupported at line {helper.get('line')}: "
                                 f"{dialog}")
            endpoint = send_dialog_action(5 + values["reward_id"], "defaultCloseDialog reward page")
        else:
            endpoint = action("close-dialog") if npc_ai.get(target) == "useitem" else action("show-quest-list")
        plan_transition(plans, "active", target_node, target, dialog, list(conditions),
                        prefix_state_actions + state_actions + prefix_item_actions + item_actions + protocol + [endpoint], helper,
                        "default-close", "talk")


def compile_collect_helper(plans: list[dict[str, Any]], call: dict[str, Any], variables: set[str]) -> None:
    path = tuple(call["control_path"])
    if control_phase(path) != "active":
        raise AuditError(f"Collect helper is outside START at line {call.get('line')}")
    targets = control_targets(path)
    dialogs = control_dialogs(path)
    if not targets or len(dialogs) != 1:
        raise AuditError(f"Collect helper has ambiguous target/dialog at line {call.get('line')}")
    step = call_integer(call, 1, "collect current step")
    next_step = call_integer(call, 2, "collect next step")
    reward = call["arguments"][3].strip() == "true"
    ok_page = call_integer(call, 4, "collect success page")
    name = variable_name(0)
    variables.add(name)
    base = [status_condition("START"), condition("quest-variable", variable=name, op="EQUAL", value=step)]
    success_state = [action("set-quest-status", status="REWARD")] if reward else \
        ([action("set-quest-variable", variable=name, value=next_step)] if next_step != step else [])
    success = success_state + [action("remove-collected-items"), action("sync-quest-status"),
                               send_dialog_action(ok_page, "collect success")]
    if call["method"] == "checkQuestItemsSimple":
        failure = [action("close-dialog")]
    else:
        failure = [send_dialog_action(call_integer(call, 5, "collect failure page"), "collect failure")]
    for target in targets:
        plan_transition(plans, "active", "reward" if reward else "active", target, dialogs[0],
                        base + [condition("quest-collect-items")], success, call, "collect-ok", "talk", 0)
        plan_transition(plans, "active", "active", target, dialogs[0], base, failure, call,
                        "collect-missing", "talk", 1)


def compile_direct_collect(plans: list[dict[str, Any]], call: dict[str, Any], groups: dict[tuple[str, ...], list[dict[str, Any]]],
                           handled: set[tuple[str, ...]], variables: set[str]) -> None:
    path = tuple(call["control_path"])
    if control_phase(path) != "active" or call["arguments"] != ["env", "true"]:
        raise AuditError(f"Unsupported direct collectItemCheck at line {call.get('line')}")
    targets = control_targets(path)
    dialogs = control_dialogs(path)
    success_paths = [candidate for candidate in groups if candidate[:len(path)] == path
                     and any(part.startswith("IF_TRUE:(QuestService.collectItemCheck") for part in candidate[len(path):])]
    failure_paths = [candidate for candidate in groups if candidate[:len(path)] == path
                     and any(part.startswith("IF_FALSE:(QuestService.collectItemCheck") for part in candidate[len(path):])]
    if not targets or len(dialogs) != 1 or len(success_paths) != 1 or len(failure_paths) != 1:
        raise AuditError(f"Direct collect branch is incomplete at line {call.get('line')}")
    success_calls = groups[success_paths[0]]
    failure_calls = groups[failure_paths[0]]
    statuses = [candidate for candidate in success_calls if candidate["method"] == "setStatus"]
    changes = [candidate for candidate in success_calls if candidate["method"] == "changeQuestStep"]
    state_actions = []
    state_conditions = []
    target_node = "active"
    if statuses:
        if changes or len(statuses) != 1 or statuses[0]["arguments"] != ["QuestStatus.REWARD"] \
                or not any(candidate["method"] == "updateQuestStatus" for candidate in success_calls):
            raise AuditError(f"Direct collect success state is incomplete at line {call.get('line')}")
        state_actions.append(action("set-quest-status", status="REWARD"))
        target_node = "reward"
    elif changes:
        if len(changes) != 1:
            raise AuditError(f"Direct collect success state is incomplete at line {call.get('line')}")
        change = changes[0]
        step = call_integer(change, 1, "current quest step")
        next_step = call_integer(change, 2, "next quest step")
        reward = call_boolean(change, 3, "changeQuestStep reward")
        index = call_integer(change, 4, "quest variable") if len(change["arguments"]) > 4 else 0
        name = variable_name(index)
        variables.add(name)
        state_conditions.append(condition("quest-variable", variable=name, op="EQUAL", value=step))
        if reward:
            state_actions.append(action("set-quest-status", status="REWARD"))
            target_node = "reward"
        elif next_step != step:
            state_actions.append(action("set-quest-variable", variable=name, value=next_step))
    elif any(candidate["method"] in {"setQuestVar", "setQuestVarById", "updateQuestStatus"}
             for candidate in success_calls):
        raise AuditError(f"Direct collect success state is incomplete at line {call.get('line')}")
    success_actions = [*state_actions, action("remove-collected-items")]
    if state_actions:
        success_actions.append(action("sync-quest-status"))
    success_actions.extend(endpoint_actions(success_calls, dialogs[0]))
    failure_actions = endpoint_actions(failure_calls, dialogs[0])
    for target in targets:
        plan_transition(plans, "active", target_node, target, dialogs[0],
                        [status_condition("START"), *state_conditions, condition("quest-collect-items")], success_actions,
                        call, "collect-ok", "talk", 0)
        plan_transition(plans, "active", "active", target, dialogs[0], [status_condition("START")],
                        failure_actions, call, "collect-missing", "talk", 1)
    handled.update({path, success_paths[0], failure_paths[0]})


def compile_finish_group(plans: list[dict[str, Any]], calls: list[dict[str, Any]], path: tuple[str, ...],
                         npc_ai: dict[int, str], quest: dict[str, Any]) -> None:
    if control_phase(path) != "reward":
        raise AuditError(f"Reward settlement is outside REWARD at line {calls[0].get('line')}")
    end = next(call for call in calls if call["method"] == "sendQuestEndDialog")
    targets = control_targets(path)
    dialogs = control_dialogs(path)
    reward_index = call_integer(end, 1, "reward group") if len(end["arguments"]) > 1 else 0
    if not targets:
        raise AuditError(f"Reward settlement has no target at line {end.get('line')}")
    for target in targets:
        if not dialogs and not control_excludes_dialog(path, "SELECT_REWARD"):
            plan_transition(plans, "reward", "reward", target, "SELECT_REWARD", [status_condition("REWARD")],
                            [send_dialog_action(5 + reward_index, "reward page")], end, "reward-page", "talk")
        finish_dialogs = [dialog for dialog in dialogs if dialog in DIALOG_REWARD_CHOICES] if dialogs else DIALOG_REWARD_CHOICES
        for dialog in finish_dialogs:
            final_protocol = action("close-dialog") if npc_ai.get(target) in {"useitem", "quest_use_item"} \
                else action("show-quest-list")
            finish_attributes = {"reward_index": reward_index}
            if quest["repeat_policy"] is not None:
                finish_attributes.update(quest["repeat_policy"])
            finish_actions = [action("finish-quest", **finish_attributes), action("sync-quest-status")]
            if quest["repeat_policy"] is not None:
                finish_actions.append(action("send-repeat-deadline-message", **quest["repeat_policy"]))
            finish_actions.append(final_protocol)
            plan_transition(plans, "reward", "complete", target, dialog, [status_condition("REWARD")],
                            finish_actions,
                            end, "finish", "talk")


def compile_start_helper_group(plans: list[dict[str, Any]], calls: list[dict[str, Any]],
                               path: tuple[str, ...], row_path: str) -> None:
    helpers = [call for call in calls if call["method"] == "sendQuestStartDialog"]
    item_calls = [call for call in calls if call["method"] == "giveQuestItem"]
    if len(helpers) != 1 or len(item_calls) > 1 or len(calls) != len(helpers) + len(item_calls):
        raise AuditError(f"Start helper arguments require unsupported capability at {row_path}:{calls[0]['line']}")
    first = helpers[0]
    targets = control_targets(path)
    dialogs = control_dialogs(path)
    delegated = any(part.startswith("IF_FALSE:") and "QuestDialog.START_DIALOG" in part for part in path) \
        or any(part.startswith("SWITCH:") and "dialog" in part.lower() and part.endswith("=default") for part in path)
    if len(first.get("arguments", [])) not in {1, 3} or item_calls and len(first["arguments"]) != 1:
        raise AuditError(f"Start helper arguments require unsupported capability at {row_path}:{first['line']}")
    starter_action = None
    starter_source = first if len(first["arguments"]) == 3 else (item_calls[0] if item_calls else None)
    if starter_source is not None:
        item_id = call_integer(starter_source, 1, "starter item id")
        count = call_integer(starter_source, 2, "starter item count")
        if item_id <= 0 or count <= 0:
            raise AuditError(f"Quest item id/count must be positive at {row_path}:{first['line']}")
        if item_calls and (not condition_give_matches_success_path(starter_source, path)
                           or starter_source.get("source_position", starter_source["line"])
                           >= first.get("source_position", first["line"])):
            raise AuditError(f"Quest item give result requires an exact success branch at line {starter_source.get('line')}")
        starter_action = action("give-quest-item", item_id=item_id, count=count, mode="TOP_UP_TO")
    if control_phase(path) != "offer" or not targets or (not dialogs and not delegated):
        raise AuditError(f"Ambiguous start helper group at {row_path}:{first['line']}")
    if not dialogs:
        dialogs = [
            "ASK_ACCEPTION",
            "ACCEPT_QUEST",
            "ACCEPT_QUEST_SIMPLE",
            "REFUSE_QUEST",
            "REFUSE_QUEST_2",
            "REFUSE_QUEST_SIMPLE",
            "FINISH_DIALOG",
        ]
    for target in targets:
        for order, dialog in enumerate(dialogs):
            if dialog == "ASK_ACCEPTION":
                target_node = "offer"
                actions = [send_dialog_action(4, "ask acceptance")]
                kind = "ask-acceptance"
            elif dialog == "ACCEPT_QUEST":
                target_node = "active"
                actions = [action("start-quest")] + ([starter_action] if starter_action else []) + [action("sync-quest-status"),
                           send_dialog_action(1003, "quest acceptance")]
                kind = "accept"
            elif dialog == "ACCEPT_QUEST_SIMPLE":
                target_node = "active"
                actions = [action("start-quest")] + ([starter_action] if starter_action else []) + \
                    [action("sync-quest-status"), action("close-dialog")]
                kind = "accept-simple"
            elif dialog in {"REFUSE_QUEST", "REFUSE_QUEST_2", "REFUSE_QUEST_SIMPLE"}:
                target_node = "offer"
                actions = [action("close-dialog")]
                kind = "refuse"
            elif dialog == "FINISH_DIALOG":
                target_node = "offer"
                actions = [action("show-quest-list")]
                kind = "finish-dialog"
            else:
                raise AuditError(f"Unsupported start dialog {dialog} at {row_path}:{first['line']}")
            plan_transition(plans, "offer", target_node, target, dialog, [status_condition("NONE")],
                            actions, first, kind, "start", order)


def collect_dialog_action_groups(row: dict[str, Any]) -> dict[tuple[str, ...], list[dict[str, Any]]]:
    groups: dict[tuple[str, ...], list[dict[str, Any]]] = defaultdict(list)
    for call in row["calls"]:
        if call["enclosing_method"] == "onDialogEvent" and call["method"] in DIALOG_STANDARD_ACTION_METHODS:
            groups[tuple(call["control_path"])].append(call)
    fold_positive_condition_gives(groups)
    for calls in groups.values():
        calls.sort(key=lambda call: (call.get("source_position", call["line"]), call["method"], call["select"]))
    return groups


def ordered_action_groups(groups: dict[tuple[str, ...], list[dict[str, Any]]]) \
        -> list[tuple[tuple[str, ...], list[dict[str, Any]]]]:
    return sorted(groups.items(), key=lambda item: (min(call["line"] for call in item[1]), item[0]))


def expand_repeat_plans(plans: list[dict[str, Any]], quest: dict[str, Any]) -> None:
    if quest["max_repeat_count"] <= 1:
        return
    repeat_plans = []
    for source in [plan for plan in plans if plan["node"] == "offer"]:
        repeated = dict(source)
        repeated["node"] = "complete"
        repeated["target"] = "complete" if source["target"] == "offer" else source["target"]
        repeated["conditions"] = [status_condition("COMPLETE"), repeat_condition(quest, True)] + source["conditions"][1:]
        repeated["kind"] = "repeat-" + source["kind"]
        repeat_plans.append(repeated)
    plans.extend(repeat_plans)


def validate_dialog_routing(row: dict[str, Any], plans: list[dict[str, Any]],
                            registrations: dict[str, list[int]]) -> list[int]:
    dialog_npcs = {plan["npc_id"] for plan in plans}
    missing_registration = sorted(dialog_npcs - set(registrations["start"]) - set(registrations["talk"]))
    if missing_registration:
        raise AuditError(f"{row['path']} dialog targets are not registered: {missing_registration}")
    return sorted({plan["npc_id"] for plan in plans if plan["node"] == "offer"})


def emit_dialog_graph(quest_id: int, variables: set[str], plans: list[dict[str, Any]]) \
        -> tuple[ET.Element, int, set[int], set[str]]:
    graph = ET.Element("quest_graph", {
        "quest_id": str(quest_id), "version": "1", "scope": "PLAYER", "initial_node": "offer",
    })
    if variables:
        wrapper = ET.SubElement(graph, "variables")
        for name in sorted(variables):
            ET.SubElement(wrapper, "variable", {"name": name, "type": "INT", "scope": "PLAYER",
                                                "initial": "0", "min": "0", "max": "255"})
    transition_count = 0
    graph_targets = set()
    graph_dialogs = set()
    for node_id in ("offer", "active", "reward", "complete"):
        node = ET.SubElement(graph, "node", {"id": node_id, **({"terminal": "true"} if node_id == "complete" else {})})
        node_plans = sorted((plan for plan in plans if plan["node"] == node_id),
                            key=lambda plan: (plan["source_line"], plan["order"], plan["npc_id"], plan["dialog"], plan["kind"]))
        if node_id != "complete" and not node_plans:
            raise AuditError(f"Quest {quest_id} generated empty non-terminal node {node_id}")
        for priority, plan in enumerate(node_plans, 1):
            transition_count += 1
            transition = ET.SubElement(node, "transition", {
                "id": f"t{transition_count:03d}_{re.sub('[^a-z0-9]+', '_', plan['kind'].lower()).strip('_')}",
                "priority": str(priority * 10),
                "to": plan["target"],
            })
            ET.SubElement(transition, "dialog", {"npc_id": str(plan["npc_id"]), "dialog": plan["dialog"]})
            conditions = ET.SubElement(transition, "conditions")
            for value in plan["conditions"]:
                ET.SubElement(conditions, value["tag"], value["attrs"])
            actions = ET.SubElement(transition, "actions")
            for value in plan["actions"]:
                ET.SubElement(actions, value["tag"], value["attrs"])
            graph_targets.add(plan["npc_id"])
            graph_dialogs.add(plan["dialog"])
    return graph, transition_count, graph_targets, graph_dialogs


def compile_dialog_action_groups(row: dict[str, Any], quest: dict[str, Any], npc_ai: dict[int, str]) \
        -> tuple[dict[tuple[str, ...], list[dict[str, Any]]], list[dict[str, Any]], set[str],
                 set[tuple[str, ...]]]:
    groups = collect_dialog_action_groups(row)
    plans: list[dict[str, Any]] = []
    variables: set[str] = set()
    handled: set[tuple[str, ...]] = set()

    for path, calls in ordered_action_groups(groups):
        direct_collect = [call for call in calls if call["method"] == "collectItemCheck"]
        if direct_collect:
            if len(direct_collect) != 1:
                raise AuditError(f"Multiple collectItemCheck calls at {row['path']}:{direct_collect[0]['line']}")
            compile_direct_collect(plans, direct_collect[0], groups, handled, variables)

    for path, calls in ordered_action_groups(groups):
        if path in handled:
            continue
        calls = validate_metadata_calls(calls)
        if not calls:
            raise AuditError(f"Uncompiled metadata-only group at {row['path']}: {path}")
        methods = {call["method"] for call in calls}
        first = calls[0]
        phase = control_phase(path)
        targets = control_targets(path)
        dialogs = control_dialogs(path)
        if methods & {"checkQuestItems", "checkQuestItemsSimple"}:
            helpers = [call for call in calls if call["method"] in {"checkQuestItems", "checkQuestItemsSimple"}]
            if len(helpers) != 1 or len(methods) != 1:
                raise AuditError(f"Ambiguous collect helper group at {row['path']}:{first['line']}")
            compile_collect_helper(plans, helpers[0], variables)
        elif "sendQuestStartDialog" in methods:
            if not methods <= {"sendQuestStartDialog", "giveQuestItem"}:
                raise AuditError(f"Ambiguous start helper group at {row['path']}:{first['line']}")
            compile_start_helper_group(plans, calls, path, row["path"])
        elif "defaultCloseDialog" in methods:
            compile_default_close_group(plans, calls, path, variables, npc_ai)
        elif methods & {"changeQuestStep", "setQuestVar", "setQuestVarById", "setStatus", "giveQuestItem",
                        "removeQuestItem"}:
            compile_mutation_group(plans, calls, path, variables, row.get("boolean_returns", []))
        elif "sendQuestEndDialog" in methods:
            if methods != {"sendQuestEndDialog"}:
                raise AuditError(f"Ambiguous reward helper group at {row['path']}:{first['line']}")
            compile_finish_group(plans, calls, path, npc_ai, quest)
        elif methods == {"sendQuestDialog"}:
            if phase not in {"offer", "active", "reward"} or not targets or not dialogs:
                raise AuditError(f"Ambiguous dialog page group at {row['path']}:{first['line']}: "
                                 f"phase={phase}, targets={targets}, dialogs={dialogs}")
            page = call_integer(first, 1, "dialog page")
            for target in targets:
                for dialog in dialogs:
                    conditions = merge_conditions(
                        [status_condition({"offer": "NONE", "active": "START", "reward": "REWARD"}[phase])],
                        compiled_path_conditions(path, variables),
                    )
                    plan_transition(plans, phase, phase, target, dialog, conditions,
                                    [send_dialog_action(page, "dialog page")], first, "dialog-page",
                                    "start" if phase == "offer" else "talk")
        elif methods == {"broadcastPacket"}:
            if phase != "offer" or not targets or not dialogs or len(calls) != 1:
                raise AuditError(f"Ambiguous player message group at {row['path']}:{first['line']}")
            for target in targets:
                for dialog in dialogs:
                    conditions = merge_conditions([status_condition("NONE")],
                                                  compiled_path_conditions(path, variables))
                    plan_transition(plans, "offer", "offer", target, dialog, conditions,
                                    [player_message(first)], first, "offer-message", "start", 1)
        elif methods == {"sendPacket"}:
            if phase not in {"offer", "active", "reward"} or not targets or not dialogs or len(calls) != 1:
                raise AuditError(f"Ambiguous sendPacket group at {row['path']}:{first['line']}")
            endpoint = dialog_window_packet_action(first)
            for target in targets:
                for dialog in dialogs:
                    conditions = merge_conditions(
                        [status_condition({"offer": "NONE", "active": "START", "reward": "REWARD"}[phase])],
                        compiled_path_conditions(path, variables),
                    )
                    plan_transition(plans, phase, phase, target, dialog, conditions,
                                    [endpoint], first, "dialog-window-packet",
                                    "start" if phase == "offer" else "talk")
        elif methods == {"closeDialogWindow"}:
            if not targets or not dialogs or len(calls) != 1:
                raise AuditError(f"Ambiguous close-dialog group at {row['path']}:{first['line']}")
            phases = [phase] if phase is not None else (["active", "reward", "complete"]
                if any(part.startswith("IF_FALSE:") and "QuestStatus.NONE" in part for part in path) else [])
            if not phases:
                raise AuditError(f"Close-dialog group has no status phase at {row['path']}:{first['line']}")
            for current_phase in phases:
                current_status = {"offer": "NONE", "active": "START", "reward": "REWARD", "complete": "COMPLETE"}[current_phase]
                conditions = merge_conditions([status_condition(current_status)],
                                              compiled_path_conditions(path, variables))
                if current_phase == "complete":
                    conditions = merge_conditions(conditions, [repeat_condition(quest, False)])
                for target in targets:
                    for dialog in dialogs:
                        plan_transition(plans, current_phase, current_phase, target, dialog, conditions,
                                        [action("close-dialog")], first, "close", "start" if current_phase == "offer" else "talk")
        elif methods == {"sendQuestSelectionDialog"}:
            if phase not in {"offer", "active", "reward"} or not targets or not dialogs or len(calls) != 1:
                raise AuditError(f"Ambiguous quest-selection group at {row['path']}:{first['line']}")
            for target in targets:
                for dialog in dialogs:
                    conditions = merge_conditions(
                        [status_condition({"offer": "NONE", "active": "START", "reward": "REWARD"}[phase])],
                        compiled_path_conditions(path, variables),
                    )
                    plan_transition(plans, phase, phase, target, dialog, conditions,
                                    [action("show-quest-list")], first, "quest-selection",
                                    "start" if phase == "offer" else "talk")
        else:
            raise AuditError(f"Uncompiled action group at {row['path']}:{first['line']}: {sorted(methods)}")
        handled.add(path)

    unhandled = [path for path in groups if path not in handled]
    if unhandled:
        raise AuditError(f"Unhandled control paths in {row['path']}: {unhandled}")
    return groups, plans, variables, handled


def compile_dialog_handler(row: dict[str, Any], fingerprint: str, quest: dict[str, Any],
                           npc_ai: dict[int, str]) -> tuple[ET.Element, dict[str, Any]]:
    quest_id = row["quest_ids"][0]
    registrations = registration_targets(row)
    groups, plans, variables, handled = compile_dialog_action_groups(row, quest, npc_ai)
    expand_repeat_plans(plans, quest)
    offer_npcs = validate_dialog_routing(row, plans, registrations)
    graph, transition_count, graph_targets, graph_dialogs = emit_dialog_graph(quest_id, variables, plans)
    parity = {
        "quest_id": quest_id,
        "shape": fingerprint,
        "source_path": row["path"],
        "classification": "DERIVED_FROM_CURRENT_HANDLER_AST",
        "source_action_calls": sum(len(calls) for calls in groups.values()),
        "compiled_action_calls": sum(len(groups[path]) for path in handled),
        "graph_transitions": transition_count,
        "registered_start_npcs": registrations["start"],
        "registered_talk_npcs": registrations["talk"],
        "offer_dialog_npcs": offer_npcs,
        "start_marker_dialog_mismatch": registrations["start"] != offer_npcs,
        "unused_talk_npcs": sorted(set(registrations["talk"]) - graph_targets),
        "graph_npcs": sorted(graph_targets),
        "graph_dialogs": sorted(graph_dialogs),
        "variables": sorted(variables),
        "matched": True,
    }
    return graph, parity


def validate_generated_schema(config: Config, xml: str) -> None:
    schema = config.project_root / "src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd"
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", suffix=".xml") as stream:
        stream.write(xml)
        stream.flush()
        run_command(["xmllint", "--noout", "--schema", str(schema), stream.name], config.project_root)


def quest_graphs_xml(graphs: Iterable[ET.Element]) -> str:
    root = ET.Element("quest_graphs")
    for graph in graphs:
        root.append(graph)
    ET.indent(root, space="\t")
    return '<?xml version="1.0" encoding="UTF-8"?>\n' + ET.tostring(root, encoding="unicode") + "\n"


def validate_member_graph_schemas(config: Config, artifacts: list[dict[str, Any]]) -> dict[str, str]:
    failures: dict[str, str] = {}

    def validate_batch(batch: list[dict[str, Any]]) -> None:
        if not batch:
            return
        try:
            validate_generated_schema(config, quest_graphs_xml(artifact["graph"] for artifact in batch))
            return
        except AuditError as error:
            if len(batch) == 1:
                path = batch[0]["member"]["path"]
                failures[path] = f"XSD_VALIDATION_FAILED:{error}"
                return
        middle = len(batch) // 2
        validate_batch(batch[:middle])
        validate_batch(batch[middle:])

    validate_batch(artifacts)
    return failures


def build_dialog_standard_outputs(config: Config, inventory: InventoryBundle, shapes: dict[str, Any]) \
        -> tuple[dict[str, Any], list[dict[str, Any]]]:
    capability_report, artifacts = analyze_dialog_compiler_members(config, inventory, shapes)
    repeat_policy_report = build_repeat_policy_report(config)
    selected = capability_report["selected_shapes"]
    blockers = list(capability_report["migration_blockers"]) + list(repeat_policy_report["blockers"])
    members = [(shape["fingerprint"], member) for shape in selected for member in shape["generation_members"]]
    expected_count = sum(shape["generation_member_count"] for shape in selected)
    rows_by_path = {row["path"]: row for row in inventory.java_rows}
    quest_ids = {member["quest_id"] for _, member in members}
    quests = load_quest_evidence(config, quest_ids)
    npc_ai = load_npc_evidence(config)
    item_ids = load_item_ids(config)
    root = ET.Element("quest_graphs")
    parity_rows = []
    ownership_rows = []
    reference_rows = []
    shape_counts: Counter[str] = Counter()
    candidate_errors = []
    for fingerprint, member in sorted(members, key=lambda value: (value[1]["quest_id"], value[1]["path"])):
        quest_id = member["quest_id"]
        row = rows_by_path.get(member["path"])
        quest = quests.get(quest_id)
        errors = []
        if row is None:
            errors.append("MISSING_HANDLER_AST")
        if quest is None:
            errors.append("MISSING_QUEST_DATA")
        artifact = artifacts.get(member["path"])
        graph = artifact["graph"] if artifact is not None else None
        parity = artifact["parity"] if artifact is not None else None
        if not errors and artifact is None:
            errors.append("MEMBER_GATE_ARTIFACT_MISSING")
        npc_references = artifact["npc_references"] if artifact is not None else []
        item_references = quest["item_ids"] if quest is not None else []
        inventory_references = artifact["inventory_references"] if artifact is not None else []
        action_item_references = artifact["action_item_references"] if artifact is not None else []
        missing_npcs = sorted(set(npc_references) - set(npc_ai))
        missing_items = sorted((set(item_references) | set(inventory_references) | set(action_item_references)) - item_ids)
        if missing_npcs:
            errors.append(f"MISSING_NPC_REFERENCES:{missing_npcs}")
        if missing_items:
            errors.append(f"MISSING_ITEM_REFERENCES:{missing_items}")
        if errors:
            candidate_errors.append({"quest_id": quest_id, "path": member["path"], "errors": errors})
        else:
            root.append(graph)
            parity_rows.append(parity)
            shape_counts[fingerprint] += 1
        owner_blockers = ["CANDIDATE_ONLY_NO_PRODUCTION_OWNER_SWITCH"]
        if parity is not None and parity["start_marker_dialog_mismatch"]:
            owner_blockers.append("START_MARKER_DIALOG_TARGET_MISMATCH")
        owner_blockers.extend(errors)
        ownership_rows.append({
            "quest_id": quest_id,
            "legacy_owner": member["path"],
            "candidate_owner": "QUEST_GRAPH",
            "status": "BLOCKED",
            "blockers": owner_blockers,
        })
        reference_rows.append({
            "quest_id": quest_id,
            "npc_ids": npc_references,
            "collect_item_ids": quest["collect_item_ids"] if quest is not None else [],
            "work_item_ids": quest["work_item_ids"] if quest is not None else [],
            "all_item_ids": item_references,
            "inventory_condition_item_ids": inventory_references,
            "item_action_ids": action_item_references,
            "reward_groups": quest["reward_groups"] if quest is not None else 0,
            "repeat_policy": quest["repeat_policy"] if quest is not None else None,
            "missing_npc_ids": missing_npcs,
            "missing_item_ids": missing_items,
        })
    xml = quest_graphs_xml(root)
    validate_generated_schema(config, xml)
    if candidate_errors:
        blockers.append({"kind": "DIALOG_STANDARD_GENERATION_ERROR", "count": len(candidate_errors)})
    generated_count = len(parity_rows)
    if generated_count != expected_count:
        blockers.append({"kind": "DIALOG_STANDARD_COVERAGE_GAP", "count": expected_count - generated_count})
    combined_hash = content_hash((inventory.input_hash + repeat_policy_report["input_manifest_hash"]
                                  + canonical_json(selected)).encode("utf-8"))
    generation_report = {
        **base_report(combined_hash),
        "authority": "CURRENT_HANDLER_AST_PLUS_LOCAL_STATIC_DATA_REFERENCES",
        "classification": "DERIVED_CANDIDATE_ONLY",
        "family": DIALOG_STANDARD_FAMILY,
        "counts": {
            "selected_shapes": len(selected),
            "input_handlers": expected_count,
            "generated_graphs": generated_count,
            "failed_handlers": len(candidate_errors),
            "production_owner_switches": 0,
            "blocked_owners": len(ownership_rows),
        },
        "shape_counts": [{"fingerprint": key, "graphs": value} for key, value in sorted(shape_counts.items())],
        "references": reference_rows,
        "errors": candidate_errors,
        "blockers": blockers,
    }
    parity_report = {
        **base_report(combined_hash),
        "authority": "CURRENT_HANDLER_AST",
        "classification": "DERIVED",
        "counts": {"input_handlers": expected_count, "matched_graphs": generated_count,
                   "mismatched_handlers": len(candidate_errors)},
        "quests": parity_rows,
        "blockers": ([{"kind": "HANDLER_GRAPH_PARITY_GAP", "count": len(candidate_errors)}]
                     if candidate_errors else []),
    }
    ownership_report = {
        **base_report(combined_hash),
        "authority": "CURRENT_PRODUCTION_OWNER_INVENTORY",
        "classification": "PROVEN_OWNER_WITH_DERIVED_CANDIDATE",
        "counts": {"candidates": len(ownership_rows), "production_owner_switches": 0,
                   "blocked_owners": len(ownership_rows),
                   "deadline_bridge_blockers": sum("NEXT_REPEAT_DEADLINE_BRIDGE_MISSING" in row["blockers"]
                                                   for row in ownership_rows)},
        "owners": ownership_rows,
        "blockers": [{"kind": "CANDIDATE_ONLY_NO_PRODUCTION_OWNER_SWITCH", "count": len(ownership_rows)}],
    }
    prefix = f"generated/{DIALOG_STANDARD_FAMILY}"
    return {
        f"{prefix}/quest_graphs.xml": xml,
        f"{prefix}/generation-report.json": generation_report,
        f"{prefix}/ownership.json": ownership_report,
        f"{prefix}/parity.json": parity_report,
        f"{prefix}/repeat-policy-report.json": repeat_policy_report,
    }, blockers


def read_json_report(path: Path) -> dict[str, Any]:
    if not path.is_file():
        raise AuditError(f"Missing generated report: {path}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise AuditError(f"Invalid generated report {path}: {error}") from error
    if not isinstance(value, dict):
        raise AuditError(f"Generated report root must be an object: {path}")
    return value


def verify_dialog_standard_outputs(config: Config) -> list[dict[str, Any]]:
    root = config.report_dir / "generated" / DIALOG_STANDARD_FAMILY
    xml_path = root / "quest_graphs.xml"
    if not xml_path.is_file():
        raise AuditError(f"Missing generated report: {xml_path}")
    try:
        xml = xml_path.read_text(encoding="utf-8")
        graphs_root = ET.fromstring(xml)
    except (OSError, ET.ParseError) as error:
        raise AuditError(f"Invalid generated quest graphs {xml_path}: {error}") from error
    if local_name(graphs_root.tag) != "quest_graphs":
        raise AuditError(f"Generated quest graph root must be quest_graphs: {xml_path}")
    graphs = [graph for graph in graphs_root if local_name(graph.tag) == "quest_graph"]
    try:
        quest_ids = [int(graph.attrib["quest_id"]) for graph in graphs]
    except (KeyError, ValueError) as error:
        raise AuditError(f"Generated quest graph has an invalid quest_id: {xml_path}") from error
    duplicate_ids = sorted(quest_id for quest_id, count in Counter(quest_ids).items() if count > 1)
    if duplicate_ids:
        raise AuditError(f"Generated quest graphs contain duplicate quest ids: {duplicate_ids}")

    generation = read_json_report(root / "generation-report.json")
    parity = read_json_report(root / "parity.json")
    ownership = read_json_report(root / "ownership.json")
    repeat = read_json_report(root / "repeat-policy-report.json")
    graph_count = len(graphs)
    expected_counts = {
        "generation generated_graphs": generation.get("counts", {}).get("generated_graphs"),
        "generation input_handlers": generation.get("counts", {}).get("input_handlers"),
        "parity matched_graphs": parity.get("counts", {}).get("matched_graphs"),
        "parity input_handlers": parity.get("counts", {}).get("input_handlers"),
        "ownership candidates": ownership.get("counts", {}).get("candidates"),
    }
    mismatched = {label: value for label, value in expected_counts.items() if value != graph_count}
    if mismatched:
        raise AuditError(f"Generated report count closure failed: graphs={graph_count}, reports={mismatched}")
    if generation.get("counts", {}).get("failed_handlers") != 0 or generation.get("errors"):
        raise AuditError("Generated report contains failed Handler members")
    if parity.get("counts", {}).get("mismatched_handlers") != 0 or parity.get("blockers"):
        raise AuditError("Generated parity report contains mismatched Handler members")
    if repeat.get("blockers"):
        raise AuditError("Generated repeat-policy report contains blockers")

    owner_rows = ownership.get("owners", [])
    owner_ids = [row.get("quest_id") for row in owner_rows]
    if sorted(owner_ids) != sorted(quest_ids):
        raise AuditError("Generated ownership quest ids do not match generated graphs")
    if ownership.get("counts", {}).get("production_owner_switches") != 0:
        raise AuditError("Candidate batch must not switch production owners")
    invalid_owners = [row.get("quest_id") for row in owner_rows
                      if row.get("status") != "BLOCKED" or row.get("candidate_owner") != "QUEST_GRAPH"
                      or "CANDIDATE_ONLY_NO_PRODUCTION_OWNER_SWITCH" not in row.get("blockers", [])]
    if invalid_owners:
        raise AuditError(f"Generated ownership rows are not candidate-only: {invalid_owners}")

    references = generation.get("references", [])
    if sorted(row.get("quest_id") for row in references) != sorted(quest_ids):
        raise AuditError("Generated reference rows do not match generated graphs")
    reported_missing = [row.get("quest_id") for row in references
                        if row.get("missing_npc_ids") or row.get("missing_item_ids")]
    if reported_missing:
        raise AuditError(f"Generated reference report contains unresolved ids: {reported_missing}")
    npc_ai = load_npc_evidence(config)
    item_ids = load_item_ids(config)
    missing_npcs = sorted({int(event.get("npc_id")) for graph in graphs for event in graph.iter("dialog")} - set(npc_ai))
    graph_items = {int(node.get("item_id")) for graph in graphs
                   for tag in ("player-inventory", "give-quest-item", "remove-quest-item")
                   for node in graph.iter(tag)}
    missing_items = sorted(graph_items - item_ids)
    if missing_npcs or missing_items:
        raise AuditError(f"Generated XML reference closure failed: npcs={missing_npcs}, items={missing_items}")
    validate_generated_schema(config, xml)
    return []


def signature_counts(handlers: list[dict[str, Any]], field: str) -> list[dict[str, Any]]:
    counts: Counter[str] = Counter()
    for handler in handlers:
        counts.update(handler[field])
    return [{"signature": signature, "count": count} for signature, count in sorted(counts.items())]


LOCAL_COMPOSITE_EXPANSIONS = {
    "setPlayerClass/3": [
        "PLAYER_CLASS_SET", "PLAYER_UPGRADE", "TELEPORT", "STATE_TRANSITION",
    ],
    "deleteQuestItems/2": [
        "FOR_EACH_INPUT_ITEM", "ITEM_COUNT_READ", "IF_POSITIVE", "ITEM_REMOVE_ALL",
    ],
    "getQuestUpdateStatus/2": [
        "CROSS_QUEST_STATE_READ", "MISSING_TO_START", "OTHER_COMPLETE_TO_START", "RETURN_STATUS",
    ],
    "getStoneId/1": [
        "PLAYER_CLASS_READ", "STATIC_LOOKUP_WITH_DEFAULT",
    ],
    "removeStigma/1": [
        "PLAYER_CLASS_ITEM_LOOKUP", "EQUIPPED_ITEM_QUERY", "FOR_EACH_EQUIPPED_ITEM",
        "ITEM_UNEQUIP", "QUEST_ITEM_REMOVE",
    ],
    "reward/2": [
        "VARIABLE_READ", "ALL_OF_JOIN", "STATUS_TO_REWARD", "PROTOCOL_SYNC",
    ],
    "getItem/1": [
        "EVENT_DIALOG_ID_READ", "STATIC_PARALLEL_ARRAY_LOOKUP",
    ],
    "spawn/1": [
        "BOUNDED_RANDOM_CHOICE", "STATIC_SPAWN_LOOKUP", "INSTANCE_SCOPED_SPAWN",
        "LAST_MATCH_WORLD_NPC_LOOKUP", "TARGET_ASSIGN", "MOVE_TO_TARGET", "AGGRO_WRITE",
        "ATTACK_EMOTE", "AI_ATTACK_SIGNAL",
    ],
    "checkReward/1": [
        "VARIABLE_READ", "ALL_OF_JOIN", "STATUS_TO_REWARD", "PROTOCOL_SYNC",
    ],
    "isStigmaEquipped/1": [
        "PLAYER_CLASS_ITEM_LOOKUP", "EQUIPPED_STIGMA_QUERY", "ANY_MATCH",
    ],
    "changeStep/2": [
        "VARIABLE_READ", "ALL_OF_JOIN", "VARIABLE_RESET", "PRIMARY_STEP_WRITE", "PROTOCOL_SYNC",
    ],
    "completeQuest/2": [
        "CROSS_QUEST_STATE_READ", "CREATE_OR_UPDATE_COMPLETE_STATE", "COMPLETION_COUNT_INCREMENT",
        "PRIMARY_VARIABLE_RESET", "PROTOCOL_SYNC",
    ],
    "isDaevanionArmorEquipped/1": [
        "EQUIPMENT_SET_PART_COUNT_READ", "ANY_OF_FULL_SET",
    ],
}
DIRECT_SIGNATURE_EXPANSIONS = {
    "add/1": ["COLLECTION_ADD"],
    "addDamage/2": ["NPC_DAMAGE_LEDGER_WRITE"],
    "addExp/2": ["PLAYER_EXP_GRANT"],
    "addHandlerSideQuestDrop/5": ["SIDE_QUEST_DROP_RULE_ADD"],
    "addHate/2": ["NPC_AGGRO_WRITE"],
    "addSkill/3": ["SKILL_GRANT"],
    "addTask/2": ["PLAYER_CONTROLLER_TASK_REGISTER"],
    "getSpot/0": ["SPAWN_TEMPLATE_POSITION_READ"],
    "abortMove/0": ["MOVEMENT_ABORT"],
    "addTitle/3": ["TITLE_GRANT"],
    "broadcastPacket/2": ["PROTOCOL_PACKET_BROADCAST"],
    "broadcastPacket/3": ["PROTOCOL_PACKET_BROADCAST"],
    "contains/1": ["COLLECTION_MEMBERSHIP_TEST"],
    "decreaseByItemId/2": ["ITEM_DECREASE_BY_TEMPLATE_ID"],
    "decreaseByObjectId/2": ["ITEM_DECREASE_BY_OBJECT_ID"],
    "decreaseKinah/1": ["KINAH_DECREASE"],
    "delete/0": ["OBJECT_CONTROLLER_DELETE"],
    "delete/1": ["NPC_DELETE"],
    "emoteStartAttacking/1": ["AI_ATTACK_EMOTE"],
    "equals/1": ["VALUE_EQUALITY_TEST"],
    "fromBoolean/1": ["BOOLEAN_TO_HANDLER_RESULT"],
    "getActiveHouse/0": ["PLAYER_ACTIVE_HOUSE_READ"],
    "getAggroList/0": ["NPC_AGGRO_BRIDGE"],
    "getAi2/0": ["NPC_AI_BRIDGE"],
    "getButler/0": ["HOUSE_BUTLER_READ"],
    "getCommonData/0": ["PLAYER_COMMON_DATA_READ"],
    "getController/0": ["OBJECT_CONTROLLER_BRIDGE"],
    "getCurrent/0": ["STAT_CURRENT_VALUE_READ"],
    "getCurrentHp/0": ["CREATURE_CURRENT_HP_READ"],
    "getDialog/0": ["EVENT_DIALOG_ACTION_READ"],
    "getDialogId/0": ["EVENT_DIALOG_ID_READ"],
    "getDistance/4": ["DISTANCE_CALCULATE"],
    "getDistance/6": ["DISTANCE_CALCULATE"],
    "getDp/0": ["PLAYER_DP_READ"],
    "getEffectController/0": ["EFFECT_CONTROLLER_BRIDGE"],
    "getEquipment/0": ["PLAYER_EQUIPMENT_READ"],
    "getEquippedItemsAllStigma/0": ["EQUIPPED_STIGMA_QUERY"],
    "getEquippedItemsByItemId/1": ["EQUIPPED_ITEM_LOOKUP"],
    "getExp/0": ["QUEST_REWARD_EXP_READ"],
    "getFirstSpawnByNpcId/2": ["INSTANCE_NPC_LOOKUP"],
    "getGameStats/0": ["PLAYER_GAME_STATS_READ"],
    "getGender/0": ["PLAYER_GENDER_READ"],
    "getHeading/0": ["OBJECT_HEADING_READ"],
    "getId/0": ["WORLD_TYPE_ID_READ"],
    "getInstanceId/0": ["INSTANCE_ID_READ"],
    "getInventory/0": ["PLAYER_INVENTORY_READ"],
    "getItemCountByItemId/1": ["ITEM_COUNT_READ"],
    "getItemId/0": ["ITEM_TEMPLATE_ID_READ"],
    "getItemTemplate/0": ["ITEM_TEMPLATE_READ"],
    "getKinah/0": ["PLAYER_KINAH_READ"],
    "getKnownList/0": ["PERCEPTION_KNOWN_LIST_READ"],
    "getKnownObjectsSnapshot/0": ["PERCEPTION_OBJECT_SNAPSHOT"],
    "getLevel/0": ["ACTOR_LEVEL_READ"],
    "getLifeStats/0": ["CREATURE_LIFE_STATS_READ"],
    "getMapId/0": ["WORLD_MAP_ID_READ"],
    "getMaxDp/0": ["PLAYER_MAX_DP_STAT_READ"],
    "getMaxHp/0": ["CREATURE_MAX_HP_READ"],
    "getMaxRepeatCount/0": ["QUEST_MAX_REPEAT_COUNT_READ"],
    "getMoveController/0": ["MOVEMENT_CONTROLLER_BRIDGE"],
    "getName/0": ["QUEST_TEMPLATE_NAME_READ"],
    "getNpc/1": ["INSTANCE_NPC_LOOKUP"],
    "getNpcId/0": ["NPC_TEMPLATE_ID_READ"],
    "getNpcs/0": ["WORLD_INSTANCE_NPC_COLLECTION_READ"],
    "getObjectId/0": ["RUNTIME_OBJECT_ID_READ"],
    "getPlayer/0": ["EVENT_PLAYER_READ"],
    "getPlayerClass/0": ["PLAYER_CLASS_READ"],
    "getPosition/0": ["OBJECT_POSITION_READ"],
    "getQuestById/1": ["QUEST_TEMPLATE_LOOKUP"],
    "getRace/0": ["PLAYER_RACE_READ"],
    "getRecipeList/0": ["PLAYER_RECIPE_LIST_READ"],
    "getRewards/0": ["QUEST_REWARD_LIST_READ"],
    "getSkillList/0": ["PLAYER_SKILL_LIST_READ"],
    "getSpawn/0": ["NPC_SPAWN_TEMPLATE_READ"],
    "getStartingClassFor/1": ["STARTING_CLASS_DERIVE"],
    "getTarget/0": ["OBJECT_TARGET_READ"],
    "getTargetId/0": ["EVENT_TARGET_ID_READ"],
    "getTemplateId/0": ["ITEM_TEMPLATE_ID_READ"],
    "getTitleList/0": ["PLAYER_TITLE_LIST_READ"],
    "getVarById/1": ["QUEST_VARIABLE_READ"],
    "getVisibleObject/0": ["EVENT_VISIBLE_OBJECT_READ"],
    "getWorldId/0": ["WORLD_ID_READ"],
    "getWorldMapInstance/0": ["WORLD_INSTANCE_READ"],
    "getX/0": ["POSITION_X_READ"],
    "getY/0": ["POSITION_Y_READ"],
    "getZ/0": ["POSITION_Z_READ"],
    "hasNext/0": ["BOUNDED_ITERATION_HAS_NEXT"],
    "havePermission/1": ["PLAYER_PERMISSION_TEST"],
    "id/0": ["QUEST_DIALOG_NUMERIC_ID_READ"],
    "isEmpty/0": ["COLLECTION_EMPTY_TEST"],
    "isFullSpecialCube/0": ["SPECIAL_CUBE_FULL_TEST"],
    "isInGroup2/0": ["PLAYER_GROUP_MEMBERSHIP_TEST"],
    "isInsideZone/1": ["PLAYER_ZONE_MEMBERSHIP_TEST"],
    "isRecipePresent/1": ["RECIPE_KNOWN_TEST"],
    "isStartingClass/0": ["PLAYER_STARTING_CLASS_TEST"],
    "itemSetPartsEquipped/1": ["EQUIPMENT_SET_PART_COUNT_READ"],
    "iterator/0": ["BOUNDED_ITERATION_OPEN"],
    "moveToTargetObject/0": ["NPC_MOVE_TO_TARGET"],
    "newFollowingToTargetCheckTask/3": ["ESCORT_FOLLOW_MONITOR_CREATE"],
    "newFollowingToTargetCheckTask/5": ["ESCORT_FOLLOW_MONITOR_CREATE"],
    "next/0": ["BOUNDED_ITERATION_NEXT"],
    "onCreatureEvent/2": ["AI_CREATURE_EVENT_SIGNAL"],
    "onDelete/0": ["OBJECT_CONTROLLER_DELETE"],
    "onDie/1": ["OBJECT_CONTROLLER_DIE"],
    "onGeneralEvent/1": ["AI_GENERAL_EVENT_SIGNAL"],
    "onLvlUpEvent/1": ["HANDLER_EVENT_FORWARD"],
    "onQuestTimerEndEvent/1": ["HANDLER_EVENT_FORWARD"],
    "removeEffect/1": ["EFFECT_REMOVE"],
    "requireNonNull/1": ["VALIDATION_ONLY_NON_CAPABILITY"],
    "scheduleRespawn/0": ["NPC_RESPAWN_SCHEDULE"],
    "sendMessage/2": ["PROTOCOL_MESSAGE_SEND"],
    "sendPacket/2": ["PROTOCOL_PACKET_SEND"],
    "setDialogId/1": ["PROTOCOL_DIALOG_ID_WRITE"],
    "setDp/1": ["PLAYER_DP_WRITE"],
    "setExtendedRewardIndex/1": ["REWARD_SELECTION_INDEX_WRITE"],
    "setFlightTeleportId/1": ["FLIGHT_TELEPORT_ID_WRITE"],
    "setQuestId/1": ["ROUTING_QUEST_ID_WRITE"],
    "setState/1": ["PLAYER_STATE_FLAG_SET"],
    "setTarget/1": ["OBJECT_TARGET_WRITE"],
    "setWalkerId/1": ["NPC_WALKER_ROUTE_WRITE"],
    "singletonList/1": ["SINGLETON_COLLECTION_CREATE"],
    "startWalking/1": ["AI_WALK_START"],
    "super/1": ["LANGUAGE_SUPER_CONSTRUCTOR"],
    "tryDecreaseKinah/1": ["KINAH_DECREASE_IF_AVAILABLE"],
    "unEquipItem/2": ["ITEM_UNEQUIP"],
    "unsetState/1": ["PLAYER_STATE_FLAG_CLEAR"],
    "updateNearbyQuests/0": ["QUEST_PROXIMITY_REFRESH"],
    "upgradePlayer/0": ["PLAYER_UPGRADE"],
}

DIRECT_SELECT_EXPANSIONS = {
    ("get/1", "DataManager.QUEST_DATA.getQuestById(questId).getRewards().get"): ["QUEST_REWARD_INDEX_LOOKUP"],
    ("get/1", "ZoneName.get"): ["ZONE_NAME_LOOKUP"],
    ("get/1", "mobs.get"): ["COLLECTION_INDEX_LOOKUP"],
    ("get/2", "Rnd.get"): ["RANDOM_INT_INCLUSIVE"],
    ("get/0", "Rnd.get"): ["RANDOM_FLOAT_UNIT_INTERVAL"],
}


def direct_call_role(signature: str, selects: Iterable[str]) -> str | None:
    method = signature.split("/", 1)[0]
    select_values = set(selects)
    if method == "super":
        return "LANGUAGE_INFRASTRUCTURE"
    if select_values and select_values <= {method, f"this.{method}"}:
        return "EVENT_FORWARDING" if method in {"onLvlUpEvent", "onQuestTimerEndEvent"} \
            else "HANDLER_LOCAL_COMPOSITE"
    if method in {"add", "contains", "equals", "hasNext", "iterator", "next", "singletonList"}:
        return "COLLECTION_INFRASTRUCTURE"
    if method in {"get"}:
        return "COLLECTION_OR_DATA_ACCESS"
    if method == "requireNonNull":
        return "VALIDATION_INFRASTRUCTURE"
    if method in {"getDialog", "getDialogId", "getGender", "getHeading", "getInstanceId", "getMapId",
                  "getNpcId", "getObjectId", "getPlayer", "getPosition", "getTarget", "getTargetId",
                  "getVisibleObject", "getWorldId", "getWorldMapInstance", "getX", "getY", "getZ"}:
        return "EVENT_CONTEXT"
    if method in {"fromBoolean", "id", "setDialogId", "setExtendedRewardIndex", "setQuestId"}:
        return "PROTOCOL_RESULT"
    if method in {"broadcastPacket", "sendMessage", "sendPacket"}:
        return "PROTOCOL_SIDE_EFFECT"
    if method in {"getActiveHouse", "getButler", "getCommonData", "getCurrent", "getCurrentHp", "getDp",
                  "getEffectController", "getEquipment", "getEquippedItemsAllStigma", "getEquippedItemsByItemId",
                  "getExp", "getGameStats", "getInventory", "getItemCountByItemId", "getItemId",
                  "getItemTemplate", "getKinah", "getLevel", "getLifeStats", "getMaxDp", "getMaxHp",
                  "getMaxRepeatCount", "getPlayerClass", "getRace", "getRecipeList", "getSkillList",
                  "getStartingClassFor", "getTemplateId", "getVarById", "havePermission", "isEmpty",
                  "isFullSpecialCube", "isInGroup2", "isInsideZone", "isRecipePresent", "isStartingClass",
                  "itemSetPartsEquipped"}:
        return "CONDITION_INPUT"
    if method in {"getFirstSpawnByNpcId", "getId", "getKnownList", "getKnownObjectsSnapshot", "getName",
                  "getNpc", "getNpcs", "getQuestById", "getRewards", "getSpawn", "getSpot", "getTitleList"}:
        return "DOMAIN_DATA_LOOKUP"
    if method in {"getAggroList", "getAi2", "getController", "getDistance", "getMoveController"}:
        return "DOMAIN_BRIDGE"
    if method == "newFollowingToTargetCheckTask":
        return "ESCORT_TASK_BRIDGE"
    if method in {"abortMove", "addDamage", "addExp", "addHandlerSideQuestDrop", "addHate", "addSkill",
                  "addTask", "addTitle", "broadcastPacket", "decreaseByItemId", "decreaseByObjectId",
                  "decreaseKinah", "delete", "emoteStartAttacking", "moveToTargetObject", "onCreatureEvent",
                  "onDelete", "onDie", "onGeneralEvent", "removeEffect", "scheduleRespawn", "setDp",
                  "setFlightTeleportId", "setState", "setTarget", "setWalkerId", "startWalking", "tryDecreaseKinah",
                  "unEquipItem", "unsetState", "updateNearbyQuests", "upgradePlayer"}:
        return "DOMAIN_SIDE_EFFECT"
    if method in {"getFirstSpawnByNpcId", "getQuestById"}:
        return "DOMAIN_DATA_LOOKUP"
    return None


def build_direct_call_catalog(inventory: InventoryBundle) -> dict[str, Any]:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for handler in inventory.handler_report["handlers"]:
        for site in handler["direct_call_sites"]:
            grouped[site["signature"]].append({
                "quest_id": handler["quest_id"],
                "path": handler["path"],
                **site,
            })
    operations = []
    for signature, sites in sorted(grouped.items()):
        selects = Counter(site["select"] for site in sites)
        callbacks = Counter(site["enclosing_method"] for site in sites)
        handlers = sorted({(site["quest_id"], site["path"]) for site in sites})
        role = direct_call_role(signature, selects)
        local_expansion = LOCAL_COMPOSITE_EXPANSIONS.get(signature)
        expansion = local_expansion or DIRECT_SIGNATURE_EXPANSIONS.get(signature)
        receiver_semantics = [{
            "select": select,
            "call_count": count,
            "semantic_expansion": DIRECT_SELECT_EXPANSIONS[(signature, select)],
        } for select, count in sorted(selects.items()) if (signature, select) in DIRECT_SELECT_EXPANSIONS]
        reviewed = expansion is not None or len(receiver_semantics) == len(selects)
        operations.append({
            "signature": signature,
            "call_count": len(sites),
            "handler_count": len(handlers),
            "mechanical_classification": "SHARED_MECHANISM_CANDIDATE" if len(handlers) >= 2 else "UNIQUE_DIRECT_OUTLIER",
            "semantic_status": "DERIVED_REVIEWED" if reviewed else "PENDING_REVIEW",
            "mechanical_role": role or "UNCLASSIFIED_DIRECT_CALL",
            **({
                "semantic_expansion": expansion,
                "semantic_evidence": (
                    [f"{path}#{signature.split('/', 1)[0]}" for _, path in handlers]
                    if local_expansion else sorted({
                        f"{site['path']}:{site['line']}#{site['select']}" for site in sites
                    })
                ),
            } if expansion else {}),
            **({"receiver_semantics": receiver_semantics} if receiver_semantics else {}),
            "receiver_selects": [{"select": select, "count": count} for select, count in sorted(selects.items())],
            "callbacks": [{"method": method, "count": count} for method, count in sorted(callbacks.items())],
            "handlers": [{"quest_id": quest_id, "path": path} for quest_id, path in handlers],
            "sites": sorted(sites, key=lambda site: (site["quest_id"], site["path"], site["line"], site["select"])),
        })
    operations.sort(key=lambda operation: (-operation["call_count"], operation["signature"]))
    unclassified = [operation["signature"] for operation in operations
                    if operation["mechanical_role"] == "UNCLASSIFIED_DIRECT_CALL"]
    pending = [operation["signature"] for operation in operations
               if operation["semantic_status"] == "PENDING_REVIEW"]
    blockers = []
    if unclassified:
        blockers.append({"kind": "UNCLASSIFIED_DIRECT_CALL", "count": len(unclassified)})
    if pending:
        blockers.append({"kind": "PENDING_DIRECT_CALL_REVIEW", "count": len(pending)})
    return {
        **base_report(inventory.input_hash),
        "authority": "CURRENT_HANDLER_AST",
        "classification": "DERIVED",
        "counts": {
            "signatures": len(operations),
            "shared_signatures": sum(operation["handler_count"] >= 2 for operation in operations),
            "unique_outliers": sum(operation["handler_count"] == 1 for operation in operations),
            "calls": sum(operation["call_count"] for operation in operations),
            "mechanical_roles": len({operation["mechanical_role"] for operation in operations}),
            "reviewed_signatures": sum(operation["semantic_status"] == "DERIVED_REVIEWED" for operation in operations),
            "pending_review_signatures": sum(operation["semantic_status"] == "PENDING_REVIEW" for operation in operations),
            "unclassified_signatures": len(unclassified),
        },
        "unclassified_signatures": unclassified,
        "operations": operations,
        "blockers": blockers,
    }


def client_hyperlink_family(token: str) -> str | None:
    normalized = token.upper()
    if normalized.startswith("HACTION_ASK_QUEST_ACCEPT") or normalized.startswith("HACTION_QUEST_ACCEPT") \
            or normalized.startswith("HACTION_QUEST_REFUSE"):
        return "ACCEPTANCE"
    if normalized.startswith("HACTION_CHECK_"):
        return "CHECK"
    if normalized == "HACTION_FINISH_DIALOG":
        return "FINISH"
    if normalized.startswith("HACTION_SELECT_QUEST_REWARD") or normalized == "HACTION_SET_SUCCEED":
        return "REWARD"
    if normalized.startswith("HACTION_SETPRO"):
        return "PROGRESS"
    if normalized.startswith("HACTION_SELECT"):
        return "CUSTOM_SELECT"
    return None


def handler_dialog_family(value: str) -> str | None:
    value = value.removeprefix("QuestDialog.")
    if re.fullmatch(r"-?\d+", value):
        dialog_id = int(value)
        if dialog_id == -1:
            return "SERVER_CONTEXT"
        if 8 <= dialog_id <= 23 or dialog_id == 1009:
            return "REWARD"
        if dialog_id in {1002, 1003, 1004, 1007, 20000, 20001}:
            return "ACCEPTANCE"
        if dialog_id in {39, 20002, 20004, 20005}:
            return "CHECK"
        if dialog_id == 1008:
            return "FINISH"
        if 10000 <= dialog_id <= 10040 or dialog_id in {10255, 20003}:
            return "PROGRESS"
        return "CUSTOM_SELECT"
    if value.startswith(("ACCEPT_QUEST", "REFUSE_QUEST", "ASK_ACCEPTION")):
        return "ACCEPTANCE"
    if value.startswith("CHECK_"):
        return "CHECK"
    if value == "FINISH_DIALOG":
        return "FINISH"
    if value.startswith("SELECTED_QUEST_REWARD") or value in {"SELECT_NO_REWARD", "SELECT_REWARD", "SET_REWARD"}:
        return "REWARD"
    if value.startswith("STEP_TO_") or value == "SETPRO_NEXT":
        return "PROGRESS"
    if value.startswith("SELECT_ACTION_") or value == "EXCHANGE_COIN":
        return "CUSTOM_SELECT"
    if value in {"NO_RIGHTS", "NULL", "START_DIALOG", "USE_OBJECT"}:
        return "SERVER_CONTEXT"
    return None


DIALOG_HELPER_FAMILIES = {
    "changeQuestStep": {"PROGRESS"},
    "checkItemExistence": {"CHECK"},
    "checkQuestItems": {"CHECK"},
    "checkQuestItemsSimple": {"CHECK"},
    "defaultCloseDialog": {"PROGRESS"},
    "finishQuest": {"REWARD"},
    "sendQuestEndDialog": {"REWARD"},
    "sendQuestRewardDialog": {"REWARD"},
    "sendQuestStartDialog": {"ACCEPTANCE", "FINISH"},
    "startQuest": {"ACCEPTANCE"},
}


def build_handler_dialog_topology(inventory: InventoryBundle, dialogs: DialogBundle) -> dict[str, Any]:
    client_by_id = {quest["quest_id"]: quest for quest in dialogs.flow_report["quests"]}
    quests = []
    unknown_handler_inputs: Counter[str] = Counter()
    unknown_client_actions: Counter[str] = Counter()
    for row in inventory.java_rows:
        if not row["handler_candidate"] or "onDialogEvent" not in row["methods"] or len(row["quest_ids"]) != 1:
            continue
        quest_id = row["quest_ids"][0]
        branches = row.get("dialog_branches", [])
        inputs = sorted({
            branch["value"].removeprefix("QuestDialog.")
            for branch in branches if branch["kind"] != "CONDITION"
        })
        handler_families = set()
        for value in inputs:
            family = handler_dialog_family(value)
            if family is None:
                unknown_handler_inputs[value] += 1
            else:
                handler_families.add(family)
        dialog_calls = sorted({
            call["method"] for call in row["calls"] if call["enclosing_method"] == "onDialogEvent"
        })
        for method in dialog_calls:
            handler_families.update(DIALOG_HELPER_FAMILIES.get(method, ()))

        client = client_by_id.get(quest_id)
        client_actions = sorted({
            token for action in client["actions"] for token in action["hactions"]
        }) if client else []
        client_families = set()
        for token in client_actions:
            family = client_hyperlink_family(token)
            if family is None:
                unknown_client_actions[token] += 1
            else:
                client_families.add(family)
        comparable_handler = handler_families - {"SERVER_CONTEXT"}
        quests.append({
            "quest_id": quest_id,
            "handler_path": row["path"],
            "client_path": client["path"] if client else None,
            "handler_branch_records": len(branches),
            "handler_inputs": inputs,
            "handler_dialog_calls": dialog_calls,
            "handler_families": sorted(handler_families),
            "client_actions": client_actions,
            "client_families": sorted(client_families),
            "client_only_family_observations": sorted(client_families - comparable_handler),
            "handler_only_family_observations": sorted(comparable_handler - client_families),
            "classification": "DERIVED_AUXILIARY_COMPARISON",
        })
    quests.sort(key=lambda quest: quest["quest_id"])
    blockers = []
    if unknown_handler_inputs:
        blockers.append({"kind": "UNKNOWN_HANDLER_DIALOG_INPUT", "count": len(unknown_handler_inputs)})
    if unknown_client_actions:
        blockers.append({"kind": "UNKNOWN_CLIENT_DIALOG_FAMILY", "count": len(unknown_client_actions)})
    return {
        **base_report(content_hash((inventory.input_hash + dialogs.input_hash).encode("ascii"))),
        "authority": "CURRENT_HANDLER_AST_AND_CLIENT_AUXILIARY",
        "classification": "DERIVED",
        "counts": {
            "java_dialog_handlers": len(quests),
            "handlers_with_client_dialog": sum(quest["client_path"] is not None for quest in quests),
            "handlers_without_client_dialog": sum(quest["client_path"] is None for quest in quests),
            "handler_branch_records": sum(quest["handler_branch_records"] for quest in quests),
            "client_only_family_observations": sum(len(quest["client_only_family_observations"]) for quest in quests),
            "handler_only_family_observations": sum(len(quest["handler_only_family_observations"]) for quest in quests),
            "unknown_handler_inputs": len(unknown_handler_inputs),
            "unknown_client_actions": len(unknown_client_actions),
        },
        "family_contract": {
            "families": ["ACCEPTANCE", "CHECK", "CUSTOM_SELECT", "FINISH", "PROGRESS", "REWARD"],
            "server_context": "START_DIALOG, USE_OBJECT, NULL and NO_RIGHTS are not client hyperlink families",
            "comparison_limit": "family observations are auxiliary topology evidence, not server-semantic conflicts",
        },
        "unknown_handler_inputs": [{"value": value, "count": count}
                                   for value, count in sorted(unknown_handler_inputs.items())],
        "unknown_client_actions": [{"token": token, "count": count}
                                   for token, count in sorted(unknown_client_actions.items())],
        "quests": quests,
        "blockers": blockers,
    }


def build_routing_contract(config: Config) -> dict[str, Any]:
    paths = [config.project_root / relative for relative in ROUTING_SOURCE_PATHS]
    missing = [path for path in paths if not path.is_file()]
    if missing:
        raise AuditError(f"Missing routing source: {missing[0]}")
    engine_path = paths[0]
    source_methods = set(re.findall(
        r"public\s+[\w<>, ?\[\]]+\s+(on[A-Z]\w*|rideAction)\s*\(",
        engine_path.read_text(encoding="utf-8"),
    ))
    catalog_methods = {method for policy in ROUTING_POLICIES for method in policy["methods"]}
    if source_methods != catalog_methods:
        missing_methods = sorted(source_methods - catalog_methods)
        stale_methods = sorted(catalog_methods - source_methods)
        raise AuditError(f"Routing policy inventory drift: missing={missing_methods}, stale={stale_methods}")
    manifest, input_hash = input_manifest({"routing-java": (config.project_root, paths)})
    return {
        **base_report(input_hash),
        "authority": "CURRENT_QUEST_ENGINE_AND_CALLERS",
        "classification": "DERIVED",
        "semantic_status": "REVIEWED_CURRENT_BEHAVIOR",
        "input_manifest": manifest,
        "counts": {
            "dispatch_entries": len(catalog_methods),
            "policies": len(ROUTING_POLICIES),
            "caller_contracts": len(ROUTING_CALLER_CONTRACTS),
        },
        "policies": list(ROUTING_POLICIES),
        "caller_contracts": list(ROUTING_CALLER_CONTRACTS),
        "shared_context": {
            "quest_id": "QuestEnv.questId is mutated before each callback",
            "reset": "only exhausted NPC-discovery onDialog resets questId to 0",
            "order": "registry insertion order; handler load order supplies insertion order",
        },
        "failure_observations": [
            "a caught exception around a fanout loop aborts remaining listeners",
            "some fanout entries have no catch and propagate callback exceptions",
            "onQuestTimerEnd dereferences a missing handler outside its null guard",
            "FAILED has caller-specific meaning and is not interchangeable with UNKNOWN",
        ],
        "blockers": [],
    }


def group_by(rows: Iterable[dict[str, Any]], field: str) -> dict[str, list[dict[str, Any]]]:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in rows:
        grouped[row[field]].append(row)
    return dict(grouped)


def build_catalog(inventory: InventoryBundle, shapes: dict[str, Any], dialogs: DialogBundle) -> dict[str, Any]:
    handlers = inventory.handler_report["handlers"]
    event_counts: Counter[str] = Counter()
    for handler in handlers:
        event_counts.update(handler["event_methods"])
    registration_signatures = signature_counts(handlers, "register_calls")
    helper_signatures = signature_counts(handlers, "helper_calls")
    state_read_signatures = signature_counts(handlers, "state_reads")
    state_write_signatures = signature_counts(handlers, "state_writes")
    service_signatures = signature_counts(handlers, "service_calls")
    unclassified_events = sorted(set(event_counts) - set(EVENT_FAMILIES))
    unclassified_registrations = [entry["signature"] for entry in registration_signatures
                                  if entry["signature"] not in REGISTRATION_FAMILIES]
    unclassified_helpers = [entry["signature"] for entry in helper_signatures
                            if helper_family(entry["signature"]) is None]
    unclassified_state_reads = [entry["signature"] for entry in state_read_signatures
                                if state_read_family(entry["signature"]) is None]
    unclassified_state_writes = [entry["signature"] for entry in state_write_signatures
                                 if state_write_family(entry["signature"]) is None]
    unclassified_services = [entry["signature"] for entry in service_signatures
                             if service_family(entry["signature"]) is None]
    catalog_blockers = inventory.handler_report["blockers"] + inventory.external_report["blockers"] \
        + dialogs.conflict_report["blockers"] + dialogs.action_report["blockers"]
    if unclassified_events:
        catalog_blockers.append({"kind": "UNCLASSIFIED_EVENT", "count": len(unclassified_events)})
    if unclassified_registrations:
        catalog_blockers.append({"kind": "UNCLASSIFIED_REGISTRATION", "count": len(unclassified_registrations)})
    if unclassified_helpers:
        catalog_blockers.append({"kind": "UNCLASSIFIED_HELPER", "count": len(unclassified_helpers)})
    if unclassified_state_reads:
        catalog_blockers.append({"kind": "UNCLASSIFIED_STATE_READ", "count": len(unclassified_state_reads)})
    if unclassified_state_writes:
        catalog_blockers.append({"kind": "UNCLASSIFIED_STATE_WRITE", "count": len(unclassified_state_writes)})
    if unclassified_services:
        catalog_blockers.append({"kind": "UNCLASSIFIED_SERVICE_CALL", "count": len(unclassified_services)})
    requirements = [
        {"id": "QG-CORE-001", "evidence": {"java_files": len(inventory.java_rows),
                                            "xml_owners": inventory.handler_report["counts"]["xml_handlers"]}},
        {"id": "QG-CORE-002", "evidence": {"event_methods": sum(event_counts.values())}},
        {"id": "QG-CORE-003", "evidence": {"registration_signatures": len(signature_counts(handlers, "register_calls"))}},
        {"id": "QG-CORE-004", "evidence": {"state_read_signatures": len(signature_counts(handlers, "state_reads"))}},
        {"id": "QG-CORE-005", "evidence": {"service_call_signatures": len(signature_counts(handlers, "service_calls"))}},
        {"id": "QG-CORE-006", "evidence": {"state_write_signatures": len(signature_counts(handlers, "state_writes"))}},
        {"id": "QG-CORE-007", "evidence": {"mechanical_shapes": shapes["counts"]["shapes"]}},
        {"id": "QG-CORE-008", "evidence": {"handlers_with_state_reads":
                                            sum(bool(handler["state_reads"]) for handler in handlers)}},
        {"id": "QG-CORE-009", "evidence": {"recovery_event_methods":
                                            sum(count for method, count in event_counts.items()
                                                if any(token in method.lower()
                                                       for token in ("timer", "die", "world", "logout")))}},
        {"id": "QG-CORE-010", "evidence": {
            "reviewed_external_surfaces": inventory.external_report["counts"]["reviewed_execution_surfaces"],
        }},
        {"id": "QG-CORE-011", "evidence": {
            "kill_event_handlers": sum(count for method, count in event_counts.items() if "Kill" in method),
        }},
        {"id": "QG-CORE-012", "evidence": {
            "helper_call_signatures": len(helper_signatures),
        }},
        {"id": "QG-CORE-013", "evidence": {"client_dialog_quests": dialogs.flow_report["counts"]["quests"]}},
        {"id": "QG-CORE-014", "evidence": {
            "owned_quests": inventory.handler_report["counts"]["unique_owned_quests"],
            "external_writers": inventory.external_report["counts"]["confirmed_task_writes"],
            "external_readers": inventory.external_report["counts"]["confirmed_task_reads"],
            "owner_conflicts": inventory.handler_report["counts"]["owner_conflicts"],
        }},
    ]
    for requirement in requirements:
        requirement["status"] = "EVIDENCE_REVIEWED"
        requirement["classification"] = "DERIVED"
    combined_hash = content_hash((inventory.input_hash + dialogs.input_hash).encode("ascii"))
    return {
        **base_report(combined_hash),
        "authority": "CURRENT_HANDLER_AND_CLIENT_AUXILIARY",
        "classification": "DERIVED",
        "event_methods": [{"method": method, "family": EVENT_FAMILIES.get(method, "UNCLASSIFIED_EVENT"),
                           "count": count} for method, count in sorted(event_counts.items())],
        "registration_signatures": [{**entry,
                                     "family": REGISTRATION_FAMILIES.get(entry["signature"],
                                                                          "UNCLASSIFIED_REGISTRATION")}
                                    for entry in registration_signatures],
        "state_read_signatures": [{**entry,
                                   "family": state_read_family(entry["signature"]) or "UNCLASSIFIED_STATE_READ"}
                                  for entry in state_read_signatures],
        "state_write_signatures": [{**entry,
                                    "family": state_write_family(entry["signature"]) or "UNCLASSIFIED_STATE_WRITE"}
                                   for entry in state_write_signatures],
        "helper_call_signatures": [{**entry,
                                    "family": helper_family(entry["signature"]) or "UNCLASSIFIED_HELPER"}
                                   for entry in helper_signatures],
        "service_call_signatures": [{**entry,
                                     "family": service_family(entry["signature"]) or "UNCLASSIFIED_SERVICE_CALL"}
                                    for entry in service_signatures],
        "direct_call_signatures": signature_counts(handlers, "direct_calls"),
        "reference_signatures": signature_counts(handlers, "reference_signatures"),
        "capability_families": [
            {"family": family, "shape_count": len(rows), "handler_count": sum(row["member_count"] for row in rows)}
            for family, rows in sorted(group_by(shapes["shapes"], "capability_family").items())
        ],
        "spec_requirements": requirements,
        "unclassified_events": unclassified_events,
        "unclassified_registrations": unclassified_registrations,
        "unclassified_helpers": unclassified_helpers,
        "unclassified_state_reads": unclassified_state_reads,
        "unclassified_state_writes": unclassified_state_writes,
        "unclassified_services": unclassified_services,
        "blockers": catalog_blockers,
    }


def build_summary(inventory: InventoryBundle, dialogs: DialogBundle,
                  shapes: dict[str, Any], catalog: dict[str, Any], dialog_topology: dict[str, Any]) -> str:
    handler_counts = inventory.handler_report["counts"]
    dialog_counts = dialogs.flow_report["counts"]
    shape_counts = shapes["counts"]
    lines = [
        "# Quest Migration PHASE-00 Summary",
        "",
        f"- Spec: `{SPEC_ID}` revision `{SPEC_REVISION}`",
        f"- Tool version: `{tool_version()}`",
        f"- Java handlers: {handler_counts['java_handlers']}",
        f"- XML handlers: {handler_counts['xml_handlers']}",
        f"- Unique owned quests: {handler_counts['unique_owned_quests']}",
        f"- Owner conflicts: {handler_counts['owner_conflicts']}",
        f"- Dynamic Java owner IDs: {handler_counts['dynamic_java_handlers']}",
        f"- Confirmed external state-reader call sites: {inventory.external_report['counts']['confirmed_task_reads']}",
        f"- Confirmed external state-writer call sites: {inventory.external_report['counts']['confirmed_task_writes']}",
        f"- Non-quest method-name collisions: {inventory.external_report['counts']['non_quest_method_collisions']}",
        f"- Ambiguous external task accesses: {inventory.external_report['counts']['ambiguous_accesses']}",
        f"- Event callback methods: {len(catalog['event_methods'])}",
        f"- Event registration signatures: {len(catalog['registration_signatures'])}",
        f"- State read signatures: {len(catalog['state_read_signatures'])}",
        f"- State write signatures: {len(catalog['state_write_signatures'])}",
        f"- QuestHandler helper signatures: {len(catalog['helper_call_signatures'])}",
        f"- Service call signatures: {len(catalog['service_call_signatures'])}",
        f"- Residual direct call signatures: {len(catalog['direct_call_signatures'])}",
        f"- Typed reference signatures: {len(catalog['reference_signatures'])}",
        f"- Mechanical shapes: {shape_counts['shapes']}",
        f"- Repeated shapes: {shape_counts['repeated_shapes']}",
        f"- Handlers in repeated shapes: {shape_counts['handlers_in_repeated_shapes']}",
        f"- Unique outliers: {shape_counts['unique_outliers']}",
        f"- Client dialog quest files: {dialog_counts['files']}",
        f"- Client dialog quest IDs: {dialog_counts['quests']}",
        f"- Client dialog opaque-content recoveries: {dialog_counts['recovered_opaque_content_files']}",
        f"- Client dialog conflicts: {dialog_counts['conflicting_quest_ids']}",
        f"- Unknown client dialog actions: {len(dialogs.action_report['unknown_actions'])}",
        f"- Handler dialog branch records: {dialog_topology['counts']['handler_branch_records']}",
        f"- Java dialog handlers matched to client dialogs: {dialog_topology['counts']['handlers_with_client_dialog']}",
        f"- Unknown handler/client dialog families: "
        f"{dialog_topology['counts']['unknown_handler_inputs'] + dialog_topology['counts']['unknown_client_actions']}",
        f"- Catalog blockers: {len(catalog['blockers'])}",
        "",
        "All counts are current-baseline facts or mechanically DERIVED classifications. "
        "They do not prove runtime parity or authorize ownership changes.",
        "",
    ]
    return "\n".join(lines)


def write_or_check(path: Path, content: str, check: bool) -> None:
    if check:
        if not path.is_file():
            raise AuditError(f"Missing generated report: {path}")
        current = path.read_text(encoding="utf-8")
        if current != content:
            raise AuditError(f"Generated report drift: {path}")
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            dir=path.parent,
            prefix=f".{path.name}.",
            delete=False,
        ) as stream:
            stream.write(content)
            temporary = Path(stream.name)
        os.replace(temporary, path)
    finally:
        if temporary is not None and temporary.exists():
            temporary.unlink()


def emit(config: Config, outputs: dict[str, Any]) -> None:
    for name, value in sorted(outputs.items()):
        content = value if isinstance(value, str) else canonical_json(value)
        write_or_check(config.report_dir / name, content, config.check)


def validate_report_dir(config: Config) -> None:
    allowed = (config.project_root / "docs/quest/audit/reports").resolve()
    report = config.report_dir.resolve()
    if report != allowed and allowed not in report.parents:
        raise AuditError(f"Report directory must be inside {allowed}: {report}")


def command_outputs(command: str, config: Config) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    inventory = collect_inventory(config) if command in {
        "inventory", "shapes", "dialog-shapes", "plan", "catalog", "report", "generate", "all",
    } else None
    dialogs = collect_dialogs(config) if command in {"dialogs", "catalog", "report", "all"} else None
    outputs: dict[str, Any] = {}
    blockers: list[dict[str, Any]] = []
    if command in {"graph-inputs", "all"}:
        graph_inputs = build_quest_graph_input_report(config)
        outputs["input-manifest-quest-graphs.json"] = graph_inputs
        blockers.extend(graph_inputs["blockers"])
    if command in {"start-conditions", "all"}:
        start_conditions = build_start_condition_report(config)
        outputs["start-condition-expansion.json"] = start_conditions
        blockers.extend(start_conditions["blockers"])
    if inventory is not None:
        if command in {"inventory", "all"}:
            outputs["input-manifest-java.json"] = inventory.manifest
            outputs["java-ast-inventory.json"] = {
                **base_report(inventory.input_hash),
                "authority": "CURRENT_SOURCE_AST",
                "classification": "DERIVED",
                "files": inventory.java_rows,
                "blockers": [],
            }
            outputs["handler-inventory.json"] = inventory.handler_report
            outputs["external-ownership.json"] = inventory.external_report
        blockers.extend(inventory.handler_report["blockers"])
        if command in {"inventory", "catalog", "report", "all"}:
            blockers.extend(inventory.external_report["blockers"])
    if dialogs is not None:
        if command in {"dialogs", "all"}:
            outputs["input-manifest-dialogs.json"] = dialogs.manifest
            outputs["client-dialog-flow.json"] = dialogs.flow_report
            outputs["client-dialog-actions.json"] = dialogs.action_report
            outputs["client-dialog-conflicts.json"] = dialogs.conflict_report
        blockers.extend(dialogs.conflict_report["blockers"])
        blockers.extend(dialogs.action_report["blockers"])
    shapes = build_shapes(inventory) if inventory is not None and command in {
        "shapes", "dialog-shapes", "plan", "catalog", "report", "generate", "all",
    } else None
    if shapes is not None and command in {"shapes", "all"}:
        outputs["mechanical-shapes.json"] = shapes
    capability_report = (analyze_dialog_shape_capabilities(config, inventory, shapes)
                         if shapes is not None and command in {"dialog-shapes", "plan", "all"} else None)
    if capability_report is not None and command in {"dialog-shapes", "all"}:
        outputs["dialog-shape-capabilities.json"] = capability_report
    if capability_report is not None and command in {"plan", "all"}:
        outputs["capability-dependency-graph.json"] = build_capability_dependency_graph(capability_report, inventory)
    if shapes is not None and command == "generate":
        generated, generation_blockers = build_dialog_standard_outputs(config, inventory, shapes)
        outputs.update(generated)
        blockers.extend(generation_blockers)
    if command == "verify":
        blockers.extend(verify_dialog_standard_outputs(config))
    direct_catalog = build_direct_call_catalog(inventory) if command in {"catalog", "report", "all"} else None
    routing_contract = build_routing_contract(config) if command in {"catalog", "report", "all"} else None
    dialog_topology = build_handler_dialog_topology(inventory, dialogs) if command in {"catalog", "report", "all"} else None
    catalog = build_catalog(inventory, shapes, dialogs) if command in {"catalog", "report", "all"} else None
    if catalog is not None and command in {"catalog", "all"}:
        catalog["blockers"].extend(direct_catalog["blockers"])
        catalog["blockers"].extend(dialog_topology["blockers"])
        outputs["capability-catalog.json"] = catalog
        outputs["direct-call-catalog.json"] = direct_catalog
        outputs["handler-dialog-topology.json"] = dialog_topology
        outputs["routing-result-contract.json"] = routing_contract
    if direct_catalog is not None:
        blockers.extend(direct_catalog["blockers"])
    if dialog_topology is not None:
        blockers.extend(dialog_topology["blockers"])
    if command in {"report", "all"}:
        outputs["summary.md"] = build_summary(inventory, dialogs, shapes, catalog, dialog_topology)
    return outputs, blockers


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description="Deterministic quest migration audit and candidate generator")
    subparsers = result.add_subparsers(dest="command", required=True)
    project_default = Path(__file__).resolve().parents[2]
    for name in ("inventory", "dialogs", "shapes", "dialog-shapes", "plan", "catalog", "graph-inputs", "start-conditions",
                 "report", "generate", "verify", "all"):
        command = subparsers.add_parser(name)
        command.add_argument("--check", action="store_true")
        command.add_argument("--project-root", type=Path, default=project_default)
        command.add_argument(
            "--dialogs-root",
            type=Path,
            default=Path("/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs"),
        )
        command.add_argument("--report-dir", type=Path)
        if name in {"generate", "verify"}:
            command.add_argument("--family", choices=(DIALOG_STANDARD_FAMILY,), required=True)
    return result


def main(argv: list[str] | None = None) -> int:
    arguments = parser().parse_args(argv)
    project_root = arguments.project_root.resolve()
    report_dir = (arguments.report_dir or project_root / "docs/quest/audit/reports").resolve()
    config = Config(
        project_root=project_root,
        java_root=project_root / "src/main/java",
        java_handler_root=project_root / "src/main/java/com/aionemu/gameserver/quest/handlers",
        xml_handler_root=project_root / "src/main/resources/aion/data/static_data/quest_script_data",
        dialogs_root=arguments.dialogs_root.resolve(),
        report_dir=report_dir,
        check=arguments.check,
    )
    try:
        validate_report_dir(config)
        outputs, blockers = command_outputs(arguments.command, config)
        emit(config, outputs)
        if blockers:
            kinds = ", ".join(f"{blocker['kind']}={blocker['count']}" for blocker in blockers)
            raise AuditError(f"Audit blockers: {kinds}")
        return 0
    except AuditError as error:
        print(error, file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())

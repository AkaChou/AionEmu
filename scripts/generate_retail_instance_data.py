#!/usr/bin/env python3
"""Generate deterministic AionEmu instance definitions from 5.8 retail data."""

from __future__ import annotations

import argparse
import hashlib
import re
import tempfile
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path


TABLES = (
    "instance_creation.xml",
    "instance_restrict.xml",
    "instance_cooltime.xml",
    "instance_cooltime2.xml",
    "matchmaker.xml",
    "team_match_maker.xml",
    "world_timeattack.xml",
    "world_timeattack2.xml",
    "infinity_indun_reward.xml",
    "instant_dungeon_battleground.xml",
    "instant_dungeon_idarenapvp.xml",
    "instant_dungeon_tournament.xml",
    "luna_indun.xml",
    "luna_cost.xml",
    "npc_scores.xml",
)
REWARD_TABLES = TABLES[6:]
MATCH_HANDLER_BY_WORLD = {
    300110000: "DREDGION",
    300210000: "DREDGION",
    300440000: "DREDGION",
    301650000: "DREDGION",
    301120000: "KAMAR",
    301210000: "OPHIDAN",
    301220000: "BASTION",
    301310000: "IDGEL_DOME",
    301670000: "SUSPICIOUS_OPHIDAN",
    301680000: "IDGEL_DOME_LANDMARK",
    302320000: "HALL_OF_TENACITY",
}
MATCH_CATEGORY_BY_WORLD = {
    300350000: "PVP_FFA",
    300360000: "PVP_SOLO",
    300420000: "TRAINING_PVP_FFA",
    300430000: "TRAINING_PVP_SOLO",
    300450000: "HARMONY",
    300550000: "GLORY",
    300570000: "TRAINING_HARMONY",
    301100000: "TRAINING_HARMONY",
}
MATCH_ADAPTER_BY_WORLD = {
    300110000: "AutoDredgionInstance",
    300210000: "AutoDredgionInstance",
    300440000: "AutoDredgionInstance",
    301650000: "AutoAsyunatarDredgionInstance",
    301120000: "AutoKamarBattlefieldInstance",
    301210000: "AutoEngulfedOphidanBridgeInstance",
    301220000: "AutoIronWallWarfrontInstance",
    301310000: "AutoIdgelDomeInstance",
    301670000: "AutoSuspiciousOphidanBridgeInstance",
    301680000: "AutoIdgelDomeLandmarkInstance",
}
MATCH_ADAPTER_BY_CATEGORY = {
    "HARMONY": "AutoHarmonyInstance",
    "TRAINING_HARMONY": "AutoHarmonyInstance",
    "PVP_FFA": "AutoPvPFFAInstance",
    "PVP_SOLO": "AutoPvPFFAInstance",
    "TRAINING_PVP_FFA": "AutoPvPFFAInstance",
    "TRAINING_PVP_SOLO": "AutoPvPFFAInstance",
    "GLORY": "AutoPvPFFAInstance",
}
MATCH_ADAPTER_PACKAGE = "com.aionemu.gameserver.model.autogroup."
TOURNAMENT_MATCH_IDS = {124, 125, 127, 128, 129, 130}
CLIENT_FILES = (
    "bin64/Aion.bin",
    "bin64/Game.dll",
    "system.cfg",
    "单机启动.bat",
    "单机启动 - 副本.bat",
)
SPECIAL_INSTANCE_WORLDS = {
    310060000: "剧情特殊地图，不属于 instance_creation",
    400030000: "Panesterra 子地图，由世界/战场系统管理",
    600080000: "演唱会活动地图，由活动 AI 创建",
    720010000: "Oriel 住宅个人空间，由 HousingService 管理",
    730010000: "Pernon 住宅个人空间，由 HousingService 管理",
}
NON_PRODUCTION_INSTANCE_WORLDS = {900210000, 900230000}
HOUSING_INSTANCE_WORLDS = {720010000, 730010000}
EVENT_INSTANCE_WORLDS = {600080000}
BEHAVIORS = (
    "HANDLER",
    "RETAIL_AI_QUEST",
    "MATCHMAKER",
    "TOURNAMENT",
    "HOUSING",
    "EVENT",
    "DATA_ONLY",
    "EXCLUDED_NON_PRODUCTION",
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def source_file(source: Path, name: str) -> tuple[Path, str]:
    regional = source / "China" / name
    return (regional, "China") if regional.is_file() else (source / name, "base")


def read_rows(path: Path) -> list[dict[str, str]]:
    return [
        {child.tag: (child.text or "").strip() for child in node}
        for node in ET.parse(path).getroot()
    ]


def positive_ids(rows: list[dict[str, str]], table: str) -> dict[str, dict[str, str]]:
    result = {}
    for row in rows:
        identifier = row.get("id", "")
        if not identifier.isdigit() or int(identifier) <= 0:
            raise ValueError(f"{table}: invalid id {identifier!r}")
        if identifier in result:
            raise ValueError(f"{table}: duplicate id {identifier}")
        result[identifier] = row
    return result


def dedupe_rows(rows: list[dict[str, str]], table: str) -> tuple[list[dict[str, str]], int]:
    result = {}
    duplicates = 0
    for row in rows:
        identifier = row.get("id", "")
        previous = result.get(identifier)
        if previous is None:
            result[identifier] = row
        elif previous == row:
            duplicates += 1
        else:
            raise ValueError(f"{table}: conflicting duplicate id {identifier}")
    return list(result.values()), duplicates


def unique_names(rows: list[dict[str, str]], table: str) -> dict[str, dict[str, str]]:
    result = {}
    for row in rows:
        name = row.get("name", "").casefold()
        if not name:
            raise ValueError(f"{table}: missing name for id {row.get('id', '?')}")
        if name in result:
            raise ValueError(f"{table}: duplicate name {row['name']}")
        result[name] = row
    return result


def world_ids(path: Path) -> dict[str, int]:
    result = {}
    for node in ET.parse(path).getroot():
        name = (node.text or "").strip().casefold()
        identifier = node.get("id", "")
        if not name or not identifier.isdigit():
            raise ValueError("WorldId.xml contains an incomplete mapping")
        if name in result:
            raise ValueError(f"duplicate world name: {name}")
        result[name] = int(identifier)
    return result


def local_instance_worlds(path: Path) -> dict[int, str]:
    return {
        int(node.get("id")): node.get("name", "")
        for node in ET.parse(path).getroot()
        if node.get("instance") == "true"
    }


def instance_handlers(aionemu: Path, active: set[int]) -> dict[int, Path]:
    root = aionemu / "src/main/java/com/aionemu/gameserver/instance/handlers/scripts"
    result = {}
    for path in sorted(root.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        for annotation in re.findall(r"@InstanceID\s*\((.*?)\)", text, re.DOTALL):
            for value in re.findall(r"\b\d{9}\b", annotation):
                world_id = int(value)
                if world_id in active:
                    if world_id in result:
                        raise ValueError(f"duplicate instance handlers for {world_id}: {result[world_id]}, {path}")
                    result[world_id] = path.relative_to(aionemu)
    return result


def java_world_references(root: Path, active: set[int]) -> dict[int, Path]:
    result = {}
    for path in sorted(root.rglob("*.java")):
        for value in re.findall(r"\b\d{9}\b", path.read_text(encoding="utf-8")):
            world_id = int(value)
            if world_id in active:
                result.setdefault(world_id, path)
    return result


def attributes(row: dict[str, str], extra: dict[str, object] | None = None) -> dict[str, str]:
    values = {key: value for key, value in sorted(row.items()) if value != ""}
    for key, value in sorted((extra or {}).items()):
        if value not in (None, ""):
            values[key] = str(value)
    return values


def write_xml(root: ET.Element, path: Path) -> None:
    ET.indent(root, space="  ")
    path.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(root).write(path, encoding="UTF-8", xml_declaration=True)


def schema(root: ET.Element) -> None:
    root.set("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
    root.set("xsi:noNamespaceSchemaLocation", "../../schemas/retail-instance-data.xsd")


def referenced_item_names(tables: dict[str, list[dict[str, str]]]) -> set[str]:
    names = set()
    for table in REWARD_TABLES:
        for row in tables[table]:
            for key, value in row.items():
                if (value and not value.isdigit() and key != "name" and "item" in key
                        and (key.endswith("_name") or "_name_" in key or key.endswith("_calculate"))):
                    names.add(value.casefold())
    for row in tables["instance_restrict.xml"]:
        for key, value in row.items():
            if value and not value.isdigit() and key.startswith("item") and key.endswith("_name"):
                names.add(value.casefold())
    return names


def resolve_items(path: Path, wanted: set[str]) -> dict[str, str]:
    result = {}
    for _event, node in ET.iterparse(path, events=("end",)):
        if node.tag != "item":
            continue
        name = (node.findtext("name") or "").strip().casefold()
        if name in wanted:
            identifier = (node.findtext("id") or "").strip()
            if not identifier.isdigit():
                raise ValueError(f"invalid item id for {name}")
            result[name] = identifier
            if len(result) == len(wanted):
                break
        node.clear()
    return result


def add_item_ids(row: dict[str, str], item_ids: dict[str, str]) -> dict[str, str]:
    result = {}
    for key, value in row.items():
        if (key != "name" and "item" in key
                and (key.endswith("_name") or "_name_" in key or key.endswith("_calculate")) and value):
            item_id = item_ids.get(value.casefold())
            if item_id:
                if key.endswith("_calculate"):
                    result[key + "_id"] = item_id
                else:
                    result[key.replace("_name_", "_id_").removesuffix("_name")
                           + ("_id" if key.endswith("_name") else "")] = item_id
    return result


def resolve_match_npcs(path: Path, match_ids: set[int]) -> tuple[dict[int, set[int]], dict[int, set[int]]]:
    normal = defaultdict(set)
    custom = defaultdict(set)
    for _event, node in ET.iterparse(path, events=("end",)):
        if node.tag != "npc":
            continue
        identifier = (node.findtext("id") or "").strip()
        if identifier.isdigit():
            npc_id = int(identifier)
            for field, target in (("match_maker", normal), ("custom_match_maker", custom)):
                for value in re.findall(r"\d+", node.findtext(field) or ""):
                    match_id = int(value)
                    if match_id in match_ids:
                        target[match_id].add(npc_id)
        node.clear()
    return normal, custom


def resolve_string_ids(path: Path, wanted: set[str]) -> dict[str, int]:
    result = {}
    wanted_by_case = {value.casefold(): value for value in wanted}
    identifier = None
    name = None
    with path.open(encoding="utf-16") as stream:
        for line in stream:
            value = line.strip()
            if value.startswith("<id>") and value.endswith("</id>"):
                text = value[4:-5]
                identifier = int(text) if text.isdigit() else None
            elif value.startswith("<name>") and value.endswith("</name>"):
                name = value[6:-7]
            elif value == "</string>":
                wanted_name = wanted_by_case.get((name or "").casefold())
                if identifier is not None and wanted_name is not None:
                    result[wanted_name] = identifier
                    if len(result) == len(wanted):
                        break
                identifier = None
                name = None
    missing = sorted(wanted - result.keys())
    if missing:
        raise ValueError(f"unmapped retail strings: {', '.join(missing[:20])}")
    return result


def launch_args(path: Path) -> str:
    text = path.read_text(encoding="ascii", errors="strict")
    match = re.search(r"(?im)^start\s+bin64\\aion\.bin\s+(.+)$", text)
    if not match:
        raise ValueError(f"cannot find Aion.bin launch arguments in {path}")
    return match.group(1).strip()


def tournament_world_points(worlds_dir: Path, world_name: str, alias: str) -> str:
    directories = {path.name.casefold(): path for path in worlds_dir.iterdir() if path.is_dir()}
    directory = directories.get(world_name.casefold())
    if directory is None:
        raise ValueError(f"missing tournament world directory: {world_name}")
    root = ET.parse(directory / "world.xml").getroot()
    points = []
    if alias:
        for location in root.iter("location_alias"):
            if (location.findtext("name") or "").strip().casefold() == alias.casefold():
                points = list(location.findall("./points/data"))
                break
        if not points:
            raise ValueError(f"missing tournament alias {world_name}/{alias}")
    else:
        point = root.find("./dead_start_at_default/data")
        if point is None:
            raise ValueError(f"missing tournament default point: {world_name}")
        points = [point]
    result = []
    for point in points:
        values = [(point.findtext(field) or "").strip() for field in ("x", "y", "z", "dir")]
        if any(not value for value in values):
            raise ValueError(f"incomplete tournament point: {world_name}/{alias or 'default'}")
        result.append(",".join(values))
    return ";".join(result)


def tournament_rounds(path: Path) -> dict[str, list[dict[str, str]]]:
    result = {}
    for tournament in ET.parse(path).getroot():
        tournament_id = (tournament.findtext("id") or "").strip()
        rows = [
            {child.tag: (child.text or "").strip() for child in node}
            for node in tournament.findall("./round_list/data")
        ]
        if not rows or [row.get("round") for row in rows] != [str(i) for i in range(1, len(rows) + 1)]:
            raise ValueError(f"invalid tournament round list: {tournament_id}")
        result[tournament_id] = rows
    return result


def generate(source: Path, client: Path, aionemu: Path, output: Path) -> dict[str, int]:
    selected = {name: source_file(source, name) for name in TABLES}
    for name, (path, _region) in selected.items():
        if not path.is_file():
            raise FileNotFoundError(f"missing retail table: {name}")
    source_rows = {name: read_rows(path) for name, (path, _region) in selected.items()}
    duplicate_rows = {}
    tables = {}
    for name, rows in source_rows.items():
        tables[name], duplicate_rows[name] = dedupe_rows(rows, name)
    by_id = {name: positive_ids(rows, name) for name, rows in tables.items()}
    creations_by_name = unique_names(tables["instance_creation.xml"], "instance_creation.xml")
    restrictions_by_name = unique_names(tables["instance_restrict.xml"], "instance_restrict.xml")
    worlds = world_ids(source / "China" / "ID" / "WorldId.xml")
    tournaments_by_id = positive_ids(tables["instant_dungeon_tournament.xml"], "instant_dungeon_tournament.xml")
    if set(tournaments_by_id) != {"1", "2", "3", "4", "5"}:
        raise ValueError("instant_dungeon_tournament.xml must contain exactly ids 1..5")
    tournaments_by_lobby = {
        row["lobby_insname"].casefold(): row for row in tournaments_by_id.values()
    }
    if len(tournaments_by_lobby) != 5:
        raise ValueError("instant_dungeon_tournament.xml contains duplicate lobby creations")
    tournament_round_rows = tournament_rounds(selected["instant_dungeon_tournament.xml"][0])
    tournament_creations = {}
    tournament_extras = {}
    for tournament in tournaments_by_id.values():
        tournament_id = tournament["id"]
        lobby = creations_by_name.get(tournament.get("lobby_insname", "").casefold())
        stage = creations_by_name.get(tournament.get("match_insname", "").casefold())
        if lobby is None or stage is None:
            raise ValueError(f"unmapped tournament creation: {tournament_id}")
        lobby_world = lobby["worldname"]
        stage_world = stage["worldname"]
        tournament_creations[lobby["name"].casefold()] = (tournament_id, "lobby")
        tournament_creations[stage["name"].casefold()] = (tournament_id, "stage")
        tournament_extras[tournament_id] = {
            "lobby_creation_id": lobby["id"],
            "lobby_world_id": worlds[lobby_world.casefold()],
            "lobby_spawn_page": lobby.get("spawn_page", "0"),
            "lobby_start_01": tournament_world_points(
                source.parent / "Worlds", lobby_world, lobby.get("start_point_alias_01", "")),
            "lobby_start_02": tournament_world_points(
                source.parent / "Worlds", lobby_world, lobby.get("start_point_alias_02", "")),
            "stage_creation_id": stage["id"],
            "stage_world_id": worlds[stage_world.casefold()],
            "stage_spawn_page": stage.get("spawn_page", "0"),
            "stage_start_01": tournament_world_points(
                source.parent / "Worlds", stage_world, stage.get("start_point_alias_01", "")),
            "stage_start_02": tournament_world_points(
                source.parent / "Worlds", stage_world, stage.get("start_point_alias_02", "")),
        }

    luna_costs_by_name = unique_names(tables["luna_cost.xml"], "luna_cost.xml")
    luna_extras = {}
    for dungeon in tables["luna_indun.xml"]:
        creation = creations_by_name.get(dungeon.get("insname", "").casefold())
        price = luna_costs_by_name.get(dungeon.get("luna_price_name", "").casefold())
        if creation is None or price is None:
            raise ValueError(f"unmapped Luna dungeon {dungeon.get('id')}")
        free_turn = int(price.get("free_turn", "0"))
        max_count = int(price.get("price_max_count", "0"))
        if free_turn < 0 or max_count < 0 or price.get("reset_type") not in {"Daily", "Weekly"}:
            raise ValueError(f"invalid Luna price {price.get('id')}")
        for number in range(1, max_count + 1):
            value = price.get(f"price{number:02d}", "")
            if not value.isdigit():
                raise ValueError(f"missing Luna price {price.get('id')}/{number}")
        world_name = creation["worldname"]
        luna_extras[dungeon["id"]] = {
            "creation_id": creation["id"],
            "world_id": worlds[world_name.casefold()],
            "spawn_page": creation.get("spawn_page", "0"),
            "start_point": tournament_world_points(
                source.parent / "Worlds", world_name, creation.get("start_point_alias_01", "")),
            "luna_price_id": price["id"],
        }

    for row in tables["instance_creation.xml"]:
        if row.get("worldname", "").casefold() not in worlds:
            raise ValueError(f"unmapped creation world: {row.get('worldname')}")
        for field in ("requisite_light", "requisite_dark"):
            value = row.get(field)
            if value and value.casefold() not in restrictions_by_name:
                raise ValueError(f"unmapped restriction {value} in creation {row['id']}")
    cooldowns = by_id["instance_cooltime2.xml"]
    for row in tables["instance_cooltime.xml"]:
        if row["name"].casefold() not in worlds:
            raise ValueError(f"unmapped cooltime world: {row['name']}")
        for field in ("coolt_tbl_id", "f2p_coolt_tbl_id"):
            value = row.get(field)
            if value and value not in cooldowns:
                raise ValueError(f"missing {field}={value} for {row['name']}")
    for row in tables["matchmaker.xml"]:
        if row.get("insname", "").casefold() not in creations_by_name:
            raise ValueError(f"unmapped matchmaker creation: {row.get('insname')}")
    tournament_matches = {
        int(row["id"]): tournaments_by_lobby[row["insname"].casefold()]["id"]
        for row in tables["matchmaker.xml"]
        if row.get("insname", "").casefold() in tournaments_by_lobby
    }
    if set(tournament_matches) != TOURNAMENT_MATCH_IDS:
        raise ValueError(f"unexpected tournament matchmakers: {sorted(tournament_matches)}")

    item_source, _item_region = source_file(source, "items.xml")
    wanted_items = referenced_item_names(tables)
    wanted_items.update(
        value.casefold()
        for rows in tournament_round_rows.values()
        for row in rows
        for key, value in row.items()
        if key.endswith("_name") and value
    )
    item_ids = resolve_items(item_source, wanted_items)
    missing_items = sorted(wanted_items - item_ids.keys())
    if missing_items:
        raise ValueError(f"unmapped retail items: {', '.join(missing_items[:20])}")

    cool_by_world = {row["name"].casefold(): row for row in tables["instance_cooltime.xml"]}
    matches_by_creation = defaultdict(list)
    for row in tables["matchmaker.xml"]:
        matches_by_creation[row["insname"].casefold()].append(row)

    match_ids = {int(row["id"]) for row in tables["matchmaker.xml"]}
    npc_path, npc_region = source_file(source, "npcs.xml")
    match_npcs, custom_match_npcs = resolve_match_npcs(npc_path, match_ids)
    string_names = {
        row[field]
        for row in tables["matchmaker.xml"]
        for field in ("desc", "desc_info")
        if row.get(field)
    }
    strings_path = source / "strings.xml"
    string_ids = resolve_string_ids(strings_path, string_names)

    definitions = ET.Element("retail_instances", {"version": "1"})
    schema(definitions)
    for row in sorted(tables["instance_creation.xml"], key=lambda value: int(value["id"])):
        world_id = worlds[row["worldname"].casefold()]
        cool = cool_by_world.get(row["worldname"].casefold())
        extra = {
            "world_id": world_id,
            "cooltime_id": cool.get("id") if cool else None,
            "restriction_light_id": restrictions_by_name.get(row.get("requisite_light", "").casefold(), {}).get("id"),
            "restriction_dark_id": restrictions_by_name.get(row.get("requisite_dark", "").casefold(), {}).get("id"),
            "matchmaker_ids": ",".join(match["id"] for match in matches_by_creation[row["name"].casefold()]),
        }
        tournament_creation = tournament_creations.get(row["name"].casefold())
        if tournament_creation:
            extra["tournament_id"], extra["tournament_role"] = tournament_creation
        ET.SubElement(definitions, "instance", attributes(row, extra))
    write_xml(definitions, output / "definitions.xml")

    limits = ET.Element("retail_instance_limits", {"version": "1"})
    schema(limits)
    for row in sorted(tables["instance_cooltime.xml"], key=lambda value: int(value["id"])):
        ET.SubElement(limits, "instance_rule", attributes(row, {"world_id": worlds[row["name"].casefold()]}))
    for row in sorted(tables["instance_cooltime2.xml"], key=lambda value: int(value["id"])):
        ET.SubElement(limits, "cooldown", attributes(row))
    write_xml(limits, output / "limits.xml")

    matchmaking = ET.Element("retail_matchmaking", {"version": "1"})
    schema(matchmaking)
    for row in sorted(tables["matchmaker.xml"], key=lambda value: int(value["id"])):
        creation = creations_by_name[row["insname"].casefold()]
        match_id = int(row["id"])
        world_id = worlds[creation["worldname"].casefold()]
        tournament_id = tournament_matches.get(match_id)
        category = "TOURNAMENT" if tournament_id else MATCH_CATEGORY_BY_WORLD.get(world_id, "GENERAL")
        adapter = None if tournament_id else MATCH_ADAPTER_BY_WORLD.get(
            world_id, MATCH_ADAPTER_BY_CATEGORY.get(category, "AutoGeneralInstance"))
        ET.SubElement(matchmaking, "match", attributes(row, {
            "creation_id": creation["id"],
            "world_id": world_id,
            "name_id": string_ids.get(row.get("desc", "")),
            "title_id": string_ids.get(row.get("desc_info", "")),
            "npc_ids": ",".join(map(str, sorted(match_npcs[match_id] | custom_match_npcs[match_id]))),
            "custom_npc_ids": ",".join(map(str, sorted(custom_match_npcs[match_id]))),
            "handler": "TOURNAMENT" if tournament_id else MATCH_HANDLER_BY_WORLD.get(world_id, "GENERAL"),
            "category": category,
            "adapter": MATCH_ADAPTER_PACKAGE + adapter if adapter else None,
            "tournament_id": tournament_id,
        }))
    for row in sorted(tables["team_match_maker.xml"], key=lambda value: int(value["id"])):
        creation = creations_by_name.get(row.get("insname", "").casefold())
        ET.SubElement(matchmaking, "team_match", attributes(row, {
            "creation_id": creation.get("id") if creation else None,
            "world_id": worlds.get(creation.get("worldname", "").casefold()) if creation else None,
            "adapter": MATCH_ADAPTER_PACKAGE + "AutoGeneralInstance",
        }))
    write_xml(matchmaking, output / "matchmaking.xml")

    rewards = ET.Element("retail_rewards", {"version": "1"})
    schema(rewards)
    for table_name in REWARD_TABLES:
        table = ET.SubElement(rewards, "table", {"name": table_name.removesuffix(".xml")})
        for row in sorted(tables[table_name], key=lambda value: int(value["id"])):
            extra = add_item_ids(row, item_ids)
            if row.get("worldname"):
                extra["world_id"] = worlds[row["worldname"].casefold()]
            if table_name == "instant_dungeon_tournament.xml":
                extra.update(tournament_extras[row["id"]])
                extra["matchmaker_ids"] = ",".join(
                    str(match_id) for match_id, tournament_id in tournament_matches.items()
                    if tournament_id == row["id"])
                rounds = tournament_round_rows[row["id"]]
                extra["round_count"] = len(rounds)
                for round_row in rounds:
                    number = round_row["round"]
                    for key, value in round_row.items():
                        if key != "round" and value:
                            extra[f"round_{number}_{key}"] = value
                    for key, value in add_item_ids(round_row, item_ids).items():
                        extra[f"round_{number}_{key}"] = value
            elif table_name == "luna_indun.xml":
                extra.update(luna_extras[row["id"]])
            ET.SubElement(table, "row", attributes(row, extra))
    write_xml(rewards, output / "rewards.xml")

    active = local_instance_worlds(aionemu / "src/main/resources/aion/data/static_data/world_maps.xml")
    if len(active) != 139:
        raise ValueError(f"expected 139 active instance worlds, found {len(active)}")
    creation_ids_by_world = defaultdict(list)
    for row in tables["instance_creation.xml"]:
        creation_ids_by_world[worlds[row["worldname"].casefold()]].append(row["id"])
    unexplained = set(active) - set(creation_ids_by_world) - set(SPECIAL_INSTANCE_WORLDS)
    if unexplained:
        raise ValueError(f"active instance worlds without retail definition: {sorted(unexplained)}")
    handlers = instance_handlers(aionemu, set(active))
    quest_references = java_world_references(
        aionemu / "src/main/java/com/aionemu/gameserver/quest", set(active))
    ai_references = java_world_references(
        aionemu / "src/main/java/com/aionemu/gameserver/ai", set(active))
    tournament_sources = defaultdict(list)
    for tournament_id, extra in tournament_extras.items():
        tournament_sources[int(extra["lobby_world_id"])].append(
            f"instant_dungeon_tournament.xml:{tournament_id}:lobby")
        tournament_sources[int(extra["stage_world_id"])].append(
            f"instant_dungeon_tournament.xml:{tournament_id}:stage")
    match_sources = defaultdict(list)
    for row in tables["matchmaker.xml"]:
        creation = creations_by_name[row["insname"].casefold()]
        match_sources[worlds[creation["worldname"].casefold()]].append(f"matchmaker.xml:{row['id']}")
    for row in tables["team_match_maker.xml"]:
        creation = creations_by_name.get(row.get("insname", "").casefold())
        if creation:
            match_sources[worlds[creation["worldname"].casefold()]].append(
                f"team_match_maker.xml:{row['id']}")
    behaviors = Counter()
    coverage = ET.Element("retail_instance_coverage", {"version": "1"})
    schema(coverage)
    for world_id, local_name in sorted(active.items()):
        creation_ids = creation_ids_by_world.get(world_id, [])
        world_name = next((name for name, value in worlds.items() if value == world_id), "")
        cool = cool_by_world.get(world_name)
        match_ids = [
            row["id"] for row in tables["matchmaker.xml"]
            if creations_by_name[row["insname"].casefold()]["worldname"].casefold() == world_name
        ]
        handler = handlers.get(world_id)
        if world_id in NON_PRODUCTION_INSTANCE_WORLDS:
            behavior, behavior_source = "EXCLUDED_NON_PRODUCTION", "world_maps.xml:test_world"
        elif world_id in tournament_sources:
            behavior, behavior_source = "TOURNAMENT", ",".join(tournament_sources[world_id])
        elif world_id in HOUSING_INSTANCE_WORLDS:
            behavior, behavior_source = "HOUSING", "HousingService"
        elif world_id in EVENT_INSTANCE_WORLDS or handler and "event" in handler.parts:
            behavior = "EVENT"
            behavior_source = str(handler or ai_references.get(world_id) or "WorldMapType.LIVE_PARTY_CONCERT_ALL")
        elif handler:
            behavior, behavior_source = "HANDLER", str(handler)
        elif world_id in match_sources:
            behavior, behavior_source = "MATCHMAKER", ",".join(match_sources[world_id])
        elif world_id in quest_references or world_id in ai_references:
            behavior = "RETAIL_AI_QUEST"
            behavior_source = str(quest_references.get(world_id) or ai_references[world_id])
        else:
            behavior = "DATA_ONLY"
            behavior_source = (f"instance_creation.xml:{','.join(creation_ids)}"
                               if creation_ids else "world_maps.xml:special_world")
        behaviors[behavior] += 1
        ET.SubElement(coverage, "world", attributes({}, {
            "id": world_id,
            "local_name": local_name,
            "retail_name": world_name,
            "classification": "standard" if creation_ids else "special",
            "creation_ids": ",".join(creation_ids),
            "cooltime_id": cool.get("id") if cool else None,
            "matchmaker_ids": ",".join(match_ids),
            "behavior": behavior,
            "behavior_source": behavior_source,
            "reason": SPECIAL_INSTANCE_WORLDS.get(world_id),
        }))
    if sum(behaviors.values()) != 139 or set(behaviors) - set(BEHAVIORS):
        raise ValueError(f"invalid instance behavior closure: {dict(behaviors)}")
    write_xml(coverage, output / "coverage.xml")

    manifest = ET.Element("retail_instance_manifest", {"version": "1", "region": "China"})
    schema(manifest)
    for name in TABLES:
        path, region = selected[name]
        ET.SubElement(manifest, "source", {
            "name": name,
            "region": region,
            "path": str(path),
            "sha256": sha256(path),
            "records": str(len(source_rows[name])),
            "effective_records": str(len(tables[name])),
            "duplicate_records": str(duplicate_rows[name]),
        })
    for name, path, region in (("npcs.xml", npc_path, npc_region), ("strings.xml", strings_path, "base")):
        ET.SubElement(manifest, "source", {
            "name": name,
            "region": region,
            "path": str(path),
            "sha256": sha256(path),
        })
    for relative in CLIENT_FILES:
        path = client / relative
        if not path.is_file():
            raise FileNotFoundError(f"missing client file: {path}")
        ET.SubElement(manifest, "client_file", {
            "path": str(path),
            "sha256": sha256(path),
            "size": str(path.stat().st_size),
        })
    for relative in ("单机启动.bat", "单机启动 - 副本.bat"):
        ET.SubElement(manifest, "launch", {"file": relative, "arguments": launch_args(client / relative)})
    ET.SubElement(manifest, "validation", {
        "active_instance_worlds": str(len(active)),
        "standard_instance_worlds": str(len(set(active) & set(creation_ids_by_world))),
        "special_instance_worlds": str(len(set(active) & set(SPECIAL_INSTANCE_WORLDS))),
        "creation_world_mappings": str(len(tables["instance_creation.xml"])),
        "matchmaker_mappings": str(len(tables["matchmaker.xml"])),
        "matchmaker_npc_mappings": str(sum(len(ids) for ids in match_npcs.values())
                                        + sum(len(ids) for ids in custom_match_npcs.values())),
        "matchmaker_string_mappings": str(len(string_ids)),
        "tournament_mappings": str(len(tournament_extras)),
        "tournament_matchmakers": str(len(tournament_matches)),
        "luna_dungeon_mappings": str(len(luna_extras)),
        "resolved_item_names": str(len(item_ids)),
        "behavior_total_worlds": str(sum(behaviors.values())),
        **{f"behavior_{behavior.lower()}_worlds": str(behaviors[behavior]) for behavior in BEHAVIORS},
        "unresolved_references": "0",
    })
    write_xml(manifest, output / "manifest.xml")
    return {"active": len(active), "standard": len(set(active) & set(creation_ids_by_world)), "special": len(SPECIAL_INSTANCE_WORLDS)}


def compare(expected: Path, actual: Path) -> None:
    names = {path.name for path in expected.glob("*.xml")} | {path.name for path in actual.glob("*.xml")}
    different = [name for name in sorted(names) if not (expected / name).is_file()
                 or not (actual / name).is_file() or (expected / name).read_bytes() != (actual / name).read_bytes()]
    if different:
        raise SystemExit(f"generated retail instance data is stale: {', '.join(different)}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=Path("/Users/mc/IdeaProjects/58Server/Map/XML"))
    parser.add_argument("--client", type=Path, default=Path("/Users/mc/IdeaProjects/5.8客户端"))
    parser.add_argument("--aionemu", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    output = args.output or args.aionemu / "src/main/resources/aion/definitions/compact/instance"
    if args.check:
        with tempfile.TemporaryDirectory() as directory:
            generated = Path(directory)
            counts = generate(args.source, args.client, args.aionemu, generated)
            compare(output, generated)
    else:
        counts = generate(args.source, args.client, args.aionemu, output)
    print(f"retail instance data: {counts['active']} active worlds, {counts['standard']} standard, {counts['special']} special")


if __name__ == "__main__":
    main()

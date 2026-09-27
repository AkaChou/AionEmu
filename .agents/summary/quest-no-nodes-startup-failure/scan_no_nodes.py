#!/usr/bin/env python3
"""NO_NODES 复现扫描：重放 QuestDefinitionCompiler 的 NO_NODES 判定。

NO_NODES reproduction scan: replays the QuestDefinitionCompiler NO_NODES rule —
an EXECUTABLE catalog entry whose quest XML yields zero <node> elements fails
startup with "NO_NODES: executable definition has no nodes".

用法 / Usage:
    python3 scan_no_nodes.py [quest_definition_dir]

默认扫描源码树目录 / Defaults to the source-tree directory:
    src/main/resources/aion/data/static_data/quest_definition

退出码 / Exit codes: 0 = 无违例 (green), 1 = 存在 NO_NODES 违例 (red)。
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

DEFAULT_DIR = Path("src/main/resources/aion/data/static_data/quest_definition")


def scan(directory: Path) -> list[tuple[int, str, str]]:
    catalog_path = directory / "quest_definition_catalog.xml"
    root = ET.parse(catalog_path).getroot()
    offenders = []
    for definition in root.findall("definition"):
        mode = definition.get("mode", "")
        if mode != "EXECUTABLE":
            continue
        quest_id = int(definition.get("id"))
        resource = definition.get("resource")
        # resource 可能带 aion/data/static_data/quest_definition/ 前缀，也可能相对目录。
        # The resource may carry the aion/data/... prefix or be relative to the directory.
        quest_file = directory / resource
        if not quest_file.is_file():
            stripped = resource
            for prefix in ("aion/data/static_data/quest_definition/", "./"):
                if stripped.startswith(prefix):
                    stripped = stripped[len(prefix):]
                    break
            quest_file = directory / stripped
        try:
            quest_root = ET.parse(quest_file).getroot()
        except ET.ParseError as e:
            offenders.append((quest_id, resource, f"XML parse error: {e}"))
            continue
        # 与 Java 侧 parseNodes 一致：<nodes> 缺失或其下没有 <node> 都算 0 个节点。
        # Mirrors the Java parseNodes: a missing <nodes> element or one without
        # any <node> child both yield zero nodes.
        nodes = quest_root.findall("./nodes/node")
        if not nodes:
            offenders.append((quest_id, resource, "executable definition has 0 nodes"))
    return offenders


def main() -> int:
    directory = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_DIR
    offenders = scan(directory)
    if not offenders:
        print(f"GREEN: {directory} — all EXECUTABLE catalog entries define at least one node")
        return 0
    for quest_id, resource, reason in offenders:
        print(f"RED: quest {quest_id} ({resource}): {reason}")
    print(f"{len(offenders)} offender(s)")
    return 1


if __name__ == "__main__":
    sys.exit(main())

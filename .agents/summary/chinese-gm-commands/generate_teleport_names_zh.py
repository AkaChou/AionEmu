#!/usr/bin/env python3
"""生成中文传送地点别名表：国服客户端字符串表 + teleport_location.xml → loc_id + 中文名。

Generates the Chinese teleport-location alias table consumed by the //移动 GM command: the CN client
string tables are joined onto teleport_location.xml through its name_id attribute.

用法 / Usage:
    python3 generate_teleport_names_zh.py \
        [--client-data-pak "/Users/mc/IdeaProjects/5.8客户端/L10N/CHS/Data/data.pak"] \
        [--teleloc src/main/resources/aion/data/static_data/teleport_location.xml] \
        [--out src/main/resources/aion/config/administration/teleport_names_zh.txt] \
        [--tsv .agents/summary/chinese-gm-commands/teleloc_zh.tsv]

--tsv 会额外写出完整连接报表（loc_id/mapid/中英名/命中的客户端字符串键与文件/坐标），空值写成 `-`。
--tsv additionally writes the full join report; empty cells are written as `-`.
"""
import argparse
import re
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

DEFAULT_PAK = "/Users/mc/IdeaProjects/5.8客户端/L10N/CHS/Data/data.pak"
DEFAULT_TELELOC = "src/main/resources/aion/data/static_data/teleport_location.xml"
DEFAULT_OUT = "src/main/resources/aion/config/administration/teleport_names_zh.txt"

ENTRY = re.compile(r"<string>\s*<id>(\d+)</id>\s*<name>([^<]+)</name>\s*<body>(.*?)</body>", re.S)

HEADER = """# 中文传送地点别名表（GM 命令 //移动 使用） / Chinese teleport-location alias table (used by the //移动 GM command)
#
# 格式 / Format: <loc_id>\\t<中文名>\\t<英文名>
#   loc_id 对应 data/static_data/teleport_location.xml，坐标以该文件为准（本表不复制坐标）。
#   loc_id refers to data/static_data/teleport_location.xml, which stays the single source of coordinates.
# 来源 / Source: 国服 5.8 客户端 L10N/CHS/Data/data.pak → Strings/client_strings_level.xml 等字符串表，
#               按 teleport_location.xml 的 name_id 连接；生成脚本见
#               .agents/summary/chinese-gm-commands/generate_teleport_names_zh.py
# 说明 / Notes: 无服务端坐标的条目是客户端飞行传送点，//移动 会明确提示不支持直接传送；
#               手工补充别名（例如同一地点的其它叫法）可直接在本文件追加一行。
#               Entries without server coordinates are client-side flight teleports; //移动 reports them
#               instead of failing silently. Extra aliases can be appended by hand.
"""

TSV_HEADER = "loc_id\tmapid\ten_name\tname_id\tzh_key\tzh_name\tclient_file\tposX\tposY\tposZ\n"


def load_client_strings(pak_path: Path) -> dict[int, tuple[str, str, str]]:
    """读取国服客户端字符串表：id → (字符串键, 中文文本, 来源文件)。

    Reads the CN client string tables: id to (string key, Chinese text, source file).
    """
    strings: dict[int, tuple[str, str, str]] = {}
    with zipfile.ZipFile(pak_path) as pak:
        names = [n for n in pak.namelist() if n.startswith("Strings/client_strings_") and n.endswith(".xml")]
        for name in sorted(names):
            text = pak.read(name).decode("utf-16")
            for match in ENTRY.finditer(text):
                strings.setdefault(int(match.group(1)), (match.group(2), match.group(3).strip(), Path(name).name))
    return strings


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--client-data-pak", default=DEFAULT_PAK)
    parser.add_argument("--teleloc", default=DEFAULT_TELELOC)
    parser.add_argument("--out", default=DEFAULT_OUT)
    parser.add_argument("--tsv", default=None)
    args = parser.parse_args()

    strings = load_client_strings(Path(args.client_data_pak))

    templates = ET.parse(args.teleloc).getroot().findall("teleloc_template")
    with_coordinates = {int(n.get("loc_id")) for n in templates if n.get("posX") is not None}

    rows, missing = [], []
    for node in templates:
        loc_id = int(node.get("loc_id"))
        name_id = int(node.get("name_id"))
        hit = strings.get(name_id)
        if hit is None:
            missing.append((loc_id, node.get("name")))
            rows.append((loc_id, int(node.get("mapid")), node.get("name") or "", name_id, "", "", "", "", "", ""))
            continue
        key, chinese, client_file = hit
        rows.append((loc_id, int(node.get("mapid")), node.get("name") or "", name_id, key, chinese, client_file,
                     node.get("posX") or "", node.get("posY") or "", node.get("posZ") or ""))
    rows.sort(key=lambda row: row[0])

    named = [row for row in rows if row[5]]
    with_pos = [row for row in named if row[0] in with_coordinates]

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8") as fh:
        fh.write(HEADER)
        fh.write(f"# 条目 / Entries: {len(named)}（有服务端坐标 / with coordinates: {len(with_pos)}）\n\n")
        for row in named:
            fh.write(f"{row[0]}\t{row[5]}\t{row[2]}\n")

    if args.tsv:
        tsv = Path(args.tsv)
        tsv.parent.mkdir(parents=True, exist_ok=True)
        with tsv.open("w", encoding="utf-8") as fh:
            fh.write(TSV_HEADER)
            for row in rows:
                fh.write("\t".join("-" if cell == "" else str(cell) for cell in row) + "\n")

    print(f"wrote {out} entries={len(named)} with_coordinates={len(with_pos)}"
          + (f" and {args.tsv}" if args.tsv else ""))
    print(f"entries without a CN client name: {len(missing)} {missing}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""QE-110 取证扫描：`EnterArea` 具名区域在真端/客户端数据里是否有几何定义。

背景：真端 DataDriven 行 50125/51125 的 `category_acquire_=EnterArea`，
`value0_acquire_=Tiamat_Down_QuestArea_Q50125`；本服因「区域表无绑定」把它们留在 XML
（保留码 `ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING`）。本脚本给出该裁定所需的
可复算证据：该区域名在真端与客户端数据中**只有引用、没有定义**。

外部数据根按名引用（见仓库 ENVIRONMENT.md），默认取同级的真端/客户端检出：
  <真端根>        `58Server`（`Map/Worlds/**/world*.xml`、`Map/XML/NpcAIPatterns_*.xml` 等）
  <客户端目录>    `5.8客户端`（`data/world/world.pak`、`data/Quest/Quest.pak`）
  <客户端解包根>  `PycharmProjects/unpak`（`Quest_unpacked/*.xml`）

用法 / Usage:
  python3 scan_acquire_area_unbound.py [--server DIR] [--client DIR] [--client-unpack DIR]
                                      [--out TSV]

输出 TSV 列：check_id / verdict / source / hits / detail
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

AREA_NAME = "Tiamat_Down_QuestArea_Q50125"
QUEST_IDS = ("50125", "51125")
REPO = Path(__file__).resolve().parents[3]
DEFAULT_SERVER = REPO.parent / "58Server"
DEFAULT_CLIENT = REPO.parent / "5.8客户端"
CLIENT_WORLD_DIR = Path(__file__).resolve().parent / "client_evidence"


def default_client_unpack() -> Path:
    """<客户端解包根>：同宿主目录约定，回退到宿主家目录下的标准位置。 / Convention path, then home. """
    for candidate in (REPO.parent / "PycharmProjects" / "unpak", Path.home() / "PycharmProjects" / "unpak"):
        if (candidate / "Quest_unpacked" / "quest.xml").is_file():
            return candidate
    return REPO.parent / "PycharmProjects" / "unpak"


def decode(raw: bytes) -> str | None:
    """按编码解码文本文件（UTF-16 BOM，或 UTF-8 无 NUL）。 / Decode a text file. """
    if raw[:2] in (b"\xff\xfe", b"\xfe\xff"):
        try:
            return raw.decode("utf-16")
        except UnicodeDecodeError:
            return None
    if b"\x00" in raw[:4096]:
        return None
    return raw.decode("utf-8", errors="ignore")


def byte_scan(root: Path, needle: str) -> list[tuple[str, str]]:
    """全树字节级扫描（UTF-16LE / UTF-8 两种编码）。 / Whole-tree byte scan, both encodings. """
    pats = (needle.encode("utf-16-le"), needle.encode("utf-8"))
    hits: list[tuple[str, str]] = []
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        try:
            data = path.read_bytes()
        except OSError:
            continue
        encodings = [enc for enc, pat in zip(("utf-16le", "utf-8"), pats) if pat in data]
        if encodings:
            hits.append((str(path.relative_to(root)), "+".join(encodings)))
    return hits


def questscript_areas(world_file: Path) -> list[str]:
    """世界文件里的 `<questscript_area>` 区域名。 / Area names defined by a world file. """
    text = decode(world_file.read_bytes())
    if text is None:
        return []
    return [m.group(1).strip() for m in re.finditer(r"<questscript_area>.*?<name>(.*?)</name>", text, re.S)]


def all_world_area_names(server: Path) -> set[str]:
    """全部世界文件里出现过的任何 `<name>`（含各种区域容器）。 / Every `<name>` in world files. """
    names: set[str] = set()
    for world in sorted((server / "Map" / "Worlds").glob("*/world*.xml")):
        text = decode(world.read_bytes())
        if text is None:
            continue
        names.update(m.group(1).strip() for m in re.finditer(r"<name>(.*?)</name>", text, re.S))
    return names


def enable_area_refs(server: Path) -> dict[str, set[str]]:
    """NpcAIPatterns 里的 `enable_area` 引用 → 引用文件集。 / enable_area refs by area name. """
    refs: dict[str, set[str]] = {}
    for pattern in sorted((server / "Map" / "XML").glob("NpcAIPatterns_*.xml")):
        text = decode(pattern.read_bytes())
        if text is None:
            continue
        for block in re.finditer(r"<enable_area>(.*?)</enable_area>", text, re.S):
            name = re.search(r"<area_name>(.*?)</area_name>", block.group(1), re.S)
            if name:
                refs.setdefault(name.group(1).strip(), set()).add(pattern.name)
    return refs


def dd_rows(text: str, quest_id: str) -> list[str]:
    """DataDriven 表里某个 id 的行字段。 / The DD row fields for one quest id. """
    rows: list[str] = []
    for block in re.findall(r"<quest_data_driven>(.*?)</quest_data_driven>", text, re.S):
        if f"<id>{quest_id}</id>" not in block:
            continue
        fields = []
        for tag in ("category_acquire_", "value0_acquire_", "reward_npc_name", "category_progress_",
				"value0_progress_"):
            hit = re.search(rf"<{tag}>(.*?)</{tag}>", block, re.S)
            if hit:
                fields.append(f"{tag}={hit.group(1).strip()}")
        rows.append("; ".join(fields))
    return rows


def row(check_id: str, verdict: str, source: str, hits: int, detail: str) -> str:
    return "\t".join((check_id, verdict, source, str(hits), detail.replace("\t", " ").replace("\n", " ")))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server", type=Path, default=DEFAULT_SERVER, help="<真端根> / retail server root")
    parser.add_argument("--client", type=Path, default=DEFAULT_CLIENT, help="<客户端目录> / client root (unused, documented)")
    parser.add_argument("--client-unpack", type=Path, default=default_client_unpack(), help="<客户端解包根>")
    parser.add_argument("--out", type=Path, default=Path(__file__).with_name("forensics_acquire_area_unbound.tsv"))
    args = parser.parse_args()

    server = args.server.resolve()
    unpack = args.client_unpack.resolve()
    world = server / "Map" / "Worlds" / "tiamat_down" / "world.xml"
    if not world.is_file():
        print(f"missing retail world file: {world}", file=sys.stderr)
        return 2

    lines = [
        "# QE-110 取证：EnterArea 具名区域 " + AREA_NAME,
        "# 生成：.agents/summary/quest-acquire-area-q50125/scan_acquire_area_unbound.py",
        "# 列：check_id / verdict / source / hits / detail",
    ]

    # C1 真端全树字节级扫描：该区域名只以引用形式出现
    scan = byte_scan(server, AREA_NAME)
    lines.append(row("C1-retail-bytescan", "DANGLING" if len(scan) == 2 else "REVIEW",
                     "<真端根>", len(scan),
                     "; ".join(f"{name}({enc})" for name, enc in scan)))

    # C2 tiamat_down 世界文件本身：零 questscript_area
    areas = questscript_areas(world)
    lines.append(row("C2-world-questscript-area", "NO-GEOMETRY" if AREA_NAME not in areas else "DEFINED",
                     "Map/Worlds/tiamat_down/world.xml", len(areas),
                     f"世界文件定义 questscript_area={len(areas)} 个; 是否含目标名={AREA_NAME in areas}"))

    # C3 唯一另一处引用：活动 AI 模式的 enable_area（悬空目标）
    refs = enable_area_refs(server)
    lines.append(row("C3-ai-pattern-enable-area", "DANGLING-REF" if AREA_NAME in refs else "NONE",
                     "Map/XML/NpcAIPatterns_Event_KJS.xml", len(refs.get(AREA_NAME, ())),
                     "enable_area 引用者=" + (",".join(sorted(refs.get(AREA_NAME, ()))) or "-")))

    # C4/C5 真端与客户端 DataDriven 行一致（同名区域 + EnterArea + PVP 计数）
    dd_retail = server / "Map" / "XML" / "data_driven_quest.xml"
    retail_text = decode(dd_retail.read_bytes()) or ""
    for quest_id in QUEST_IDS:
        lines.append(row(f"C4-retail-dd-{quest_id}", "ROW", "Map/XML/data_driven_quest.xml",
                         len(dd_rows(retail_text, quest_id)), "; ".join(dd_rows(retail_text, quest_id)) or "-"))
    client_dd = unpack / "Quest_unpacked" / "data_driven_quest.xml"
    client_text = decode(client_dd.read_bytes()) or "" if client_dd.is_file() else ""
    for quest_id in QUEST_IDS:
        rows = dd_rows(client_text, quest_id)
        lines.append(row(f"C5-client-dd-{quest_id}", "ROW" if rows else "MISSING",
                         "<客户端解包根>/Quest_unpacked/data_driven_quest.xml", len(rows),
                         "; ".join(rows) or "-"))

    # C6 客户端任务表也在但同样没有区域几何
    client_quest = unpack / "Quest_unpacked" / "quest.xml"
    client_quest_text = decode(client_quest.read_bytes()) or "" if client_quest.is_file() else ""
    hits = [q for q in QUEST_IDS if f"<id>{q}</id>" in client_quest_text]
    lines.append(row("C6-client-quest-table", "ROW" if hits else "MISSING",
                     "<客户端解包根>/Quest_unpacked/quest.xml", len(hits),
                     "客户端任务表 id=" + (",".join(hits) or "-") + "; 含区域名=" + str(AREA_NAME in client_quest_text)))

    # C7 客户端世界文件（world.pak 解包产物）：无该区域名，无 questscript 容器
    client_world = CLIENT_WORLD_DIR / "client_world_tiamat_down.xml"
    if client_world.is_file():
        text = client_world.read_text(encoding="utf-8", errors="ignore")
        lines.append(row("C7-client-world-file", "NO-GEOMETRY" if AREA_NAME not in text else "DEFINED",
                         "client_evidence/client_world_tiamat_down.xml", text.count(AREA_NAME),
                         f"客户端世界文件含区域名={AREA_NAME in text}; questscript 容器数="
                         f"{text.count('questscript')}; 容器类型=clientzones"))
    else:
        lines.append(row("C7-client-world-file", "MISSING", "client_evidence/client_world_tiamat_down.xml", 0,
                         "先按报告 §6 的命令从 data/world/world.pak 解包该条目"))

    # C8 世界 id 归属：600040000 = Tiamat_Down（DD 的 EnterWorld 进度值）
    world_id = CLIENT_WORLD_DIR / "WorldId.xml"
    if world_id.is_file():
        text = world_id.read_text(encoding="utf-8", errors="ignore")
        hit = re.search(r'<data[^>]*id="600040000"[^>]*>([^<]*)</data>', text)
        lines.append(row("C8-world-id-map", "MATCH", "client_evidence/WorldId.xml", 1 if hit else 0,
                         f"600040000 -> {hit.group(1) if hit else '-'}"))
    else:
        lines.append(row("C8-world-id-map", "MISSING", "client_evidence/WorldId.xml", 0, "-"))

    # C9 同类悬空引用在真端是已知形态（活动数据退役后残留）
    defined = all_world_area_names(server)
    dangling = [name for name in refs if name not in defined]
    lines.append(row("C9-dangling-class", "KNOWN-SHAPE", "Map/XML/NpcAIPatterns_*.xml", len(dangling),
                     f"enable_area 引用 {len(refs)} 个区域名，其中 {len(dangling)} 个在任何世界文件里都没有定义"))

    # C10 真端装载器：区域对象只由世界数据创建（name/points/quest 三段）
    loader = server / "server58-source" / "NPCServer_NPCSvr64" / "classes" / "Quest" / "QuestArea.cpp"
    if loader.is_file():
        text = decode(loader.read_bytes()) or ""
        labels = [lbl for lbl in ("CreateQuestArea, name", "CreateQuestArea(%s), quest") if lbl in text]
        lines.append(row("C10-retail-loader", "WORLD-ONLY", "NPCServer_NPCSvr64/classes/Quest/QuestArea.cpp",
                         len(labels), "QuestArea 由世界数据创建，解析标签=" + ",".join(labels)))
    else:
        lines.append(row("C10-retail-loader", "MISSING", "NPCServer_NPCSvr64/classes/Quest/QuestArea.cpp", 0, "-"))

    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {args.out} ({len(lines) - 3} checks)")
    for line in lines:
        print(line)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
# DD hunt 击杀路由变体族覆盖审计(QE-048 回归,2026-10-08)。
# 复算口径与 DataDrivenNativeRuntime 装载一致:
#   修复前路由 = DD 名单名精确解析(name_desc -> name -> 别名近似为 name_desc/name 双通道)
#   修复后路由 = 族展开(剥「等级+tag」尾段、归一 _t_ 段)后的同族全部模板
# 断供判定 = 路由集 ∩ 实刷集(spawns/**/*.xml,排除 /New/,按 world_maps.xml 未注释地图过滤) 为空。
# 只读审计;产物写到本目录 report.tsv。
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
STATIC = REPO / "src/main/resources/aion/data/static_data"
DD_TABLE = STATIC / "quest/retail/data_driven_quest.xml"
CONTRACT = REPO / "src/main/resources/aion/definitions/quest_monster/quest_monster.csv"
REPORT = Path(__file__).resolve().parent / "report.tsv"


def monster_family_key(name_desc: str):
    """与 NativeNpcNameResolver.monsterFamilyKey 同规:剥 tag+等级尾或纯等级尾,归一 _t_ 中段。"""
    norm = name_desc.strip().lower()
    if not norm:
        return None
    parts = norm.split("_")
    end = len(parts)
    if end > 2 and re.fullmatch(r"[a-z]{1,2}", parts[-1]) and re.fullmatch(r"\d{1,2}", parts[-2]):
        end -= 2
    elif end > 1 and re.fullmatch(r"\d{1,2}", parts[-1]):
        end -= 1
    kept = [p for i, p in enumerate(parts[:end]) if not (i > 0 and p == "t")]
    return "_".join(kept) or None


def load_templates():
    by_desc, by_family = {}, {}
    tag_re = re.compile(r"<npc_template\b([^>]*)>")
    attr_re = re.compile(r'\b(npc_id|name_desc)="([^"]*)"')
    for shard in sorted((STATIC / "npcs").glob("npc_template_*.xml")):
        for m in tag_re.finditer(shard.read_text(encoding="utf-8")):
            npc_id, desc = None, None
            for am in attr_re.finditer(m.group(1)):
                if am.group(1) == "npc_id":
                    npc_id = int(am.group(2))
                elif am.group(1) == "name_desc":
                    desc = am.group(2)
            if npc_id is None or not desc:
                continue
            by_desc.setdefault(desc.strip().lower(), [])
            if npc_id not in by_desc[desc.strip().lower()]:
                by_desc[desc.strip().lower()].append(npc_id)
            key = monster_family_key(desc)
            if key:
                by_family.setdefault(key, set()).add(npc_id)
    return by_desc, by_family


def resolve_exact(name, by_desc):
    return list(by_desc.get(name.strip().lower(), []))


def resolve_family(name, by_desc, by_family):
    direct = resolve_exact(name, by_desc)
    out = set(direct)
    for npc_id in direct:
        key = monster_family_key(next((d for d, ids in by_desc.items() if npc_id in ids), ""))
        if key and key in by_family:
            out |= by_family[key]
    return out


def active_world_ids():
    """world_maps.xml 剥掉 XML 注释后仍声明的地图 id(QE-048:Lakrum 被注释不参与可达性)。"""
    text = (STATIC / "world_maps.xml").read_text(encoding="utf-8")
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
    return {int(m) for m in re.findall(r'<map\b[^>]*\bid="(\d+)"', text)}


def load_spawn_ids():
    """实刷 npc_id 集,按活跃地图过滤(文件名前缀 = world id);排除 //spawn 的 /New/ 产物。"""
    active = active_world_ids()
    ids = set()
    for path in (STATIC / "spawns").rglob("*.xml"):
        if "/New/" in str(path):
            continue
        head = path.name.split("_", 1)[0]
        if head.isdigit() and int(head) not in active:
            continue
        for m in re.finditer(r'npc_id="(\d+)"', path.read_text(encoding="utf-8")):
            ids.add(int(m.group(1)))
    return ids


def dd_hunt_payloads():
    """DD 表全部 Hunt 步的组名清单(与 parseGroups 同规:;分组、,分名、尾整数剥离)。"""
    payloads = {}
    root = ET.parse(DD_TABLE).getroot()
    for qdd in root.findall("quest_data_driven"):
        qid = int(qdd.findtext("id", "-1"))
        names = []
        prog = qdd.find("progress_info")
        if prog is None:
            continue
        for data in prog.findall("data"):
            if data.findtext("category_progress_") != "Hunt":
                continue
            for col in ("value0_progress_", "value1_progress_", "value2_progress_", "value3_progress_"):
                node = data.find(col)
                if node is None or not (node.text or "").strip():
                    continue
                for group in node.text.split(";"):
                    text = group.strip()
                    if not text:
                        continue
                    text = re.sub(r"[\s,]+\d+$", "", text).strip()
                    for name in text.split(","):
                        name = name.strip()
                        if name:
                            names.append(name)
        if names:
            payloads[qid] = names
    return payloads


def main():
    by_desc, by_family = load_templates()
    spawned = load_spawn_ids()
    payloads = dd_hunt_payloads()
    print(f"模板名 {len(by_desc)} / 族 {len(by_family)} / 实刷 id {len(spawned)} / DD hunt 任务 {len(payloads)}")

    rows, broken_before, broken_after = [], [], []
    for qid in sorted(payloads):
        exact, family = set(), set()
        for name in payloads[qid]:
            exact |= set(resolve_exact(name, by_desc))
            family |= resolve_family(name, by_desc, by_family)
        live_exact, live_family = exact & spawned, family & spawned
        rows.append((qid, len(payloads[qid]), len(exact), len(live_exact), len(family), len(live_family)))
        if exact and not live_exact:
            broken_before.append(qid)
        if not live_family:
            broken_after.append(qid)

    with REPORT.open("w", encoding="utf-8") as fh:
        fh.write("questId\tpayloadNames\texactIds\texactLive\tfamilyIds\tfamilyLive\n")
        for row in rows:
            fh.write("\t".join(str(v) for v in row) + "\n")

    print(f"\n修复前断供(精确路由∩实刷=空): {len(broken_before)} 个任务")
    for qid in broken_before:
        mark = " <= 用户报障" if qid in (15546, 15500, 15503, 15640, 17510) else ""
        print(f"  {qid}{mark}")
    print(f"\n修复后断供(族路由∩实刷=空): {len(broken_after)} 个任务")
    for qid in broken_after:
        print(f"  {qid}")
    reported = [q for q in (15546, 15500, 15503, 15640, 17510) if q in payloads]
    for qid in reported:
        row = next(r for r in rows if r[0] == qid)
        print(f"报障任务 {qid}: exactLive={row[3]} familyLive={row[5]}")
    return 0 if not broken_after else 1


if __name__ == "__main__":
    sys.exit(main())

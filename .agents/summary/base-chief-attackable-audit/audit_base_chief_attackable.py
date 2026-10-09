#!/usr/bin/env python3
"""审计基地占领关键 NPC 的可攻击性 (Base capture-critical NPC attackability audit).

合同 (contract, derived from Base.java semantics):
- handler="CHIEF" 的 spawn = 基地指挥官, Base.spawnBoss() 只给它挂 BaseBossDeathListener,
  死亡驱动占领; npc_type 必须为 ATTACKABLE, 否则该基地永久无法占领.
- handler="SLAYER" 的 spawn = 袭击方 NPC, spawnAttackers() 刷出攻打基地; 必须为 ATTACKABLE,
  否则袭击战无法开打.

输出: 每个违反合同的 npc_id 一行 TSV: map_file | base_id | handler | npc_id | name_desc | npc_type
用法: python3 -I audit_base_chief_attackable.py [--fix]  (--fix 见 fix_hints.txt 生成说明)
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SPAWNS = ROOT / "src/main/resources/aion/data/static_data/spawns/Bases"
NPCS = ROOT / "src/main/resources/aion/data/static_data/npcs"

TPL_RE = re.compile(r'<npc_template\b[^>]*>')
ID_RE = re.compile(r'npc_id="(\d+)"')


def load_templates():
    """npc_id -> (name_desc, npc_type), 全量扫描 8 个 npc_template 文件."""
    tpl = {}
    for f in sorted(NPCS.glob("npc_template_*.xml")):
        for m in TPL_RE.finditer(f.read_text(encoding="utf-8")):
            s = m.group(0)
            nid = ID_RE.search(s)
            if not nid:
                continue
            name = re.search(r'name_desc="([^"]*)"', s)
            ntype = re.search(r'\bnpc_type="([^"]*)"', s)
            tpl[nid.group(1)] = (name.group(1) if name else "?", ntype.group(1) if ntype else "?")
    return tpl


def audit():
    tpl = load_templates()
    violations = []
    checked = 0
    for f in sorted(SPAWNS.glob("*.xml")):
        text = f.read_text(encoding="utf-8")
        # 按 base_spawn 块切, 保留 base id 上下文
        for block in re.finditer(r'<base_spawn id="(\d+)">(.*?)</base_spawn>', text, re.S):
            base_id, body = block.group(1), block.group(2)
            for sm in re.finditer(r'<spawn npc_id="(\d+)"\s+handler="(CHIEF|SLAYER)"', body):
                checked += 1
                nid, handler = sm.group(1), sm.group(2)
                name_desc, ntype = tpl.get(nid, ("TEMPLATE_MISSING", "?"))
                if ntype != "ATTACKABLE":
                    violations.append((f.name, base_id, handler, nid, name_desc, ntype))
    return checked, violations


def main():
    checked, violations = audit()
    print(f"checked={checked} (CHIEF+SLAYER spawns across spawns/Bases/*.xml)")
    print(f"violations={len(violations)}")
    out = Path(__file__).parent / "violations.tsv"
    with out.open("w", encoding="utf-8") as fh:
        fh.write("map_file\tbase_id\thandler\tnpc_id\tname_desc\tnpc_type\n")
        for row in violations:
            fh.write("\t".join(row) + "\n")
    print(f"written: {out}")
    # 按 (handler, npc_type) 汇总
    from collections import Counter
    summary = Counter((v[2], v[5]) for v in violations)
    for (handler, ntype), n in sorted(summary.items()):
        print(f"  handler={handler} npc_type={ntype}: {n}")
    sys.exit(1 if violations else 0)


if __name__ == "__main__":
    main()

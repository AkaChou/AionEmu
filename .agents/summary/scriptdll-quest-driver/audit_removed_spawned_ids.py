#!/usr/bin/env python3
"""审计 Phase 4-2 对齐批次删除的 npc-id：区分「等价类别名」与「真正的多算怪」。

口径：
  * 仍被 spawns/**/*.xml 刷出（≥1 spot）= 运行期可达实体；
  * 与同 dimension 保留 id 共享 name_id（同一客户端显示名）= 等价类别名（删除是回归）；
  * 其余 = 真端表与客户端 CSV 都未列出的多算怪（删除是修正）。

输出：removed-spawned-audit.tsv
"""
from __future__ import annotations

import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
PROD = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
SPAWNS = REPO / "src/main/resources/aion/data/static_data/spawns"
NPCS = REPO / "src/main/resources/aion/data/static_data/npcs"

TEMPLATE_RE = re.compile(r'<npc_template\b[^>]*?npc_id="(\d+)"[^>]*?>')
ATTR_RE = re.compile(r'(\w[\w-]*)="([^"]*)"')


def npc_index() -> dict[str, tuple[str, str]]:
    index: dict[str, tuple[str, str]] = {}
    for path in sorted(NPCS.glob("npc_template_*.xml")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for match in TEMPLATE_RE.finditer(text):
            attrs = dict(ATTR_RE.findall(match.group(0)))
            index[match.group(1)] = (attrs.get("name_desc", ""), attrs.get("name_id", ""))
    return index


def dims_npc_ids(text: str) -> dict[str, set[str]]:
    out: dict[str, set[str]] = {}
    for match in re.finditer(r'<dimension\b[^>]*/>', text):
        tag = match.group(0)
        field = re.search(r'field="([^"]+)"', tag)
        ids = re.search(r'npc-ids="([^"]*)"', tag)
        if field and ids:
            out[field.group(1)] = set(ids.group(1).split())
    return out


def head_text(path: str) -> str | None:
    proc = subprocess.run(["git", "show", f"HEAD:{path}"], cwd=REPO, capture_output=True, text=True)
    return proc.stdout if proc.returncode == 0 else None


def spawned_with_spots() -> dict[str, str]:
    result: dict[str, str] = {}
    for path in SPAWNS.rglob("*.xml"):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for spawn in root.iter("spawn"):
            npc = spawn.get("npc_id")
            if npc and len(spawn.findall("spot")) > 0:
                result.setdefault(npc, str(path.relative_to(REPO)))
    return result


def main() -> int:
    spawns, npcs = spawned_with_spots(), npc_index()
    rows = []
    # 退役 XML 只在 git 历史里（退役 = 原样删除），因此不存在"新旧差异行"。
    # Retired XMLs live in git history unchanged, so they cannot contribute diff rows.
    for path in sorted(PROD.glob("*.xml")):
        rel = str(path.relative_to(REPO))
        old = head_text(rel)
        if old is None:
            continue
        new = path.read_text(encoding="utf-8")
        old_dims, new_dims = dims_npc_ids(old), dims_npc_ids(new)
        for field, old_ids in old_dims.items():
            kept = new_dims.get(field, set())
            kept_name_ids = {npcs.get(i, ("", ""))[1] for i in kept}
            for npc in sorted(old_ids - kept):
                desc, name_id = npcs.get(npc, ("?", "?"))
                alias = "same_name_id" if name_id in kept_name_ids else "distinct"
                rows.append((rel.replace(str(PROD.relative_to(REPO)) + "/", ""), field, npc,
                             desc, name_id, alias, ";".join(sorted(kept)), spawns.get(npc, "")))
    out = REPO / ".agents/summary/scriptdll-quest-driver/removed-spawned-audit.tsv"
    with out.open("w", encoding="utf-8") as fh:
        fh.write("# quest\tfield\tremoved_id\tremoved_name_desc\tremoved_name_id\trelation_to_kept\tkept_ids\tspawned_in\n")
        for row in rows:
            fh.write("\t".join(row) + "\n")
    alias_rows = [r for r in rows if r[6] and r[5] == "same_name_id"]
    distinct_rows = [r for r in rows if r[6] and r[5] == "distinct"]
    print(f"removed={len(rows)} still_spawned={sum(1 for r in rows if r[6])} "
          f"alias_still_spawned={len(alias_rows)} distinct_still_spawned={len(distinct_rows)}")
    print("--- 等价类别名（删除即回归） ---")
    for row in alias_rows:
        print("\t".join(row))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

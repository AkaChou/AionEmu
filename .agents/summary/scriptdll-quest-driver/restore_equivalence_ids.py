#!/usr/bin/env python3
"""回滚 Phase 4-2 对「同一客户端显示名（同 name_id）的等价类刷怪 id」的误删。

判定（三条件同时成立才回滚）：
  1. 该 id 是 Phase 4-2 从某 dimension 删除的（证据：removed-spawned-audit.tsv）；
  2. 它与该 dimension 保留的 id 共享 name_id（客户端显示名相同）；
  3. 其 name_id 家族大小 <= FAMILY_LIMIT（排除 350000 之类占位名大族）。

用法：python3 restore_equivalence_ids.py [--apply]
"""
from __future__ import annotations

import argparse
import collections
import re
import subprocess
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
PROD = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
PROD_REL = "src/main/resources/aion/data/static_data/quest_definition/quests"
NPCS = REPO / "src/main/resources/aion/data/static_data/npcs"
AUDIT = REPO / ".agents/summary/scriptdll-quest-driver/removed-spawned-audit.tsv"
FAMILY_LIMIT = 16

TEMPLATE = re.compile(r'<npc_template\b[^>]*?npc_id="(\d+)"[^>]*?>')
ATTR = re.compile(r'(\w[\w-]*)="([^"]*)"')
DIM = re.compile(r'<dimension\b[^>]*field="([^"]+)"[^>]*npc-ids="([^"]*)"[^>]*/>')


def family_index() -> tuple[dict[int, str], dict[str, set[int]]]:
    name_id: dict[int, str] = {}
    members: dict[str, set[int]] = collections.defaultdict(set)
    for path in sorted(NPCS.glob("npc_template_*.xml")):
        for match in TEMPLATE.finditer(path.read_text(encoding="utf-8", errors="ignore")):
            attrs = dict(ATTR.findall(match.group(0)))
            key = attrs.get("name_id")
            if not key:
                continue
            npc = int(match.group(1))
            name_id[npc] = key
            members[key].add(npc)
    return name_id, members


def retired_text(name: str) -> str | None:
    """已退役 XML 从 git 历史读取（仓库不再保留测试作用域冻结副本）。

    Retired XML text comes from git history; no test-scope copies exist anymore.
    """
    proc = subprocess.run(["git", "show", f"HEAD:{PROD_REL}/{name}"], cwd=REPO,
                          capture_output=True, text=True)
    return proc.stdout if proc.returncode == 0 else None


def load_audit() -> list[tuple[str, str, int]]:
    rows = []
    for line in AUDIT.read_text(encoding="utf-8").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        quest, field, removed, _desc, _name_id, relation, _kept, _spawn = line.split("\t")
        if relation == "same_name_id":
            rows.append((quest.removeprefix("test:").removesuffix(".xml"), field, int(removed)))
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    name_id, members = family_index()
    wanted: dict[Path, dict[str, set[int]]] = {}
    resurrect: set[Path] = set()
    skipped = []
    for quest, field, removed in load_audit():
        name = f"{quest}.xml"
        target = PROD / name
        if target.exists():
            text = target.read_text(encoding="utf-8")
        else:
            text = retired_text(name)
            if text is None:
                skipped.append((quest, field, removed, "no-file"))
                continue
            # 退役任务：回滚即从 git 历史复活生产 XML（同时需同步保留清单）。
            resurrect.add(target)
        for dim_field, ids in DIM.findall(text):
            if dim_field != field:
                continue
            kept = {int(x) for x in ids.split()}
            key = name_id.get(removed)
            if key is None:
                skipped.append((quest, field, removed, "no-template"))
            elif len(members[key]) > FAMILY_LIMIT:
                skipped.append((quest, field, removed, "family-too-large"))
            elif not any(name_id.get(k) == key for k in kept):
                skipped.append((quest, field, removed, "not-alias-of-kept"))
            else:
                wanted.setdefault(target, {}).setdefault(field, set()).add(removed)
    total = sum(len(v) for fields in wanted.values() for v in fields.values())
    print(f"files={len(wanted)} ids={total} skipped={len(skipped)}")
    for quest, field, removed, reason in skipped:
        print(f"  SKIP {quest} {field} {removed} ({reason})")
    for path, fields in sorted(wanted.items()):
        text = path.read_text(encoding="utf-8")
        for field, ids in fields.items():
            def repl(match: re.Match[str], field: str = field, ids: set[int] = ids) -> str:
                tag, current = match.group(0), match.group(1)
                merged = sorted({int(x) for x in current.split()} | ids)
                return tag.replace(f'npc-ids="{current}"', f'npc-ids="{" ".join(str(i) for i in merged)}"')
            pattern = re.compile(r'<dimension\b[^>]*field="%s"[^>]*npc-ids="([^"]*)"[^>]*/>' % re.escape(field))
            text, count = pattern.subn(repl, text, count=1)
            if count != 1:
                raise SystemExit(f"failed to patch {path} {field}")
            print(f"  {path.name} {field} += {' '.join(str(i) for i in sorted(ids))}")
        if args.apply:
            path.write_text(text, encoding="utf-8")
            if path in resurrect:
                print(f"  RESURRECT {path.name} from git history (同步保留清单 owner)")
    print("dry-run" if not args.apply else "applied")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

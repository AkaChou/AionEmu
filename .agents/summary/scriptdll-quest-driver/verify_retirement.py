#!/usr/bin/env python3
"""退役收口验证：目录一致性 + 悬空引用。

检查项：
  1. quest_definition_catalog.xml 每个 <definition> 的 resource 都存在；
  2. 生产 quests 目录文件数 == catalog 条目数；
  3. catalog 条目 + 保留清单 owner=RETAIL_TABLE 行 == 生产任务全集 6224，且两者无交集；
  4. 仓库内不存在测试作用域退役副本目录（旧 XML 只保留在 git 历史里）；
  5. 生产代码/脚本里不存在指向已退役 XML 的生产资源引用（catalog/docs 说明除外）。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
CATALOG = REPO / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"
RETENTION = REPO / "src/test/resources/quest/retail-xml-retention.tsv"
FIXTURE_DIR = REPO / "src/test/resources/quest/retired"
UNIVERSE = 6224


def retired_ids() -> set[str]:
    """已退役任务 id（保留清单 owner=RETAIL_TABLE）；旧 XML 只在 git 历史里。"""
    ids: set[str] = set()
    for line in RETENTION.read_text(encoding="utf-8").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) >= 2 and parts[1] == "RETAIL_TABLE":
            ids.add(parts[0])
    return ids


def main() -> int:
    text = CATALOG.read_text(encoding="utf-8")
    entries = re.findall(r'<definition id="(\d+)" resource="([^"]+)"', text)
    missing = [r for _, r in entries if not (REPO / "src/main/resources" / r).exists()]
    on_disk = {p.stem for p in QUESTS.glob("*.xml")}
    catalog_ids = {q for q, _ in entries}
    retired = retired_ids()
    problems = []
    if missing:
        problems.append(f"catalog resources missing: {missing[:5]} (n={len(missing)})")
    if len(entries) != len(on_disk):
        problems.append(f"catalog={len(entries)} but directory={len(on_disk)}")
    if catalog_ids != on_disk:
        problems.append("catalog ids != directory ids")
    if catalog_ids & retired:
        problems.append(f"retired quests still in catalog: {sorted(catalog_ids & retired)[:5]}")
    if FIXTURE_DIR.exists():
        problems.append(f"test-scope retired fixture dir must not exist: {FIXTURE_DIR.relative_to(REPO)}")
    if len(catalog_ids) + len(retired) != UNIVERSE:
        problems.append(f"catalog+retired={len(catalog_ids) + len(retired)} != universe {UNIVERSE}")
    refs = []
    for path in list((REPO / "src/main").rglob("*")) + list((REPO / "scripts").rglob("*")):
        if not path.is_file() or path.suffix not in {".java", ".xml", ".py", ".sh", ".properties", ".yml"}:
            continue
        if QUESTS in path.parents or path == CATALOG:
            continue
        body = path.read_text(encoding="utf-8", errors="ignore")
        for m in re.finditer(r"quest_definition/quests/(\d+)\.xml", body):
            if m.group(1) in retired:
                refs.append(f"{path.relative_to(REPO)} -> {m.group(1)}.xml")
    if refs:
        problems.append(f"dangling production references: {refs[:5]} (n={len(refs)})")
    print(f"catalog={len(catalog_ids)} directory={len(on_disk)} retired={len(retired)} "
          f"sum={len(catalog_ids) + len(retired)}")
    if problems:
        for problem in problems:
            print("FAIL:", problem)
        return 1
    print("OK: 目录一致，无悬空生产引用")
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""M2-e / P0c-3：把已迁移（IR 等价 286 + 裁定真端优先 62）的 SimpleHunt 任务从生产 XML 定义中退役。

做两件事（都可用 git 回退，不做任何提交）：
1) 删除生产资源 `src/main/resources/.../quest_definition/quests/<id>.xml`
   （**不保留测试作用域副本**：历史内容由 git 历史承担，退役事实只记在保留清单行）；
2) 从生产白名单 `quest_definition_catalog.xml` 移除对应 `<definition>` 行。

集合来源：`retail-simple-hunt-ir-fingerprints.tsv`（286 行，XML 等价证据）
∪ `retail-simple-hunt-adjudicated-ir-fingerprints.tsv`（P0c-3 哨兵裁定行，真端优先）。
用法：python3 -B m2e_retire_migrated_xml.py [--dry-run]
"""
import os
import argparse
import hashlib
import re
import subprocess
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
FINGERPRINTS = REPO / 'src/test/resources/quest/retail-simple-hunt-ir-fingerprints.tsv'
ADJUDICATED_FINGERPRINTS = (REPO / 'src/test/resources/quest'
	/ 'retail-simple-hunt-adjudicated-ir-fingerprints.tsv')
PROD_DIR = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quests'
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml'
EVIDENCE = REPO / '.agents/summary/scriptdll-quest-driver/m2e-retired-xml-evidence.tsv'


def migrated_ids():
    ids = []
    for path in (FINGERPRINTS, ADJUDICATED_FINGERPRINTS):
        if not path.is_file():
            continue
        for line in path.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            ids.append(line.split('\t')[0])
    return list(dict.fromkeys(ids))


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def in_git_history(rel):
    """HEAD 历史里是否仍有该 XML（幂等重跑判据；仓库不再保留测试作用域副本）。"""
    return subprocess.run(['git', 'cat-file', '-e', f'HEAD:{rel}'], cwd=REPO,
                          capture_output=True).returncode == 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()

    ids = migrated_ids()
    moved, missing, kept = [], [], []
    evidence = ['# M2-e 退役证据：quest_id / 退役前生产 XML sha256 / 退役后位置']
    for quest_id in ids:
        source = PROD_DIR / f'{quest_id}.xml'
        rel = str(source.relative_to(REPO))
        if source.is_file():
            digest = sha256(source)
            if not args.dry_run:
                source.unlink()
            moved.append(quest_id)
            evidence.append(f'{quest_id}\t{digest}\tgit-history:{rel}')
        elif in_git_history(rel):
            kept.append(quest_id)
            evidence.append(f'{quest_id}\t-\t(已退役，内容在 git 历史里)')
        else:
            missing.append(quest_id)

    text = CATALOG.read_text(encoding='utf-8')
    removed = 0
    lines = []
    for line in text.splitlines(keepends=True):
        match = re.search(r'<definition id="(\d+)"', line)
        if match and match.group(1) in set(ids):
            removed += 1
            continue
        lines.append(line)
    if not args.dry_run:
        CATALOG.write_text(''.join(lines), encoding='utf-8')
        EVIDENCE.write_text('\n'.join(evidence) + '\n', encoding='utf-8')

    remaining = len(re.findall(r'<definition id="\d+"', ''.join(lines)))
    print(f'migrated={len(ids)} moved={len(moved)} already_retired={len(kept)} missing={len(missing)}')
    print(f'catalog entries removed={removed} remaining={remaining}')
    if missing:
        print('MISSING:', missing[:20])
    return 0 if not missing else 1


if __name__ == '__main__':
    raise SystemExit(main())

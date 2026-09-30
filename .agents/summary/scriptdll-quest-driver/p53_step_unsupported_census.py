#!/usr/bin/env python3
"""P5-3 分桶：RETAIL_STEP_UNSUPPORTED 667 行按（接取类别 × 进度步骤组合）分解。

输出：每行 quest_id / acquire / 组合标签 / 步骤明细摘要；组合直方图放 stdout。
用法：python3 -B p53_step_unsupported_census.py
"""
import os
import re
from collections import Counter
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/data_driven_quest.xml'
DRIFT = REPO / 'src/test/resources/quest/retail-data-driven-drift.tsv'
OUT = Path(__file__).resolve().parent / 'p53-step-unsupported-census.tsv'


def step_unsupported_ids():
    ids = set()
    for line in DRIFT.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) >= 2 and parts[1] == 'REJECTED:RETAIL_STEP_UNSUPPORTED':
            ids.add(int(parts[0]))
    return ids


def main():
    wanted = step_unsupported_ids()
    rows = {}
    for block in TABLE.read_text(encoding='utf-8').split('<quest_data_driven>')[1:]:
        id_match = re.search(r'<id>(\d+)</id>', block)
        if not id_match:
            continue
        quest_id = int(id_match.group(1))
        if quest_id not in wanted:
            continue
        acquire = re.search(r'<category_acquire_>([^<]*)</category_acquire_>', block)
        steps = re.findall(r'<category_progress_>([^<]*)</category_progress_>', block)
        values = re.findall(r'<value0_progress_>([^<]*)</value0_progress_>', block)
        label = '+'.join(sorted(set(s.strip().lower() for s in steps))) if steps else 'NO_PROGRESS'
        rows[quest_id] = (acquire.group(1).strip() if acquire else 'NONE', label,
                          '|'.join(v.strip()[:40] for v in values))
    combo = Counter((acq, label) for acq, label, _ in rows.values())
    print(f'total={len(rows)} (drift={len(wanted)})')
    for (acq, label), count in combo.most_common():
        print(f'{count}\t{acq}\t{label}')
    lines = ['# quest_id\tacquire\tcombo\tvalues']
    for quest_id in sorted(rows):
        acq, label, values = rows[quest_id]
        lines.append(f'{quest_id}\t{acq}\t{label}\t{values}')
    OUT.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(f'written={OUT}')


if __name__ == '__main__':
    main()

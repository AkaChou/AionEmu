#!/usr/bin/env python3
"""把 RetailSimpleHuntFamilyGateTest 导出的拒绝直方图转成登记表 TSV（M2-c 批次 2）。

用法：
  mvn -o -q -Dtest=RetailSimpleHuntFamilyGateTest -Dretail.hunt.rejectsOut=/tmp/hunt-family6.txt test
  python3 .agents/summary/scriptdll-quest-driver/m2c2_register_rejects.py /tmp/hunt-family6.txt

输出：src/test/resources/quest/retail-simplehunt-compiler-rejects.tsv（quest_id, code, detail）
拒绝码语义见 RetailSimpleHuntDefinitionCompiler#precheck 的 javadoc。
"""
from __future__ import annotations

import pathlib
import sys

REPO = pathlib.Path(__file__).resolve().parents[3]
OUT = REPO / 'src/test/resources/quest/retail-simplehunt-compiler-rejects.tsv'
HEADER = [
    '# SimpleHunt 家族编译器拒绝登记表（quest_id, code, detail）',
    '# 语义："真端数据文件无法确定该任务的某个字段" → 该任务继续走 quest-definition XML（降级）。',
    '# 由 RetailSimpleHuntFamilyGateTest 消费：owner=RETAIL_TABLE 的任务必须全部合成，',
    '# 其余任务的拒绝码必须与本表逐条一致（新增/消失的拒绝都必须先在此登记）。',
    '# 一个任务可能同时有多个缺口，登记的是编译器 precheck 的首个判定（顺序：接取 NPC → 报告 NPC → 计数 → 怪名）。',
    '# 生成：.agents/summary/scriptdll-quest-driver/m2c2_register_rejects.py',
]


def main() -> int:
    dump = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else '/tmp/hunt-family6.txt')
    rows = []
    for line in dump.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) < 2 or parts[1] == 'ACCEPTED':
            continue
        detail = parts[2] if len(parts) > 2 else ''
        rows.append(f'{parts[0]}\t{parts[1]}\t{detail}')
    OUT.write_text('\n'.join(HEADER + rows) + '\n', encoding='utf-8')
    print(f'wrote {OUT} rows={len(rows)}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

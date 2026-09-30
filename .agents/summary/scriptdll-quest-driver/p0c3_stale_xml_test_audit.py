#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-3 收口审计：测试源码里"直接按 classpath 读任务 XML"的退役任务。

退役任务不再有 `src/main/resources/.../quests/<id>.xml`；若测试仍按资源路径直读，
在脏 target/classes 下会读到上一次构建的陈旧副本（假通过），clean 构建下会 NPE。
本脚本把这类引用找出来，避免"退役后仍靠旧 XML 通过"的假绿。

输出：p0c3-stale-xml-test-refs.tsv（test_file, quest_id, snippet）
"""
import os
import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
RETENTION = REPO / 'src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv'
TESTS = REPO / 'src/test/java'
XML_DIR = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quests'
OUT = Path(__file__).resolve().parent / 'p0c3-stale-xml-test-refs.tsv'
LITERAL = re.compile(r'quests/(\d+)\.xml')


def retired_ids():
    owners = {}
    for line in RETENTION.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) >= 4:
            owners[int(parts[0])] = parts[1]
    return {qid for qid, owner in owners.items() if owner == 'RETAIL_TABLE'}


def main():
    retired = retired_ids()
    rows = []
    for path in sorted(TESTS.rglob('*.java')):
        text = path.read_text(encoding='utf-8', errors='replace')
        for match in LITERAL.finditer(text):
            qid = int(match.group(1))
            if qid not in retired:
                continue
            line_no = text.count('\n', 0, match.start()) + 1
            rows.append({
                'test_file': str(path.relative_to(REPO)),
                'line': line_no,
                'quest_id': qid,
                'snippet': text.splitlines()[line_no - 1].strip()[:120],
            })
    with OUT.open('w', encoding='utf-8') as fh:
        fh.write('# P0c-3 审计：测试直读已退役任务 XML（脏 target/classes 下会假通过）\n')
        fh.write('test_file\tline\tquest_id\tsnippet\n')
        for row in rows:
            fh.write('\t'.join(str(row[c]) for c in ('test_file', 'line', 'quest_id', 'snippet')) + '\n')
    print('退役任务:', len(retired))
    print('直读退役 XML 的测试引用:', len(rows))
    for row in rows:
        print('  %s:%d -> %d' % (row['test_file'], row['line'], row['quest_id']))
    print('输出:', OUT)
    return 0


if __name__ == '__main__':
    sys.exit(main())

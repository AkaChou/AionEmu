#!/usr/bin/env python3
"""M5-b3：SimpleCollectItem 60 个带 ROUTE/OTHER 轴的逐行差异归纳。

口径与门禁一致：先按 RetailSimpleCollectItemGateTest#axisOf 把两侧独有行归轴，
只保留 ROUTE（结构转换）与 OTHER（非转换行）两类，再做形状归纳。
输出：m5b3-route-axis-breakdown.tsv（任务 × 侧 × 轴 × 形状 × 原文）
"""
import collections
import pathlib
import re
import sys

BASE = pathlib.Path('.agents/summary/scriptdll-quest-driver')
DETAIL = BASE / 'retail-simple-collect-item-drift-detail.tsv'
LINES = BASE / 'retail-simple-collect-item-diff-lines.tsv'
OUT = BASE / 'm5b3-route-axis-breakdown.tsv'

DIALOG_TOKENS = ('1012', '1013', '10255', '1008', '1009', '39', '20002', '10')
NUM = re.compile(r'\d+')


def axis_of(line):
    if line.startswith('N\t'):
        return 'REWARD_ROW' if re.fullmatch(r'N\tREWARD/\d+', line) else 'OTHER'
    if line.startswith('T\t'):
        if 'REWARD/0' in line or 'REWARD/1' in line or 'REWARD/2' in line:
            return 'REWARD_ROW'
        for token in DIALOG_TOKENS:
            if 'dialogId=' + token in line:
                return 'DIALOG_' + token
        return 'ROUTE'
    if line.startswith('L\t'):
        return 'OTHER'
    return 'OTHER'


def shape_of(line):
    """结构形状：源状态、事件、目标状态、条件/动作/提交后摘要（数字归一）。"""
    parts = line.split('\t', 1)
    kind, body = parts[0], parts[1] if len(parts) > 1 else ''
    if kind != 'T':
        return kind + '|' + NUM.sub('#', body)
    cols = body.split('>')
    if len(cols) < 7:
        return 'T|MALFORMED'
    src, event, tgt, cond, act, after, prio = cols[:7]
    src_st = src.split('/')[0]
    tgt_st = tgt.split('/')[0]

    def norm_list(text):
        text = text.strip()
        if text in ('', '[]'):
            return ''
        return NUM.sub('#', text)

    return (f'T|{src_st}|{NUM.sub("#", event)}|{tgt_st}'
            f'|cond={norm_list(cond)}|act={norm_list(act)}|after={norm_list(after)}')


def main():
    detail = {}
    for raw in DETAIL.read_text(encoding='utf-8').splitlines():
        if raw.startswith('#') or not raw.strip():
            continue
        parts = raw.split('\t')
        if len(parts) >= 6:
            detail[int(parts[0])] = parts[5]

    counter = collections.Counter()
    quests_by_shape = collections.defaultdict(list)
    out = [['quest_id', 'side', 'axis', 'shape', 'raw']]
    targets = sorted(q for q, axes in detail.items() if 'ROUTE' in axes or 'OTHER' in axes)
    for raw in LINES.read_text(encoding='utf-8').splitlines():
        if not raw.strip():
            continue
        qid_s, rest = raw.split('\t', 1)
        qid = int(qid_s)
        if qid not in targets:
            continue
        r_text, x_text = rest.split(' ;; X=', 1) if ' ;; X=' in rest else (rest, '')
        for side, text, prefix in (('RETAIL', r_text, 'R='), ('XML', x_text, '')):
            body = text[2:] if text.startswith('R=') else text
            for line in filter(None, (l.strip() for l in body.split(' | '))):
                axis = axis_of(line)
                if axis not in ('ROUTE', 'OTHER'):
                    continue
                sh = shape_of(line)
                counter[(side, axis, sh)] += 1
                quests_by_shape[(side, axis, sh)].append(qid)
                out.append([str(qid), side, axis, sh, line])

    OUT.write_text('\n'.join('\t'.join(r) for r in out) + '\n', encoding='utf-8')
    print(f'# targets={len(targets)} rows={len(out) - 1} -> {OUT}')
    for (side, axis, sh), n in counter.most_common():
        sample = quests_by_shape[(side, axis, sh)]
        uniq = sorted(set(sample))
        head = ','.join(str(q) for q in uniq[:10])
        print(f'{n:4d}\t{side:6s}\t{axis:5s}\t{sh}\t[{len(uniq)} 任务] {head}{"..." if len(uniq) > 10 else ""}')
    return 0


if __name__ == '__main__':
    sys.exit(main())

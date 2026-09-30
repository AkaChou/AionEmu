#!/usr/bin/env python3
"""P0c-36：族内「中间对话行阶梯」缺口清单（修正 START 过滤 + 三轴证据）。"""
from __future__ import annotations
import os
import csv, re
from pathlib import Path
from collections import Counter

REPO = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
DIALOGS = Path(f"{os.environ.get('AION_UNPACK_ROOT', os.path.expanduser('~/PycharmProjects/unpak'))}/data_unpacked/Dialogs")
RET = REPO / 'src/main/resources/aion/data/static_data/quest_retail'


def load_rows(path, key=0, val=1):
    out = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#'):
            p = line.split('\t')
            if p[0].isdigit():
                out[int(p[0])] = p[val]
    return out


def main():
    body = {int(m.group(1)): m.group(2) for m in re.finditer(
        r'<id id="(\d+)">(.*?)</id>', (RET / 'Quest_SimpleTalk.xml').read_text(encoding='utf-8'), re.S)}
    cp = {}
    for m in re.finditer(r'<quest>(.*?)</quest>', (RET / 'quest.xml').read_text(encoding='utf-8'), re.S):
        b = m.group(1)
        q = re.search(r'<id>(\d+)</id>', b)
        if q:
            c = re.search(r'<collect_progress>(\d+)</collect_progress>', b)
            cp[int(q.group(1))] = int(c.group(1)) if c else 0
    fam = load_rows(REPO / 'src/test/resources/quest/retail-simple-talk-drift.tsv')
    owner = load_rows(RET / 'retail-xml-retention.tsv')
    rows_n = load_rows(RET / 'quest_client_summary_rows.tsv')
    src = {}
    with open(REPO / 'docs/quest/client-dialog-mapping/quest-dialog-pages.csv', encoding='utf-8-sig') as f:
        for r in csv.DictReader(f):
            src.setdefault(int(r['quest_id']), r['source_file'])
    nodes = {}
    for line in (RET / 'quest_client_talk_chain_steps.tsv').read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#'):
            p = line.split('\t')
            if p[0].isdigit() and p[1] == 'N':
                nodes.setdefault(int(p[0]), []).append((p[2], p[3], p[4]))
    gap, modelled, noname, nosteps = [], [], [], []
    for q in sorted(fam):
        talks = [m.group(2).strip() for m in re.finditer(r'<talk_npc(\d)>([^<]*)</talk_npc\1>', body.get(q, ''))]
        if not talks:
            continue
        rel = src.get(q)
        if not rel or not (DIALOGS / rel).exists():
            nosteps.append(q)
            continue
        s = (DIALOGS / rel).read_text(encoding='utf-8', errors='replace')
        m = re.search(r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', s, re.S)
        if not m:
            nosteps.append(q)
            continue
        steps = [re.sub(r'\s+', ' ', re.sub(r'<[^>]+>', '', x)).strip()
                 for x in re.findall(r'<step>(.*?)</step>', m.group(1), re.S)]
        K = len(talks)
        named = len(steps) == K + 1 and all(('STR_DIC_N_' + talks[i]) in steps[i] for i in range(K))
        if not named:
            noname.append((q, K, len(steps), rows_n.get(q)))
            continue
        start = [int(n[2]) for n in nodes.get(q, []) if n[1] == 'START' and n[2].lstrip('-').isdigit()]
        reward = [int(n[2]) for n in nodes.get(q, []) if n[1] == 'REWARD' and n[2].lstrip('-').isdigit()]
        have = set(start) | set(reward)
        need = set(range(K + 1))
        rec = (q, K, len(steps), cp.get(q), sorted(start), sorted(reward), owner.get(q, 'XML'), fam[q])
        (modelled if need <= have else gap).append(rec)
    print('族内 talk 行：%d（步名对齐 %d：已建模 %d / 缺阶梯 %d；步名不对齐 %d；无客户端 steps %d）'
          % (len(modelled) + len(gap) + len(noname) + len(nosteps), len(modelled) + len(gap),
             len(modelled), len(gap), len(noname), len(nosteps)))
    print('--- 缺阶梯 %d 行（owner 分布 %s）' % (len(gap), dict(Counter(r[6] for r in gap))))
    for r in gap:
        print('  q=%-6d K=%d steps=%d cp=%-2s startVar=%-12s rewardVar=%-6s owner=%-13s drift=%s'
              % (r[0], r[1], r[2], r[3], str(r[4]), str(r[5]), r[6], r[7]))
    write_tsv(gap)
    print('--- 步名不对齐样本 %d 行:' % len(noname))
    for r in noname[:12]:
        print('  q=%-6d K=%d steps=%s clientRows=%s' % r)


def write_tsv(rows):
    tsv = HERE / 'p0c36-talk-ladder-census.tsv'
    head = ('# P0c-36 中间对话行阶梯普查（三轴证据；外部客户端 steps 文本 + 真端 talk_npcK + collect_progress）\n'
            '# 轴① steps 文本逐行点名 STR_DIC_N_<talk_npcK>；轴② steps = K+1；轴③ collect_progress = 末行索引\n'
            '# quest_id\tK\tsteps\tcollect_progress\tstart_var0s\treward_var0\towner\tdrift\t判定\n')
    lines = []
    for r in rows:
        verdict = 'GAP_LADDER_MISSING' if r[6] == 'XML_RETENTION' or r[4] or r[5] else 'GAP_LADDER_MISSING'
        lines.append('%d\t%d\t%d\t%s\t%s\t%s\t%s\t%s\t%s'
            % (r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7], verdict))
    tsv.write_text(head + '\n'.join(lines) + '\n', encoding='utf-8')
    print('census -> %s（%d 行）' % (tsv, len(rows)))


if __name__ == '__main__':
    main()

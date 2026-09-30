#!/usr/bin/env python3
"""P0c-36 普查：SimpleTalk 退役行的「中间对话行阶梯」缺口与证据。

判据（fail-closed，三源）：
  ① 真端行 talk_npcK 字段序 = 任务书前 K 行的对话 NPC（行文本含 STR_DIC_N_<talk_npcK>）；
  ② 客户端 quest_summary <steps> 行数 = K + 1，末行含 [%collectitem] 且提到 reward_npc_name；
  ③ 真端 quest.xml collect_progress = 末行索引（K）。
只有三条同时成立才判「阶梯缺失需合成」。
"""
from __future__ import annotations
import os
import csv, re, sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
DIALOGS = Path(f"{os.environ.get('AION_UNPACK_ROOT', os.path.expanduser('~/PycharmProjects/unpak'))}/data_unpacked/Dialogs")
RET = REPO / 'src/main/resources/aion/data/static_data/quest_retail'


def retail_rows():
    s = (RET / 'Quest_SimpleTalk.xml').read_text(encoding='utf-8')
    return {int(m.group(1)): m.group(2) for m in re.finditer(r'<id id="(\d+)">(.*?)</id>', s, re.S)}


def quest_xml_rows():
    s = (RET / 'quest.xml').read_text(encoding='utf-8')
    out = {}
    for m in re.finditer(r'<quest>(.*?)</quest>', s, re.S):
        b = m.group(1)
        q = re.search(r'<id>(\d+)</id>', b)
        if not q:
            continue
        cp = re.search(r'<collect_progress>(\d+)</collect_progress>', b)
        out[int(q.group(1))] = int(cp.group(1)) if cp else 0
    return out


def client_steps(qid, source_by_qid):
    src = source_by_qid.get(qid)
    if not src:
        return None
    p = DIALOGS / src
    if not p.exists():
        return None
    s = p.read_text(encoding='utf-8', errors='replace')
    m = re.search(r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', s, re.S)
    if not m:
        return None
    return [re.sub(r'\s+', ' ', re.sub(r'<[^>]+>', '', b)).strip()
            for b in re.findall(r'<step>(.*?)</step>', m.group(1), re.S)]


def main():
    body = retail_rows()
    cprog = quest_xml_rows()
    retired = {}
    for line in (RET / 'retail-xml-retention.tsv').read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#'):
            p = line.split('\t')
            if p[0].isdigit():
                retired[int(p[0])] = p[2]
    src_by = {}
    with open(REPO / 'docs/quest/client-dialog-mapping/quest-dialog-pages.csv', encoding='utf-8-sig') as f:
        for r in csv.DictReader(f):
            src_by.setdefault(int(r['quest_id']), r['source_file'])
    nodes = {}
    for line in (RET / 'quest_client_talk_chain_steps.tsv').read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#'):
            p = line.split('\t')
            if p[0].isdigit() and p[1] == 'N':
                nodes.setdefault(int(p[0]), []).append((p[2], p[3], p[4]))
    rows = []
    missing_evidence = []
    nodata = []
    for q, b in sorted(body.items()):
        if retired.get(q) != 'SimpleTalk':
            continue
        talks = [m.group(2).strip() for m in re.finditer(r'<talk_npc(\d)>([^<]*)</talk_npc\1>', b)]
        if not talks:
            continue
        reward = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', b)
        reward = reward.group(1).strip() if reward else ''
        steps = client_steps(q, src_by)
        if steps is None:
            nodata.append(q)
            continue
        K = len(talks)
        ok1 = len(steps) == K + 1 and all('STR_DIC_N_' + talks[i] in steps[i] for i in range(K))
        ok2 = bool(steps) and '[%collectitem]' in steps[-1] and reward and ('STR_DIC_N_' + reward) in steps[-1]
        ok3 = cprog.get(q) == len(steps) - 1
        start_nodes = [n for n in nodes.get(q, []) if n[1] == 'START']
        vars_ = sorted(int(n[2]) for n in start_nodes if n[2].isdigit())
        rows.append((q, K, len(steps), cprog.get(q), vars_, start_nodes
                     and [n[0] for n in start_nodes], ok1, ok2, ok3))
    gap = [r for r in rows if r[5] and r[6] and r[7] and r[8] and (r[3] not in r[4])]
    print('退役 SimpleTalk 有 talk_npc 行：%d（无客户端 steps 数据 %d 行）' % (len(rows), len(nodata)))
    print('三轴证据齐备且缺 var0=collect_progress 节点（=阶梯缺失）：%d 行' % len(gap))
    for r in gap:
        print('  q=%-6d talk=%d steps=%d collect_progress=%s startNodes=%s' % (r[0], r[1], r[2], r[3], sorted(r[4])))
    bad = [r for r in rows if not (r[6] and r[7] and r[8])]
    print('三轴证据不全（不判阶梯，逐行另证）：%d 行' % len(bad))
    for r in bad[:15]:
        print('  q=%-6d talk=%d steps=%d cp=%s ok=(step文本=%s,末行=%s,cp=%s)' % (r[0], r[1], r[2], r[3], r[6], r[7], r[8]))


if __name__ == '__main__':
    main()

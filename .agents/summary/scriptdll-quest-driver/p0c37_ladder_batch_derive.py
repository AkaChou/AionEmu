#!/usr/bin/env python3
"""P0c-37：阶梯余量分批的机器裁定（把 P0c-36 的手工三轴判定脚本化）。

输入：p0c36-talk-ladder-census.tsv（族内缺阶梯行）+ 真端表 / quest.xml / 客户端解包 CSV / 登记表。
输出：逐行裁定（K / 推进方式 / 阶段页链 / 阻塞轴），可行行进 p0c37-talk-ladder-decisions.tsv。

判定（全程 fail-closed，任何一轴没有证据就落到阻塞桶，不猜）：
  K        = 真端 talk_npc1..3 个数 == 客户端 steps-1（步名对齐由 P0c-36 普查保证）
  cp 轴    = collect_progress ∈ {0, K}；0 = 掉落门不限行（QuestService.isQuestDrop 只在
             collectingStep != 0 时校验 var0，见 src/main/java/com/aionemu/gameserver/services/QuestService.java），
             K = 掉落生效行（QE-061）
  reward 轴= 登记表 reward 节点 var0 == K（客户端末行 = 交付行；不等则归 REWARD 投影轴，本批不含）
  mode     = 阶段页链：末页带 HACTION_SETPRO{k}(10000+i) → SETPRO；页链无推进/续页 → TALK（仅 K=1）

自校验：已裁定的 24202 必须推出 SETPRO/K=2、80320 必须推出 TALK/K=1（与 P0c-36 手工裁定一致）——
这条校验同时验证本脚本的阶段页链遍历与生成器 apply_talk_ladder 同口径。
"""
from __future__ import annotations

import csv
import re
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
RET = REPO / 'src/main/resources/aion/data/static_data/quest_retail'
CLIENT_CSV = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-pages.csv'
CLIENT_ACTIONS = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-action-details.csv'
KNOWN = {24202: (2, 'SETPRO'), 80320: (1, 'TALK')}


def load_kv(path, val=1):
    out = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#'):
            p = line.split('\t')
            if p[0].isdigit():
                out[int(p[0])] = p[val]
    return out


def load_talks(body):
    return [m.group(2).strip() for m in re.finditer(r'<talk_npc(\d)>([^<]*)</talk_npc\1>', body)]


def derive_chain(q, k, client_pages, page_ids, actions):
    """阶段页链推导（与生成器 apply_talk_ladder 同一口径）。

    每阶段从 SELECT{k+1} 起走「续页」动作（动作 id 同时是页 id、且目标页名以 SELECT 开头），
    直到撞上推进按钮 HACTION_SETPRO{k}(10000+i)。找不到推进按钮且页链为空 → TALK（仅 K=1）。
    返回 (chain_desc, mode, reason)；reason 非空表示不能唯一裁定。
    """
    chain_desc = []
    mode = None
    for i in range(k):
        stage = i + 1
        page = 'SELECT%d' % (stage + 1)
        if page not in client_pages.get(q, set()):
            return chain_desc, None, '阶段 %d 页 %s 不在册' % (stage, page)
        cur, visited, chain, button = page, set(), [], None
        while True:
            acts = actions.get(q, {}).get(cur.lower(), set())
            if 10000 + i in acts:
                button = cur
                break
            cont = [(a, page_ids[q][a]) for a in acts
                    if a in page_ids[q] and page_ids[q][a].startswith('SELECT')]
            if not cont:
                break
            if len(cont) != 1:
                return chain_desc, None, '阶段 %d 页 %s 续页不唯一 %s' % (stage, cur, cont)
            chain.append((cur, cont[0][1]))
            cur = cont[0][1]
            if cur in visited:
                return chain_desc, None, '阶段 %d 续页循环' % stage
            visited.add(cur)
        if button is None:
            if k != 1 or chain:
                return chain_desc, None, '阶段 %d 无 SETPRO 且 K>1/有续页（TALK 只对单阶段成立）' % stage
            mode = 'TALK'
            chain_desc.append('stage%d:%s(TALK,QUEST_SELECT 推进)' % (stage, page))
        else:
            mode = 'SETPRO'
            chain_desc.append('stage%d:%s>%s>SETPRO%d(%d)@%s'
                              % (stage, page, '>'.join(t for _, t in chain) or '-', stage, 10000 + i, button))
    return chain_desc, mode, ''


def main():
    retail_body = {int(m.group(1)): m.group(2) for m in re.finditer(
        r'<id id="(\d+)">(.*?)</id>', (RET / 'Quest_SimpleTalk.xml').read_text(encoding='utf-8'), re.S)}
    cp = {}
    for m in re.finditer(r'<quest>(.*?)</quest>', (RET / 'quest.xml').read_text(encoding='utf-8'), re.S):
        b = m.group(1)
        q = re.search(r'<id>(\d+)</id>', b)
        if q:
            c = re.search(r'<collect_progress>(\d+)</collect_progress>', b)
            cp[int(q.group(1))] = int(c.group(1)) if c else 0
    rows_n = load_kv(RET / 'quest_client_summary_rows.tsv')
    # REWARD 投影裁定（P0c-34 通道）：生成器读该表覆盖 XML 转写值，并与客户端末行互证；
    # 本脚本同口径，否则会把已被裁定的行误判成 reward 轴阻塞。
    reward_overrides = {}
    ro_file = HERE / 'p0c34-chain-reward-row-overrides.tsv'
    if ro_file.exists():
        for line in ro_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p8 = line.split('\t')
            reward_overrides[int(p8[0])] = int(p8[1])
    owner = load_kv(RET / 'retail-xml-retention.tsv')
    nodes = defaultdict(list)
    for line in (RET / 'quest_client_talk_chain_steps.tsv').read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#'):
            p = line.split('\t')
            if p[0].isdigit() and p[1] == 'N':
                nodes[int(p[0])].append((p[2], p[3], p[4]))
    client_pages = defaultdict(set)
    page_ids = defaultdict(dict)
    with CLIENT_CSV.open(encoding='utf-8-sig') as fh:
        header = fh.readline().rstrip('\n').split(',')
        col = {name: i for i, name in enumerate(header)}
        for line in fh:
            p = line.rstrip('\n').split(',')
            if len(p) >= len(header):
                q = int(p[col['quest_id']])
                client_pages[q].add(p[col['html_page_name']].upper())
                if p[col['page_id']].isdigit():
                    page_ids[q][int(p[col['page_id']])] = p[col['html_page_name']].upper()
    actions = defaultdict(lambda: defaultdict(set))
    with CLIENT_ACTIONS.open(encoding='utf-8-sig', newline='') as fh:
        for row in csv.DictReader(fh):
            if (row['source_variant'] == 'active' and row['page_mapping'] == 'exact'
                    and row['action_id'].isdigit()):
                actions[int(row['quest_id'])][row['html_page_name'].lower()].add(int(row['action_id']))

    gap = [int(l.split('\t')[0]) for l in (HERE / 'p0c36-talk-ladder-census.tsv')
           .read_text(encoding='utf-8').splitlines() if l and not l.startswith('#')]
    # P0c-37：可达性分区（探针实测 QuestDialogOrderAudit）。只有"中间对话页确实不可达"的行才是真缺口；
    # 阶段以单 START 节点的 var0 旗标 + 条件化路由承载的行（普查轴①/②/③看不见 transition 级编码）
    # 不是缺口，不得重写其形状。
    partition = {}
    part_file = HERE / 'p0c37-gap-partition.tsv'
    if part_file.exists():
        for line in part_file.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            p9 = line.split('\t')
            partition[int(p9[0])] = (p9[1], p9[2])
    decision_files = ('p0c36-talk-ladder-decisions.tsv', 'p0c37-talk-ladder-decisions.tsv')
    done = set()
    existing = {}
    for name in decision_files:
        f = HERE / name
        if not f.exists():
            continue
        for line in f.read_text(encoding='utf-8').splitlines():
            if not line or line.startswith('#'):
                continue
            p7 = line.split('\t')
            done.add(int(p7[0]))
            if name == 'p0c37-talk-ladder-decisions.tsv':
                existing[int(p7[0])] = line

    ok_rows, blocked, decided = [], [], []
    for q in sorted(gap):
        talks = load_talks(retail_body.get(q, ''))
        k = len(talks)
        chain_desc, mode, reason = derive_chain(q, k, client_pages, page_ids, actions)
        if q in done:
            decided.append((q, k, mode))
            continue
        verdict, unreached = partition.get(q, ('UNMEASURED', '-'))
        if verdict != 'GENUINE_GAP':
            blocked.append((q, k, 'NO_LIVE_GAP', '中间页已可达（%s）：%s' % (verdict, unreached)))
            continue
        reward = [n[2] for n in nodes.get(q, []) if n[1] == 'REWARD' and n[2].lstrip('-').isdigit()]
        reward_source = 'registry'
        if q in reward_overrides:
            reward = [str(reward_overrides[q])]
            reward_source = 'override'
            assert int(rows_n.get(q, '0')) - 1 == reward_overrides[q], \
                '%d REWARD 裁定 %d != 客户端末行 %d' % (q, reward_overrides[q], int(rows_n.get(q, '0')) - 1)
        started = [n for n in nodes.get(q, []) if n[1] == 'START']
        if not nodes.get(q):
            blocked.append((q, k, 'NO_REGISTRY_RECORDS', '登记表无该行记录（未合成，需先采纳）'))
            continue
        if cp.get(q, 0) not in (0, k):
            blocked.append((q, k, 'CP_AXIS', 'collect_progress=%s 不在 {0,K=%d}（QE-061 掉落行）'
                            % (cp.get(q), k)))
            continue
        if reward != [str(k)]:
            blocked.append((q, k, 'REWARD_AXIS', '登记表 reward var0=%s != K=%d（客户端末行 = 交付行）'
                            % (reward, k)))
            continue
        if len(started) != 1:
            blocked.append((q, k, 'START_NODE', 'START 节点 %d 个（期望 1）' % len(started)))
            continue
        if mode is None:
            blocked.append((q, k, 'MODE_UNRESOLVED', reason))
            continue
        ok_rows.append((q, k, mode,
                        'steps%s(talk_npc%d=%s);cp=%d;reward_var0=%s(%s);started_var0=%s;%s;owner=%s'
                        % (rows_n.get(q, -1), k, talks[k - 1], cp.get(q, 0), reward[0], reward_source,
                           started[0][2], ','.join(chain_desc), owner.get(q, '?'))))

    print('缺阶梯 %d 行（其中已裁定 %d）：可入阶梯批 %d / 阻塞 %d'
          % (len(gap), len(done), len(ok_rows), len(blocked)))
    print('--- 可入阶梯批 ---')
    for r in ok_rows:
        print('  q=%-6d K=%d %-6s %s' % (r[0], r[1], r[2], r[3]))
    print('--- 阻塞 ---')
    for r in blocked:
        print('  q=%-6d K=%d %-18s %s' % r)

    # 自校验：已裁定行必须复现 P0c-36 的手工结论（同时验证页链遍历与生成器同口径）
    for q, (want_k, want_mode) in KNOWN.items():
        hit = [r for r in decided if r[0] == q] + [r for r in ok_rows if r[0] == q]
        assert hit and hit[0][1] == want_k and hit[0][2] == want_mode, \
            '自校验失败：%d 期望 K=%d/%s 实际 %s' % (q, want_k, want_mode, hit)
    print('自校验 OK：24202 → SETPRO/K=2、80320 → TALK/K=1（与 P0c-36 手工裁定一致）')

    merged = dict(existing)
    for r in ok_rows:
        merged[r[0]] = '%d\t%d\t%s\t%s' % r
    with (HERE / 'p0c37-talk-ladder-decisions.tsv').open('w', encoding='utf-8') as fh:
        fh.write('# P0c-37 阶梯余量分批裁定（三轴机器推导；自校验对齐 P0c-36 手工裁定）\n')
        fh.write('# quest_id\tK\tadvance_mode\tbasis\n')
        for q in sorted(merged):
            fh.write(merged[q] + '\n')
    print('decisions -> p0c37-talk-ladder-decisions.tsv（既有 %d + 新增 %d = %d 行）'
          % (len(existing), len(ok_rows), len(merged)))


if __name__ == '__main__':
    main()

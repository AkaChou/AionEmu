#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-46 角色轴普查（XML_ONLY_ROLE_AXIS_PENDING 的收口工具）

轴的来历
--------
P0c-45 的 `npc_check` 标记只判「集合成员」：NPC 只要出现在本任务的真端/客户端名字集合里就记
`RETAIL_MATCH`/`CLIENT_MATCH`，**不看它服务哪个角色**。于是「NPC 在声明集内、却服务了真端没
给它的角色」（把交付/领奖流挂在链上任意 NPC 上）不会被任何既有门禁标出。

判据历史（判例 QE-072：判据必须先在已知缺陷快照上触发、在修复后现状上不触发）
--------------------------------------------------------------------------
  v1  「同名块在多个 NPC 上重复」→ **否决**：40 项里绝大多数是**逐 NPC 对话绑定**
      （1484 的 5 个 NPC = 接取 + 3 阶段 + 报告，`B NPC_START` 是每个链上 NPC 的 QUEST_SELECT
      入口，删掉会打断阶段交互）。误报源 = 判据没看角色，也没看真端 talk_npcK 声明。
  v2  投影式三轴，但页分类把 **SELECT5/SELECT6 当作阶段页**（实为真端报告页族）→ 248 项
      压倒性假阳性（主体是 `page:SELECT5`）；覆盖比较也没做**角色域内**裁剪，把阶段页算进
      交付覆盖，导致覆盖守卫对全部候选 FAIL。
  v3（本文件）：
    ① 轴域收窄为 **DELIVER**（交付/领奖角色），ACCEPT/STAGE 扩散只作信息列——它们分属
       P0c-42 接取入口轴与阶梯轴，判据与守卫不同，不得混判。
    ② 页分类按真端语义：SELECT5/SELECT6/SHOW_SELECT_QUEST_REWARD_WINDOW* → DELIVER；
       SELECT1/SELECT1_*/SHOW_ASK_QUEST_ACCEPT_WINDOW → ACCEPT；SELECT2..4 → STAGE。
    ③ 覆盖守卫**角色域内**：被剪 NPC 的交付投影（块参数 + 交付动作 + 交付页）⊆ 声明 owner 的；
       块逐字相同是最强形态（33/38 项如此）。
    ④ 裁定对象 = 声明集**单值**的 (任务, NPC)：真端 `reward_npc_name` 唯一解析、客户端
       `end_npc_ids`（全行并集）唯一、且两者**相等**；否则记 `DELIVER_DECL_UNRESOLVED`
       （fail-closed，人工取证，不剪）。
    ⑤ 分类：`DELIVER_DUPLICATE_BLOCK`（可剪，块逐字重复）/ `DELIVER_SUBSET_ROLE_OK`
       （交付投影是 owner 的**真子集**，等价剪）/ `DELIVER_UNCOVERED`（该 NPC 自带 owner
       没有的交付页或交付动作 → 结构不同，**不得剪**，另行裁定）。

校准（必须成立）
----------------
  A. `p0c43-registry-pre.tsv`（24123 改道前）：必须出现 24123 的 DELIVER_UNCOVERED@204345
     （接取 NPC 服务整条交付流：SELECT5/SELECT6/reward window/CHECK_*/SET_SUCCEED，
     而 owner 204387 当时只有阶段行 ⇒ 覆盖不成立）。
  B. 现行登记表：24123 不得出现任何 DELIVER 候选（P0c-43 已改道）；
     35010/35011 不得出现（P0c-45 已剪 799806）。

用法
----
  python3 -B p0c46_role_axis_census.py [--registry <tsv>] [--out <tsv>] [--emit-decisions <tsv>]
"""

import argparse
import collections
import csv
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[3]
RETAIL_XML = ROOT / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml'
NPC_DIR = ROOT / 'src/main/resources/aion/data/static_data/npcs'
INDEX_CSV = ROOT / 'docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv'
DEFAULT_REG = ROOT / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv'

ACCEPT_ACTIONS = {
    'ASK_QUEST_ACCEPT', 'QUEST_ACCEPT_1', 'QUEST_ACCEPT_2', 'QUEST_ACCEPT_3', 'QUEST_ACCEPT_4',
    'QUEST_ACCEPT_SIMPLE', 'QUEST_REFUSE_1', 'QUEST_REFUSE_2', 'QUEST_REFUSE_SIMPLE',
}
DELIVER_ACTIONS = {
    'SELECT_QUEST_REWARD', 'CHECK_USER_HAS_QUEST_ITEM', 'CHECK_USER_HAS_QUEST_ITEM_SIMPLE',
    'SET_SUCCEED',
}
ACCEPT_PAGES = re.compile(r'^(SELECT1|SELECT1_\d+|SHOW_ASK_QUEST_ACCEPT_WINDOW)$')
DELIVER_PAGES = re.compile(r'^(SHOW_SELECT_QUEST_REWARD_WINDOW\d*|SELECT5|SELECT6|SELECT\d*_?REWARD.*)$')
STAGE_ACTIONS = re.compile(r'^SETPRO\d+$')
STAGE_PAGES = re.compile(r'^SELECT([2-4]|\d\d+)(_\d+)?$')
SENTINELS = {'', '-', '0', 'NONE', '_None_', '_none_', 'None'}


def load_retail_names():
    text = RETAIL_XML.read_text(encoding='utf-8')
    out = {}
    for m in re.finditer(r'<id id="(\d+)">(.*?)</id>', text, re.S):
        qid, body = int(m.group(1)), m.group(2)

        def g(tag):
            mm = re.search(r'<%s>([^<]*)</%s>' % (tag, tag), body)
            return mm.group(1).strip() if mm else ''
        out[qid] = {'acquired': g('acquired_npc_name'), 'reward': g('reward_npc_name'),
                    'talk': [g('talk_npc%d' % k) for k in range(1, 64) if g('talk_npc%d' % k)]}
    return out


def load_npc_names():
    idx = collections.defaultdict(set)
    for fl in sorted(NPC_DIR.glob('npc_template_*.xml')):
        for m in re.finditer(r'<npc_template\b[^>]*>', fl.read_text(encoding='utf-8', errors='ignore')):
            t = m.group()
            n = re.search(r'name_desc="([^"]*)"', t)
            i = re.search(r'\bnpc_id="(\d+)"', t)
            if not (n and i):
                continue
            idx[n.group(1)].add(i.group(1))
            if n.group(1).lower().startswith('npc_'):
                idx[n.group(1)[4:]].add(i.group(1))
    return idx


def load_client_index():
    out = collections.defaultdict(lambda: {'start': set(), 'end': set(), 'progress': set()})
    with INDEX_CSV.open(encoding='utf-8-sig') as fh:
        for r in csv.DictReader(fh):
            q = int(r['quest_id'])
            out[q]['start'] |= set(re.findall(r'\d+', r['start_npc_ids'] or ''))
            out[q]['end'] |= set(re.findall(r'\d+', r['end_npc_ids'] or ''))
            out[q]['progress'] |= set(re.findall(r'\d+', r['progress_npc_ids'] or ''))
    return out


def load_registry(path):
    reg = collections.defaultdict(list)
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p = line.split('\t')
        reg[int(p[0])].append(p)
    return reg


def pages_of_row(p):
    pages = set()
    for cell in (p[9] if len(p) > 9 else '', p[11] if len(p) > 11 else ''):
        for mm in re.finditer(r'PAGE:([A-Z0-9_]+)', cell):
            pages.add(mm.group(1))
        if cell and '=' in cell:
            for part in cell.split(';'):
                if '=' in part:
                    pages.add(part.split('=')[0].strip())
    return pages


def served_roles(lines):
    """NPC -> {role: 证据串集}；块参数单独存（逐字比较用）。"""
    served = collections.defaultdict(lambda: collections.defaultdict(set))
    blocks = collections.defaultdict(set)
    for p in lines:
        if p[1] == 'R':
            npc, act, pgs = p[3], p[4], pages_of_row(p)
            if act in ACCEPT_ACTIONS:
                served[npc]['ACCEPT'].add('act:' + act)
            if act in DELIVER_ACTIONS:
                served[npc]['DELIVER'].add('act:' + act)
            if STAGE_ACTIONS.match(act):
                served[npc]['STAGE'].add('act:' + act)
            for pg in pgs:
                if DELIVER_PAGES.match(pg):
                    served[npc]['DELIVER'].add('page:' + pg)
                elif ACCEPT_PAGES.match(pg):
                    served[npc]['ACCEPT'].add('page:' + pg)
                elif STAGE_PAGES.match(pg):
                    served[npc]['STAGE'].add('page:' + pg)
        elif p[1] == 'B' and p[2] in ('NPC_REPORT', 'NPC_COMPLETE'):
            served[p[3]]['DELIVER'].add('block:' + p[2])
            # 块参数逐字比较：取 (块名, source, target, extra...)，**不含 npc 列**（身份轴与角色轴分离）。
            blocks[p[3]].add(tuple([p[2]] + p[4:]))
    return served, blocks


def census(registry_path):
    retail, npcnames = load_retail_names(), load_npc_names()
    client = load_client_index()
    reg = load_registry(registry_path)
    rows = []

    def resolve(raw):
        name = (raw or '').strip()
        if name in SENTINELS:
            return set(), 'EMPTY'
        ids = set(npcnames.get(name, set()))
        if not ids and name.lower().startswith('npc_'):
            ids = set(npcnames.get(name[4:], set()))
        return (ids, 'OK') if ids else (set(), 'UNRESOLVED')

    for q in sorted(reg):
        rb, cb = retail.get(q, {}), client.get(q, {'start': set(), 'end': set(), 'progress': set()})
        acq, acq_s = resolve(rb.get('acquired', ''))
        rew, rew_s = resolve(rb.get('reward', ''))
        talk, talk_s = set(), 'EMPTY'
        for t in rb.get('talk', []):
            ids, st = resolve(t)
            talk |= ids
            if st == 'UNRESOLVED':
                talk_s = 'UNRESOLVED'
            elif st == 'OK' and talk_s != 'UNRESOLVED':
                talk_s = 'OK'
        served, blocks = served_roles(reg[q])
        reward_decl = rew | cb['end']
        chain_decl = acq | talk | cb['start'] | cb['progress']

        for npc in sorted(served):
            ev = served[npc]['DELIVER']
            if not ev or npc in reward_decl:
                continue
            role = 'CHAIN' if npc in chain_decl else 'UNDECLARED'
            if len(reward_decl) != 1:
                rows.append((q, npc, 'DELIVER_DECL_UNRESOLVED', role, sorted(reward_decl),
                             rb.get('reward', ''), sorted(cb['end']), '|'.join(sorted(ev)),
                             'retail=%s/client=%s' % (rew_s, 'OK' if cb['end'] else 'EMPTY')))
                continue
            owner = sorted(reward_decl)[0]
            if blocks[npc] == blocks[owner]:
                verdict = 'DELIVER_DUPLICATE_BLOCK'
            elif ev <= served[owner]['DELIVER']:
                verdict = 'DELIVER_SUBSET_ROLE_OK'
            else:
                verdict = 'DELIVER_UNCOVERED'
            rows.append((q, npc, verdict, role, sorted(reward_decl), rb.get('reward', ''),
                         sorted(cb['end']), '|'.join(sorted(ev)),
                         'retail_acq=%s talk=%s retail_rew=%s client_start=%s client_prog=%s' % (
                             sorted(acq), sorted(talk), rew_s, sorted(cb['start']), sorted(cb['progress']))))
        for axis, decl in (('ACCEPT', acq | cb['start']), ('STAGE', talk | cb['progress'])):
            ckey = {'ACCEPT': 'start', 'STAGE': 'progress'}[axis]
            for npc in sorted(served):
                ev = served[npc][axis]
                if ev and npc not in decl:
                    rows.append((q, npc, 'INFO_%s_SPREAD' % axis,
                                 'CHAIN' if npc in chain_decl else 'UNDECLARED', sorted(decl), '',
                                 sorted(cb[ckey]), '|'.join(sorted(ev)), 'informational'))
    return rows


HDR = ['quest_id', 'npc_id', 'verdict', 'role', 'declared_ids', 'retail_reward_name', 'client_end',
       'served', 'decl_status']


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--registry', default=str(DEFAULT_REG))
    ap.add_argument('--out', default='')
    ap.add_argument('--emit-decisions', default='')
    args = ap.parse_args()
    rows = census(pathlib.Path(args.registry))
    lines = ['# P0c-46 角色轴普查 v3（DELIVER 判定轴 + ACCEPT/STAGE 信息轴）: registry=%s' % args.registry,
             '\t'.join(HDR)]
    lines += ['\t'.join(str(x) for x in r) for r in rows]
    text = '\n'.join(lines) + '\n'
    if args.out:
        pathlib.Path(args.out).write_text(text, encoding='utf-8')
    if args.emit_decisions:
        dec = ['# P0c-46 角色收窄裁定（code=LEGACY_ROLE_SPREAD_DELIVER）',
               '# 判据（生成器逐轴 fail-closed 复算，见 build_quest_client_talk_chain_steps.py 装载段）：',
               '#   ① 被剪 NPC 的交付块与声明 owner **逐字相同**（去掉 npc 列比较）；',
               '#   ② 被剪 NPC 的交付投影（块 + 交付动作 + 交付页）⊆ owner 的；',
               '#   ③ 被剪 NPC 在真端 acquired∪talk_npcK 或客户端 start∪progress 内有**另一角色**；',
               '#   ④ 真端 reward_npc_name 唯一解析、客户端 end_npc_ids（全行并集）唯一、两者相等且 == owner。',
               '# 五源取证：真端 SimpleTalk 表 ×2（reward/acquired/talk）+ 客户端任务书角色列 + 客户端任务书页索引',
               '# + 客户端生命周期对齐（client-lifecycle-alignment.csv：仅接取 NPC 为 CLIENT_LIFECYCLE_ALIGNED，',
               '#   其余链上 NPC 为 EVIDENCE_REQUIRED）。',
               '# ' + '\t'.join(['quest_id', 'code', 'prune_npc', 'prune_role', 'owner_npc',
                                  'owner_name', 'client_end', 'served'])]
        for r in rows:
            if r[2] in ('DELIVER_DUPLICATE_BLOCK',) and r[3] == 'CHAIN':
                dec.append('\t'.join([str(r[0]), 'LEGACY_ROLE_SPREAD_DELIVER', str(r[1]), r[3],
                                      str(r[4][0]), r[5], ','.join(r[6]), r[7]]))
        pathlib.Path(args.emit_decisions).write_text('\n'.join(dec) + '\n', encoding='utf-8')
    cnt = collections.Counter(r[2] for r in rows)
    print('registry=%s' % args.registry)
    for k, v in sorted(cnt.items()):
        ids = sorted({r[0] for r in rows if r[2] == k})
        show = '' if k.startswith('INFO_') else ' %s' % ids
        print('  %-26s %3d 项 / %2d 任务%s' % (k, v, len(ids), show))
    for r in rows:
        if not r[2].startswith('INFO_'):
            print('   %-7d %-9s %-24s role=%-10s owner=%-9s ev=%s' % (r[0], r[1], r[2], r[3],
                  ','.join(r[4])[:9], r[7][:60]))
    return 0


if __name__ == '__main__':
    sys.exit(main())

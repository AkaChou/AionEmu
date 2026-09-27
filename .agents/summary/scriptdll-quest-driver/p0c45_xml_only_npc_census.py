#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-45：`npc_check=XML_ONLY:*` 全登记表身份轴普查（投影式，QE-072 口径）。

判据沿革（每次修正留档，QE-072 要求）：
  v1  仅按"标记字符串存在"计数 ⇒ 104 行，无法区分"口径假阳性"与真漂移。
  v2  引入 declared 集**展开**（census `names` 列里 `CLIENT:a,b` 是复合条目、`SENTINEL:x` 是哨兵字面量）
      ⇒ 93 行的标记是**口径假阳性**（生成器把复合条目当整串比较：`'799800' not in
      {'SENTINEL:_faction_','798946','CLIENT:799800,799801'}` 恒真）。
  v3  剩下的 11 行按**投影**判形（不用节点标签/动作名字面，QE-072）：
      served(npc) = 该 NPC 在块内的 (动作集, 下发页集)，其中下发页取自该行 after-commit 的
      `DIALOG:SHOW_QUEST_PAGE:*` / `DIALOG:SHOW_SELECTION_PAGE:*` token；
      与"同任务全部 declared NPC 的并集"比较：
        served ⊆ union  → EXTRA_DUPLICATE（多余人：删掉不丢页；可裁）
        否则             → SUBSTITUTE_OWNER（替身：真干活的人在真端声明集外；删掉会丢页）
  v4  再按 **owner**（retention 清单）分层：XML_RETENTION 的 IR 属 XML（标记只是转写证据）⇒
      RETAINED_*；RETAIL_TABLE 的 IR 由本登记表驱动 ⇒ LIVE_*（生产面缺陷候选）。

校准（QE-072 硬要求）：`--calibrate-pre <2482 修复前快照>` 断言 278018/278020 判为 LIVE_SUBSTITUTE_OWNER，
且现状登记表里这些 NPC 已不再出现（判据在"修复前/修复后"双向成立才可使用）。

输出：p0c45-xml-only-npc-census.tsv（每 flagged NPC 一行）
  quest_id | owner | npc | marker | rows | actions | pages | covered_by_declared |
  dev_recorded | client_reg_ids | verdict
"""
import argparse
import collections
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
REGISTRY = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv'
CENSUS = HERE / 'p0c10e-talk-chain-census.tsv'
RETENTION = REPO / 'src/test/resources/quest/retail-xml-retention.tsv'
REWARD_REG = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv'
OUT = HERE / 'p0c45-xml-only-npc-census.tsv'

PAGE_TOKEN = re.compile(r'DIALOG:SHOW_(?:QUEST|SELECTION)_PAGE:([A-Z0-9_]+)')


def load_census():
    """declared 名集（原始形态）+ 展开形态 + DEVIATION 记录的 NPC 集。"""
    raw, expanded, dev = {}, {}, {}
    for line in CENSUS.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p = line.split('\t')
        qid = int(p[0])
        entries = p[3].split('|')
        ids, client_ids = set(), set()
        for e in entries:
            if e.startswith('CLIENT:'):
                for t in e[len('CLIENT:'):].split(','):
                    if t.strip().isdigit():
                        client_ids.add(t.strip())
            elif e.startswith('SENTINEL:') or not e.isdigit():
                continue
            else:
                ids.add(e)
        raw[qid] = entries
        expanded[qid] = ids | client_ids
        dev[qid] = set(re.findall(r'@(\d+)', p[6]))
    return raw, expanded, dev


def load_owners():
    owners = {}
    for line in RETENTION.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p = line.split('\t')
        if len(p) >= 4:
            owners[int(p[0])] = p[1]
    return owners


def load_client_reward():
    reg = {}
    for line in REWARD_REG.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p = line.split('\t')
        reg[int(p[0])] = p[1]
    return reg


def load_registry(path):
    """quest → list[(kind, npc, action, pages)]（R 记录投影）。"""
    per = collections.defaultdict(list)
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p = line.split('\t')
        if p[1] != 'R':
            continue
        qid, npc, action = int(p[0]), p[3], p[4]
        pages = set()
        for token in (p[5], p[6], p[9]):
            pages |= set(PAGE_TOKEN.findall(token))
        per[qid].append((npc, action, pages, p[10] if len(p) > 10 else '-'))
    return per


def classify(per, raw, expanded, dev, owners, client_reg):
    rows = []
    for qid in sorted(per):
        flagged = collections.defaultdict(lambda: dict(actions=set(), pages=set(), rows=0, marker='-'))
        for npc, action, pages, marker in per[qid]:
            if not marker.startswith('XML_ONLY:'):
                continue
            f = flagged[npc]
            f['actions'].add(action)
            f['pages'] |= pages
            f['rows'] += 1
            f['marker'] = marker
        if not flagged:
            continue
        # 同任务 declared NPC 的投影并集（EXTRA vs SUBSTITUTE 判据）
        union_actions, union_pages = set(), set()
        for npc, action, pages, _m in per[qid]:
            if npc in expanded.get(qid, set()):
                union_actions.add(action)
                union_pages |= pages
        owner = owners.get(qid, '?')
        for npc, f in sorted(flagged.items()):
            covered = f['actions'] <= union_actions and f['pages'] <= union_pages
            recorded = npc in dev.get(qid, set())
            reg_ids = set(re.findall(r'\d+', client_reg.get(qid, '')))
            if npc in expanded.get(qid, set()):
                # v2：标记口径假阳性——CLIENT: 复合条目未展开（生成器把整串当 id 比较）
                verdict = 'MARKER_ARTIFACT_CLIENT_CHANNEL'
            elif covered:
                verdict = 'RETAINED_EXTRA_OWNER' if owner == 'XML_RETENTION' else 'LIVE_EXTRA_OWNER'
            else:
                verdict = 'RETAINED_SUBSTITUTE_OWNER' if owner == 'XML_RETENTION' else 'LIVE_SUBSTITUTE_OWNER'
            rows.append((qid, owner, npc, f['marker'], f['rows'], ';'.join(sorted(f['actions'])),
                ';'.join(sorted(f['pages'])), 'YES' if covered else 'NO',
                'YES' if recorded else 'NO', ','.join(sorted(reg_ids)) or '-', verdict))
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--registry', default=str(REGISTRY))
    ap.add_argument('--calibrate-pre', default=None,
        help='2482 修复前快照（p0c44-registry-pre.tsv）：断言第三方 NPC 判为 LIVE_SUBSTITUTE_OWNER')
    args = ap.parse_args()

    raw, expanded, dev = load_census()
    owners = load_owners()
    client_reg = load_client_reward()
    per = load_registry(Path(args.registry))
    rows = classify(per, raw, expanded, dev, owners, client_reg)

    if args.calibrate_pre:
        pre = load_registry(Path(args.calibrate_pre))
        pre_rows = classify(pre, raw, expanded, dev, owners, client_reg)
        pre_map = {(r[0], r[2]): r[10] for r in pre_rows}
        # 判据必须在"修复前"触发、在"修复后"不触发（QE-072 双向校准）
        for npc in ('278018', '278020'):
            v = pre_map.get((2482, npc))
            if v != 'LIVE_SUBSTITUTE_OWNER':
                print('CALIBRATION_FAIL 修复前快照 %d@%s 判定=%s（期望 LIVE_SUBSTITUTE_OWNER）' % (2482, npc, v))
                raise SystemExit(1)
        now = {(r[0], r[2]) for r in rows}
        for npc in ('278018', '278020'):
            if (2482, npc) in now:
                print('CALIBRATION_FAIL 现状登记表仍有 %d@%s' % (2482, npc))
                raise SystemExit(1)
        print('CALIBRATION_OK 2482@278018/278020：修复前= LIVE_SUBSTITUTE_OWNER，修复后= 无标记')

    out = ['# P0c-45 XML_ONLY 身份轴普查（投影式 v4；判据沿革见 p0c45_xml_only_npc_census.py 文件头）',
        '# quest_id\towner\tnpc\tmarker\trows\tactions\tpages\tcovered_by_declared\tdev_recorded'
        '\tclient_reg_ids\tverdict']
    for r in rows:
        out.append('\t'.join(str(x) for x in r))
    OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')

    verdicts = collections.Counter(r[10] for r in rows)
    print('普查 -> %s（%d flagged NPC 行）' % (OUT, len(rows)))
    for v, n in sorted(verdicts.items()):
        print('   %-32s %d' % (v, n))
    live = [r for r in rows if r[10].startswith('LIVE_')]
    if live:
        print('   生产面（RETAIL_TABLE）候选：')
        for r in live:
            print('     %d npc=%s rows=%d actions=%s covered=%s dev=%s' % (r[0], r[2], r[4], r[5], r[7], r[8]))


if __name__ == '__main__':
    main()

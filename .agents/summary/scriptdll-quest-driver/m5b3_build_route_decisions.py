#!/usr/bin/env python3
"""M5-b3：生成 SimpleCollectItem 60 个 ROUTE/OTHER 漂移行的逐任务裁定表。

判定口径（用户口径 = 真端优先，XML 只作对照）：
- ADOPT_RETAIL：真端/客户端证据支持真端形态，XML 为历史错误或缺项 → 放行退役；
- KEEP_XML  ：真端表无法表达 XML 里的路线/语义 → 保留 XML 降级。

输入：retail-simple-collect-item-drift-detail.tsv、retail-simple-collect-item-diff-lines.tsv、
      生产 quest XML（判断对象 TALK 路由是否存在）。
输出：m5b3-collect-route-decisions.tsv
"""
import pathlib
import re

REPO = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
HERE = REPO / '.agents/summary/scriptdll-quest-driver'
DETAIL = HERE / 'retail-simple-collect-item-drift-detail.tsv'
DIFF = HERE / 'retail-simple-collect-item-diff-lines.tsv'
QUEST_DIR = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quests'
OUT = HERE / 'm5b3-collect-route-decisions.tsv'

KEEP = {
    1137: ('WORK_ITEM_GRANT_AND_COLLECT_ROUTE',
           '真端 quest_work_item1=quest_1137a；XML 在接取路由显式 give-item 182200512 且有 collect-item 182200513 进度刷新路由；'
           '真端驱动两者都没有（引擎无 work-item 自动发放；CollectItem 进度路由不在合成器语义内）→ 保留 XML 降级'),
    28503: ('COLLECT_PROGRESS_ROUTE',
            'XML 有 collect-item 182212016 进度刷新路由；真端合成器无对应表达 → 保留 XML 降级'),
    2237: ('REPORT_NPC_DIVERGENCE',
           '真端 SimpleCollectItem.reward_npc_name=DF1A_Anmuring_E(=832822，接取 NPC)；客户端任务书交付对象为 Daike(203629)；'
           'XML 额外把交付挂在采集对象 700145 → 三方不一致，待复核前保留 XML'),
}
NPC_ID_CASES = {21464, 30052, 30152}


def targets():
    out = {}
    for raw in DETAIL.read_text(encoding='utf-8').splitlines():
        if raw.startswith('#') or not raw.strip():
            continue
        parts = raw.split('\t')
        if len(parts) >= 6 and ('ROUTE' in parts[5] or 'OTHER' in parts[5]):
            out[int(parts[0])] = parts[5]
    return out


def retail_only_lines(qid):
    for raw in DIFF.read_text(encoding='utf-8').splitlines():
        if not raw.strip():
            continue
        head, rest = raw.split('\t', 1)
        if int(head) != qid:
            continue
        r_text, x_text = rest.split(' ;; X=', 1) if ' ;; X=' in rest else (rest, '')
        return [l for l in r_text[2:].split(' | ') if l.strip()], [l for l in x_text.split(' | ') if l.strip()]
    return [], []


def object_route_state(qid):
    """返回 (drop_npc, talk_on_drop, object_after_commit)。"""
    path = QUEST_DIR / f'{qid}.xml'
    if not path.is_file():
        return None, None, None
    xml = path.read_text(encoding='utf-8')
    drops = sorted({int(m) for m in re.findall(r'<drop npc-id="(\d+)"', xml)})
    talks = {int(m) for m in re.findall(r'<dialog type="TALK_TO_NPC" npc-id="(\d+)"', xml)}
    hit = [d for d in drops if d in talks]
    after = None
    if hit:
        m = re.search(r'<dialog type="TALK_TO_NPC" npc-id="%d"/>' % hit[0] + r'\s*</event>\s*(<after-commit>.*?</after-commit>)?'
                      r'\s*</transition>', xml, re.S)
        after = 'CloseDialog' if m and m.group(1) and 'close-dialog' in m.group(1) else 'none'
    return drops, hit, after


def main():
    rows = ['# M5-b3 SimpleCollectItem ROUTE/OTHER 轴逐任务裁定（真端优先；XML 只作对照）',
            '# 生成：python3 -B m5b3_build_route_decisions.py',
            '# quest_id\tverdict\tbasis\taxes\tevidence']
    for qid, axes in sorted(targets().items()):
        r_lines, x_lines = retail_only_lines(qid)
        drops, talk_on_drop, after = object_route_state(qid)
        bare_object_route = any(l.startswith('T\tSTART/0>TalkToNpc[') and 'dialogId=null' in l
                                and '[]>[]>[]>-' in l for l in r_lines)
        if qid in KEEP:
            basis, evidence = KEEP[qid]
            verdict = 'KEEP_XML'
        elif qid in NPC_ID_CASES:
            verdict, basis = 'ADOPT_RETAIL', 'NPC_ID_RESOLUTION'
            evidence = ('客户端任务书 NPC 名与真端 reward/talk 名一致（Gellius / CaspaGhost_01 / Gelastra），'
                        'XML 解析成同名族其它 NPC → 真端 id 优先')
        elif qid == 1136:
            verdict, basis = 'ADOPT_RETAIL', 'OBJECT_TALK_AFTER_COMMIT'
            evidence = '对象 TALK 路由真端无 after-commit，XML 多 CloseDialog（无客户端页证据；与 DIALOG_1008 同口径）'
        elif qid == 14150:
            verdict, basis = 'ADOPT_RETAIL', 'JOURNAL_ROW_OVERFLOW'
            evidence = '客户端任务书 2 行（末行 1）；真端 REWARD/1 ✓，XML REWARD/2 + START/1 + COMPLETE/2 越界（QE-051）'
        elif qid == 2527:
            verdict, basis = 'ADOPT_RETAIL', 'OBJECT_HANDIN_DEAD_ROUTE'
            evidence = '客户端任务书交付对象 = 接取 NPC(NPC_Gark)；XML 把交付同时挂在采集对象 700328（死路由）'
        elif bare_object_route and not talk_on_drop:
            verdict, basis = 'ADOPT_RETAIL', 'OBJECT_TALK_ROUTE_MISSING'
            evidence = (f'XML 缺采集对象 TALK 路由（drop={drops}）；家族 29 个已退役 XML + 本批 6 个都显式声明该路由，'
                        '且 TalkToNpc 注册是对象成为任务目标的引擎路径')
        else:
            verdict, basis = 'ADOPT_RETAIL', 'OBJECT_TALK_ROUTE_VARIANT'
            evidence = f'对象挂点差异（drop={drops} talk={talk_on_drop} after={after}）→ 按真端形态放行'
        rows.append(f'{qid}\t{verdict}\t{basis}\t{axes}\t{evidence}')
    OUT.write_text('\n'.join(rows) + '\n', encoding='utf-8')
    adopt = sum(1 for r in rows[3:] if '\tADOPT_RETAIL\t' in r)
    keep = sum(1 for r in rows[3:] if '\tKEEP_XML\t' in r)
    basis_hist = {}
    for r in rows[3:]:
        b = r.split('\t')[2]
        basis_hist[b] = basis_hist.get(b, 0) + 1
    print(f'rows={len(rows) - 3} adopt_retail={adopt} keep_xml={keep} -> {OUT}')
    for b, n in sorted(basis_hist.items(), key=lambda kv: -kv[1]):
        print(f'  {n:3d}  {b}')


if __name__ == '__main__':
    main()

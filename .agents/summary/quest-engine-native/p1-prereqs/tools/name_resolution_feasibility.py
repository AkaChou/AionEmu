#!/usr/bin/env python3
"""P1 前置：名字解析可行性研究（只读）。

对 P0a 判定的 NATIVE_NAME_MISSING（真名缺失）与 NATIVE_NAME_AMBIGUOUS 两桶，
逐名实测三种桥接策略的覆盖率与歧义率：
  A. 子串/后缀规范化（模板名包含或结尾于真端名）
  B. 词元包含（模板名词元集 ⊇ 真端名词元集，允许地图前缀差异）
  C. 客户端 quest_monster.csv 侧存在性（B 类证据，不作解析只作交叉）
不产出任何生产代码/数据；输出可行性矩阵供 P1 决策。

注意：词元包含会匹配「等级/词元完全一致但地图前缀不同」的模板——是否采信属用户决策，
本研究只测覆盖率与碰撞率，不判定正确性。
"""
import collections
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2] / 'p0a' / 'tools'))
import owner_identity as oi  # noqa: E402

OUT = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test/.agents/summary/quest-engine-native/p1-prereqs')


def main():
    retail = oi.parse_family_tables()
    name2ids = oi.parse_npc_names()
    template_names = sorted(name2ids.keys())

    # 词元倒排索引（候选预筛）
    postings = collections.defaultdict(set)
    for idx, t in enumerate(template_names):
        for tok in set(t.split('_')):
            postings[tok].add(idx)

    client_monsters = set()
    for line in (oi.CLIENT / 'quest_monster.csv').read_text(errors='replace').splitlines()[1:]:
        parts = [p.strip() for p in line.split(',')]
        if len(parts) >= 7:
            client_monsters |= {m.strip().lower() for m in parts[6].split() if m.strip()}

    # 收集缺失真名 + 歧义名（行加权）
    missing = collections.Counter()
    ambiguous = collections.Counter()
    for r in retail.values():
        for nm in r['acquire'] + r['reward'] + r['monsters']:
            ids = name2ids.get(nm)
            if ids is None and not oi.is_sentinel(nm):
                missing[nm] += 1
            elif ids is not None and len(ids) > 1:
                ambiguous[nm] += 1

    stats = collections.Counter()
    rows = []
    for nm, weight in sorted(missing.items(), key=lambda kv: -kv[1]):
        toks = set(nm.split('_'))
        cand = set()
        for tok in toks:
            cand |= postings.get(tok, set())
        substr = suffix = tokensup = 0
        substr_ids, suffix_ids, tokensup_ids = [], [], []
        for idx in cand:
            t = template_names[idx]
            if nm in t:
                substr += 1
                substr_ids.append(t)
            if t.endswith(nm):
                suffix += 1
                suffix_ids.append(t)
            ttoks = set(t.split('_'))
            if toks <= ttoks:
                tokensup += 1
                tokensup_ids.append(t)
        in_client = nm in client_monsters
        if tokensup == 1:
            bucket = 'RESOLVABLE_TOKENSUP_UNIQUE'
        elif tokensup > 1:
            bucket = f'AMBIGUOUS_TOKENSUP({tokensup})'
        elif substr >= 1:
            bucket = f'SUBSTR_ONLY({substr})'
        elif in_client:
            bucket = 'CLIENT_EVIDENCE_ONLY'
        else:
            bucket = 'NOT_PRESENT_ANYWHERE'
        stats[bucket.split('(')[0]] += 1
        rows.append((nm, weight, substr, suffix, tokensup, in_client, bucket,
                     ';'.join(tokensup_ids[:4]) or ';'.join(substr_ids[:4])))
    amby = sum(ambiguous.values())

    OUT.mkdir(parents=True, exist_ok=True)
    with open(OUT / 'name-resolution-coverage.tsv', 'w') as f:
        f.write('missing_name\trow_weight\tsuffix_substr_matches\tsuffix_matches\ttoken_superset_matches\t'
                'in_client_csv\tbucket\tmatched_templates\n')
        for row in rows:
            f.write('\t'.join(str(x) for x in row) + '\n')

    unique_missing = len(rows)
    row_total = sum(w for _, w, *_ in rows)
    print(f'unique missing real names: {unique_missing}  (row-weighted: {row_total})')
    print(f'ambiguous names (exact-hit multi-id): {len(ambiguous)} (row-weighted: {amby})')
    print('bucket coverage (unique names):')
    for k, v in stats.most_common():
        print(f'  {k}: {v}')
    row_by_bucket = collections.Counter()
    for r in rows:
        row_by_bucket[r[6].split('(')[0]] += r[1]
    print('bucket coverage (row-weighted):')
    for k, v in row_by_bucket.most_common():
        print(f'  {k}: {v}')
    print('top NOT_PRESENT_ANYWHERE samples:',
          [r[0] for r in rows if r[6].startswith('NOT_PRESENT')][:15])
    print('top RESOLVABLE_TOKENSUP_UNIQUE samples:',
          [(r[0], r[7]) for r in rows if r[6].startswith('RESOLVABLE')][:10])
    print(f'wrote {OUT / "name-resolution-coverage.tsv"}')


if __name__ == '__main__':
    main()

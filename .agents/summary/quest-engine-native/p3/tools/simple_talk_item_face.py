#!/usr/bin/env python3
"""P3: SimpleTalk 物品面独立复算（只读，独立于 Java 实现）。

口径（与真端 SimpleTalk 通道一致，2026-10-01 定稿）：
  1) 交付门候选 = 真端 quest.xml 的 collect_item1..N；为空时取 quest_work_item1..N 的首项
     （与退役旧链路 workItemRequirement 同法）。
  2) 符号解析 = 去 ITEM_ 前缀后按物品模板 name_desc（小写）查 id；name_desc 缺失即未解析。
  3) 门声明了物品但存在任一项不可解 ⇒ 整行 fail-closed（报告门永不放行，等价旧 IR HasItem(全部声明物)）；
     此时若门整体不可解，登记证据项 item_check:<qid>。

输入：
  A. 真端表（repo 内入仓快照）Quest_SimpleTalk.xml
  B. 真端 quest.xml（collect_item / quest_work_item）
  C. 服务端物品模板 items/item/*.xml（name_desc → id）
输出：stdout 计数 + TSV 快照（--out）
"""
import argparse
import pathlib
import re
import sys

REPO = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
ROOT = REPO / 'src/main/resources/aion/data/static_data'
TALK = ROOT / 'quest/retail/Quest_SimpleTalk.xml'
QUEST = ROOT / 'quest/retail/quest.xml'
ITEM_DIR = ROOT / 'items/item'
SRC_TALK = pathlib.Path('/Users/mc/IdeaProjects/58Server/Map/XML/Quest_SimpleTalk.xml')

ROW = re.compile(r'<id id="(\d+)">(.*?)</id>', re.S)
FIELD = re.compile(r'<(\w+)>(.*?)</\1>', re.S)


def read(path):
    raw = path.read_bytes()
    return raw.decode('utf-16' if raw[:2] in (b'\xff\xfe', b'\xfe\xff') else 'utf-8', errors='replace')


def talk_rows():
    text = read(TALK)
    rows = {}
    for qid, body in ROW.findall(text):
        rows[int(qid)] = {k: v.strip() for k, v in FIELD.findall(body)}
    return rows


def quest_rows():
    text = read(QUEST)
    out = {}
    for body in re.findall(r'<quest>(.*?)</quest>', text, re.S):
        fields = {}
        for k, v in FIELD.findall(body):
            fields.setdefault(k, []).append(v.strip())
        ids = fields.get('id')
        if not ids:
            continue
        out[int(ids[0])] = fields
    return out


def numbered(fields, prefix):
    out = []
    for k, values in fields.items():
        m = re.fullmatch(rf'{prefix}(\d*)', k)
        if m:
            out.append((int(m.group(1)) if m.group(1) else 0, values[0]))
    out.sort()
    return [v for _, v in out if v]


def item_index():
    index = {}
    for path in sorted(ITEM_DIR.glob('*.xml')):
        text = read(path)
        for tmpl in re.finditer(r'<item_template\b([^>]*)>', text):
            attrs = tmpl.group(1)
            name = re.search(r'name_desc="([^"]*)"', attrs)
            iid = re.search(r'\bid="(\d+)"', attrs)
            if name and iid:
                index.setdefault(name.group(1).strip().lower(), int(iid.group(1)))
    return index


def resolve(symbol, items):
    """两类通道各一套约定（全量复算事实）：表 give/remove 列 = ITEM_X 形；
    quest.xml collect/work 列 = 原名形（含 item_* 真名）。故先原名、再 ITEM_ 别名。"""
    parts = symbol.split()
    if not parts:
        return None
    stem = parts[0].lower()
    count = int(parts[1]) if len(parts) > 1 else 1
    iid = items.get(stem)
    if iid is None and stem.startswith('item_'):
        iid = items.get(stem[len('item_'):])
    return None if iid is None else (iid, count)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=None)
    args = ap.parse_args()

    talk = talk_rows()
    quests = quest_rows()
    items = item_index()

    checked = [qid for qid, row in talk.items() if row.get('item_check')]
    resolvable, fail_closed, gaps, partials = [], [], [], []
    lines = ['kind\tquest_id\tgate\tsymbols']
    for qid in sorted(checked):
        fields = quests.get(qid, {})
        channel = 'collect_item'
        symbols = numbered(fields, 'collect_item')
        if not symbols:
            channel = 'quest_work_item'
            work = numbered(fields, 'quest_work_item')
            symbols = work[:1]
        resolved = [(s, resolve(s, items)) for s in symbols]
        ok = [r for _, r in resolved if r]
        missing = [s for s, r in resolved if not r]
        if symbols and not missing:
            resolvable.append(qid)
            lines.append(f'RESOLVED\t{qid}\t{channel}\t{"; ".join(symbols)}')
        elif not ok:
            fail_closed.append(qid)
            gaps.append(qid)
            lines.append(f'GATE_GAP\t{qid}\t{channel}\t{"; ".join(symbols) or "<empty>"}')
        else:
            fail_closed.append(qid)
            partials.append((qid, missing))
            lines.append(f'GATE_PARTIAL\t{qid}\t{channel}\tmissing: {"; ".join(missing)}')

    # 交付门回退符号（无 collect/work 但声明了发放物时旧链路的行为）
    fallback = []
    for qid in sorted(checked):
        fields = quests.get(qid, {})
        if numbered(fields, 'collect_item') or numbered(fields, 'quest_work_item'):
            continue
        row = talk[qid]
        candidates = [row.get('give_item')] if row.get('give_item') else []
        candidates += [row.get(f'give_item{i}') for i in (1, 2, 3)]
        candidates += [row.get(f'remove_item{i}') for i in (1, 2, 3)]
        fallback.append((qid, [c for c in candidates if c]))

    print(f'talk rows                : {len(talk)}')
    print(f'item_check rows          : {len(checked)}')
    print(f'gate resolved rows       : {len(resolvable)}')
    print(f'gate fail-closed rows    : {len(fail_closed)}  (GAP {len(gaps)} / PARTIAL {len(partials)})')
    print(f'gate with no xml channel : {len(fallback)}  (fallback rows)')
    print('gate gap ids             : ' + ','.join(map(str, gaps)))
    print('gate partial ids         : ' + ','.join(str(q) for q, _ in partials))

    if SRC_TALK.exists():
        src = {}
        for qid, body in ROW.findall(read(SRC_TALK).replace('\r\n', '\n')):
            src[int(qid)] = {k: v.strip() for k, v in FIELD.findall(body)}
        diff = [q for q in set(src) | set(talk) if src.get(q) != talk.get(q)]
        print(f'source vs repo rows      : {len(src)} src / {len(talk)} repo / {len(diff)} differing'
              f'  ({SRC_TALK})')
        if diff:
            print('  first diffs:', [(q, src.get(q), talk.get(q)) for q in sorted(diff)[:3]])

    if args.out:
        pathlib.Path(args.out).write_text('\n'.join(lines) + '\n', encoding='utf-8')
        print(f'wrote {args.out}')
    return 0


if __name__ == '__main__':
    sys.exit(main())

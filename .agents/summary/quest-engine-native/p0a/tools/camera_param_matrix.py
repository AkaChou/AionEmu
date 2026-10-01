#!/usr/bin/env python3
"""P0a: 相机参数矩阵 — 全树提取 FUN_180cb13b0(6位)/FUN_180cb14e0(10位) 调用点，
解码 (questId, slot, required, fullValue, flag)，校验：
  - 同任务宽度混用
  - 同任务 fullValue 一致性
  - fullValue == 各槽 required 的按位组合
  - required 超过 mask
  - 相机 id 与 DD 表交集（真端 DD 零相机）
  - 家族归属
输出: camera-params.tsv + 汇总统计(stdout)
"""
import collections
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

SRC_TREE = pathlib.Path('/Users/mc/IdeaProjects/58Server/server58-source')
XML = pathlib.Path('/Users/mc/IdeaProjects/58Server/Map/XML')
OUT = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test/.agents/summary/quest-engine-native/p0a/camera-params.tsv')

CALL_RE = re.compile(
    r'FUN_180cb(13b0|14e0)\(\s*(0x[0-9a-fA-F]+|\d+)\s*,\s*\w+\s*,\s*(0x[0-9a-fA-F]+|\d+)\s*,'
    r'\s*(0x[0-9a-fA-F]+|\d+)\s*,\s*(0x[0-9a-fA-F]+|\d+)\s*,\s*(0x[0-9a-fA-F]+|\d+)\s*\)')


def num(tok):
    return int(tok, 16 if tok.lower().startswith('0x') else 10)


def load_ids(path, tag, id_from_child=False):
    raw = path.read_bytes()
    text = raw.decode('utf-16') if raw[:2] in (b'\xff\xfe', b'\xfe\xff') else raw.decode('utf-8', errors='replace')
    m = re.search(r'<!DOCTYPE.*?\]>', text, flags=re.S)
    if m:
        dtd = m.group(0)
        text = text[:m.start()] + text[m.end():]
        for n, v in re.findall(r'<!ENTITY\s+(\S+)\s+"([^"]*)"\s*>', dtd):
            text = text.replace(f'&{n};', v)
    root = ET.fromstring(text)
    ids = set()
    for k in root.iter(tag):
        v = k.get('id') or k.get('quest_id')
        if v is None and id_from_child:
            cid = k.find('id')
            v = cid.text if cid is not None else None
        if v is not None and str(v).strip().lstrip('#').isdigit():
            ids.add(int(str(v).strip().lstrip('#')))
    return ids


FAMILIES = {
    'SimpleHunt': (XML / 'Quest_SimpleHunt.xml', 'id', False),
    'SimpleSerialHunt': (XML / 'Quest_SimpleSerialHunt.xml', 'id', False),
    'SimpleTalk': (XML / 'Quest_SimpleTalk.xml', 'id', False),
    'SimpleCollectItem': (XML / 'Quest_SimpleCollectItem.xml', 'id', False),
    'SimpleUseItem': (XML / 'Quest_SimpleUseItem.xml', 'id', False),
    'SimpleItemPlay': (XML / 'Quest_SimpleItemPlay.xml', 'id', False),
    'SimpleGather': (XML / 'Quest_SimpleGather.xml', 'id', False),
    'CombineTask': (XML / 'Quest_CombineTask.xml', 'id', False),
    'DataDriven': (XML / 'data_driven_quest.xml', 'quest_data_driven', False),
    'quest.xml': (XML / 'quest.xml', 'quest', True),
}


def main():
    fam_ids = {}
    for fam, (path, tag, child) in FAMILIES.items():
        try:
            fam_ids[fam] = load_ids(path, tag, child)
        except Exception as e:  # noqa: BLE001
            print(f'FAMILY_LOAD_FAIL {fam}: {e}', file=sys.stderr)
            fam_ids[fam] = set()

    rows = []          # (questId, width, slot, required, fullValue, flag, file, line)
    skipped = 0
    for cpp in sorted(SRC_TREE.rglob('*.cpp')):
        try:
            text = cpp.read_text(errors='replace')
        except Exception:
            continue
        for m in CALL_RE.finditer(text):
            qid = num(m.group(2))
            width = 6 if m.group(1) == '13b0' else 10
            slot, required, full, flag = num(m.group(3)), num(m.group(4)), num(m.group(5)), num(m.group(6))
            line = text[:m.start()].count('\n') + 1
            rel = str(cpp.relative_to(SRC_TREE))
            rows.append((qid, width, slot, required, full, flag, rel, line))
    total_literal = len(rows)

    # count non-literal / unparsed occurrences
    raw_count_6 = raw_count_10 = 0
    for cpp in SRC_TREE.rglob('*.cpp'):
        try:
            t = cpp.read_text(errors='replace')
        except Exception:
            continue
        raw_count_6 += t.count('FUN_180cb13b0(')
        raw_count_10 += t.count('FUN_180cb14e0(')

    by_q = collections.defaultdict(list)
    for r in rows:
        by_q[r[0]].append(r)

    # validations
    mixed = [q for q, rs in by_q.items() if len({r[1] for r in rs}) > 1]
    fullv_mismatch = {}
    over_mask = []
    fullv_bad = []
    for q, rs in by_q.items():
        fvs = {r[4] for r in rs}
        if len(fvs) > 1:
            fullv_mismatch[q] = sorted(fvs)
        w = rs[0][1]
        mask = 0x3F if w == 6 else 0x3FF
        for r in rs:
            if r[3] > mask:
                over_mask.append(r)
        # recompute expected fullValue from all slot requires
        expect = 0
        for r in rs:
            expect |= r[3] << ((r[2] - 1) * w)
        if expect != rs[0][4]:
            fullv_bad.append((q, hex(rs[0][4]), hex(expect)))

    cam_ids = set(by_q)
    dd_overlap = sorted(cam_ids & fam_ids.get('DataDriven', set()))
    not_in_any_family = sorted(q for q in cam_ids
                               if not any(q in fam_ids[f] for f in
                                          ('SimpleHunt', 'SimpleSerialHunt', 'SimpleTalk',
                                           'SimpleCollectItem', 'SimpleUseItem',
                                           'SimpleItemPlay', 'SimpleGather', 'CombineTask')))

    with open(OUT, 'w') as f:
        f.write('questId\twidth\tslot\trequired\tfullValue\tfullValue_hex\tflag\tfullValue_ok\tfamily\tevidence\n')
        for q in sorted(by_q):
            rs = sorted(by_q[q], key=lambda r: (r[2], r[1]))
            fam = next((f for f in ('SimpleHunt', 'SimpleSerialHunt', 'SimpleTalk',
                                    'SimpleCollectItem', 'SimpleUseItem',
                                    'SimpleItemPlay', 'SimpleGather', 'CombineTask')
                        if q in fam_ids[f]), 'DD' if q in fam_ids['DataDriven'] else 'UNDECLARED')
            expect = 0
            for r in rs:
                expect |= r[3] << ((r[2] - 1) * r[1])
            for qid, w, slot, req, full, flag, rel, line in rs:
                f.write(f'{qid}\t{w}\t{slot}\t{req}\t{full}\t{hex(full)}\t{flag}\t'
                        f'{"OK" if full == expect else f"BAD(expect={hex(expect)})"}\t{fam}\t{rel}:{line}\n')

    print(f'raw occurrences: 6bit={raw_count_6} 10bit={raw_count_10}')
    print(f'literal parsed rows: {total_literal} (unparsed/non-literal: {(raw_count_6+raw_count_10)-total_literal})')
    print(f'distinct quest ids: {len(cam_ids)}')
    print(f'width mixing quests: {len(mixed)} {mixed[:10]}')
    print(f'fullValue mismatch quests: {len(fullv_mismatch)} {list(fullv_mismatch.items())[:5]}')
    print(f'required over mask rows: {len(over_mask)}')
    print(f'fullValue != recomputed: {len(fullv_bad)} {fullv_bad[:5]}')
    print(f'camera∩DataDriven: {len(dd_overlap)} {dd_overlap[:10]}')
    print(f'camera ids not in any family table (incl CombineTask): {len(not_in_any_family)} sample={not_in_any_family[:15]}')
    print(f'rows written: {OUT}')


if __name__ == '__main__':
    main()

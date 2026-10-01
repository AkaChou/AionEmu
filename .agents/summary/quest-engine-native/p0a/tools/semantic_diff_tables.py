#!/usr/bin/env python3
"""P0a: 入仓真端表 vs 真端源表的语义级对拍（canonical 行多重集比较）。
判别 hash DIFF 是「编码/换行差异」还是「内容漂移」。
"""
import hashlib
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

SRC = pathlib.Path('/Users/mc/IdeaProjects/58Server/Map/XML')
REPO = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test/src/main/resources/aion/data/static_data/quest')
PAIRS = [
    ('quest.xml', REPO / 'retail/quest.xml'),
    ('data_driven_quest.xml', REPO / 'retail/data_driven_quest.xml'),
    ('Quest_SimpleHunt.xml', REPO / 'retail/Quest_SimpleHunt.xml'),
    ('Quest_CombineTask.xml', REPO / 'retail/Quest_CombineTask.xml'),
    ('Quest_SimpleCollectItem.xml', REPO / 'retail/Quest_SimpleCollectItem.xml'),
    ('Quest_SimpleItemPlay.xml', REPO / 'retail/Quest_SimpleItemPlay.xml'),
    ('Quest_SimpleSerialHunt.xml', REPO / 'retail/Quest_SimpleSerialHunt.xml'),
    ('quest_random_rewards.xml', REPO / 'legacy/quest_random_rewards.xml'),
]


def load(path):
    raw = path.read_bytes()
    text = raw.decode('utf-16') if raw[:2] in (b'\xff\xfe', b'\xfe\xff') else raw.decode('utf-8', errors='replace')
    m = re.search(r'<!DOCTYPE.*?\]>', text, flags=re.S)
    if m:
        dtd = m.group(0)
        text = text[:m.start()] + text[m.end():]
        # substitute custom entities declared in the internal subset
        for name, value in re.findall(r'<!ENTITY\s+(\S+)\s+"([^"]*)"\s*>', dtd):
            text = text.replace(f'&{name};', value)
    return ET.fromstring(text)


def canon_row(el):
    parts = [el.tag, sorted(el.attrib.items())]
    for child in el:
        parts.append((child.tag, tuple(sorted(child.attrib.items())), (child.text or '').strip()))
        parts.append(canon_row(child))
    return str(parts)


def canon_rows(path):
    root = load(path)
    kids = list(root)
    from collections import Counter
    row_name, _ = Counter(k.tag for k in kids).most_common(1)[0]
    rows = {}
    for k in kids:
        if k.tag != row_name:
            continue
        rid = None
        v = k.get('id') or k.get('quest_id')
        if v is None:
            cid = k.find('id') if k.find('id') is not None else k.find('quest_id')
            v = cid.text if cid is not None else None
        if v is not None and v.strip().lstrip('#').isdigit():
            rid = int(v.strip().lstrip('#'))
        key = rid if rid is not None else f'#{len(rows)}'
        rows[key] = hashlib.sha256(canon_row(k).encode()).hexdigest()
    return row_name, rows


def main():
    print('\t'.join(['table', 'verdict', 'rows_src', 'rows_repo',
                     'content_diff_ids', 'samples_src_vs_repo']))
    for name, repo in PAIRS:
        src = SRC / name
        try:
            _, srows = canon_rows(src)
        except Exception as e:  # noqa: BLE001
            print(f'{name}\tSRC_PARSE_FAIL:{e}')
            continue
        try:
            _, rrows = canon_rows(repo)
        except Exception as e:  # noqa: BLE001
            print(f'{name}\tREPO_PARSE_FAIL:{e}')
            continue
        diff = sorted(k for k in set(srows) & set(rrows) if srows[k] != rrows[k])
        only_s = sorted(set(srows) - set(rrows))
        only_r = sorted(set(rrows) - set(srows))
        samples = []
        for k in diff[:3]:
            samples.append(f'id{k}:src={srows[k][:8]} repo={rrows[k][:8]}')
        verdict = 'SEMANTIC_EQUAL' if not diff and not only_s and not only_r else 'CONTENT_DRIFT'
        print('\t'.join([name, verdict, str(len(srows)), str(len(rrows)),
                         f'diff={len(diff)} only_src={len(only_s)} only_repo={len(only_r)}',
                         ' '.join(samples)]))


if __name__ == '__main__':
    main()

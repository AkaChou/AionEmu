#!/usr/bin/env python3
"""P0a: 真端表来源清单 — 行数（按行元素口径）、源/仓 sha256、编码、入仓对拍。
只读审计工具；输出 TSV 到 stdout。
"""
import collections
import hashlib
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

SRC = pathlib.Path('/Users/mc/IdeaProjects/58Server/Map/XML')
REPO = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test/src/main/resources/aion/data/static_data/quest')

TABLES = [
    'quest.xml', 'data_driven_quest.xml', 'HtmlPages.xml',
    'Quest_SimpleTalk.xml', 'Quest_SimpleHunt.xml', 'Quest_CombineTask.xml',
    'Quest_SimpleCollectItem.xml', 'Quest_SimpleUseItem.xml',
    'Quest_SimpleItemPlay.xml', 'Quest_SimpleSerialHunt.xml',
    'Quest_SimpleGather.xml', 'item_quest.xml', 'npcfactions_quest.xml',
    'challenge_task.xml', 'quest_random_rewards.xml',
    'jumping_addquest.xml', 'jumping_endquest.xml',
]


def sha256(p):
    h = hashlib.sha256()
    with open(p, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def load_root(path):
    raw = path.read_bytes()
    if raw[:2] in (b'\xff\xfe', b'\xfe\xff'):
        text = raw.decode('utf-16')
    else:
        text = raw.decode('utf-8', errors='replace')
    # strip DOCTYPE internal subset (ends with ']>') so ElementTree can parse
    m = re.search(r'<!DOCTYPE.*?\]>', text, flags=re.S)
    if m:
        text = text[:m.start()] + text[m.end():]
    return ET.fromstring(text)


def row_stats(root):
    """顶层容器下的直接子元素 = 行元素；返回 (行元素名, 行数, id 属性值集合或 None)。"""
    kids = list(root)
    if not kids:
        return ('(empty)', 0, set())
    names = collections.Counter(k.tag for k in kids)
    row_name, cnt = names.most_common(1)[0]
    ids = set()
    for k in kids:
        if k.tag == row_name:
            v = k.get('id') or k.get('quest_id') or k.get('questId')
            if v is None:  # id as child element (quest.xml style)
                cid = k.find('id') if k.find('id') is not None else k.find('quest_id')
                v = cid.text if cid is not None else None
            if v is not None and v.strip().lstrip('#').isdigit():
                ids.add(int(v.lstrip('#')))
    return (row_name, cnt, ids)


def find_repo_counterpart(name):
    for base in (REPO / 'retail', REPO / 'legacy', REPO):
        for p in sorted(base.rglob(name)):
            return p
    return None


def id_attr_for(name, root):
    # non-quest-id tables keep their own key columns in the id column
    if name == 'HtmlPages.xml':
        return None
    return None


def main():
    print('\t'.join(['table', 'row_element', 'rows', 'src_sha256', 'src_bytes',
                     'repo_path', 'repo_sha256', 'hash_match', 'id_rows_src', 'id_rows_repo', 'id_diff']))
    for name in TABLES:
        src = SRC / name
        if not src.exists():
            # search one level deep
            hits = list(SRC.rglob(name))
            if not hits:
                print('\t'.join([name, 'SOURCE_MISSING', '', '', '', '', '', '', '', '', '']))
                continue
            src = hits[0]
        try:
            root = load_root(src)
            row_name, cnt, ids = row_stats(root)
        except Exception as e:  # noqa: BLE001
            print('\t'.join([name, f'PARSE_FAIL:{e}', '', sha256(src)[:16], str(src.stat().st_size), '', '', '', '', '', '']))
            continue
        repo = find_repo_counterpart(name)
        repo_sha = ''
        hash_match = ''
        repo_ids = ''
        id_diff = ''
        if repo is not None:
            repo_sha = sha256(repo)
            hash_match = 'SAME' if repo_sha == sha256(src) else 'DIFF'
            try:
                rroot = load_root(repo)
                _, rcnt, rids = row_stats(rroot)
                repo_ids = str(len(rids)) if rids else ''
                if ids and rids:
                    only_src = sorted(ids - rids)[:10]
                    only_repo = sorted(rids - ids)[:10]
                    id_diff = f'+src{len(ids - rids)}/+repo{len(rids - ids)}'
                    if only_src or only_repo:
                        id_diff += f' e.g. src_only={only_src} repo_only={only_repo}'
            except Exception as e:  # noqa: BLE001
                repo_ids = f'PARSE_FAIL'
        print('\t'.join([name, row_name, str(cnt), sha256(src), str(src.stat().st_size),
                         str(repo.relative_to(REPO)) if repo else 'NOT_IN_REPO',
                         repo_sha, hash_match,
                         str(len(ids)) if ids else '', repo_ids, id_diff]))


if __name__ == '__main__':
    main()

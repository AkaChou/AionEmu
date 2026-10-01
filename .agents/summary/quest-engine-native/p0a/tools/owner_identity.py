#!/usr/bin/env python3
"""P0a: owner 重冻 + owner-identity.tsv 三源身份矩阵（只读）。

四路输入：
  A. 真端家族表（/58Server/Map/XML/Quest_*.xml + data_driven_quest.xml）→ 表行声明
  B. 客户端解包（Quest_unpacked/quest.xml, data_driven_quest.xml, quest_monster.csv）→ 客户端证据
  C. 服务端静态数据（static_data/npcs/npc_template_*.xml）→ 名字→npc_id 唯一解析
  D. 本服台账 retail-xml-retention.tsv + definitions/quests/*.xml → 过渡期 owner 对拍

verdict 判定（计划 §2.6 口径）：
  NATIVE_READY_RAW_VARS_PENDING   三源齐且名字唯一解析（raw vars 待 DB 审计，单列）
  NATIVE_NAME_MISSING/AMBIGUOUS   真端声明名字在服务端静态数据缺失/歧义
  CLIENT_EVIDENCE_MISSING         id 不在客户端 quest.xml
  NO_TABLE_XML_ONLY               无真端表行，仅 XML 定义
  CONFLICT_CANDIDATE              真端表行与 XML 定义同时存在（现由台账仲裁）
"""
import collections
import pathlib
import re
import xml.etree.ElementTree as ET

HERE = pathlib.Path(__file__).resolve()
REPO = next(candidate for candidate in [HERE, *HERE.parents] if (candidate / 'pom.xml').is_file())


def host_dir(relative: str) -> pathlib.Path:
    """按 ENVIRONMENT.md 的同宿主目录约定解析外部根（支持 <workspace> 与 HOME 两种布局）。"""
    for candidate in [REPO.parent / relative, REPO.parent.parent / relative,
                      pathlib.Path.home() / relative]:
        if candidate.exists():
            return candidate
    raise SystemExit("cannot locate external root '" + relative + "'")


XML = host_dir('58Server') / 'Map/XML'
CLIENT = host_dir('PycharmProjects/unpak/Quest_unpacked')
NPC_DIR = REPO / 'src/main/resources/aion/data/static_data/npcs'
RETENTION = REPO / 'src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv'
DEFS = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quests'
OUT = REPO / '.agents/summary/quest-engine-native/p0a/owner-identity.tsv'

FAMILIES = ['SimpleTalk', 'SimpleHunt', 'SimpleSerialHunt', 'SimpleCollectItem',
            'SimpleUseItem', 'SimpleItemPlay', 'SimpleGather', 'CombineTask', 'DataDriven']
FAM_FILE = {f: (XML / ('data_driven_quest.xml' if f == 'DataDriven' else f'Quest_{f}.xml')) for f in FAMILIES}


def load(path):
    raw = path.read_bytes()
    text = raw.decode('utf-16') if raw[:2] in (b'\xff\xfe', b'\xfe\xff') else raw.decode('utf-8', errors='replace')
    m = re.search(r'<!DOCTYPE.*?\]>', text, flags=re.S)
    if m:
        dtd = m.group(0)
        text = text[:m.start()] + text[m.end():]
        for n, v in re.findall(r'<!ENTITY\s+(\S+)\s+"([^"]*)"\s*>', dtd):
            text = text.replace(f'&{n};', v)
    return ET.fromstring(text)


def names_of(row, prefix):
    """collect comma-split values of fields starting with prefix (acquired_npc_name, monsterN, task_npc...)."""
    out = []
    for c in row:
        if c.tag == prefix or re.fullmatch(rf'{prefix}\d*', c.tag):
            out += [t.strip().lower() for t in re.split(r'[;,]', (c.text or '')) if t.strip()]
    return out


def parse_family_tables():
    retail = {}   # id -> dict(family, acquire[], reward[], monsters[])
    for fam in FAMILIES:
        path = FAM_FILE[fam]
        if not path.exists():
            continue
        root = load(path)
        kids = list(root)
        if not kids:
            continue
        row_tag = collections.Counter(k.tag for k in kids).most_common(1)[0][0]
        for row in root:
            if row.tag != row_tag:
                continue
            v = row.get('id') or row.get('quest_id')
            if v is None:
                cid = row.find('id')
                v = cid.text if cid is not None else None
            if v is None or not str(v).strip().isdigit():
                continue
            qid = int(v)
            acquire = names_of(row, 'acquired_npc_name') + names_of(row, 'value0_acquire_')
            reward = names_of(row, 'reward_npc_name') + names_of(row, 'task_npc')
            monsters = names_of(row, 'monster')
            retail[qid] = {
                'family': fam,
                'acquire': sorted(set(acquire)),
                'reward': sorted(set(reward)),
                'monsters': sorted(set(monsters)),
            }
    return retail


def parse_npc_names():
    """名字→npc_id 索引：name ∪ name_desc 双属性。

    修正记录（2026-10-01）：初版只索引 name 属性，漏掉模板的 name_desc（真端式全名，
    如 npc_id=804915 name="Soglo" name_desc="DF5_Soglo_E"，真端任务表引用的是 name_desc），
    导致 13174 个名字引用被误判缺失；双属性索引后缺失降到 877。证据链见
    p1-prereqs/name-resolution-decision.md。
    Index both `name` and `name_desc`: retail quest tables reference the name_desc-style
    full dev names; indexing `name` alone was an audit-tool defect, not a data gap.
    """
    name2ids = collections.defaultdict(set)
    import re
    for p in sorted(NPC_DIR.glob('npc_template_*.xml')):
        raw = p.read_text(errors='replace')
        for m in re.finditer(r'<npc_template\b([^>]*)>', raw):
            attrs = m.group(1)
            nid = re.search(r'npc_id="(\d+)"', attrs)
            if not nid:
                continue
            nm = re.search(r'\bname="([^"]*)"', attrs)
            nd = re.search(r'name_desc="([^"]*)"', attrs)
            for attr in (nm, nd):
                if attr:
                    v = attr.group(1).strip().lower()
                    if v:
                        name2ids[v].add(nid.group(1))
    return name2ids


def parse_client():
    cq = load(CLIENT / 'quest.xml')
    client_ids = set()
    for el in cq.iter('quest'):
        cid = el.findtext('id')
        if cid and cid.strip().isdigit():
            client_ids.add(int(cid))
    csv_ids = set()
    csv_monsters = collections.defaultdict(set)
    for line in (CLIENT / 'quest_monster.csv').read_text(errors='replace').splitlines()[1:]:
        parts = [p.strip() for p in line.split(',')]
        if parts and parts[0].isdigit():
            qid = int(parts[0])
            csv_ids.add(qid)
            if len(parts) >= 7:
                csv_monsters[qid] |= {m.strip().lower() for m in parts[6].split() if m.strip()}
    return client_ids, csv_ids, csv_monsters


def parse_retention():
    rows = {}
    for line in RETENTION.read_text().splitlines():
        if not line.strip() or line.startswith('#'):
            continue
        parts = line.split('\t')
        if parts and parts[0].isdigit():
            rows[int(parts[0])] = parts[1:] if len(parts) > 1 else []
    return rows


SENTINEL_RE = re.compile(r'^(_[a-z]+_|test_|world_quest_|event_|quest_|[a-z]*_challengetask_)')


def is_sentinel(nm):
    return bool(SENTINEL_RE.match(nm))


def resolve(names, name2ids):
    """UNIQUE / AMBIGUOUS / MISSING / EMPTY — 全部名字的聚合判定 + 缺失清单（哨兵/真名分列）。"""
    if not names:
        return 'EMPTY', []
    missing, ambiguous, sentinels = [], [], []
    for nm in names:
        ids = name2ids.get(nm)
        if not ids:
            (sentinels if is_sentinel(nm) else missing).append(nm)
        elif len(ids) > 1:
            ambiguous.append(f'{nm}({len(ids)})')
    real_missing = [m for m in missing if m not in sentinels]
    if real_missing:
        return f'MISSING({len(real_missing)})', real_missing[:5]
    if ambiguous:
        return f'AMBIGUOUS({len(ambiguous)})', ambiguous[:5]
    if sentinels:
        return f'SENTINEL_ONLY({len(sentinels)})', sentinels[:5]
    return 'UNIQUE', []


def main():
    retail = parse_family_tables()
    name2ids = parse_npc_names()
    client_ids, csv_ids, csv_monsters = parse_client()
    retention = parse_retention()
    xml_def_ids = {int(p.stem) for p in DEFS.glob('*.xml') if p.stem.isdigit()}

    all_ids = sorted(set(retail) | set(retention) | xml_def_ids)
    stats = collections.Counter()
    fam_stats = collections.Counter()
    missing_names = collections.Counter()

    with open(OUT, 'w') as f:
        f.write('questId\tretail_family\tretail_acquire\tretail_reward\tretail_monsters\t'
                'npc_acquire\tnpc_reward\tnpc_monsters\tclient_quest_xml\tclient_monster_csv\t'
                'retention_owner\tretention_reason\txml_definition\tverdict\tdetail\n')
        for qid in all_ids:
            r = retail.get(qid)
            ret = retention.get(qid, [])
            ret_owner = ret[0] if ret else 'ABSENT'
            in_xml = qid in xml_def_ids
            in_client = qid in client_ids
            in_csv = qid in csv_ids
            if r is None:
                verdict = 'NO_TABLE_XML_ONLY' if in_xml and not ret_owner.startswith('RETAIL') else (
                    'LEDGER_ONLY_NO_TABLE' if ret_owner.startswith('RETAIL') else 'UNREFERENCED_ID')
                f.write(f'{qid}\t\t\t\t\t\t\t\t{in_client}\t{in_csv}\t{ret_owner}\t'
                        f'{ret[1] if len(ret) > 1 else ""}\t{in_xml}\t{verdict}\t\n')
                stats[verdict] += 1
                continue
            fam = r['family']
            a_res, a_det = resolve(r['acquire'], name2ids)
            w_res, w_det = resolve(r['reward'], name2ids)
            m_res, m_det = resolve(r['monsters'], name2ids)
            if in_xml and not ret_owner.startswith('XML'):
                verdict = 'CONFLICT_CANDIDATE'
            elif not in_client:
                verdict = 'CLIENT_EVIDENCE_MISSING'
            elif any(x.startswith('MISSING') for x in (a_res, w_res, m_res)):
                verdict = 'NATIVE_NAME_MISSING'
            elif any(x.startswith('AMBIGUOUS') for x in (a_res, w_res, m_res)):
                verdict = 'NATIVE_NAME_AMBIGUOUS'
            elif any(x.startswith('SENTINEL_ONLY') for x in (a_res, w_res, m_res)):
                verdict = 'NATIVE_READY_SENTINEL_ACQUIRE'
            else:
                verdict = 'NATIVE_READY_RAW_VARS_PENDING'
            detail = []
            for label, res, det in (('acquire', a_res, a_det), ('reward', w_res, w_det), ('monsters', m_res, m_det)):
                if det:
                    detail.append(f'{label}:{res}={{" ".join(det)}}' if False else f'{label}:{det}')
            f.write(f'{qid}\t{fam}\t{";".join(r["acquire"])}\t{";".join(r["reward"])}\t{";".join(r["monsters"])}\t'
                    f'{a_res}\t{w_res}\t{m_res}\t{in_client}\t{in_csv}\t{ret_owner}\t'
                    f'{ret[1] if len(ret) > 1 else ""}\t{in_xml}\t{verdict}\t{" ".join(detail)}\n')
            stats[verdict] += 1
            fam_stats[(fam, verdict)] += 1
            for nm in r['acquire'] + r['reward']:
                if nm not in name2ids:
                    missing_names[nm] += 1

    print(f'rows: {len(all_ids)} -> {OUT}')
    print('retail table rows (真端):', len(retail))
    print('retention rows:', len(retention), ' xml defs:', len(xml_def_ids))
    print('npc template distinct names:', len(name2ids))
    print('verdicts:')
    for k, v in sorted(stats.items()):
        print(f'  {k}: {v}')
    print('by family x verdict:')
    for (fam, v), n in sorted(fam_stats.items()):
        print(f'  {fam:18s} {v:32s} {n}')
    print('top missing npc names:', missing_names.most_common(15))


if __name__ == '__main__':
    main()

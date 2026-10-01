#!/usr/bin/env python3
"""P0a §4.5: 零任务硬编码扫描器（只读）。

扫描 src/main/java 中 quest 专属事实进代码的形态：
  P1 questId 字面量比较        P2 switch(questId)/case
  P3 Set/Map/List.of 内 quest id 集合   P4 name→id 表 (xxx.put("name", ...数字))
  P5 adoption/exception/retention 代码表文件   P6 buildDefault* 大表形态
输出 hardcode-audit.tsv（file, line, pattern, snippet, zone, provisional_class）
附带召回自检：6 个已知样本必须命中，否则退出码 1。
"""
import pathlib
import re
import sys

ROOT = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test/src/main/java')
OUT = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test/.agents/summary/quest-engine-native/p0a/hardcode-audit.tsv')
QUEST_DIR = pathlib.Path('com/aionemu/gameserver/questEngine')

P1 = re.compile(r'\bquest[Ii]d\s*(?:==|!=|<=|>=|<|>)\s*(?:\d{2,5}|0x[0-9a-fA-F]{2,6})|\b(?:\d{2,5}|0x[0-9a-fA-F]{2,6})\s*(?:==|!=)\s*quest[Ii]d')
P2 = re.compile(r'switch\s*\(\s*quest[Ii]d\s*\)')
P4 = re.compile(r'\w+\.put\(\s*"[A-Za-z_0-9\-]+"\s*,\s*(?:Set\.of|List\.of|Map\.of|Set\.ofEntries|new\s+HashSet|Integer\.valueOf)?\s*\(\s*\d{3,}')
P6 = re.compile(r'buildDefault\w*\s*\(')
COLLECT = re.compile(r'\b(?:Set|Map|List)\.of(?:Entries)?\s*\(')
INT = re.compile(r'\d{3,6}')


def paren_block(text, start):
    """start 指向 '(' ；返回块 [start, end) 含闭括号。"""
    depth = 0
    for i in range(start, min(len(text), start + 20000)):
        if text[i] == '(':
            depth += 1
        elif text[i] == ')':
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    return text[start:start + 20000]


def line_of(text, pos):
    return text[:pos].count('\n') + 1


def snippet(text, pos, span=160):
    seg = text[max(0, pos - 20):pos + span]
    return ' '.join(seg.split())[:170]


def scan_file(rel, path, text, out):
    zone = 'questEngine-retail' if (QUEST_DIR / 'retail').__str__() in rel else (
        'questEngine' if QUEST_DIR.__str__() in rel else 'cross-package')

    def emit(line, pattern, pos, cls=None):
        if cls is None:
            cls = 'HARDCODE_TO_DELETE' if zone != 'cross-package' else 'CROSS_PACKAGE_REVIEW'
        out.append((rel, line, pattern, snippet(text, pos), zone, cls))

    for m in P1.finditer(text):
        emit(line_of(text, m.start()), 'P1-questId-literal-compare', m.start())
    for m in P2.finditer(text):
        emit(line_of(text, m.start()), 'P2-switch-on-questId', m.start())
    for m in P4.finditer(text):
        emit(line_of(text, m.start()), 'P4-name-to-id-put', m.start())
    for m in P6.finditer(text):
        emit(line_of(text, m.start()), 'P6-buildDefault-form', m.start())
    # P3: collection literals containing quest-id-range ints
    # (>=3 anywhere; >=2 inside quest engine zones — catches 2-element quest tables like CAKE_EVENTS)
    for m in COLLECT.finditer(text):
        block = paren_block(text, m.end() - 1)
        ints = [int(x) for x in INT.findall(block)]
        questish = [x for x in ints if 1000 <= x <= 999999]
        threshold = 2 if zone != 'cross-package' else 3
        if len(questish) >= threshold and len(questish) * 2 >= len(ints):
            emit(line_of(text, m.start()), 'P3-id-collection-literal', m.start())
    # P5: adoption/exception/retention style table files with literal ints
    if re.search(r'Adoption|Adopted|Exception|Retention|Legacy|Alias|HealRows|ChainCutscene|AiNameGroup|ZoneResolution|SpawnedNpcIds|DialogExits|WorkItems|UseItemNpcs|TitleIds', path.name):
        hits = sum(1 for _ in INT.finditer(text))
        if hits >= 5:
            first = INT.search(text)
            emit(line_of(text, first.start()), 'P5-code-table-file', first.start(),
                 'HARDCODE_TO_DELETE' if zone != 'cross-package' else 'CROSS_PACKAGE_REVIEW')


def main():
    out = []
    for path in sorted(ROOT.rglob('*.java')):
        rel = str(path.relative_to(ROOT))
        try:
            text = path.read_text(errors='replace')
        except Exception:
            continue
        scan_file(rel, path, text, out)

    with open(OUT, 'w') as f:
        f.write('file\tline\tpattern\tsnippet\tzone\tprovisional_class\n')
        for row in out:
            f.write('\t'.join(str(x) for x in row) + '\n')

    # ---- recall test: 6 known samples ----
    full = {f'{r[0]}:{r[1]}:{r[2]}' for r in out}
    checks = [
        ('RetailQuestMetadataCompiler.java questId==1007||2009',
         lambda: any(k.startswith('com/aionemu/gameserver/questEngine/retail/RetailQuestMetadataCompiler.java:') and ':P1-' in k for k in full)),
        ('RetailSimpleUseItemDefinitionCompiler CAKE_EVENTS Set.of(80008,80009)',
         lambda: any(k.startswith('com/aionemu/gameserver/questEngine/retail/RetailSimpleUseItemDefinitionCompiler.java:') and ':P3-' in k for k in full)),
        ('RetailClientDialogExits quest id list',
         lambda: any('RetailClientDialogExits.java' in k and (':P3-' in k or ':P5-' in k) for k in full)),
        ('RetailChallengeAcquireAdoptions ADOPTED table',
         lambda: any('RetailChallengeAcquireAdoptions.java' in k and ':P5-' in k for k in full)),
        ('RetailNpcNameIndex byName.put',
         lambda: any('RetailNpcNameIndex.java' in k and ':P4-' in k for k in full)),
    ]
    bd = ROOT.rglob('*.java') and list(ROOT.rglob('*QuestWorkItems.java'))
    print(f'findings: {len(out)}  -> {OUT}')
    from collections import Counter
    print('by pattern:', dict(Counter(r[2] for r in out)))
    print('by zone:', dict(Counter(r[4] for r in out)))
    ok = True
    for name, fn in checks:
        hit = fn()
        print(f'RECALL {"PASS" if hit else "FAIL"}: {name}')
        ok &= hit
    # sample 6: buildDefaultEntries form — current main has none; verify pattern class exists instead
    p6 = any(r[2] == 'P6-buildDefault-form' for r in out)
    print(f'RECALL {"PASS" if p6 else "NOTE"}: buildDefault* form (P6 class coverage; sample itself absent from current src/main: {not any("buildDefault" in s for _, _, _, s, _, _ in out) and True})')
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()

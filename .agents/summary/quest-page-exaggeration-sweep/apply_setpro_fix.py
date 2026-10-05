#!/usr/bin/env python3
"""SETPROn 类「页 10 翻译夸大」批量修复：38 行删除 + 2114 两行替换为真端页。"""
import re, os

BASE = 'src/main/resources/aion/data/static_data/quest/definitions/quests'
DEL = 'del'; SEL2 = 'sel2'; SEL3 = 'sel3'
ROWS = {
 1007: [('s2', f'reward{n}', 'SETPRO3', 'FUN_180fa57d0', 'tier') for n in (10,20,30,40,50,60)],
 1922: [('started', 's4', 'SETPRO12', 'FUN_180f6fcd0', DEL)],
 1990: [('started1', 'started2', 'SETPRO2', 'FUN_180caf150（通用口）', DEL),
        ('started2-complete', 'started3', 'SETPRO3', 'FUN_180fc9fb0', DEL)],
 2001: [('started', 'v1', 'SETPRO1', 'FUN_180f74830', DEL), ('v2', 'k1', 'SETPRO3', 'FUN_180fa2d70', DEL)],
 2004: [('started', 'v1', 'SETPRO1', 'FUN_180fa4310', DEL), ('v2', 'k1', 'SETPRO3', 'FUN_180fc0f00', DEL)],
 2006: [('started', 'v1', 'SETPRO1', 'FUN_180f81d30', DEL)],
 2009: [('s2', f'reward{n}', 'SETPRO3', 'FUN_180fa2900', 'tier') for n in (10,20,30,40,50,60)],
 2114: [('unaccepted', 'active', 'SETPRO1', 'FUN_180f40160（真端页 0x548）', SEL2),
        ('unaccepted', 'active', 'SETPRO2', 'FUN_180f40160（真端页 0x69d）', SEL3)],
 2122: [('started', 'v1', 'SETPRO1', 'FUN_180f75120', DEL)],
 2221: [('started', 'step1', 'SETPRO1', 'FUN_180caf150（通用口）', DEL)],
 2223: [('started', 'step1', 'SETPRO1', 'FUN_180f80030', DEL)],
 2900: [('started0', 'started1', 'SETPRO1', 'FUN_180f7a340', DEL), ('started1', 'started2', 'SETPRO2', 'FUN_180f9a830', DEL),
        ('started2', 'started3', 'SETPRO3', 'FUN_180fabcb0', DEL), ('started3', 'started4', 'SETPRO4', 'FUN_180fb3d40', DEL),
        ('started10', 'reward', 'SETPRO10', 'FUN_180f8d630', DEL)],
 2990: [('started1', 'started2', 'SETPRO2', 'FUN_180caf150（通用口）', DEL), ('started2-complete', 'started3', 'SETPRO3', 'FUN_180fca140', DEL)],
 3090: [('started', 's1', 'SETPRO1', 'FUN_180fe3940', DEL)],
 3721: [('started', 's1', 'SETPRO1', 'FUN_180caf150（通用口）', DEL)],
 3940: [('hunt-done', 's3', 'SETPRO3', 'FUN_180fca740', DEL)],
 4914: [('started', 'k1', 'SETPRO1', 'FUN_180caf150（通用口）', DEL)],
 4944: [('k300', 's3', 'SETPRO3', 'FUN_180fca3e0', DEL)],
 14054: [('s4', 'reward', 'SETPRO6', 'FUN_180fce410/FUN_180fce860', DEL)],
 21114: [('s1', 's2', 'SETPRO2', 'FUN_180f9a4f0（发物+通用口）', DEL), ('s3', 's4', 'SETPRO4', 'FUN_180caf150（通用口）', DEL)],
}
PAGE_LINE = '<dialog type="SHOW_SELECTION_PAGE" page="SELECT_QUEST"/>'
total = 0
for qid, rows in ROWS.items():
    path = f'{BASE}/{qid}.xml'
    text = open(path, encoding='utf-8').read()
    for src, dst, act, fun, kind in rows:
        # 定位该 source->target 转换块
        m = None
        for tm in re.finditer(r'( *)<transition source="' + re.escape(src) + r'" target="' + re.escape(dst) + r'"[^>]*>(.*?)</transition>', text, re.S):
            if f'action="{act}"' in tm.group(2):
                m = tm; break
        assert m, f'{qid} {src}->{dst} {act} not found'
        block = m.group(0)
        assert PAGE_LINE in block, f'{qid} {src}->{dst} {act} has no page line'
        if kind in (DEL, 'tier'):
            if kind == 'tier':
                note = (f'<!-- 真端取证（ScriptDLL64 {fun}）：{act} → 0x100(quest,10/20/30/40/50/60) 分档收尾，'
                        f'零发页；页 10 系旧翻译夸大，已删（2026-10-05 同类排查）。 -->')
            else:
                note = (f'<!-- 真端取证（ScriptDLL64 {fun}）：{act} → 仅推进+刷新，零发页；'
                        f'页 10 系旧翻译夸大，已删（2026-10-05 同类排查）。 -->')
            new_block = '    ' + note + '\n' + re.sub(r'[ \t]*' + re.escape(PAGE_LINE) + r'[ \t]*\n', '', block)
        else:
            page = 'SELECT2' if kind == SEL2 else 'SELECT3'
            note = f'<!-- 真端取证（ScriptDLL64 {fun}）：推进后真端发页 {page}（HTML 族页），非页 10——已按真端替换。 -->'
            new_block = '    ' + note + '\n' + block.replace(PAGE_LINE, f'<dialog type="SHOW_QUEST_PAGE" page="{page}"/>')
        text = text.replace(block, new_block, 1)
        total += 1
    open(path, 'w', encoding='utf-8').write(text)
    print(f'{qid}.xml: {len(rows)} rows')
print('total rows fixed:', total)

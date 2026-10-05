#!/usr/bin/env python3
"""推进类行补关窗（v2，保守缩进处理）：真端 0xf0/0x100+0x5d8＝关窗——sync → sync+close-dialog。"""
import re

BASE = 'src/main/resources/aion/data/static_data/quest/definitions/quests'
ROWS = {
 1002: [('s0','s1','SETPRO1','FUN_180f734b0'),('s1','s2','SETPRO2','FUN_180f9f410'),('s5','s6','SETPRO3','FUN_180fbeaf0'),
        ('s12','s13','SETPRO4','FUN_180f8f690'),('s14','reward','SETPRO6','FUN_180f90280'),('s6','s6','FINISH_DIALOG','FUN_180fa5580 等三处')],
 1007: [('s2',f'reward{n}','SETPRO3','FUN_180fa57d0','tier') for n in (10,20,30,40,50,60)],
 1922: [('started','s4','SETPRO12','FUN_180f6fcd0')],
 1990: [('started1','started2','SETPRO2','FUN_180caf150（通用口）'),('started2-complete','started3','SETPRO3','FUN_180fc9fb0')],
 2001: [('started','v1','SETPRO1','FUN_180f74830'),('v2','k1','SETPRO3','FUN_180fa2d70')],
 2004: [('started','v1','SETPRO1','FUN_180fa4310'),('v2','k1','SETPRO3','FUN_180fc0f00')],
 2006: [('started','v1','SETPRO1','FUN_180f81d30')],
 2009: [('s2',f'reward{n}','SETPRO3','FUN_180fa2900','tier') for n in (10,20,30,40,50,60)],
 2122: [('started','v1','SETPRO1','FUN_180f75120')],
 2221: [('started','step1','SETPRO1','FUN_180caf150（通用口）')],
 2223: [('started','step1','SETPRO1','FUN_180f80030')],
 2900: [('started0','started1','SETPRO1','FUN_180f7a340'),('started1','started2','SETPRO2','FUN_180f9a830'),
        ('started2','started3','SETPRO3','FUN_180fabcb0'),('started3','started4','SETPRO4','FUN_180fb3d40'),
        ('started10','reward','SETPRO10','FUN_180f8d630')],
 2990: [('started1','started2','SETPRO2','FUN_180caf150（通用口）'),('started2-complete','started3','SETPRO3','FUN_180fca140')],
 3090: [('started','s1','SETPRO1','FUN_180fe3940')],
 3721: [('started','s1','SETPRO1','FUN_180caf150（通用口）')],
 3940: [('hunt-done','s3','SETPRO3','FUN_180fca740')],
 4914: [('started','k1','SETPRO1','FUN_180caf150（通用口）')],
 4944: [('k300','s3','SETPRO3','FUN_180fca3e0')],
 14054: [('s4','reward','SETPRO6','FUN_180fce410/FUN_180fce860')],
 21114: [('s1','s2','SETPRO2','FUN_180f9a4f0（发物+通用口）'),('s3','s4','SETPRO4','FUN_180caf150（通用口）')],
}
total = 0
for qid, rows in ROWS.items():
    path = f'{BASE}/{qid}.xml'
    text = open(path, encoding='utf-8').read()
    for row in rows:
        src, dst, act, fun = row[0], row[1], row[2], row[3]
        tier = len(row) > 4 and row[4] == 'tier'
        tm = None
        for m in re.finditer(r'(?P<ind>[ \t]*)<transition source="' + re.escape(src) + r'" target="' + re.escape(dst) + r'"[^>]*>(?P<body>.*?)</transition>', text, re.S):
            if f'action="{act}"' in m.group('body'):
                tm = m; break
        assert tm, f'{qid} {src}->{dst} {act} not found'
        ind = tm.group('ind')
        body = tm.group('body')
        assert '<close-dialog/>' not in body, f'{qid} {src}->{dst} already close'
        sm = re.search(r'(?P<sind>[ \t]*)<sync-quest-state [^/]*/>\n', body)
        assert sm, f'{qid} {src}->{dst} no sync'
        body_new = body.replace(sm.group(0), sm.group(0) + sm.group('sind') + '<close-dialog/>\n', 1)
        # 旧注释（紧邻上方，可多行）
        pre = text[:tm.start()]
        cstart = pre.rfind('<!--')
        assert cstart != -1 and '真端取证' in pre[cstart:pre.find('-->', cstart)]
        cend = pre.find('-->', cstart) + 3
        assert pre[cend:].strip() == '', f'{qid} {src}->{dst} comment not adjacent'
        lstart = pre.rfind('\n', 0, cstart) + 1
        note = (f'<!-- 真端取证（ScriptDLL64 {fun}）：{act} → 0x100(quest,10/20/30/40/50/60) 分档收尾 + '
                f'0x5d8 关窗（实机 2026-10-05 两次点击实证）；页 10 系翻译夸大，已删。 -->') if tier else \
               (f'<!-- 真端取证（ScriptDLL64 {fun}）：{act} → 推进 + 0x5d8 关窗（实机 2026-10-05 两次点击实证）；'
                f'页 10 系翻译夸大，已删。 -->')
        full_new = tm.group(0).replace(body, body_new, 1)   # 保留缩进与全部属性（priority 等）
        text = pre[:lstart] + ind + note + '\n' + full_new + text[tm.end():]
        total += 1
    open(path, 'w', encoding='utf-8').write(text)
    print(f'{qid}.xml: {len(rows)} rows')
print('total:', total)

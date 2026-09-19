#!/usr/bin/env python3
"""Thin every eager fallback holder whose target class has been retired."""
import json
import re
from pathlib import Path

retired = set()
for r in json.loads(Path('/tmp/retire-all.json').read_text()):
    retired.add(r['cls'])
retired |= set("""InGameShopEn AbyssLandingSpecialService AnnouncementService BGService CuringZoneService DebugService
FindGroupService FlyRingService GameTimeService LandingUpdateService MailService PeriodicSaveService SpringZoneService
TaskManagerFromDB ThievesGuildService VeteranRewardsService WebshopService AionPacketHandlerFactory ChatServer
DataManager EventScheduler IDFactory LoginServer LsPacketHandlerFactory PacketFloodFilter World""".split())


def method_doc(indent, cls):
    return (f"{indent}/**\n"
            f"{indent} * 返回 {cls}：双源兜底已退役，交由 {{@link {cls}#getInstance()}} fail-fast。\n"
            f"{indent} * Returns {cls}: the dual-source fallback is retired; delegates to\n"
            f"{indent} * {cls}.getInstance() and fails fast.\n"
            f"{indent} *\n"
            f"{indent} * @return {cls} 实例 / {cls} instance\n"
            f"{indent} */")


def thin_file(path):
    p = Path(path)
    lines = p.read_text(encoding='utf-8').splitlines()
    thinned = []
    changed = True
    while changed:
        changed = False
        for i, l in enumerate(lines):
            m = re.match(r'\s*private static final class (\w+Fallback) \{', l)
            if not m:
                continue
            holder = m.group(1)
            bal = 0
            end = None
            for k in range(i, min(len(lines), i + 30)):
                bal += lines[k].count('{') - lines[k].count('}')
                if bal == 0:
                    end = k
                    break
            block = '\n'.join(lines[i:end + 1])
            gm = re.search(r'(\w+)\.getInstance\(\)', block)
            if not gm or gm.group(1) not in retired:
                continue
            cls = gm.group(1)
            accessor = next((k for k, al in enumerate(lines)
                             if re.search(rf'\breturn {holder}\.INSTANCE;', al)), None)
            if accessor is None:
                continue
            indent = re.match(r'[ \t]*', lines[accessor]).group(0)
            lines[accessor] = f'{indent}return {cls}.getInstance();'
            d = accessor - 1
            if d >= 0 and lines[d].strip() == '*/':
                s = d
                while lines[s].strip() != '/**':
                    s -= 1
                lines[s:d + 1] = method_doc(indent, cls).splitlines()
            hs = i
            while hs - 1 >= 0 and (lines[hs - 1].strip() == '' or
                                   lines[hs - 1].lstrip().startswith(('/**', '*', '*/'))):
                hs -= 1
            del lines[hs:end + 1]
            thinned.append(cls)
            changed = True
            break
    p.write_text('\n'.join(lines).rstrip('\n') + '\n', encoding='utf-8')
    return thinned


if __name__ == '__main__':
    base = Path('src/main/java/com/aionemu/gameserver/lifecycle')
    total = []
    for f in sorted(base.glob('*Fallbacks.java')):
        got = thin_file(f)
        if got:
            print(f'{f.name}: {got}')
            total += got
    print('thinned entries:', len(total))

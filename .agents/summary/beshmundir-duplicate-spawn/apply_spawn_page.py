#!/usr/bin/env python3
"""给 spawn 文件中指定 npc_id 的 <spawn> 元素补 spawn_page 属性（幂等）。

Adds a spawn_page attribute to <spawn> elements whose npc_id is listed, so the
default instance page 0 skips these retail page-1 twins (SpawnEngine.matchesInstance).
Already-tagged elements are left untouched.

用法 / Usage:
    python3 apply_spawn_page.py <spawn file> <page> <id,id,id,...>
"""

import re
import sys

SPAWN_TAG_RE = re.compile(r'^(?P<indent>\s*)<spawn\s+npc_id="(?P<npc_id>\d+)"(?P<rest>[^>]*)>\s*$')


def main(path, page, ids_csv):
    wanted = set(ids_csv.split(","))
    changed = []
    lines = []
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            match = SPAWN_TAG_RE.match(line)
            if match and match.group("npc_id") in wanted and "spawn_page" not in match.group("rest"):
                rest = match.group("rest")
                line = f'{match.group("indent")}<spawn npc_id="{match.group("npc_id")}"{rest} spawn_page="{page}">\n'
                changed.append(match.group("npc_id"))
            lines.append(line)
    with open(path, "w", encoding="utf-8") as handle:
        handle.writelines(lines)
    print(f"tagged {len(changed)} spawn elements: {' '.join(changed)}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])

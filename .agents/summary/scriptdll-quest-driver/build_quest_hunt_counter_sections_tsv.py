#!/usr/bin/env python3
"""混合链 hunt 段的客户端计数 SECTION 登记（quest_monster.csv Progress 门逐段解析）。

客户端硬合同：SECTION_n = 打包 quest_vars int 的 6 位位段 [6n, 6n+6)。quest_monster.csv 的
Progress 门给出每一 hunt 行的 SECTION_0（行阶梯目标值）与计数 SECTION（SECTION_n<count）。
本表逐 quest 按行号排序输出各 hunt 段的计数 SECTION 序号（1 基段序 = DD 表 hunt 步序）。

行格式：quest_id \t sec1,sec2,...（按段序；如 15306 = 五段共享 `1`，18990 = 逐段 `1,2`）

用法：python3 -B build_quest_hunt_counter_sections.tsv.py <quest_ids...>
"""
import os
import csv
import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

CLIENT_CSV = Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/Quest_unpacked/quest_monster.csv")
OUT = Path(f"{REPO}/src/main/resources/aion/data/static_data/quest_retail/quest_client_hunt_counter_sections.tsv")

GATE = re.compile(r'SECTION_(\d+)<(\d+)')


def main():
    wanted = {int(a) for a in sys.argv[1:]}
    per_quest = {}
    with CLIENT_CSV.open(encoding='utf-8-sig') as f:
        for r in csv.reader(f):
            if not r or not r[0].strip().isdigit():
                continue
            quest_id = int(r[0].strip())
            if wanted and quest_id not in wanted:
                continue
            gate = r[1]
            m = GATE.search(gate)
            if not m:
                continue
            section = int(m.group(1))
            per_quest.setdefault(quest_id, []).append((len(per_quest.get(quest_id, [])), section))
    lines = [
        '# 混合链 hunt 段计数 SECTION 登记（quest_id → 各段计数 SECTION 序号，段序 = DD hunt 步序）',
        '# 来源：quest_monster.csv Progress 门的 SECTION_n<count 逐段解析；SECTION_n = 位段 [6n, 6n+6)',
        '# 生成：python3 -B .agents/summary/scriptdll-quest-driver/build_quest_hunt_counter_sections.tsv.py <ids...>',
    ]
    for quest_id in sorted(per_quest):
        sections = [str(section) for _, section in sorted(per_quest[quest_id])]
        lines.append(f'{quest_id}\t{",".join(sections)}')
    OUT.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(f'{OUT.name}: {len(per_quest)} quests')
    for line in lines[3:]:
        print(' ', line)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

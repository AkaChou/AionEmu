#!/usr/bin/env python3
"""P4e-1 冻结事实复现探针（只读）：证明 20 行冻结事实 + 仓内源足以重放退役前的手工表。

三条断言：
  ① Python 分析生成器（TALK 输入接回退役快照）输出 − WITHHELD(20035,20501) ==
     `git show HEAD:<手工表>` 逐字节相同（冻结事实的“排除面”与“保留面”正确）；
  ② 构建期 Java 生成物（target/generated-resources/...，存在时）与手工表**只差第 4 行生成器署名**
     （数据行 86/86 逐字节相同）；
  ③ 冻结事实文件的三类行数与实测一致（exclude 7 / cutscene 11 / withheld 2）。

依赖（分析面，只读）：外部客户端解包目录 /Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs
与退役快照 retired-tsv/quest_client_talk_pages.tsv.retired-20260927。

用法：python3 -B .agents/summary/quest-native-dispatch/tools/p4e_talk_collect_repro_probe.py
"""
from __future__ import annotations

import pathlib
import re
import subprocess
import sys
import tempfile

REPO = pathlib.Path(__file__).resolve().parents[4]
GENERATOR = REPO / '.agents/summary/scriptdll-quest-driver/build_quest_client_talk_collect_chain_pages.py'
RETIRED_TALK = REPO / '.agents/summary/quest-native-dispatch/retired-tsv/quest_client_talk_pages.tsv.retired-20260927'
FACTS = REPO / 'src/main/resources/aion/definitions/quest_dialog/talk_collect_frozen_facts.csv'
MANUAL_TABLE_REL = 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_collect_chain_pages.tsv'
GENERATED_TABLE = (REPO / 'target/generated-resources/aion/data/static_data/quest_retail'
                   / pathlib.Path(MANUAL_TABLE_REL).name)
DIALOGS = pathlib.Path('/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs')
WITHHELD = {'20035', '20501'}
EXPECTED_KINDS = {'exclude': 7, 'cutscene': 11, 'withheld': 2}


def manual_table() -> list[str]:
    """退役前的手工表（工作树已删，取 HEAD 版本）。 / The pre-retirement manual table."""
    return subprocess.run(['git', 'show', f'HEAD:{MANUAL_TABLE_REL}'], cwd=REPO,
                          capture_output=True, text=True, check=True).stdout.splitlines()


def main() -> int:
    failures = []
    for path in (GENERATOR, RETIRED_TALK, FACTS):
        if not path.is_file():
            print(f'FAIL missing input: {path}')
            return 2
    if not DIALOGS.is_dir():
        print(f'FAIL missing external client evidence: {DIALOGS}')
        return 2

    kinds = {}
    for line in FACTS.read_text(encoding='utf-8').splitlines():
        if line.strip() and not line.startswith('#'):
            kinds[line.split(',')[0]] = kinds.get(line.split(',')[0], 0) + 1
    print(f'frozen facts kinds={kinds}')
    if kinds != EXPECTED_KINDS:
        failures.append(f'③ 冻结事实行数漂移: {kinds} != {EXPECTED_KINDS}')

    source = GENERATOR.read_text(encoding='utf-8')
    patched = re.sub(
        r"TALK = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_pages\.tsv'",
        f"TALK = Path({str(RETIRED_TALK)!r})", source)
    if patched == source:
        print('FAIL could not patch the TALK input line (generator drifts)')
        return 2
    with tempfile.TemporaryDirectory() as tmp:
        probe = pathlib.Path(tmp) / 'gen.py'
        out = pathlib.Path(tmp) / 'regen.tsv'
        probe.write_text(patched, encoding='utf-8')
        result = subprocess.run([sys.executable, '-B', str(probe), f'--out={out}'],
                                capture_output=True, text=True)
        print(result.stdout.splitlines()[0] if result.stdout else '')
        if result.returncode != 0:
            print(result.stderr[-2000:])
            return 2
        regen = out.read_text(encoding='utf-8').splitlines()

    manual = manual_table()
    filtered = [l for l in regen if l.startswith('#') or l.split('\t')[0] not in WITHHELD]
    ok1 = filtered == manual
    print(f'① python regen {len(regen)} 行 − WITHHELD = {len(filtered)} 行 vs 手工表 {len(manual)} 行: '
          f'{"逐字节相同" if ok1 else "不一致"}')
    if not ok1:
        failures.append('① 冻结事实无法重放手工表')

    if GENERATED_TABLE.is_file():
        generated = GENERATED_TABLE.read_text(encoding='utf-8').splitlines()
        diff = [i for i, (a, b) in enumerate(zip(generated, manual), 1) if a != b]
        data_same = generated[4:] == manual[4:] and len(generated) == len(manual)
        print(f'② 构建期生成物 {len(generated)} 行 vs 手工表: 差异行={diff}（应为 [4]），数据行相同={data_same}')
        if diff != [4] or not data_same:
            failures.append('② 构建期生成物与手工表差异超出第 4 行署名')
    else:
        print('② 跳过：尚未构建 target/generated-resources（先跑 mvn generate-resources）')

    for message in failures:
        print('FAIL ' + message)
    print('PASS: P4e-1 冻结事实与构建期生成物均与退役前手工表一致' if not failures else 'PROBE FAILED')
    return 0 if not failures else 1


if __name__ == '__main__':
    raise SystemExit(main())

#!/usr/bin/env python3
"""1929.xml：翻转 after-commit 里 `sync-quest-state` 与 `close-dialog` 的先后顺序。

Flip the relative order of ``sync-quest-state`` and ``close-dialog`` inside after-commit blocks of
1929.xml.

方向 / direction:
  close-first（默认）  ``sync-quest-state`` → ``close-dialog``  改成  ``close-dialog`` → ``sync-quest-state``
  sync-first           ``close-dialog`` → ``sync-quest-state``  改回  ``sync-quest-state`` → ``close-dialog``

背景：第 5 轮曾假设「客户端收到关窗包会重置烙印窗口渲染，槽位必须是最后一个到达的协议事实」，
把 36 处顺序翻转成 close-first；第 6 轮实机复测证伪了该假设（关窗在前/在后结果一致，凹槽仍被关闭），
真正的原因是客户端对关窗包的槽位缓存重置晚于同一批包生效，修复改为「打开烙印窗口前重发槽位数」
（PlayerQuestDialogPort/DialogService）。因此此处用 sync-first 把 XML 还原成仓库既有约定顺序
（sync → close，QE-143）以缩小差异。

第 5 轮 close-first 假设与本轮结论均见 DIAGNOSIS.zh-CN.md。
在仓库根目录运行：
  python3 .agents/summary/quest-1929-stigma-dialog/reorder_close_before_sync.py [close-first|sync-first]
"""
from pathlib import Path
import re
import sys

REPO = Path(__file__).resolve().parents[3]
XML = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests/1929.xml"

CLOSE_FIRST = "close-first"
SYNC_FIRST = "sync-first"

direction = sys.argv[1] if len(sys.argv) > 1 else CLOSE_FIRST
if direction not in (CLOSE_FIRST, SYNC_FIRST):
    print(f"unknown direction {direction!r}; expected {CLOSE_FIRST} or {SYNC_FIRST}")
    sys.exit(2)

first, second = (("sync-quest-state", "close-dialog") if direction == CLOSE_FIRST
                 else ("close-dialog", "sync-quest-state"))

text = XML.read_text(encoding="utf-8")
SYNC = r'<sync-quest-state(?: mode="(?P<mode>[A-Z_]+)")?/>'
CLOSE = r"<close-dialog/>"
pattern = re.compile(
    rf"(?P<i1>[ \t]*){SYNC if first == 'sync-quest-state' else CLOSE}\n"
    rf"(?P<i2>[ \t]*){SYNC if second == 'sync-quest-state' else CLOSE}"
)


def swap(match: "re.Match[str]") -> str:
    mode = f" mode=\"{match.group('mode')}\"" if match.group("mode") else ""
    first_tag = f"<{first}{mode}/>" if first == "sync-quest-state" else f"<{first}/>"
    second_tag = f"<{second}{mode}/>" if second == "sync-quest-state" else f"<{second}/>"
    return f"{match.group('i1')}{second_tag}\n{match.group('i2')}{first_tag}"


new_text, count = pattern.subn(swap, text)
if count == 0:
    print(f"no {first} -> {second} pairs found; nothing to do")
    sys.exit(1)

XML.write_text(new_text, encoding="utf-8")
print(f"reordered {count} after-commit pairs ({first} -> {second}) in {XML.relative_to(REPO)}")

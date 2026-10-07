#!/usr/bin/env python3
"""1929.xml：把 22 条发放分支（spawned98 → granted95）的 after-commit 从
「sync-quest-state + close-dialog」改为「sync-quest-state + show-dialog-window(dialog-id=1)」。

原因（第 6/7/8 轮实机证据链）：客户端在收到服务端关窗包时会**不可恢复地**清掉烙印槽位展开态——
之后的每一次槽位数重发（SM_CUBE_UPDATE）都救不回来；发放分支是拓印教学里唯一带关窗的交互，
所以只能不关窗：改为直接打开烙印窗口（真端/退役 XML 的发放分支本来就是
`give-item` + `show-dialog-window(1)`，开窗前会先登记对话对象）。

打开窗口需要对话对象 ID：该按钮（SELECT5_2）在客户端不带对象 ID（快照 targetless），
故由 `PlayerQuestDialogPort.resolveObjectId` 回落到玩家正在进行的任务对话授权。

第 5/6 轮顺序假设与第 7/8 轮结论见 DIAGNOSIS.zh-CN.md。在仓库根目录运行：
  python3 .agents/summary/quest-1929-stigma-dialog/rewrite_grant_after_commit.py
"""
from pathlib import Path
import re
import sys

REPO = Path(__file__).resolve().parents[3]
XML = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests/1929.xml"

OLD = """      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>"""
NEW = """      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <show-dialog-window dialog-id="1"/>
      </after-commit>"""

text = XML.read_text(encoding="utf-8")
pattern = re.compile(
    r'(<transition source="spawned98" target="granted95">.*?)' + re.escape(OLD) + r'(.*?</transition>)',
    re.S,
)
new_text, count = pattern.subn(lambda m: m.group(1) + NEW + m.group(2), text)
if count == 0:
    print("no grant transitions matched; nothing to do")
    sys.exit(1)

XML.write_text(new_text, encoding="utf-8")
print(f"rewrote {count} grant after-commit blocks in {XML.relative_to(REPO)}")

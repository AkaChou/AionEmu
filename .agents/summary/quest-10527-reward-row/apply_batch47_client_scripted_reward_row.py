#!/usr/bin/env python3
"""批次 47：1123（Where's Tutty?）领奖态投影回到 `REWARD/var0=0`（用户真机判定）。

用户判定（2026-09-22 真机，本任务权威验收口径）：
- “看完影片，状态应该是 reward 0”。
- 现场截图证据：本批前（批次 15 把 reward 投影改成 1 之后）影片结束任务说明**整块空白**；
  仓库同型先例是批次 29 的 1466（“legacy 落盘 2，越出 0..1，任务书会空白”，修法是归一化到范围内）。

根因（客户端脚本驱动的行索引）：
- 1123 的客户端脚本是 `Quest_unpacked/quest_script_monster.csv` 的
  `1123,ProgressAll,,sensoryArea,,1,LF1_SensoryArea_Q88`：客户端自己累计感应区进度，
  任务说明行 = **客户端自身进度 + 服务端 SECTION_0**；把服务端 SECTION_0 从 0 抬到 1 会让
  合计越出任务书声明的两行（槽位 `[%0]`/`[%3]`），于是两条都不亮、任务说明空白。
- 同型旁证：全库另外两个 `ProgressAll + sensoryArea` 任务 `50008/51008`（同样两行、槽位 0/3）
  在审计脚本里早已登记为「var0 不是任务书行号」；同族的 1122/1124/30507 没有客户端脚本行
  （纯服务端 var0 驱动），所以批次 15 的“最后一行 = 1”只适用于它们，不适用于 1123。
- 迁移前 Java handler `_1123Wheres_Tutty#onEnterZoneEvent` 也是 `playQuestMovie(env, 11)` +
  `setStatus(REWARD)`（**不写 var0**，落盘 0），与本批目标一致。

本批改动（4 处，全部断言式替换）：
1. `reward` 节点投影 `var0 1 -> 0`；
2. enter-zone 事务恢复“播片 + 落 REWARD/0 + 刷新”（与 legacy 同序），保证影片结束事件丢失时也能推进；
3. 新增 `reward -> reward` 的 `<movie-end movie-id="11"/>` 重同步（过场结束后再刷一次任务书，
   对应批次 46 的刷新时机收口）；
4. 自愈边由 `REWARD/var0=0 -> 1` 改成 `REWARD/var0=1 -> 0`（把批次 15 期间已落盘的错值拉回 0）。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch47_client_scripted_reward_row.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch47_client_scripted_reward_row.py --apply
"""

from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_ID = 1123
ZONE = "LF1_SENSORY_AREA_Q1123_210010000"
MOVIE_ID = 11

REPLACEMENTS: list[tuple[str, str]] = [
	# 1) reward 节点投影回到 0
	(
		"""    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
""",
		"""    <node label="reward" status="REWARD">
      <var name="var0" value="0"/>
    </node>
""",
	),
	# 2) + 3) enter-zone 播片并落 REWARD/0；影片结束再重同步一次
	(
		"""    <!-- QE-024：进入感应区只播片（自环），行推进交给客户端影片结束回调——过场遮罩期间下发状态
         刷新会被客户端丢弃，任务书停在行 0（用户 2026-09-22 真机反馈“看完剧情后没有推进到下一步”）。
         同族 1336（ProgressAll + sensoryArea ×12）就是 enter-zone 播片自环 + movie-end 落行的写法。
         QE-024: the sensory zone only plays the movie; the journal row lands on the client's movie-end
         callback (CM_PLAY_MOVIE_END), matching the sibling family 1336. -->
    <transition source="started" target="started">
      <event>
        <enter-zone zone="LF1_SENSORY_AREA_Q1123_210010000"/>
      </event>
      <after-commit>
        <play-movie movie-id="11"/>
      </after-commit>
    </transition>
    <transition source="started" target="reward">
      <event>
        <movie-end movie-id="11"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
""",
		"""    <!-- 客户端脚本驱动的任务书行（ProgressAll + sensoryArea）：1123 的客户端会自己累计感应区进度，
         任务说明行 = 客户端进度 + 服务端 SECTION_0，所以服务端这里必须保持 legacy 的 `REWARD/var0=0`
         （用户 2026-09-22 真机判定：“看完影片，状态应该是 reward 0”）；抬到 1 会让任务说明整块空白。
         进入感应区：播片 11 + 落 REWARD/var0=0 + 刷新（与 legacy `playQuestMovie` + `setStatus(REWARD)` 同序）。
         Client-scripted journal (ProgressAll + sensoryArea): the client adds its own sensory progress to
         SECTION_0, so the server keeps the legacy REWARD/var0=0; raising it to 1 blanks the journal. -->
    <transition source="started" target="reward">
      <event>
        <enter-zone zone="LF1_SENSORY_AREA_Q1123_210010000"/>
      </event>
      <after-commit>
        <play-movie movie-id="11"/>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <!-- 过场结束后再刷一次任务书（客户端 CM_PLAY_MOVIE_END 回调）。
         Re-sync the journal on the client movie-end callback. -->
    <transition source="reward" target="reward">
      <event>
        <movie-end movie-id="11"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
""",
	),
	# 4) 自愈边反向：把批次 15 期间落盘的 REWARD/1 拉回 0
	(
		"""    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
""",
		"""    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="1"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="0"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
""",
	),
]


def tidy(text: str) -> str:
	return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def apply_to(text: str) -> str:
	current = tidy(text)
	for index, (old, new) in enumerate(REPLACEMENTS, start=1):
		count = current.count(old)
		if count > 1:
			raise SystemExit(f"BATCH47_ERROR {QUEST_ID}: replacement {index} matched {count} times")
		if count == 1:
			current = current.replace(old, new, 1)
			continue
		if new.strip() and new.strip() in current:
			continue  # 已应用
		raise SystemExit(f"BATCH47_ERROR {QUEST_ID}: replacement {index} found neither source nor target")
	return current


def main(argv: list[str]) -> int:
	apply = "--apply" in argv
	check = "--check" in argv or not apply
	path = QUESTS / f"{QUEST_ID}.xml"
	current = tidy(path.read_text(encoding="utf-8"))
	target = apply_to(current)
	if check:
		if current == target:
			print(f"BATCH47_OK {QUEST_ID} already-applied")
			return 0
		print(f"BATCH47_PENDING {QUEST_ID} differs from target document")
		return 1
	if current == target:
		print(f"BATCH47_OK {QUEST_ID} already-applied")
		return 0
	path.write_text(target, encoding="utf-8")
	print(f"BATCH47_APPLIED {QUEST_ID}")
	return 0


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))

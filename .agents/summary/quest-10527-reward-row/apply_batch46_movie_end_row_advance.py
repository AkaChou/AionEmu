#!/usr/bin/env python3
"""批次 46：1123（Where's Tutty?）感应区影片结束回调再落行 1（QE-024 + QE-051 客户端验收回归）。

背景（用户 2026-09-22 真机 + QUEST-TRACE）：
- 现场 trace 证明**服务端已经正确推进**：接取 `SM_QUEST_ACTION 状态=3 步数=0`，
  进入 `LF1_SENSORY_AREA_Q1123_210010000` 后同一 tick 下发 `状态=4 步数=1`（REWARD/行 1）。
- 但用户验收反馈“看完剧情后没有推进到下一步”：影片仍在客户端播放（过场遮罩）期间下发的状态刷新
  被客户端丢弃，任务书停留在行 0；本任务客户端脚本是
  `Quest_unpacked/quest_script_monster.csv` 的 `1123,ProgressAll,,sensoryArea,,1,LF1_SensoryArea_Q88`
  —— 与同族 `1336`（12 个感应区影片，全部 `ProgressAll sensoryArea`）同型。
- 同族权威范式（1336，`quest_definition/quests/1336.xml`）：进入感应区的 transition 只做
  `<play-movie>` 自环，行推进由客户端影片结束回调
  （`CM_PLAY_MOVIE_END` -> `QuestEvent.MovieEnd`）在同一 tick 之后的 `<movie-end>` transition 里
  以 `sync-quest-state` 落盘；QE-024 也正是这条“影片推进必须实现明确状态迁移、不得留纯电影自环”的边界。
- 本批把 1123 的“enter-zone 内同时播片 + 落 REWARD”拆成两步：
  ① `started` 上 enter-zone 自环，仅 `<play-movie movie-id="11"/>`；
  ② `started -> reward` 用 `<movie-end movie-id="11"/>` 落 REWARD/var0=1 并
     `LEVEL_AND_VISIBILITY_REFRESH`（影片结束后才刷新任务书）。
  reward 投影、自愈边、领奖页与 `npc-complete` 合同全部保持不变；若客户端未回影片结束事件，
  玩家重新进入同一感应区即可重放影片并再次触发（自环不消费状态，无死锁）。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch46_movie_end_row_advance.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch46_movie_end_row_advance.py --apply
"""

from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_ID = 1123
ZONE = "LF1_SENSORY_AREA_Q1123_210010000"
MOVIE_ID = 11

OLD = f"""    <transition source="started" target="reward">
      <event>
        <enter-zone zone="{ZONE}"/>
      </event>
      <after-commit>
        <play-movie movie-id="{MOVIE_ID}"/>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
"""

NEW = f"""    <!-- QE-024：进入感应区只播片（自环），行推进交给客户端影片结束回调——过场遮罩期间下发状态
         刷新会被客户端丢弃，任务书停在行 0（用户 2026-09-22 真机反馈“看完剧情后没有推进到下一步”）。
         同族 1336（ProgressAll + sensoryArea ×12）就是 enter-zone 播片自环 + movie-end 落行的写法。
         QE-024: the sensory zone only plays the movie; the journal row lands on the client's movie-end
         callback (CM_PLAY_MOVIE_END), matching the sibling family 1336. -->
    <transition source="started" target="started">
      <event>
        <enter-zone zone="{ZONE}"/>
      </event>
      <after-commit>
        <play-movie movie-id="{MOVIE_ID}"/>
      </after-commit>
    </transition>
    <transition source="started" target="reward">
      <event>
        <movie-end movie-id="{MOVIE_ID}"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
"""


def tidy(text: str) -> str:
	return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def main(argv: list[str]) -> int:
	apply = "--apply" in argv
	check = "--check" in argv or not apply
	path = QUESTS / f"{QUEST_ID}.xml"
	current = tidy(path.read_text(encoding="utf-8"))
	if OLD not in current:
		if NEW in current:
			print(f"BATCH46_OK {QUEST_ID} already-applied")
			return 0
		raise SystemExit(f"BATCH46_ERROR {QUEST_ID}: neither the pre-batch nor the target block was found")
	target = current.replace(OLD, NEW, 1)
	if check:
		print(f"BATCH46_PENDING {QUEST_ID} differs from target document")
		return 1
	path.write_text(target, encoding="utf-8")
	print(f"BATCH46_APPLIED {QUEST_ID}")
	return 0


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))

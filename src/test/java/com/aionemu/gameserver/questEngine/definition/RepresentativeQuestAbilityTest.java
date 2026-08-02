package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.cancelQuestTimer;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.broadcastNpcEmotion;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.movieEnd;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.playMovie;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.spawnNpcAtPlayer;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.startFollow;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.startQuestTimer;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.startWalking;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.teleportPlayer;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.useItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.watchFollowZone;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 真实代表任务逐能力纵向验证 (D-0024 / D-0025 路线)。
 *
 * 每个能力从真实 handler 源码提取固定可投影事实 (字面量), 经 DSL 编译为 IR,
 * 断言 after-commit action 精确忠实投影。运行时来源的值 (如玩家坐标) 用示例值
 * 演示并标注, 不作为静态事实伪造。
 *
 * 代表任务:
 *   2533  Beritras Curse        → 定时器   (questTimerStart 300 / questTimerEnd)
 *   2333  A Ribbit Out Of Water → 刷怪+AI  (spawn 204416 + startWalking + FOLLOW_ME)
 *   24154 Better Than Last Time → 影片+传送 (playQuestMovie 249/250, MovieEnd→teleport)
 */
class RepresentativeQuestAbilityTest {
	private static final EvidenceRef TIMER_EVIDENCE = new EvidenceRef("TEMPLATE_HANDLER",
		"src/main/java/com/aionemu/gameserver/quest/handlers/beluslan/_2533BeritrasCurse.java:43,82",
		"questTimerStart(env, 300) 在物品使用+区域内启动; SELECT_REWARD 分支 questTimerEnd 取消");
	private static final EvidenceRef SPAWN_AI_EVIDENCE = new EvidenceRef("TEMPLATE_HANDLER",
		"src/main/java/com/aionemu/gameserver/quest/handlers/morheim/_2333ARibbitOutOfWater.java:76-80",
		"STEP_TO_2 在玩家当前 world/instance/position 刷 204416, startWalking, FOLLOW_ME, START_EMOTE2, 并注册 DF2_ITEMUSEAREA_Q2333 跟随检查");
	private static final EvidenceRef MOVIE_TELEPORT_EVIDENCE = new EvidenceRef("TEMPLATE_HANDLER",
		"src/main/java/com/aionemu/gameserver/quest/handlers/beluslan/_24154Better_Than_Last_Time.java:62,99,125",
		"ACCEPT_QUEST→playQuestMovie(249); USE_OBJECT(var==2)→playQuestMovie(250); MovieEnd(250)→teleportTo(220040000,2452,2471,673,(byte)28)");

	@Test
	void timer2533StartFactsProjectFaithfully() {
		CompiledQuestDefinition def = quest(2533)
			.evidence(TIMER_EVIDENCE)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.on(useItem(182204425)).when(statusIs(QuestStatus.START)).goTo("start")
			.afterCommit(startQuestTimer(300))
			.compile();

		assertEquals(List.of(new AfterCommitAction.StartQuestTimer(300)),
			def.definition().transitions().get(0).afterCommit());
	}

	@Test
	void timer2533CancelFactsProjectFaithfully() {
		CompiledQuestDefinition def = quest(2533)
			.evidence(TIMER_EVIDENCE)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.on(talkToNpc(204801)).when(statusIs(QuestStatus.START)).goTo("start")
			.afterCommit(cancelQuestTimer())
			.compile();

		assertEquals(List.of(new AfterCommitAction.CancelQuestTimer()),
			def.definition().transitions().get(0).afterCommit());
	}

	@Test
	void spawnAndAi2333FactsProjectFaithfully() {
		CompiledQuestDefinition def = quest(2333)
			.evidence(SPAWN_AI_EVIDENCE)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("ready", project(QuestStatus.START, vars("var0", 1)))
			.node("following", project(QuestStatus.START, vars("var0", 2)))
			.on(talkToNpc(798084, com.aionemu.gameserver.questEngine.model.QuestDialog.STEP_TO_2))
			.from("ready").goTo("following")
			.afterCommit(spawnNpcAtPlayer("escort", 204416, (byte) 8))
			.afterCommit(startWalking("escort"))
			.afterCommit(startFollow("escort"))
			.afterCommit(broadcastNpcEmotion("escort", QuestNpcEmotion.START_EMOTE2))
			.afterCommit(watchFollowZone("escort", "DF2_ITEMUSEAREA_Q2333"))
			.compile();

		assertEquals(List.of(
			new AfterCommitAction.SpawnNpc("escort", 204416, new QuestSpawnLocation.PlayerPosition((byte) 8)),
			new AfterCommitAction.StartWalking("escort"),
			new AfterCommitAction.StartFollow("escort"),
			new AfterCommitAction.BroadcastNpcEmotion("escort", QuestNpcEmotion.START_EMOTE2),
			new AfterCommitAction.WatchFollowZone("escort", "DF2_ITEMUSEAREA_Q2333")),
			def.definition().transitions().get(0).afterCommit());
	}

	@Test
	void movieAndTeleport24154FactsProjectFaithfully() {
		// USE_OBJECT only starts movie 250. MovieEnd(250) is the authoritative callback
		// that teleports and advances var0 from 2 to 3.
		CompiledQuestDefinition movieDef = quest(24154)
			.evidence(MOVIE_TELEPORT_EVIDENCE)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("movie-ready", project(QuestStatus.START, vars("var0", 2)))
			.node("teleported", project(QuestStatus.START, vars("var0", 3)))
			.on(talkToNpc(700359, com.aionemu.gameserver.questEngine.model.QuestDialog.USE_OBJECT))
			.from("movie-ready").goTo("movie-ready")
			.afterCommit(playMovie(250))
			.on(movieEnd(250)).from("movie-ready").goTo("teleported")
			.afterCommit(teleportPlayer(220040000, 2452f, 2471f, 673f, (byte) 28))
			.compile();

		assertEquals(List.of(new AfterCommitAction.PlayMovie(250)),
			movieDef.definition().transitions().get(0).afterCommit());
		assertEquals(List.of(
			new AfterCommitAction.TeleportPlayer(220040000, 2452f, 2471f, 673f, (byte) 28)),
			movieDef.definition().transitions().get(1).afterCommit());

		// 接取时播放 249。
		CompiledQuestDefinition acceptDef = quest(24154)
			.evidence(MOVIE_TELEPORT_EVIDENCE)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(talkToNpc(204774, com.aionemu.gameserver.questEngine.model.QuestDialog.ACCEPT_QUEST))
			.from("unaccepted").goTo("started")
			.afterCommit(playMovie(249))
			.compile();

		assertEquals(List.of(new AfterCommitAction.PlayMovie(249)),
			acceptDef.definition().transitions().get(0).afterCommit());
	}
}

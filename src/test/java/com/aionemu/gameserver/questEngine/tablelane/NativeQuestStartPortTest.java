package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * native 接取建档口（计划 §6.2 NativeQuestStatePort）：真端 quest.xml 轴判定 + 建档/复位，
 * 不依赖 typed {@code QuestTemplate}（{@code QuestService.startQuest} 对已切换行必然缺模板）。
 * <p>
 * Native start port gate: retail-axis adjudication and state creation without the typed template.
 */
class NativeQuestStartPortTest {

	/** min 1 / pc_light / max_repeat 1 / 无 bm、无前置。 */
	private static final int PLAIN = 1101;
	/** min 10 / pc_light / max_repeat 100（可重复）。 */
	private static final int REPEATABLE = 1963;
	/** min 11 / 前置 {@code finished_quest_cond1=Q1131}。 */
	private static final int GATED = 1132;
	private static final int GATE_QUEST = 1131;
	/** {@code bm_restrict_category=1}（语义未坐实 ⇒ fail-closed）。 */
	private static final int BM_RESTRICTED = 1329;
	/** {@code minlevel_permitted=999}：真端不可接取行。 */
	private static final int UNREACHABLE = 2732;

	@Test
	void npcAcceptCreatesTheRowAtStart() {
		Player player = player(20);
		NativeQuestStartPort.StartResult result = port().start(player, PLAIN);

		assertTrue(result.started(), () -> "接取必须成立，实际=" + result);
		QuestState state = player.getQuestStateList().getQuestState(PLAIN);
		assertEquals(QuestStatus.START, state.getStatus());
		assertEquals(0, state.getQuestVars().getQuestVars(), "接取复位 raw vars = 0");
	}

	@Test
	void unreachableRowsFailClosedOnTheLevelAxis() {
		Player player = player(65);
		NativeQuestStartPort.StartResult result = port().start(player, UNREACHABLE);

		assertEquals(NativeQuestStartPort.Outcome.LEVEL_BLOCKED, result.outcome(),
				"minlevel=999 是真端不可达值，不是无限制");
		assertNull(player.getQuestStateList().getQuestState(UNREACHABLE), "被拒时不得建档");
	}

	@Test
	void bmRestrictedRowsFailClosedUntilTheAxisIsProven() {
		Player player = player(40);
		NativeQuestStartPort.StartResult result = port().start(player, BM_RESTRICTED);

		assertEquals(NativeQuestStartPort.Outcome.BM_RESTRICT_UNRESOLVED, result.outcome(),
				"bm_restrict_category 位集语义未坐实：必须 fail-closed，禁止放行兜底");
		assertNull(player.getQuestStateList().getQuestState(BM_RESTRICTED));
	}

	@Test
	void prerequisitesGateTheAccept() {
		Player player = player(20);
		assertEquals(NativeQuestStartPort.Outcome.PREREQUISITE_MISSING, port().start(player, GATED).outcome(),
				"前置 Q" + GATE_QUEST + " 未完成时必须拒绝");

		player.getQuestStateList().addQuest(GATE_QUEST,
				new QuestState(GATE_QUEST, QuestStatus.COMPLETE, 0, 1, null, 0, null));
		assertTrue(port().start(player, GATED).started(), "前置完成后可接取");
	}

	@Test
	void repeatableRowsRestartFromTheCompletedState() {
		Player player = player(20);
		player.getQuestStateList().addQuest(REPEATABLE,
				new QuestState(REPEATABLE, QuestStatus.COMPLETE, 0x1f, 3, null, 0, null));

		assertTrue(port().start(player, REPEATABLE).started(), "未达重复上限时可再次接取");
		QuestState state = player.getQuestStateList().getQuestState(REPEATABLE);
		assertEquals(QuestStatus.START, state.getStatus());
		assertEquals(0, state.getQuestVars().getQuestVars(), "重复接取必须复位 raw vars");
		assertEquals(3, state.getCompleteCount(), "重复计数由完成流维护，接取不得改写");

		state.setStatus(QuestStatus.COMPLETE);
		state.setCompleteCount(100);
		assertEquals(NativeQuestStartPort.Outcome.REPEAT_LIMIT, port().start(player, REPEATABLE).outcome(),
				"达到 max_repeat_count 后拒绝");
	}

	@Test
	void nonRepeatableCompletedRowsAreRejected() {
		Player player = player(20);
		player.getQuestStateList().addQuest(PLAIN,
				new QuestState(PLAIN, QuestStatus.COMPLETE, 0, 1, null, 0, null));

		assertEquals(NativeQuestStartPort.Outcome.REPEAT_LIMIT, port().start(player, PLAIN).outcome(),
				"max_repeat_count=1 的行完成后不得再开");
	}

	@Test
	void inProgressRowsAreRejected() {
		Player player = player(20);
		player.getQuestStateList().addQuest(PLAIN,
				new QuestState(PLAIN, QuestStatus.START, 0, 0, null, 0, null));

		assertEquals(NativeQuestStartPort.Outcome.ALREADY_RUNNING, port().start(player, PLAIN).outcome());
	}

	@Test
	void systemGrantWritesTheStateWithoutNpcAxes() {
		Player player = player(20);
		assertTrue(port().grant(player, REPEATABLE).started(), "系统发放只做状态面");
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(REPEATABLE).getStatus());
		assertEquals(NativeQuestStartPort.Outcome.ALREADY_RUNNING,
				port().grant(player, REPEATABLE).outcome(), "进行中不得重复发放");
	}

	private static NativeQuestStartPort port() {
		return NativeQuestStartPort.instance();
	}

	private static Player player(int level) {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData pcd = new PlayerCommonData(10001);
		pcd.setRace(Race.ELYOS);
		pcd.setGender(Gender.MALE);
		// setPlayerClass / setLevel 需要经验表就绪（单测无服务栈）⇒ 直接写字段。
		setField(pcd, PlayerCommonData.class, "playerClass", PlayerClass.WARRIOR);
		setField(pcd, PlayerCommonData.class, "level", level);
		try {
			java.lang.reflect.Field field = Player.class.getDeclaredField("playerCommonData");
			field.setAccessible(true);
			field.set(player, pcd);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		player.setQuestStateList(new QuestStateList());
		return player;
	}

	private static void setField(Object target, Class<?> type, String name, Object value) {
		try {
			java.lang.reflect.Field field = type.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}

package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * native 接取建档口（计划 §6.2 NativeQuestStatePort）：原版 quest.xml 轴判定 + 建档/复位，
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
	/** {@code bm_restrict_category=1}（账号限制位 20 = {@code quest_acquire1}）。 */
	private static final int BM_RESTRICTED = 1329;
	/** {@code minlevel_permitted=999}：原版不可接取行。 */
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
				"minlevel=999 是原版不可达值，不是无限制");
		assertNull(player.getQuestStateList().getQuestState(UNREACHABLE), "被拒时不得建档");
	}

	@Test
	void bmRestrictedRowsFollowTheAccountRestrictionBit() {
		Player player = player(40);
		// 原版判定 = 玩家限制位图第 (类别 + 19) 位：类别 1 ⇒ quest_acquire1(20)。
		// The retail check is bit (category + 19) of the player's restriction bitmap: 1 ⇒ quest_acquire1.
		int[] requestedBit = {-1};
		NativeQuestStartPort restricted = new NativeQuestStartPort(NativeQuestXmlTable.instance(),
			(ignored, bitIndex) -> {
				requestedBit[0] = bitIndex;
				return true;
			});
		assertEquals(NativeQuestStartPort.Outcome.BM_RESTRICT_BLOCKED,
			restricted.start(player, BM_RESTRICTED).outcome(), "限制位命中 ⇒ 拒接");
		assertEquals(NativeQuestStartPort.QUEST_ACQUIRE_FIRST_BIT, requestedBit[0],
			"类别 1 必须查 quest_acquire1 位");
		assertNull(player.getQuestStateList().getQuestState(BM_RESTRICTED), "拒接不得建档");

		// 本服无计费来源 ⇒ 生产位集为空（原版全订阅账号同形）⇒ 该行按原版可接取。
		// No billing source here ⇒ the production bitmap is empty ⇒ the row is acquirable.
		assertTrue(port().start(player, BM_RESTRICTED).started(), "限制位未命中 ⇒ 可接取");
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(BM_RESTRICTED).getStatus());
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

	/** 原版 class_permitted 词表限职业：{@code fighter knight}（1913，min 10 ⇒ 展开 GLADIATOR/TEMPLAR）。 */
	private static final int CLASS_RESTRICTED = 1913;

	@Test
	void classRestrictedRowsFollowTheRetailTokenMapping() {
		// 原版 token（fighter/knight/wizard…）不是 PlayerClass 枚举名；逐字比较会让限职业行永远拒接。
		// Retail tokens (fighter/knight/wizard…) are not PlayerClass names; a literal comparison would
		// reject every class-restricted row forever.
		Player gladiator = player(20, PlayerClass.GLADIATOR);
		// 原版前置 {@code Q1007:1} = 奖励档 1（0 基 0），必须先完成前置任务。
		gladiator.getQuestStateList().addQuest(1007,
			new QuestState(1007, QuestStatus.COMPLETE, 0, 1, null, 0, null));
		assertTrue(port().start(gladiator, CLASS_RESTRICTED).started(), "fighter ⇒ GLADIATOR 必须放行");

		Player templar = player(20, PlayerClass.TEMPLAR);
		templar.getQuestStateList().addQuest(1007,
			new QuestState(1007, QuestStatus.COMPLETE, 0, 1, null, 0, null));
		assertTrue(port().start(templar, CLASS_RESTRICTED).started(), "knight ⇒ TEMPLAR 必须放行");

		Player wrongSlot = player(20, PlayerClass.GLADIATOR);
		wrongSlot.getQuestStateList().addQuest(1007,
			new QuestState(1007, QuestStatus.COMPLETE, 0, 1, null, 2, null));
		assertEquals(NativeQuestStartPort.Outcome.PREREQUISITE_MISSING,
			port().start(wrongSlot, CLASS_RESTRICTED).outcome(),
			"Q1007:1 必须比对奖励档（0 基 0），档位不符即拒绝");

		Player warrior = player(20, PlayerClass.WARRIOR);
		assertEquals(NativeQuestStartPort.Outcome.CLASS_BLOCKED,
				port().start(warrior, CLASS_RESTRICTED).outcome(), "未入词表的职业必须 fail-closed");
		assertNull(warrior.getQuestStateList().getQuestState(CLASS_RESTRICTED), "被拒时不得建档");
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
		return player(level, PlayerClass.WARRIOR);
	}

	/**
	 * unfinished 轴（DD 链式接取面专属，2026-10-08 缺口修复批）：10033 的原版
	 * {@code unfinished_quest_cond = Q10025/Q14062}——引用行"未"COMPLETE 才通过（方向不许写反）；
	 * 共享的 {@link NativeQuestStartPort#start} 判定面刻意不含该轴（其它 native 接取面口径不扩大）。
	 */
	@Test
	void unfinishedConditionsGateOnlyTheChainFace() {
		NativeQuestStartPort port = port();
		Player player = player(52);
		assertTrue(port.unfinishedConditionsPass(player, 10033), "引用行未 COMPLETE ⇒ 通过");

		Player blocked = player(52);
		blocked.getQuestStateList().addQuest(10025,
			new QuestState(10025, QuestStatus.COMPLETE, 0, 0, null, 0, null));
		assertFalse(port.unfinishedConditionsPass(blocked, 10033), "引用行已 COMPLETE ⇒ 不通过");
		// 共享口口径锁定：start() 不判 unfinished（链式面自行判定，见方法注释）。
		// The shared port stays unchanged on purpose: start() does not adjudicate the unfinished axis.
		assertTrue(port.start(blocked, 10033).started(), "start() 判定面不含 unfinished 轴");
	}

	private static Player player(int level, PlayerClass playerClass) {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData pcd = new PlayerCommonData(10001);
		pcd.setRace(Race.ELYOS);
		pcd.setGender(Gender.MALE);
		// setPlayerClass / setLevel 需要经验表就绪（单测无服务栈）⇒ 直接写字段。
		setField(pcd, PlayerCommonData.class, "playerClass", playerClass);
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

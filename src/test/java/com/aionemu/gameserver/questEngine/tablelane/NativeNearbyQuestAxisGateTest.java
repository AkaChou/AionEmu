package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.aionemu.gameserver.controllers.PlayerController;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestStartPort.Outcome;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestStartPort.ZoneVerdict;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMap3DInstance;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * native 行的附近任务提示轴门禁（计划 §10.3-#18）。
 * <p>
 * 原版事实基线：{@code MainServer/User::_UpdateQuestAcquireCondition} 对世界「可接取任务清单」逐行调
 * {@code Quest::CanAcquireQuest}（清单形态 {@code param_5 = 0}：不短路、不提示），按返回值写 opcode 127
 * 条目 —— {@code 2} ⇒ 平条目 {@code questId}；{@code 1}（仅等级轴不达且
 * {@code minlevel_permitted <= level + 1}）⇒ {@code questId | 0x20000} 软标记；{@code 0} ⇒ 丢弃。
 * 清单来源 = 世界中 NPC 携带的任务集合（{@code World::CheckAcquirableQuestFromNewNpc}），与模拟器
 * {@code WorldMapInstance#getQuestIds()}（NPC 任务并集）同源；opcode {@code 0x0181} 按原版混淆式
 * {@code (op + 0xD5) ^ 0xD5} 反解 = {@code 127}。
 * <p>
 * The native nearby-quest axis gate (plan §10.3-#18). Every assertion is sourced from the retail
 * {@code quest.xml} rows, the retention list owner column, or the retail opcode-127 semantics.
 */
class NativeNearbyQuestAxisGateTest {

	/** CombineTask 行：{@code minlevel_permitted=9} / {@code max_repeat_count=255} / pc_light。 */
	private static final int COMBINE_MINLEVEL_9 = 5000;
	/** SimpleTalk 行：{@code minlevel_permitted=10} / {@code max_repeat_count=100}。 */
	private static final int TALK_MINLEVEL_10_REPEAT_100 = 1963;
	/** SimpleTalk 行：{@code minlevel_permitted=11} / 前置 {@code Q1131}。 */
	private static final int TALK_MINLEVEL_11_PREREQ = 1132;
	private static final int PREREQUISITE = 1131;
	/** SimpleTalk 行：{@code minlevel_permitted=10} / {@code race_permitted=pc_dark}。 */
	private static final int TALK_MINLEVEL_10_DARK = 80001;
	/** SimpleHunt 行：id &gt; 0xFFFF，{@code minlevel_permitted=10}。 */
	private static final int HUNT_OVER_16BIT_MINLEVEL_10 = 80010;
	/** SimpleHunt 行：{@code minlevel_permitted=46} / {@code maxlevel_permitted=50}。 */
	private static final int HUNT_MINLEVEL_46_MAXLEVEL_50 = 3713;
	/** SimpleTalk 行：{@code minlevel_permitted=10} / {@code gender_permitted=female}。 */
	private static final int TALK_MINLEVEL_10_FEMALE = 3966;
	/** SimpleTalk 行：{@code max_repeat_count=1}（一次性任务）。 */
	private static final int TALK_ONCE = 1101;
	/** 线上等级上限（原版 {@code minlevel_permitted > 80} 的行永不入列）。 / The live level cap. */
	private static final int LEVEL_CAP = 80;

	// ------------------------------------------------------------ 原版三值分档

	@Test
	void levelAxisKeepsTheRetailTriple() {
		assertEquals(ZoneVerdict.ACQUIRABLE, port().zoneVerdict(elyosWarrior(9), COMBINE_MINLEVEL_9),
			"达到 minlevel ⇒ 原版 2（平条目）");
		assertEquals(ZoneVerdict.LEVEL_SOON, port().zoneVerdict(elyosWarrior(8), COMBINE_MINLEVEL_9),
			"只差 1 级 ⇒ 原版 1（0x20000 软标记）");
		assertEquals(ZoneVerdict.OMITTED, port().zoneVerdict(elyosWarrior(7), COMBINE_MINLEVEL_9),
			"差 2 级 ⇒ 原版 0（不入列表）");
	}

	@Test
	void overMaxLevelIsAHardZero() {
		int questId = HUNT_MINLEVEL_46_MAXLEVEL_50;
		assertEquals(ZoneVerdict.LEVEL_SOON, port().zoneVerdict(elyosWarrior(45), questId), "45 = 46 - 1");
		assertEquals(ZoneVerdict.ACQUIRABLE, port().zoneVerdict(elyosWarrior(46), questId), "下限可达");
		assertEquals(ZoneVerdict.ACQUIRABLE, port().zoneVerdict(elyosWarrior(50), questId), "上限可达");
		assertEquals(ZoneVerdict.OMITTED, port().zoneVerdict(elyosWarrior(51), questId),
			"超 maxlevel 是原版硬 0，不得落成软标记");
	}

	@Test
	void softVerdictRequiresEveryOtherAxisToPass() {
		// 种族：pc_dark 行对天族是硬 0；对魔族在差 1 级时才是软 1。
		Player elyos = elyosWarrior(9);
		Player asmodian = player(Race.ASMODIANS, PlayerClass.WARRIOR, 9);
		assertEquals(Outcome.RACE_BLOCKED,
			port().evaluateNpcAcquire(elyosWarrior(10), TALK_MINLEVEL_10_DARK).outcome());
		assertEquals(ZoneVerdict.OMITTED, port().zoneVerdict(elyos, TALK_MINLEVEL_10_DARK),
			"非等级轴失败 ⇒ 原版清单形态仍判 0");
		assertEquals(ZoneVerdict.LEVEL_SOON, port().zoneVerdict(asmodian, TALK_MINLEVEL_10_DARK),
			"其余轴通过且只差 1 级 ⇒ 软 1");

		// 性别：female 行对男角色是硬 0。
		Player male = elyosWarrior(9);
		Player female = elyosWarrior(9);
		setGender(female, Gender.FEMALE);
		assertEquals(ZoneVerdict.OMITTED, port().zoneVerdict(male, TALK_MINLEVEL_10_FEMALE));
		assertEquals(ZoneVerdict.LEVEL_SOON, port().zoneVerdict(female, TALK_MINLEVEL_10_FEMALE));

		// 前置：未完成 Q1131 时是硬 0；补完成后同一行、同一等级落软 1。
		Player gated = elyosWarrior(10);
		assertEquals(Outcome.PREREQUISITE_MISSING,
			port().evaluateNpcAcquire(elyosWarrior(11), TALK_MINLEVEL_11_PREREQ).outcome());
		assertEquals(ZoneVerdict.OMITTED, port().zoneVerdict(gated, TALK_MINLEVEL_11_PREREQ));
		gated.getQuestStateList().addQuest(PREREQUISITE,
			new QuestState(PREREQUISITE, QuestStatus.COMPLETE, 0, 1, null, 0, null));
		assertEquals(ZoneVerdict.LEVEL_SOON, port().zoneVerdict(gated, TALK_MINLEVEL_11_PREREQ));
		Player ready = elyosWarrior(11);
		ready.getQuestStateList().addQuest(PREREQUISITE,
			new QuestState(PREREQUISITE, QuestStatus.COMPLETE, 0, 1, null, 0, null));
		assertEquals(ZoneVerdict.ACQUIRABLE, port().zoneVerdict(ready, TALK_MINLEVEL_11_PREREQ));
	}

	@Test
	void stateAxisFollowsTheRetailRecordSemantics() {
		int repeatable = TALK_MINLEVEL_10_REPEAT_100;
		Player player = elyosWarrior(11);
		NativeTalkFixture.add(player, repeatable, QuestStatus.START, 0);
		assertEquals(ZoneVerdict.OMITTED, port().zoneVerdict(player, repeatable), "进行中 ⇒ 原版状态字节非 0");

		Player repeatFresh = elyosWarrior(11);
		complete(repeatFresh, repeatable, 0);
		assertEquals(ZoneVerdict.ACQUIRABLE, port().zoneVerdict(repeatFresh, repeatable), "已完成未达上限 ⇒ 可再接");

		Player repeatSpent = elyosWarrior(11);
		QuestState spent = complete(repeatSpent, repeatable, 100);
		assertEquals(100, spent.getCompleteCount());
		assertEquals(ZoneVerdict.OMITTED, port().zoneVerdict(repeatSpent, repeatable),
			"finishedcount >= max_repeat_count ⇒ 原版 0");

		Player once = elyosWarrior(1);
		complete(once, TALK_ONCE, 1);
		assertEquals(ZoneVerdict.OMITTED, port().zoneVerdict(once, TALK_ONCE),
			"max_repeat_count=1 且已完成 ⇒ 原版 0");
	}

	// ------------------------------------------------------------ 全族扫描

	@Test
	void softVerdictNeverMasksANonLevelAxisAcrossTheNativeUnion() {
		List<Integer> routed = nativeRoutedQuestIds();
		assertTrue(routed.size() > 5000, "原生路由行规模（原版 6224 清单里的已切换族）=" + routed.size());
		assertTrue(routed.stream().anyMatch(id -> id > 0xFFFF),
			"路由集必须含 id > 0xFFFF 的原版行（包编码回归面）");

		Player player = elyosWarrior(1);
		int soft = 0;
		int hardZeroWithNonLevelFailure = 0;
		int capped = 0;
		for (int questId : routed) {
			NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElse(null);
			if (row == null) {
				continue;
			}
			int minLevel = intField(row, "minlevel_permitted");
			if (minLevel > LEVEL_CAP) {
				capped++;
				assertEquals(ZoneVerdict.OMITTED, port().zoneVerdict(elyosWarrior(LEVEL_CAP), questId),
					"线上限之下永不入列（Q" + questId + "）");
				continue;
			}
			if (minLevel < 2) {
				continue;
			}
			setLevel(player, minLevel);
			Outcome atMinLevel = port().evaluateNpcAcquire(player, questId).outcome();
			setLevel(player, minLevel - 1);
			ZoneVerdict verdict = port().zoneVerdict(player, questId);
			assertEquals(atMinLevel == Outcome.STARTED, verdict == ZoneVerdict.LEVEL_SOON,
				"软结论只允许出现在「其余轴全通过 + 恰好差 1 级」的行（Q" + questId + "）");
			if (verdict == ZoneVerdict.LEVEL_SOON) {
				soft++;
			} else if (atMinLevel != Outcome.STARTED) {
				hardZeroWithNonLevelFailure++;
			}
		}
		assertTrue(capped > 0, "已切换族含原版 minlevel_permitted=999 的不可接取行（本批实测 " + capped + " 行）");
		assertTrue(soft > 0, "软档必须在原版数据里可达（否则本轴是死码）；本批实测 " + soft + " 行");
		assertTrue(hardZeroWithNonLevelFailure > 0,
			"必须覆盖「非等级轴失败」的反例；本批实测 " + hardZeroWithNonLevelFailure + " 行");
	}

	// ------------------------------------------------------------ owner 分流 + 包编码 + 控制器接线

	@Test
	void nearbyFlagsBranchByOwner() throws Exception {
		ObjectProvider<QuestEngine> previous = installBareEngine();
		try {
			Player atLevel = elyosWarrior(10);
			Map<Integer, Integer> flags = QuestService.nearbyQuestFlags(atLevel,
				List.of(TALK_MINLEVEL_10_DARK, COMBINE_MINLEVEL_9, 1132, 999999));
			assertEquals(Map.of(COMBINE_MINLEVEL_9, 0), flags,
				"平条目：原版判定通过的行才进清单（种族不符/前置未完成/非原生行不入列）");

			Map<Integer, Integer> mixed = QuestService.nearbyQuestFlags(elyosWarrior(9),
				List.of(HUNT_OVER_16BIT_MINLEVEL_10, TALK_MINLEVEL_10_REPEAT_100, COMBINE_MINLEVEL_9));
			assertEquals(Map.of(COMBINE_MINLEVEL_9, 0, TALK_MINLEVEL_10_REPEAT_100, 1,
				HUNT_OVER_16BIT_MINLEVEL_10, 1), mixed,
				"平条目与软标记共存，且含 id > 0xFFFF 的原版行（按 ID 升序）");
		} finally {
			restoreEngine(previous);
		}
	}

	@Test
	void controllerPublishesTheRetailListIntoTheNearbyPacket() throws Exception {
		ObjectProvider<QuestEngine> previous = installBareEngine();
		try {
			Player player = elyosWarrior(9);
			WorldMapInstance instance = new ObjenesisStd().newInstance(WorldMap3DInstance.class);
			setField(WorldMapInstance.class, instance, "questIds",
				new ArrayList<>(List.of(TALK_MINLEVEL_10_DARK, COMBINE_MINLEVEL_9,
					HUNT_OVER_16BIT_MINLEVEL_10)));
			MapRegion region = new ObjenesisStd().newInstance(MapRegion.class);
			setField(MapRegion.class, region, "parent", instance);
			WorldPosition position = new WorldPosition(210010000);
			setField(WorldPosition.class, position, "mapRegion", region);
			setField(WorldPosition.class, position, "isSpawned", true);
			setField(VisibleObject.class, player, "position", position);
			PlayerController controller = new PlayerController();
			controller.setOwner(player);

			controller.updateNearbyQuests();

			SM_NEARBY_QUESTS packet = lastNearbyPacket(player);
			assertNotNull(packet, "控制器必须下发 SM_NEARBY_QUESTS");
			assertEquals(Map.of(COMBINE_MINLEVEL_9, 0, HUNT_OVER_16BIT_MINLEVEL_10, 1),
				nearbyList(packet), "清单内容 = 原版判定（种族不符行不入列）");
		} finally {
			restoreEngine(previous);
		}
	}

	// ------------------------------------------------------------ 夹具

	private static NativeQuestStartPort port() {
		return NativeQuestStartPort.instance();
	}

	private static Player elyosWarrior(int level) {
		return player(Race.ELYOS, PlayerClass.WARRIOR, level);
	}

	private static Player player(Race race, PlayerClass playerClass, int level) {
		return NativeTalkFixture.player(race, playerClass, level);
	}

	/** 原生路由集（七个已切换族的并集）。 / The routed union of the seven migrated families. */
	private static List<Integer> nativeRoutedQuestIds() {
		TreeSet<Integer> ids = new TreeSet<>();
		ids.addAll(SimpleHuntHandler.instance().routedQuestIds());
		ids.addAll(SimpleSerialHuntHandler.instance().routedQuestIds());
		ids.addAll(SimpleTalkHandler.instance().routedQuestIds());
		ids.addAll(SimpleCollectItemHandler.instance().routedQuestIds());
		ids.addAll(SimpleUseItemHandler.instance().routedQuestIds());
		ids.addAll(SimpleItemPlayHandler.instance().routedQuestIds());
		ids.addAll(SimpleCombineTaskHandler.instance().routedQuestIds());
		return List.copyOf(ids);
	}

	private static int intField(NativeQuestXmlTable.QuestRow row, String tag) {
		Integer value = row.integer(tag);
		return value == null ? 0 : value;
	}

	/** 建一条已完成记录并写入完成次数（原版 finishedcount）。 / Seeds a completed record with a finish count. */
	private static QuestState complete(Player player, int questId, int finishedCount) {
		QuestState state = NativeTalkFixture.add(player, questId, QuestStatus.COMPLETE, 0);
		state.setCompleteCount(finishedCount);
		return state;
	}

	/** 装一个裸引擎（空 typed 目录）：owner 分流与包编码不需要生产目录。 / A bare engine for the owner split. */
	@SuppressWarnings("unchecked")
	private static ObjectProvider<QuestEngine> installBareEngine() throws ReflectiveOperationException {
		Field providerField = QuestEngine.class.getDeclaredField("instanceProvider");
		providerField.setAccessible(true);
		ObjectProvider<QuestEngine> previous = (ObjectProvider<QuestEngine>) providerField.get(null);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(NativeNearbyQuestAxisGateTest.class.getName() + ".engine", new QuestEngine());
		QuestEngine.setInstanceProvider(beanFactory.getBeanProvider(QuestEngine.class));
		return previous;
	}

	private static void restoreEngine(ObjectProvider<QuestEngine> previous) {
		QuestEngine.setInstanceProvider(previous);
	}

	private static SM_NEARBY_QUESTS lastNearbyPacket(Player player) {
		SM_NEARBY_QUESTS found = null;
		for (AionServerPacket packet : sentPackets(player)) {
			if (packet instanceof SM_NEARBY_QUESTS nearby) {
				found = nearby;
			}
		}
		return found;
	}

	@SuppressWarnings("unchecked")
	private static List<AionServerPacket> sentPackets(Player player) {
		return (List<AionServerPacket>) readField(AionConnection.class, player.getClientConnection(),
			"sendMsgQueue");
	}

	@SuppressWarnings("unchecked")
	private static Map<Integer, Integer> nearbyList(SM_NEARBY_QUESTS packet) {
		return (Map<Integer, Integer>) readField(SM_NEARBY_QUESTS.class, packet, "nearbyQuestList");
	}

	private static void setLevel(Player player, int level) {
		setField(PlayerCommonData.class, player.getCommonData(), "level", level);
	}

	private static void setGender(Player player, Gender gender) {
		setField(PlayerCommonData.class, player.getCommonData(), "gender", gender);
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) {
		try {
			Field field = declaringClass.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static Object readField(Class<?> declaringClass, Object target, String name) {
		try {
			Field field = declaringClass.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(target);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}

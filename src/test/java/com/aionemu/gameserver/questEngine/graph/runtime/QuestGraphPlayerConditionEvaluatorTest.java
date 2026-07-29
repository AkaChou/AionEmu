package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus.START;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.FAILED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.MATCHED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.NOT_MATCHED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Condition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerAbyssRankCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.KillVictimLevelDeltaCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.InvasionWorldActiveCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerClassCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerEquippedCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerGenderCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerInventoryCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerLevelCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerRaceCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerTitleCondition;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillInWorldEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WorldEnteredEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;
import com.aionemu.gameserver.questEngine.model.ConditionOperation;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 验证玩家接取资格条件的类型化只读评估。
 * Verifies typed, read-only evaluation of player start-eligibility conditions.
 */
class QuestGraphPlayerConditionEvaluatorTest {

	private static final DialogEvent EVENT = new DialogEvent("dialog", 7, 1000, 100, "QUEST_SELECT");

	/**
	 * 验证全部已支持玩家条件的正向匹配。
	 * Verifies positive matches for all supported player conditions.
	 */
	@Test
	void matchesAllSupportedConditions() throws ReflectiveOperationException {
		QuestGraphPlayerConditionEvaluator evaluator = new QuestGraphPlayerConditionEvaluator(player());
		List<Condition> conditions = List.of(new PlayerLevelCondition(10, 55),
			new PlayerRaceCondition(Set.of(Race.ELYOS)), new PlayerClassCondition(Set.of(PlayerClass.TEMPLAR)),
			new PlayerGenderCondition(Gender.MALE), new PlayerTitleCondition(42),
			new PlayerAbyssRankCondition(AbyssRankEnum.STAR1_OFFICER),
			new PlayerInventoryCondition(182200001, ConditionOperation.GREATER_EQUAL, 3), new PlayerEquippedCondition(182200001));

		for (Condition condition : conditions) {
			assertEquals(MATCHED, evaluator.evaluate(invocation(condition, EVENT)));
		}
	}

	/**
	 * 验证每类资格不匹配都返回 NOT_MATCHED 而不产生副作用。
	 * Verifies that every eligibility mismatch returns NOT_MATCHED without side effects.
	 */
	@Test
	void rejectsEveryMismatchedValue() throws ReflectiveOperationException {
		QuestGraphPlayerConditionEvaluator evaluator = new QuestGraphPlayerConditionEvaluator(player());
		List<Condition> conditions = List.of(new PlayerLevelCondition(51, null), new PlayerRaceCondition(Set.of(Race.ASMODIANS)),
			new PlayerClassCondition(Set.of(PlayerClass.GLADIATOR)), new PlayerGenderCondition(Gender.FEMALE),
			new PlayerTitleCondition(43), new PlayerAbyssRankCondition(AbyssRankEnum.STAR2_OFFICER),
			new PlayerInventoryCondition(182200001, ConditionOperation.GREATER, 3), new PlayerEquippedCondition(182200002));

		for (Condition condition : conditions) {
			assertEquals(NOT_MATCHED, evaluator.evaluate(invocation(condition, EVENT)));
		}
	}

	/**
	 * 验证背包数量使用旧 XML condition 的全部六种数值比较语义。
	 * Verifies all six numeric comparison semantics used by the legacy XML inventory condition.
	 */
	@Test
	void evaluatesEveryInventoryComparison() throws ReflectiveOperationException {
		QuestGraphPlayerConditionEvaluator evaluator = new QuestGraphPlayerConditionEvaluator(player());
		List<PlayerInventoryCondition> conditions = List.of(
			new PlayerInventoryCondition(182200001, ConditionOperation.EQUAL, 3),
			new PlayerInventoryCondition(182200001, ConditionOperation.GREATER, 2),
			new PlayerInventoryCondition(182200001, ConditionOperation.GREATER_EQUAL, 3),
			new PlayerInventoryCondition(182200001, ConditionOperation.LESSER, 4),
			new PlayerInventoryCondition(182200001, ConditionOperation.LESSER_EQUAL, 3),
			new PlayerInventoryCondition(182200001, ConditionOperation.NOT_EQUAL, 2));

		for (PlayerInventoryCondition condition : conditions) {
			assertEquals(MATCHED, evaluator.evaluate(invocation(condition, EVENT)));
		}
	}

	/**
	 * 验证当前玩家与服务端受害者等级快照的双边和单边差值比较，并对错误事件失败关闭。
	 * Verifies bounded and one-sided current-player/victim snapshot level deltas and fail-closed wrong-event handling.
	 */
	@Test
	void evaluatesKillVictimLevelDeltaFromAuthoritativeEvent() throws ReflectiveOperationException {
		QuestGraphPlayerConditionEvaluator evaluator = new QuestGraphPlayerConditionEvaluator(player());
		KillInWorldEvent event = new KillInWorldEvent("kill", 7, 1000, 0, 8, 45);

		assertEquals(MATCHED, evaluator.evaluate(invocation(new KillVictimLevelDeltaCondition(-5, 9), event)));
		assertEquals(MATCHED, evaluator.evaluate(invocation(new KillVictimLevelDeltaCondition(null, 5), event)));
		assertEquals(MATCHED, evaluator.evaluate(invocation(new KillVictimLevelDeltaCondition(5, null), event)));
		assertEquals(NOT_MATCHED, evaluator.evaluate(invocation(new KillVictimLevelDeltaCondition(null, 4), event)));
		assertEquals(NOT_MATCHED, evaluator.evaluate(invocation(new KillVictimLevelDeltaCondition(6, null), event)));
		assertEquals(FAILED, evaluator.evaluate(invocation(new KillVictimLevelDeltaCondition(-5, 9), EVENT)));
	}

	/**
	 * 验证入侵资格只读取持久化世界进入凭据，并拒绝错误世界、无凭据和错误事件。
	 * Verifies that invasion eligibility reads only persisted world-entry authority and rejects wrong worlds, absent authority, and wrong events.
	 */
	@Test
	void evaluatesInvasionWorldFromAuthoritativeEventSnapshot() throws ReflectiveOperationException {
		QuestGraphPlayerConditionEvaluator evaluator = new QuestGraphPlayerConditionEvaluator(player());
		InvasionWorldActiveCondition condition = new InvasionWorldActiveCondition(220050000);

		assertEquals(MATCHED, evaluator.evaluate(invocation(condition,
			new WorldEnteredEvent("enter", 7, 1000, 220050000, 1, 1, 2, 3, true))));
		assertEquals(NOT_MATCHED, evaluator.evaluate(invocation(condition,
			new WorldEnteredEvent("wrong", 7, 1000, 210010000, 1, 1, 2, 3, true))));
		assertEquals(NOT_MATCHED, evaluator.evaluate(invocation(condition,
			new WorldEnteredEvent("closed", 7, 1000, 220050000, 1, 1, 2, 3, false))));
		assertEquals(FAILED, evaluator.evaluate(invocation(condition, EVENT)));
	}

	/**
	 * 验证玩家服务状态缺失时不会被解释为业务不匹配或默认成功。
	 * Verifies that missing player services are not interpreted as a business mismatch or default success.
	 */
	@Test
	void failsWhenRequiredPlayerStateCannotBeRead() throws ReflectiveOperationException {
		Player player = player();
		setField(Player.class, player, "titleList", null);
		assertEquals(FAILED, new QuestGraphPlayerConditionEvaluator(player).evaluate(invocation(new PlayerTitleCondition(42), EVENT)));

		player = player();
		setField(Player.class, player, "abyssRank", null);
		assertEquals(FAILED, new QuestGraphPlayerConditionEvaluator(player)
			.evaluate(invocation(new PlayerAbyssRankCondition(AbyssRankEnum.STAR1_OFFICER), EVENT)));

		player = player();
		setField(Player.class, player, "inventory", null);
		assertEquals(FAILED, new QuestGraphPlayerConditionEvaluator(player)
			.evaluate(invocation(new PlayerInventoryCondition(182200001, ConditionOperation.EQUAL, 3), EVENT)));

		player = player();
		player.setEquipment(null);
		assertEquals(FAILED, new QuestGraphPlayerConditionEvaluator(player)
			.evaluate(invocation(new PlayerEquippedCondition(182200001), EVENT)));
	}

	/**
	 * 验证事件玩家与 evaluator owner 不一致时显式失败。
	 * Verifies explicit failure when the event player differs from the evaluator owner.
	 */
	@Test
	void rejectsForeignEventOwner() throws ReflectiveOperationException {
		QuestGraphPlayerConditionEvaluator evaluator = new QuestGraphPlayerConditionEvaluator(player());
		DialogEvent foreign = new DialogEvent("foreign", 8, 1000, 100, "QUEST_SELECT");

		assertEquals(FAILED, evaluator.evaluate(invocation(new PlayerLevelCondition(1, null), foreign)));
	}

	/**
	 * 创建带稳定静态属性的最小玩家 fixture。
	 * Creates a minimal player fixture with stable static attributes.
	 */
	private static Player player() throws ReflectiveOperationException {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData commonData = new PlayerCommonData(7);
		TitleList titleList = new TitleList();
		putTitleId(titleList, 42);
		AbyssRank abyssRank = new ObjenesisStd().newInstance(AbyssRank.class);
		setField(AbyssRank.class, abyssRank, "rank", AbyssRankEnum.STAR1_OFFICER);
		setField(PlayerCommonData.class, commonData, "level", 50);
		commonData.setRace(Race.ELYOS);
		commonData.setPlayerClass(PlayerClass.TEMPLAR);
		commonData.setGender(Gender.MALE);
		setField(AionObject.class, player, "objectId", 7);
		setField(Player.class, player, "playerCommonData", commonData);
		setField(Player.class, player, "titleList", titleList);
		setField(Player.class, player, "abyssRank", abyssRank);
		setField(Player.class, player, "inventory", new TestStorage(Map.of(182200001, 3L)));
		player.setEquipment(new TestEquipment(player, List.of(182200001)));
		return player;
	}

	/**
	 * 向最小称号 fixture 写入一个 ID，避免依赖全局静态数据。
	 * Adds one id to the minimal title fixture without depending on global static data.
	 */
	@SuppressWarnings("unchecked")
	private static void putTitleId(TitleList titleList, int titleId) throws ReflectiveOperationException {
		Field field = TitleList.class.getDeclaredField("titles");
		field.setAccessible(true);
		((Map<Integer, Object>) field.get(titleList)).put(titleId, null);
	}

	/**
	 * 设置 Objenesis fixture 无法通过构造器初始化的字段。
	 * Sets a field that the Objenesis fixture cannot initialize through constructors.
	 */
	private static void setField(Class<?> owner, Object target, String name, Object value) throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	/**
	 * 创建 evaluator 输入。
	 * Creates evaluator input.
	 */
	private static ConditionInvocation invocation(Condition condition, QuestGraphEvent event) {
		return new ConditionInvocation(condition, 1, START, event);
	}

	/**
	 * 提供只读物品数量的最小 CUBE fixture。
	 * Provides a minimal CUBE fixture for read-only item counts.
	 */
	private static final class TestStorage extends PlayerStorage {
		private final Map<Integer, Long> itemCounts;

		/** 创建稳定的物品数量映射。 / Creates a stable item-count mapping. */
		private TestStorage(Map<Integer, Long> itemCounts) {
			super(StorageType.CUBE);
			this.itemCounts = Map.copyOf(itemCounts);
		}

		/** 返回指定物品的 fixture 数量。 / Returns the fixture count for an item. */
		@Override
		public long getItemCountByItemId(int itemId) {
			return itemCounts.getOrDefault(itemId, 0L);
		}
	}

	/** 提供稳定已装备物品 ID 的最小 fixture。 / Provides a minimal fixture with stable equipped item ids. */
	private static final class TestEquipment extends Equipment {
		private final List<Integer> itemIds;

		/** 创建已装备物品快照。 / Creates an equipped-item snapshot. */
		private TestEquipment(Player player, List<Integer> itemIds) {
			super(player);
			this.itemIds = List.copyOf(itemIds);
		}

		/** 返回 fixture 已装备物品 ID。 / Returns fixture equipped item ids. */
		@Override
		public List<Integer> getEquippedItemIds() {
			return itemIds;
		}
	}
}

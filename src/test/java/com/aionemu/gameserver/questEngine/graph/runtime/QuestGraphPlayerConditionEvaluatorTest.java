package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus.START;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.FAILED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.MATCHED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.NOT_MATCHED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
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
	void matchesAllSupportedConditions() {
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
	void rejectsEveryMismatchedValue() {
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
	void evaluatesEveryInventoryComparison() {
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
	void evaluatesKillVictimLevelDeltaFromAuthoritativeEvent() {
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
	void evaluatesInvasionWorldFromAuthoritativeEventSnapshot() {
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
	void failsWhenRequiredPlayerStateCannotBeRead() {
		TestPlayer player = player();
		player.titleList = null;
		assertEquals(FAILED, new QuestGraphPlayerConditionEvaluator(player).evaluate(invocation(new PlayerTitleCondition(42), EVENT)));

		player = player();
		player.abyssRank = null;
		assertEquals(FAILED, new QuestGraphPlayerConditionEvaluator(player)
			.evaluate(invocation(new PlayerAbyssRankCondition(AbyssRankEnum.STAR1_OFFICER), EVENT)));

		player = player();
		player.inventory = null;
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
	void rejectsForeignEventOwner() {
		QuestGraphPlayerConditionEvaluator evaluator = new QuestGraphPlayerConditionEvaluator(player());
		DialogEvent foreign = new DialogEvent("foreign", 8, 1000, 100, "QUEST_SELECT");

		assertEquals(FAILED, evaluator.evaluate(invocation(new PlayerLevelCondition(1, null), foreign)));
	}

	/**
	 * 创建带稳定静态属性的最小玩家 fixture。
	 * Creates a minimal player fixture with stable static attributes.
	 */
	private static TestPlayer player() {
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.titleList = new TestTitleList(Set.of(42));
		TestAbyssRank abyssRank = new ObjenesisStd().newInstance(TestAbyssRank.class);
		abyssRank.rank = AbyssRankEnum.STAR1_OFFICER;
		player.abyssRank = abyssRank;
		player.inventory = new TestStorage(Map.of(182200001, 3L));
		player.setEquipment(new TestEquipment(player, List.of(182200001)));
		return player;
	}

	/** 提供不依赖反射的稳定玩家属性。 / Provides stable player attributes without reflection. */
	private static final class TestPlayer extends Player {
		private TitleList titleList;
		private AbyssRank abyssRank;
		private PlayerStorage inventory;

		/** 仅声明 Objenesis 测试类型；测试不会调用真实玩家构造链。 / Declares the Objenesis test type; tests never invoke the real player constructor chain. */
		private TestPlayer() {
			super(null, null, null, null);
		}

		/** 返回稳定玩家 ID。 / Returns the stable player id. */
		@Override
		public Integer getObjectId() {
			return 7;
		}

		/** 返回稳定玩家等级。 / Returns the stable player level. */
		@Override
		public byte getLevel() {
			return 50;
		}

		/** 返回稳定玩家阵营。 / Returns the stable player race. */
		@Override
		public Race getRace() {
			return Race.ELYOS;
		}

		/** 返回稳定玩家职业。 / Returns the stable player class. */
		@Override
		public PlayerClass getPlayerClass() {
			return PlayerClass.TEMPLAR;
		}

		/** 返回稳定玩家性别。 / Returns the stable player gender. */
		@Override
		public Gender getGender() {
			return Gender.MALE;
		}

		/** 返回可控称号 fixture。 / Returns the controllable title fixture. */
		@Override
		public TitleList getTitleList() {
			return titleList;
		}

		/** 返回可控深渊军衔 fixture。 / Returns the controllable abyss-rank fixture. */
		@Override
		public AbyssRank getAbyssRank() {
			return abyssRank;
		}

		/** 返回可控背包 fixture。 / Returns the controllable inventory fixture. */
		@Override
		public PlayerStorage getInventory() {
			return inventory;
		}
	}

	/** 提供独立于静态称号数据的只读称号集合。 / Provides a read-only title set independent of static title data. */
	private static final class TestTitleList extends TitleList {
		private final Set<Integer> titleIds;

		/** 创建稳定称号 ID 集合。 / Creates a stable title-id set. */
		private TestTitleList(Set<Integer> titleIds) {
			this.titleIds = Set.copyOf(titleIds);
		}

		/** 判断 fixture 是否包含称号。 / Returns whether the fixture contains the title. */
		@Override
		public boolean contains(int titleId) {
			return titleIds.contains(titleId);
		}
	}

	/** 提供不依赖持久化字段的稳定深渊军衔。 / Provides a stable abyss rank without persisted fields. */
	private static final class TestAbyssRank extends AbyssRank {
		private AbyssRankEnum rank;

		/** 仅声明 Objenesis 测试类型；测试不会调用真实军衔构造链。 / Declares the Objenesis test type; tests never invoke the real rank constructor chain. */
		private TestAbyssRank() {
			super(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}

		/** 返回稳定军衔。 / Returns the stable abyss rank. */
		@Override
		public AbyssRankEnum getRank() {
			return rank;
		}
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

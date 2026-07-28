package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus.START;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.FAILED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.MATCHED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.NOT_MATCHED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Condition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerClassCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerGenderCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerLevelCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerRaceCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatusCondition;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;

/**
 * 验证玩家接取资格条件的类型化只读评估。
 * Verifies typed, read-only evaluation of player start-eligibility conditions.
 */
class QuestGraphPlayerConditionEvaluatorTest {

	private static final DialogEvent EVENT = new DialogEvent("dialog", 7, 1000, 100, "QUEST_SELECT");

	/**
	 * 验证状态、等级、阵营、职业和性别条件的正向匹配。
	 * Verifies positive matches for status, level, race, class, and gender conditions.
	 */
	@Test
	void matchesAllSupportedConditions() throws ReflectiveOperationException {
		QuestGraphPlayerConditionEvaluator evaluator = new QuestGraphPlayerConditionEvaluator(player());
		List<Condition> conditions = List.of(new QuestStatusCondition(START), new PlayerLevelCondition(10, 55),
			new PlayerRaceCondition(Set.of(Race.ELYOS)), new PlayerClassCondition(Set.of(PlayerClass.TEMPLAR)),
			new PlayerGenderCondition(Gender.MALE));

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
		List<Condition> conditions = List.of(new QuestStatusCondition(com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus.REWARD),
			new PlayerLevelCondition(51, null), new PlayerRaceCondition(Set.of(Race.ASMODIANS)),
			new PlayerClassCondition(Set.of(PlayerClass.GLADIATOR)), new PlayerGenderCondition(Gender.FEMALE));

		for (Condition condition : conditions) {
			assertEquals(NOT_MATCHED, evaluator.evaluate(invocation(condition, EVENT)));
		}
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
		setField(PlayerCommonData.class, commonData, "level", 50);
		commonData.setRace(Race.ELYOS);
		commonData.setPlayerClass(PlayerClass.TEMPLAR);
		commonData.setGender(Gender.MALE);
		setField(AionObject.class, player, "objectId", 7);
		setField(Player.class, player, "playerCommonData", commonData);
		return player;
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
	private static ConditionInvocation invocation(Condition condition, DialogEvent event) {
		return new ConditionInvocation(condition, 1, START, event);
	}
}

package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DeleteRecipeAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.LearnRecipeAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.NotifyRecipeRejectionAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RecipeLearnableCondition;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRecipeBridge.DeleteRecipeCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRecipeBridge.LearnRecipeCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRecipeBridge.NotifyRecipeRejectionCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRecipeBridge.RecipeCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

/** 验证 recipe eligibility、幂等持久化动作和拒绝协议共享封闭 typed bridge。 / Verifies recipe eligibility, idempotent durable actions, and rejection protocol share one closed typed bridge. */
class QuestGraphRecipeBridgeTest {

	private static final DialogEvent EVENT = new DialogEvent("recipe", 7, 1000, 203709, "ACCEPT_QUEST");

	/** 验证期望可学习性可反转，错误 owner 和端点失败均失败关闭。 / Verifies expected learnability can be inverted and wrong owners or endpoint failures fail closed. */
	@Test
	void evaluatesExpectedEligibilityAndRejectsWrongAuthority() {
		QuestGraphRecipeBridge bridge = new QuestGraphRecipeBridge(7, query -> ConditionResult.MATCHED,
			command -> PreflightResult.READY, command -> ActionResult.APPLIED);

		assertEquals(ConditionResult.MATCHED, bridge.evaluate(condition(true, EVENT)));
		assertEquals(ConditionResult.NOT_MATCHED, bridge.evaluate(condition(false, EVENT)));
		assertEquals(ConditionResult.FAILED, bridge.evaluate(condition(true,
			new DialogEvent("wrong", 8, 1000, 203709, "ACCEPT_QUEST"))));

		QuestGraphRecipeBridge failed = new QuestGraphRecipeBridge(7, query -> {
			throw new IllegalStateException("unavailable");
		}, command -> PreflightResult.READY, command -> ActionResult.APPLIED);
		assertEquals(ConditionResult.FAILED, failed.evaluate(condition(true, EVENT)));
	}

	/** 验证学习、删除和拒绝协议生成唯一命令，协议动作不冒充 required preflight。 / Verifies learn, delete, and rejection protocol produce unique commands and protocol does not masquerade as required preflight. */
	@Test
	void mapsClosedRecipeCommandsAndProtocolBoundary() {
		List<RecipeCommand> preflight = new ArrayList<>();
		List<RecipeCommand> executed = new ArrayList<>();
		QuestGraphRecipeBridge bridge = new QuestGraphRecipeBridge(7, query -> ConditionResult.MATCHED, command -> {
			preflight.add(command);
			return PreflightResult.READY;
		}, command -> {
			executed.add(command);
			return ActionResult.APPLIED;
		});

		assertEquals(PreflightResult.READY, bridge.preflight(action(new LearnRecipeAction(155004001), "learn")));
		assertEquals(PreflightResult.READY, bridge.preflight(action(new DeleteRecipeAction(155004001), "delete")));
		assertInstanceOf(LearnRecipeCommand.class, preflight.get(0));
		assertInstanceOf(DeleteRecipeCommand.class, preflight.get(1));
		assertEquals(PreflightResult.FAILED,
			bridge.preflight(action(new NotifyRecipeRejectionAction(155004001), "notify")));

		assertEquals(ActionResult.APPLIED, bridge.execute(action(new LearnRecipeAction(155004001), "learn")));
		assertEquals(ActionResult.APPLIED, bridge.execute(action(new DeleteRecipeAction(155004001), "delete")));
		assertEquals(ActionResult.APPLIED, bridge.execute(action(new NotifyRecipeRejectionAction(155004001), "notify")));
		assertInstanceOf(LearnRecipeCommand.class, executed.get(0));
		assertInstanceOf(DeleteRecipeCommand.class, executed.get(1));
		assertInstanceOf(NotifyRecipeRejectionCommand.class, executed.get(2));
	}

	/** 创建 recipe 条件调用。 / Creates a recipe-condition invocation. */
	private static ConditionInvocation condition(boolean expected, DialogEvent event) {
		return new ConditionInvocation(new RecipeLearnableCondition(155004001, expected), 5000, QuestStatus.NONE, event);
	}

	/** 创建 recipe 动作调用。 / Creates a recipe-action invocation. */
	private static ActionInvocation action(Action action, String key) {
		return new ActionInvocation(action, 5000, 0, QuestStatus.START, EVENT, RepeatDeadlineResolution.NOT_APPLICABLE, null, key);
	}
}

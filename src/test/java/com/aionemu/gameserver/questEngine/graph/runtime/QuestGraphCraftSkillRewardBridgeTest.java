package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.dataholders.RecipeData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.CraftSkillEligibilityCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.CraftSkillEligibilityPolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.GrantCraftSkillRewardAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncCraftSkillRewardAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCraftSkillReferenceCatalog;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphCraftSkillRewardBridge.EligibilitySnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphCraftSkillRewardBridge.GrantCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphCraftSkillRewardBridge.SyncCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

import jakarta.xml.bind.JAXBContext;

/**
 * 验证制作晋级资格、持久化 grant 和提交后协议使用封闭 typed bridge。
 * Verifies craft-promotion eligibility, durable grant, and post-commit protocol use a closed typed bridge.
 */
class QuestGraphCraftSkillRewardBridgeTest {

	private static final DialogEvent EVENT = new DialogEvent("craft", 7, 1000, 203788, "SELECT_REWARD");
	private static final QuestGraphCraftSkillReferenceCatalog CATALOG = catalog();

	/**
	 * 验证 dialog 与 grant 的容量策略精确保留旧 Handler 的不对称行为。
	 * Verifies dialog and grant capacity policies preserve the legacy Handler's asymmetric behavior exactly.
	 */
	@Test
	void evaluatesClosedEligibilityPolicies() {
		List<EligibilitySnapshot> snapshots = new ArrayList<>(List.of(
			new EligibilitySnapshot(false, 0, false),
			new EligibilitySnapshot(true, 400, false),
			new EligibilitySnapshot(true, 399, false),
			new EligibilitySnapshot(true, 399, true)));
		QuestGraphCraftSkillRewardBridge bridge = bridge(query -> snapshots.remove(0));

		assertEquals(ConditionResult.MATCHED, bridge.evaluate(condition(CraftSkillEligibilityPolicy.CAPACITY_IF_EXISTING_NOT_TARGET, EVENT)));
		assertEquals(ConditionResult.MATCHED, bridge.evaluate(condition(CraftSkillEligibilityPolicy.CAPACITY_IF_EXISTING_NOT_TARGET, EVENT)));
		assertEquals(ConditionResult.NOT_MATCHED, bridge.evaluate(condition(CraftSkillEligibilityPolicy.CAPACITY_IF_EXISTING_NOT_TARGET, EVENT)));
		assertEquals(ConditionResult.MATCHED, bridge.evaluate(condition(CraftSkillEligibilityPolicy.CAPACITY_REQUIRED, EVENT)));
		assertEquals(ConditionResult.FAILED, bridge.evaluate(condition(CraftSkillEligibilityPolicy.CAPACITY_REQUIRED,
			new DialogEvent("wrong", 8, 1000, 203788, "SELECT_REWARD"))));
	}

	/**
	 * 验证 grant 预检和执行生成同一稳定命令，协议动作不会冒充 required action。
	 * Verifies grant preflight and execution produce the same stable command and protocol never masquerades as required work.
	 */
	@Test
	void mapsGrantCommandAndRejectsUnknownActions() {
		List<GrantCommand> preflight = new ArrayList<>();
		List<GrantCommand> executed = new ArrayList<>();
		QuestGraphCraftSkillRewardBridge bridge = new QuestGraphCraftSkillRewardBridge(7, Race.ELYOS, CATALOG,
			query -> new EligibilitySnapshot(false, 0, true), command -> {
				preflight.add(command);
				return PreflightResult.READY;
			}, command -> {
				executed.add(command);
				return ActionResult.APPLIED;
			}, command -> ActionResult.APPLIED, command -> ActionResult.APPLIED);

		ActionInvocation grant = action(new GrantCraftSkillRewardAction(40002, 400), "grant");
		assertEquals(PreflightResult.READY, bridge.preflight(grant));
		assertEquals(ActionResult.APPLIED, bridge.execute(grant));
		assertEquals(preflight, executed);
		assertEquals(Race.ELYOS, executed.getFirst().race());
		assertEquals(List.of(10, 20), executed.getFirst().recipeIds());
		assertThrows(UnsupportedOperationException.class, () -> executed.getFirst().recipeIds().add(30));
		assertEquals(PreflightResult.FAILED, bridge.preflight(action(new SyncCraftSkillRewardAction(40002), "sync")));
		assertEquals(ActionResult.FAILED, bridge.execute(action(new SyncQuestStatusAction(), "unknown")));
	}

	/**
	 * 验证 grant 命令拒绝非玩家阵营、乱序、重复和空 recipe 计划成员。
	 * Verifies grant commands reject non-player races and unsorted, duplicate, or null recipe-plan members.
	 */
	@Test
	void rejectsInvalidFrozenRecipePlans() {
		assertThrows(IllegalArgumentException.class,
			() -> new GrantCommand(1941, 7, Race.PC_ALL, 40002, 400, List.of(10), "grant"));
		assertThrows(IllegalArgumentException.class,
			() -> new GrantCommand(1941, 7, Race.ELYOS, 40002, 400, List.of(20, 10), "grant"));
		assertThrows(IllegalArgumentException.class,
			() -> new GrantCommand(1941, 7, Race.ELYOS, 40002, 400, List.of(10, 10), "grant"));
		List<Integer> withNull = new ArrayList<>();
		withNull.add(null);
		assertThrows(NullPointerException.class,
			() -> new GrantCommand(1941, 7, Race.ELYOS, 40002, 400, withNull, "grant"));
	}

	/**
	 * 验证协议失败进入显式 retry，成功接管后同一稳定键不重复投影，并支持 cleanup。
	 * Verifies protocol failure enters explicit retry, an accepted stable key is not reprojected, and cleanup is supported.
	 */
	@Test
	void retriesAndDeduplicatesPostCommitProtocol() {
		AtomicInteger direct = new AtomicInteger();
		AtomicInteger retries = new AtomicInteger();
		List<SyncCommand> retried = new ArrayList<>();
		QuestGraphCraftSkillRewardBridge bridge = new QuestGraphCraftSkillRewardBridge(7, Race.ELYOS, CATALOG,
			query -> new EligibilitySnapshot(false, 0, true), command -> PreflightResult.READY,
			command -> ActionResult.APPLIED, command -> {
				direct.incrementAndGet();
				return ActionResult.FAILED;
			}, command -> {
				retries.incrementAndGet();
				retried.add(command);
				return ActionResult.APPLIED;
			});
		ActionInvocation sync = action(new SyncCraftSkillRewardAction(40002), "sync");

		assertEquals(ActionResult.APPLIED, bridge.execute(sync));
		assertEquals(ActionResult.ALREADY_APPLIED, bridge.execute(sync));
		assertEquals(1, direct.get());
		assertEquals(1, retries.get());
		assertEquals(1, retried.size());
		assertEquals(1, bridge.size());
		bridge.clear();
		assertEquals(0, bridge.size());
		assertEquals(ActionResult.APPLIED, bridge.execute(sync));
		assertEquals(2, direct.get());
		assertEquals(2, retries.get());
	}

	/**
	 * 验证端点异常、空结果和无效资格快照全部失败关闭。
	 * Verifies endpoint exceptions, null results, and invalid eligibility snapshots all fail closed.
	 */
	@Test
	void failsClosedOnEndpointFailures() {
		QuestGraphCraftSkillRewardBridge bridge = new QuestGraphCraftSkillRewardBridge(7, Race.ELYOS, CATALOG, query -> {
			throw new IllegalStateException("unavailable");
		}, command -> null, command -> null, command -> null, command -> null);

		assertEquals(ConditionResult.FAILED, bridge.evaluate(condition(CraftSkillEligibilityPolicy.CAPACITY_REQUIRED, EVENT)));
		assertEquals(PreflightResult.FAILED, bridge.preflight(action(new GrantCraftSkillRewardAction(40002, 400), "grant")));
		assertEquals(ActionResult.FAILED, bridge.execute(action(new GrantCraftSkillRewardAction(40002, 400), "grant")));
		assertEquals(ActionResult.FAILED, bridge.execute(action(new SyncCraftSkillRewardAction(40002), "sync")));
	}

	/**
	 * 验证通用玩家 evaluator 拒绝接管制作资格，专用 bridge 同时拒绝外部 owner 与未知制作引用。
	 * Verifies the generic player evaluator refuses craft eligibility while the dedicated bridge rejects foreign owners and unknown references.
	 */
	@Test
	void enforcesDedicatedOwnershipAndReferenceClosure() {
		Player player = new ObjenesisStd().newInstance(TestPlayer.class);
		ConditionInvocation craft = condition(CraftSkillEligibilityPolicy.CAPACITY_REQUIRED, EVENT);
		assertEquals(ConditionResult.FAILED, new QuestGraphPlayerConditionEvaluator(player).evaluate(craft));

		QuestGraphCraftSkillRewardBridge bridge = bridge(query -> new EligibilitySnapshot(false, 0, true));
		DialogEvent foreign = new DialogEvent("foreign", 8, 1000, 203788, "SELECT_REWARD");
		assertEquals(ConditionResult.FAILED,
			bridge.evaluate(condition(CraftSkillEligibilityPolicy.CAPACITY_REQUIRED, foreign)));
		assertEquals(PreflightResult.FAILED,
			bridge.preflight(action(new GrantCraftSkillRewardAction(40002, 400), "grant", foreign)));
		assertEquals(ActionResult.FAILED,
			bridge.execute(action(new SyncCraftSkillRewardAction(40002), "sync", foreign)));
		assertEquals(ConditionResult.FAILED, bridge.evaluate(new ConditionInvocation(
			new CraftSkillEligibilityCondition(49999, 400, CraftSkillEligibilityPolicy.CAPACITY_REQUIRED), 1941,
			QuestStatus.START, EVENT)));
		assertEquals(ActionResult.FAILED, bridge.execute(action(new SyncCraftSkillRewardAction(49999), "unknown")));
	}

	/** 创建使用默认成功端点的 bridge。 / Creates a bridge with default successful endpoints. */
	private static QuestGraphCraftSkillRewardBridge bridge(
			java.util.function.Function<QuestGraphCraftSkillRewardBridge.EligibilityQuery, EligibilitySnapshot> eligibility) {
		return new QuestGraphCraftSkillRewardBridge(7, Race.ELYOS, CATALOG, eligibility, command -> PreflightResult.READY,
			command -> ActionResult.APPLIED, command -> ActionResult.APPLIED, command -> ActionResult.APPLIED);
	}

	/** 创建制作资格条件调用。 / Creates a craft-eligibility condition invocation. */
	private static ConditionInvocation condition(CraftSkillEligibilityPolicy policy, DialogEvent event) {
		return new ConditionInvocation(new CraftSkillEligibilityCondition(40002, 400, policy), 1941, QuestStatus.START, event);
	}

	/** 创建制作动作调用。 / Creates a craft action invocation. */
	private static ActionInvocation action(Action action, String key) {
		return action(action, key, EVENT);
	}

	/** 创建指定事件 owner 的制作动作调用。 / Creates a craft-action invocation for the specified event owner. */
	private static ActionInvocation action(Action action, String key, DialogEvent event) {
		return new ActionInvocation(action, 1941, 0, QuestStatus.REWARD, event,
			RepeatDeadlineResolution.NOT_APPLICABLE, null, key);
	}

	/** 从聚焦 RecipeData 构造独立制作引用目录。 / Builds the independent craft reference catalog from focused RecipeData. */
	private static QuestGraphCraftSkillReferenceCatalog catalog() {
		String xml = "<recipe_templates>"
			+ "<recipe_template id=\"20\" skillid=\"40002\" skillpoint=\"400\" autolearn=\"1\" race=\"PC_ALL\"/>"
			+ "<recipe_template id=\"10\" skillid=\"40002\" skillpoint=\"1\" autolearn=\"1\" race=\"ELYOS\"/>"
			+ "<recipe_template id=\"11\" skillid=\"40002\" skillpoint=\"1\" autolearn=\"1\" race=\"ASMODIANS\"/>"
			+ "<recipe_template id=\"30\" skillid=\"40003\" skillpoint=\"1\" autolearn=\"0\" race=\"PC_ALL\"/>"
			+ "</recipe_templates>";
		try {
			RecipeData data = (RecipeData) JAXBContext.newInstance(RecipeData.class).createUnmarshaller()
				.unmarshal(new StringReader(xml));
			return QuestGraphCraftSkillReferenceCatalog.build(data);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to build craft reference fixture", e);
		}
	}

	/** 提供稳定 owner ID 且不依赖反射的最小玩家 fixture。 / Provides a minimal player fixture with a stable owner id and no reflection. */
	private static final class TestPlayer extends Player {

		/** 仅声明 Objenesis 测试类型；测试不会调用真实玩家构造链。 / Declares the Objenesis test type; tests never invoke the real player constructor chain. */
		private TestPlayer() {
			super(null, null, null, null);
		}

		/** 返回 graph fixture 的稳定玩家 ID。 / Returns the stable player id for the graph fixture. */
		@Override
		public Integer getObjectId() {
			return 7;
		}
	}
}

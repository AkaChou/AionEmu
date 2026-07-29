package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.CraftSkillEligibilityCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.CraftSkillEligibilityPolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.GrantCraftSkillRewardAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncCraftSkillRewardAction;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCraftSkillReferenceCatalog;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;

/**
 * 将制作晋级资格、持久化奖励和提交后协议连接到封闭 typed 端口。
 * Connects craft-promotion eligibility, durable rewards, and post-commit protocol to closed typed ports.
 */
public final class QuestGraphCraftSkillRewardBridge {

	private final int playerId;
	private final Race race;
	private final QuestGraphCraftSkillReferenceCatalog references;
	private final Function<EligibilityQuery, EligibilitySnapshot> eligibility;
	private final Function<GrantCommand, PreflightResult> grantPreflight;
	private final Function<GrantCommand, ActionResult> grantExecutor;
	private final Function<SyncCommand, ActionResult> protocol;
	private final Function<SyncCommand, ActionResult> protocolRetry;
	private final Set<String> acceptedProtocolKeys = new HashSet<>();

	/**
	 * 创建绑定单一玩家的制作奖励 bridge。grant 端口必须按 idempotencyKey 持久冻结并收敛阵营 recipe plan，
	 * 仅在技能与全部 recipe 已持久化后返回成功；protocolRetry 必须可观测或持久接管提交后投影。
	 * Creates a craft-reward bridge bound to one player. The grant port must durably freeze and converge the race-specific
	 * recipe plan by idempotencyKey and succeed only after the skill and every recipe are durable; protocolRetry must
	 * observably or durably accept post-commit projection failures.
	 */
	public QuestGraphCraftSkillRewardBridge(int playerId, Race race, QuestGraphCraftSkillReferenceCatalog references,
			Function<EligibilityQuery, EligibilitySnapshot> eligibility,
			Function<GrantCommand, PreflightResult> grantPreflight, Function<GrantCommand, ActionResult> grantExecutor,
			Function<SyncCommand, ActionResult> protocol, Function<SyncCommand, ActionResult> protocolRetry) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Craft-skill reward bridge player id is invalid");
		}
		this.playerId = playerId;
		if (race != Race.ELYOS && race != Race.ASMODIANS) {
			throw new IllegalArgumentException("Craft-skill reward bridge race is invalid");
		}
		this.race = race;
		this.references = Objects.requireNonNull(references, "craft references");
		this.eligibility = Objects.requireNonNull(eligibility, "craft eligibility");
		this.grantPreflight = Objects.requireNonNull(grantPreflight, "craft grant preflight");
		this.grantExecutor = Objects.requireNonNull(grantExecutor, "craft grant executor");
		this.protocol = Objects.requireNonNull(protocol, "craft protocol");
		this.protocolRetry = Objects.requireNonNull(protocolRetry, "craft protocol retry");
	}

	/**
	 * 评估当前制作晋级策略；owner mismatch、未知条件、无效快照和端点异常均失败关闭。
	 * Evaluates the current craft-promotion policy; owner mismatch, unknown conditions, invalid snapshots, and endpoint
	 * exceptions all fail closed.
	 */
	public ConditionResult evaluate(ConditionInvocation invocation) {
		try {
			if (!validOwner(invocation) || !(invocation.condition() instanceof CraftSkillEligibilityCondition condition)) {
				return ConditionResult.FAILED;
			}
			if (!references.craftSkillIds().contains(condition.craftSkillId())) {
				return ConditionResult.FAILED;
			}
			EligibilityQuery query = new EligibilityQuery(invocation.questId(), playerId, condition.craftSkillId(),
				condition.targetLevel(), condition.policy());
			EligibilitySnapshot snapshot = Objects.requireNonNull(eligibility.apply(query), "craft eligibility snapshot");
			boolean eligible = switch (condition.policy()) {
				case CAPACITY_IF_EXISTING_NOT_TARGET -> !snapshot.skillPresent()
					|| snapshot.currentLevel() == condition.targetLevel() || snapshot.capacityAvailable();
				case CAPACITY_REQUIRED -> snapshot.capacityAvailable();
			};
			return eligible ? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
		} catch (RuntimeException e) {
			return ConditionResult.FAILED;
		}
	}

	/**
	 * 在 PREPARED 前要求 grant 端口确认可持久冻结并收敛 recipe plan；协议动作不得参与 required preflight。
	 * Requires the grant port to confirm it can durably freeze and converge the recipe plan before PREPARED; protocol
	 * actions never participate in required preflight.
	 */
	public PreflightResult preflight(ActionInvocation invocation) {
		try {
			if (!validOwner(invocation) || !(invocation.action() instanceof GrantCraftSkillRewardAction action)) {
				return PreflightResult.FAILED;
			}
			return Objects.requireNonNull(grantPreflight.apply(grantCommand(invocation, action)), "craft grant preflight result");
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
	}

	/**
	 * 执行幂等 grant 或提交后协议投影；未知动作、owner mismatch 和未被 retry 接管的协议失败均显式失败。
	 * Executes an idempotent grant or post-commit protocol projection; unknown actions, owner mismatch, and protocol
	 * failures not accepted by retry all fail explicitly.
	 */
	public synchronized ActionResult execute(ActionInvocation invocation) {
		try {
			if (!validOwner(invocation)) {
				return ActionResult.FAILED;
			}
			if (invocation.action() instanceof GrantCraftSkillRewardAction action) {
				return Objects.requireNonNull(grantExecutor.apply(grantCommand(invocation, action)), "craft grant result");
			}
			if (invocation.action() instanceof SyncCraftSkillRewardAction action) {
				return executeProtocol(syncCommand(invocation, action));
			}
			return ActionResult.FAILED;
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	/** 清理玩家会话内已接受的协议幂等键。 / Clears accepted protocol idempotency keys for the player session. */
	public synchronized void clear() {
		acceptedProtocolKeys.clear();
	}

	/** 返回当前已接受协议键数量，仅用于确定性测试和审计。 / Returns accepted protocol-key count for deterministic tests and audit only. */
	public synchronized int size() {
		return acceptedProtocolKeys.size();
	}

	/** 执行直接协议并在失败时转交显式 retry 端口。 / Executes direct protocol and delegates failures to the explicit retry port. */
	private ActionResult executeProtocol(SyncCommand command) {
		if (acceptedProtocolKeys.contains(command.idempotencyKey())) {
			return ActionResult.ALREADY_APPLIED;
		}
		ActionResult direct;
		try {
			direct = Objects.requireNonNull(protocol.apply(command), "craft protocol result");
		} catch (RuntimeException e) {
			direct = ActionResult.FAILED;
		}
		if (accepted(direct)) {
			acceptedProtocolKeys.add(command.idempotencyKey());
			return direct;
		}
		try {
			ActionResult retried = Objects.requireNonNull(protocolRetry.apply(command), "craft protocol retry result");
			if (accepted(retried)) {
				acceptedProtocolKeys.add(command.idempotencyKey());
				return retried;
			}
		} catch (RuntimeException ignored) {
			// The caller receives an explicit FAILED when neither protocol endpoint accepts ownership.
		}
		return ActionResult.FAILED;
	}

	/** 构造持久化 grant 命令。 / Builds a durable grant command. */
	private GrantCommand grantCommand(ActionInvocation invocation, GrantCraftSkillRewardAction action) {
		List<Integer> recipeIds = references.autolearnRecipeIds(race, action.craftSkillId(), action.targetLevel());
		return new GrantCommand(invocation.questId(), playerId, race, action.craftSkillId(), action.targetLevel(), recipeIds,
			invocation.idempotencyKey());
	}

	/** 构造提交后同步命令。 / Builds a post-commit synchronization command. */
	private SyncCommand syncCommand(ActionInvocation invocation, SyncCraftSkillRewardAction action) {
		if (!references.craftSkillIds().contains(action.craftSkillId())) {
			throw new IllegalArgumentException("Craft sync skill reference is unknown");
		}
		return new SyncCommand(invocation.questId(), playerId, action.craftSkillId(), invocation.idempotencyKey());
	}

	/** 校验条件事件属于当前玩家。 / Validates that a condition event belongs to the current player. */
	private boolean validOwner(ConditionInvocation invocation) {
		return invocation != null && invocation.event().playerId() == playerId;
	}

	/** 校验动作事件属于当前玩家。 / Validates that an action event belongs to the current player. */
	private boolean validOwner(ActionInvocation invocation) {
		return invocation != null && invocation.event().playerId() == playerId;
	}

	/** 判断端点是否已完成或接管命令。 / Returns whether an endpoint completed or accepted command ownership. */
	private static boolean accepted(ActionResult result) {
		return result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED;
	}

	/** 表示制作晋级只读资格查询。 / Represents a read-only craft-promotion eligibility query. */
	public record EligibilityQuery(int questId, int playerId, int craftSkillId, int targetLevel,
			CraftSkillEligibilityPolicy policy) {
		/** 校验 owner、引用、目标等级和封闭策略。 / Validates owner, reference, target level, and closed policy. */
		public EligibilityQuery {
			validate(questId, playerId, craftSkillId, targetLevel, "query");
			Objects.requireNonNull(policy, "craft eligibility policy");
		}
	}

	/** 表示资格端点返回的服务端技能和容量快照。 / Represents the server-side skill and capacity snapshot returned by eligibility. */
	public record EligibilitySnapshot(boolean skillPresent, int currentLevel, boolean capacityAvailable) {
		/** 校验技能存在性与等级快照一致。 / Validates consistency between skill presence and level snapshot. */
		public EligibilitySnapshot {
			if (skillPresent && currentLevel <= 0 || !skillPresent && currentLevel != 0) {
				throw new IllegalArgumentException("Craft eligibility snapshot is invalid");
			}
		}
	}

	/** 表示必须持久冻结并收敛 recipe plan 的制作奖励命令。 / Represents a craft reward command that must durably freeze and converge its recipe plan. */
	public record GrantCommand(int questId, int playerId, Race race, int craftSkillId, int targetLevel,
			List<Integer> recipeIds, String idempotencyKey) {
		/** 校验 owner、阵营、引用、等级、冻结计划和稳定键。 / Validates owner, race, reference, level, frozen plan, and stable key. */
		public GrantCommand {
			validate(questId, playerId, craftSkillId, targetLevel, idempotencyKey);
			if (race != Race.ELYOS && race != Race.ASMODIANS) {
				throw new IllegalArgumentException("Craft reward race is invalid");
			}
			recipeIds = List.copyOf(Objects.requireNonNull(recipeIds, "craft reward recipe plan"));
			int previous = 0;
			for (Integer recipeId : recipeIds) {
				if (recipeId == null || recipeId <= previous) {
					throw new IllegalArgumentException("Craft reward recipe plan must be positive, unique, and sorted");
				}
				previous = recipeId;
			}
		}
	}

	/** 表示固定制作晋级消息和 recipe 投影的提交后命令。 / Represents the post-commit command for fixed promotion and recipe projections. */
	public record SyncCommand(int questId, int playerId, int craftSkillId, String idempotencyKey) {
		/** 校验 owner、引用和稳定键。 / Validates owner, reference, and stable key. */
		public SyncCommand {
			if (questId <= 0 || playerId <= 0 || craftSkillId <= 0 || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Craft sync command is invalid");
			}
		}
	}

	/** 校验制作命令公共字段。 / Validates common craft-command fields. */
	private static void validate(int questId, int playerId, int craftSkillId, int targetLevel, String stableKey) {
		if (questId <= 0 || playerId <= 0 || craftSkillId <= 0 || targetLevel != 400 && targetLevel != 500
				|| stableKey == null || stableKey.isBlank()) {
			throw new IllegalArgumentException("Craft reward bridge input is invalid");
		}
	}
}

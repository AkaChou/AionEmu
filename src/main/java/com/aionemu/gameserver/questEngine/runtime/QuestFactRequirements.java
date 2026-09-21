package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 单个 transition 真正读取的事实族集合。
 * Set of fact families that a single transition actually reads.
 *
 * <p>为什么存在：快照里最贵的事实族（任务 ID 集合、背包、装备、制作技能）只有少数条件/动作会读，而原实现
 * 每次派发都全量捕获。需求由 transition 的 {@code conditions()} 与 {@code actions()} 静态推导，采集端据此
 * 跳过本转换读不到的事实族；被跳过的事实族保持「未捕获」，读取方 fail-closed（返回不匹配或抛
 * {@link IllegalStateException}），绝不伪装成 0。Rationale: the expensive fact families (quest-id sets,
 * inventory, equipment, craft skills) are read by only a few conditions/actions, yet every dispatch used to
 * capture all of them. The requirement set is derived statically from the transition's conditions and
 * actions, so the capture side can skip what this transition never reads. Skipped families stay "not
 * captured" and fail closed instead of being passed off as zero.</p>
 *
 * <p>完备性由编译器保证：{@link QuestCondition} 与 {@link QuestAction} 都是 sealed 接口，下面的 switch 不写
 * {@code default}，因此新增任何条件/动作都会让推导无法编译，不可能静默漏采。Completeness is enforced by the
 * compiler: {@link QuestCondition} and {@link QuestAction} are sealed and the switches below carry no
 * {@code default}, so a new condition or action breaks the build instead of silently skipping a fact.</p>
 *
 * @param startEligibility      开始资格事实 / start-eligibility facts
 * @param eventActivityQuestIds 需要读取事件服务成员资格的任务 ID / quest ids whose event-service membership is read
 * @param worldFacts            世界实例事实（NPC 模板与区域名） / world-instance facts (NPC templates and zone names)
 * @param questIdSets           已完成/进行中任务 ID 集合 / completed and in-progress quest-id sets
 * @param inventory             背包事实 / inventory facts
 * @param equipment             装备与套装事实 / equipment and item-set facts
 * @param craft                 制作技能与配方事实 / craft skill and recipe facts
 */
public record QuestFactRequirements(boolean startEligibility, Set<Integer> eventActivityQuestIds, boolean worldFacts,
		boolean questIdSets, boolean inventory, boolean equipment, boolean craft) {

	public QuestFactRequirements {
		eventActivityQuestIds = Set.copyOf(Objects.requireNonNull(eventActivityQuestIds, "eventActivityQuestIds"));
	}

	/**
	 * 从 definition 与 transition 推导最小事实集。
	 * 包含元数据声明的前置条件（从 NONE 状态获取未接取任务时读取的完成/进行中任务 ID 集合及装备事实）。
	 * Derives the minimal fact set from definition and transition, including metadata-declared
	 * prerequisites (completed/in-progress quest-id sets and equipment facts read when acquiring an unaccepted quest).
	 *
	 * @param definition 任务定义 / quest definition
	 * @param event      运行时事件 / the runtime event
	 * @param transition 转换 / the transition
	 * @return 该转换真正需要的事实族 / the fact families this transition reads
	 */
	public static QuestFactRequirements of(CompiledQuestDefinition definition, QuestEvent event,
			QuestTransition transition) {
		Objects.requireNonNull(definition, "definition");
		QuestFactRequirements base = of(definition.id(), event, transition);
		boolean questIdSets = base.questIdSets();
		boolean equipment = base.equipment();

		// 接取转换（从 NONE 状态进入非 NONE 状态）会由 planner 隐式执行 metadataPrerequisitesSatisfied，
		// 该检查需要预读元数据声明的前置任务（完成/未完成/已接/未接）以及装备前置事实。
		// Transitions that acquire an unaccepted quest (from NONE to non-NONE) implicitly run
		// metadataPrerequisitesSatisfied in the planner, which reads metadata-declared prerequisites
		// (finished/unfinished/acquired/noacquired) and equipped start conditions.
		if (transition.sourceNode() != null && transition.targetNode() != null) {
			QuestNode source = definition.definition().nodes().stream()
				.filter(node -> node.label().equals(transition.sourceNode())).findFirst().orElse(null);
			QuestNode target = definition.definition().nodes().stream()
				.filter(node -> node.label().equals(transition.targetNode())).findFirst().orElse(null);
			if (source != null && target != null
					&& source.projection().status() == QuestStatus.NONE
					&& target.projection().status() != QuestStatus.NONE) {
				var metadata = definition.definition().metadata();
				if (!metadata.prerequisites().isEmpty()) {
					questIdSets = true;
				}
				for (var group : metadata.startConditionGroups()) {
					for (var condition : group.conditions()) {
						switch (condition.type().toLowerCase(java.util.Locale.ROOT)) {
							case "finished":
							case "unfinished":
							case "noacquired":
							case "acquired":
								questIdSets = true;
								break;
							case "equipped":
								equipment = true;
								break;
							default:
								break;
						}
					}
				}
			}
		}

		if (questIdSets == base.questIdSets() && equipment == base.equipment()) {
			return base;
		}
		return new QuestFactRequirements(base.startEligibility(), base.eventActivityQuestIds(),
			base.worldFacts(), questIdSets, base.inventory(), equipment, base.craft());
	}

	/**
	 * 从 transition 推导最小事实集。
	 * Derives the minimal fact set from the transition.
	 *
	 * @param questId    transition 所属任务 ID（{@code EventActive.questId() == 0} 表示自引用）
	 *                   / owning quest id used by self-referencing {@code EventActive.questId() == 0}
	 * @param event      运行时事件（放弃事件也会触发任务工作物品清理）
	 *                   / the runtime event (abandon events also trigger quest work-item cleanup)
	 * @param transition 转换 / the transition
	 * @return 该转换真正需要的事实族 / the fact families this transition reads
	 */
	public static QuestFactRequirements of(int questId, QuestEvent event, QuestTransition transition) {
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(transition, "transition");
		boolean startEligibility = false;
		boolean worldFacts = false;
		boolean questIdSets = false;
		boolean inventory = false;
		boolean equipment = false;
		boolean craft = false;
		boolean completes = false;
		boolean abandons = event instanceof QuestEvent.Abandon
			|| transition.event() instanceof QuestEvent.Abandon;
		Set<Integer> eventActivityQuestIds = new HashSet<>();
		for (QuestCondition condition : transition.conditions()) {
			switch (condition) {
				case QuestCondition.StartEligible ignored:
					startEligibility = true;
					break;
				case QuestCondition.EventActive eventActive:
					eventActivityQuestIds.add(eventActive.questId() == 0 ? questId : eventActive.questId());
					break;
				case QuestCondition.WorldNpcIs ignored:
					worldFacts = true;
					break;
				case QuestCondition.ZoneIs ignored:
					worldFacts = true;
					break;
				case QuestCondition.QuestsFinished ignored:
					questIdSets = true;
					break;
				case QuestCondition.UnfinishedQuest ignored:
					questIdSets = true;
					break;
				case QuestCondition.AcquiredQuest ignored:
					questIdSets = true;
					break;
				case QuestCondition.NoAcquiredQuest ignored:
					questIdSets = true;
					break;
				case QuestCondition.HasItem ignored:
					inventory = true;
					break;
				case QuestCondition.EquipmentSetEquipped ignored:
					equipment = true;
					break;
				case QuestCondition.EquippedItem ignored:
					equipment = true;
					break;
				case QuestCondition.RecipeKnown ignored:
					craft = true;
					break;
				case QuestCondition.CanGrantCraftSkill ignored:
					craft = true;
					break;
				// 其余条件只读基础事实：状态、变量、职业、种族、性别、世界 ID、货币、队伍、会员、DP、完成次数。
				// The remaining conditions read base facts only: status, variables, class, race, gender, world id,
				// currencies, team, membership, DP and completion count.
				case QuestCondition.StatusIs ignored:
					break;
				case QuestCondition.QuestVariableIs ignored:
					break;
				case QuestCondition.VariableAtLeast ignored:
					break;
				case QuestCondition.VariableBelow ignored:
					break;
				case QuestCondition.VariableSumIs ignored:
					break;
				case QuestCondition.VariableSumBelow ignored:
					break;
				case QuestCondition.PvpVictimLevelDelta ignored:
					break;
				case QuestCondition.PvpRecipientInZone ignored:
					break;
				case QuestCondition.PlayerClassIs ignored:
					break;
				case QuestCondition.AdvancedClassIs ignored:
					break;
				case QuestCondition.GenderIs ignored:
					break;
				case QuestCondition.PlayerRaceIs ignored:
					break;
				case QuestCondition.PlayerInGroup ignored:
					break;
				case QuestCondition.WorldIs ignored:
					break;
				case QuestCondition.NpcHpBelowPercent ignored:
					break;
				case QuestCondition.CurrencyAtLeast ignored:
					break;
				case QuestCondition.CurrencyBelow ignored:
					break;
				case QuestCondition.MembershipPermission ignored:
					break;
				case QuestCondition.DpAtMax ignored:
					break;
				case QuestCondition.CompleteCountIs ignored:
					break;
			}
		}
		for (QuestAction action : transition.actions()) {
            switch (action) {
                case QuestAction.RemoveItem ignored:
                    inventory = true;
                    break;
                case QuestAction.GiveItem ignored:
                    inventory = true;
                    break;
                case QuestAction.UnequipItem ignored:
                    equipment = true;
                    break;
                // 其余动作不改写需要预读的事实族；制作动作只写技能/配方，不读快照中的制作事实。
                // The remaining actions do not rewrite a fact family that has to be pre-read; craft actions only
                // write skills/recipes and never read the captured craft facts.
                case QuestAction.SetVariable ignored:
                    break;
                case QuestAction.IncrementVariable ignored:
                    break;
                case QuestAction.SetStatus ignored:
                    break;
                case QuestAction.GrantReward ignored:
                    break;
                case QuestAction.GrantSelectedReward ignored:
                    break;
                case QuestAction.DecreaseCurrency ignored:
                    break;
                case QuestAction.SetCurrency ignored:
                    break;
                case QuestAction.LearnRecipe ignored:
                    break;
                case QuestAction.ForgetRecipe ignored:
                    break;
                case QuestAction.GrantCraftSkill ignored:
                    break;
                case QuestAction.CompleteQuest ignored:
                    completes = true;
                    break;
                case QuestAction.PromoteArchDaeva ignored:
                    break;
                case QuestAction.BlockDefaultItemUse ignored:
                    break;
                case QuestAction.AbandonQuest ignored:
                    abandons = true;
                    break;
            }
		}
		// 完成与放弃会让 planner 追加 questWorkItems 的 RemoveItem(ALL)，提交前的 preflight 仍会读取背包数量；
		// 这类转换占比很小，因此整类保守地要求背包事实，换取不依赖 metadata 的稳定判定。
		// Completion and abandonment let the planner append RemoveItem(ALL) for quest work items and the
		// pre-commit preflight still reads inventory counts; such transitions are rare, so the whole class
		// requests inventory facts, which keeps the decision independent of quest metadata.
		if (completes || abandons) {
			inventory = true;
		}
		return new QuestFactRequirements(startEligibility, eventActivityQuestIds, worldFacts, questIdSets,
			inventory, equipment, craft);
	}

	/**
	 * 兼容入口：调用方只声明了三个旧门控时，其余事实族一律按「需要」处理。
	 * Compatibility entry point: when a caller only declares the three legacy gates, every other family is
	 * treated as required, keeping the old capture behaviour.
	 *
	 * @param startEligibility      开始资格门控 / start-eligibility gate
	 * @param eventActivityQuestIds 事件服务事实门控 / event-service membership gate
	 * @param worldFacts            世界事实门控 / world-facts gate
	 * @return 保守的事实需求 / conservative requirements
	 */
	public static QuestFactRequirements conservative(boolean startEligibility, Set<Integer> eventActivityQuestIds,
			boolean worldFacts) {
		return new QuestFactRequirements(startEligibility, eventActivityQuestIds, worldFacts, true, true, true, true);
	}
}

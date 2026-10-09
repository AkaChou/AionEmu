package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;

/**
 * 生产定义事件接线合同：定义里出现的每个 typed event 都必须在运行时/服务侧存在投递路径。
 * <p>
 * 这是启动期 {@code QuestEngine.prepareProductionDefinitions} 的第二道合同（在交互对象合同之后）。
 * 它此前只作为 {@code QuestEngine} 里的内联 instanceof 链存在，于是新增事件类型（如 P0c 的
 * {@link QuestEvent.SystemGrant}）一旦被原版定义使用，就会在服务端启动时抛
 * {@code typed production event is not wired into QuestEngine}——而 T3 全绿（无测试覆盖该链）。
 * 现在清单归一到本类，启动路径与门禁共用同一份口径。
 * <p>
 * Startup contract for typed production event wiring, shared by {@code QuestEngine} and the gates so
 * that the whitelist cannot drift away from the startup path again.
 */
public final class QuestProductionEventWiring {

	private QuestProductionEventWiring() {
	}

	/** 校验一批可执行定义；出现未接线事件即抛出（与启动期同文案）。 / Validates a batch of executable definitions. */
	public static void validate(Iterable<CompiledQuestDefinition> definitions) {
		for (CompiledQuestDefinition definition : definitions) {
			validateDefinition(definition);
		}
	}

	/** 校验单个定义。 / Validates one definition. */
	public static void validateDefinition(CompiledQuestDefinition definition) {
		for (QuestTransition transition : definition.definition().transitions()) {
			if (!isWired(transition.event())) {
				throw new IllegalStateException("typed production event is not wired into QuestEngine: "
					+ transition.event().type());
			}
		}
	}

	/**
	 * 该事件类型是否有运行时投递路径。
	 * <p>
	 * {@link QuestEvent.SystemGrant} 是唯一的"服务侧投递"事件：阵营日常轮换 / 进区域 / 挑战任务由
	 * {@code RetailSystemGrantDispatcher} 显式调用 {@code QuestService.startQuest}，不经过事件端口，
	 * 因此在这里与端口投递事件同等放行。
	 * Whether the event type has a delivery path; {@code SystemGrant} is delivered service-side by the
	 * retail grant dispatcher rather than through an event port.
	 */
	public static boolean isWired(QuestEvent event) {
		return event instanceof QuestEvent.TalkToNpc
			|| event instanceof QuestEvent.KillNpc
			|| event instanceof QuestEvent.KillNpcSet
			|| event instanceof QuestEvent.AttackNpc
			|| event instanceof QuestEvent.CanAct
			|| event instanceof QuestEvent.EnterZone
			|| event instanceof QuestEvent.LevelUp
			|| event instanceof QuestEvent.EnterWorld
			|| event instanceof QuestEvent.UseItem
			|| event instanceof QuestEvent.ItemPlay
			|| event instanceof QuestEvent.GetItem
			|| event instanceof QuestEvent.CollectItem
			|| event instanceof QuestEvent.PassFlyingRing
			|| event instanceof QuestEvent.EnterWindStream
			|| event instanceof QuestEvent.AtDistance
			|| event instanceof QuestEvent.Die
			|| event instanceof QuestEvent.LogOut
			|| event instanceof QuestEvent.MovieEnd
			|| event instanceof QuestEvent.NpcReachTarget
			|| event instanceof QuestEvent.NpcLostTarget
			|| event instanceof QuestEvent.ZoneMissionEnd
			|| event instanceof QuestEvent.EventQuestRefresh
			|| event instanceof QuestEvent.InvisibleTimerEnd
			|| event instanceof QuestEvent.FailCraft
			|| event instanceof QuestEvent.EquipItem
			|| event instanceof QuestEvent.Abandon
			|| event instanceof QuestEvent.DredgionReward
			|| event instanceof QuestEvent.HouseItemUse
			|| event instanceof QuestEvent.KillInWorld
			|| event instanceof QuestEvent.KillRanked
			|| event instanceof QuestEvent.LeaveZone
			|| event instanceof QuestEvent.QuestTimerEnd
			|| event instanceof QuestEvent.UseSkill
			|| event instanceof QuestEvent.QuestDialog
			|| event instanceof QuestEvent.BonusApply
			|| event instanceof QuestEvent.SystemGrant;
	}
}

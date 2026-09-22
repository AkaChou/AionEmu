package com.aionemu.gameserver.taskmanager.tasks;

import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.common.legacy.GroupEvent;
import com.aionemu.gameserver.model.team2.common.legacy.PlayerAllianceEvent;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.taskmanager.AbstractIterativePeriodicTaskManager;

/**
 * 队伍/联盟移动更新任务：同步队员位置后移除任务（再次移动时重新加入）。
 * Team/alliance movement update task: syncs member positions then removes the task (re-added on next move).
 * @author Sarynth
 */
public final class TeamMoveUpdater extends AbstractIterativePeriodicTaskManager<Player> {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 * -- SETTER --
	 *  注入 Spring 实例提供者。
	 *  Inject the Spring instance provider.
	 */
	@Setter
	private static volatile ObjectProvider<TeamMoveUpdater> instanceProvider;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static TeamMoveUpdater getInstance() {
		ObjectProvider<TeamMoveUpdater> provider = instanceProvider;
		TeamMoveUpdater provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("TeamMoveUpdater 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 以 2000ms 周期构造队伍移动更新器。
	 * Construct the team-move updater with a 2000ms period.
	 */
	public TeamMoveUpdater() {
		super(2000);
	}

	/**
	 * 同步队伍/联盟移动事件，并停止该玩家任务。
	 * Sync group/alliance movement events and stop this player's task.
	 * 玩家 / Player
	 */
	@Override
	protected void callTask(Player player) {
		this.stopTask(player);
		if (player.isInGroup2()) {
			PlayerGroupService.updateGroup(player, GroupEvent.MOVEMENT);
		}
		if (player.isInAlliance2()) {
			PlayerAllianceService.updateAlliance(player, PlayerAllianceEvent.MOVEMENT);
		}
	}

	/**
	 * 耗时统计方法名。
	 * Method name for runtime stats.
	 * @return 方法名 / Method name
	 */
	@Override
	protected String getCalledMethodName() {
		return "teamMoveUpdate()";
	}
}

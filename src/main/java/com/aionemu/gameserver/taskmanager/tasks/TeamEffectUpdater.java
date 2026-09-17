package com.aionemu.gameserver.taskmanager.tasks;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.common.legacy.GroupEvent;
import com.aionemu.gameserver.model.team2.common.legacy.PlayerAllianceEvent;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.taskmanager.AbstractIterativePeriodicTaskManager;

/**
 * 队伍/联盟效果更新任务：在线玩家触发一次队伍效果同步后移除。
 * Team/alliance effect update task: syncs group/alliance effects once for online players, then removes the task.
 */
public final class TeamEffectUpdater extends AbstractIterativePeriodicTaskManager<Player> {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<TeamEffectUpdater> instanceProvider;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static TeamEffectUpdater getInstance() {
		ObjectProvider<TeamEffectUpdater> provider = instanceProvider;
		TeamEffectUpdater provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("TeamEffectUpdater 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param provider 实例提供者 / Provider
	 */
	public static void setInstanceProvider(ObjectProvider<TeamEffectUpdater> provider) {
		instanceProvider = provider;
	}

	/**
	 * 以 500ms 周期构造队伍效果更新器。
	 * Construct the team-effect updater with a 500ms period.
	 */
	public TeamEffectUpdater() {
		super(500);
	}

	/**
	 * 若在线则同步队伍/联盟效果，并停止该玩家任务。
	 * If online, sync group/alliance effects and stop this player's task.
	 *
	 * 玩家 / Player
	 */
	@Override
	protected void callTask(Player player) {
		this.stopTask(player);
		if (player.isOnline()) {
			if (player.isInGroup2()) {
				PlayerGroupService.updateGroup(player, GroupEvent.UPDATE);
				PlayerGroupService.updateGroup(player, GroupEvent.UNK_53);
			}
			if (player.isInAlliance2()) {
				PlayerAllianceService.updateAlliance(player, PlayerAllianceEvent.UPDATE);
			}
		}
	}

	/**
	 * 耗时统计方法名。
	 * Method name for runtime stats.
	 *
	 * @return 方法名 / Method name
	 */
	@Override
	protected String getCalledMethodName() {
		return "teamEffectUpdate()";
	}
}

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
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	private static final class SingletonHolder {

		/**
		 * 默认单例实例。
		 * Default singleton instance.
		 */
		private static final TeamEffectUpdater INSTANCE = new TeamEffectUpdater();
	}

	/**
	 * 获取单例：优先 Spring 提供者，否则静态 holder。
	 * Get the singleton: prefer Spring provider, otherwise the static holder.
	 *
	 * @return 更新器实例 / Updater instance
	 */
	public static TeamEffectUpdater getInstance() {
		ObjectProvider<TeamEffectUpdater> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
		}
		return SingletonHolder.INSTANCE;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * Provider
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
		if (player.isOnline()) {
			if (player.isInGroup2()) {
				PlayerGroupService.updateGroup(player, GroupEvent.UPDATE);
			}
			if (player.isInAlliance2()) {
				PlayerAllianceService.updateAlliance(player, PlayerAllianceEvent.UPDATE);
			}
		}
		this.stopTask(player);
	}

	/**
	 * 耗时统计方法名。
	 * Method name for runtime stats.
	 *
	 * Method name
	 */
	@Override
	protected String getCalledMethodName() {
		return "teamEffectUpdate()";
	}
}

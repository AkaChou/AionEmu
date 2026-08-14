package com.aionemu.gameserver.taskmanager.tasks;

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
 *
 * @author Sarynth
 */
public final class TeamMoveUpdater extends AbstractIterativePeriodicTaskManager<Player> {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<TeamMoveUpdater> instanceProvider;

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	private static final class SingletonHolder {

		/**
		 * 默认单例实例。
		 * Default singleton instance.
		 */
		private static final TeamMoveUpdater INSTANCE = new TeamMoveUpdater();
	}

	/**
	 * 获取单例：优先 Spring 提供者，否则静态 holder。
	 * Get the singleton: prefer Spring provider, otherwise the static holder.
	 *
	 * @return 更新器实例 / Updater instance
	 */
	public static TeamMoveUpdater getInstance() {
		ObjectProvider<TeamMoveUpdater> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
		}
		return SingletonHolder.INSTANCE;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param provider 实例提供者 / Provider
	 */
	public static void setInstanceProvider(ObjectProvider<TeamMoveUpdater> provider) {
		instanceProvider = provider;
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
	 *
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
	 *
	 * @return 方法名 / Method name
	 */
	@Override
	protected String getCalledMethodName() {
		return "teamMoveUpdate()";
	}
}

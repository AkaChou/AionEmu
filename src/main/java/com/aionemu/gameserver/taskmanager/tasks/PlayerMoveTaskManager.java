package com.aionemu.gameserver.taskmanager.tasks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.taskmanager.AbstractPeriodicTaskManager;

/**
 * 玩家移动推进任务：周期性调用移动控制器推进目标点。
 * Player movement advancement task: periodically advances move controllers toward destinations.
 *
 * @author ATracer
 */
public class PlayerMoveTaskManager extends AbstractPeriodicTaskManager {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<PlayerMoveTaskManager> instanceProvider;

	/**
	 * 正在移动的玩家/生物（objectId → 对象）。
	 * Moving players/creatures (objectId → creature).
	 */
	private final Map<Integer, Creature> movingPlayers = new ConcurrentHashMap<Integer, Creature>();

	/**
	 * 以 200ms 周期构造玩家移动任务管理器。
	 * Construct the player-move task manager with a 200ms period.
	 */
	public PlayerMoveTaskManager() {
		super(200);
	}

	/**
	 * 将玩家加入移动跟踪集合。
	 * Add a player to the moving set.
	 *
	 * @param player 玩家或生物 / Player or creature
	 */
	public void addPlayer(Creature player) {
		movingPlayers.put(player.getObjectId(), player);
	}

	/**
	 * 将玩家移出移动跟踪集合。
	 * Remove a player from the moving set.
	 *
	 * @param player 玩家或生物 / Player or creature
	 */
	public void removePlayer(Creature player) {
		movingPlayers.remove(player.getObjectId());
	}

	/**
	 * 对所有跟踪中的对象推进一次移动。
	 * Advance movement once for every tracked object.
	 */
	@Override
	public void run() {
		for (Creature player : movingPlayers.values()) {
			player.getMoveController().moveToDestination();
		}
	}

	/**
	 * 获取单例：优先 Spring 提供者，否则静态 holder。
	 * Get the singleton: prefer Spring provider, otherwise the static holder.
	 *
	 * @return 管理器实例 / Manager instance
	 */
	public static final PlayerMoveTaskManager getInstance() {
		ObjectProvider<PlayerMoveTaskManager> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<PlayerMoveTaskManager> provider) {
		instanceProvider = provider;
	}

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	private static final class SingletonHolder {

		/**
		 * 默认单例实例。
		 * Default singleton instance.
		 */
		private static final PlayerMoveTaskManager INSTANCE = new PlayerMoveTaskManager();
	}
}

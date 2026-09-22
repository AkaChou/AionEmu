package com.aionemu.gameserver.taskmanager.tasks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.taskmanager.AbstractPeriodicTaskManager;

/**
 * 玩家移动推进任务：周期性调用移动控制器推进目标点。
 * Player movement advancement task: periodically advances move controllers toward destinations.
 * @author ATracer
 */
public class PlayerMoveTaskManager extends AbstractPeriodicTaskManager {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
     * -- SETTER --
     *  注入 Spring 实例提供者。
     *  Inject the Spring instance provider.
     */
	@Setter
    private static volatile ObjectProvider<PlayerMoveTaskManager> instanceProvider;

	/**
	 * 正在移动的玩家/生物（objectId → 对象）。
	 * Moving players/creatures (objectId → creature).
	 */
	private final Map<Integer, Creature> movingPlayers = new ConcurrentHashMap<>();

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
	 * @param player 玩家或生物 / Player or creature
	 */
	public void addPlayer(Creature player) {
		movingPlayers.put(player.getObjectId(), player);
	}

	/**
	 * 将玩家移出移动跟踪集合。
	 * Remove a player from the moving set.
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
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static final PlayerMoveTaskManager getInstance() {
		ObjectProvider<PlayerMoveTaskManager> provider = instanceProvider;
		PlayerMoveTaskManager provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("PlayerMoveTaskManager 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

}

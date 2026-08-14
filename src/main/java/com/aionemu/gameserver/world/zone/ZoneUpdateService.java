package com.aionemu.gameserver.world.zone;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.taskmanager.AbstractFIFOPeriodicTaskManager;

/**
 * 区域刷新周期任务：FIFO 处理生物区域重算，并对玩家检查水位/死亡高度。
 * Zone refresh periodic task: FIFO revalidation of creature zones, and water/death-level checks for players.
 *
 * @author ATracer
 */
public class ZoneUpdateService extends AbstractFIFOPeriodicTaskManager<Creature> {
	/** 可选 Spring 单例提供者 / optional Spring singleton provider */
	private static volatile ObjectProvider<ZoneUpdateService> instanceProvider;

	/**
	 * 以 500ms 周期创建区域刷新服务。
	 * Create the zone-update service with a 500ms period.
	 */
	public ZoneUpdateService() {
		super(500);
	}

	/**
	 * 刷新生物区域，并对玩家执行水位/死亡高度检查。
	 * Refresh the creature's zones and run water/death-level checks for players.
	 *
	 * @param creature 待处理生物 / creature to process
	 */
	@Override
	protected void callTask(Creature creature) {
		creature.getController().refreshZoneImpl();
		if (creature instanceof Player) {
			ZoneLevelService.checkZoneLevels((Player) creature);
		}
	}

	/**
	 * 返回被调用方法名（用于任务诊断）。
	 * Return the called method name (for task diagnostics).
	 *
	 * @return 被调方法名 / the method name
	 */
	@Override
	protected String getCalledMethodName() {
		return "ZoneUpdateService()";
	}

	/**
	 * 获取单例：优先 Spring 提供者，否则回退内部持有者。
	 * Get the singleton: prefer Spring provider, otherwise fall back to the internal holder.
	 *
	 * @return 区域刷新服务 / zone update service
	 */
	public static ZoneUpdateService getInstance() {
		ObjectProvider<ZoneUpdateService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置 Spring 单例提供者。
	 * Set the Spring singleton provider.
	 *
	 * @param provider Spring 单例提供者 / the Spring singleton provider
	 */
	public static void setInstanceProvider(ObjectProvider<ZoneUpdateService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 内部单例持有者。
	 * Internal singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final ZoneUpdateService instance = new ZoneUpdateService();
	}
}

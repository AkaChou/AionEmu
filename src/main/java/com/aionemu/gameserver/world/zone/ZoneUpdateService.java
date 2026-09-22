package com.aionemu.gameserver.world.zone;

import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.taskmanager.AbstractFIFOPeriodicTaskManager;

/**
 * 区域刷新周期任务：FIFO 处理生物区域重算，并对玩家检查水位/死亡高度。
 * Zone refresh periodic task: FIFO revalidation of creature zones, and water/death-level checks for players.
 * @author ATracer
 */
public class ZoneUpdateService extends AbstractFIFOPeriodicTaskManager<Creature> {
	/** 可选 Spring 单例提供者 / optional Spring singleton provider
     * -- SETTER --
     *  设置 Spring 单例提供者。
     *  Set the Spring singleton provider.
     */
	@Setter
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
	 * @return 被调方法名 / the method name
	 */
	@Override
	protected String getCalledMethodName() {
		return "ZoneUpdateService()";
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
	public static ZoneUpdateService getInstance() {
		ObjectProvider<ZoneUpdateService> provider = instanceProvider;
		ZoneUpdateService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("ZoneUpdateService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

}

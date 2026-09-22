package com.aionemu.gameserver.services;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.road.Road;
import com.aionemu.gameserver.model.templates.road.RoadTemplate;

/**
 * 道路服务，根据静态数据生成并刷出道路实体。
 * Road service that creates and spawns road entities from static data.
 * @author SheppeR
 */
@Slf4j
public class RoadService {

    /**
     * -- SETTER --
     *  设置 Spring 实例提供者。
     *  Sets the Spring instance provider.
     */
    @Setter
    private static volatile ObjectProvider<RoadService> instanceProvider;

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
	public static final RoadService getInstance() {
		ObjectProvider<RoadService> provider = instanceProvider;
		RoadService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("RoadService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

    /**
	 * 构造并刷出全部道路实体。
	 * Constructs and spawns all road entities.
	 */
	public RoadService() {
		for (RoadTemplate rt : DataManager.ROAD_DATA.getRoadTemplates()) {
			Road r = new Road(rt);
			r.spawn();
			log.debug("Added " + r.getName() + " at m=" + r.getWorldId() + ",x=" + r.getX() + ",y=" + r.getY() + ",z="
					+ r.getZ());
		}
	}
}

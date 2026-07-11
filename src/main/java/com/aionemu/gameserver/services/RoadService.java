package com.aionemu.gameserver.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.road.Road;
import com.aionemu.gameserver.model.templates.road.RoadTemplate;

/**
 * 道路服务，根据静态数据生成并刷出道路实体。
 * Road service that creates and spawns road entities from static data.
 *
 * @author SheppeR
 */
@Slf4j
public class RoadService {

	private static volatile ObjectProvider<RoadService> instanceProvider;

	private static class SingletonHolder {

		protected static final RoadService instance = new RoadService();
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 */
	public static final RoadService getInstance() {
		ObjectProvider<RoadService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<RoadService> provider) {
		instanceProvider = provider;
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

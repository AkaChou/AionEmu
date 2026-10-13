package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.services.drop.DropService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import com.aionemu.gameserver.lifecycle.TestServiceProviders;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Spliterator;

import com.aionemu.gameserver.lifecycle.WorldServiceBindings;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LOOT_STATUS.Status;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class SM_LOOT_STATUSTest {

	private WorldServiceBindings worldServices;

	@BeforeEach
	void installRetiredServiceProvider() {
		DropRegistrationService dropRegistrationService = new DropRegistrationService(new DropService());
		TestServiceProviders.install(DropRegistrationService.class, dropRegistrationService);
		// D1a 后 holder 定位器不再回退到单例：显式构造世界服务门面，掉落状态包才能解析同一注册服务实例。
		// After D1a the holder locator no longer falls back to the singleton: construct the world-service
		// facade explicitly so the loot-status packet resolves the same registration instance.
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(DropRegistrationService.class.getName(), dropRegistrationService);
		worldServices = new WorldServiceBindings(null, null,
			beanFactory.getBeanProvider(DropRegistrationService.class));
	}

	@AfterEach
	void clearRetiredServiceProvider() {
		worldServices.destroy();
		TestServiceProviders.clear(DropRegistrationService.class);
	}

	@Test
	void readsDropItemsUnderTheirMutationLock() {
		int targetObjectId = Integer.MIN_VALUE;
		var dropMap = DropRegistrationService.getInstance().getCurrentDropMap();
		dropMap.put(targetObjectId, new LockCheckingSet());
		try {
			new SM_LOOT_STATUS(targetObjectId, Status.LOOT_ENABLE);
		} finally {
			dropMap.remove(targetObjectId);
		}
	}

	private static final class LockCheckingSet extends HashSet<DropItem> {
		@Override
		public Spliterator<DropItem> spliterator() {
			assertTrue(Thread.holdsLock(this), "drop items must be read under the same lock used by pickup writes");
			return super.spliterator();
		}
	}
}

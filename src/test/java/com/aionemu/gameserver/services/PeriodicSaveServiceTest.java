package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.PeriodicSaveConfig;
import com.aionemu.gameserver.persistence.dao.impl.AbyssRankDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.InventoryDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.ItemStoneDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerQuestDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerSkillDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerStigmasEquippedDaoImpl;

/**
 * 集中式周期存盘服务单元测试。
 * Unit test for centralized periodic save service.
 */
class PeriodicSaveServiceTest {

	@Test
	void initializesAndSchedulesAllGlobalTasks() throws ReflectiveOperationException {
		PeriodicSaveConfig.LEGION_ITEMS = 1200;
		PeriodicSaveConfig.PLAYER_GENERAL = 900;
		PeriodicSaveConfig.PLAYER_ITEMS = 900;

		PeriodicSaveService service = newService();

		Future<?> legionTask = getField(service, "legionWhUpdateTask");
		Future<?> generalTask = getField(service, "playerGeneralUpdateTask");
		Future<?> itemsTask = getField(service, "playerItemsUpdateTask");

		assertNotNull(legionTask, "军团仓库周期任务应成功调度");
		assertNotNull(generalTask, "玩家常规数据集中周期任务应成功调度");
		assertNotNull(itemsTask, "玩家物品数据集中周期任务应成功调度");

		assertFalse(legionTask.isCancelled());
		assertFalse(generalTask.isCancelled());
		assertFalse(itemsTask.isCancelled());
	}

	@Test
	void cancelAllTasksCancelsAndNullsAllFutures() throws ReflectiveOperationException {
		PeriodicSaveService service = newService();

		Method cancelMethod = PeriodicSaveService.class.getDeclaredMethod("cancelAllTasks");
		cancelMethod.setAccessible(true);
		cancelMethod.invoke(service);

		assertNull(getField(service, "legionWhUpdateTask"));
		assertNull(getField(service, "playerGeneralUpdateTask"));
		assertNull(getField(service, "playerItemsUpdateTask"));
	}

	@Test
	void savePlayerMethodsHandleNullGracefully() {
		PeriodicSaveService service = newService();
		assertDoesNotThrow(() -> service.savePlayerGeneral(null));
		assertDoesNotThrow(() -> service.savePlayerItems(null));
	}

	@SuppressWarnings("unchecked")
	private static PeriodicSaveService newService() {
		return new PeriodicSaveService(new AbyssRankDaoImpl(), new PlayerSkillDaoImpl(),
			new PlayerQuestDaoImpl(new org.springframework.transaction.support.TransactionTemplate()),
			new PlayerDaoImpl(), new PlayerStigmasEquippedDaoImpl(), new InventoryDaoImpl(), new ItemStoneDaoImpl());
	}

	private static <T> T getField(PeriodicSaveService service, String fieldName) throws ReflectiveOperationException {
		Field field = PeriodicSaveService.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(service);
	}
}

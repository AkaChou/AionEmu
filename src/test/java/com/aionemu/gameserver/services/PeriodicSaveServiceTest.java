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

		PeriodicSaveService service = new PeriodicSaveService();

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
		PeriodicSaveService service = new PeriodicSaveService();

		Method cancelMethod = PeriodicSaveService.class.getDeclaredMethod("cancelAllTasks");
		cancelMethod.setAccessible(true);
		cancelMethod.invoke(service);

		assertNull(getField(service, "legionWhUpdateTask"));
		assertNull(getField(service, "playerGeneralUpdateTask"));
		assertNull(getField(service, "playerItemsUpdateTask"));
	}

	@Test
	void savePlayerMethodsHandleNullGracefully() {
		PeriodicSaveService service = new PeriodicSaveService();
		assertDoesNotThrow(() -> service.savePlayerGeneral(null));
		assertDoesNotThrow(() -> service.savePlayerItems(null));
	}

	@SuppressWarnings("unchecked")
	private static <T> T getField(PeriodicSaveService service, String fieldName) throws ReflectiveOperationException {
		Field field = PeriodicSaveService.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(service);
	}
}

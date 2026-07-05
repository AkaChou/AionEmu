package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;

class AStationServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void accountIndexIsSafeForConcurrentMoveCallbacks() throws Exception {
		assertTrue(ConcurrentMap.class.isAssignableFrom(AStationService.class.getDeclaredField("accountsOnAStation").getType()));
	}

	@Test
	void checkAStationMoveKeepsEnterAndLeaveStateConsistent() throws Exception {
		AStationService service = objenesis.newInstance(AStationService.class);
		Map<Integer, Player> accounts = new ConcurrentHashMap<Integer, Player>();
		setField(service, "accountsOnAStation", accounts);
		Player player = player(1, "player");

		service.checkAStationMove(player, 100, false);

		assertTrue(player.isOnAStation());
		assertSame(player, accounts.get(100));

		service.checkAStationMove(player, 100, true);

		assertFalse(player.isOnAStation());
		assertFalse(accounts.containsKey(100));
	}

	@Test
	void duplicateAStationMoveDoesNotReaddPlayerAfterMovingBack() throws Exception {
		TestAStationService service = objenesis.newInstance(TestAStationService.class);
		Map<Integer, Player> accounts = new ConcurrentHashMap<Integer, Player>();
		setField(service, AStationService.class, "accountsOnAStation", accounts);
		Player player = player(1, "player");
		player.setOnAStation(true);
		accounts.put(100, player);

		service.checkAStationMove(player, 100, false);

		assertEquals(1, service.moveBackCalls);
		assertFalse(player.isOnAStation());
		assertFalse(accounts.containsKey(100));
	}

	private Player player(int objectId, String name) throws ReflectiveOperationException {
		Player player = objenesis.newInstance(Player.class);
		PlayerCommonData commonData = new PlayerCommonData(objectId);
		commonData.setName(name);
		setField(player, AionObject.class, "objectId", objectId);
		setField(player, Player.class, "playerCommonData", commonData);
		return player;
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		setField(target, target.getClass(), fieldName, value);
	}

	private static void setField(Object target, Class<?> owner, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static class TestAStationService extends AStationService {
		private int moveBackCalls;

		@Override
		public void handleMoveBack(Player player) {
			moveBackCalls++;
		}
	}
}

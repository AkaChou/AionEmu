package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.services.conquerors.Conqueror;
import com.aionemu.gameserver.services.protectors.Protector;
import com.aionemu.gameserver.world.WorldPosition;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class ProtectorConquerorServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();
	private boolean oldEnabled;
	private int oldDecrease;
	private Map<Integer, ProtectorConquerorService.WorldType> oldHandledWorlds;

	@BeforeEach
	void setUp() throws ReflectiveOperationException {
		oldEnabled = CustomConfig.PROTECTOR_CONQUEROR_ENABLE;
		oldDecrease = CustomConfig.PROTECTOR_CONQUEROR_DECREASE;
		oldHandledWorlds = new LinkedHashMap<Integer, ProtectorConquerorService.WorldType>(handledWorlds());
		CustomConfig.PROTECTOR_CONQUEROR_ENABLE = true;
		CustomConfig.PROTECTOR_CONQUEROR_DECREASE = 2;
		handledWorlds().clear();
		handledWorlds().put(110010000, ProtectorConquerorService.WorldType.ELYOS);
	}

	@AfterEach
	void tearDown() throws ReflectiveOperationException {
		CustomConfig.PROTECTOR_CONQUEROR_ENABLE = oldEnabled;
		CustomConfig.PROTECTOR_CONQUEROR_DECREASE = oldDecrease;
		handledWorlds().clear();
		handledWorlds().putAll(oldHandledWorlds);
	}

	@Test
	void decayRemovesExpiredEntriesWithoutConcurrentModification() throws Exception {
		ProtectorConquerorService service = new ProtectorConquerorService();
		Map<Integer, Protector> protectors = new LinkedHashMap<Integer, Protector>();
		protectors.put(1, protector(player(1), 1));
		protectors.put(2, protector(player(2), 1));
		Map<Integer, Conqueror> conquerors = new LinkedHashMap<Integer, Conqueror>();
		conquerors.put(3, conqueror(player(3), 1));
		conquerors.put(4, conqueror(player(4), 1));
		setField(service, "protectors", protectors);
		setField(service, "conquerors", conquerors);

		assertDoesNotThrow(() -> service.decayProtectorConquerorRanks());
		assertTrue(protectors.isEmpty());
		assertTrue(conquerors.isEmpty());
	}

	@Test
	void liveRankMapsUseConcurrentMapsForScheduledDecayAndKillUpdates() throws Exception {
		ProtectorConquerorService service = new ProtectorConquerorService();

		assertTrue(field(service, "protectors") instanceof ConcurrentMap);
		assertTrue(field(service, "conquerors") instanceof ConcurrentMap);
		assertTrue(field(service, "worldProtectors") instanceof ConcurrentMap);
		assertTrue(field(service, "worldConqueror") instanceof ConcurrentMap);
	}

	private Player player(int objectId) throws ReflectiveOperationException {
		Player player = objenesis.newInstance(Player.class);
		PlayerCommonData commonData = new PlayerCommonData(objectId);
		commonData.setRace(Race.ELYOS);
		setField(AionObject.class, player, "objectId", objectId);
		setField(Player.class, player, "playerCommonData", commonData);
		setField(VisibleObject.class, player, "position", new WorldPosition(110010000));
		return player;
	}

	private static Protector protector(Player owner, int victims) {
		Protector protector = new Protector(owner);
		protector.victims = victims;
		return protector;
	}

	private static Conqueror conqueror(Player owner, int victims) {
		Conqueror conqueror = new Conqueror(owner);
		conqueror.victims = victims;
		return conqueror;
	}

	@SuppressWarnings("unchecked")
	private static Map<Integer, ProtectorConquerorService.WorldType> handledWorlds() throws ReflectiveOperationException {
		Field field = ProtectorConquerorService.class.getDeclaredField("handledWorlds");
		field.setAccessible(true);
		return (Map<Integer, ProtectorConquerorService.WorldType>) field.get(null);
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		setField(target.getClass(), target, fieldName, value);
	}

	private static void setField(Class<?> type, Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = type.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Object field(Object target, String fieldName) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}
}

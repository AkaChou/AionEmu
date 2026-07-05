package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.trade.Exchange;

class ExchangeServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void exchangeIndexIsSafeForPacketAndLogoutThreads() throws Exception {
		assertTrue(ConcurrentMap.class.isAssignableFrom(ExchangeService.class.getDeclaredField("exchanges").getType()));
	}

	@Test
	void cancelExchangeRemovesBothSidesUnderOneExchangeMapLock() throws Exception {
		ExchangeService service = objenesis.newInstance(ExchangeService.class);
		LockAssertingExchangeMap exchanges = new LockAssertingExchangeMap();
		setField(service, "exchanges", exchanges);
		Player player = player(1, "player");
		Player partner = player(2, "partner");
		player.setTrading(true);
		partner.setTrading(true);
		exchanges.put(player.getObjectId(), new Exchange(player, partner));
		exchanges.put(partner.getObjectId(), new Exchange(partner, player));
		exchanges.assertRemoveHoldsMapLock = true;

		service.cancelExchange(player);

		assertFalse(service.isPlayerInExchange(player));
		assertFalse(service.isPlayerInExchange(partner));
		assertFalse(player.isTrading());
		assertFalse(partner.isTrading());
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

	private static final class LockAssertingExchangeMap extends ConcurrentHashMap<Integer, Exchange> {
		private boolean assertRemoveHoldsMapLock;

		@Override
		public Exchange remove(Object key) {
			if (assertRemoveHoldsMapLock && !Thread.holdsLock(this)) {
				throw new AssertionError("exchange pair cleanup must hold the exchange map lock");
			}
			return super.remove(key);
		}
	}
}

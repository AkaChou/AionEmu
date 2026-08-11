package com.aionemu.gameserver.model.gameobjects.player;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPendingQuestShareTest {
	@Test
	void serverIssuedShareCanBeConsumedOnlyOnce() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(player, "pendingQuestShares", ConcurrentHashMap.<Integer>newKeySet());

		player.addPendingQuestShare(28738);

		assertTrue(player.consumePendingQuestShare(28738));
		assertFalse(player.consumePendingQuestShare(28738));
		assertFalse(player.consumePendingQuestShare(99999));
		assertThrows(IllegalArgumentException.class, () -> player.addPendingQuestShare(0));
	}

	private static void setField(Player player, String name, Set<Integer> value) throws Exception {
		Field field = Player.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(player, value);
	}
}

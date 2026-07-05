package com.aionemu.gameserver.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CONQUEROR_PROTECTOR;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class SMConquerorProtectorTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void constructorCopiesPlayerCollection() throws ReflectiveOperationException {
		List<Player> players = new ArrayList<Player>();
		players.add(objenesis.newInstance(Player.class));

		SM_CONQUEROR_PROTECTOR packet = new SM_CONQUEROR_PROTECTOR(players);
		players.clear();

		assertEquals(1, players(packet).size());
	}

	@SuppressWarnings("unchecked")
	private static Collection<Player> players(SM_CONQUEROR_PROTECTOR packet) throws ReflectiveOperationException {
		Field field = SM_CONQUEROR_PROTECTOR.class.getDeclaredField("players");
		field.setAccessible(true);
		return (Collection<Player>) field.get(packet);
	}
}

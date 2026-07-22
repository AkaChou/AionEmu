package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeEntry;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;

class SMLunaShopTest {

	@Test
	void writesEachWardrobeEntryOnce() throws ReflectiveOperationException {
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setWardrobe(new PlayerWardrobeList(List.of(
				new PlayerWardrobeEntry(110101, 1, 0, PersistentState.UPDATED),
				new PlayerWardrobeEntry(110102, 2, 0, PersistentState.UPDATED))));
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		setField(connection, "activePlayer", new AtomicReference<>(player));

		SM_LUNA_SHOP packet = new SM_LUNA_SHOP(8, 2, 2);
		ByteBuffer buffer = ByteBuffer.allocate(64);
		packet.setBuf(buffer);
		packet.writeImpl(connection);
		buffer.flip();

		assertEquals(31, buffer.remaining());
		assertEquals(8, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(2, Byte.toUnsignedInt(buffer.get()));
		assertEquals(2, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(Set.of(110101, 110102), Set.of(readEntry(buffer), readEntry(buffer)));
		assertEquals(0, buffer.remaining());
	}

	private static int readEntry(ByteBuffer buffer) {
		buffer.get();
		int itemId = buffer.getInt();
		assertEquals(0, buffer.getInt());
		assertEquals(1, buffer.getInt());
		return itemId;
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}

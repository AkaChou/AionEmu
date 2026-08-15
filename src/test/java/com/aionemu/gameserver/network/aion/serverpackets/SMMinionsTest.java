package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.templates.minion.MinionDopingBag;

class SMMinionsTest {
	@Test
	void stopFunctionPacketHasNoPayload() {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		SM_MINIONS packet = new SM_MINIONS(10);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(10, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(0, buffer.remaining());
	}

	@Test
	void fullListWritesAllSixDopingSlots() throws Exception {
		MinionDopingBag bag = new MinionDopingBag();
		for (int slot = 0; slot < 6; slot++) {
			bag.setItem(1001 + slot, slot);
		}
		MinionCommonData commonData = new ObjenesisStd().newInstance(MinionCommonData.class);
		setField(commonData, "minionObjId", 10);
		setField(commonData, "minionId", 980020);
		setField(commonData, "masterObjectId", 20);
		setField(commonData, "name", "M");
		setField(commonData, "dopingBag", bag);

		ByteBuffer buffer = ByteBuffer.allocate(128);
		SM_MINIONS packet = new SM_MINIONS(0, List.of(commonData));
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(0, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
		for (int i = 0; i < 5; i++) {
			buffer.getInt();
		}
		while (buffer.getShort() != 0) {
		}
		buffer.position(buffer.position() + 12);
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		for (int slot = 0; slot < 6; slot++) {
			assertEquals(1001 + slot, buffer.getInt());
		}
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}

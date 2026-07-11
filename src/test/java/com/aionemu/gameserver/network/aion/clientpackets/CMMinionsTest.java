package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

class CMMinionsTest {

	@Test
	void readsAllFourFunctionParameters() throws Exception {
		CM_MINIONS packet = readFunctionPacket(0, 2, 1001, 3, 5);

		assertEquals(0, value(packet, "subSwitch"));
		assertEquals(2, value(packet, "functId"));
		assertEquals(1001, value(packet, "minionObjectId"));
		assertEquals(3, value(packet, "functionParam1"));
		assertEquals(5, value(packet, "functionParam2"));
	}

	@Test
	void autoLootUsesTheSameFixedPacketLayout() throws Exception {
		CM_MINIONS packet = readFunctionPacket(1, 1001, 0, 77, 88);

		assertEquals(1, value(packet, "subSwitch"));
		assertEquals(1001, value(packet, "functId"));
		assertEquals(0, value(packet, "minionObjectId"));
	}

	private static CM_MINIONS readFunctionPacket(int subSwitch, int first, int second, int third, int fourth) {
		ByteBuffer buffer = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) 9);
		buffer.putInt(subSwitch).putInt(first).putInt(second).putInt(third).putInt(fourth).flip();

		CM_MINIONS packet = new CM_MINIONS(0, State.IN_GAME);
		packet.setBuffer(buffer);
		assertTrue(packet.read());
		assertEquals(0, packet.getRemainingBytes());
		return packet;
	}

	private static int value(CM_MINIONS packet, String name) throws Exception {
		Field field = CM_MINIONS.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(packet);
	}
}

package com.aionemu.gameserver.network.loginserver.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class AccountVipAuthResponseTest {

	@Test
	void readsReturnFlagBeforeIndependentVipState() throws Exception {
		ByteBuffer buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(42);
		buffer.put((byte) 1);
		putS(buffer, "test");
		buffer.putLong(10);
		buffer.putLong(20);
		buffer.put((byte) 0);
		buffer.put((byte) 7);
		buffer.putLong(30);
		buffer.putLong(40);
		buffer.put((byte) 1);
		buffer.put((byte) 4);
		buffer.putLong(1035);
		buffer.flip();

		CM_ACOUNT_AUTH_RESPONSE packet = new CM_ACOUNT_AUTH_RESPONSE(1);
		packet.setBuffer(buffer);

		assertTrue(packet.read());
		assertEquals(0, packet.getRemainingBytes());
		assertEquals((byte) 4, field(packet, "vipLevel"));
		assertEquals(1035L, field(packet, "vipExp"));
	}

	private static Object field(Object instance, String name) throws Exception {
		Field field = instance.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(instance);
	}

	private static void putS(ByteBuffer buffer, String value) {
		for (int i = 0; i < value.length(); i++) {
			buffer.putChar(value.charAt(i));
		}
		buffer.putChar('\0');
	}
}

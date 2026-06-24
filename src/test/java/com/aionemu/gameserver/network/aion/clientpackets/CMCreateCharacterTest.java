package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.network.aion.AionConnection.State;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class CMCreateCharacterTest {

	@Test
	void readsCharacterCreationEntryRequestBeforeValidatingClassId() {
		CM_CREATE_CHARACTER packet = new CM_CREATE_CHARACTER(0x0153, State.AUTHED);
		packet.setBuffer(characterCreationEntryRequestWithClassId(93));

		assertTrue(packet.read());
	}

	private static ByteBuffer characterCreationEntryRequestWithClassId(int classId) {
		ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(0);
		putS(buffer, "");
		putS(buffer, "Test");
		putBytes(buffer, 50 - ("Test".length() * 2));
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(classId);
		putDefaultAppearance(buffer);
		buffer.put((byte) 1);
		buffer.flip();
		return buffer;
	}

	private static void putDefaultAppearance(ByteBuffer buffer) {
		for (int i = 0; i < 5; i++) {
			buffer.putInt(0);
		}
		for (int i = 0; i < 8; i++) {
			buffer.put((byte) 0);
		}
		buffer.putInt(0);
		for (int i = 0; i < 52; i++) {
			buffer.put((byte) 0);
		}
		buffer.putFloat(1.0f);
	}

	private static void putS(ByteBuffer buffer, String value) {
		for (int i = 0; i < value.length(); i++) {
			buffer.putChar(value.charAt(i));
		}
		buffer.putChar('\0');
	}

	private static void putBytes(ByteBuffer buffer, int length) {
		for (int i = 0; i < length; i++) {
			buffer.put((byte) 0);
		}
	}

}

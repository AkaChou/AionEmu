package com.aionemu.gameserver.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class EncryptionKeyPairTest {

	@Test
	void rejectsPacketsTooShortForValidationWithoutModifyingThem() {
		EncryptionKeyPair keyPair = new EncryptionKeyPair(0x12345678);
		for (int size = 0; size < 5; size++) {
			byte[] packet = new byte[size];
			assertFalse(keyPair.decrypt(ByteBuffer.wrap(packet)));
			assertArrayEquals(new byte[size], packet);
		}
	}
}

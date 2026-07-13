package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class SMCharBmPackListTest {

	@Test
	void writesLongLivedChinaVipBenefits() {
		assertArrayEquals(new byte[] {
			3, 3, 0,
			4, 1, 0, 0, 0, -1, -1, -1, 127,
			4, 2, 0, 0, 0, -1, -1, -1, 127,
			4, 3, 0, 0, 0, -1, -1, -1, 127
		}, payload(SM_CHAR_BM_PACK_LIST.vip(6)));
	}

	@Test
	void writesEmptyVipListForNonVipAccount() {
		assertArrayEquals(new byte[] { 3, 0, 0 }, payload(SM_CHAR_BM_PACK_LIST.vip(0)));
	}

	@Test
	void preservesExistingSubtypePayloads() {
		assertArrayEquals(new byte[] { 1, 0, 0 }, payload(new SM_CHAR_BM_PACK_LIST(1)));
		assertArrayEquals(new byte[] { 2, 1, 0, 2, (byte) 0xB8, 0x0B, 0, 0, (byte) 0xD2, (byte) 0xEC, 5, 0 },
				payload(new SM_CHAR_BM_PACK_LIST(2)));
	}

	@Test
	void rejectsUnknownVipStage() {
		assertThrows(IllegalArgumentException.class, () -> SM_CHAR_BM_PACK_LIST.vip(7));
	}

	private static byte[] payload(SM_CHAR_BM_PACK_LIST packet) {
		ByteBuffer buffer = ByteBuffer.allocate(64);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		byte[] payload = new byte[buffer.position()];
		buffer.flip();
		buffer.get(payload);
		return payload;
	}
}

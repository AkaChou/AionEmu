package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class SMCharBmPackListTest {

	@Test
	void charSelectEncodesScoreInDuration() {
		assertArrayEquals(new byte[] {
			3, 3, 0,
			4, 1, 0, 0, 0, (byte) 0xAF, 0x0E, 0, 0,
			4, 2, 0, 0, 0, (byte) 0xAF, 0x0E, 0, 0,
			4, 3, 0, 0, 0, (byte) 0xAF, 0x0E, 0, 0
		}, payload(SM_CHAR_BM_PACK_LIST.vipForCharSelect(6, 0L)));
	}

	@Test
	void inWorldUsesRemainingSecondsClampedAboveScoreRange() {
		byte[] p = payload(SM_CHAR_BM_PACK_LIST.vip(5, 100));
		// 100 < 3760 → clamped to 3760
		assertEquals(3760,
			(p[8] & 0xff) | ((p[9] & 0xff) << 8) | ((p[10] & 0xff) << 16) | ((p[11] & 0xff) << 24));

		p = payload(SM_CHAR_BM_PACK_LIST.vip(5, 86400));
		assertEquals(86400,
			(p[8] & 0xff) | ((p[9] & 0xff) << 8) | ((p[10] & 0xff) << 16) | ((p[11] & 0xff) << 24));
	}

	@Test
	void mapsLevelFiveToScore3758ForCharSelect() {
		assertEquals(3758, SM_CHAR_BM_PACK_LIST.resolveScore(5, 0L));
		byte[] p = payload(SM_CHAR_BM_PACK_LIST.vipForCharSelect(5, 0L));
		assertEquals(3758, (p[8] & 0xff) | ((p[9] & 0xff) << 8) | ((p[10] & 0xff) << 16) | ((p[11] & 0xff) << 24));
	}

	@Test
	void prefersExplicitVipExpOverLevelFloor() {
		assertEquals(2000, SM_CHAR_BM_PACK_LIST.resolveScore(5, 2000L));
	}

	@Test
	void writesEmptyVipListForNonVipAccount() {
		assertArrayEquals(new byte[] { 3, 0, 0 }, payload(SM_CHAR_BM_PACK_LIST.vip(0, 0)));
		assertArrayEquals(new byte[] { 3, 0, 0 }, payload(SM_CHAR_BM_PACK_LIST.vipForCharSelect(0, 0L)));
	}

	@Test
	void preservesExistingSubtypePayloads() {
		assertArrayEquals(new byte[] { 1, 0, 0 }, payload(new SM_CHAR_BM_PACK_LIST(1)));
		assertArrayEquals(new byte[] { 2, 1, 0, 2, (byte) 0xB8, 0x0B, 0, 0, (byte) 0xD2, (byte) 0xEC, 5, 0 },
				payload(new SM_CHAR_BM_PACK_LIST(2)));
	}

	@Test
	void rejectsUnknownVipStage() {
		assertThrows(IllegalArgumentException.class, () -> SM_CHAR_BM_PACK_LIST.vip(7, 1));
		assertThrows(IllegalArgumentException.class, () -> SM_CHAR_BM_PACK_LIST.vipForCharSelect(7, 0L));
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

package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.account.Account;

class SMNotifyVipIconPacketTest {

	@Test
	void clearsCustomIconOverride() {
		SM_NOTIFY_VIP_ICON packet = new SM_NOTIFY_VIP_ICON(0x12345678);

		assertEquals(0x163, packet.getOpcode());
		assertArrayEquals(new byte[] { 0x78, 0x56, 0x34, 0x12, 0, 0 }, payload(packet));
	}

	@Test
	void resolvesActiveVipStageForStandardIcon() {
		Account account = new Account(1);
		account.setVipLevel((byte) 4);
		account.setVipExpireTime(0);
		assertEquals(4, SM_PLAYER_INFO.activeVipLevel(account));

		account.setVipExpireTime(1);
		assertEquals(0, SM_PLAYER_INFO.activeVipLevel(account));
	}

	private static byte[] payload(SM_NOTIFY_VIP_ICON packet) {
		ByteBuffer buffer = ByteBuffer.allocate(6);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		return buffer.array();
	}
}

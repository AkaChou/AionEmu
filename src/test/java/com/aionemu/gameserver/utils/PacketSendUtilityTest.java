package com.aionemu.gameserver.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import org.junit.jupiter.api.Test;

/**
 * 校验封包发送工具对空目标玩家的容忍度。
 * Verifies that the packet utility tolerates a null target player.
 */
class PacketSendUtilityTest {

	@Test
	void sendPacketIgnoresNullPlayer() {
		assertDoesNotThrow(() -> PacketSendUtility.sendPacket(null, new SM_PLAY_MOVIE(0, 426)));
	}
}

package com.aionemu.gameserver.services.player;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PlayerEnterWorldVipTest {

	@Test
	void appliesVipBenefitsBeforeInitialStatsPacket() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/services/player/PlayerEnterWorldService.java"));
		int applyBenefits = source.indexOf("VipService.applyBenefits(player);");
		int sendItemInfos = source.indexOf("sendItemInfos(client, player);");

		assertTrue(applyBenefits >= 0);
		assertTrue(applyBenefits < sendItemInfos);
		assertEquals(applyBenefits, source.lastIndexOf("VipService.applyBenefits(player);"));
	}

	@Test
	void sendsClientVipBenefitsAfterBaseBenefitPack() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_LEVEL_READY.java"));
		int basePack = source.indexOf("new SM_CHAR_BM_PACK_LIST(1)");
		int vipPack = source.indexOf("PacketSendUtility.sendPacket(activePlayer, SM_CHAR_BM_PACK_LIST.vip(");

		assertTrue(basePack >= 0);
		assertTrue(basePack < vipPack);
		assertEquals(vipPack, source.lastIndexOf("PacketSendUtility.sendPacket(activePlayer, SM_CHAR_BM_PACK_LIST.vip("));
	}
}

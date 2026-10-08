package com.aionemu.gameserver.ai.instance.beshmundirTemple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.dataholders.Portal2Data;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.services.teleport.PortalService;

import jakarta.xml.bind.JAXBContext;

/**
 * 帕休曼迪尔寺院入口门（NPC 730231）回归：单人进入必须与 PortalService 的副本进入豁免口径一致
 * （管理员等级或会员特权），非豁免玩家继续被「此区域仅小队可进入」拦截。
 * Regression for the Beshmundir Temple entrance door (NPC 730231): solo entry must follow the
 * PortalService instance-entry exemption (admin level or membership perk), while non-exempt
 * players stay blocked by the group-only message.
 */
class BeshmundirsWalkAI2Test {

	private static final Path SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/beshmundirTemple/BeshmundirsWalkAI2.java");
	private static final Path PORTAL_TEMPLATES = Path.of(
			"src/main/resources/aion/data/static_data/portals/portal_template2.xml");

	@Test
	void adminLevelAndMembershipPerkBypassTheGroupRequirement() {
		int required = AdminConfig.INSTANCE_REQ;
		assertTrue(PortalService.canBypassInstanceGroupRequirement(required, false));
		assertFalse(PortalService.canBypassInstanceGroupRequirement(required - 1, false));
		assertTrue(PortalService.canBypassInstanceGroupRequirement(required - 1, true));
	}

	@Test
	void soloEntranceConsultsTheSharedExemptionAndKeepsTheGroupOnlyMessage() throws IOException {
		String case65 = case65Section(Files.readString(SOURCE));

		assertTrue(case65.contains("PortalService.canBypassInstanceGroupRequirement(player.getAccessLevel(),"),
				"单人分支必须走共享豁免判据 / the solo branch must consult the shared exemption");
		assertTrue(case65.contains("new SM_SYSTEM_MESSAGE(1390256)"),
				"非豁免玩家仍须收到仅小队提示 / non-exempt players keep the group-only message");
		assertTrue(case65.contains("new SM_DIALOG_WINDOW(getObjectId(), 4762)"),
				"豁免分支须弹难度选择 / the exempt branch opens the difficulty dialog");
	}

	@Test
	void entranceDoorCarriesTheSixPlayerInstanceRequirement() throws Exception {
		Portal2Data data = (Portal2Data) JAXBContext.newInstance(Portal2Data.class)
				.createUnmarshaller().unmarshal(PORTAL_TEMPLATES.toFile());

		assertNotNull(data.getPortalUse(730231), "missing portal_use for NPC 730231");
		PortalPath path = data.getPortalUse(730231).getPortalPath(Race.PC_ALL);
		assertNotNull(path, "missing portal path for NPC 730231");
		assertEquals(6, path.getPlayerCount());
		assertTrue(path.isInstance());
	}

	private static String case65Section(String source) {
		int start = source.indexOf("case 65:");
		int end = source.indexOf("case 4763:", start);
		assertTrue(start >= 0 && end > start, "case 65..case 4763 section must exist");
		return source.substring(start, end);
	}
}

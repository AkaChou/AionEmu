package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.templates.portal.PortalPath;

import jakarta.xml.bind.JAXBContext;

class PortalSourceWorldDataTest {

	@Test
	void selectsSharedScriptTransportRoutesBySourceWorld() throws Exception {
		Portal2Data data = (Portal2Data) JAXBContext.newInstance(Portal2Data.class).createUnmarshaller().unmarshal(
				Path.of("src/main/resources/aion/data/static_data/portals/portal_template2.xml").toFile());

		assertScriptTransport(data.getPortalDialog(730321, 10000, Race.ELYOS, 300200000), 3002002);
		assertScriptTransport(data.getPortalDialog(730321, 10000, Race.ELYOS, 302330000), 3023302);
		assertScriptTransport(data.getPortalDialog(730538, 10000, Race.ELYOS, 300240000), 3002401);
		assertScriptTransport(data.getPortalDialog(730538, 10000, Race.ELYOS, 300241000), 3002411);
		assertNull(data.getPortalDialog(730321, 10000, Race.ELYOS, 300240000));
		assertEquals(3005106, data.getPortalDialog(730641, 10000, Race.ELYOS, 300510000).getLocId());
		assertEquals(1352, data.getTeleportDialogId(730641));
		assertAlias(data.getPortalDialog(731811, 104, Race.ELYOS, 301540000), 3015433,
				"Alias_3rd_Boss_Room_In_1");
		assertAlias(data.getPortalDialog(731812, 104, Race.ELYOS, 301540000), 3015434,
				"Alias_4th_Boss_Room_In_2");
		PortalPath firstArchive = data.getPortalDialog(731809, 104, Race.ELYOS, 301540000);
		assertAlias(firstArchive, 3015431, "teleport_01");
		assertNull(firstArchive.getPortalReq());
		PortalPath secondArchive = data.getPortalDialog(731810, 104, Race.ELYOS, 301540000);
		assertAlias(secondArchive, 3015432, "teleport_02");
		assertNull(secondArchive.getPortalReq());
		assertNull(data.getPortalDialog(731809, 104, Race.ELYOS, 301540001));
		assertAlias(data.getPortalDialog(805744, 104, Race.ELYOS, 301520000), 3015200,
				"IDSeal_Q_Boss_Point");
		assertAlias(data.getPortalDialog(834188, 104, Race.ELYOS, 302100000), 3021001,
				"IDTransform_Save_Point_01");
		assertAlias(data.getPortalDialog(834188, 104, Race.ELYOS, 302110000), 3021101,
				"IDTransform_Save_Point_01");
		assertNull(data.getPortalDialog(834188, 104, Race.ELYOS, 301520000));
		assertAlias(data.getPortalDialog(832924, 104, Race.ELYOS, 301400000), 3014001,
				"Loc_Stage2_Enter");
		assertAlias(data.getPortalDialog(832925, 104, Race.ELYOS, 301590000), 3015901,
				"Loc_Stage2_Enter");
		assertNull(data.getPortalDialog(832925, 104, Race.ELYOS, 301400001));
		PortalPath invadeLight = data.getPortalDialog(834320, 10000, Race.ELYOS, 302200000);
		assertAlias(invadeLight, 3022001, "Location_Invade_Start_L");
		assertEquals(185000282, invadeLight.getPortalReq().getItemReq().get(0).getItemId());
		assertEquals(1403685, invadeLight.getPortalReq().getItemReq().get(0).getErrMessageId());
		assertFalse(invadeLight.getPortalReq().getItemReq().get(0).isConsume());
		PortalPath invadeDark = data.getPortalDialog(834270, 10000, Race.ASMODIANS, 302300000);
		assertAlias(invadeDark, 3023001, "Location_Invade_Start_D");
		assertEquals(185000283, invadeDark.getPortalReq().getItemReq().get(0).getItemId());
		assertEquals(1403685, invadeDark.getPortalReq().getItemReq().get(0).getErrMessageId());
		assertFalse(invadeDark.getPortalReq().getItemReq().get(0).isConsume());

		int[] groupArbiters = { 799573, 205426, 205427, 205428, 205429, 205430, 205431 };
		String[] groupAliases = { "STAGE_00_START", "STAGE_04_START", "STAGE_05_START", "STAGE_06_START",
				"STAGE_07_START", "STAGE_08_START", "STAGE_09_START" };
		for (int i = 0; i < groupArbiters.length; i++) {
			assertArenaReentry(data.getPortalDialog(groupArbiters[i], 10000, Race.ELYOS, 300300000),
					3003000, groupAliases[i], 186000124);
		}
		int[] soloArbiters = { 205682, 205683, 205684, 205685, 205663, 205686, 205687, 205664, 205665 };
		for (int i = 0; i < soloArbiters.length; i++) {
			assertArenaReentry(data.getPortalDialog(soloArbiters[i], 10000, Race.ELYOS, 300320000),
					3003200, "STAGE_0" + i + "_START", 186000134);
		}

		PortalPath existing = data.getPortalDialog(700088, 10000, Race.ELYOS, 310030000);
		assertNotNull(existing);
		assertEquals(TeleportAnimation.FIRE_ANIMATION, existing.getAnimation());
	}

	private static void assertScriptTransport(PortalPath path, int locId) {
		assertNotNull(path);
		assertEquals(locId, path.getLocId());
		assertEquals(TeleportAnimation.BEAM_ANIMATION, path.getAnimation());
	}

	private static void assertAlias(PortalPath path, int locId, String alias) {
		assertNotNull(path);
		assertEquals(locId, path.getLocId());
		assertEquals(alias, path.getDestinationAlias());
	}

	private static void assertArenaReentry(PortalPath path, int locId, String alias, int itemId) {
		assertAlias(path, locId, alias);
		assertEquals("JOIN_PLAY", path.getInstanceAction());
		var item = path.getPortalReq().getItemReq().get(0);
		assertEquals(itemId, item.getItemId());
		assertEquals(1, item.getItemCount());
		assertEquals(1097, item.getErrItem());
		assertTrue(item.isConsume());
	}
}

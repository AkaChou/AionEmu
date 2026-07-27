package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
		assertAlias(data.getPortalDialog(805744, 104, Race.ELYOS, 301520000), 3015200,
				"IDSeal_Q_Boss_Point");
		assertAlias(data.getPortalDialog(834188, 104, Race.ELYOS, 302100000), 3021001,
				"IDTransform_Save_Point_01");
		assertAlias(data.getPortalDialog(834188, 104, Race.ELYOS, 302110000), 3021101,
				"IDTransform_Save_Point_01");
		assertNull(data.getPortalDialog(834188, 104, Race.ELYOS, 301520000));

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
}

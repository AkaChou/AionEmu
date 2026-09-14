package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.Portal2Data;
import com.aionemu.gameserver.dataholders.PortalLocData;
import com.aionemu.gameserver.dataholders.TeleporterData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.portal.PortalDialog;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.portal.PortalReq;
import com.aionemu.gameserver.model.templates.portal.QuestReq;
import com.aionemu.gameserver.model.templates.teleport.TeleportLocation;
import com.aionemu.gameserver.model.templates.teleport.TeleporterTemplate;

import jakarta.xml.bind.JAXBContext;

/**
 * 锁定龙界（英吉斯温/格尔克马罗斯）传送门与空间移动师必须完成对应阵营的入场使命任务。
 * Locks Balaurea (Inggison/Gelkmaros) portals and teleporters to their racial entry mission completion.
 */
class BalaureaTeleporterQuestRequirementTest {

	private static final Path PORTAL_TEMPLATES = Path.of(
			"src/main/resources/aion/data/static_data/portals/portal_template2.xml");
	private static final Path PORTAL_LOCS = Path.of(
			"src/main/resources/aion/data/static_data/portals/portal_loc.xml");
	private static final Path TELEPORTER_XML = Path.of(
			"src/main/resources/aion/data/static_data/npc_teleporter.xml");

	@Test
	void inggisonAndGelkmarosPortalsRequireTheRacialEntryMission() throws Exception {
		Portal2Data data = (Portal2Data) JAXBContext.newInstance(Portal2Data.class)
				.createUnmarshaller().unmarshal(PORTAL_TEMPLATES.toFile());

		// 天族英吉斯温入口门户 -> 10031
		assertDialogPortal(data, 730529, 10000, 2101300, Race.ELYOS, 10031);
		assertDialogPortal(data, 730218, 10000, 2101300, Race.ELYOS, 10031);
		assertDialogPortal(data, 730425, 10000, 2101300, Race.ELYOS, 10031);
		assertDialogPortal(data, 730535, 10001, 2101300, Race.ELYOS, 10031);
		assertDialogPortal(data, 805606, 104, 2101300, Race.PC_ALL, 10031);

		// 魔族格尔克马罗斯入口门户 -> 20031
		assertDialogPortal(data, 730530, 10000, 2201400, Race.ASMODIANS, 20031);
		assertDialogPortal(data, 731833, 10000, 2201400, Race.ASMODIANS, 20031);
		assertDialogPortal(data, 730428, 10000, 2201400, Race.ASMODIANS, 20031);
		assertDialogPortal(data, 730536, 10001, 2201400, Race.ASMODIANS, 20031);
		assertDialogPortal(data, 805620, 104, 2201400, Race.PC_ALL, 20031);
	}

	@Test
	void masterInggisonUsesTheSpawnedTalocsHollowEntranceNpc() throws Exception {
		Portal2Data data = (Portal2Data) JAXBContext.newInstance(Portal2Data.class)
				.createUnmarshaller().unmarshal(PORTAL_TEMPLATES.toFile());

		assertTalocsHollowPortal(data, 799022);
		assertTalocsHollowPortal(data, 835605);
	}

	@Test
	void spaceTeleportersRequireTheRacialEntryMission() throws Exception {
		TeleporterData data = (TeleporterData) JAXBContext.newInstance(TeleporterData.class)
				.createUnmarshaller().unmarshal(TELEPORTER_XML.toFile());

		// 天族空间移动师前往英吉斯温外港（loc 313/96）需要 10031
		assertNpcTeleport(data, 203726, 313, 10031);
		assertNpcTeleport(data, 205806, 313, 10031);
		assertNpcTeleport(data, 730218, 96, 10031);
		assertNpcTeleport(data, 804782, 313, 10031);
		assertNpcTeleport(data, 205825, 313, 10031);

		// 魔族空间移动师前往格尔克马罗斯外港（loc 314/98）需要 20031
		assertNpcTeleport(data, 204191, 314, 20031);
		assertNpcTeleport(data, 205807, 314, 20031);
		assertNpcTeleport(data, 730219, 98, 20031);
		assertNpcTeleport(data, 804753, 314, 20031);
		assertNpcTeleport(data, 205846, 314, 20031);
	}

	@Test
	void externalBalaureaPortalsRequireTheRacialMissionOutsideInstances() {
		// 未完成 10031 时从圣天界进入英吉斯温被拦截
		assertFalse(PortalService.isBalaureaEntryAllowed(210050000, 110010000, false, false));

		// 英吉斯温内部跨地图/同区域传送放行
		assertTrue(PortalService.isBalaureaEntryAllowed(210050000, 210050000, false, false));

		// 副本出口放行
		assertTrue(PortalService.isBalaureaEntryAllowed(210050000, 300150000, true, false));

		// 完成使命后正常放行
		assertTrue(PortalService.isBalaureaEntryAllowed(210050000, 110010000, false, true));
	}

	@Test
	void inggisonPortalLocationsTargetTheLiveWorld() throws Exception {
		PortalLocData data = (PortalLocData) JAXBContext.newInstance(PortalLocData.class)
				.createUnmarshaller().unmarshal(PORTAL_LOCS.toFile());

		assertEquals(210050000, data.getPortalLoc(2100500).getWorldId());
		assertEquals(210050000, data.getPortalLoc(2101300).getWorldId());
	}

	@Test
	void inggisonTeleportsAlwaysResolveToTheLiveWorld() {
		assertEquals(210050000, TeleportService2.resolveInggisonWorldId(210130000));
		assertEquals(210050000, TeleportService2.resolveInggisonWorldId(210050000));
		assertEquals(210040000, TeleportService2.resolveInggisonWorldId(210040000));
	}

	private static void assertDialogPortal(Portal2Data data, int npcId, int dialogId, int locId, Race race, int questId) {
		PortalPath portalPath = data.getPortalDialog(npcId, dialogId, race);
		assertNotNull(portalPath, "missing portal path for NPC " + npcId + " dialog " + dialogId);
		assertEquals(locId, portalPath.getLocId());

		PortalReq portalReq = portalPath.getPortalReq();
		assertNotNull(portalReq, "missing portal_req for NPC " + npcId + " dialog " + dialogId);
		assertEquals(List.of(questId), portalReq.getQuestReq().stream().map(QuestReq::getQuestId).toList());
		assertEquals(List.of(3), portalReq.getQuestReq().stream().map(QuestReq::getQuestStep).toList());
	}

	private static void assertNpcTeleport(TeleporterData data, int npcId, int locId, int questId) {
		TeleporterTemplate template = data.getTeleporterTemplateByNpcId(npcId);
		assertNotNull(template, "missing teleporter template for NPC " + npcId);
		TeleportLocation location = template.getTeleLocIdData().getTeleportLocation(locId);
		assertNotNull(location, "missing telelocation " + locId + " for NPC " + npcId);
		assertEquals(questId, location.getRequiredQuest());
		assertEquals(3, location.getRequiredQuestStep());
	}

	private static void assertTalocsHollowPortal(Portal2Data data, int npcId) {
		PortalPath portalPath = data.getPortalDialog(npcId, 10000, Race.ELYOS);
		assertNotNull(portalPath, "missing Taloc's Hollow portal for NPC " + npcId);
		assertEquals(3001900, portalPath.getLocId());
		assertTrue(portalPath.isInstance());
		assertEquals(Race.ELYOS, portalPath.getRace());
		assertNotNull(portalPath.getPortalReq());
		assertEquals(51, portalPath.getPortalReq().getMinLevel());
	}
}

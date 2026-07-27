package com.aionemu.gameserver.services.teleport;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.List;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.InstanceConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.LocationAliasPoint;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.model.templates.portal.ItemReq;
import com.aionemu.gameserver.model.templates.portal.PortalLoc;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.portal.PortalReq;
import com.aionemu.gameserver.model.templates.portal.QuestReq;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.instance.InstanceAdmissionService;
import com.aionemu.gameserver.services.instance.InstanceAdmissionService.Admission;
import com.aionemu.gameserver.services.instance.InstanceLimitService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 传送门服务，校验副本/门户进入条件并完成组队注册与传送。
 * Portal service validating instance/portal entry requirements and performing team register + transfer.
 */
@Slf4j

public class PortalService {

	/**
	 * 按门户路径将玩家送入目标实例或区域。
	 * Ports a player into the target instance/area along a portal path.
	 *
	 * @param portalPath 门户路径模板 / Portal path template
	 * 玩家 / Player
	 * Interacting NPC object id
	 */
	public static void port(final PortalPath portalPath, final Player player, int npcObjectId) {
		if (!CustomConfig.ENABLE_INSTANCES) {
			return;
		}
		PortalLoc loc = DataManager.PORTAL_LOC_DATA.getPortalLoc(portalPath.getLocId());
		if (loc == null) {
			log.warn(I18n.get("log.e07f399d3e8d", portalPath.getLocId()));
			return;
		}
		LocationAliasPoint aliasDestination = null;
		if (!portalPath.getDestinationAlias().isBlank()) {
			List<LocationAliasPoint> points = DataManager.RETAIL_AI_DATA
					.findLocationAlias(loc.getWorldId(), portalPath.getDestinationAlias());
			if (points == null || points.isEmpty()) {
				log.warn(I18n.get("log.f35e2f1a0b67", portalPath.getDestinationAlias(), loc.getWorldId()));
				return;
			}
			aliasDestination = Rnd.get(points);
		}
		boolean instanceTitleReq = !player.havePermission(MembershipConfig.INSTANCES_TITLE_REQ);
		boolean instanceRaceReq = !player.havePermission(MembershipConfig.INSTANCES_RACE_REQ);
		boolean instanceQuestReq = !player.havePermission(MembershipConfig.INSTANCES_QUEST_REQ);
		boolean instanceGroupReq = !InstanceConfig.ALLOW_SOLO_ENTRY
				&& !player.havePermission(MembershipConfig.INSTANCES_GROUP_REQ);
		int mapId = loc.getWorldId();
		int playerSize = portalPath.getPlayerCount();
		boolean isInstance = portalPath.isInstance();
		if (instanceRaceReq && !checkRace(player, portalPath.getRace())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MOVE_PORTAL_ERROR_INVALID_RACE);
			return;
		}
		if (instanceGroupReq && !checkPlayerSize(player, portalPath, npcObjectId)) {
			return;
		}
		int siegeId = portalPath.getSiegeId();
		if (instanceRaceReq && siegeId != 0 && !checkSiegeId(player, siegeId)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MOVE_PORTAL_ERROR_INVALID_RACE);
			return;
		}
		PortalReq portalReq = portalPath.getPortalReq();
		if (portalReq != null) {
			if (!checkEnterLevel(player, mapId, portalReq, npcObjectId)) {
				return;
			}
			if (instanceQuestReq && !checkQuestsReq(player, npcObjectId, portalReq.getQuestReq())) {
				return;
			}
			if (!isInstance && !checkItemReq(player, npcObjectId, portalReq.getItemReq())) {
				return;
			}
			int titleId = portalReq.getTitleId();
			if (instanceTitleReq && titleId != 0) {
				if (!checkTitle(player, titleId)) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_DIRECT_PORTAL_NOT_TITLE);
					return;
				}
			}
			if (!isInstance && !checkKinah(player, portalReq.getKinahReq())) {
				return;
			}
		}
		if (!isInstance) {
			if (InstanceAdmissionService.chargeNonInstancePortal(portalPath, player)) {
				easyTransfer(player, loc, aliasDestination, portalPath.getAnimation());
			}
			return;
		}
		Admission admission = InstanceAdmissionService.admit(portalPath, loc, player);
		if (admission == null) {
			return;
		}
		try {
			if (!transfer(player, loc, aliasDestination, admission.instance(), admission.reentry(), portalPath.getAnimation())) {
				admission.rollback();
			}
		} catch (RuntimeException | Error e) {
			admission.rollback();
			throw e;
		}
	}

	private static boolean checkKinah(Player player, int kinah) {
		Storage inventory = player.getInventory();
		if (inventory.getKinah() < kinah) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_KINA(kinah));
			return false;
		}
		return true;
	}

	private static boolean checkEnterLevel(Player player, int mapId, PortalReq portalReq, int npcObjectId) {
		int enterMinLvl = portalReq.getMinLevel();
		int enterMaxLvl = portalReq.getMaxLevel();
		Row rule = InstanceLimitService.rule(mapId);
		if (rule != null) {
			String suffix = player.getRace() == Race.ELYOS ? "light" : "dark";
			enterMinLvl = rule.intValue("enter_min_level_" + suffix, enterMinLvl);
			enterMaxLvl = rule.intValue("enter_max_level_" + suffix, enterMaxLvl);
		}
		if (rule != null && player.isMentor()) {
			if (!rule.booleanValue("can_enter_mentor")) {
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_MSG_MENTOR_CANT_ENTER(com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).getName()));
				return false;
			}
		}
		if (!isLevelAllowed(player.getLevel(), enterMinLvl, enterMaxLvl)) {
			int errDialog = portalReq.getErrLevel();
			if (errDialog != 0) {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npcObjectId, errDialog));
			} else {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
			}
			return false;
		}
		return true;
	}

	static boolean isLevelAllowed(int playerLevel, int minLevel, int maxLevel) {
		return playerLevel >= minLevel && (maxLevel <= 0 || playerLevel <= maxLevel);
	}

	private static boolean checkPlayerSize(Player player, PortalPath portalPath, int npcObjectId) {
		int errDialog = portalPath.getErrGroup();
		int playerSize = portalPath.getPlayerCount();
		if (playerSize > 2 && playerSize <= 6) {
			if (!player.isInGroup2()) {
				if (errDialog != 0) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npcObjectId, errDialog));
				} else {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ENTER_ONLY_PARTY_DON);
				}
				return false;
			}
		} else if (playerSize > 6 && playerSize <= 24) {
			if (!player.isInAlliance2()) {
				if (errDialog != 0) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npcObjectId, errDialog));
				} else {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ENTER_ONLY_FORCE_DON);
				}
				return false;
			}
		} else if (playerSize > 24) {
			if (!player.isInLeague()) {
				if (errDialog != 0) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npcObjectId, errDialog));
				} else {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ENTER_ONLY_UNION_DON);
				}
				return false;
			}
		}
		return true;
	}

	private static boolean checkRace(Player player, Race portalRace) {
		return player.getRace().equals(portalRace) || portalRace.equals(Race.PC_ALL);
	}

	private static boolean checkSiegeId(Player player, int siegeId) {
		FortressLocation loc = GameFeatureServices.siegeService().getFortress(siegeId);
		if (loc != null && loc.getRace().getRaceId() != player.getRace().getRaceId()) {
			return false;
		}
		return true;
	}

	private static boolean checkTitle(Player player, int titleId) {
		return player.getCommonData().getTitleId() == titleId;
	}

	private static boolean checkQuestsReq(Player player, int npcObjectId, List<QuestReq> questReq) {
		if (questReq != null) {
			for (QuestReq quest : questReq) {
				int questId = quest.getQuestId();
				int questStep = quest.getQuestStep();
				final QuestState qs = player.getQuestStateList().getQuestState(questId);
				if (qs == null || (questStep == 0 && qs.getStatus() != QuestStatus.COMPLETE
						|| (qs.getQuestVarById(0) < quest.getQuestStep() && qs.getStatus() != QuestStatus.COMPLETE))) {
					int errDialog = quest.getErrQuest();
					if (errDialog != 0) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npcObjectId, errDialog));
					} else {
						PacketSendUtility.sendPacket(player,
								SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
					}
					return false;
				}
			}
		}
		return true;
	}

	private static boolean checkItemReq(Player player, int npcObjectId, List<ItemReq> itemReq) {
		if (itemReq != null) {
			Storage inventory = player.getInventory();
			for (ItemReq item : itemReq) {
				if (inventory.getItemCountByItemId(item.getItemId()) < item.getItemCount()) {
					int errDialog = item.getErrItem();
					if (errDialog != 0) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npcObjectId, errDialog));
					} else if (item.getErrMessageId() != 0) {
						PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(item.getErrMessageId()));
					} else {
						PacketSendUtility.sendPacket(player,
								SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM_TRY_LATER);
					}
					return false;
				}
			}
		}
		return true;
	}

	private static boolean transfer(Player player, PortalLoc loc, LocationAliasPoint aliasDestination,
			WorldMapInstance instance, boolean reenter,
			TeleportAnimation animation) {
		float x = aliasDestination == null ? loc.getX() : aliasDestination.x();
		float y = aliasDestination == null ? loc.getY() : aliasDestination.y();
		float z = aliasDestination == null ? loc.getZ() : aliasDestination.z();
		byte heading = aliasDestination == null ? loc.getH() : MathUtil.convertDegreeToHeading(aliasDestination.direction());
		player.setInstanceStartPos(x, y, z);
		if (!TeleportService2.teleportTo(player, loc.getWorldId(), instance.getInstanceId(), x, y, z, heading, animation)) {
			return false;
		}
		if (!reenter) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_DUNGEON_COUNT_USE);
		}
		DynamicInstance dynamic = instance.getDynamicInstance();
		if (dynamic == null) {
			throw new IllegalStateException("Instance has no retail dynamic state: " + loc.getWorldId());
		}
		Row definition = dynamic.getCreationId() == 0 ? null
				: DataManager.RETAIL_INSTANCE_DATA.definition(dynamic.getCreationId());
		if (dynamic.getOwnerType() == DynamicInstance.OWNER_PLAYER
				|| definition != null && definition.value("type").contains("PRIVATE")) {
			PlayerGroupService.removePlayer(player);
			PlayerAllianceService.removePlayer(player);
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401718));
		}
		return true;
	}

	private static void easyTransfer(Player player, PortalLoc loc, LocationAliasPoint aliasDestination,
			TeleportAnimation animation) {
		float x = aliasDestination == null ? loc.getX() : aliasDestination.x();
		float y = aliasDestination == null ? loc.getY() : aliasDestination.y();
		float z = aliasDestination == null ? loc.getZ() : aliasDestination.z();
		byte heading = aliasDestination == null ? loc.getH() : MathUtil.convertDegreeToHeading(aliasDestination.direction());
		TeleportService2.teleportTo(player, loc.getWorldId(), x, y, z, heading, animation);
	}
}

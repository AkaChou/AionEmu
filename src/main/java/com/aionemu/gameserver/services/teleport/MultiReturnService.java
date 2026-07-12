package com.aionemu.gameserver.services.teleport;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.portal.PortalLoc;
import com.aionemu.gameserver.services.instance.InstanceService;

/**
 * 多重回城服务，处理多目的地回城卷轴传送与世界 ID 映射。
 * Multi-return service handling multi-destination return-scroll teleports and world-id mapping.
 *
 * @author Rinzler (Encom)
 */
public class MultiReturnService {
	/**
	 * 按传送点 ID 将玩家传送到目标世界。
	 * Teleports the player to a target world by portal location id.
	 *
	 * 玩家 / Player
	 * Portal location id
	 * Target world id
	 */
	public static void Teleport(Player player, int LocId, int worldId) {
		InstanceService.onLeaveInstance(player);
		PortalLoc loc = DataManager.PORTAL_LOC_DATA.getPortalLoc(LocId);
		TeleportService2.teleportTo(player, worldId, loc.getX(), loc.getY(), loc.getZ(), player.getHeading(),
				TeleportAnimation.BEAM_ANIMATION);
	}

	/**
	 * 将世界地图 ID 映射为多重回城用的传送点/区域 ID。
	 * Maps a world map id to the multi-return teleport/zone id.
	 *
	 * World map id
	 *
	 * @param race 玩家种族（部分地图按种族区分） / Player race (some maps are race-split)
	 * @param race
	 * @return 映射后的 ID；未知地图返回 0 / Mapped id; 0 when unknown
	 */
	public static int getTeleportWorldId(int worldId, Race race) {
		switch (worldId) {
		// 天族 / Elyos
		case 110010000: // Sanctum.
			return 1100100;
		case 110070000: // Kaisinel Academy.
			return 1100702;
		case 210020000: // Eltnen.
			return 2100200;
		case 210030000: // Verteron.
			return 2100300;
		case 210040000: // Heiron.
			return 2100400;
		case 210130000: // Inggison.
			return 2100500;
		case 210060000: // Theobomos.
			return 2100600;
		case 210070000: // Cygnea.
			return 2100700;
		case 210100000: // Iluma.
			return 2101010;
		case 700010000: // Oriel.
			return 7000101;
		// 魔族 / Asmodians
		case 120010000: // Pandaemonium.
			return 1200100;
		case 120080000: // Marchutan Priory.
			return 1200800;
		case 220020000: // Morheim.
			return 2200200;
		case 220030000: // Altgard.
			return 2200300;
		case 220040000: // Beluslan.
			return 2200400;
		case 220050000: // Brusthonin.
			return 2200500;
		case 220140000: // Gelkmaros.
			return 2200700;
		case 220080000: // Enshar.
			return 2200800;
		case 220110000: // Norsvold.
			return 2201110;
		case 710010000: // Pernon.
			return 7100100;
		case 400010000: // Reshanta.
			return (race == Race.ELYOS ? 4000100 : 4000101);
		case 600100000: // Levinshor.
			return (race == Race.ELYOS ? 6001001 : 6001000);
		}
		return 0;
	}
}

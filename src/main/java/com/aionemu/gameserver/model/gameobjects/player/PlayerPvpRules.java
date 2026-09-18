package com.aionemu.gameserver.model.gameobjects.player;

import java.util.List;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 玩家 PvP 判定规则域。
 * Player PvP-rule domain.
 *
 * <p>该类型只服务 {@link Player}：负责世界 PvP 开关、PvP 禁区与同族 PvP 区域判定。
 * 公开关系入口仍保留在 {@link Player}，此处为无状态静态策略。
 * This type only serves {@link Player}: it owns world PvP flags, PvP-disabled zones and same-race
 * PvP zone checks. The public relation entry points stay on {@link Player}; this is a stateless
 * static policy.</p>
 */
final class PlayerPvpRules {

	private PlayerPvpRules() {
	}

	/**
	 * 判断两个玩家是否满足 PvP 规则。
	 * Decides whether two players satisfy the PvP rules.
	 *
	 * @param player 当前玩家 / current player
	 * @param enemy 目标玩家 / target player
	 * @return 满足 PvP 规则时为 true / true when PvP rules allow it
	 */
	static boolean canPvP(Player player, Player enemy) {
		int worldId = enemy.getWorldId();
		if (!enemy.getRace().equals(player.getRace())) {
			if (GameWorldBootstrapServices.world().getWorldMap(player.getWorldId()).isPvpAllowed()) {
				return !isInDisablePvPZone(player) && !isInDisablePvPZone(enemy);
			} else {
				return isInPvPZone(player) && isInPvPZone(enemy);
			}
		} else {
			if (worldId != 210020000 && // Elten.
					worldId != 210040000 && // Heiron.
					worldId != 210060000 && // Theobomos.
					worldId != 210070000 && // Cygnea.
					worldId != 210100000 && // Iluma.
					worldId != 210050000 && // Inggison.
					worldId != 220020000 && // Morheim.
					worldId != 220040000 && // Beluslan.
					worldId != 220050000 && // Brusthonin.
					worldId != 220080000 && // Enshar.
					worldId != 220110000 && // Norsvold.
					worldId != 220070000 && // Gelkmaros.
					worldId != 220140000 && // Gelkmaros [Master Server].
					// \\//\\//\\//\\//\\//
					worldId != 400010000 && // Reshanta.
					// \\//帕内斯特拉//\\// / \\//Panesterra//\\//
					worldId != 400020000 && // 贝洛斯 / Belus.
					worldId != 400040000 && // Aspida.
					worldId != 400050000 && // Atanatos.
					worldId != 400060000 && // Disillon.
					// \\//\\//\\//\\//\\//
					worldId != 600040000 && // Tiamaranta's Eye.
					worldId != 600041000 && // Tiamaranta's Eye [Master Server].
					worldId != 600050000 && // Katalam.
					worldId != 600090000 && // Kaldor.
					worldId != 600100000 && // Levinshor.
					worldId != 600010000 && // Silentera Canyon.
					worldId != 600110000) { // Silentera Canyon [Master Server].
				return player.isInsideZoneType(ZoneType.PVP)
						&& enemy.isInsideZoneType(ZoneType.PVP)
						&& !player.isInSameTeam(enemy);
			}
		}
		return false;
	}

	private static boolean isInDisablePvPZone(Player player) {
		List<ZoneInstance> zones = player.getPosition().getMapRegion().getZones(player);
		for (ZoneInstance zone : zones) {
			if (!zone.isPvpAllowed()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isInPvPZone(Player player) {
		List<ZoneInstance> zones = player.getPosition().getMapRegion().getZones(player);
		for (ZoneInstance zone : zones) {
			if (!zone.isPvpAllowed()) {
				return true;
			}
		}
		return false;
	}
}

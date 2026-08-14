package com.aionemu.gameserver.services.siegeservice;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.dao.SiegeDAO;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SIEGE_LOCATION_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
/**
 * 攻城自动种族切换，按计划自动变更要塞归属。
 * Siege auto-race switch automatically changing fortress ownership on schedule.
 */
@Slf4j(topic = "SIEGE_LOG")

public class SiegeAutoRace {
	private static String[] siegeIds = SiegeConfig.SIEGE_AUTO_LOCID.split(";");

	/**
	 * 执行自动种族切换。
	 * Runs auto race switch.
	 *
	 * @param locid 据点 ID / location id
	 */
	public static void AutoSiegeRace(final int locid) {
		final SiegeLocation loc = GameFeatureServices.siegeService().getSiegeLocation(locid);
		if (!loc.getRace().equals(SiegeRace.ASMODIANS) || !loc.getRace().equals(SiegeRace.ELYOS)) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					GameFeatureServices.siegeService().startSiege(locid);
				}
			}, 300000);
			GameFeatureServices.siegeService().deSpawnNpcs(locid);
			final int oldOwnerRaceId = loc.getRace().getRaceId();
			final int legionId = loc.getLegionId();
			final String legionName = legionId != 0 ? GameCoreGameplayServices.legionService().getLegion(legionId).getLegionName()
					: "";
			final DescriptionId NameId = new DescriptionId(loc.getTemplate().getNameId());
			if (ElyosAutoSiege(locid)) {
				loc.setRace(SiegeRace.ELYOS);
			}
			if (AsmoAutoSiege(locid)) {
				loc.setRace(SiegeRace.ASMODIANS);
			}
			loc.setLegionId(0);
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				/**
				 * visit 方法。
				 * visit method.
				 *
				 * @param player 玩家 / player
				 */
				public void visit(Player player) {
					if (legionId != 0 && player.getRace().getRaceId() == oldOwnerRaceId) {
						// %0 征服了 %1。 / %0 has conquered %1.
						PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1301038, legionName, NameId));
					}
					// %0 成功征服了 %1。 / %0 succeeded in conquering %1.
					PacketSendUtility.sendPacket(player,
							new SM_SYSTEM_MESSAGE(1404542, loc.getRace().getDescriptionId(), NameId));
					PacketSendUtility.sendPacket(player, new SM_SIEGE_LOCATION_INFO(loc));
				}
			});
			if (ElyosAutoSiege(locid)) {
				GameFeatureServices.siegeService().spawnNpcs(locid, SiegeRace.ELYOS, SiegeModType.PEACE);
			} else if (AsmoAutoSiege(locid)) {
				GameFeatureServices.siegeService().spawnNpcs(locid, SiegeRace.ASMODIANS, SiegeModType.PEACE);
			}
			DAOManager.getDAO(SiegeDAO.class).updateSiegeLocation(loc);
		}
		GameFeatureServices.siegeService().broadcastUpdate(loc);
	}

	/**
	 * 判断据点是否参与自动攻城。
	 * Returns whether the location is auto-sieged.
	 *
	 * @param locId 据点 ID / location id
	 * @return 是否自动攻城 / whether auto siege
	 */
	public static boolean isAutoSiege(int locId) {
		return ElyosAutoSiege(locId) || AsmoAutoSiege(locId);
	}

	/**
	 * 光之部自动归属。
	 * Elyos auto ownership.
	 *
	 * @param locId 据点 ID / location id
	 * @return 是否天族自动归属 / whether Elyos auto ownership
	 */
	public static boolean ElyosAutoSiege(int locId) {
		for (String id : siegeIds[0].split(",")) {
			if (locId == Integer.parseInt(id)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 暗之部自动归属。
	 * Asmodian auto ownership.
	 *
	 * @param locId 据点 ID / location id
	 * @return 是否魔族自动归属 / whether Asmodian auto ownership
	 */
	public static boolean AsmoAutoSiege(int locId) {
		for (String id : siegeIds[1].split(",")) {
			if (locId == Integer.parseInt(id)) {
				return true;
			}
		}
		return false;
	}
}
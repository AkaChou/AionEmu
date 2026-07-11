package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.SiegeDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.siege.*;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.siegeservice.BalaurAssaultService;
import com.aionemu.gameserver.services.siegeservice.Siege;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 攻城管理指令；启停攻城、列出据点、强制占领与发起龙族突袭。
 * Admin command for starting/stopping sieges, listing locations, forced capture and Balaur assaults.
 */
@SuppressWarnings("rawtypes")
public class SiegeCommand extends AdminCommand {

	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";
	private static final String COMMAND_LIST = "list";
	private static final String COMMAND_LIST_LOCATIONS = "locations";
	private static final String COMMAND_LIST_SIEGES = "sieges";
	private static final String COMMAND_CAPTURE = "capture";
	private static final String COMMAND_ASSAULT = "assault";
	
	public SiegeCommand() {
		super("siegecommand");
	}
	
	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param player 执行指令的管理员 / admin executing the command
	 * command arguments
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStopSiege(player, params);
		} else if (COMMAND_LIST.equalsIgnoreCase(params[0])) {
			handleList(player, params);
		} else if (COMMAND_LIST_SIEGES.equals(params[0])) {
			listLocations(player);
		} else if (COMMAND_CAPTURE.equals(params[0])) {
			capture(player, params);
		} else if (COMMAND_ASSAULT.equals(params[0])) {
			assault(player, params);
		}
	}
	
	protected void handleStartStopSiege(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int siegeLocId = NumberUtils.toInt(params[1]);
		if (!isValidSiegeLocationId(player, siegeLocId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameFeatureServices.siegeService().isSiegeInProgress(siegeLocId)) {
				PacketSendUtility.sendMessage(player, "Siege Location " + siegeLocId + " is already under siege");
			} else {
				PacketSendUtility.sendMessage(player, "Siege Location " + siegeLocId + " - starting siege!");
				GameFeatureServices.siegeService().startSiege(siegeLocId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameFeatureServices.siegeService().isSiegeInProgress(siegeLocId)) {
				PacketSendUtility.sendMessage(player, "Siege Location " + siegeLocId + " is not under siege");
			} else {
				PacketSendUtility.sendMessage(player, "Siege Location " + siegeLocId + " - stopping siege!");
				GameFeatureServices.siegeService().stopSiege(siegeLocId);
			}
		}
	}
	
	protected boolean isValidSiegeLocationId(Player player, int fortressId) {
		if (!GameFeatureServices.siegeService().getSiegeLocations().keySet().contains(fortressId)) {
			PacketSendUtility.sendMessage(player, "Id " + fortressId + " is invalid");
			return false;
		}
		return true;
	}
	
	protected void handleList(Player player, String[] params) {
		if (params.length != 2) {
			showHelp(player);
			return;
		} if (COMMAND_LIST_LOCATIONS.equalsIgnoreCase(params[1])) {
			listLocations(player);
		} else if (COMMAND_LIST_SIEGES.equalsIgnoreCase(params[1])) {
			listSieges(player);
		} else {
			showHelp(player);
		}
	}
	
	protected void listLocations(Player player) {
		for (FortressLocation f : GameFeatureServices.siegeService().getFortresses().values()) {
			PacketSendUtility.sendMessage(player, "Fortress: " + f.getLocationId() + " belongs to " + f.getRace());
		} for (ArtifactLocation a : GameFeatureServices.siegeService().getStandaloneArtifacts().values()) {
			PacketSendUtility.sendMessage(player, "Artifact: " + a.getLocationId() + " belongs to " + a.getRace());
		}
	}
	
	protected void listSieges(Player player) {
		for (Integer i : GameFeatureServices.siegeService().getSiegeLocations().keySet()) {
			Siege s = GameFeatureServices.siegeService().getSiege(i);
			if (s != null) {
				int secondsLeft = GameFeatureServices.siegeService().getRemainingSiegeTimeInSeconds(i);
				String minSec = secondsLeft / 60 + "m ";
				minSec += secondsLeft % 60 + "s";
				PacketSendUtility.sendMessage(player, "Location: " + i + ": " + minSec + " left.");
			}
		}
	}
	
	protected void capture(Player player, String[] params) {
		if (params.length < 3 || !NumberUtils.isCreatable(params[1])) {
			showHelp(player);
			return;
		}
		int siegeLocationId = NumberUtils.toInt(params[1]);
		if (!GameFeatureServices.siegeService().getSiegeLocations().keySet().contains(siegeLocationId)) {
			PacketSendUtility.sendMessage(player, "Invalid Siege Location Id: " + siegeLocationId);
			return;
		}
		SiegeRace sr = null;
		try {
			sr = SiegeRace.valueOf(params[2].toUpperCase());
		}
		catch (IllegalArgumentException e) {
		}
		Legion legion = null;
		if (sr == null) {
			try {
				int legionId = Integer.valueOf(params[2]);
				legion = GameCoreGameplayServices.legionService().getLegion(legionId);
			} catch (NumberFormatException e) {
				String legionName = "";
				for (int i = 2; i < params.length; i++)
					legionName += " " + params[i];
				legion = GameCoreGameplayServices.legionService().getLegion(legionName.trim());
			} if (legion != null) {
				int legionBGeneral = GameCoreGameplayServices.legionService().getLegionBGeneral(legion.getLegionId());
				if (legionBGeneral != 0) {
					PlayerCommonData BGeneral = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(legionBGeneral);
					sr = SiegeRace.getByRace(BGeneral.getRace());
				}
			}
		} if (legion == null && sr == null) {
			PacketSendUtility.sendMessage(player, params[2] + " is not valid siege race or legion name");
			return;
		}
		SiegeLocation loc = GameFeatureServices.siegeService().getSiegeLocation(siegeLocationId);
		Siege s = GameFeatureServices.siegeService().getSiege(siegeLocationId);
		if (s != null) {
			s.getSiegeCounter().addRaceDamage(sr, s.getBoss().getLifeStats().getMaxHp() + 1);
			s.setBossKilled(true);
			GameFeatureServices.siegeService().stopSiege(siegeLocationId);
			loc.setLegionId(legion != null ? legion.getLegionId() : 0);
		} else {
			GameFeatureServices.siegeService().deSpawnNpcs(siegeLocationId);
			loc.setVulnerable(false);
			loc.setUnderShield(false);
			loc.setRace(sr);
			loc.setLegionId(legion != null ? legion.getLegionId() : 0);
			GameFeatureServices.siegeService().spawnNpcs(siegeLocationId, sr, SiegeModType.PEACE);
			DAOManager.getDAO(SiegeDAO.class).updateSiegeLocation(loc);
			switch (siegeLocationId) {
				// 希尔西要塞。 / Siel's Western Fortress.
				case 1131:
					if (loc.getRace() == SiegeRace.ASMODIANS) {
						// 在卡普斯岛的谢林。 / Shairing At Carpus Isle.
						GameFeatureServices.baseService().capture(108, Race.ASMODIANS);
						// 在希尔左翼的博米雄。 / Bomishung At Siel's Left Wing.
						GameFeatureServices.baseService().capture(109, Race.ASMODIANS);
					} else if (loc.getRace() == SiegeRace.ELYOS) {
						// 在卡普斯岛的谢林。 / Shairing At Carpus Isle.
						GameFeatureServices.baseService().capture(108, Race.ELYOS);
						// 在希尔左翼的博米雄。 / Bomishung At Siel's Left Wing.
						GameFeatureServices.baseService().capture(109, Race.ELYOS);
					} else if (loc.getRace() == SiegeRace.BALAUR) {
						// 在卡普斯岛的谢林。 / Shairing At Carpus Isle.
						GameFeatureServices.baseService().capture(108, Race.NPC);
						// 在希尔左翼的博米雄。 / Bomishung At Siel's Left Wing.
						GameFeatureServices.baseService().capture(109, Race.NPC);
					}
				break;
				// 希尔东要塞。 / Siel's Eastern Fortress.
				case 1132:
					if (loc.getRace() == SiegeRace.ASMODIANS) {
						// 在希尔右翼的萨斯明。 / Sasming At Siel's Right Wing.
						GameFeatureServices.baseService().capture(110, Race.ASMODIANS);
					} else if (loc.getRace() == SiegeRace.ELYOS) {
						// 在希尔右翼的萨斯明。 / Sasming At Siel's Right Wing.
						GameFeatureServices.baseService().capture(110, Race.ELYOS);
					} else if (loc.getRace() == SiegeRace.BALAUR) {
						// 在希尔右翼的萨斯明。 / Sasming At Siel's Right Wing.
						GameFeatureServices.baseService().capture(110, Race.NPC);
					}
				break;
				// 硫磺要塞。 / Sulfur Fortress.
				case 1141:
					if (loc.getRace() == SiegeRace.ASMODIANS) {
						// 在硫磺群岛的奥哈隆。 / Oharung At The Sulfur Archipelago.
						GameFeatureServices.baseService().capture(105, Race.ASMODIANS);
						// 在西风岛的乔阿林。 / Joarin At Zephyr Island.
					    GameFeatureServices.baseService().capture(106, Race.ASMODIANS);
						// 在雷博岛的特米伦。 / Temirun At Leibo Island.
					    GameFeatureServices.baseService().capture(107, Race.ASMODIANS);
					} else if (loc.getRace() == SiegeRace.ELYOS) {
						// 在硫磺群岛的奥哈隆。 / Oharung At The Sulfur Archipelago.
						GameFeatureServices.baseService().capture(105, Race.ELYOS);
						// 在西风岛的乔阿林。 / Joarin At Zephyr Island.
					    GameFeatureServices.baseService().capture(106, Race.ELYOS);
						// 在雷博岛的特米伦。 / Temirun At Leibo Island.
					    GameFeatureServices.baseService().capture(107, Race.ELYOS);
					} else if (loc.getRace() == SiegeRace.BALAUR) {
						// 在硫磺群岛的奥哈隆。 / Oharung At The Sulfur Archipelago.
						GameFeatureServices.baseService().capture(105, Race.NPC);
						// 在西风岛的乔阿林。 / Joarin At Zephyr Island.
					    GameFeatureServices.baseService().capture(106, Race.NPC);
						// 在雷博岛的特米伦。 / Temirun At Leibo Island.
					    GameFeatureServices.baseService().capture(107, Race.NPC);
					}
				break;
			}
		}
		GameFeatureServices.siegeService().broadcastUpdate(loc);
	}
	
	protected void assault(Player player, String[] params) {
		if (params.length < 2 || (!NumberUtils.isCreatable(params[1]) && !NumberUtils.isCreatable(params[2]))) {
			showHelp(player);
			return;
		}
		int siegeLocationId = NumberUtils.toInt(params[1]);
		int delay = NumberUtils.toInt(params[2]);
		if (!GameFeatureServices.siegeService().getSiegeLocations().keySet().contains(siegeLocationId)) {
			PacketSendUtility.sendMessage(player, "Invalid Siege Location Id: " + siegeLocationId);
			return;
		}
		GameCoreGameplayServices.balaurAssaultService().startAssault(player, siegeLocationId, delay);
	}
	
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //siegecommand Help\n" + "//siegecommand start|stop <LocationId>\n" + "//siegecommand list locations|sieges\n" + "//siegecommand capture <LocationId> <siegeRaceName|legionName|legionId>\n" + "//siegecommand assault <LocationId> <delaySec>");
		java.util.Set<Integer> fortressIds = GameFeatureServices.siegeService().getFortresses().keySet();
		java.util.Set<Integer> artifactIds = GameFeatureServices.siegeService().getStandaloneArtifacts().keySet();
		PacketSendUtility.sendMessage(player, "Fortress: " + StringUtils.join(fortressIds, ", "));
		PacketSendUtility.sendMessage(player, "Artifacts: " + StringUtils.join(artifactIds, ", "));
	}
}

package com.aionemu.gameserver.commands.admin;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.SpawnsData2;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.WorldMapType;
import com.aionemu.gameserver.world.knownlist.Visitor;
import lombok.extern.slf4j.Slf4j;

/**
 * 刷怪点热重载指令；按地图名或全部重载静态刷新数据并重生世界 NPC。
 * Admin command that hot-reloads spawn data for a map or all maps and re-spawns world NPCs.
 */
@Slf4j
public class ReloadSpawn extends AdminCommand
{
	public ReloadSpawn() {
		super("reload_spawn");
	}
	
	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param player 执行指令的管理员 / admin executing the command
	 */
	@Override
	public void execute(Player player, String... params) {
		int worldId;
		String destination;
		worldId = 0;
		destination = "null";
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(player, "syntax //reload_spawn <location name | all>");
			return;
		}
		else {
			StringBuilder sbDestination = new StringBuilder();
			for(String p : params)
				sbDestination.append(p + " ");
		
			destination = sbDestination.toString().trim();
		// 天族。 / ELYOS.
		if (destination.equalsIgnoreCase("Sanctum"))
			worldId = WorldMapType.SANCTUM.getId();
		else if (destination.equalsIgnoreCase("Kaisinel"))
			worldId = WorldMapType.KAISINEL.getId();
		else if (destination.equalsIgnoreCase("Academy"))
			worldId = WorldMapType.KAISINEL_ACADEMY.getId();
		else if (destination.equalsIgnoreCase("Abbey1"))
			worldId = WorldMapType.WISPLIGHT_ABBEY.getId();
		else if (destination.equalsIgnoreCase("Poeta"))
			worldId = WorldMapType.POETA.getId();
		else if (destination.equalsIgnoreCase("Verteron"))
			worldId = WorldMapType.VERTERON.getId();
		else if (destination.equalsIgnoreCase("Eltnen"))
			worldId = WorldMapType.ELTNEN.getId();
		else if (destination.equalsIgnoreCase("Theobomos"))
			worldId = WorldMapType.THEOBOMOS.getId();
		else if (destination.equalsIgnoreCase("Heiron"))
			worldId = WorldMapType.HEIRON.getId();
		else if (destination.equalsIgnoreCase("Inggison"))
			worldId = WorldMapType.INGGISON.getId();
		else if (destination.equalsIgnoreCase("Cygnea"))
			worldId = WorldMapType.CYGNEA.getId();
		else if (destination.equalsIgnoreCase("Idian_L"))
			worldId = WorldMapType.IDIAN_DEPTHS_L.getId();
		else if (destination.equalsIgnoreCase("Iluma"))
			worldId = WorldMapType.ILUMA.getId();
		else if (destination.equalsIgnoreCase("Tower_Of_Eternity_E"))
			worldId = WorldMapType.TOWER_OF_ETERNITY_E.getId();
		// 魔族。 / ASMODIANS.
		else if  (destination.equalsIgnoreCase("Pandaemonium"))
			worldId = WorldMapType.PANDAEMONIUM.getId();
		else if (destination.equalsIgnoreCase("Marchutan"))
			worldId = WorldMapType.MARCHUTAN.getId();
		else if (destination.equalsIgnoreCase("Priory"))
			worldId = WorldMapType.MARCHUTAN_PRIORY.getId();
		else if (destination.equalsIgnoreCase("Abbey2"))
			worldId = WorldMapType.FATEBOUND_ABBEY.getId();
		else if (destination.equalsIgnoreCase("Ishalgen"))
			worldId = WorldMapType.ISHALGEN.getId();
		else if (destination.equalsIgnoreCase("Altgard"))
			worldId = WorldMapType.ALTGARD.getId();
		else if (destination.equalsIgnoreCase("Morheim"))
			worldId = WorldMapType.MORHEIM.getId();
		else if (destination.equalsIgnoreCase("Brusthonin"))
			worldId = WorldMapType.BRUSTHONIN.getId();
		else if (destination.equalsIgnoreCase("Beluslan"))
			worldId = WorldMapType.BELUSLAN.getId();
		else if (destination.equalsIgnoreCase("Gelkmaros"))
			worldId = WorldMapType.GELKMAROS.getId();
		else if (destination.equalsIgnoreCase("Enshar"))
			worldId = WorldMapType.ENSHAR.getId();
		else if (destination.equalsIgnoreCase("Idian_D"))
			worldId = WorldMapType.IDIAN_DEPTHS_D.getId();
		else if (destination.equalsIgnoreCase("Norsvold"))
			worldId = WorldMapType.NORSVOLD.getId();
		else if (destination.equalsIgnoreCase("Tower_Of_Eternity_A"))
			worldId = WorldMapType.TOWER_OF_ETERNITY_A.getId();
		// 其他区域 / Other Zone
		else if (destination.equalsIgnoreCase("Silentera"))
			worldId = WorldMapType.SILENTERA_CANYON.getId();
		else if (destination.equalsIgnoreCase("Kaldor"))
			worldId = WorldMapType.KALDOR.getId();
		else if (destination.equalsIgnoreCase("Levinshor"))
			worldId = WorldMapType.LEVINSHOR.getId();
		// 雷珊塔 / Reshanta
		else if (destination.equalsIgnoreCase("Reshanta"))
			worldId = WorldMapType.RESHANTA.getId();
		// 帕内斯特拉 / Panesterra
		else if (destination.equalsIgnoreCase("Belus"))
			worldId = WorldMapType.BELUS.getId();
		else if (destination.equalsIgnoreCase("Aspida"))
			worldId = WorldMapType.ASPIDA.getId();
		else if (destination.equalsIgnoreCase("Atanatos"))
			worldId = WorldMapType.ATANATOS.getId();
		else if (destination.equalsIgnoreCase("Disillon"))
			worldId = WorldMapType.DISILLON.getId();
		// 房屋 / Housing
		else if (destination.equalsIgnoreCase("Oriel"))
			worldId = WorldMapType.ORIEL.getId();
		else if (destination.equalsIgnoreCase("Pernon"))
			worldId = WorldMapType.PERNON.getId();
		else if (destination.equalsIgnoreCase("All"))
			worldId = 0;
		else {
				PacketSendUtility.sendMessage(player, "Could not find the specified map !");
				return;
			}
		}
		try {
			DataManager.SPAWNS_DATA2 = SpawnsData2.load(Config.dataFile("./data/static_data/spawns"));
		} catch (Exception e) {
			PacketSendUtility.sendMessage(player, "Spawn reload failed; existing spawns were kept.");
			log.error(I18n.get("log.61ab9dbbf5c5"), e);
			return;
		}
		final String destinationMap = destination;
		if (destination.equalsIgnoreCase("All")) {
			// 天族。 / ELYOS.
			reloadMap(WorldMapType.SANCTUM.getId(), player, "Sanctum");
			reloadMap(WorldMapType.KAISINEL.getId(), player, "Kaisinel");
			reloadMap(WorldMapType.KAISINEL_ACADEMY.getId(), player, "Academy");
			reloadMap(WorldMapType.WISPLIGHT_ABBEY.getId(), player, "Wisplight");
			reloadMap(WorldMapType.POETA.getId(), player, "Poeta");
			reloadMap(WorldMapType.VERTERON.getId(), player, "Verteron");
			reloadMap(WorldMapType.ELTNEN.getId(), player, "Eltnen");
			reloadMap(WorldMapType.THEOBOMOS.getId(), player, "Theobomos");
			reloadMap(WorldMapType.HEIRON.getId(), player, "Heiron");
			reloadMap(WorldMapType.INGGISON.getId(), player, "Inggison");
			reloadMap(WorldMapType.CYGNEA.getId(), player, "Cygnea");
			reloadMap(WorldMapType.IDIAN_DEPTHS_L.getId(), player, "Idian_L");
			reloadMap(WorldMapType.ILUMA.getId(), player, "Iluma");
			reloadMap(WorldMapType.TOWER_OF_ETERNITY_E.getId(), player, "Tower_Of_Eternity_E");
			// 魔族。 / ASMODIANS.
			reloadMap(WorldMapType.PANDAEMONIUM.getId(), player, "Pandaemonium");
			reloadMap(WorldMapType.MARCHUTAN.getId(), player, "Marchutan");
			reloadMap(WorldMapType.MARCHUTAN_PRIORY.getId(), player, "Priory");
			reloadMap(WorldMapType.FATEBOUND_ABBEY.getId(), player, "Fatebound");
			reloadMap(WorldMapType.ISHALGEN.getId(), player, "Ishalgen");
			reloadMap(WorldMapType.ALTGARD.getId(), player, "Altgard");
			reloadMap(WorldMapType.MORHEIM.getId(), player, "Morheim");
			reloadMap(WorldMapType.BRUSTHONIN.getId(), player, "Brusthonin");
			reloadMap(WorldMapType.BELUSLAN.getId(), player, "Beluslan");
			reloadMap(WorldMapType.GELKMAROS.getId(), player, "Gelkmaros");
			reloadMap(WorldMapType.ENSHAR.getId(), player, "Enshar");
			reloadMap(WorldMapType.IDIAN_DEPTHS_D.getId(), player, "Idian_D");
			reloadMap(WorldMapType.NORSVOLD.getId(), player, "Norsvold");
			reloadMap(WorldMapType.TOWER_OF_ETERNITY_A.getId(), player, "Tower_Of_Eternity_A");
			// 雷珊塔 / Reshanta
			reloadMap(WorldMapType.RESHANTA.getId(), player, "Reshanta");
			// 帕内斯特拉 / Panesterra
			reloadMap(WorldMapType.BELUS.getId(), player, "Belus");
			reloadMap(WorldMapType.ASPIDA.getId(), player, "Aspida");
			reloadMap(WorldMapType.ATANATOS.getId(), player, "Atanatos");
			reloadMap(WorldMapType.DISILLON.getId(), player, "Disillon");
			// 其他区域 / Other Zone
			reloadMap(WorldMapType.SILENTERA_CANYON.getId(), player, "Silentera");
			reloadMap(WorldMapType.KALDOR.getId(), player, "Kaldor");
			reloadMap(WorldMapType.LEVINSHOR.getId(), player, "Levinshor");
			// 房屋 / Housing
			reloadMap(WorldMapType.ORIEL.getId(), player, "Oriel");
			reloadMap(WorldMapType.PERNON.getId(), player, "Pernon");
		} else {	
			reloadMap(worldId, player, destinationMap);
		}
	}
	
	private void reloadMap (int worldId, Player admin, String destinationMap) {
		final int IdWorld = worldId;
		final Player adm = admin;
		final String dest = destinationMap;
		if (IdWorld != 0) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllObjects(new Visitor<VisibleObject>() {
				@Override
				public void visit(VisibleObject object) {
					if (object.getWorldId() != IdWorld) {
						return;
					} if (object instanceof Npc || object instanceof Gatherable || object instanceof StaticObject) {
						object.getController().delete();
					}
				}
			});
			SpawnEngine.spawnWorldMap(IdWorld);
			PacketSendUtility.sendMessage(adm, "Spawns for map: " + IdWorld + " (" + dest + ") reloaded succesfully");
		}
	}
	
	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 *
	 * @param player 接收提示的玩家 / player receiving the message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //reload_spawn <location name | all>");
	}
}

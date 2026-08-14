package com.aionemu.gameserver.services.rift;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.aionemu.gameserver.controllers.RVController;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RIFT_ANNOUNCE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 裂隙信息同步器，负责向玩家广播裂隙开启/关闭与统计公告。
 * Rift informer that broadcasts open/close state and announce stats to players.
 *
 * @author Rinzler (Encom)
 */
public class RiftInformer {
	/**
	 * 获取指定世界已生成的裂隙 NPC 列表。
	 * Returns spawned rift NPCs for the given world.
	 *
	 * @param worldId 世界地图 ID / World map id
	 * @return 该世界的裂隙 NPC / Rift NPCs in that world
	 */
	public static List<Npc> getSpawned(int worldId) {
		List<Npc> rifts = RiftManager.getSpawned();
		List<Npc> worldRifts = new CopyOnWriteArrayList<Npc>();
		for (Npc rift : rifts) {
			if (rift.getWorldId() == worldId) {
				worldRifts.add(rift);
			}
		}
		return worldRifts;
	}

	/**
	 * 向指定世界（及其双子地图）广播完整裂隙信息。
	 * Broadcast full rift info to the world and its twin map.
	 *
	 * @param worldId 世界地图 ID / World map id
	 */
	public static void sendRiftsInfo(int worldId) {
		syncRiftsState(worldId, getPackets(worldId));
		int twinId = getTwinId(worldId);
		if (twinId > 0) {
			syncRiftsState(twinId, getPackets(twinId));
		}
	}

	/**
	 * 向指定玩家（及其世界双子地图）发送裂隙信息。
	 * Send rift info to a player and the twin map of their world.
	 *
	 * @param player 目标玩家 / Target player
	 */
	public static void sendRiftsInfo(Player player) {
		syncRiftsState(player, getPackets(player.getWorldId()));
		int twinId = getTwinId(player.getWorldId());
		if (twinId > 0) {
			syncRiftsState(twinId, getPackets(twinId));
		}
	}

	/**
	 * 向多个世界同步裂隙详情（不含公告汇总）。
	 * Sync rift details (without announce summary) to multiple worlds.
	 *
	 * @param worlds 世界 ID 数组 / World id array
	 */
	public static void sendRiftInfo(int[] worlds) {
		for (int worldId : worlds) {
			syncRiftsState(worldId, getPackets(worlds[0], -1));
		}
	}

	/**
	 * 向世界广播单个裂隙消失通知。
	 * Broadcast a single rift despawn notice to a world.
	 *
	 * @param worldId 世界地图 ID / World map id
	 * @param objId 裂隙对象 ID / Rift object id
	 */
	public static void sendRiftDespawn(int worldId, int objId) {
		syncRiftsState(worldId, getPackets(worldId, objId), true);
	}

	private static List<AionServerPacket> getPackets(int worldId) {
		return getPackets(worldId, 0);
	}

	private static List<AionServerPacket> getPackets(int worldId, int objId) {
		List<AionServerPacket> packets = new ArrayList<AionServerPacket>();
		if (objId == -1) {
			for (Npc rift : getSpawned(worldId)) {
				RVController controller = (RVController) rift.getController();
				if (!controller.isMaster()) {
					continue;
				}
				packets.add(new SM_RIFT_ANNOUNCE(controller, false));
			}
		} else if (objId > 0) {
			packets.add(new SM_RIFT_ANNOUNCE(objId));
		} else {
			packets.add(new SM_RIFT_ANNOUNCE(getAnnounceData(worldId)));
			for (Npc rift : getSpawned(worldId)) {
				RVController controller = (RVController) rift.getController();
				if (!controller.isMaster()) {
					continue;
				}
				packets.add(new SM_RIFT_ANNOUNCE(controller, true));
				packets.add(new SM_RIFT_ANNOUNCE(controller, false));
			}
		}
		return packets;
	}

	private static void syncRiftsState(Player player, final List<AionServerPacket> packets) {
		for (AionServerPacket packet : packets) {
			PacketSendUtility.sendPacket(player, packet);
		}
	}

	private static void syncRiftsState(int worldId, final List<AionServerPacket> packets) {
		syncRiftsState(worldId, packets, false);
	}

	private static void syncRiftsState(int worldId, final List<AionServerPacket> packets, final boolean isDespawnInfo) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId).getMainWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				syncRiftsState(player, packets);
			}
		});
	}

	private static Map<Integer, Integer> getAnnounceData(int worldId) {
		Map<Integer, Integer> localRifts = new LinkedHashMap<>();
		for (int i = 0; i < 8; i++) {
			localRifts.put(i, 0);
		}
		for (Npc rift : getSpawned(worldId)) {
			RVController rc = (RVController) rift.getController();
			localRifts = calcRiftsData(rc, localRifts);
		}
		return localRifts;
	}

	private static Map<Integer, Integer> calcRiftsData(RVController rift, Map<Integer, Integer> local) {
		if (rift.isMaster()) {
			local.put(0, local.get(0) + 1);
			if (rift.isVortex()) {
				local.put(1, local.get(1) + 1);
			}
			local.put(2, local.get(2) + 1);
			local.put(3, local.get(3) + 1);
			local.put(4, local.get(4) + 1);
		} else {
			local.put(5, local.get(5) + 1);
			local.put(6, local.get(6) + 1);
			if (rift.isVortex()) {
				local.put(7, local.get(7) + 1);
			}
		}
		return local;
	}

	/**
	 * 返回跨种族裂隙对应的双子地图 ID；无对应时返回 0。
	 * Returns the twin map id for cross-race rifts; 0 when none.
	 */
	private static int getTwinId(int worldId) {
		switch (worldId) {
		/**
	 * 天族 / Elyos
	 */
		case 110070000: // Kaisinel Academy -> Brusthonin
			return 220050000;
		case 210020000: // Eltnen -> Morheim
			return 220020000;
		case 210040000: // Heiron -> Beluslan
			return 220040000;
		case 210050000: // Inggison -> Gelkmaros
			return 220070000;
		case 210130000: // Inggison [Master Server] -> Gelkmaros [Master Server]
			return 220140000;
		case 210070000: // Cygnea -> Enshar
			return 220080000;
		case 210060000: // Theobomos -> Marchutan Priory
			return 120080000;
		case 210100000: // Iluma -> Norsvold
			return 220110000;
		/**
	 * 魔族 / Asmodians
	 */
		case 120080000: // Marchutan Priory -> Theobomos
			return 210060000;
		case 220020000: // Morheim -> Eltnen
			return 210020000;
		case 220040000: // Beluslan -> Heiron
			return 210040000;
		case 220050000: // Brusthonin -> Kaisinel Academy
			return 110070000;
		case 220070000: // Gelkmaros -> Inggison
			return 210050000;
		case 220140000: // Gelkmaros [Master Server] -> Inggison [Master Server]
			return 210130000;
		case 220080000: // Enshar -> Cygnea
			return 210070000;
		case 220110000: // Norsvold -> Iluma
			return 210100000;
		default:
			return 0;
		}
	}
}

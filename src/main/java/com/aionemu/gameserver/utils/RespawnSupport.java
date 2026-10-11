package com.aionemu.gameserver.utils;

import java.io.IOException;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;

/**
 * NPC 重刷共享工具：删除目标 NPC 后按新位姿重刷并持久化，供 {@code //fixh}/{@code //fixz} 复用。
 * Shared respawn helper: deletes the target NPC, re-spawns it at a new pose and persists it; reused by {@code //fixh} and {@code //fixz}.
 */
public final class RespawnSupport {

	private RespawnSupport() {
	}

	/**
	 * 删除目标 NPC 并按新坐标重刷（固定 295 秒重生时间），持久化生成并向管理员回报结果。
	 * Deletes the target NPC, re-spawns it at the given position (fixed 295s respawn time), persists the spawn and reports the result to the admin.
	 */
	public static void respawnNpc(Player admin, Npc target, float x, float y, float z, byte heading, String tag) {
		SpawnTemplate temp = target.getSpawn();

		// 删除生成/NPC / delete spawn,npc
		target.getController().delete();

		// 生成 NPC / spawn npc
		SpawnTemplate spawn = SpawnEngine.addNewSpawn(temp.getWorldId(), temp.getNpcId(), x, y, z, heading, 295);

		VisibleObject visibleObject = SpawnEngine.spawnObject(spawn, admin.getInstanceId());

		if (visibleObject == null) {
			PacketSendUtility.sendMessage(admin, "npc id " + temp.getNpcId() + " was not found!");
			return;
		}

		try {
			DataManager.SPAWNS_DATA2.saveSpawn(admin, visibleObject, false);
		} catch (IOException e) {
			PacketSendUtility.sendMessage(admin, "Could not save spawn");
		}

		PacketSendUtility.sendMessage(admin, visibleObject.getObjectTemplate().getName() + tag);
	}
}

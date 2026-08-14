package com.aionemu.gameserver.commands.admin;

import java.io.IOException;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 修正目标 NPC 高度（Z）并重新保存刷出的命令（{@code //fixz}）。
 * Admin command that fixes target NPC Z height and re-saves the spawn ({@code //fixz}).
 */
public class FixZ extends AdminCommand {

	/**
	 * 注册命令名为 {@code fixz}。
	 * Registers the command name {@code fixz}.
	 */
	public FixZ() {
		super("fixz");
	}

	/**
	 * 以管理员当前 Z 坐标重刷目标 NPC 并持久化。
	 * Re-spawns the targeted NPC at the admin's current Z and persists it.
	 *
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (admin.getAccessLevel() < 1) {
			PacketSendUtility.sendMessage(admin, "You dont have enough rights to use this command!");
			return;
		}

		if (admin.getTarget() != null) {
			if (admin.getTarget() instanceof Npc) {
				Npc target = (Npc) admin.getTarget();
				final SpawnTemplate temp = target.getSpawn();
				int respawnTime = 295;
				boolean permanent = true;

				// 删除生成/NPC / delete spawn,npc
				target.getController().delete();

				// 生成 NPC / spawn npc
				int templateId = temp.getNpcId();
				float x = temp.getX();
				float y = temp.getY();
				float z = admin.getZ();
				byte heading = temp.getHeading();
				int worldId = temp.getWorldId();

				SpawnTemplate spawn = SpawnEngine.addNewSpawn(worldId, templateId, x, y, z, heading, respawnTime);

				if (spawn == null) {
					PacketSendUtility.sendMessage(admin, "There is no template with id " + templateId);
					return;
				}

				VisibleObject visibleObject = SpawnEngine.spawnObject(spawn, admin.getInstanceId());

				if (visibleObject == null) {
					PacketSendUtility.sendMessage(admin, "npc id " + templateId + " was not found!");
				}
				else if (permanent) {
					try {
						DataManager.SPAWNS_DATA2.saveSpawn(admin, visibleObject, false);
					}
					catch (IOException e) {
						PacketSendUtility.sendMessage(admin, "Could not save spawn");
					}
				}

				String objectName = visibleObject.getObjectTemplate().getName();
				PacketSendUtility.sendMessage(admin, objectName + "FixZ");
			}

		}
		else {
			PacketSendUtility.sendMessage(admin, "Only in target!");
		}
	}

	/**
	 * 按参数生成并刷出 NPC。
	 * Creates and spawns an NPC from the given parameters.
	 *
	 * @param x X 坐标 / X coordinate
	 * @param y Y 坐标 / Y coordinate
	 * @param z Z 坐标 / Z coordinate
	 */
	protected VisibleObject spawn(int npcId, int mapId, int instanceId, float x, float y, float z, byte heading, String walkerId, int walkerIdx, int respawnTime) {
		SpawnTemplate template = SpawnEngine.addNewSpawn(mapId, npcId, x, y, z, heading, respawnTime);
		return SpawnEngine.spawnObject(template, instanceId);
	}
}

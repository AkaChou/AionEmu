package com.aionemu.gameserver.commands.admin;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 刷怪点坐标更新指令；修改选中 NPC 的 x/y/z/h/walker 或整体坐标并保存。
 * Admin command that updates a targeted NPC spawn's x/y/z/h/walker or full coordinates and saves them.
 *
 * @author KID
 * @modified Rolandas
 */
@Slf4j
public class SpawnUpdate extends AdminCommand {

	public SpawnUpdate() {
		super("spawnu");
	}

	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param admin 执行指令的管理员 / admin executing the command
	 * command arguments
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params[0].equalsIgnoreCase("set")) {
			Npc npc = null;
			if (admin.getTarget() != null && admin.getTarget() instanceof Npc)
				npc = (Npc) admin.getTarget();

			if (npc == null) {
				PacketSendUtility.sendMessage(admin, "you need to target Npc type.");
				return;
			}

			SpawnTemplate spawn = npc.getSpawn();
			
			if (params[1].equalsIgnoreCase("x")) {
				float x;
				if (params.length < 3)
					x = admin.getX();
				else
					x = Float.parseFloat(params[2]);
				npc.getPosition().setXYZH(x, null, null, null);
				PacketSendUtility.sendPacket(admin, new SM_DELETE(npc, 0));
				PacketSendUtility.sendPacket(admin, new SM_NPC_INFO(npc, admin));
				PacketSendUtility.sendMessage(admin, "updated npcs x to " + x + ".");
				try {
					DataManager.SPAWNS_DATA2.saveSpawn(admin, npc, false);
				}
				catch (IOException e) {
					log.error(I18n.get("log.9c4062a74204", npc.getObjectId(), e), e);
					PacketSendUtility.sendMessage(admin, "Could not save spawn");
				}
				return;
			}
			
			if (params[1].equalsIgnoreCase("y")) {
				float y;
				if (params.length < 3)
					y = admin.getY();
				else
					y = Float.parseFloat(params[2]);
				npc.getPosition().setXYZH(null, y, null, null);
				PacketSendUtility.sendPacket(admin, new SM_DELETE(npc, 0));
				PacketSendUtility.sendPacket(admin, new SM_NPC_INFO(npc, admin));
				PacketSendUtility.sendMessage(admin, "updated npcs y to " + y + ".");
				try {
					DataManager.SPAWNS_DATA2.saveSpawn(admin, npc, false);
				}
				catch (IOException e) {
					log.error(I18n.get("log.d90b26fbea8f", npc.getObjectId(), e), e);
					PacketSendUtility.sendMessage(admin, "Could not save spawn");
				}
				return;
			}
			
			if (params[1].equalsIgnoreCase("z")) {
				float z;
				if (params.length < 3)
					z = admin.getZ();
				else
					z = Float.parseFloat(params[2]);
				npc.getPosition().setZ(z);
				PacketSendUtility.sendPacket(admin, new SM_DELETE(npc, 0));
				PacketSendUtility.sendPacket(admin, new SM_NPC_INFO(npc, admin));
				PacketSendUtility.sendMessage(admin, "updated npcs z to " + z + ".");
				try {
					DataManager.SPAWNS_DATA2.saveSpawn(admin, npc, false);
				}
				catch (IOException e) {
					log.error(I18n.get("log.e620e6fd8ff4", npc.getObjectId(), e), e);
					PacketSendUtility.sendMessage(admin, "Could not save spawn");
				}
				return;
			}
			
			if (params[1].equalsIgnoreCase("h")) {
				byte h;
				if (params.length < 3) {
					byte heading = admin.getHeading();
					if (heading > 60)
						heading -= 60;
					else
						heading += 60;
					h = heading;
				}
				else
					h = Byte.parseByte(params[2]);
				npc.getPosition().setH(h);
				PacketSendUtility.sendPacket(admin, new SM_DELETE(npc, 0));
				PacketSendUtility.sendPacket(admin, new SM_NPC_INFO(npc, admin));
				PacketSendUtility.sendMessage(admin, "updated npcs heading to " + h + ".");
				try {
					DataManager.SPAWNS_DATA2.saveSpawn(admin, npc, false);
				}
				catch (IOException e) {
					log.error(I18n.get("log.8c96d7fce14b", npc.getObjectId(), e), e);
					PacketSendUtility.sendMessage(admin, "Could not save spawn");
				}
				return;
			}
			
			if (params[1].equalsIgnoreCase("xyz")) {
				PacketSendUtility.sendPacket(admin, new SM_DELETE(npc, 0));
				npc.getPosition().setXYZH(admin.getX(), null, null, null);
				try {
					DataManager.SPAWNS_DATA2.saveSpawn(admin, npc, false);
					PacketSendUtility.sendPacket(admin, new SM_NPC_INFO(npc, admin));
					npc.getPosition().setXYZH(null, admin.getY(), null, null);
					DataManager.SPAWNS_DATA2.saveSpawn(admin, npc, false);
					PacketSendUtility.sendPacket(admin, new SM_NPC_INFO(npc, admin));
					npc.getPosition().setXYZH(null, null, admin.getZ(), null);
					DataManager.SPAWNS_DATA2.saveSpawn(admin, npc, false);
					PacketSendUtility.sendPacket(admin, new SM_NPC_INFO(npc, admin));
					PacketSendUtility.sendMessage(admin, "updated npcs coordinates to " + admin.getX() + ", " + admin.getY() + ", " + admin.getZ() + ".");
				}
				catch (IOException e) {
					log.error(I18n.get("log.0301bbdb1f5a", npc.getObjectId(), e), e);
					PacketSendUtility.sendMessage(admin, "Could not save spawn");
				}
				return;
			}
			
			if (params[1].equalsIgnoreCase("w")) {
				String walkerId = null;
				if (params.length == 3)
					walkerId = params[2].toUpperCase();
				if (walkerId != null) {
					WalkerTemplate template = DataManager.WALKER_DATA.getWalkerTemplate(walkerId);
					if (template == null) {
						PacketSendUtility.sendMessage(admin, "No such template exists in npc_walker.xml.");
						return;
					}
					List<SpawnGroup2> allSpawns = DataManager.SPAWNS_DATA2.getSpawnsByWorldId(npc.getWorldId());
					List<SpawnTemplate> sameIds = new ArrayList<SpawnTemplate>();
					for (SpawnGroup2 spawnGroup : allSpawns) {
						for (SpawnTemplate spawnTemplate : spawnGroup.getSpawnTemplates()) {
							if (walkerId.equals(spawnTemplate.getWalkerId())) {
								sameIds.add(spawnTemplate);
							}
						}
					}
					if (sameIds.size() >= template.getPool()) {
						PacketSendUtility.sendMessage(admin, "Can not assign, walker pool reached the limit.");
						return;
					}
				}
				spawn.setWalkerId(walkerId);
				PacketSendUtility.sendPacket(admin, new SM_DELETE(npc, 0));
				PacketSendUtility.sendPacket(admin, new SM_NPC_INFO(npc, admin));
				if (walkerId == null)
					PacketSendUtility.sendMessage(admin, "removed npcs walker_id for " + npc.getNpcId() + ".");
				else
					PacketSendUtility.sendMessage(admin, "updated npcs walker_id to " + walkerId + ".");
				try {
					DataManager.SPAWNS_DATA2.saveSpawn(admin, npc, false);
				}
				catch (IOException e) {
					log.error(I18n.get("log.9398ae5c1ffd", npc.getObjectId(), e), e);
					PacketSendUtility.sendMessage(admin, "Could not save spawn");
				}
			}
		}
	}

	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 *
	 * @param player 接收提示的玩家 / player receiving the message
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "<usage //spawnu set (x | y | z | h | w | xyz)");
	}
}

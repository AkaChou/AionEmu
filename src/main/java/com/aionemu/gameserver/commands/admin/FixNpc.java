package com.aionemu.gameserver.commands.admin;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * 批量修正 NPC 刷出高度/位置的管理命令（{@code //fixnpc}）。
 * Admin command that batch-fixes NPC spawn heights/positions ({@code //fixnpc}).
 */
@Slf4j
public class FixNpc extends AdminCommand
{
	private Npc npc = null;
	private int numofspawns = 0;
	private int spawned = 0;
	private Future<?> task = null;

	/**
	 * 注册命令名为 {@code fixnpc}。
	 * Registers the command name {@code fixnpc}.
	 */
	public FixNpc() {
		super("fixnpc");
	}

	/**
	 * 对当前目标或按 start/stop 批量修正刷出并保存。
	 * Fixes the current target spawn or batch-fixes via start/stop.
	 * @param params start [counter] | stop，或无参时处理当前目标 / start [counter] | stop, or current target when empty
	 */
	@Override
	public void execute(final Player admin, String... params) {
		if (admin.getAccessLevel() < 5) {
			PacketSendUtility.sendMessage(admin, "You dont have enough rights to use this command!");
			return;
		}
		if (params.length == 0 && admin.getTarget() != null) {
			if (admin.getTarget() instanceof Npc target) {
				final SpawnTemplate temp = target.getSpawn();
				final float adminZ = admin.getZ();
				List<SpawnGroup2> spawnId = DataManager.SPAWNS_DATA2.getSpawnsByWorldId(admin.getWorldId());
				PacketSendUtility.sendMessage(admin, "SpawnId: " + spawnId);
				for (final SpawnGroup2 spawn : spawnId) {
					StringBuilder comment = new StringBuilder();
					comment.append(target.getObjectTemplate().getName()).append(" (");
					int isObject = target.getSpawn().getEntityId();
					if (isObject > 0) {
						comment.append("Object");
					}
					else {
						comment.append("Npc");
					}
					comment.append(" ").append(target.getObjectTemplate().getRank().name()).append(" ");
					comment.append("lvl:").append(target.getLevel()).append(")");
					int time = 9000;
					task = GameThreadPoolServices.threadPoolManager().schedule(() -> respawnAndSave(admin, admin.getWorldId(), spawn.getNpcId(), temp.getX(), temp.getY(), adminZ, temp.getHeading(), temp.getRespawnTime(), target), time);
					PacketSendUtility.sendMessage(admin, comment + " [Spawned] ");
				}
			}
			else {
				PacketSendUtility.sendMessage(admin, "Only instances of Npc are allowed as target!");
				return;
			}
		}
		else if ((params.length == 1 || params.length == 2) && "start".equalsIgnoreCase(params[0])) {
			int stop = 0;
			if (params.length == 1) {
				stop = -1;
			}
			else if ("start".equalsIgnoreCase(params[0])) {
				stop = Integer.parseInt(params[1]);
			}
			List<SpawnGroup2> spawngroups = DataManager.SPAWNS_DATA2.getSpawnsByWorldId(admin.getWorldId());
			List<SpawnTemplate> templates = new ArrayList<>();
			PacketSendUtility.sendMessage(admin, "[Auto Spawn]: will start in 10 seconds.");
			for (final SpawnGroup2 spawngroup : spawngroups) {
				templates.addAll(spawngroup.getSpawnTemplates());
				numofspawns += spawngroup.getSpawnTemplates().size();
			}
			PacketSendUtility.sendMessage(admin, "[Aprox Time]: " + ((numofspawns * 3.6) / 60) + " Minutes.");
			int time = 9000;
			int counter = 0;
			for (final SpawnTemplate template : templates) {
				if (counter >= stop && stop >= 0) {
					break;
				}
				++counter;
				time += 3000;
				task = GameThreadPoolServices.threadPoolManager().schedule(() -> {
					TeleportService2.teleportTo(admin, template.getWorldId(), template.getX(), template.getY(), template.getZ(), (byte) 0);
					admin.getKnownList().doOnAllNpcs(n -> {
						if (MathUtil.getDistance((int) n.getX(), (int) n.getY(), (int) admin.getX(), (int) admin.getY()) < 3) {
							npc = n;
						}
					});
				}, time);
				time += 3000;
				task = GameThreadPoolServices.threadPoolManager().schedule(() -> {
					if (npc != null) {
						PacketSendUtility.broadcastPacketAndReceive(admin, new SM_FORCED_MOVE(npc, admin));
					}
				}, time);
				time += 3000;
				task = GameThreadPoolServices.threadPoolManager().schedule(() -> {
					if (npc != null) {
						StringBuilder comment = new StringBuilder();
						comment.append(npc.getObjectTemplate().getName()).append(" (");
						int isObject = npc.getSpawn().getEntityId();
						if (isObject != 0) {
							comment.append("Object");
						}
						else {
							comment.append("Npc");
						}
						comment.append(" ").append(npc.getObjectTemplate().getRank().name()).append(" ");
						comment.append("lvl:").append(npc.getLevel()).append(")");
						Spawn spawnId = DataManager.SPAWNS_DATA2.getSpawnsForNpc(admin.getWorldId(), npc.getNpcId());
						if (spawnId != null) {
							log.info(I18n.get("log.ee4fb40d60d3", template.getNpcId(), template.getWorldId(), template.getX(), template.getY(), template.getZ()));
						}
						respawnAndSave(admin, template.getWorldId(), template.getNpcId(), template.getX(), template.getY(), admin.getZ(), template.getHeading(), template.getRespawnTime(), npc);
						++spawned;
						PacketSendUtility.sendMessage(admin, spawned + ". " + comment + " spawned");
						npc = null;
					}
					else {
						if (template != null) {
							log.info(I18n.get("log.15d5ee9b4d27", template.getNpcId(), template.getWorldId(), template.getX(), template.getY(), template.getZ()));
						}
					}
				}, time);
			}
		}
		else if ((params.length == 1 || params.length == 2) && "stop".equalsIgnoreCase(params[0])) {
			if (task != null) {
				task.cancel(true);
				task = null;
			}
		}
		else {
			PacketSendUtility.sendMessage(admin, "Syntax: //fixnpc <start> <counter>");
		}
		PacketSendUtility.sendMessage(admin, "[Number Of Spawns]: " + numofspawns);
	}

	/**
	 * 按新位姿刷出对象、删除旧对象并持久化生成，保存失败时记录日志并提示。
	 * Spawns an object at the new pose, deletes the old one and persists the spawn; logs and notifies on save failure.
	 */
	private static void respawnAndSave(Player admin, int worldId, int npcId, float x, float y, float z, byte heading, int respawnTime, Creature oldNpc) {
		SpawnTemplate spawn2 = SpawnEngine.addNewSpawn(worldId, npcId, x, y, z, heading, respawnTime);
		VisibleObject visibleObject = SpawnEngine.spawnObject(spawn2, admin.getInstanceId());
		oldNpc.getController().delete();
		try {
			DataManager.SPAWNS_DATA2.saveSpawn(admin, visibleObject, false);
		}
		catch (IOException e) {
			log.error(I18n.get("log.242d2bd13c3f", visibleObject.getObjectId()), e);
			PacketSendUtility.sendMessage(admin, "Could not save spawn");
		}
	}

	/**
	 * 执行失败时的语法提示。
	 * Syntax hint on failure.
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //kill <target | all | <range>>");
	}
}

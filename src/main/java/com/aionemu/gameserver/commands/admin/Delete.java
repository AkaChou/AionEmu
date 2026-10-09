package com.aionemu.gameserver.commands.admin;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.siegespawns.SiegeSpawnTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.io.IOException;

/**
 * 删除当前目标 NPC 刷出的管理命令（{@code //delete}，非持久化）。
 * 生效仅限本次运行实例，不写入刷怪数据；需要持久化删除时使用 {@code //deletes}。
 * Admin command that deletes the targeted NPC spawn ({@code //delete}, non-persisting).
 * The removal lasts only for the current server session and is not written to the spawn data;
 * use {@code //deletes} to persist the deletion.
 * @author Luno
 */
@Slf4j
public class Delete extends AdminCommand {

	/**
	 * 注册命令名为 {@code delete}。
	 * Registers the command name {@code delete}.
	 */
	public Delete() {
		super("delete");
	}

	/**
	 * 删除目标 NPC 刷出（非持久化，不支持池化/攻城刷出）。
	 * Deletes the targeted NPC spawn without persisting (pooled/siege spawns are not allowed).
	 */
	@Override
	public void execute(Player player, String... params) {
		executeDelete(player, false);
	}

	/**
	 * 删除共享执行体；{@code persist=false} 时仅从当前实例移除目标，{@code persist=true} 时把删除写入刷怪数据。
	 * Shared delete executor; with {@code persist=false} the target is only removed from the current
	 * instance, while {@code persist=true} also writes the deletion to the spawn data.
	 * @param player 执行指令的玩家 / player executing the command
	 * @param persist 是否把删除写入刷怪数据 / whether to write the deletion to the spawn data
	 */
	static void executeDelete(Player player, boolean persist) {
		VisibleObject cre = player.getTarget();
		if (!(cre instanceof Npc npc)) {
			PacketSendUtility.sendMessage(player, "Wrong target");
			return;
		}
		SpawnTemplate template = npc.getSpawn();
		if (template.hasPool()) {
			PacketSendUtility.sendMessage(player, "Can't delete pooled spawn template");
			return;
		}
		if (template instanceof SiegeSpawnTemplate) {
			PacketSendUtility.sendMessage(player, "Can't delete siege spawn template");
			return;
		}
		npc.getController().delete();
		if (!persist) {
			PacketSendUtility.sendMessage(player, "Spawn removed (temporary, not saved)");
			return;
		}
		try {
			DataManager.SPAWNS_DATA2.saveSpawn(player, npc, true);
		}
		catch (IOException e) {
			log.error(I18n.get("log.dd0177354196", npc.getObjectId()), e);
			PacketSendUtility.sendMessage(player, "Could not remove spawn");
			return;
		}
		PacketSendUtility.sendMessage(player, "Spawn removed");
	}

}

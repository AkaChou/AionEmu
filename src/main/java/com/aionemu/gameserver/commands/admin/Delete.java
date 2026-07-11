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
 * 删除当前目标 NPC 刷出并持久化的管理命令（{@code //delete}）。
 * Admin command that deletes the targeted NPC spawn and persists the change ({@code //delete}).
 *
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
	 * 删除目标 NPC 刷出（不支持池化/攻城刷出）。
	 * Deletes the targeted NPC spawn (pooled/siege spawns are not allowed).
	 *
	 * admin
	 * unused
	 */
	@Override
	public void execute(Player player, String... params) {

		VisibleObject cre = player.getTarget();
		if (!(cre instanceof Npc)) {
			PacketSendUtility.sendMessage(player, "Wrong target");
			return;
		}
		Npc npc = (Npc) cre;
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
		try {
			DataManager.SPAWNS_DATA2.saveSpawn(player, npc, true);
		}
		catch (IOException e) {
			log.error(I18n.get("log.dd0177354196", npc.getObjectId(), e));
			PacketSendUtility.sendMessage(player, "Could not remove spawn");
			return;
		}
		PacketSendUtility.sendMessage(player, "Spawn removed");
	}

}

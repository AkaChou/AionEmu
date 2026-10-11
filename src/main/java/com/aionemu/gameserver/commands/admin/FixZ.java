package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
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
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (admin.getAccessLevel() < 1) {
			PacketSendUtility.sendMessage(admin, "You dont have enough rights to use this command!");
			return;
		}

		if (admin.getTarget() != null) {
			if (admin.getTarget() instanceof Npc target) {
				SpawnTemplate temp = target.getSpawn();
				RespawnSupport.respawnNpc(admin, target, temp.getX(), temp.getY(), admin.getZ(), temp.getHeading(), "FixZ");
			}
		}
		else {
			PacketSendUtility.sendMessage(admin, "Only in target!");
		}
	}
}

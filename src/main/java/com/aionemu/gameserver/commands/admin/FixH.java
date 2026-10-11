package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.RespawnSupport;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 修正目标 NPC 朝向并重新保存刷出的命令（{@code //fixh}）。
 * Admin command that fixes target NPC heading and re-saves the spawn ({@code //fixh}).
 */
public class FixH extends AdminCommand {

	/**
	 * 注册命令名为 {@code fixh}。
	 * Registers the command name {@code fixh}.
	 */
	public FixH() {
		super("fixh");
	}

	/**
	 * 以管理员朝向翻转后的值重刷目标 NPC 并持久化。
	 * Re-spawns the targeted NPC with heading flipped from the admin and persists it.
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

				// 朝向翻转 60 / flip heading by 60
				byte heading = admin.getHeading();
				if (heading > 60) {
					heading -= 60;
				} else {
					heading += 60;
				}

				RespawnSupport.respawnNpc(admin, target, temp.getX(), temp.getY(), temp.getZ(), heading, "FixH");
			}
		} else {
			PacketSendUtility.sendMessage(admin, "Only in target!");
		}
	}
}

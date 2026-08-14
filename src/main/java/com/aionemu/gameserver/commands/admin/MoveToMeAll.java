package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_SPAWN;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 将全服或按种族筛选的在线玩家批量传送到管理员身边的命令。
 * Admin command to mass-teleport all online players, or by race, to the admin.
 *
 * @author Shepper Helped by @alfa24t
 */
public class MoveToMeAll extends AdminCommand {

	/**
	 * 以别名 {@code movetomeall} 构造命令。
	 * Construct the command with alias {@code movetomeall}.
	 */
	public MoveToMeAll() {
		super("movetomeall");
	}

	/**
	 * 按 {@code all|elyos|asmos} 将对应玩家传送到管理员位置。
	 * Teleport matching players to the admin for {@code all|elyos|asmos}.
	 *
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "syntax //movetomeall < all | elyos | asmos >");
			return;
		}

		if (params[0].equals("all")) {
			for (final Player p : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
				if (!p.equals(admin)) {
					TeleportService2.teleportTo(p, admin.getWorldId(), admin.getInstanceId(), admin.getX(), admin.getY(),
						admin.getZ(), admin.getHeading());
					PacketSendUtility.sendPacket(p, new SM_PLAYER_SPAWN(p));

					PacketSendUtility.sendMessage(admin, "Player " + p.getName() + " teleported.");
					PacketSendUtility.sendMessage(p, "Teleportd by " + admin.getName() + ".");
				}
			}
		}

		if (params[0].equals("elyos")) {
			for (final Player p : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
				if (!p.equals(admin)) {
					if (p.getRace() == Race.ELYOS) {
						TeleportService2.teleportTo(p, admin.getWorldId(), admin.getInstanceId(), admin.getX(), admin.getY(),
							admin.getZ(), admin.getHeading());
						PacketSendUtility.sendPacket(p, new SM_PLAYER_SPAWN(p));

						PacketSendUtility.sendMessage(admin, "Player " + p.getName() + " teleported.");
						PacketSendUtility.sendMessage(p, "Teleportd by " + admin.getName() + ".");
					}
				}
			}
		}

		if (params[0].equals("asmos")) {
			for (final Player p : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
				if (!p.equals(admin)) {
					if (p.getRace() == Race.ASMODIANS) {
						TeleportService2.teleportTo(p, admin.getWorldId(), admin.getInstanceId(), admin.getX(), admin.getY(),
							admin.getZ(), admin.getHeading());
						PacketSendUtility.sendPacket(p, new SM_PLAYER_SPAWN(p));

						PacketSendUtility.sendMessage(admin, "Player " + p.getName() + " teleported.");
						PacketSendUtility.sendMessage(p, "Teleportd by " + admin.getName() + ".");
					}
				}
			}
		}
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //movetomeall < all | elyos | asmos >");
	}
}

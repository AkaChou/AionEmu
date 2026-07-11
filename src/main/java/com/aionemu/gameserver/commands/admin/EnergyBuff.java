package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理休息能量/救赎点数的命令（{@code //energy}）。
 * Admin command that manages repose energy and salvation points ({@code //energy}).
 *
 * @author Source
 */
public class EnergyBuff extends AdminCommand {

	/**
	 * 注册命令名为 {@code energy}。
	 * Registers the command name {@code energy}.
	 */
	public EnergyBuff() {
		super("energy");
	}

	/**
	 * 对目标玩家查看、增加或重置休息/救赎能量，或刷新属性包。
	 * Views, adds or resets repose/salvation energy on the target player, or refreshes stats.
	 *
	 * admin
	 * repose|salvation|refresh and sub-action。 / repose|salvation|refresh and sub-action
	 */
	@Override
	public void execute(Player player, String... params) {
		VisibleObject target = player.getTarget();
		if (target == null) {
			PacketSendUtility.sendMessage(player, "No target selected");
			return;
		}

		Creature creature = (Creature) target;
		if (params == null || params.length < 1) {
			onFail(player, null);
		}
		else if (target instanceof Player) {
			if (params[0].equals("repose")) {
				Player targetPlayer = (Player) creature;
				if (params[1].equals("info"))
					PacketSendUtility.sendMessage(player, "Current EoR: " + targetPlayer.getCommonData().getCurrentReposteEnergy() + "\n Max EoR: " + targetPlayer.getCommonData().getMaxReposteEnergy());
				else if (params[1].equals("add"))
					targetPlayer.getCommonData().addReposteEnergy(Long.parseLong(params[2]));
				else if (params[1].equals("reset"))
					targetPlayer.getCommonData().setCurrentReposteEnergy(0);
			}
			else if (params[0].equals("salvation")) {
				Player targetPlayer = (Player) creature;
				if (params[1].equals("info"))
					PacketSendUtility.sendMessage(player, "Current EoS: " + targetPlayer.getCommonData().getCurrentSalvationPercent());
				else if (params[1].equals("add"))
					targetPlayer.getCommonData().addSalvationPoints(Long.parseLong(params[2]));
				else if (params[1].equals("reset"))
					targetPlayer.getCommonData().resetSalvationPoints();
			}
			else if (params[0].equals("refresh")) {
				Player targetPlayer = (Player) creature;
				PacketSendUtility.sendPacket(targetPlayer, new SM_STATS_INFO(targetPlayer));
			}
		}
		else
			PacketSendUtility.sendMessage(player, "This is not player");
	}

	/**
	 * 执行失败时的语法提示。
	 * Syntax hint on failure.
	 *
	 * admin
	 * error message
	 */
	@Override
	public void onFail(Player player, String message) {
		String syntax = "//energy repose|salvation|refresh info|reset|add [points]";
		PacketSendUtility.sendMessage(player, syntax);
	}
}

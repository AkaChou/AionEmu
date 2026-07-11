package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.WorldMapType;

/**
 * 种族切换指令；将目标玩家改为天族或魔族并传送至对应主城。
 * Admin command that changes the target player's race to Elyos or Asmodian and teleports to the capital.
 *
 * @author Centisgood(Barahime)
 */
public class SetRace extends AdminCommand {

	public SetRace() {
		super("setrace");
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
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "syntax: //setrace <elyos | asmodians>");
			return;
		}

		VisibleObject visibleobject = admin.getTarget();

		if (visibleobject == null || !(visibleobject instanceof Player)) {
			PacketSendUtility.sendMessage(admin, "Wrong select target.");
			return;
		}

		Player target = (Player) visibleobject;
		if (params[0].equalsIgnoreCase("elyos")) {
			target.getCommonData().setRace(Race.ELYOS);
			TeleportService2.teleportTo(target, WorldMapType.SANCTUM.getId(), 1322, 1511, 568, 0);
			PacketSendUtility.sendMessage(target, "Has been moved to Sanctum.");
		}
		else if (params[0].equalsIgnoreCase("asmodians")) {
			target.getCommonData().setRace(Race.ASMODIANS);
			TeleportService2.teleportTo(target, WorldMapType.PANDAEMONIUM.getId(), 1679, 1400, 195, 0);
			PacketSendUtility.sendMessage(target, "Has been moved to Pandaemonium");
		}
		PacketSendUtility.sendMessage(admin,
			target.getName() + " race has been changed to " + params[0] + ".\n" + target.getName()
				+ " has been moved to town.");
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
		PacketSendUtility.sendMessage(player, "syntax: //setrace <elyos | asmodians>");
	}
}
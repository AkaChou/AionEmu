package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import org.apache.commons.lang3.math.NumberUtils;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.RiftService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 裂隙管理指令；按世界 ID 开启或关闭裂隙。
 * Admin command that opens or closes rifts by world ID.
 *
 * @author Ranastic
 */
public class Rift extends AdminCommand
{
	private static final String COMMAND_OPEN = "open";
	private static final String COMMAND_CLOSE = "close";
	
	public Rift() {
		super("rift");
	}
	
	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param player 执行指令的管理员 / admin executing the command
	 * command arguments
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_CLOSE.equalsIgnoreCase(params[0]) || COMMAND_OPEN.equalsIgnoreCase(params[0])) {
			handleRift(player, params);
		}
	}
	
	protected void handleRift(Player player, String... params) {
		if (params.length < 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int id = NumberUtils.toInt(params[1]);
		boolean result;
		if (!isValidId(player, id)) {
			showHelp(player);
			return;
		} if (COMMAND_OPEN.equalsIgnoreCase(params[0])) {
			result = GameLocationBootstrapServices.riftService().openRifts(id);
			PacketSendUtility.sendMessage(player, result ? "Rifts is opened!" : "Rifts was already opened");
		} else if (COMMAND_CLOSE.equalsIgnoreCase(params[0])) {
			result = GameLocationBootstrapServices.riftService().closeRifts(id);
			PacketSendUtility.sendMessage(player, result ? "Rifts is closed!" : "Rifts was already closed");
		}
	}
	
	protected boolean isValidId(Player player, int id) {
		if (!GameLocationBootstrapServices.riftService().isValidId(id)) {
			PacketSendUtility.sendMessage(player, "Id " + id + " is invalid");
			return false;
		}
		return true;
	}
	
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //rift open|close <worldId>");
	}
}
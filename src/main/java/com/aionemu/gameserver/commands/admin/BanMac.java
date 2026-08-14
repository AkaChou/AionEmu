package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员 MAC 封禁命令：按 MAC 地址封禁目标或指定地址。
 * Admin MAC-ban command: bans a MAC address from the target or a given value.
 *
 * @author KID, nrg
 */
public class BanMac extends AdminCommand {

	/**
	 * 注册 {@code //banmac} 命令。
	 * Registers the {@code //banmac} command.
	 */
	public BanMac() {
		super("banmac");
	}

	/**
	 * 执行 MAC 封禁：解析时长与地址（或从目标玩家读取）后封禁。
	 * Executes MAC ban: parses duration and address (or from target player), then bans.
	 *
	 * @param params 参数：时长（分钟）、MAC（可选） / duration in minutes, optional mac
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			onFail(player, "Please add one or more parameters");
			return;
		}

		int time;
            String address;
            String targetName = "direct_type";
                
            // 尝试解析 / try parsing
		try {
			time = Integer.parseInt(params[0]); 
                if (time == 0)  //0 is 10 years since system don't allow infinte banns without rework - it's pseudo infinity
                    time = 60 * 24 * 365 * 10;
		}
		catch (NumberFormatException e) {
			onFail(player, "Please enter a valid integer amount of minutes");
            return;
		}
            // MAC 是否已定义？ / is mac defined?
            if (params.length > 1) {
                address = params[1];
            }
            else {  //no address defined
                VisibleObject target = player.getTarget();
                if (target != null && target instanceof Player) {
			if (target.getObjectId() == player.getObjectId()) {
				onFail(player, "Omg, disselect yourself please.");
				return;
			}

			Player targetpl = (Player) target;
			address = targetpl.getClientConnection().getMacAddress();
			targetName = targetpl.getName();
            targetpl.getClientConnection().closeNow();
            }
                else {
                    onFail(player, "You should select a player or give me any mac address");
                    return;
                }
            }
		GameServerNetworkServices.bannedMacManager().banAddress(address, System.currentTimeMillis() + time * 60 * 1000, "author=" + player.getName() + ", " + player.getObjectId() + "; target=" + targetName);
	}

	/**
	 * 参数错误时输出 {@code //banmac} 用法或错误提示。
	 * Prints {@code //banmac} usage or the error message on failure.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
        if (!message.equals(""))
            PacketSendUtility.sendMessage(player, message);
            PacketSendUtility.sendMessage(player, "Syntax: //banmac [time in minutes] <mac>");
            PacketSendUtility.sendMessage(player, "Note: 0 minutes will cause permanent ban");
	}
}

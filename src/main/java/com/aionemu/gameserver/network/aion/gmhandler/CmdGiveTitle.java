package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.configs.administration.PanelConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.World;

/**
 * GM 指令：为目标玩家授予称号。
 * GM command handler that grants a title to the target player.
 *
 * @author Alcapwnd
 */
public class CmdGiveTitle extends AbstractGMHandler {

	/**
	 * 创建处理器并立即授予称号。
	 * Creates the handler and immediately grants the title.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 称号 ID 字符串 / title id as string
	 */
	public CmdGiveTitle(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 校验权限后为目标玩家添加指定称号。
	 * After access check, adds the given title to the target player.
	 */
	public void run() {
		Player t = admin;

		if (admin.getClientConnection().getAccount().getAccessLevel() <= PanelConfig.GIVETITLE_PANEL_LEVEL) {
			PacketSendUtility.sendMessage(admin, "You haven't access this panel commands");
			return;
		}

		if (admin.getTarget() != null && admin.getTarget() instanceof Player) {
			t = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(admin.getTarget().getName()));
		}
		Integer titleId = Integer.parseInt(params);
		if (t != null) {
			if (!t.getTitleList().addTitle(titleId, false, 0)) {
				PacketSendUtility.sendMessage(admin,
						"you can't add title #" + titleId + " to " + (t.equals(admin) ? "yourself" : t.getName()));
			} else {
				PacketSendUtility.sendMessage(admin, "you added to " + t.getName() + " title #" + titleId);
				PacketSendUtility.sendMessage(t, admin.getName() + " gave you title #" + titleId);
			}
		}
	}
}

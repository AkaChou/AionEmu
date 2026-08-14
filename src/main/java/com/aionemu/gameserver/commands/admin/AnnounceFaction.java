package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

import java.util.Iterator;

/**
 * 管理员阵营公告命令：向天族或魔族玩家广播消息。
 * Admin faction-announce command: broadcasts a message to Elyos or Asmodian players.
 *
 * @author Divinity
 */
public class AnnounceFaction extends AdminCommand {

	/**
	 * 注册 {@code //announcefaction} 命令。
	 * Registers the {@code //announcefaction} command.
	 */
	public AnnounceFaction() {
		super("announcefaction");
	}

	/**
	 * 执行阵营公告：按 ely/asmo 过滤在线玩家并居中广播。
	 * Executes faction announce: filters online players by ely/asmo and center-broadcasts.
	 *
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length < 2) {
			PacketSendUtility.sendMessage(player, "Syntax: //announcefaction <ely | asmo> <message>");
		}
		else {
			Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
			String message = null;

			if (params[0].equals("ely"))
				message = "Elyos : ";
			else
				message = "Asmodians : ";

			// 带空格添加 / Add with space
			for (int i = 1; i < params.length - 1; i++)
				message += params[i] + " ";

			// 添加最后一项，末尾不加空格 / Add the last without the end space
			message += params[params.length - 1];

			Player target = null;

			while (iter.hasNext()) {
				target = iter.next();

				if (target.getAccessLevel() > getLevel() || target.getRace() == Race.ELYOS
					&& params[0].equals("ely"))
					PacketSendUtility.sendBrightYellowMessageOnCenter(target, message);
				else if (target.getAccessLevel() > getLevel()
					|| target.getCommonData().getRace() == Race.ASMODIANS && params[0].equals("asmo"))
					PacketSendUtility.sendBrightYellowMessageOnCenter(target, message);
			}
		}
	}

	/**
	 * 参数错误时输出 {@code //announcefaction} 用法。
	 * Prints {@code //announcefaction} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //announcefaction <ely | asmo> <message>");
	}
}

package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.drop.DropLists;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 按分段导出掉落列表 XML 的管理命令（{@code //drop}）。
 * Admin command that exports drop lists to XML by segment ({@code //drop}).
 *
 * @author Phantom, ATracer
 */
public class Drop extends AdminCommand {

	/**
	 * 注册命令名为 {@code drop}。
	 * Registers the command name {@code drop}.
	 */
	public Drop() {
		super("drop");
	}

	/**
	 * 按分段编号导出对应 NPC ID 范围的掉落数据。
	 * Exports drop data for the NPC id range of the given segment number.
	 *
	 */
	@Override
	public void execute(Player player, String... params) {
		int num = Integer.parseInt(params[0]);
		int min = 0;
		int max = 0;
		switch (num) {
			case 1:
				min = 200000;max = 212500;
				break;
			case 2:
				min = 212501;max = 215000;
				break;
			case 3:
				min = 215001;max = 217500;
				break;
			case 4:
				min = 217501;max = 260000;
				break;
			case 5:
				min = 260001;max = 840000;
				break;
		}
		DropLists.Xmlmian(min, max);
	}

}

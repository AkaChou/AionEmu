package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 切换管理员自身种族（天族/魔族）的命令（{@code //changerace}）。
 * Admin command that toggles the admin's race between Elyos and Asmodians ({@code //changerace}).
 *
 * @author ginho1
 */
public class ChangeRace extends AdminCommand {
	/**
	 * 注册命令名为 {@code changerace}。
	 * Registers the command name {@code changerace}.
	 */
	public ChangeRace() {
		super("changerace");
	}

	/**
	 * 在天族与魔族之间切换管理员种族并刷新外观。
	 * Toggles the admin race between Elyos and Asmodians and refreshes appearance.
	 *
	 */
	@Override
	public void execute(Player admin, String... params) {

		if(admin.getCommonData().getRace() == Race.ELYOS)
			admin.getCommonData().setRace(Race.ASMODIANS);
		else
			admin.getCommonData().setRace(Race.ELYOS);

		admin.clearKnownlist();
		PacketSendUtility.sendPacket(admin, new SM_PLAYER_INFO(admin, false));
		admin.updateKnownlist();
	}
}

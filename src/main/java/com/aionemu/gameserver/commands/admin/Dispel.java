package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 驱散目标玩家全部增益/减益效果的管理命令（{@code //dispel}）。
 * Admin command that removes all buff/debuff effects from the targeted player ({@code //dispel}).
 *
 * @author Hilgert
 */
public class Dispel extends AdminCommand {

	/**
	 * 注册命令名为 {@code dispel}。
	 * Registers the command name {@code dispel}.
	 */
	public Dispel() {
		super("dispel");
	}

	/**
	 * 移除目标玩家的全部效果。
	 * Removes all effects from the targeted player.
	 *
	 * admin
	 * unused
	 */
	@Override
	public void execute(Player admin, String... params) {
		Player target = null;
		VisibleObject creature = admin.getTarget();

		if (creature == null) {
			PacketSendUtility.sendMessage(admin, "You should select a target first!");
			return;
		}

		if (creature instanceof Player) {
			target = (Player) creature;
			target.getEffectController().removeAllEffects();
			PacketSendUtility.sendMessage(admin, creature.getName() + " had all buff effects dispelled !");
		}
	}

}

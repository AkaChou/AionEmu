package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对当前目标造成伤害的管理命令（{@code //damage}）。
 * Admin command that deals damage to the current target ({@code //damage}).
 *
 * @author Source
 */
public class Damage extends AdminCommand {

	/**
	 * 注册命令名为 {@code damage}。
	 * Registers the command name {@code damage}.
	 */
	public Damage() {
		super("damage");
	}

	/**
	 * 对目标生物造成固定或百分比伤害。
	 * Deals absolute or percentage damage to the targeted creature.
	 *
	 * admin
	 * @param params 伤害值或百分比（如 50%） / damage amount or percent (e.g. 50%)
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length > 1)
			onFail(admin, null);

		VisibleObject target = admin.getTarget();
		if (target == null)
			PacketSendUtility.sendMessage(admin, "No target selected");
		else if (target instanceof Creature) {
			Creature creature = (Creature) target;
			int dmg;
			try {
				String percent = params[0];
				Pattern damage = Pattern.compile("([^%]+)%");
				Matcher result = damage.matcher(percent);

				if (result.find()) {
					dmg = Integer.parseInt(result.group(1));

					if (dmg < 100)
						creature.getController().onAttack(admin, (int) (dmg / 100f * creature.getLifeStats().getMaxHp()), true);
					else
						creature.getController().onAttack(admin, creature.getLifeStats().getMaxHp() + 1, true);
				}
				else
					creature.getController().onAttack(admin, Integer.parseInt(params[0]), true);
			}
			catch (Exception ex) {
				onFail(admin, null);
			}
		}
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
		PacketSendUtility.sendMessage(player, "syntax //damage <dmg | dmg%>" + "\n<dmg> must be a number.");
	}
}

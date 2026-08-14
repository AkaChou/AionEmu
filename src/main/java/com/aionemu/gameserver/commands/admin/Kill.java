package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员击杀命令：击杀选中目标、范围内生物或已知列表中的全部生物。
 * Admin kill command: kill selected target, creatures in range, or all known creatures.
 *
 * @author ATracer, Wakizashi
 */
public class Kill extends AdminCommand {

	public Kill() {
		super("kill");
	}

	/**
	 * 无参击杀选中目标；all 击杀已知列表；否则按距离范围击杀。
	 * No args kills selection; all kills known list; otherwise kill by range.
	 *
	 * @param admin 执行命令的管理员 / Admin executing the command
	 * @param params 可选 all 或范围距离 / Optional all or range distance
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length > 1) {
			PacketSendUtility.sendMessage(admin, "syntax //kill <target | all | <range>>");
			return;
		}

		if (params.length == 0) {
			VisibleObject target = admin.getTarget();
			if (target == null) {
				PacketSendUtility.sendMessage(admin, "No target selected");
				return;
			}
			if (target instanceof Creature) {
				Creature creature = (Creature) target;
				creature.getController().onAttack(admin, creature.getLifeStats().getMaxHp() + 1, true);
			}
		}
		else {
			int range = 0;
			if (params[0].equals("all"))
				range = -1;
			else {
				try {
					range = Integer.parseInt(params[0]);
				}
				catch (NumberFormatException ex) {
					PacketSendUtility.sendMessage(admin, "<range> must be a number.");
					return;
				}
			}
			for (VisibleObject obj : admin.getKnownList().getKnownObjectsSnapshot()) {
				if (obj instanceof Creature) {
					Creature creature = (Creature) obj;
					if (range < 0 || MathUtil.isIn3dRange(admin, obj, range))
						creature.getController().onAttack(admin, creature.getLifeStats().getMaxHp() + 1, true);
				}
			}
		}
	}

	/**
	 * 参数错误时显示命令语法。
	 * Show command syntax on invalid arguments.
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //kill <target | all | <range>>");
	}
}

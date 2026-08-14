package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.services.MotionLoggingService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * 动作（Motion）日志服务调试与攻击速度调整管理员命令。
 * Admin command for motion-logging service debugging and attack-speed tweaks.
 *
 * @author kecimis
 */
public class Motion extends AdminCommand implements StatOwner {

	/**
	 * 以别名 {@code motion} 构造命令。
	 * Construct the command with alias {@code motion}.
	 */
	public Motion() {
		super("motion");
	}

	/**
	 * 执行子命令：help、start、analyze、createxml、savetosql、advanced、as。
	 * Run subcommands: help, start, analyze, createxml, savetosql, advanced, as.
	 *
	 * @param player 执行 GM / Admin player
	 * @param params 子命令与可选参数 / Subcommand and optional args
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			onFail(player, "");
			return;
		}
		if (params[0].equalsIgnoreCase("help")) {
			onFail(player, "");
			PacketSendUtility.sendMessage(player, "//motion start - starts MotionLoggingService, plus loads data from db");
			PacketSendUtility.sendMessage(player, "//motion advanced - turns on/of advanced logging info");
			PacketSendUtility.sendMessage(player, "//motion as (value) - adds attack speed");
			PacketSendUtility.sendMessage(player, "//motion analyze - creats .txt files in SERVER_DIR/motions with detailed info about motions");
			PacketSendUtility.sendMessage(player, "//motion savetosql - saves content of MotionLoggingService to database");
			PacketSendUtility.sendMessage(player, "//motion createxml - create new_motion_times.xml in definitions/compact/skills");
		}
		else if (params[0].equalsIgnoreCase("start")) {
			GameFeatureServices.motionLoggingService().start();
			PacketSendUtility.sendMessage(player, "MotionLogginService was started!\nData loaded from DB.");
		}
		else if (params[0].equalsIgnoreCase("analyze")) {
			GameFeatureServices.motionLoggingService().createAnalyzeFiles();
			PacketSendUtility.sendMessage(player, "Created testing files!");
		}
		else if (params[0].equalsIgnoreCase("createxml")) {
			GameFeatureServices.motionLoggingService().createFinalFile();
			PacketSendUtility.sendMessage(player, "Created new_motion_times.xml in definitions/compact/skills!");
		}
		else if (params[0].equalsIgnoreCase("savetosql")) {
			GameFeatureServices.motionLoggingService().saveToSql();
			PacketSendUtility.sendMessage(player, "MotionLog data saved to sql!");
		}
		else if (params[0].equalsIgnoreCase("advanced")) {
			GameFeatureServices.motionLoggingService().setAdvancedLog((!GameFeatureServices.motionLoggingService().getAdvancedLog()));
			PacketSendUtility.sendMessage(player, "AdvancedLog set to: "+GameFeatureServices.motionLoggingService().getAdvancedLog());
		}
		else if (params[0].equalsIgnoreCase("as")) {
			int parameter = 10000;
			if (params.length == 2) {
				try {
					parameter = Integer.parseInt(params[1]);
				}
				catch (NumberFormatException e) {
					PacketSendUtility.sendMessage(player, "Parameter should number");
					return;
				}
			}
			this.addAttackSpeed(player, -parameter);
			PacketSendUtility.sendMessage(player, "Attack Speed updated");
		}
		else
			onFail(player, "");
	}

	private void addAttackSpeed(Player player, int i) {
		if (i == 0) {
			player.getGameStats().endEffect(this);
		}	else {
			List<IStatFunction> modifiers = new ArrayList<IStatFunction>();
			modifiers.add(new StatAddFunction(StatEnum.ATTACK_SPEED, i, true));
			player.getGameStats().endEffect(this);
			player.getGameStats().addEffect(this, modifiers);
		}
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax: //motion <HELP|analyze|savetosql|advanced|as>");
	}
}

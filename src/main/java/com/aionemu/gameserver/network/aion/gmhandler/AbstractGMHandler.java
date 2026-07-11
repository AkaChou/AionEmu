package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令处理器抽象基类，封装管理员、参数与当前选中玩家目标。
 * Abstract base for GM command handlers, holding admin, params and selected player target.
 *
 * @author Magenik, Antraxx, Alcapwnd
 */
abstract public class AbstractGMHandler {

	/** 指令参数字符串 / Command parameter string. */
	protected String params;
	/** 执行指令的管理员 / Admin executing the command. */
	protected Player admin;
	/** 选中的玩家目标（可为 null） / Selected player target (may be null) */
	protected Player target;

	/**
	 * 构造 GM 指令处理器并解析当前选中目标。
	 * Constructs the GM handler and resolves the current selected target.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 指令参数字符串 / command parameter string
	 */
	public AbstractGMHandler(Player admin, String params) {
		this.admin = admin;
		this.params = params;
		getTarget();
	}

	/**
	 * 从管理员当前选中对象解析玩家目标；非玩家则置空。
	 * Resolves the player target from the admin's current selection; clears if not a player.
	 */
	public void getTarget() {
		VisibleObject t = admin.getTarget();
		if (t instanceof Player) {
			target = (Player) t;
			return;
		}
		target = null;
	}

	/**
	 * 检查是否已选中有效玩家目标，失败时向管理员提示。
	 * Checks whether a valid player target is selected; notifies the admin on failure.
	 *
	 * @return 目标有效返回 {@code true} / {@code true} if a valid player target is set
	 */
	public boolean checkTarget() {
		if (target != null) {
			return true;
		}

		PacketSendUtility.sendMessage(admin, "Target not found or target is not a player.");
		return false;
	}
}

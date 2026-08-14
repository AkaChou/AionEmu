package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.base.BaseLocation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.base.Base;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 管理员基地命令：列出基地归属，或将基地占领为指定阵营。
 * Admin base command: lists base ownership, or captures a base for a given race.
 *
 * @author Rinzler
 */
@SuppressWarnings("rawtypes")
public class BaseCommand extends AdminCommand
{
	private static final String COMMAND_LIST = "list";
	private static final String COMMAND_CAPTURE = "capture";
	
	/**
	 * 注册 {@code //base} 命令。
	 * Registers the {@code //base} command.
	 */
	public BaseCommand() {
		super("base");
	}
	
	/**
	 * 执行基地管理：list/capture 子命令。
	 * Executes base management: list/capture subcommands.
	 *
	 * @param params 参数：list|capture 及附加参数 / list|capture and extra args
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_LIST.equalsIgnoreCase(params[0])) {
			handleList(player, params);
		} else if (COMMAND_CAPTURE.equals(params[0])) {
			capture(player, params);
		}
	}
	
	/**
	 * 校验基地地点 ID 是否有效。
	 * Validates whether the base location id exists.
	 *
	 *
	 * @return {@code true} if valid。
	 */
	protected boolean isValidBaseLocationId(Player player, int baseId) {
		if (!GameFeatureServices.baseService().getBaseLocations().keySet().contains(baseId)) {
			PacketSendUtility.sendMessage(player, "Id " + baseId + " is invalid");
			return false;
		}
		return true;
	}
	
	/**
	 * 列出所有基地及其当前归属阵营。
	 * Lists all bases and their current owning race.
	 *
	 */
	protected void handleList(Player player, String[] params) {
		if (params.length != 1) {
			showHelp(player);
			return;
		} for (BaseLocation base : GameFeatureServices.baseService().getBaseLocations().values()) {
			PacketSendUtility.sendMessage(player, "Base:" + base.getId() + " belongs to " + base.getRace());
		}
	}
	
	/**
	 * 将指定基地占领为给定阵营。
	 * Captures the specified base for the given race.
	 *
	 * @param params 参数：capture、基地 ID、阵营 / capture, base id, race
	 */
	protected void capture(Player player, String[] params) {
		if (params.length < 3 || !NumberUtils.isCreatable(params[1])) {
			showHelp(player);
			return;
		}
		int baseId = NumberUtils.toInt(params[1]);
		if (!isValidBaseLocationId(player, baseId)) {
			return;
		}
		Race race = null;
		try {
			race = Race.valueOf(params[2].toUpperCase());
		} catch (IllegalArgumentException e) {
		} if (race == null) {
			PacketSendUtility.sendMessage(player, params[2] + " is not valid race");
			showHelp(player);
			return;
		}
		Base base = GameFeatureServices.baseService().getActiveBase(baseId);
		if (base != null) {
			GameFeatureServices.baseService().capture(baseId, race);
		}
	}
	
	/**
	 * 向管理员输出 {@code //base} 用法。
	 * Sends {@code //base} usage help to the admin.
	 *
	 */
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //base Help\n" + "//base list\n" + "//base capture <Id> <Race (ELYOS, ASMODIANS, NPC)>");
	}
}

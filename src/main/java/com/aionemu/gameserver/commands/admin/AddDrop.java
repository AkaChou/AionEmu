package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员添加掉落命令（当前未实现，仅提示）。
 * Admin add-drop command (currently unimplemented; prints a notice only).
 *
 * @author ATracer
 */
public class AddDrop extends AdminCommand {

	/**
	 * 注册 {@code //adddrop} 命令。
	 * Registers the {@code //adddrop} command.
	 */
	public AddDrop() {
		super("adddrop");
	}

	/**
	 * 执行添加掉落：当前功能未实现。
	 * Executes add-drop: currently not implemented.
	 *
	 * @param params 参数：怪物/物品/数量/几率 / mob, item, min/max, chance
	 */
	@Override
	public void execute(Player player, String... params) {
		PacketSendUtility.sendMessage(player, "Now this is not implemented.");
		/*
		if (params.length != 5) {
			onFail(player, null);
			return;
		}

		try {
			final int mobId = Integer.parseInt(params[0]);
			final int itemId = Integer.parseInt(params[1]);
			final int min = Integer.parseInt(params[2]);
			final int max = Integer.parseInt(params[3]);
			final float chance = Float.parseFloat(params[4]);

			DropList dropList = GameWorldServices.dropRegistrationService().getDropList();

			DropTemplate dropTemplate = new DropTemplate(mobId, itemId, min, max, chance, false);
			dropList.addDropTemplate(mobId, dropTemplate);

			DB.insertUpdate("INSERT INTO droplist (" + "`mob_id`, `item_id`, `min`, `max`, `chance`)" + " VALUES "
				+ "(?, ?, ?, ?, ?)", new IUStH() {

				@Override
				public void handleInsertUpdate(PreparedStatement ps) throws SQLException {
					ps.setInt(1, mobId);
					ps.setInt(2, itemId);
					ps.setInt(3, min);
					ps.setInt(4, max);
					ps.setFloat(5, chance);
					ps.execute();
				}
			});
		}
		catch (Exception ex) {
			PacketSendUtility.sendMessage(player, "Only numbers are allowed");
			return;
		}
		*/
	}

	/**
	 * 参数错误时输出 {@code //adddrop} 用法。
	 * Prints {@code //adddrop} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //adddrop <mobid> <itemid> <min> <max> <chance>");
	}
}

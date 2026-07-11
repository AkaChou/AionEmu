package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemRemodelService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：短时预览指定物品外观。
 * Player command: previews a given item appearance for a short duration.
 *
 * @author Kashim
 */
public class cmd_preview extends PlayerCommand {

	private static final int REMODEL_PREVIEW_DURATION = 15;

	/**
	 * 注册命令别名 {@code preview}。
	 * Registers the command alias {@code preview}.
	 */
	public cmd_preview() {
		super("preview");
	}

	/**
	 * 解析物品 ID 并启动短时外观预览。
	 * Parses the item id and starts a short appearance preview.
	 *
	 * @param admin 执行命令的玩家 / invoking player
	 * item-id parameters
	 */
	public void executeCommand(Player admin, String[] params) {

		if (params.length < 1 || params[0] == "") {
			PacketSendUtility.sendMessage(admin, "Syntax: .preview <itemid>");
			return;
		}

		int itemId = 0;
		try {
			itemId = Integer.parseInt(params[0]);
		}
		catch (Exception e) {
			PacketSendUtility.sendMessage(admin, "Error! Item id's are numbers like 187000090 or [item:187000090]!");
			return;
		}
		ItemRemodelService.commandViewRemodelItem(admin, itemId, REMODEL_PREVIEW_DURATION);
	}

	/**
	 * 将可变参数转发到 {@link #executeCommand(Player, String[])}。
	 * Forwards varargs to {@link #executeCommand(Player, String[])}.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * command parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		executeCommand(player, params);
	}
}

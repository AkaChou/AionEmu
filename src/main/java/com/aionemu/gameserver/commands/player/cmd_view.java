package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemRemodelService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：预览指定物品外观（较长时长）。
 * Player command: previews a given item appearance for a longer duration.
 */
public class cmd_view extends PlayerCommand
{
    private static final int REMODEL_PREVIEW_DURATION = 60;

	/**
	 * 注册命令别名 {@code view}。
	 * Registers the command alias {@code view}.
	 */
	public cmd_view() {
        super("view");
    }

	/**
	 * 解析物品 ID 并启动外观预览。
	 * Parses the item id and starts an appearance preview.
	 *
	 * @param admin 执行命令的玩家 / invoking player
	 * @param params 物品 ID 参数 / item-id parameters
	 */
    public void executeCommand(Player admin, String[] params) {
        if (params.length < 1 || params[0] == "") {
            PacketSendUtility.sendMessage(admin, "Syntax: .view <itemid>");
            return;
        }
        int itemId = 0;
        try {
            itemId = Integer.parseInt(params[0]);
        } catch (@SuppressWarnings("unused") Exception e) {
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
	 * @param params 命令参数 / command parameters
	 */
    @Override
    public void execute(Player player, String... params) {
        executeCommand(player, params);
    }
}

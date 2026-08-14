package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 玩家命令：按物品 ID 或物品链接删除背包中的一件物品。
 * Player command: deletes one inventory item by item id or item link.
 *
 * @author Source
 */
public class cmd_clean extends PlayerCommand {

	/**
	 * 注册命令别名 {@code clean}。
	 * Registers the command alias {@code clean}.
	 */
    public cmd_clean() {
        super("clean");
    }

	/**
	 * 解析物品 ID/链接并从背包减少一件该物品。
	 * Parses item id/link and decreases one matching item from inventory.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 物品 ID 或链接 / item id or link
	 */
    @Override
    public void execute(Player player, String... params) {
        String msg = "syntax .clean <item ID> or <item @link>";

        if (params.length == 0) {
            onFail(player, msg);
            return;
        }

        int itemId = 0;

        try {
            String item = params[0];
            // 部分物品链接在 Id 前有空格 / Some item links have space before Id
            if (item.equals("[item:")) {
                item = params[1];
                Pattern id = Pattern.compile("(\\d{9})");
                Matcher result = id.matcher(item);
                if (result.find()) {
                    itemId = Integer.parseInt(result.group(1));
                }
            } else {
                Pattern id = Pattern.compile("\\[item:(\\d{9})");
                Matcher result = id.matcher(item);

                if (result.find()) {
                    itemId = Integer.parseInt(result.group(1));
                } else {
                    itemId = Integer.parseInt(params[0]);
                }
            }
        } catch (NumberFormatException e) {
            try {
                String item = params[1];
                // 部分物品链接在 Id 前有空格 / Some item links have space before Id
                if (item.equals("[item:")) {
                    item = params[2];
                    Pattern id = Pattern.compile("(\\d{9})");
                    Matcher result = id.matcher(item);
                    if (result.find()) {
                        itemId = Integer.parseInt(result.group(1));
                    }
                } else {
                    Pattern id = Pattern.compile("\\[item:(\\d{9})");
                    Matcher result = id.matcher(item);

                    if (result.find()) {
                        itemId = Integer.parseInt(result.group(1));
                    } else {
                        itemId = Integer.parseInt(params[1]);
                    }
                }
            } catch (NumberFormatException ex) {
                PacketSendUtility.sendMessage(player, "You must give Id or @link to item.");
                return;
            } catch (Exception ex2) {
                onFail(player, msg);
                return;
            }
        }

        Storage bag = player.getInventory();
        Item item = bag.getFirstItemByItemId(itemId);
        if (item != null || itemId == 0) {
            bag.decreaseByObjectId(item.getObjectId(), 1);
            PacketSendUtility.sendMessage(player, "Item removed succesfully");
        } else {
            PacketSendUtility.sendMessage(player, "You don't have that item");
        }
    }

	/**
	 * 参数错误时回显失败消息。
	 * Echoes the failure message when arguments are invalid.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param message 失败提示消息 / failure message
	 */
    @Override
    public void onFail(Player player, String message) {
        PacketSendUtility.sendMessage(player, message);
    }
}

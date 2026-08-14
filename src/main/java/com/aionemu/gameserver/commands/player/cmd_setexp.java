package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：设置个人经验倍率（0.0–10.0）。
 * Player command: sets personal experience multiplier (0.0–10.0).
 */
public class cmd_setexp extends PlayerCommand {

    /**
     * 注册命令别名 {@code setexp}。
     * Registers the command alias {@code setexp}.
     */
    public cmd_setexp() {
        super("setexp");
    }

    /**
     * 解析倍率参数并写入玩家公共数据。
     * Parses the multiplier argument and stores it on player common data.
     *
     * @param player 执行命令的玩家 / invoking player
     * @param params 倍率值 / multiplier value
     */
    @Override
    public void execute(Player player, String... params) {
        if (params.length != 1) {
            onFail(player, null);
            return;
        }
        try {
            double multiplier = Double.parseDouble(params[0]);
            if (multiplier < 0 || multiplier > 10) { // 限制在0%到1000%之间
                PacketSendUtility.sendMessage(player, "Multiplier must be between 0.0 and 10.0.");
                return;
            }
            player.getCommonData().setExpMultiplier(multiplier);
            PacketSendUtility.sendMessage(player, "Experience multiplier set to " + (multiplier * 100) + "%.");
        } catch (NumberFormatException e) {
            PacketSendUtility.sendMessage(player, "Invalid multiplier value.");
        }
    }

    /**
     * 参数错误时提示用法。
     * Shows usage when arguments are invalid.
     *
     * @param player 执行命令的玩家 / invoking player
     * @param message 失败提示消息 / failure message
     */
    @Override
    public void onFail(Player player, String message) {
        PacketSendUtility.sendMessage(player, "Usage: .setexp <multiplier>");
    }
}

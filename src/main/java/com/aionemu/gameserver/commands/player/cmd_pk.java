package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：开启/关闭 PK（Bandit）模式。
 * Player command: toggles PK (Bandit) mode on or off.
 *
 * @author wanke
 */
public class cmd_pk extends PlayerCommand
{
	/**
	 * 注册命令别名 {@code pk}。
	 * Registers the command alias {@code pk}.
	 */
    public cmd_pk() {
        super("pk");
    }

	/**
	 * 在 Bandit 状态之间切换。
	 * Starts or stops Bandit mode for the player.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
    @Override
    public void execute(Player player, String... params) {
        if (!player.isBandit()) {
            GameFeatureServices.banditService().startBandit(player);
            PacketSendUtility.sendSys3Message(player, "\uE005", "<[PK] Bandit> started !!!");
        } else {
            GameFeatureServices.banditService().stopBandit(player);
            PacketSendUtility.sendSys3Message(player, "\uE005", "<[PK] Bandit> stop !!!");
        }
    }
}

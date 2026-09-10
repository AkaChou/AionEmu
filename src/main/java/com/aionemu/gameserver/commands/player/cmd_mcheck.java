package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：立即检查所有锁定任务的开启条件并尝试启动。
 * Player command: immediately checks all LOCKED missions for start conditions and starts them if met.
 *
 * @author vlog
 */
public class cmd_mcheck extends PlayerCommand {

	/**
	 * 注册命令别名 {@code mcheck}。
	 * Registers the command alias {@code mcheck}.
	 */
	public cmd_mcheck() {
		super("mcheck");
	}

	/**
	 * 遍历玩家任务列表，对 LOCKED 状态任务触发升级检查。
	 * Iterates quest states and triggers level-up checks for LOCKED quests.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		GameEngineServices.questEngine().recheckLockedQuestStates(player);
		PacketSendUtility.sendMessage(player, "Missions checked successfully");
	}

}

package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
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
		Collection<QuestState> qsl = player.getQuestStateList().getAllQuestState();
		for (QuestState qs : qsl) {
			if (qs.getStatus() == QuestStatus.LOCKED) {
				int questId = qs.getQuestId();
				GameEngineServices.questEngine().onLvlUp(new QuestEnv(null, player, questId, 0));
			}
		}
		PacketSendUtility.sendMessage(player, "Missions checked successfully");
	}

}

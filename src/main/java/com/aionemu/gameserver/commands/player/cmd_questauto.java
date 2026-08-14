package com.aionemu.gameserver.commands.player;

import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：将白名单内进行中的任务直接设为可领奖。
 * Player command: sets a whitelisted in-progress quest directly to reward status.
 *
 * @author ATracer
 */
public class cmd_questauto extends PlayerCommand {

	/**
	 * 在此配置自动任务（如 new int[]{1245,1345,7895}）。
	 * put quests for automation here (new int[]{1245,1345,7895})
	 */
	private final int[] questIds = new int[] {};

	/**
	 * 注册命令别名 {@code questauto}。
	 * Registers the command alias {@code questauto}.
	 */
	public cmd_questauto() {
		super("questauto");
	}

	/**
	 * 若任务在支持列表且处于 START，则切换为 REWARD 并同步客户端。
	 * If the quest is supported and START, switches it to REWARD and syncs the client.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 任务 ID / quest id
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(player, "syntax .questauto <questid>");
			return;
		}

		int questId = 0;
		try {
			questId = Integer.parseInt(params[0]);
		}
		catch (Exception ex) {
			PacketSendUtility.sendMessage(player, "wrong quest id");
			return;
		}
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			PacketSendUtility.sendMessage(player, "quest is not started");
			return;
		}

		if (!ArrayUtils.contains(questIds, questId)) {
			PacketSendUtility.sendMessage(player, "this quest is not supported");
			return;
		}

		qs.setStatus(QuestStatus.REWARD);
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, qs.getStatus(), qs.getQuestVars().getQuestVars()));
	}

}

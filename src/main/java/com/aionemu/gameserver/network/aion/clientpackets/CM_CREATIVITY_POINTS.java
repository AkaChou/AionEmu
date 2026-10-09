package com.aionemu.gameserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCreativityServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.panel_cp.PanelCp;
import com.aionemu.gameserver.model.templates.panel_cp.PanelCpType;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 请求分配或重置创造力点数的客户端包。
 * Client packet requesting allocation or reset of creativity points.
 * @author Falke_34
 */
@Slf4j
public class CM_CREATIVITY_POINTS extends AionClientPacket {


	private Player activePlayer;
	private int type;
	private int plusSize;
	private int id;
	private int point;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_CREATIVITY_POINTS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		activePlayer = getConnection().getActivePlayer();
		type = readC();
		switch (type) {
		case 0: // 应用 / Apply
			plusSize = readH();
			for (int i = 0; i < plusSize; i++) {
				id = readD();
				point = readH();
				PanelCp pcp = DataManager.PANEL_CP_DATA.getPanelCpId(id);

				if (pcp.getPanelCpType() == PanelCpType.STAT_UP) {
					if (point <= 255) {
						GameCreativityServices.creativityStatsService().onEssenceApply(activePlayer, type, plusSize, id, point);
					} else if (point > 255) {
						PacketSendUtility.sendBrightYellowMessageOnCenter(activePlayer, "Essence bug detected... Please reset points or relog for solv this issue!");
					}
				} else if (pcp.getPanelCpType() == PanelCpType.LEARN_SKILL) {
					GameCreativityServices.creativitySkillService().learnSkill(activePlayer, id, point);
				} else if (pcp.getPanelCpType() == PanelCpType.ENCHANT_SKILL) {
					if (point > pcp.getCountMax()) {
						log.warn(I18n.get("log.4b4619583261", pcp.getCountMax(), point, id, activePlayer.getName()));
						return;
					}
					GameCreativityServices.creativitySkillService().enchantSkill(activePlayer, id, point);
				}
			}
			PacketSendUtility.sendPacket(activePlayer, new SM_STATS_INFO(activePlayer));
			break;
		case 1: // 重置 / Reset
			plusSize = readH();
			break;
		default:
			break;
		}
	}

	@Override
	protected void runImpl() {
		if (activePlayer == null) {
			return;
		}
		if (activePlayer.getLifeStats().isAlreadyDead()) {
			return;
		}
		if (type == 1) {
			GameCreativityServices.creativityEssenceService().onResetEssence(activePlayer, plusSize);
		}
		
		// 应用创造力点数后检查任务 / Check quests after applying creativity points
		if (type == 0) {
			checkQuestCompletion(activePlayer);
		}
	}
	
	private void checkQuestCompletion(Player player) {
		// 真端 0x100 状态推进不写轴：这两个任务没有玩法内进度步，注入创造力后直接置 REWARD，
		// var0 保持接取值 0。写入打包步数 1 会让客户端任务书按 0 基行匹配落空（镜像任务 10522
		// 实机 2026-10-08 报障：REWARD/var0=1 步骤空白，//quest set 10522 reward 0 后恢复）。
		// The retail 0x100 status advance never writes the axis: neither quest has an in-play progress
		// step, so applying creativity points only sets REWARD and var0 stays at its acquired 0.
		// Writing packed step 1 breaks the client journal row match -- the journal goes blank at
		// REWARD/var0=1; per the mirror quest 10522 live report of 2026-10-08, //quest set 10522
		// reward 0 restores it.
		if (player.getQuestStateList().hasQuest(20522)) {
			QuestState qs = player.getQuestStateList().getQuestState(20522);
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				qs.setStatus(QuestStatus.REWARD);
				PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(20522, qs.getStatus(), qs.getQuestVars().getQuestVars()));
				player.getController().updateNearbyQuests();
			}
		}
		if (player.getQuestStateList().hasQuest(10522)) {
			QuestState qs = player.getQuestStateList().getQuestState(10522);
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				qs.setStatus(QuestStatus.REWARD);
				PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(10522, qs.getStatus(), qs.getQuestVars().getQuestVars()));
				player.getController().updateNearbyQuests();
			}
		}
	}
}
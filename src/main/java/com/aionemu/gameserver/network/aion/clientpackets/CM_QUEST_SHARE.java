package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端任务分享请求包，向队伍/联盟成员推送可接任务。
 * Client packet to share a quest with party or alliance members.
 */
public class CM_QUEST_SHARE extends AionClientPacket {
	public int questId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_QUEST_SHARE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		this.questId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = this.getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		var metadata = GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null);
		QuestState questState = player.getQuestStateList().getQuestState(this.questId);
		if (!canShare(metadata, questState)) {
			return;
		}
		if (player.isInGroup2()) {
			for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
				if (player == member)
					continue;
				if (!MathUtil.isIn3dRange(member, player, GroupConfig.GROUP_MAX_DISTANCE)) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100000, member.getName()));
					continue;
				}
				if ("FORCE".equals(metadata.targetType())) { // Alliance.
					PacketSendUtility.sendPacket(member, new SM_SYSTEM_MESSAGE(1100005, player.getName()));
					continue;
				}
				if (!canReceiveByState(metadata, member.getQuestStateList().getQuestState(questId))) {
					continue;
				}
				if (member.isInFlyingState()) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100003, member.getName()));
					continue;
				}
				if (!canReceiveByLevel(metadata, member.getLevel())) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100003, member.getName()));
					PacketSendUtility.sendPacket(member, new SM_SYSTEM_MESSAGE(1100003, player.getName()));
					continue;
				}
				PacketSendUtility.sendPacket(member, new SM_QUEST_ACTION(this.questId, member.getObjectId(), true));
			}
		} else if (player.isInAlliance2()) {
			for (Player member : player.getPlayerAllianceGroup2().getOnlineMembers()) {
				if (player == member)
					continue;
				if (!MathUtil.isIn3dRange(member, player, GroupConfig.GROUP_MAX_DISTANCE)) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100000, member.getName()));
					continue;
				}
				if ("UNION".equals(metadata.targetType())) { // League.
					PacketSendUtility.sendPacket(member, new SM_SYSTEM_MESSAGE(1100005, player.getName()));
					continue;
				}
				if (!canReceiveByState(metadata, member.getQuestStateList().getQuestState(questId))) {
					continue;
				}
				if (member.isInFlyingState()) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100003, member.getName()));
					continue;
				}
				if (!canReceiveByLevel(metadata, member.getLevel())) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100003, member.getName()));
					PacketSendUtility.sendPacket(member, new SM_SYSTEM_MESSAGE(1100003, player.getName()));
					continue;
				}
				PacketSendUtility.sendPacket(member, new SM_QUEST_ACTION(this.questId, member.getObjectId(), true));
			}
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1100000));
			return;
		}
	}

	static boolean canShare(QuestMetadata metadata, QuestState ownerState) {
		return metadata != null && !metadata.cannotShare() && ownerState != null
			&& ownerState.getStatus() != QuestStatus.COMPLETE;
	}

	static boolean canReceiveByState(QuestMetadata metadata, QuestState state) {
		if (metadata == null || state == null) {
			return metadata != null;
		}
		if (metadata.repeatPolicy().maxRepeatCount() == 1) {
			return state.getStatus() == QuestStatus.NONE;
		}
		return state.getStatus() != QuestStatus.START && state.getStatus() != QuestStatus.REWARD;
	}

	static boolean canReceiveByLevel(QuestMetadata metadata, int level) {
		return metadata != null && level >= metadata.minLevel() && level <= metadata.maxLevel();
	}
}

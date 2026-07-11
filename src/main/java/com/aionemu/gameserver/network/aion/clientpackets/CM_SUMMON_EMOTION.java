package com.aionemu.gameserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端召唤物情绪/状态切换请求包（飞行、拔刀、滑翔等）。
 * Client packet for summon emotion/state changes (fly, attack stance, glide, etc.).
 */
@Slf4j
public class CM_SUMMON_EMOTION extends AionClientPacket {

	@SuppressWarnings("unused")
	private int objId;

	private int emotionTypeId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SUMMON_EMOTION(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		objId = readD();
		emotionTypeId = readC();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		EmotionType emotionType = EmotionType.getEmotionTypeById(emotionTypeId);
		if (emotionType == EmotionType.UNK) {
			log.error(I18n.get("log.b376a042bdcc", Integer.toHexString(emotionTypeId).toUpperCase()));
		}
		Summon summon = player.getSummon();
		if (summon == null)
			return;
		switch (emotionType) {
		case FLY:
		case LAND:
			PacketSendUtility.broadcastPacket(summon, new SM_EMOTION(summon, EmotionType.START_EMOTE2));
			PacketSendUtility.broadcastPacket(summon, new SM_EMOTION(summon, emotionType));
			break;
		case ATTACKMODE:
			summon.setState(CreatureState.WEAPON_EQUIPPED);
			PacketSendUtility.broadcastPacket(summon, new SM_EMOTION(summon, emotionType));
			break;
		case NEUTRALMODE:
			summon.unsetState(CreatureState.WEAPON_EQUIPPED);
			PacketSendUtility.broadcastPacket(summon, new SM_EMOTION(summon, emotionType));
			break;
		case GLIDING:
			if (!summon.isInState(CreatureState.GLIDING)) {
				summon.setState(CreatureState.GLIDING);
				PacketSendUtility.broadcastPacket(summon, new SM_EMOTION(summon, emotionType));
			}
			break;
		case GLIDING_END:
			if (summon.isInState(CreatureState.GLIDING)) {
				summon.unsetState(CreatureState.GLIDING);
				PacketSendUtility.broadcastPacket(summon, new SM_EMOTION(summon, emotionType));
			}
			break;
		}
	}
}

package com.aionemu.gameserver.ai2.manager;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * NPC 表情/状态管理器：在攻击、跟随、行走、返回与待机时切换生物状态并广播情绪包。
 * NPC emote/state manager: toggles creature states and broadcasts emotion packets for attack, follow, walk, return, and idle.
 */
public class EmoteManager {

	/**
	 * 开始攻击表情：取消行走状态并装备武器，广播攻击模式情绪。
	 * Starts attack emote: clears walking state, equips weapon, and broadcasts attack-mode emotions.
	 *
	 * @param owner NPC 实例 / NPC instance
	 */
	public static final void emoteStartAttacking(Npc owner) {
		Creature target = (Creature) owner.getTarget();
        if (target == null) {
        // log.warn(I18n.get("log.72559eb3cf63", owner.getObjectId()));
        return;
        }
		owner.unsetState(CreatureState.WALKING);
		if (!owner.isInState(CreatureState.WEAPON_EQUIPPED)) {
			owner.setState(CreatureState.WEAPON_EQUIPPED);
			PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.START_EMOTE2, 0, target.getObjectId()));
			PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.ATTACKMODE, 0, target.getObjectId()));
		}
	}

	/**
	 * 停止攻击表情：卸下武器状态，若目标为玩家则发送 NPC 返回战斗提示。
	 * Stops attack emote: clears weapon-equipped state; if target is a player, sends NPC return combat message.
	 *
	 * @param owner NPC 实例 / NPC instance
	 */
	public static final void emoteStopAttacking(Npc owner) {
		owner.unsetState(CreatureState.WEAPON_EQUIPPED);
		if (owner.getTarget() != null && owner.getTarget() instanceof Player) {
			PacketSendUtility.sendPacket((Player) owner.getTarget(), SM_SYSTEM_MESSAGE.STR_UI_COMBAT_NPC_RETURN(owner.getObjectTemplate().getNameId()));
		}
	}

	/**
	 * 开始跟随表情：取消行走并广播中立模式情绪。
	 * Starts follow emote: clears walking and broadcasts neutral-mode emotions.
	 *
	 * @param owner NPC 实例 / NPC instance
	 */
	public static final void emoteStartFollowing(Npc owner) {
		owner.unsetState(CreatureState.WALKING);
		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.START_EMOTE2, 0, 0));
		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.NEUTRALMODE, 0, 0));
	}

	/**
	 * 开始行走表情：设置行走状态并广播行走情绪。
	 * Starts walk emote: sets walking state and broadcasts walk emotion.
	 *
	 * @param owner NPC 实例 / NPC instance
	 */
	public static final void emoteStartWalking(Npc owner) {
		owner.setState(CreatureState.WALKING);
		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.WALK));
	}

	/**
	 * 停止行走表情：清除行走状态。
	 * Stops walk emote: clears walking state.
	 *
	 * @param owner NPC 实例 / NPC instance
	 */
	public static final void emoteStopWalking(Npc owner) {
		owner.unsetState(CreatureState.WALKING);
	}

	/**
	 * 开始返回表情：广播中立模式情绪。
	 * Starts return emote: broadcasts neutral-mode emotions.
	 *
	 * @param owner NPC 实例 / NPC instance
	 */
	public static final void emoteStartReturning(Npc owner) {
		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.START_EMOTE2, 0, 0));
		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.NEUTRALMODE, 0, 0));
	}

	/**
	 * 开始待机表情：设置行走状态并广播中立模式情绪。
	 * Starts idle emote: sets walking state and broadcasts neutral-mode emotions.
	 *
	 * @param owner NPC 实例 / NPC instance
	 */
	public static final void emoteStartIdling(Npc owner) {
		owner.setState(CreatureState.WALKING);
		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.START_EMOTE2, 0, 0));
		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.NEUTRALMODE, 0, 0));
	}
}

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
import com.aionemu.gameserver.world.knownlist.KnownList;

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
	 * 停止攻击表情：卸下武器状态，并在目标玩家仍能看见该 NPC 时发送“放弃追踪”提示。
	 * Stops attack emote: clears the weapon state, and sends the give-up notice only while the target player
	 * can still see the NPC.
	 *
	 * @param owner NPC 实例 / NPC instance
	 */
	public static final void emoteStopAttacking(Npc owner) {
		owner.unsetState(CreatureState.WEAPON_EQUIPPED);
		if (owner.getTarget() instanceof Player playerTarget && canSeeDisengagingNpc(owner, playerTarget)) {
			PacketSendUtility.sendPacket(playerTarget,
					SM_SYSTEM_MESSAGE.STR_UI_COMBAT_NPC_RETURN(owner.getObjectTemplate().getNameId()));
		}
	}

	/**
	 * 判断脱战 NPC 是否仍在目标玩家的已知列表内（玩家客户端仍能看到它）。
	 * Whether the disengaging NPC is still inside the target player's known list (the client still renders it).
	 *
	 * <p>背景：真端 {@code max_chase_time} 到期同样会结束战斗。克罗坦要塞的空中防空眼 276225
	 * （{@code max_chase_time=8}、0 移速）追不上飞行玩家，8 秒后脱战返回出生点；此时目标玩家可能已在数百米外的
	 * 空中，若仍无条件下发 {@code STR_UI_COMBAT_NPC_RETURN}(1300039)，玩家就会在周围无怪处看到
	 * “龙族监视者之眼放弃追踪”这类幽灵提示（2026-09-21 报障）。脱战本身不变，只是不再向已经看不见该 NPC 的
	 * 玩家描述它的动向。
	 * Background: the retail {@code max_chase_time} also ends a fight. Krotan's aerial sentinel eye 276225
	 * ({@code max_chase_time=8}, zero move speed) cannot reach a flying player and disengages eight seconds later;
	 * the target may already be hundreds of meters away, so an unconditional {@code STR_UI_COMBAT_NPC_RETURN}
	 * (1300039) renders a phantom "gives up pursuit" notice in empty mid-air. Combat behaviour is unchanged; the
	 * server just stops narrating the disengage to a player who can no longer see the NPC.</p>
	 *
	 * @param owner 脱战的 NPC / the disengaging NPC
	 * @param playerTarget 该 NPC 当前的目标玩家 / the player currently targeted by the NPC
	 * @return 目标玩家已知列表仍包含该 NPC 时返回 {@code true} / {@code true} when the player still knows the NPC
	 */
	static boolean canSeeDisengagingNpc(Npc owner, Player playerTarget) {
		KnownList knownList = playerTarget.getKnownList();
		return knownList != null && knownList.knowns(owner);
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

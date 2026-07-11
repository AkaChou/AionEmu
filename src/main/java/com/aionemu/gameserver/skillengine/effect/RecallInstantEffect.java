package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 瞬时召回效果：向队友发起召回请求，接受后传送至施法者。
 * Instant recall effect: invites a teammate to teleport to the caster on accept.
 *
 * @author Bio
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RecallInstantEffect")
public class RecallInstantEffect extends EffectTemplate {

	/**
	 * 发起召回确认请求。
	 * Starts the recall confirmation request.
	 */
	@Override
	public void applyEffect(Effect effect) {
		final Creature effector = effect.getEffector();
		final Player effected = (Player) effect.getEffected();

		final int worldId = effect.getWorldId();
		final int instanceId = effect.getInstanceId();
		final float locationX = effect.getSkill().getX();
		final float locationY = effect.getSkill().getY();
		final float locationZ = effect.getSkill().getZ();
		final byte locationH = effect.getSkill().getH();

		RequestResponseHandler rrh = new RequestResponseHandler(effector) {

			@Override
			public void denyRequest(Creature effector, Player effected) {

				PacketSendUtility.sendPacket((Player) effector,
						SM_SYSTEM_MESSAGE.STR_MSG_Recall_Rejected_EFFECT(effected.getName()));
				PacketSendUtility.sendPacket(effected,
						SM_SYSTEM_MESSAGE.STR_MSG_Recall_Rejected_EFFECT(effector.getName()));
			}

			@Override
			public void acceptRequest(Creature effector, Player effected) {
				TeleportService2.teleportTo(effected, worldId, instanceId, locationX, locationY, locationZ, locationH);
			}
		};

		effected.getResponseRequester().putRequest(SM_QUESTION_WINDOW.STR_SUMMON_PARTY_DO_YOU_ACCEPT_REQUEST, rrh);
		PacketSendUtility.sendPacket(effected,
				new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_SUMMON_PARTY_DO_YOU_ACCEPT_REQUEST, 0, 0,
						effector.getName(), "Summon Group Member", 30));
	}

	/**
	 * 校验召回目标是否合法。
	 * Validates whether the recall target is legal.
	 */
	@Override
	public void calculate(Effect effect) {
		final Creature effector = effect.getEffector();

		if (!(effect.getEffected() instanceof Player)) {
			return;
		}
		Player effected = (Player) effect.getEffected();

		if (effected.getController().isInCombat()) {
			return;
		}
		if (effector.getWorldId() == effected.getWorldId() && !effector.isInInstance()
				&& !(effector.isEnemy(effected))) {
			effect.getSkill().setTargetPosition(effector.getX(), effector.getY(), effector.getZ(),
					effector.getHeading());
			effect.addSucessEffect(this);
		}
	}
}

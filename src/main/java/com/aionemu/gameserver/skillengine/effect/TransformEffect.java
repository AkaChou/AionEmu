package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerTransformDAO;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRANSFORM;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.TransformType;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 变身效果基类：切换模型/面板，并在结束时恢复或叠加其他变身。
 * Base transform effect: switches model/panel and restores or stacks other transforms on end.
 *
 * @author Sweetkr, kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TransformEffect")
public abstract class TransformEffect extends EffectTemplate {

	@XmlAttribute
	protected int model;
	@XmlAttribute
	protected TransformType type = TransformType.NONE;
	@XmlAttribute
	protected int panelid;
	@XmlAttribute
	protected int itemId;
	@XmlAttribute(name = "use_item")
	protected boolean useItem;
	@XmlAttribute(name = "transform_level")
	protected int transformLevel;
	@XmlAttribute(name = "cant_recall")
	protected boolean cantRecall;
	@XmlAttribute(name = "cant_jump")
	protected boolean cantJump;
	@XmlAttribute(name = "cant_attack")
	protected boolean cantAttack;
	@XmlAttribute(name = "cant_use_item")
	protected boolean cantUseItem;
	@XmlAttribute(name = "cant_fly")
	protected boolean cantFly;
	@XmlAttribute(name = "cant_use_skill")
	protected boolean cantUseSkill;
	@XmlAttribute(name = "cant_move")
	protected boolean cantMove;
	@XmlAttribute(name = "animation_skill_id")
	protected int animationSkillId;

	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 结束变身：清除异常状态、恢复模型并广播。
	 * Ends transform: clears abnormal, restores model and broadcasts.
	 *
	 * @param effect 运行中效果 / runtime effect
	 * @param state 关联异常状态，可为 null / related abnormal state, may be null
	 */
	public void endEffect(Effect effect, AbnormalState state) {
		final Creature effected = effect.getEffected();

		if (state != null) {
			effected.getEffectController().unsetAbnormal(state.getId());
		}
		effected.getTransformModel().removeRestrictions(
				cantFly, cantUseSkill, cantUseItem, cantAttack, cantJump, cantRecall, cantMove);
		if (!effected.getTransformModel().isActiveTransform(effect)) {
			return;
		}

		Effect nextEffect = null;
		TransformEffect nextTemplate = null;
		for (Effect candidate : effected.getEffectController().getAbnormalEffects()) {
			if (candidate == effect) {
				continue;
			}
			for (EffectTemplate template : candidate.getEffectTemplates()) {
				if (template instanceof TransformEffect transform
						&& (nextTemplate == null || transform.transformLevel > nextTemplate.transformLevel)) {
					nextEffect = candidate;
					nextTemplate = transform;
				}
			}
		}
		if (effected instanceof Player) {
			DAOManager.getDAO(PlayerTransformDAO.class).deletePlTransfo(effected.getObjectId());
		}
		if (nextTemplate != null) {
			nextTemplate.applyTransform(nextEffect);
			return;
		}
		clearTransform(effected);
	}

	/**
	 * 开始变身：设置异常状态、模型/面板并广播。
	 * Starts transform: sets abnormal, model/panel and broadcasts.
	 *
	 * @param effect 运行中效果 / runtime effect
	 * @param effectId 异常状态，可为 null / abnormal state, may be null
	 */
	public void startEffect(Effect effect, AbnormalState effectId) {
		final Creature effected = effect.getEffected();

		if (effectId != null) {
			effect.setAbnormal(effectId.getId());
			effected.getEffectController().setAbnormal(effectId.getId());
		}

		effected.getTransformModel().addRestrictions(
				cantFly, cantUseSkill, cantUseItem, cantAttack, cantJump, cantRecall, cantMove);
		if (cantFly && effected instanceof Player player) {
			player.getFlyController().endFly(true);
		}
		if (effected.getTransformModel().canReplaceActiveTransform(transformLevel)) {
			applyTransform(effect);
		}
	}

	private void applyTransform(Effect effect) {
		Creature effected = effect.getEffected();
		effected.getTransformModel().setModelId(model);
		effected.getTransformModel().setPanelId(panelid);
		effected.getTransformModel().setItemId(itemId);
		effected.getTransformModel().setTransformType(type);
		effected.getTransformModel().setActiveTransform(effect, transformLevel, useItem, animationSkillId);
		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_TRANSFORM(effected, panelid, true, itemId));

		if (effected instanceof Player player) {
			player.setTransformedModelId(model);
			player.setTransformedItemId(itemId);
			player.setTransformedPanelId(panelid);
			DAOManager.getDAO(PlayerTransformDAO.class).storePlTransfo(effected.getObjectId(), panelid, itemId);
		}
	}

	private void clearTransform(Creature effected) {
		if (effected instanceof Summon || effected instanceof Player) {
			effected.getTransformModel().setModelId(0);
		} else if (effected instanceof Npc) {
			effected.getTransformModel().setModelId(effected.getObjectTemplate().getTemplateId());
		}
		effected.getTransformModel().setPanelId(0);
		effected.getTransformModel().setItemId(0);
		effected.getTransformModel().setTransformType(effected instanceof Player ? TransformType.PC : TransformType.NONE);
		effected.getTransformModel().clearActiveTransform();
		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_TRANSFORM(effected, 0, false, 0));

		if (effected instanceof Player player) {
			player.setTransformedModelId(0);
			player.setTransformedItemId(0);
			player.setTransformedPanelId(0);
		}
	}

	/**
	 * 获取变身类型。
	 * Returns the transform type.
	 *
	 * @return 变身类型 / transform type
	 */
	public TransformType getTransformType() {
		return type;
	}

	/**
	 * 获取变身模型 ID。
	 * Returns the transform model id.
	 *
	 * @return 模型 ID / model id
	 */
	public int getTransformId() {
		return model;
	}

	/**
	 * 获取变身面板 ID。
	 * Returns the transform panel id.
	 *
	 * @return 面板 ID / panel id
	 */
	public int getPanelId() {
		return panelid;
	}
}

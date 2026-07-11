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
		if (effected instanceof Player) {
			int newModel = 0;
			TransformType transformType = TransformType.PC;
			for (Effect tmp : effected.getEffectController().getAbnormalEffects()) {
				for (EffectTemplate template : tmp.getEffectTemplates()) {
					if (template instanceof TransformEffect) {
						if (((TransformEffect) template).getTransformId() == model)
							continue;
						newModel = ((TransformEffect) template).getTransformId();
						transformType = ((TransformEffect) template).getTransformType();
						break;
					}
				}
			}
			effected.getTransformModel().setModelId(newModel);
			effected.getTransformModel().setTransformType(transformType);
			effected.getTransformModel().setItemId(0);
			DAOManager.getDAO(PlayerTransformDAO.class).deletePlTransfo(effected.getObjectId());
		} else if (effected instanceof Summon) {
			effected.getTransformModel().setModelId(0);
		} else if (effected instanceof Npc) {
			effected.getTransformModel().setModelId(effected.getObjectTemplate().getTemplateId());
		}
		effected.getTransformModel().setPanelId(0);
		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_TRANSFORM(effected, 0, false, 0));

		if (effected instanceof Player) {
			((Player) effected).setTransformed(false);
			((Player) effected).setTransformedModelId(0);
			((Player) effected).setTransformedItemId(0);
			((Player) effected).setTransformedPanelId(0);
		}
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

		effected.getTransformModel().setModelId(model);
		effected.getTransformModel().setPanelId(panelid);
		effected.getTransformModel().setItemId(itemId);
		effected.getTransformModel().setTransformType(effect.getTransformType());
		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_TRANSFORM(effected, panelid, true, itemId));

		if (effected instanceof Player) {
			((Player) effected).setTransformed(true);
			((Player) effected).setTransformedModelId(model);
			((Player) effected).setTransformedItemId(itemId);
			((Player) effected).setTransformedItemId(panelid);
			DAOManager.getDAO(PlayerTransformDAO.class).storePlTransfo(effected.getObjectId(), panelid, itemId);
		}
	}

	/**
	 * 获取变身类型。
	 * Returns the transform type.
	 *
	 * transform type
	 */
	public TransformType getTransformType() {
		return type;
	}

	/**
	 * 获取变身模型 ID。
	 * Returns the transform model id.
	 *
	 * model id
	 */
	public int getTransformId() {
		return model;
	}

	/**
	 * 获取变身面板 ID。
	 * Returns the transform panel id.
	 *
	 * panel id
	 */
	public int getPanelId() {
		return panelid;
	}
}

package com.aionemu.gameserver.skillengine.effect;


import com.aionemu.boot.i18n.I18n;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.skillengine.model.DispelType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;

/**
 * 通用驱散效果：按效果 ID、ID 范围、效果类型或槽位类型移除效果。
 * Generic dispel effect: removes effects by effect id, id range, effect type, or slot type.
 *
 * @author ATracer
 */
@Slf4j
public class DispelEffect extends EffectTemplate {

	@XmlElement(type = Integer.class)
	protected List<Integer> effectids;
	@XmlElement
	protected List<String> effecttype;
	@XmlElement
	protected List<String> slottype;
	@XmlAttribute
	protected DispelType dispeltype;
	@XmlAttribute
	protected Integer value;

	/**
	 * 按配置的驱散类型移除目标效果。
	 * Removes target effects according to the configured dispel type.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (effect.getEffected() == null || effect.getEffected().getEffectController() == null) {
			return;
		}

		if (dispeltype == null) {
			return;
		}

		if ((dispeltype == DispelType.EFFECTID || dispeltype == DispelType.EFFECTIDRANGE) && effectids == null) {
			return;
		}

		if (dispeltype == DispelType.EFFECTTYPE && effecttype == null) {
			return;
		}

		if (dispeltype == DispelType.SLOTTYPE && slottype == null) {
			return;
		}

		switch (dispeltype) {
		case EFFECTID:
			for (Integer effectId : effectids) {
				effect.getEffected().getEffectController().removeEffectByEffectId(effectId);
			}
			break;
		case EFFECTIDRANGE:
			for (int i = effectids.get(0); i <= effectids.get(1); i++) {
				effect.getEffected().getEffectController().removeEffectByEffectId(i);
			}
			break;
		case EFFECTTYPE:
			for (String type : effecttype) {
				EffectType temp = null;
				try {
					temp = EffectType.valueOf(type);
				} catch (Exception e) {
					log.error(I18n.get("log.9cc59c110d11", type, e));
				}
				if (temp != null) {
					effect.getEffected().getEffectController().removeEffectByEffectType(temp);
				}
			}
			break;
		case SLOTTYPE:
			for (String type : slottype) {
				effect.getEffected().getEffectController()
						.removeAbnormalEffectsByTargetSlot(SkillTargetSlot.valueOf(type));
			}
			break;
		}
	}
}

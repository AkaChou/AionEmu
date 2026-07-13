package com.aionemu.gameserver.skillengine.effect;


import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.skillengine.model.DispelType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;

/**
 * 通用驱散效果：按效果 ID、ID 范围、效果类型或槽位类型移除效果。
 * Generic dispel effect: removes effects by effect id, id range, effect type, or slot type.
 *
 * @author ATracer
 */
public class DispelEffect extends EffectTemplate {

	@XmlElement(type = Integer.class)
	protected List<Integer> effectids;
	@XmlElement
	protected List<String> effecttype;
	@XmlElement
	protected List<String> slottype;
	@XmlAttribute
	protected DispelType dispeltype;
	@XmlAttribute(name = "dispel_level_delta")
	protected int dispelLevelDelta;
	@XmlAttribute(name = "dispel_level")
	protected int dispelLevel = 100;
	@XmlAttribute
	protected int dpower;
	@XmlAttribute
	protected int power = 255;

	public DispelEffect() {
		value = 255;
	}

	/**
	 * 按配置的驱散类型移除目标效果。
	 * Removes target effects according to the configured dispel type.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (effect.getEffected() == null || effect.getEffected().getEffectController() == null || dispeltype == null) {
			return;
		}

		EffectController controller = effect.getEffected().getEffectController();
		int skillLevel = effect.getSkillLevel();
		switch (dispeltype) {
		case EFFECTID:
			if (effectids != null) {
				controller.removeEffectsByDispel(
					target -> target.getSuccessEffect().stream().anyMatch(et -> effectids.contains(et.getEffectid())),
					getMaxTargets(skillLevel), getDispelLevel(skillLevel), getDispelPower(skillLevel));
			}
			break;
		case EFFECTIDRANGE:
			if (effectids != null && effectids.size() >= 2) {
				int first = effectids.get(0);
				int last = effectids.get(1);
				controller.removeEffectsByDispel(
					target -> target.getSuccessEffect().stream()
						.anyMatch(et -> et.getEffectid() >= first && et.getEffectid() <= last),
					getMaxTargets(skillLevel), getDispelLevel(skillLevel), getDispelPower(skillLevel));
			}
			break;
		case EFFECTTYPE:
			Set<EffectType> effectTypes = enumValues(effecttype, EffectType.class);
			if (!effectTypes.isEmpty()) {
				controller.removeEffectsByDispel(
					target -> target.getSuccessEffect().stream().anyMatch(et -> effectTypes.contains(et.getEffectType())),
					getMaxTargets(skillLevel), getDispelLevel(skillLevel), getDispelPower(skillLevel));
			}
			break;
		case SLOTTYPE:
			Set<SkillTargetSlot> slots = enumValues(slottype, SkillTargetSlot.class);
			if (!slots.isEmpty()) {
				controller.removeEffectsByDispel(target -> slots.contains(target.getTargetSlotEnum()),
					getMaxTargets(skillLevel), getDispelLevel(skillLevel), getDispelPower(skillLevel));
			}
			break;
		}
	}

	public int getMaxTargets(int skillLevel) {
		return value + delta * skillLevel;
	}

	public int getDispelLevel(int skillLevel) {
		return dispelLevel + dispelLevelDelta * skillLevel;
	}

	public int getDispelPower(int skillLevel) {
		return power + dpower * skillLevel;
	}

	private static <E extends Enum<E>> Set<E> enumValues(List<String> values, Class<E> type) {
		Set<E> result = EnumSet.noneOf(type);
		if (values != null) {
			for (String value : values) {
				try {
					result.add(Enum.valueOf(type, value));
				} catch (IllegalArgumentException ignored) {
				}
			}
		}
		return result;
	}
}

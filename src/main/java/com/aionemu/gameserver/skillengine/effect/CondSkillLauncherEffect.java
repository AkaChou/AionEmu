package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * 条件技能触发效果：HP/MP 低于阈值时自动维持指定技能。
 * Conditional skill launcher: maintains a linked skill while HP/MP is below a threshold.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CondSkillLauncherEffect")
public class CondSkillLauncherEffect extends EffectTemplate {

	@XmlAttribute(name = "skill_id")
	protected int skillId;
	@XmlAttribute
	protected HealType type;

	/**
	 * 将效果加入受影响者的效果控制器。
	 * Adds the effect to the effected creature's effect controller.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 结束时清理属性修正与观察者。
	 * Cleans up stat modifiers and the observer on end.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getGameStats().endEffect(effect);
		ActionObserver observer = effect.getActionObserver(position);
		if (observer != null) {
			effect.getEffected().getObserveController().removeObserver(observer);
		}
	}

	/**
	 * 注册生命值观察者：低于阈值时触发关联技能，恢复后移除。
	 * Observes HP/MP, applying the linked skill below the threshold and removing it above.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		if (type != HealType.HP && type != HealType.MP) {
			return;
		}
		ActionObserver observer = new ActionObserver(ObserverType.LIFE_CHANGED) {
			private Effect conditionalEffect;

			@Override
			public synchronized void lifeChanged(HealType changedType, int currentValue) {
				if (changedType == type) {
					int maxValue = type == HealType.HP ? effect.getEffected().getLifeStats().getMaxHp()
							: effect.getEffected().getLifeStats().getMaxMp();
					if (currentValue <= value / 100f * maxValue) {
						if (conditionalEffect == null) {
							conditionalEffect = applyConditionalEffect(effect);
						}
					} else if (conditionalEffect != null) {
						conditionalEffect.endEffect();
						conditionalEffect = null;
					}
				}
			}

			@Override
			public synchronized void onRemoved() {
				if (conditionalEffect != null) {
					conditionalEffect.endEffect();
					conditionalEffect = null;
				}
			}
		};
		effect.getEffected().getObserveController().addObserver(observer);
		effect.setActionObserver(observer, position);
		int currentValue = type == HealType.HP ? effect.getEffected().getLifeStats().getCurrentHp()
				: effect.getEffected().getLifeStats().getCurrentMp();
		observer.lifeChanged(type, currentValue);
	}

	private Effect applyConditionalEffect(Effect parentEffect) {
		if (!parentEffect.getSkillTemplate().isPassive()) {
			return SkillEngine.getInstance().applyEffect(skillId, parentEffect.getEffected(), parentEffect.getEffected());
		}
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (template == null) {
			return null;
		}
		Effect effect = new Effect(parentEffect.getEffected(), parentEffect.getEffected(), template, template.getLvl(), 0);
		effect.setIsForcedEffect(true);
		effect.initialize();
		effect.setForcedDuration(true);
		effect.applyEffect();
		return effect;
	}
}

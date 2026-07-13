package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 延迟技能效果：效果持续结束后触发关联技能（可范围多目标）。
 * Delayed skill effect: fires a linked skill after the effect duration (may hit multiple targets).
 */
public class DelayedSkillEffect extends EffectTemplate {
	@XmlAttribute(name = "skill_id")
	protected int skillId;
	@XmlAttribute(name = "use_current_level")
	protected boolean useCurrentLevel;

	/**
	 * 将效果加入受影响者的效果控制器。
	 * Adds the effect to the effected creature's effect controller.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 在效果时长后施放关联技能。
	 * Casts the linked skill after the effect duration.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(final Effect effect) {
		if (effect.getSkill() == null) {
			return;
		}
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (effect.getEffected().getEffectController().hasAbnormalEffect(effect.getSkill().getSkillId())) {
					final SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillId);
					if (template == null) {
						return;
					}
					int launchedSkillLevel = Math.max(1,
						useCurrentLevel ? effect.getSkillLevel() : calculateValue(effect.getSkillLevel()));
					if (template.getProperties().getTargetMaxCount() > 1) {
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllObjects(new Visitor<VisibleObject>() {
							@Override
							public void visit(VisibleObject object) {
								if (object instanceof Creature target && MathUtil.getDistance(effect.getEffected(), target) <= template.getProperties().getRevisionDistance()) {
									GameEngineServices.skillEngine().applyEffectDirectly(template.getSkillId(), effect.getEffector(),
										target, template.getDuration(), launchedSkillLevel);
								}
							}
						});
					} else {
						Effect e = new Effect(effect.getEffector(), effect.getEffected(), template, launchedSkillLevel, 0);
						e.initialize();
						e.applyEffect();
					}
				}
			}
		}, effect.getEffectsDuration());
	}

	/**
	 * 结束延迟技能效果。
	 * Ends the delayed skill effect.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
	}
}

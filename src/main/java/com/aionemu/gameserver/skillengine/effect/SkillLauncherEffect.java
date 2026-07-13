package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * 技能发射效果：立即以配置的 skill_id 对目标再施放一个技能。
 * Skill launcher effect: immediately applies another skill identified by skill_id.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SkillLauncherEffect")
public class SkillLauncherEffect extends EffectTemplate {

	@XmlAttribute(name = "skill_id")
	protected int skillId;
	@XmlAttribute
	protected int delay;
	@XmlAttribute
	protected String group;

	/**
	 * 通过技能引擎应用配置的技能效果。
	 * Applies the configured skill effect through the skill engine.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Runnable launch = () -> launch(effect);
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(launch, delay);
		} else {
			launch.run();
		}
	}

	private void launch(Effect effect) {
		if ("FORCE".equals(group) && effect.getEffected() instanceof Player player && player.isInAlliance2()) {
			for (Player member : player.getPlayerAlliance2().getOnlineMembers()) {
				if (member.getWorldId() == player.getWorldId() && member.getInstanceId() == player.getInstanceId()) {
					launch(effect.getEffector(), member);
				}
			}
			return;
		}
		launch(effect.getEffector(), effect.getEffected());
	}

	private void launch(Creature effector, Creature effected) {
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (template == null) {
			return;
		}
		Effect launched = new Effect(effector, effected, template, Math.max(1, value), 0);
		launched.initialize();
		launched.applyEffect();
	}

	/**
	 * 返回要发射的技能 ID。
	 * Returns the skill id to launch.
	 */
	public int getLaunchSkillId() {
		return skillId;
	}
}

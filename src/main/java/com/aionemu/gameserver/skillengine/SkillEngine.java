package com.aionemu.gameserver.skillengine;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 技能引擎门面：按模板/玩家技能列表创建并应用技能与效果。
 * Skill engine facade: create and apply skills/effects from templates and player skill lists.
 *
 * @author ATracer
 */
public class SkillEngine {

	/**
	 * 静态单例引用。
	 * Static singleton reference.
	 */
	public static final SkillEngine skillEngine = new SkillEngine();

	/**
	 * Spring 实例提供者（可选覆盖静态单例）。
	 * Optional Spring provider that may override the static singleton.
	 */
	private static volatile ObjectProvider<SkillEngine> instanceProvider;

	/**
	 * 私有构造，禁止外部直接实例化。
	 * Private constructor; not for direct instantiation.
	 */
	private SkillEngine() {

	}

	/**
	 * 为玩家已学习技能创建 Skill 实例。
	 * Creates a Skill for a skill the player has learned.
	 *
	 * casting player
	 * skill id
	 * first target
	 *
	 * @return Skill 实例，模板不存在时为 null / skill instance, or null if template missing
	 */
	public Skill getSkillFor(Player player, int skillId, VisibleObject firstTarget) {
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillId);

		if (template == null) {
			return null;
		}
		return getSkillFor(player, template, firstTarget);
	}

	/**
	 * 按模板为玩家已学习技能创建 Skill 实例（非激怒激活时须已学习）。
	 * Creates a Skill from a template for a learned player skill (must be learned unless PROVOKED).
	 *
	 * casting player
	 * skill template
	 * first target
	 *
	 * @return Skill 实例，未学习且非激怒时为 null / skill instance, or null if not learned and not provoked
	 */
	public Skill getSkillFor(Player player, SkillTemplate template, VisibleObject firstTarget) {
		// 玩家没有该技能且未被挑衅 / player doesn't have such skill and ist not provoked
		if (template.getActivationAttribute() != ActivationAttribute.PROVOKED) {
			if (!player.getSkillList().isSkillPresent(template.getSkillId())) {
				return null;
			}
		}

		Creature target = null;
		if (firstTarget instanceof Creature) {
			target = (Creature) firstTarget;
		}
		return new Skill(template, player, target);
	}

	/**
	 * 按模板与指定技能等级为玩家创建 Skill 实例。
	 * Creates a Skill for a player with an explicit skill level.
	 *
	 * casting player
	 * skill template
	 * first target
	 * skill level
	 * skill instance
	 */
	public Skill getSkillFor(Player player, SkillTemplate template, VisibleObject firstTarget, int skillLevel) {
		Creature target = null;
		if (firstTarget instanceof Creature) {
			target = (Creature) firstTarget;
		}
		return new Skill(template, player, target, skillLevel);
	}

	/**
	 * 为未学习技能（如物品技能）创建 Skill 实例。
	 * Creates a Skill for skills not learned by the player (e.g. item skills).
	 *
	 * caster
	 * skill id
	 * skill level
	 * first target
	 *
	 * @return Skill 实例，模板不存在时为 null / skill instance, or null if template missing
	 */
	public Skill getSkill(Creature creature, int skillId, int skillLevel, VisibleObject firstTarget) {
		return getSkill(creature, skillId, skillLevel, firstTarget, null);
	}

	/**
	 * 为未学习技能创建 Skill，可附带物品模板。
	 * Creates a Skill for non-learned skills, optionally bound to an item template.
	 *
	 * caster
	 * skill id
	 * skill level
	 * first target
	 * @param itemTemplate 关联物品模板，可为 null / related item template, may be null
	 * @return Skill 实例，模板不存在时为 null / skill instance, or null if template missing
	 */
	public Skill getSkill(Creature creature, int skillId, int skillLevel, VisibleObject firstTarget,
			ItemTemplate itemTemplate) {
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (template == null) {
			return null;
		}
		Creature target = null;
		if (firstTarget instanceof Creature) {
			target = (Creature) firstTarget;
		}
		return new Skill(template, creature, skillLevel, target, itemTemplate);
	}

	/**
	 * 获取技能引擎实例（优先 Spring 提供者，否则静态单例）。
	 * Returns the skill engine instance (Spring provider if set, else static singleton).
	 *
	 * skill engine instance
	 */
	public static SkillEngine getInstance() {
		ObjectProvider<SkillEngine> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> skillEngine);
		}
		return skillEngine;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<SkillEngine> provider) {
		instanceProvider = provider;
	}

	/**
	 * 强制直接对目标应用技能效果（忽略学习状态，可指定持续时长）。
	 * Applies a skill effect directly to the target as a forced effect (optional duration).
	 *
	 * skill id
	 * caster
	 * effected creature
	 * @param duration 强制持续时长（毫秒），&gt;0 时锁定时长 / forced duration in ms; &gt;0 locks duration
	 */
	public void applyEffectDirectly(int skillId, Creature effector, Creature effected, int duration) {
		applyEffectDirectly(skillId, effector, effected, duration, 0);
	}

	/**
	 * 强制直接应用指定等级的技能效果；等级不为正时沿用模板等级。
	 * Applies a forced effect at an explicit skill level; non-positive values use the template level.
	 */
	public void applyEffectDirectly(int skillId, Creature effector, Creature effected, int duration, int skillLevel) {
		SkillTemplate st = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (st == null) {
			return;
		}
		final Effect ef = new Effect(effector, effected, st, skillLevel > 0 ? skillLevel : st.getLvl(), duration);
		ef.setIsForcedEffect(true);
		ef.initialize();
		if (duration > 0) {
			ef.setForcedDuration(true);
		}
		ef.applyEffect();
	}

	/**
	 * 对目标应用普通技能效果，保留命中与抗性判定。
	 * Applies a regular skill effect while preserving hit and resistance checks.
	 *
	 * @return 创建的效果，技能不存在时为 null / created effect, or null if the skill is missing
	 */
	public Effect applyEffect(int skillId, Creature effector, Creature effected) {
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (template == null) {
			return null;
		}
		Effect effect = new Effect(effector, effected, template, template.getLvl(), 0);
		effect.initialize();
		effect.applyEffect();
		return effect;
	}
}

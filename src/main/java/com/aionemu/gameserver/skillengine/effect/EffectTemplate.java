package com.aionemu.gameserver.skillengine.effect;


import com.aionemu.boot.i18n.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.change.Change;
import com.aionemu.gameserver.skillengine.condition.Conditions;
import com.aionemu.gameserver.skillengine.effect.modifier.ActionModifier;
import com.aionemu.gameserver.skillengine.effect.modifier.ActionModifiers;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HitType;
import com.aionemu.gameserver.skillengine.model.HopType;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.SkillType;
import com.aionemu.gameserver.skillengine.model.SpellStatus;
import com.aionemu.gameserver.skillengine.model.TransformType;
import com.aionemu.gameserver.utils.stats.StatFunctions;

import lombok.extern.slf4j.Slf4j;

/**
 * 技能效果模板基类：计算、应用、开始/结束效果，并持有 XML 绑定字段。
 * Base skill effect template: calculate/apply/start/end effects; holds XML-bound fields.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Effect")
@Slf4j
public abstract class EffectTemplate {
	static final Set<StatEnum> ABNORMAL_RESISTANCE_STATS = Set.of(
			StatEnum.BLEED_RESISTANCE, StatEnum.BIND_RESISTANCE, StatEnum.BLIND_RESISTANCE,
			StatEnum.CHARM_RESISTANCE, StatEnum.CONFUSE_RESISTANCE, StatEnum.CURSE_RESISTANCE,
			StatEnum.DEFORM_RESISTANCE, StatEnum.DISEASE_RESISTANCE, StatEnum.FEAR_RESISTANCE,
			StatEnum.OPENAREIAL_RESISTANCE, StatEnum.PARALYZE_RESISTANCE, StatEnum.PERIFICATION_RESISTANCE,
			StatEnum.NOFLY_RESISTANCE, StatEnum.POISON_RESISTANCE, StatEnum.PULLED_RESISTANCE,
			StatEnum.ROOT_RESISTANCE, StatEnum.SIMPLE_ROOT_RESISTANCE,
			StatEnum.SILENCE_RESISTANCE, StatEnum.SLEEP_RESISTANCE, StatEnum.SLOW_RESISTANCE,
			StatEnum.SNARE_RESISTANCE, StatEnum.SPIN_RESISTANCE, StatEnum.STAGGER_RESISTANCE,
			StatEnum.STUMBLE_RESISTANCE, StatEnum.STUN_RESISTANCE);
	static final Set<StatEnum> STUNLIKE_RESISTANCE_STATS = Set.of(
			StatEnum.CHARM_RESISTANCE, StatEnum.OPENAREIAL_RESISTANCE, StatEnum.SPIN_RESISTANCE,
			StatEnum.STAGGER_RESISTANCE, StatEnum.STUMBLE_RESISTANCE, StatEnum.STUN_RESISTANCE);
	static final Set<StatEnum> AR_ALL_RESISTANCE_STATS = Set.of(
			StatEnum.BLEED_RESISTANCE, StatEnum.BIND_RESISTANCE, StatEnum.BLIND_RESISTANCE,
			StatEnum.CONFUSE_RESISTANCE, StatEnum.CURSE_RESISTANCE, StatEnum.DEFORM_RESISTANCE,
			StatEnum.DISEASE_RESISTANCE, StatEnum.FEAR_RESISTANCE, StatEnum.PARALYZE_RESISTANCE,
			StatEnum.NOFLY_RESISTANCE, StatEnum.PERIFICATION_RESISTANCE, StatEnum.POISON_RESISTANCE,
			StatEnum.PULLED_RESISTANCE, StatEnum.ROOT_RESISTANCE, StatEnum.SILENCE_RESISTANCE,
			StatEnum.SIMPLE_ROOT_RESISTANCE, StatEnum.SLEEP_RESISTANCE,
			StatEnum.SLOW_RESISTANCE, StatEnum.SNARE_RESISTANCE);

	protected ActionModifiers modifiers;
	protected List<Change> change;
	@XmlAttribute
	protected int effectid;
	@XmlAttribute(required = true)
	protected int duration2;
	@XmlAttribute
	protected int duration1;
	@XmlAttribute(name = "randomtime")
	protected int randomTime;
	@XmlAttribute(name = "e")
	protected int position;
	@XmlAttribute(name = "basiclvl")
	protected int basicLvl;
	@XmlAttribute(name = "hittype", required = false)
	protected HitType hitType = HitType.EVERYHIT;
	@XmlAttribute(name = "hittypeprob2", required = false)
	protected int hitTypeProb = 1000;
	@XmlAttribute(name = "hittypeprob1", required = false)
	protected int hitTypeProbDelta;
	@XmlAttribute(name = "element")
	protected SkillElement element = SkillElement.NONE;
	@XmlElement(name = "subeffect")
	protected SubEffect subEffect;
	@XmlElement(name = "conditions")
	protected Conditions effectConditions;
	@XmlElement(name = "subconditions")
	protected Conditions effectSubConditions;
	@XmlAttribute(name = "hoptype")
	protected HopType hopType;
	@XmlAttribute(name = "hopa")
	protected int hopA; // effects the agro-value (hate)
	@XmlAttribute(name = "hopb")
	protected int hopB; // effects the agro-value (hate)
	@XmlAttribute(name = "noresist")
	protected boolean noResist;
	@XmlAttribute(name = "accmod1")
	protected int accMod1;// accdelta
	@XmlAttribute(name = "accmod2")
	protected int accMod2;// accvalue
	@XmlAttribute(name = "mrresist")
	protected boolean mrResist = true;
	@XmlAttribute(name = "preeffect")
	protected String preEffect;
	@XmlAttribute(name = "preeffect_prob")
	protected int preEffectProb = 100;
	@XmlAttribute(name = "preeffect_prob_delta")
	protected int preEffectProbDelta;
	@XmlAttribute(name = "critprobmod2")
	protected int critProbMod2 = 100;
	@XmlAttribute(name = "critprobmod1")
	protected int critProbMod1;
	@XmlAttribute(name = "critadddmg1")
	protected int critAddDmg1 = 0;
	@XmlAttribute(name = "critadddmg2")
	protected int critAddDmg2 = 0;
	@XmlAttribute
	protected int value;
	@XmlAttribute
	protected int delta;
	@XmlAttribute
	protected boolean consume = true;
	@XmlTransient
	protected EffectType effectType = null;
	@XmlTransient

	/**
	 * 获取效果基础数值。
	 * Returns the base effect value.
	 *
	 * @return 基础数值 / base value
	 */
	public int getValue() {
		return value;
	}

	public boolean isMrResist() {
		return mrResist;
	}

	/**
	 * 获取每技能等级的数值增量。
	 * Returns the per-skill-level delta.
	 *
	 * @return 每级增量 / level delta
	 */
	public int getDelta() {
		return delta;
	}

	protected int calculateValue(int skillLevel) {
		return value + delta * skillLevel;
	}

	public boolean isConsume() {
		return consume;
	}

	/**
	 * 获取第二时长参数（主持续时长）。
	 * Returns the primary duration parameter (duration2).
	 *
	 * @return 第二时长参数 / duration2
	 */
	public int getDuration2() {
		return duration2;
	}

	/**
	 * 获取第一时长参数。
	 * Returns the secondary duration parameter (duration1).
	 *
	 * @return 第一时长参数 / duration1
	 */
	public int getDuration1() {
		return duration1;
	}

	/**
	 * 获取随机时间偏移。
	 * Returns the random time offset.
	 *
	 * @return 随机时间偏移 / random time
	 */
	public int getRandomTime() {
		return randomTime;
	}

	/**
	 * 获取行动修正器集合。
	 * Returns the action modifiers container.
	 *
	 * @return 修正器集合 / modifiers
	 */
	public ActionModifiers getModifiers() {
		return modifiers;
	}

	/**
	 * 获取属性变更列表。
	 * Returns the stat change list.
	 *
	 * @return 属性变更列表 / changes
	 */
	public List<Change> getChange() {
		return change;
	}

	/**
	 * 获取效果 ID。
	 * Returns the effect id.
	 *
	 * @return 效果 ID / effect id
	 */
	public int getEffectid() {
		return effectid;
	}

	/**
	 * 获取效果在技能中的位置序号。
	 * Returns the effect position index within the skill.
	 *
	 * @return 位置序号 / position
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * 获取基础等级要求。
	 * Returns the basic level requirement.
	 *
	 * @return 基础等级要求 / basic level
	 */
	public int getBasicLvl() {
		return basicLvl;
	}

	/**
	 * 按技能等级返回命中类型触发概率（千分比）。
	 * Returns the hit-type trigger probability in per mille for a skill level.
	 */
	public int getHitTypeProbability(int skillLevel) {
		return hitTypeProb + hitTypeProbDelta * skillLevel;
	}

	/**
	 * 获取技能元素属性。
	 * Returns the skill element.
	 *
	 * @return 元素属性 / element
	 */
	public SkillElement getElement() {
		return element;
	}

	/**
	 * 获取前置效果位置串（下划线分隔）。
	 * Returns the pre-effect position string (underscore-separated).
	 *
	 * @return 前置效果串 / pre-effect string
	 */
	public String getPreEffect() {
		return preEffect;
	}

	/**
	 * 获取前置效果触发概率（0–100）。
	 * Returns the pre-effect trigger probability (0–100).
	 *
	 * @return 触发概率 / probability
	 */
	public int getPreEffectProb() {
		return preEffectProb;
	}

	int getPreEffectProbability(int skillLevel) {
		return preEffectProb + preEffectProbDelta * skillLevel;
	}

	/**
	 * 获取暴击概率修正。
	 * Returns the critical probability modifier.
	 *
	 * @return 暴击概率修正 / crit probability mod
	 */
	public int getCritProbMod2() {
		return critProbMod2;
	}

	public int getCritProbMod1() {
		return critProbMod1;
	}

	int getCriticalProbability(int skillLevel) {
		return critProbMod2 + critProbMod1 * skillLevel;
	}

	/**
	 * 获取暴击附加伤害 1。
	 * Returns critical bonus damage 1.
	 *
	 * @return 暴击附加伤害 / bonus damage
	 */
	public int getCritAddDmg1() {
		return critAddDmg1;
	}

	/**
	 * 获取暴击附加伤害 2。
	 * Returns critical bonus damage 2.
	 *
	 * @return 暴击附加伤害 / bonus damage
	 */
	public int getCritAddDmg2() {
		return critAddDmg2;
	}

	int getCriticalAdditionalDamage(int skillLevel) {
		return critAddDmg2 + critAddDmg1 * skillLevel;
	}

	/**
	 * 获取效果条件集合。
	 * Returns the effect conditions.
	 *
	 * @return 条件集合 / conditions
	 */
	public Conditions getEffectConditions() {
		return effectConditions;
	}

	/**
	 * 获取子效果条件集合。
	 * Returns the sub-effect conditions.
	 *
	 * @return 子效果条件 / sub-effect conditions
	 */
	public Conditions getEffectSubConditions() {
		return effectSubConditions;
	}

	/**
	 * 选取首个条件成立的行动修正器。
	 * Selects the first action modifier whose condition matches.
	 *
	 * @param effect 运行中效果 / runtime effect
	 * @return 匹配的修正器，无则 null / matching modifier, or null
	 */
	protected ActionModifier getActionModifiers(Effect effect) {
		if (modifiers == null) {
			return null;
		}

		for (ActionModifier modifier : modifiers.getActionModifiers()) {
			if (modifier.check(effect)) {
				return modifier;
			}
		}
		return null;
	}

	/**
	 * 获取效果类型（反序列化后缓存）。
	 * Returns the effect type (cached after unmarshalling).
	 *
	 * @return 效果类型 / effect type
	 */
	public EffectType getEffectType() {
		return effectType;
	}

	/**
	 * 获取子效果配置。
	 * Returns the sub-effect configuration.
	 *
	 * @return 子效果配置 / sub-effect
	 */
	public SubEffect getSubEffect() {
		return subEffect;
	}

	/**
	 * 计算效果是否成功（无额外抗性参数）。
	 * Calculates whether the effect succeeds (no extra resist params).
	 *
	 * @param effect 运行中效果 / runtime effect
	 */
	public void calculate(Effect effect) {
		calculate(effect, null, null);
	}

	/**
	 * 完整效果命中计算：条件、前置效果、抗性、闪避/魔抗，成功则登记。
	 * Full hit calculation: conditions, pre-effects, resist, dodge/magic resist; registers on success.
	 * <p>
	 * 步骤 / Steps: 1) conditions 2) pre-effect 3) effect resist 4) noresist
	 * 5) physical/magical 6) cannotmiss 7) dodge/magic resist 8) add success
	 *
	 * @param effect 运行中效果 / runtime effect
	 * @param statEnum 相关抗性属性，可为 null / related resist stat, may be null
	 * @param spellStatus 成功时设置的法术状态，可为 null / spell status on success, may be null
	 * @return 若成功则为 true / true if successful
	 */
	public boolean calculate(Effect effect, StatEnum statEnum, SpellStatus spellStatus) {
		if (effect.getSkillTemplate().isPassive()) {
			this.addSuccessEffect(effect, spellStatus);
			return true;
		}

		if (statEnum != null && isAlteredState(statEnum) && isImuneToAbnormal(effect, statEnum)) {
			return false;
		}
		if (effect.getIsForcedEffect()) {
			this.addSuccessEffect(effect, spellStatus);
			return true;
		}

		if (!effectConditionsCheck(effect)) {
			return false;
		}

		if (this.getPosition() > 1) {
			List<Integer> positions = getPreEffects();
			for (int pos : positions) {
				if (!effect.isInSuccessEffects(pos)) {
					return false;
				}
			}

			if (Rnd.get(100) >= getPreEffectProbability(effect.getSkillLevel())) {
				return false;
			}
		}

		if (!this.calculateEffectResistRate(effect, statEnum)) {
			if (!effect.isDamageEffect()) {
				effect.clearSucessEffects();
			}
			effect.setAttackStatus(AttackStatus.BUF);
			return false;
		}

		SkillType skillType = effect.getSkillType();
		if (isMagicalEffectTemp()) {
			skillType = SkillType.MAGICAL;
		}

		boolean cannotMiss = false;
		if (this instanceof SkillAttackInstantEffect)
			cannotMiss = ((SkillAttackInstantEffect) this).isCannotmiss();
		if (!noResist && !cannotMiss) {
			int boostResist = 0;
			switch (effect.getSkillTemplate().getSubType()) {
			case DEBUFF:
				boostResist = effect.getEffector().getGameStats().getStat(StatEnum.BOOST_RESIST_DEBUFF, 0).getCurrent();
				break;
			default:
				break;
			}

			int accMod = accMod2 + accMod1 * effect.getSkillLevel() + effect.getAccModBoost() + boostResist
					+ effect.getEffector().getObserveController().getSkillAccuracyModifier(skillType);
			switch (skillType) {
			case PHYSICAL:
				switch (effect.getEffector().getAttackType()) {
				case PHYSICAL:
					if (StatFunctions.calculatePhysicalDodgeRate(effect.getEffector(), effect.getEffected(), accMod)) {
						return false;
					}
					break;
				default:
					if (Rnd.nextInt(1000) < StatFunctions.calculateMagicalResistRate(effect.getEffector(),
							effect.getEffected(), accMod, getElement())) {
						return false;
					}
					break;
				}
				break;
			case MAGICAL:
				if (Rnd.nextInt(1000) < StatFunctions.calculateMagicalResistRate(effect.getEffector(),
						effect.getEffected(), accMod, getElement())) {
					return false;
				}
			default:
				break;
			}
		}

		this.addSuccessEffect(effect, spellStatus);
		return true;
	}

	private void addSuccessEffect(Effect effect, SpellStatus spellStatus) {
		effect.addSucessEffect(this);
		if (spellStatus != null) {
			effect.setSpellStatus(spellStatus);
		}
	}

	/**
	 * 检查效果模板的全部条件状态。
	 * Check all condition statuses for effect template
	 */
	private boolean effectConditionsCheck(Effect effect) {
		Conditions effectConditions = getEffectConditions();
		return effectConditions != null ? effectConditions.validate(effect) : true;
	}

	private List<Integer> getPreEffects() {
		List<Integer> preEffects = new ArrayList<Integer>();

		if (this.getPreEffect() == null) {
			return preEffects;
		}

		String[] parts = this.getPreEffect().split("_");
		for (String part : parts) {
			preEffects.add(Integer.parseInt(part));
		}
		return preEffects;
	}

	/**
	 * 将效果应用到被影响者。
	 * Applies the effect to the effected creature.
	 *
	 * @param effect 运行中效果 / runtime effect
	 */
	public abstract void applyEffect(Effect effect);

	/**
	 * 在被影响者上开始效果（默认空实现）。
	 * Starts the effect on the effected (empty by default).
	 *
	 * @param effect 运行中效果 / runtime effect
	 */
	public void startEffect(Effect effect) {
	};

	/**
	 * 计算并初始化子效果。
	 * Calculates and initializes the sub-effect.
	 *
	 * @param effect 运行中效果 / runtime effect
	 */
	public void calculateSubEffect(Effect effect) {
		if (subEffect == null) {
			return;
		}
		// 子效果条件的预检查 / Pre-Check for sub effect conditions
		if (!effectSubConditionsCheck(effect)) {
			effect.setSubEffectAborted(true);
			return;
		}

		// 触发子效果的几率 / chance to trigger subeffect
		if (Rnd.get(100) > subEffect.getChance()) {
			return;
		}

		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(subEffect.getSkillId());
		int level = 1;
		if (subEffect.isAddEffect()) {
			level = effect.getSignetBurstedCount();
		}
		Effect newEffect = new Effect(effect.getEffector(), effect.getEffected(), template, level, 0);
		newEffect.setAccModBoost(effect.getAccModBoost());
		newEffect.initialize();
		if (newEffect.getSpellStatus() != SpellStatus.DODGE && newEffect.getSpellStatus() != SpellStatus.RESIST) {
			effect.setSpellStatus(newEffect.getSpellStatus());
		}
		effect.setSubEffect(newEffect);
		effect.setSkillMoveType(newEffect.getSkillMoveType());
		effect.setTragetLoc(newEffect.getTargetX(), newEffect.getTargetY(), newEffect.getTargetZ());
	}

	/**
	 * 检查效果的全部子效果条件状态。
	 * Check all sub effect condition statuses for effect
	 */
	private boolean effectSubConditionsCheck(Effect effect) {
		return effectSubConditions != null ? effectSubConditions.validate(effect) : true;
	}

	/**
	 * 按 hop 类型累计仇恨（仅在效果成功时有效）。
	 * Accumulates hate by hop type (only when the effect succeeded).
	 *
	 * @param effect 运行中效果 / runtime effect
	 */
	public void calculateHate(Effect effect) {
		if (hopType == null) {
			return;
		}

		if (effect.getSuccessEffect().isEmpty()) {
			return;
		}

		int currentHate = effect.getEffectHate();
		if (hopType != null) {
			switch (hopType) {
			case DAMAGE:
				currentHate += effect.getReserved1();
				break;
			case SKILLLV:
				int skillLvl = effect.getSkillLevel();
				currentHate += hopB + hopA * skillLvl; // Agro-value of the effect
			default:
				break;
			}
		}
		if (currentHate == 0) {
			currentHate = 1;
		}
		effect.setEffectHate(StatFunctions.calculateHate(effect.getEffector(), currentHate));
	}

	/**
	 * 应用已计算的子效果。
	 * Applies the already-calculated sub-effect.
	 *
	 * @param effect 运行中效果 / runtime effect
	 */
	public void startSubEffect(Effect effect) {
		if (subEffect == null) {
			return;
		}
		// 子效果条件的应用-检查 / Apply-Check for sub effect conditions
		if (effect.isSubEffectAbortedBySubConditions()) {
			return;
		}
		if (effect.getSubEffect() != null) {
			effect.getSubEffect().applyEffect();
		}
	}

	/**
	 * 周期触发时的动作（默认空实现）。
	 * Periodic tick action (empty by default).
	 *
	 * @param effect 运行中效果 / runtime effect
	 */
	public void onPeriodicAction(Effect effect) {
	}

	/**
	 * 结束效果（默认空实现）。
	 * Ends the effect (empty by default).
	 *
	 * @param effect 运行中效果 / runtime effect
	 */
	public void endEffect(Effect effect) {
	}

	/**
	 * 计算效果抗性检定：true 表示未抵抗，false 表示被抵抗。
	 * Rolls effect resistance: true = not resisted, false = resisted.
	 *
	 * @param effect 运行中效果 / runtime effect
	 * @param statEnum 抗性属性，null 视为通过 / resist stat; null means pass
	 * @return 是否通过抗性 / true if not resisted
	 */
	public boolean calculateEffectResistRate(Effect effect, StatEnum statEnum) {

		if (effect.getEffected() == null || effect.getEffected().getGameStats() == null || effect.getEffector() == null
				|| effect.getEffector().getGameStats() == null) {
			return false;
		}
		Creature effected = effect.getEffected();
		Creature effector = effect.getEffector();

		if (statEnum == null) {
			return true;
		}
		if (noResist) {
			return true;
		}

		int resistance = effected.getGameStats().getStat(statEnum, 0).getCurrent();
		StatEnum penetrationStat = this.getPenetrationStat(statEnum);
			int penetration = penetrationStat == null ? 0
					: effector.getGameStats().getStat(penetrationStat, 0).getCurrent();
			if (isAlteredState(statEnum)) {
				if (AR_ALL_RESISTANCE_STATS.contains(statEnum)) {
					resistance += effected.getGameStats().getStat(StatEnum.ABNORMAL_RESISTANCE_ALL, 0).getCurrent();
				}
				if (STUNLIKE_RESISTANCE_STATS.contains(statEnum)) {
					resistance += effected.getGameStats().getStat(StatEnum.STUNLIKE_RESISTANCE, 0).getCurrent();
				}
				int resistChance = calculateAbnormalResistChance(resistance, penetration,
						getExclusiveStatusResistance(effect), effected.isInState(CreatureState.RESTING));
				return Rnd.get(1, 1000) > resistChance;
			}
		int effectPower = 1000 - resistance + penetration;
		return Rnd.get(1000) <= effectPower;
	}

	static int calculateAbnormalResistChance(int resistance, int penetration, int exclusiveResistance, boolean resting) {
		int chance = resistance - penetration;
		if (resting) {
			chance = (int) (chance * 0.3f);
		}
		return Math.max(0, Math.min(1000, chance + exclusiveResistance));
	}

	private boolean isImuneToAbnormal(Effect effect, StatEnum statEnum) {
		Creature effected = effect.getEffected();
		if (effected != effect.getEffector()) {
			if (effected instanceof Npc) {
				Npc npc = (Npc) effected;
				if (npc.getObjectTemplate().isImmuneTo(statEnum) || npc.hasEntity() || npc instanceof Kisk
						|| npc.getAi2().ask(AIQuestion.CAN_RESIST_ABNORMAL).isPositive()) {
					return true;
				}
				if (npc.getObjectTemplate().getStatsTemplate().getRunSpeed() == 0) {
					if (statEnum == StatEnum.PULLED_RESISTANCE || statEnum == StatEnum.STAGGER_RESISTANCE
							|| statEnum == StatEnum.STUMBLE_RESISTANCE) {
						return true;
					}
				}
			}
			if (effected.getTransformModel().getType() == TransformType.AVATAR) {
				if (statEnum == StatEnum.SLOW_RESISTANCE) {
					return true;
				}
			}
		}
		return false;
	}

	private int getExclusiveStatusResistance(Effect effect) {
		SkillTemplate skill = effect.getSkillTemplate();
		if (!(effect.getEffected() instanceof Player player)
				|| skill.getExclusiveAttribute() == null || DataManager.SKILL_DATA == null) {
			return 0;
		}
		return DataManager.SKILL_DATA.getExclusiveStatusImmune(
				player.getEquipment().getEquippedItemIds(), skill.getExclusiveAttribute());
	}

	private boolean isAlteredState(StatEnum stat) {
		return ABNORMAL_RESISTANCE_STATS.contains(stat);
	}

	private StatEnum getPenetrationStat(StatEnum statEnum) {
		switch (statEnum) {
		case BLEED_RESISTANCE:
			return StatEnum.BLEED_RESISTANCE_PENETRATION;
		case BLIND_RESISTANCE:
			return StatEnum.BLIND_RESISTANCE_PENETRATION;
		case BIND_RESISTANCE:
			return StatEnum.BIND_RESISTANCE_PENETRATION;
		case CHARM_RESISTANCE:
			return StatEnum.CHARM_RESISTANCE_PENETRATION;
		case CONFUSE_RESISTANCE:
			return StatEnum.CONFUSE_RESISTANCE_PENETRATION;
		case CURSE_RESISTANCE:
			return StatEnum.CURSE_RESISTANCE_PENETRATION;
		case DISEASE_RESISTANCE:
			return StatEnum.DISEASE_RESISTANCE_PENETRATION;
		case DEFORM_RESISTANCE:
			return StatEnum.DEFORM_RESISTANCE_PENETRATION;
		case FEAR_RESISTANCE:
			return StatEnum.FEAR_RESISTANCE_PENETRATION;
		case NOFLY_RESISTANCE:
			return StatEnum.NOFLY_RESISTANCE_PENETRATION;
		case OPENAREIAL_RESISTANCE:
			return StatEnum.OPENAREIAL_RESISTANCE_PENETRATION;
		case PARALYZE_RESISTANCE:
			return StatEnum.PARALYZE_RESISTANCE_PENETRATION;
		case PERIFICATION_RESISTANCE:
			return StatEnum.PERIFICATION_RESISTANCE_PENETRATION;
		case POISON_RESISTANCE:
			return StatEnum.POISON_RESISTANCE_PENETRATION;
		case PULLED_RESISTANCE:
			return StatEnum.PULLED_RESISTANCE_PENETRATION;
		case ROOT_RESISTANCE:
			return StatEnum.ROOT_RESISTANCE_PENETRATION;
		case SILENCE_RESISTANCE:
			return StatEnum.SILENCE_RESISTANCE_PENETRATION;
		case SIMPLE_ROOT_RESISTANCE:
			return StatEnum.SIMPLE_ROOT_RESISTANCE_PENETRATION;
		case SLEEP_RESISTANCE:
			return StatEnum.SLEEP_RESISTANCE_PENETRATION;
		case SLOW_RESISTANCE:
			return StatEnum.SLOW_RESISTANCE_PENETRATION;
		case SNARE_RESISTANCE:
			return StatEnum.SNARE_RESISTANCE_PENETRATION;
		case SPIN_RESISTANCE:
			return StatEnum.SPIN_RESISTANCE_PENETRATION;
		case STAGGER_RESISTANCE:
			return StatEnum.STAGGER_RESISTANCE_PENETRATION;
		case STUMBLE_RESISTANCE:
			return StatEnum.STUMBLE_RESISTANCE_PENETRATION;
		case STUN_RESISTANCE:
			return StatEnum.STUN_RESISTANCE_PENETRATION;
		default:
			return null;
		}
	}

	/**
	 * 部分效果即使用于物理技能仍属魔法，含护法星/杀星/弓星等眩晕。
	 * certain effects are magical even when used in physical skills; includes stuns from chanter/sin/ranger etc these effects(effecttemplates) are
	 * dependent on magical accuracy and magical resist
	 * 
	 * @return
	 */
	private boolean isMagicalEffectTemp() {
		if (this instanceof SilenceEffect || this instanceof SleepEffect || this instanceof RootEffect
				|| this instanceof SnareEffect || this instanceof StunEffect || this instanceof PoisonEffect
				|| this instanceof BindEffect || this instanceof BleedEffect || this instanceof BlindEffect
				|| this instanceof DeboostHealEffect || this instanceof ParalyzeEffect || this instanceof SlowEffect)
			return true;
		return false;
	}

	void afterUnmarshal(Unmarshaller u, Object parent) {
		EffectType temp = null;
		try {
			temp = EffectType
					.valueOf(this.getClass().getName().replaceAll("com.aionemu.gameserver.skillengine.effect.", "")
							.replaceAll("Effect", "").toUpperCase());
		} catch (Exception e) {
			log.info(I18n.get("log.67edc30354d5", this.getClass().getName()));
		}
		this.effectType = temp;
	}
}

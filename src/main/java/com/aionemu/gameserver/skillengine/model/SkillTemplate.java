package com.aionemu.gameserver.skillengine.model;

import java.util.HashMap;
import java.util.Iterator;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.skillengine.action.Actions;
import com.aionemu.gameserver.skillengine.condition.ChainCondition;
import com.aionemu.gameserver.skillengine.condition.Condition;
import com.aionemu.gameserver.skillengine.condition.Conditions;
import com.aionemu.gameserver.skillengine.condition.HpCondition;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.EffectType;
import com.aionemu.gameserver.skillengine.effect.Effects;
import com.aionemu.gameserver.skillengine.periodicaction.PeriodicActions;
import com.aionemu.gameserver.skillengine.properties.Properties;

/**
 * 技能静态模板：绑定属性、条件、效果、动作与冷却等配置。
 * Skill static template: properties, conditions, effects, actions and cooldown.
 *
 * @author ATracer modified by Wakizashi
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "skillTemplate", propOrder = { "properties", "startconditions", "useconditions",
		"useequipmentconditions", "effects", "actions", "periodicActions", "motion", "retailFields" })
public class SkillTemplate {
	protected Properties properties;
	protected Conditions startconditions;
	protected Conditions useconditions;
	protected Conditions useequipmentconditions;
	protected Effects effects;
	protected Actions actions;
	@XmlElement(name = "periodicactions")
	protected PeriodicActions periodicActions;
	protected Motion motion;
	@XmlElement(name = "retail_fields")
	protected RetailSkillFields retailFields;
	@XmlAttribute(name = "skill_id", required = true)
	protected int skillId;
	@XmlAttribute(name = "name_desc")
	protected String namedesc;
	@XmlAttribute(required = true)
	protected String name;
	@XmlAttribute(required = true)
	protected int nameId;
	@XmlAttribute
	protected String stack = "NONE";
	@XmlAttribute
	protected String skillgroup = "NONE";
	@XmlAttribute(name = "skill_group_name")
	protected String skill_group_name;
	@XmlAttribute
	protected int delayId;
	@XmlAttribute
	protected int lvl;
	@XmlAttribute(name = "skilltype", required = true)
	protected SkillType type = SkillType.NONE;
	@XmlAttribute(name = "type_message")
	protected SkillType messageType;
	@XmlAttribute(name = "skillsubtype", required = true)
	protected SkillSubType subType;
	@XmlAttribute(name = "tslot")
	protected SkillTargetSlot targetSlot;
	@XmlAttribute(name = "tslot_level")
	protected int targetSlotLevel;
	@XmlAttribute(name = "dispel_category")
	protected DispelCategoryType dispelCategory = DispelCategoryType.NONE;
	@XmlAttribute(name = "req_dispel_level")
	protected int reqDispelLevel;
	@XmlAttribute(name = "req_dispel_count")
	protected Integer reqDispelCount;
	@XmlAttribute(name = "activation", required = true)
	protected ActivationAttribute activationAttribute;
	@XmlAttribute(required = true)
	protected int duration;
	@XmlAttribute(name = "apply_casting_time_bonus")
	protected boolean applyCastingTimeBonus = true;
	@XmlAttribute(name = "no_save_on_logout")
	protected boolean noSaveOnLogout;
	@XmlAttribute(name = "spend_time_on_logout")
	protected boolean spendTimeOnLogout;
	@XmlAttribute(name = "remain_cooltime_on_login")
	protected boolean remainCooltimeOnLogin;
	@XmlAttribute(name = "no_jump_cancel")
	protected boolean noJumpCancel;
	@XmlAttribute(name = "toggle_timer")
	protected int toggleTimer;
	@XmlAttribute(name = "cooldown")
	protected int cooldown;
	@XmlAttribute(name = "cooldown_delta")
	protected int cooldownDelta;
	@XmlAttribute(name = "delay_type")
	protected int delayType;
	@XmlAttribute(name = "nonchained_cooldown")
	protected int nonchainedCooldown;
	@XmlAttribute(name = "penalty_skill_id")
	protected int penaltySkillId;
	@XmlAttribute(name = "penalty_skill_message")
	protected boolean penaltySkillMessage;
	@XmlAttribute(name = "pvp_damage")
	protected int pvpDamage;
	@XmlAttribute(name = "pvp_duration")
	protected int pvpDuration;
	@XmlAttribute(name = "chain_skill_prob")
	protected int chainSkillProb;
	@XmlAttribute(name = "cancel_rate")
	protected int cancelRate;
	@XmlAttribute(name = "stance")
	protected boolean stance;
	@XmlAttribute(name = "stance_type")
	protected int stanceType;
	@XmlAttribute(name = "stance_usable")
	protected boolean stanceUsable;
	@XmlAttribute(name = "skillset_exception")
	protected int skillSetException;
	@XmlAttribute(name = "skillset_maxoccur")
	protected int skillSetMaxOccur;
	@XmlAttribute(name = "avatar")
	protected boolean isDeityAvatar;
	@XmlAttribute(name = "ground")
	protected boolean isGroundSkill;
	@XmlAttribute(name = "ammospeed")
	protected int ammoSpeed;
	@XmlAttribute
	protected int obstacle;
	@XmlAttribute(name = "conflict_id")
	protected int conflictId;
	@XmlAttribute(name = "counter_skill")
	protected AttackStatus counterSkill = null;
	@XmlAttribute(name = "noremoveatdie")
	protected boolean noRemoveAtDie = false;
	@XmlAttribute(name = "remove_flyend")
	protected boolean removeFlyEnd;
	@XmlAttribute(name = "applymboost")
	protected boolean applyMboost = true;
	@XmlAttribute(name = "applyhealboost")
	protected boolean applyHealBoost = true;
	@XmlAttribute(name = "applymphealboost")
	protected boolean applyMpHealBoost = true;
	@XmlAttribute(name = "applymcrit")
	protected boolean applyMcrit = true;
	@XmlAttribute(name = "hostile_type")
	protected HostileType hostileType = HostileType.DIRECT;
	@XmlAttribute(name = "charge_set_name")
	protected String charge_set_name;
	@XmlAttribute(name = "damage_attenuation")
	protected String damageAttenuation;
	@XmlAttribute(name = "broadcast_use_message")
	protected boolean broadcastUseMessage;
	@XmlAttribute(name = "hide_decrease_count")
	protected int hideDecreaseCount;
	@XmlAttribute(name = "is_familiar_skill")
	protected boolean familiarSkill;
	@XmlAttribute(name = "max_maintain_count")
	protected int maxMaintainCount;
	@XmlAttribute(name = "target_stop")
	protected int targetStop;
	@XmlAttribute(name = "ultra_skill")
	protected boolean ultraSkill;
	@XmlAttribute(name = "ultra_transfer")
	protected boolean ultraTransfer;
	@XmlAttribute(name = "exclusive_attribute")
	protected String exclusiveAttribute;
	@XmlAttribute(name = "stigma")
	protected StigmaType stigmaType = StigmaType.NONE;
	@XmlTransient
	protected HashMap<Integer, Integer> effectIds = null;
	@XmlAttribute(name = "skill_group")
	private String skill_group;
	/**
	 * 获取技能属性（射程、目标等）。
	 * Gets skill properties (range, target, etc.).
	 *
	 */
	public Properties getProperties() {
		return properties;
	}
	/**
	 * 获取开始施法条件。
	 * Gets start-cast conditions.
	 *
	 */
	public Conditions getStartconditions() {
		return startconditions;
	}
	/**
	 * 获取使用条件。
	 * Gets use conditions.
	 *
	 */
	public Conditions getUseconditions() {
		return useconditions;
	}
	/**
	 * 获取装备使用条件。
	 * Gets equipment use conditions.
	 *
	 */
	public Conditions getUseEquipmentconditions() {
		return useequipmentconditions;
	}
	/**
	 * 获取效果集合。
	 * Gets effect collection.
	 *
	 */
	public Effects getEffects() {
		return effects;
	}
	/**
	 * 获取动作集合（消耗等）。
	 * Gets action collection (costs, etc.).
	 *
	 */
	public Actions getActions() {
		return actions;
	}
	/**
	 * 获取周期动作。
	 * Gets periodic actions.
	 *
	 */
	public PeriodicActions getPeriodicActions() {
		return periodicActions;
	}
	/**
	 * 获取动作时间配置。
	 * Gets motion timing config.
	 *
	 */
	public Motion getMotion() {
		return motion;
	}

	public RetailSkillFields getRetailFields() {
		return retailFields;
	}
	/**
	 * 获取技能 ID。
	 * Gets skill id.
	 *
	 */
	public int getSkillId() {
		return skillId;
	}
	/**
	 * 获取技能名称。
	 * Gets skill name.
	 *
	 */
	public String getName() {
		return name;
	}
	/**
	 * 获取名称字符串 ID。
	 * Gets name string id.
	 *
	 */
	public int getNameId() {
		return nameId;
	}
	/**
	 * 获取效果堆叠键。
	 * Gets effect stack key.
	 *
	 */
	public String getStack() {
		return stack;
	}
	/**
	 * 获取技能分组。
	 * Gets skill group.
	 *
	 */
	public String getSkillGroup() {
		return skill_group;
	}
	/**
	 * 获取技能组显示名。
	 * Gets skill group display name.
	 *
	 */
	public String getGroup() {
		return skill_group_name;
	}
	/**
	 * 获取技能等级。
	 * Gets skill level.
	 *
	 */
	public int getLvl() {
		return lvl;
	}
	/**
	 * 获取技能类型（物理/魔法）。
	 * Gets skill type (physical/magical).
	 *
	 */
	public SkillType getType() {
		return type;
	}

	public SkillType getMessageType() {
		return messageType == null ? type : messageType;
	}
	/**
	 * 获取技能子类型。
	 * Gets skill sub type.
	 *
	 */
	public SkillSubType getSubType() {
		return subType;
	}
	/**
	 * 获取目标槽位。
	 * Gets target slot.
	 *
	 */
	public SkillTargetSlot getTargetSlot() {
		return targetSlot;
	}
	/**
	 * 获取目标槽位等级。
	 * Gets target slot level.
	 *
	 */
	public int getTargetSlotLevel() {
		return targetSlotLevel;
	}
	/**
	 * 获取驱散分类。
	 * Gets dispel category.
	 *
	 */
	public DispelCategoryType getDispelCategory() {
		return dispelCategory;
	}
	/**
	 * 获取所需驱散等级。
	 * Gets required dispel level.
	 *
	 */
	public int getReqDispelLevel() {
		return reqDispelLevel;
	}

	/**
	 * 获取所需驱散计数（维持型默认 30，其余默认 10）。
	 * Gets required dispel count (30 for maintain skills, 10 otherwise).
	 *
	 * @return 所需驱散计数 / required dispel count
	 */
	public int getReqDispelCount() {
		return reqDispelCount != null ? reqDispelCount : isMaintain() ? 30 : 10;
	}
	/**
	 * 获取效果持续时间。
	 * Gets effect duration.
	 *
	 */
	public int getDuration() {
		return duration;
	}

	public boolean isApplyCastingTimeBonus() {
		return applyCastingTimeBonus;
	}

	public boolean isNoSaveOnLogout() {
		return noSaveOnLogout;
	}

	public boolean isSpendTimeOnLogout() {
		return spendTimeOnLogout;
	}

	public boolean isRemainCooltimeOnLogin() {
		return remainCooltimeOnLogin;
	}

	/**
	 * 获取切换型计时。
	 * Gets toggle timer.
	 *
	 */
	public int getToggleTimer() {
		return toggleTimer;
	}

	/**
	 * 获取烙印类型。
	 * Gets stigma type.
	 *
	 */
	public StigmaType getStigmaType() {
		return stigmaType;
	}
	/**
	 * 获取激活属性。
	 * Gets activation attribute.
	 *
	 */
	public ActivationAttribute getActivationAttribute() {
		return activationAttribute;
	}

	/**
	 * 是否被动技能。
	 * Whether this is a passive skill.
	 *
	 */
	public boolean isPassive() {
		return activationAttribute == ActivationAttribute.PASSIVE;
	}

	/**
	 * 是否切换型技能。
	 * Whether this is a toggle skill.
	 *
	 */
	public boolean isToggle() {
		return activationAttribute == ActivationAttribute.TOGGLE;
	}

	/**
	 * 是否激怒/反击触发。
	 * Whether this is a provoked skill.
	 *
	 */
	public boolean isProvoked() {
		return activationAttribute == ActivationAttribute.PROVOKED;
	}

	/**
	 * 是否维持型技能。
	 * Whether this is a maintain skill.
	 *
	 */
	public boolean isMaintain() {
		return activationAttribute == ActivationAttribute.MAINTAIN;
	}

	/**
	 * 是否主动技能。
	 * Whether this is an active skill.
	 *
	 */
	public boolean isActive() {
		return activationAttribute == ActivationAttribute.ACTIVE;
	}
	/**
	 * 按位置获取效果模板（1-based）。
	 * Gets effect template by 1-based position.
	 *
	 * @param position 1-based 位置 / 1-based position
	 * @return 效果模板，无则 null / effect template or null
	 */
	public EffectTemplate getEffectTemplate(int position) {
		return effects != null && effects.getEffects().size() >= position ? effects.getEffects().get(position - 1)
				: null;

	}
	/**
	 * 获取冷却时间。
	 * Gets cooldown.
	 *
	 */
	public int getCooldown() {
		return cooldown;
	}

	public int getCooldownDelta() {
		return cooldownDelta;
	}

	public int scaleCooldownByAttackDelay(int cooldown, int attackDelay) {
		return delayType == 1 ? (int) (cooldown * (attackDelay / 1000f)) : cooldown;
	}

	public int getNonchainedCooldown() {
		return nonchainedCooldown;
	}

	public String getExclusiveAttribute() {
		return exclusiveAttribute;
	}

	/**
	 * 获取惩罚技能 ID。
	 * Gets penalty skill id.
	 *
	 */
	public int getPenaltySkillId() {
		return penaltySkillId;
	}

	public boolean isPenaltySkillMessage() {
		return penaltySkillMessage;
	}
	/**
	 * 获取 PvP 伤害系数。
	 * Gets PvP damage factor.
	 *
	 */
	public int getPvpDamage() {
		return pvpDamage;
	}
	/**
	 * 获取 PvP 持续时间。
	 * Gets PvP duration.
	 *
	 */
	public int getPvpDuration() {
		return pvpDuration;
	}
	/**
	 * 获取连锁技能概率。
	 * Gets chain skill probability.
	 *
	 */
	public int getChainSkillProb() {
		return chainSkillProb;
	}
	/**
	 * 获取施法被打断概率。
	 * Gets cast cancel rate.
	 *
	 */
	public int getCancelRate() {
		return cancelRate;
	}
	/**
	 * 是否姿态技能。
	 * Whether this is a stance skill.
	 *
	 */
	public boolean isStance() {
		return getStanceType() != 0;
	}

	public int getStanceType() {
		return stanceType != 0 ? stanceType : stance ? 1 : 0;
	}

	public boolean isStanceUsable() {
		return stanceUsable;
	}
	/**
	 * 获取技能组例外标识。
	 * Gets skill-set exception id.
	 *
	 */
	public int getSkillSetException() {
		return skillSetException;
	}
	/**
	 * 获取技能组最大共存数。
	 * Gets skill-set max occurrence.
	 *
	 */
	public int getSkillSetMaxOccur() {
		return skillSetMaxOccur;
	}

	/**
	 * 是否含复活效果。
	 * Whether has resurrect effect.
	 *
	 */
	public boolean hasResurrectEffect() {
		return getEffects() != null && getEffects().isResurrect();
	}

	/**
	 * 是否含物品飞行值治疗。
	 * Whether has item FP heal effect.
	 *
	 */
	public boolean hasItemHealFpEffect() {
		return getEffects() != null && getEffects().isEffectTypePresent(EffectType.PROCFPHEALINSTANT);
	}

	/**
	 * 是否含闪避效果。
	 * Whether has evade effect.
	 *
	 */
	public boolean hasEvadeEffect() {
		return getEffects() != null && getEffects().isEffectTypePresent(EffectType.EVADE);
	}

	/**
	 * 是否含瞬时召回。
	 * Whether has recall instant effect.
	 *
	 */
	public boolean hasRecallInstant() {
		return getEffects() != null && getEffects().isEffectTypePresent(EffectType.RECALLINSTANT);
	}

	/**
	 * 是否应用魔法暴击。
	 * Whether magical crit is applied.
	 *
	 * @return 是否魔法暴击 / mcrit applied
	 */
	public boolean isMcritApplied() {
		return applyMcrit;
	}

	public boolean isMboostApplied() {
		return applyMboost;
	}

	public boolean isHealBoostApplied() {
		return applyHealBoost;
	}

	public boolean isMpHealBoostApplied() {
		return applyMpHealBoost;
	}

	public HostileType getHostileType() {
		return hostileType;
	}

	public boolean hasDamageAttenuation() {
		return damageAttenuation != null;
	}

	public boolean isBroadcastUseMessage() {
		return broadcastUseMessage;
	}

	public int getHideDecreaseCount() {
		return hideDecreaseCount;
	}

	public boolean isFamiliarSkill() {
		return familiarSkill;
	}

	public int getMaxMaintainCount() {
		return maxMaintainCount;
	}

	public boolean isTargetStop() {
		return targetStop != 0;
	}

	public boolean isUltraSkill() {
		return ultraSkill;
	}

	public boolean isUltraTransfer() {
		return ultraTransfer;
	}

	/**
	 * 是否含治疗效果。
	 * Whether has heal effect.
	 *
	 */
	public boolean hasHealEffect() {
		return getEffects() != null && (getEffects().isEffectTypePresent(EffectType.HEAL)
				|| getEffects().isEffectTypePresent(EffectType.HEALINSTANT));
	}

	/**
	 * 是否含随机位移效果。
	 * Whether has random move effect.
	 *
	 * @return 是否随机位移 / has random move
	 */
	public boolean hasRandomMoveEffect() {
		return getEffects() != null && (getEffects().isEffectTypePresent(EffectType.RANDOMMOVELOC));
	}

	/**
	 * 获取冷却延迟 ID（无则用 skillId）。
	 * Gets delay/cooldown id (falls back to skillId).
	 *
	 */
	public int getDelayId() {
		return (delayId > 0) ? delayId : skillId;
	}

	/**
	 * 是否神性化身技能。
	 * Whether deity avatar skill.
	 *
	 */
	public boolean isDeityAvatar() {
		return isDeityAvatar;
	}

	/**
	 * 是否地面技能。
	 * Whether ground skill.
	 *
	 */
	public boolean isGroundSkill() {
		return isGroundSkill;
	}

	/**
	 * 获取反击所需攻击状态。
	 * Gets counter-skill attack status.
	 *
	 */
	public AttackStatus getCounterSkill() {
		return counterSkill;
	}

	/**
	 * 获取弹药速度。
	 * Gets ammo speed.
	 *
	 */
	public int getAmmoSpeed() {
		return ammoSpeed;
	}

	public int getObstacle() {
		return obstacle;
	}

	/**
	 * 获取冲突 ID。
	 * Gets conflict id.
	 *
	 */
	public int getConflictId() {
		return conflictId;
	}

	/**
	 * 获取名称描述键。
	 * Gets name description key.
	 *
	 */
	public String getNamedesc() {
		return namedesc;
	}

	/**
	 * 死亡时是否不移除。
	 * Whether not removed on death.
	 *
	 * @return 死亡不移除 / no remove at die
	 */
	public boolean isNoRemoveAtDie() {
		return noRemoveAtDie;
	}

	public boolean isRemoveFlyEnd() {
		return removeFlyEnd;
	}

	public boolean isNoJumpCancel() {
		return noJumpCancel;
	}

	/**
	 * 获取充能集合名。
	 * Gets charge set name.
	 *
	 */
	public String getChargeSetName() {
		return charge_set_name;
	}

	/**
	 * 按技能等级计算效果总时长。
	 * Computes effects duration for skill level.
	 *
	 */
	public int getEffectsDuration(int skillLevel) {
		int duration = 0;
		Iterator<EffectTemplate> itr = getEffects().getEffects().iterator();
		while (itr.hasNext() && duration == 0) {
			EffectTemplate et = itr.next();
			int effectDuration = et.getDuration2() + et.getDuration1() * skillLevel;
			if (et.getRandomTime() > 0) {
				effectDuration -= Rnd.get(et.getRandomTime());
			}
			duration = duration > effectDuration ? duration : effectDuration;
		}
		return duration;
	}

	/**
	 * 从开始条件中提取连锁条件。
	 * Extracts chain condition from start conditions.
	 *
	 * @return 连锁条件，无则 null / chain condition or null
	 */
	public ChainCondition getChainCondition() {
		if (startconditions != null) {
			for (Condition cond : startconditions.getConditions()) {
				if (cond instanceof ChainCondition) {
					return (ChainCondition) cond;
				}
			}
		}
		return null;
	}

	/**
	 * 获取效果 ID→基础等级映射。
	 * Gets effect-id to basic-level map.
	 *
	 */
	public HashMap<Integer, Integer> getEffectIds() {
		return this.effectIds;
	}

	/**
	 * JAXB 反序列化钩子：构建效果 ID 到基础等级的映射。
	 * JAXB unmarshal hook: builds the effect-id to basic-level map.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (this.getEffects() != null && this.getEffects().getEffects() != null) {
			for (EffectTemplate et : this.getEffects().getEffects()) {
				if (et.getEffectid() != 0) {
					if (effectIds == null) {
						effectIds = new HashMap<Integer, Integer>();
					}
					effectIds.put(et.getEffectid(), et.getBasicLvl());
				}
			}
		}
	}
	/**
	 * 从开始条件中提取 HP 条件。
	 * Extracts HP condition from start conditions.
	 *
	 * @return HP 条件，无则 null / HP condition or null
	 */
	public HpCondition getHpCondition() {
		if (startconditions != null) {
			for (Condition cond : startconditions.getConditions()) {
				if (cond instanceof HpCondition) {
					return (HpCondition) cond;
				}
			}
		}
		return null;
	}
}

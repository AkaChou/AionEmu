package com.aionemu.gameserver.skillengine.model;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Future;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STANCE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_ACTIVATION;
import com.aionemu.gameserver.skillengine.condition.Conditions;
import com.aionemu.gameserver.skillengine.effect.AuthorizeBoostEffect;
import com.aionemu.gameserver.skillengine.effect.DamageEffect;
import com.aionemu.gameserver.skillengine.effect.DelayedSpellAttackInstantEffect;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.Effects;
import com.aionemu.gameserver.skillengine.effect.EnchantBoostEffect;
import com.aionemu.gameserver.skillengine.effect.FearEffect;
import com.aionemu.gameserver.skillengine.effect.HideEffect;
import com.aionemu.gameserver.skillengine.effect.ParalyzeEffect;
import com.aionemu.gameserver.skillengine.effect.PetOrderUseUltraSkillEffect;
import com.aionemu.gameserver.skillengine.effect.ProcAtkInstantEffect;
import com.aionemu.gameserver.skillengine.effect.SanctuaryEffect;
import com.aionemu.gameserver.skillengine.effect.SummonEffect;
import com.aionemu.gameserver.skillengine.effect.TransformEffect;
import com.aionemu.gameserver.skillengine.periodicaction.PeriodicAction;
import com.aionemu.gameserver.skillengine.periodicaction.PeriodicActions;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 运行时效果实例：绑定施法者/目标，驱动效果初始化、应用、周期与结束。
 * Runtime effect instance: binds effector/effected and drives init, apply, tick and end.
 *
 * @author ATracer
 */
public class Effect implements StatOwner {

	private Skill skill;
	private SkillTemplate skillTemplate;
	private int skillLevel;
	private int duration;
	private long endTime;
	private PeriodicActions periodicActions;
	private SkillMoveType skillMoveType = SkillMoveType.DEFAULT;
	private Creature effected;
	private Creature effector;
	private Future<?> task = null;
	private Future<?>[] periodicTasks = null;
	private Future<?> periodicActionsTask = null;
	private boolean isHideEffect = false;
	private boolean isParalyzeEffect = false;
	private boolean isSanctuaryEffect = false;
	private float targetX = 0;
	private float targetY = 0;
	private float targetZ = 0;
	private int mpShield = 0;
	private int reserved1;
	private final IdentityHashMap<EffectTemplate, Integer> reserved1ByEffect = new IdentityHashMap<>();
	private EffectTemplate currentEffectTemplate;
	private int reserved2;
	private int reserved3;
	private int reserved4;
	private int reserved5;
	private int[] reservedInts;
	private SpellStatus spellStatus = SpellStatus.NONE;
	private DashStatus dashStatus = DashStatus.NONE;
	private AttackStatus attackStatus = AttackStatus.NORMALHIT;
	private int shieldDefense;
	private int reflectedDamage = 0;
	private int reflectedSkillId = 0;
	private int protectedSkillId = 0;
	private int protectedDamage = 0;
	private int protectorId = 0;
	private boolean addedToController;
	private AttackCalcObserver[] attackStatusObserver;
	private AttackCalcObserver[] attackShieldObserver;
	private boolean launchSubEffect = true;
	private Effect subEffect;
	private boolean isStopped;
	private boolean isDelayedDamage;
	private boolean isDamageEffect;
	private boolean isPetOrder;
	private boolean isSummoning;
	// 经验加成。 / Xp Boost.
	private boolean isXpBoost;
	// AP 加成。 / Ap Boost.
	private boolean isApBoost;
	// Dr 加成。 / Dr Boost.
	private boolean isDrBoost;
	// Bdr 加成。 / Bdr Boost.
	private boolean isBdrBoost;
	// 授权加成。 / Authorize Boost.
	private boolean isAuthorizeBoost;
	// 强化加成。 / Enchant Boost.
	private boolean isEnchantBoost;
	// 强化选项加成。 / Enchant Option Boost.
	private boolean isEnchantOptionBoost;
	// 伊顿掉落加成。 / Idun Drop Boost.
	private boolean isIdunDropBoost;
	// 新效果 / New Effect
	private boolean isSprintFpReduce;
	private boolean isReturnCoolReduce;
	private boolean isDeathPenaltyReduce;
	private boolean isOdellaRecoverIncrease;
	private boolean isCancelOnDmg;
	private boolean subEffectAbortedBySubConditions;
	private ItemTemplate itemTemplate;
	private boolean isHiPass;
	private boolean isNoDeathPenalty;
	private boolean isNoDeathPenaltyReduce;
	private boolean isNoResurrectPenalty;
	private int tauntHate;
	private int effectHate;
	private List<EffectTemplate> successEffects = new ArrayList<EffectTemplate>();
	private int carvedSignet = 0;
	private int signetBurstedCount = 0;
	protected int abnormals;
	private ActionObserver[] actionObserver;
	float x, y, z;
	int worldId, instanceId;
	private boolean forcedDuration = false;
	private boolean isForcedEffect = false;
	private int power = 10;
	private int accModBoost = 0;
	private EffectResult effectResult = EffectResult.NORMAL;
	private final AtomicBoolean allowGodstoneActivation = new AtomicBoolean(true);

	public boolean tryActivateGodstone() {
		return allowGodstoneActivation.compareAndSet(true, false);
	}

	/**
	 * 获取关联技能实例。
	 * Gets related skill instance.
	 *
	 * skill
	 */
	public final Skill getSkill() {
		return skill;
	}

	/**
	 * 设置异常状态掩码。
	 * Sets abnormal state mask.
	 *
	 * mask
	 */
	public void setAbnormal(int mask) {
		abnormals |= mask;
	}

	/**
	 * 获取异常状态掩码。
	 * Gets abnormal state mask.
	 *
	 * mask
	 */
	public int getAbnormals() {
		return abnormals;
	}

	/**
	 * 构造运行时效果实例。
	 * Constructs a runtime effect instance.
	 *
	 */
	public Effect(Creature effector, Creature effected, SkillTemplate skillTemplate, int skillLevel, int duration) {
		this.effector = effector;
		this.effected = effected;
		this.skillTemplate = skillTemplate;
		this.skillLevel = skillLevel;
		this.duration = duration;
		this.periodicActions = skillTemplate.getPeriodicActions();
		this.power = initializePower(skillTemplate);
	}

	/**
	 * 构造运行时效果实例。
	 * Constructs a runtime effect instance.
	 *
	 */
	public Effect(Creature effector, Creature effected, SkillTemplate skillTemplate, int skillLevel, int duration,
			ItemTemplate itemTemplate) {
		this(effector, effected, skillTemplate, skillLevel, duration);
		this.itemTemplate = itemTemplate;
	}

	/**
	 * 构造运行时效果实例。
	 * Constructs a runtime effect instance.
	 *
	 */
	public Effect(Skill skill, Creature effected, int duration, ItemTemplate itemTemplate) {
		this(skill.getEffector(), effected, skill.getSkillTemplate(), skill.getSkillLevel(), duration, itemTemplate);
		this.skill = skill;
	}

	/**
	 * 设置世界坐标。
	 * Sets world position.
	 *
	 * 世界 ID / world id
	 * instance id
	 * @param x X
	 * @param y Y
	 * @param z Z
	 */
	public void setWorldPosition(int worldId, int instanceId, float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.worldId = worldId;
		this.instanceId = instanceId;
	}

	/**
	 * 获取施法者 ID。
	 * Gets effector id.
	 *
	 * @return ID
	 */
	public int getEffectorId() {
		return effector.getObjectId();
	}

	/**
	 * 获取技能 ID。
	 * Gets skill id.
	 *
	 * skill id
	 */
	public int getSkillId() {
		return skillTemplate.getSkillId();
	}

	/**
	 * 获取技能名称。
	 * Gets skill name.
	 *
	 * name
	 */
	public String getSkillName() {
		return skillTemplate.getName();
	}

	/**
	 * 获取技能模板。
	 * Gets skill template.
	 *
	 * template
	 */
	public final SkillTemplate getSkillTemplate() {
		return skillTemplate;
	}

	public boolean isDamageProtectorEnabled() {
		return !(currentEffectTemplate instanceof ProcAtkInstantEffect proc) || proc.isCheckProtector();
	}

	/**
	 * 获取技能子类型。
	 * Gets skill sub type.
	 *
	 * sub type
	 */
	public SkillSubType getSkillSubType() {
		return skillTemplate.getSubType();
	}

	/**
	 * 获取技能组例外。
	 * Gets skill-set exception.
	 *
	 * exception
	 */
	public int getSkillSetException() {
		return skillTemplate.getSkillSetException();
	}

	/**
	 * 获取技能组最大共存。
	 * Gets skill-set max occurrence.
	 *
	 * max occur
	 */
	public int getSkillSetMaxOccur() {
		return skillTemplate.getSkillSetMaxOccur();
	}

	/**
	 * 获取堆叠键。
	 * Gets stack key.
	 *
	 * stack
	 */
	public String getStack() {
		return skillTemplate.getStack();
	}

	/**
	 * 获取技能组。
	 * Gets skill group.
	 *
	 * group
	 */
	public String getGroup() {
		return skillTemplate.getGroup();
	}

	/**
	 * 获取技能等级。
	 * Gets skill level.
	 *
	 * level
	 */
	public int getSkillLevel() {
		return skillLevel;
	}

	/**
	 * 获取技能堆叠等级。
	 * Gets skill stack level.
	 *
	 * stack level
	 */
	public int getSkillStackLvl() {
		return skillTemplate.getLvl();
	}

	/**
	 * 获取技能类型。
	 * Gets skill type.
	 *
	 * type
	 */
	public SkillType getSkillType() {
		return skillTemplate.getType();
	}

	/**
	 * 获取持续时间。
	 * Gets duration.
	 *
	 * duration
	 */
	public int getDuration() {
		return duration;
	}

	/**
	 * 设置持续时间。
	 * Sets duration.
	 *
	 * new duration
	 */
	public void setDuration(int newDuration) {
		this.duration = newDuration;
	}

	/**
	 * 获取受影响者。
	 * Gets the effected creature.
	 *
	 * effected
	 */
	public Creature getEffected() {
		return effected;
	}

	/**
	 * 获取施法者。
	 * Gets the effector.
	 *
	 * effector
	 */
	public Creature getEffector() {
		return effector;
	}

	/**
	 * 是否被动。
	 * Whether passive.
	 *
	 * whether
	 */
	public boolean isPassive() {
		return skillTemplate.isPassive();
	}

	/**
	 * 设置主任务。
	 * Sets main task.
	 *
	 * task
	 */
	public void setTask(Future<?> task) {
		this.task = task;
	}

	/**
	 * 获取周期任务。
	 * Gets periodic task.
	 *
	 * @param i 索引（从 1 起） / 1-based index
	 * task
	 */
	public Future<?> getPeriodicTask(int i) {
		return periodicTasks != null ? periodicTasks[i - 1] : null;
	}

	/**
	 * 设置周期任务。
	 * Sets periodic task.
	 *
	 * task
	 * @param i 索引 / index
	 */
	public void setPeriodicTask(Future<?> periodicTask, int i) {
		if (periodicTasks == null) {
			periodicTasks = new Future<?>[4];
		}
		this.periodicTasks[i - 1] = periodicTask;
	}

	/**
	 * 获取保留值 1。
	 * Gets reserved value 1.
	 *
	 * value
	 */
	public int getReserved1() {
		return currentEffectTemplate == null ? reserved1 : reserved1ByEffect.getOrDefault(currentEffectTemplate, reserved1);
	}

	/**
	 * 设置保留值 1。
	 * Sets reserved value 1.
	 *
	 * value
	 */
	public void setReserved1(int reserved1) {
		this.reserved1 = reserved1;
		if (currentEffectTemplate != null) {
			reserved1ByEffect.put(currentEffectTemplate, reserved1);
		}
	}

	/**
	 * 获取保留值 2。
	 * Gets reserved value 2.
	 *
	 * value
	 */
	public int getReserved2() {
		return reserved2;
	}

	/**
	 * 设置保留值 2。
	 * Sets reserved value 2.
	 *
	 * value
	 */
	public void setReserved2(int reserved2) {
		this.reserved2 = reserved2;
	}

	/**
	 * 获取保留值 3。
	 * Gets reserved value 3.
	 *
	 * value
	 */
	public int getReserved3() {
		return reserved3;
	}

	/**
	 * 设置保留值 3。
	 * Sets reserved value 3.
	 *
	 * value
	 */
	public void setReserved3(int reserved3) {
		this.reserved3 = reserved3;
	}

	/**
	 * 获取保留值 4。
	 * Gets reserved value 4.
	 *
	 * value
	 */
	public int getReserved4() {
		return reserved4;
	}

	/**
	 * 设置保留值 4。
	 * Sets reserved value 4.
	 *
	 * value
	 */
	public void setReserved4(int reserved4) {
		this.reserved4 = reserved4;
	}

	/**
	 * 获取保留值 5。
	 * Gets reserved value 5.
	 *
	 * value
	 */
	public int getReserved5() {
		return reserved5;
	}

	/**
	 * 设置保留值 5。
	 * Sets reserved value 5.
	 *
	 * value
	 */
	public void setReserved5(int reserved5) {
		this.reserved5 = reserved5;
	}

	/**
	 * 获取攻击状态。
	 * Gets attack status.
	 *
	 * status
	 */
	public AttackStatus getAttackStatus() {
		return attackStatus;
	}

	/**
	 * 设置攻击状态。
	 * Sets attack status.
	 *
	 * status
	 */
	public void setAttackStatus(AttackStatus attackStatus) {
		this.attackStatus = attackStatus;
	}

	/**
	 * 获取效果模板列表。
	 * Gets effect template list.
	 *
	 * templates
	 */
	public List<EffectTemplate> getEffectTemplates() {
		return skillTemplate.getEffects().getEffects();
	}

	/**
	 * 是否瞬时 MP 治疗。
	 * Whether MP heal instant.
	 *
	 * whether
	 */
	public boolean isMphealInstant() {
		Effects effects = skillTemplate.getEffects();
		return effects != null && effects.isMpHealInstant();
	}

	/**
	 * 是否切换型。
	 * Whether toggle.
	 *
	 * whether
	 */
	public boolean isToggle() {
		return skillTemplate.getActivationAttribute() == ActivationAttribute.TOGGLE;
	}

	/**
	 * 是否咏唱/战歌。
	 * Whether chant.
	 *
	 * whether
	 */
	public boolean isChant() {
		return skillTemplate.getTargetSlot() == SkillTargetSlot.CHANT;
	}

	/**
	 * 是否增益。
	 * Whether buff.
	 *
	 * whether
	 */
	public boolean isBuff() {
		return skillTemplate.getTargetSlot() == SkillTargetSlot.BUFF;
	}

	/**
	 * 是否游侠类增益。
	 * Whether ranger buff.
	 *
	 * whether
	 */
	public boolean isRangerBuff() {
		int skillId = skillTemplate.getSkillId();
		switch (skillId) {
		case 796: // Strong Shots.
		case 809: // Dodging.
		case 813: // Focused Shots.
		case 888: // Hunter's Might.
		case 889: // Bestial Fury.
		case 1053: // Aiming.
		case 1099: // Hunter's Eye.
			return true;
		default:
			return false;
		}
	}

	/**
	 * 获取目标槽位 ID。
	 * Gets target slot id.
	 *
	 * slot
	 */
	public int getTargetSlot() {
		return skillTemplate.getTargetSlot().ordinal();
	}

	/**
	 * 获取目标槽位枚举。
	 * Gets target slot enum.
	 *
	 * slot
	 */
	public SkillTargetSlot getTargetSlotEnum() {
		return skillTemplate.getTargetSlot();
	}

	/**
	 * 获取目标槽位等级。
	 * Gets target slot level.
	 *
	 * level
	 */
	public int getTargetSlotLevel() {
		return skillTemplate.getTargetSlotLevel();
	}

	/**
	 * 获取驱散分类。
	 * Gets dispel category.
	 *
	 * category
	 */
	public DispelCategoryType getDispelCategory() {
		return skillTemplate.getDispelCategory();
	}

	/**
	 * 获取所需驱散等级。
	 * Gets required dispel level.
	 *
	 * level
	 */
	public int getReqDispelLevel() {
		return skillTemplate.getReqDispelLevel();
	}
	/**
	 * 获取攻击状态观察者。
	 * Gets attack status observer.
	 *
	 * @param i 索引 / index
	 * observer
	 */
	public AttackCalcObserver getAttackStatusObserver(int i) {
		return attackStatusObserver != null ? attackStatusObserver[i - 1] : null;
	}
	/**
	 * 设置攻击状态观察者。
	 * Sets attack status observer.
	 *
	 * observer
	 * @param i 索引 / index
	 */
	public void setAttackStatusObserver(AttackCalcObserver attackStatusObserver, int i) {
		if (this.attackStatusObserver == null) {
			this.attackStatusObserver = new AttackCalcObserver[4];
		}
		this.attackStatusObserver[i - 1] = attackStatusObserver;
	}
	/**
	 * 获取护盾观察者。
	 * Gets attack shield observer.
	 *
	 * @param i 索引 / index
	 * observer
	 */
	public AttackCalcObserver getAttackShieldObserver(int i) {
		return attackShieldObserver != null ? attackShieldObserver[i - 1] : null;
	}
	/**
	 * 设置护盾观察者。
	 * Sets attack shield observer.
	 *
	 * observer
	 * @param i 索引 / index
	 */
	public void setAttackShieldObserver(AttackCalcObserver attackShieldObserver, int i) {
		if (this.attackShieldObserver == null) {
			this.attackShieldObserver = new AttackCalcObserver[4];
		}
		this.attackShieldObserver[i - 1] = attackShieldObserver;
	}

	/**
	 * 获取保留整型数组值。
	 * Gets reserved int array value.
	 *
	 * @param i 索引 / index
	 * value
	 */
	public int getReservedInt(int i) {
		return reservedInts != null ? reservedInts[i - 1] : 0;
	}

	/**
	 * 设置保留整型数组值。
	 * Sets reserved int array value.
	 *
	 * @param i 索引 / index
	 * value
	 */
	public void setReservedInt(int i, int value) {
		if (this.reservedInts == null) {
			this.reservedInts = new int[4];
		}
		this.reservedInts[i - 1] = value;
	}
	/**
	 * 是否触发子效果。
	 * Whether to launch sub-effect.
	 *
	 * whether
	 */
	public boolean isLaunchSubEffect() {
		return launchSubEffect;
	}
	/**
	 * 设置是否触发子效果。
	 * Sets launch sub-effect flag.
	 *
	 * whether
	 */
	public void setLaunchSubEffect(boolean launchSubEffect) {
		this.launchSubEffect = launchSubEffect;
	}
	/**
	 * 获取护盾防御值。
	 * Gets shield defense value.
	 *
	 * defense
	 */
	public int getShieldDefense() {
		return shieldDefense;
	}
	/**
	 * 设置护盾防御值。
	 * Sets shield defense value.
	 *
	 * defense
	 */
	public void setShieldDefense(int shieldDefense) {
		this.shieldDefense = shieldDefense;
	}
	/**
	 * 获取反射伤害。
	 * Gets reflected damage.
	 *
	 * damage
	 */
	public int getReflectedDamage() {
		return this.reflectedDamage;
	}

	/**
	 * 设置反射伤害。
	 * Sets reflected damage.
	 *
	 * damage
	 */
	public void setReflectedDamage(int value) {
		this.reflectedDamage = value;
	}

	/**
	 * 获取反射技能 ID。
	 * Gets reflected skill id.
	 *
	 * skill id
	 */
	public int getReflectedSkillId() {
		return this.reflectedSkillId;
	}

	/**
	 * 设置反射技能 ID。
	 * Sets reflected skill id.
	 *
	 * skill id
	 */
	public void setReflectedSkillId(int value) {
		this.reflectedSkillId = value;
	}

	/**
	 * 获取保护技能 ID。
	 * Gets protected skill id.
	 *
	 * skill id
	 */
	public int getProtectedSkillId() {
		return this.protectedSkillId;
	}

	/**
	 * 设置保护技能 ID。
	 * Sets protected skill id.
	 *
	 * skill id
	 */
	public void setProtectedSkillId(int skillId) {
		this.protectedSkillId = skillId;
	}

	/**
	 * 获取被保护伤害。
	 * Gets protected damage.
	 *
	 * damage
	 */
	public int getProtectedDamage() {
		return this.protectedDamage;
	}

	/**
	 * 设置被保护伤害。
	 * Sets protected damage.
	 *
	 * damage
	 */
	public void setProtectedDamage(int protectedDamage) {
		this.protectedDamage = protectedDamage;
	}

	/**
	 * 获取保护者 ID。
	 * Gets protector id.
	 *
	 * @return ID
	 */
	public int getProtectorId() {
		return this.protectorId;
	}

	/**
	 * 设置保护者 ID。
	 * Sets protector id.
	 *
	 * @param protectorId ID
	 */
	public void setProtectorId(int protectorId) {
		this.protectorId = protectorId;
	}
	/**
	 * 获取法术状态。
	 * Gets spell status.
	 *
	 * status
	 */
	public SpellStatus getSpellStatus() {
		return spellStatus;
	}
	/**
	 * 设置法术状态。
	 * Sets spell status.
	 *
	 * status
	 */
	public void setSpellStatus(SpellStatus spellStatus) {
		this.spellStatus = spellStatus;
	}
	/**
	 * 获取冲刺状态。
	 * Gets dash status.
	 *
	 * status
	 */
	public DashStatus getDashStatus() {
		return dashStatus;
	}
	/**
	 * 设置冲刺状态。
	 * Sets dash status.
	 *
	 * status
	 */
	public void setDashStatus(DashStatus dashStatus) {
		this.dashStatus = dashStatus;
	}
	/**
	 * 获取刻印数量。
	 * Gets carved signet count.
	 *
	 * count
	 */
	public int getCarvedSignet() {
		return this.carvedSignet;
	}

	/**
	 * 设置刻印数量。
	 * Sets carved signet count.
	 *
	 * count
	 */
	public void setCarvedSignet(int value) {
		this.carvedSignet = value;
	}
	/**
	 * 获取子效果。
	 * Gets sub-effect.
	 *
	 * sub-effect
	 */
	public Effect getSubEffect() {
		return subEffect;
	}
	/**
	 * 设置子效果。
	 * Sets sub-effect.
	 *
	 * sub-effect
	 */
	public void setSubEffect(Effect subEffect) {
		this.subEffect = subEffect;
	}
	/**
	 * 是否包含指定效果 ID。
	 * Whether contains effect id.
	 *
	 * effect id
	 * whether
	 */
	public boolean containsEffectId(int effectId) {
		for (EffectTemplate template : successEffects) {
			if (template.getEffectid() == effectId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取变身类型。
	 * Gets transform type.
	 *
	 * type
	 */
	public TransformType getTransformType() {
		for (EffectTemplate et : skillTemplate.getEffects().getEffects()) {
			if (et instanceof TransformEffect) {
				return ((TransformEffect) et).getTransformType();
			}
		}
		return TransformType.NONE;
	}

	/**
	 * 设置强制持续标记。
	 * Sets forced duration flag.
	 *
	 * whether
	 */
	public void setForcedDuration(boolean forcedDuration) {
		this.forcedDuration = forcedDuration;
	}

	/**
	 * 设置强制效果标记。
	 * Sets forced effect flag.
	 *
	 * whether
	 */
	public void setIsForcedEffect(boolean isForcedEffect) {
		this.isForcedEffect = isForcedEffect;
	}

	/**
	 * 是否强制效果。
	 * Whether forced effect.
	 *
	 * whether
	 */
	public boolean getIsForcedEffect() {
		return this.isForcedEffect || DataManager.MATERIAL_DATA.isMaterialSkill(this.getSkillId());
	}
	/**
	 * 初始化效果（计算命中与成功效果）。
	 * Initializes effect (hit calc and success effects).
	 *
	 */
	public void initialize() {
		if (skillTemplate.getEffects() == null) {
			return;
		}
		for (EffectTemplate template : getEffectTemplates()) {
			currentEffectTemplate = template;
			try {
				template.calculate(this);
			} finally {
				currentEffectTemplate = null;
			}
			if (template instanceof DelayedSpellAttackInstantEffect) {
				setDelayedDamage(true);
			}
			if (template instanceof PetOrderUseUltraSkillEffect) {
				setPetOrder(true);
			}
			if (template instanceof SummonEffect) {
				setSumonning(true);
			}
			if (template instanceof DamageEffect) {
				setDamageEffect(true);
			}
			if (template instanceof HideEffect) {
				isHideEffect = true;
			}
			if (template instanceof ParalyzeEffect) {
				isParalyzeEffect = true;
			}
			if (template instanceof SanctuaryEffect) {
				isSanctuaryEffect = true;
			}
			if (template instanceof EnchantBoostEffect) {
				isEnchantBoost = true;
			}
			if (template instanceof AuthorizeBoostEffect) {
				isAuthorizeBoost = true;
			}
		}
		for (EffectTemplate template : getEffectTemplates()) {
			currentEffectTemplate = template;
			try {
				template.calculateHate(this);
			} finally {
				currentEffectTemplate = null;
			}
		}
		if (this.isLaunchSubEffect()) {
			for (EffectTemplate template : successEffects) {
				template.calculateSubEffect(this);
			}
		}
		if (successEffects.isEmpty()) {
			skillMoveType = SkillMoveType.RESIST;
			if (skillTemplate.getMessageType() == SkillType.PHYSICAL) {
				if (getAttackStatus() == AttackStatus.CRITICAL) {
					setAttackStatus(AttackStatus.CRITICAL_DODGE);
				} else {
					setAttackStatus(AttackStatus.DODGE);
				}
			} else {
				if (getAttackStatus() == AttackStatus.CRITICAL) {
					setAttackStatus(AttackStatus.CRITICAL_RESIST);
				} else {
					setAttackStatus(AttackStatus.RESIST);
				}
			}
		}
		switch (AttackStatus.getBaseStatus(getAttackStatus())) {
		case DODGE:
			setSpellStatus(SpellStatus.DODGE);
			break;
		case PARRY:
			if (getSpellStatus() == SpellStatus.NONE) {
				setSpellStatus(SpellStatus.PARRY);
			}
			break;
		case BLOCK:
			if (getSpellStatus() == SpellStatus.NONE) {
				setSpellStatus(SpellStatus.BLOCK);
			}
			break;
		case RESIST:
			setSpellStatus(SpellStatus.RESIST);
			break;
		default:
			break;
		}
	}
	/**
	 * 应用已成功的效果。
	 * Applies successful effects.
	 *
	 */
	public void applyEffect() {

		/**
	 * broadcast final hate to all visible objects
	 */
		if (effectHate != 0 && skillTemplate.getHostileType() != HostileType.NONE) {
			if (skillTemplate.getHostileType() == HostileType.DIRECT && getEffected() instanceof Npc && !isDelayedDamage() && !isPetOrder()
					&& !isSummoning()) {
				getEffected().getAggroList().addHate(effector, 1);
			}
			effector.getController().broadcastHate(effectHate);
		}

		if (skillTemplate.getEffects() == null || successEffects.isEmpty())
			return;

		for (EffectTemplate template : successEffects) {
			if (getEffected() != null) {
				if (getEffected().getLifeStats().isAlreadyDead() && !skillTemplate.hasResurrectEffect()) {
					continue;
				}
			}
			currentEffectTemplate = template;
			try {
				template.applyEffect(this);
				template.startSubEffect(this);
			} finally {
				currentEffectTemplate = null;
			}
		}
	}
	/**
	 * 开始效果生命周期。
	 * Starts effect lifecycle.
	 *
	 * whether restored
	 */
	public void startEffect(boolean restored) {
		if (successEffects.isEmpty()) {
			return;
		}
		shedulePeriodicActions();
		for (EffectTemplate template : successEffects) {
			template.startEffect(this);
			checkUseEquipmentConditions();
			checkCancelOnDmg();
		}
		if (isToggle() && effector instanceof Player) {
			activateToggleSkill();
		}
		if (!restored && !forcedDuration) {
			duration = getEffectsDuration();
		}
		if (isToggle()) {
			duration = skillTemplate.getToggleTimer();
		}
		if (isEnchantBoost() && effector instanceof Player) {
			((Player) effector).setEnchantBoost(true);
		}
		if (isAuthorizeBoost() && effector instanceof Player) {
			((Player) effector).setAuthorizeBoost(true);
		}
		if (duration == 0) {
			return;
		}
		if (isOpenAerialSkill()) {
			duration = skillTemplate.getDuration();
		}
		endTime = System.currentTimeMillis() + duration;
		task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				endEffect();
			}
		}, duration);
	}

	/**
	 * Will activate toggle skill and start checking task
	 */
	private void activateToggleSkill() {
		PacketSendUtility.sendPacket((Player) effector, new SM_SKILL_ACTIVATION(getSkillId(), true));
	}

	/**
	 * Will deactivate toggle skill and stop checking task
	 */
	private void deactivateToggleSkill() {
		PacketSendUtility.sendPacket((Player) effector, new SM_SKILL_ACTIVATION(getSkillId(), false));
	}
	/**
	 * 结束效果并清理。
	 * Ends effect and cleans up.
	 *
	 */
	public synchronized void endEffect() {
		if (isStopped) {
			return;
		}
		int previousAbnormals = effected.getEffectController().getAbnormals();
		for (EffectTemplate template : successEffects) {
			template.endEffect(this);
		}
		int leftAbnormals = previousAbnormals & ~effected.getEffectController().getAbnormals();
		if (leftAbnormals != 0 && effected instanceof Npc npc) {
			npc.getAi2().onLeaveAbnormalState(effector, leftAbnormals);
		}
		// 若效果为姿态，则从玩家移除姿态 / If effect is a stance, remove stance from player
		if (effector instanceof Player) {
			Player player = (Player) effector;
			if (player.getController().getStanceSkillId() == getSkillId()) {
				PacketSendUtility.sendPacket(player, new SM_PLAYER_STANCE(player, 0));
				player.getController().startStance(0);
			}
		}
		if (isToggle() && effector instanceof Player) {
			deactivateToggleSkill();
		}
		if (isEnchantBoost() && effector instanceof Player) {
			((Player) effector).setEnchantBoost(false);
		}
		if (isAuthorizeBoost() && effector instanceof Player) {
			((Player) effector).setAuthorizeBoost(false);
		}
		stopTasks();
		effected.getEffectController().clearEffect(this);
		this.isStopped = true;
		this.addedToController = false;
	}

	public boolean isStopped() {
		return isStopped;
	}
	/**
	 * 停止相关任务。
	 * Stops related tasks.
	 *
	 */
	public void stopTasks() {
		if (task != null) {
			task.cancel(false);
			task = null;
		}

		if (periodicTasks != null) {
			for (Future<?> periodicTask : this.periodicTasks) {
				if (periodicTask != null) {
					periodicTask.cancel(false);
					periodicTask = null;
				}
			}
		}
		stopPeriodicActions();
	}
	/**
	 * 获取剩余时间。
	 * Gets remaining time.
	 *
	 * remaining millis
	 */
	public int getRemainingTime() {
		int remainingTime = (int) (endTime - System.currentTimeMillis());
		if (this.getDuration() >= 86400000) {
			remainingTime = -1;
		}
		return remainingTime > 0 ? remainingTime : -1;
	}
	/**
	 * 获取结束时间戳。
	 * Gets end timestamp.
	 *
	 * timestamp
	 */
	public long getEndTime() {
		return endTime;
	}
	/**
	 * 获取 PvP 伤害系数。
	 * Gets PvP damage factor.
	 *
	 * factor
	 */
	public int getPvpDamage() {
		return skillTemplate.getPvpDamage();
	}

	/**
	 * 获取关联物品模板。
	 * Gets related item template.
	 *
	 * template
	 */
	public ItemTemplate getItemTemplate() {
		return itemTemplate;
	}
	/**
	 * 加入受影响者控制器。
	 * Adds to effected controller.
	 *
	 */
	public void addToEffectedController() {
		if ((!addedToController) && (effected.getLifeStats() != null) && (!effected.getLifeStats().isAlreadyDead())) {
			addedToController = effected.getEffectController().addEffect(this);
		}
	}
	/**
	 * 获取效果仇恨。
	 * Gets effect hate.
	 *
	 * hate
	 */
	public int getEffectHate() {
		return effectHate;
	}
	/**
	 * 设置效果仇恨。
	 * Sets effect hate.
	 *
	 * hate
	 */
	public void setEffectHate(int effectHate) {
		this.effectHate = effectHate;
	}
	/**
	 * 获取嘲讽仇恨。
	 * Gets taunt hate.
	 *
	 * hate
	 */
	public int getTauntHate() {
		return tauntHate;
	}
	/**
	 * 设置嘲讽仇恨。
	 * Sets taunt hate.
	 *
	 * hate
	 */
	public void setTauntHate(int tauntHate) {
		this.tauntHate = tauntHate;
	}
	/**
	 * 获取动作观察者。
	 * Gets action observer.
	 *
	 * @param i 索引 / index
	 * observer
	 */
	public ActionObserver getActionObserver(int i) {
		return actionObserver != null ? actionObserver[i - 1] : null;
	}
	/**
	 * 设置动作观察者。
	 * Sets action observer.
	 *
	 * observer
	 * @param i 索引 / index
	 */
	public void setActionObserver(ActionObserver observer, int i) {
		if (actionObserver == null) {
			actionObserver = new ActionObserver[4];
		}
		actionObserver[i - 1] = observer;
	}

	/**
	 * 添加成功效果模板。
	 * Adds successful effect template.
	 *
	 * effect template
	 */
	public void addSucessEffect(EffectTemplate effect) {
		if (!successEffects.contains(effect)) {
			successEffects.add(effect);
		}
	}

	/**
	 * 是否在成功效果列表中。
	 * Whether in success effects.
	 *
	 * position
	 * whether
	 */
	public boolean isInSuccessEffects(int position) {
		for (EffectTemplate effect : successEffects) {
			if (effect.getPosition() == position) {
				return true;
			}
		}
		return false;
	}
	/**
	 * 获取成功效果集合。
	 * Gets success effect collection.
	 *
	 * collection
	 */
	public Collection<EffectTemplate> getSuccessEffect() {
		return successEffects;
	}

	/**
	 * 将全部效果标为成功。
	 * Marks all effects as success.
	 *
	 */
	public void addAllEffectToSucess() {
		successEffects.clear();
		for (EffectTemplate template : getEffectTemplates()) {
			successEffects.add(template);
		}
	}

	/**
	 * 清空成功效果列表。
	 * Clears success effect list.
	 *
	 */
	public void clearSucessEffects() {
		successEffects.clear();
	}

	private void shedulePeriodicActions() {
		if (periodicActions == null || periodicActions.getPeriodicActions() == null
				|| periodicActions.getPeriodicActions().isEmpty()) {
			return;
		}
		int checktime = periodicActions.getChecktime();
		periodicActionsTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				for (PeriodicAction action : periodicActions.getPeriodicActions()) {
					action.act(Effect.this);
				}
			}
		}, 0, checktime);
	}

	private void stopPeriodicActions() {
		if (periodicActionsTask != null) {
			periodicActionsTask.cancel(false);
			periodicActionsTask = null;
		}
	}

	/**
	 * 获取效果总时长。
	 * Gets total effects duration.
	 *
	 * duration
	 */
	public int getEffectsDuration() {
		int duration = 0;

		// 遍历技能效果直至能计算出持续时间。 / iterate skill's effects until we can calculate a duration time, which is
		// 对全部有效 / valid for all of them
		Iterator<EffectTemplate> itr = successEffects.iterator();
		while (itr.hasNext() && duration == 0) {
			EffectTemplate et = itr.next();
			int effectDuration = et.getDuration2() + et.getDuration1() * getSkillLevel();
			if (et.getRandomTime() > 0) {
				effectDuration -= Rnd.get(et.getRandomTime());
			}
			duration = duration > effectDuration ? duration : effectDuration;
		}

		// adjust with BOOST_DURATION
		switch (skillTemplate.getSubType()) {
		case BUFF:
			duration = effector.getGameStats().getStat(StatEnum.BOOST_DURATION_BUFF, duration).getCurrent();
			break;

		default:
			break;
		}

		// 按 PVP 持续时间调整 / adjust with pvp duration
		if (effected instanceof Player && skillTemplate.getPvpDuration() != 0) {
			duration = duration * skillTemplate.getPvpDuration() / 100;
		}
		if (duration > 86400000) {
			duration = 86400000;
		}
		return duration;
	}

	/**
	 * 是否神性化身。
	 * Whether deity avatar.
	 *
	 * whether
	 */
	public boolean isDeityAvatar() {
		return skillTemplate.isDeityAvatar();
	}
	/**
	 * 获取 X 坐标。
	 * Gets X coordinate.
	 *
	 * @return X
	 */
	public float getX() {
		return x;
	}
	/**
	 * 获取 Y 坐标。
	 * Gets Y coordinate.
	 *
	 * @return Y
	 */
	public float getY() {
		return y;
	}
	/**
	 * 获取 Z 坐标。
	 * Gets Z coordinate.
	 *
	 * @return Z
	 */
	public float getZ() {
		return z;
	}

	/**
	 * 获取地图 ID。
	 * Gets world id.
	 *
	 * 世界 ID / world id
	 */
	public int getWorldId() {
		return worldId;
	}

	/**
	 * 获取实例 ID。
	 * Gets instance id.
	 *
	 * instance id
	 */
	public int getInstanceId() {
		return instanceId;
	}
	/**
	 * 获取技能位移类型。
	 * Gets skill move type.
	 *
	 * type
	 */
	public SkillMoveType getSkillMoveType() {
		return skillMoveType;
	}
	/**
	 * 设置技能位移类型。
	 * Sets skill move type.
	 *
	 * type
	 */
	public void setSkillMoveType(SkillMoveType skillMoveType) {
		this.skillMoveType = skillMoveType;
	}
	/**
	 * 获取目标 X。
	 * Gets target X.
	 *
	 * @return X
	 */
	public float getTargetX() {
		return targetX;
	}
	/**
	 * 获取目标 Y。
	 * Gets target Y.
	 *
	 * @return Y
	 */
	public float getTargetY() {
		return targetY;
	}
	/**
	 * 获取目标 Z。
	 * Gets target Z.
	 *
	 * @return Z
	 */
	public float getTargetZ() {
		return targetZ;
	}

	/**
	 * 设置目标位置（历史拼写）。
	 * Sets target location (legacy spelling).
	 *
	 * @param x X
	 * @param y Y
	 * @param z Z
	 */
	public void setTragetLoc(float x, float y, float z) {
		this.targetX = x;
		this.targetY = y;
		this.targetZ = z;
	}

	/**
	 * 设置子效果中止标记。
	 * Sets sub-effect aborted flag.
	 *
	 * whether
	 */
	public void setSubEffectAborted(boolean value) {
		this.subEffectAbortedBySubConditions = value;
	}

	/**
	 * 子效果是否因条件中止。
	 * Whether sub-effect aborted by conditions.
	 *
	 * whether
	 */
	public boolean isSubEffectAbortedBySubConditions() {
		return this.subEffectAbortedBySubConditions;
	}

	/**
	 * 设置经验加成标记。
	 * Sets XP boost flag.
	 *
	 * whether
	 */
	public void setXpBoost(boolean value) {
		this.isXpBoost = value;
	}

	/**
	 * 是否经验加成。
	 * Whether XP boost.
	 *
	 * whether
	 */
	public boolean isXpBoost() {
		return this.isXpBoost;
	}

	/**
	 * 设置 AP 加成标记。
	 * Sets AP boost flag.
	 *
	 * whether
	 */
	public void setApBoost(boolean value) {
		this.isApBoost = value;
	}

	/**
	 * 是否 AP 加成。
	 * Whether AP boost.
	 *
	 * whether
	 */
	public boolean isApBoost() {
		return this.isApBoost;
	}

	/**
	 * 设置掉落加成标记。
	 * Sets drop boost flag.
	 *
	 * whether
	 */
	public void setDrBoost(boolean value) {
		this.isDrBoost = value;
	}

	/**
	 * 是否掉落加成。
	 * Whether drop boost.
	 *
	 * whether
	 */
	public boolean isDrBoost() {
		return this.isDrBoost;
	}

	/**
	 * 设置 BDR 加成标记。
	 * Sets BDR boost flag.
	 *
	 * whether
	 */
	public void setBdrBoost(boolean value) {
		this.isBdrBoost = value;
	}

	/**
	 * 是否 BDR 加成。
	 * Whether BDR boost.
	 *
	 * whether
	 */
	public boolean isBdrBoost() {
		return this.isBdrBoost;
	}

	/**
	 * 设置鉴定加成标记。
	 * Sets authorize boost flag.
	 *
	 * whether
	 */
	public void setAuthorizeBoost(boolean value) {
		this.isAuthorizeBoost = value;
	}

	/**
	 * 是否鉴定加成。
	 * Whether authorize boost.
	 *
	 * whether
	 */
	public boolean isAuthorizeBoost() {
		return this.isAuthorizeBoost;
	}

	/**
	 * 设置附魔加成标记。
	 * Sets enchant boost flag.
	 *
	 * whether
	 */
	public void setEnchantBoost(boolean value) {
		this.isEnchantBoost = value;
	}

	/**
	 * 是否附魔加成。
	 * Whether enchant boost.
	 *
	 * whether
	 */
	public boolean isEnchantBoost() {
		return this.isEnchantBoost;
	}

	/**
	 * 设置附魔词条加成标记。
	 * Sets enchant option boost flag.
	 *
	 * whether
	 */
	public void setEnchantOptionBoost(boolean value) {
		this.isEnchantOptionBoost = value;
	}

	/**
	 * 是否附魔词条加成。
	 * Whether enchant option boost.
	 *
	 * whether
	 */
	public boolean isEnchantOptionBoost() {
		return this.isEnchantOptionBoost;
	}

	/**
	 * 设置伊顿掉落加成标记。
	 * Sets Idun drop boost flag.
	 *
	 * whether
	 */
	public void setIdunDropBoost(boolean value) {
		this.isIdunDropBoost = value;
	}

	/**
	 * 是否伊顿掉落加成。
	 * Whether Idun drop boost.
	 *
	 * whether
	 */
	public boolean isIdunDropBoost() {
		return this.isIdunDropBoost;
	}

	/**
	 * 设置死亡惩罚减免标记。
	 * Sets no-death-penalty-reduce flag.
	 *
	 * whether
	 */
	public void setNoDeathPenaltyReduce(boolean value) {
		this.isNoDeathPenaltyReduce = value;
	}

	/**
	 * 是否死亡惩罚减免。
	 * Whether no death penalty reduce.
	 *
	 * whether
	 */
	public boolean isNoDeathPenaltyReduce() {
		return this.isNoDeathPenaltyReduce;
	}

	/**
	 * 设置无死亡惩罚标记。
	 * Sets no-death-penalty flag.
	 *
	 * whether
	 */
	public void setNoDeathPenalty(boolean value) {
		this.isNoDeathPenalty = value;
	}

	/**
	 * 是否无死亡惩罚。
	 * Whether no death penalty.
	 *
	 * whether
	 */
	public boolean isNoDeathPenalty() {
		return this.isNoDeathPenalty;
	}

	/**
	 * 设置无复活惩罚标记。
	 * Sets no-resurrect-penalty flag.
	 *
	 * whether
	 */
	public void setNoResurrectPenalty(boolean value) {
		this.isNoResurrectPenalty = value;
	}

	/**
	 * 是否无复活惩罚。
	 * Whether no resurrect penalty.
	 *
	 * whether
	 */
	public boolean isNoResurrectPenalty() {
		return this.isNoResurrectPenalty;
	}

	/**
	 * 设置 HiPass 标记。
	 * Sets HiPass flag.
	 *
	 * whether
	 */
	public void setHiPass(boolean value) {
		this.isHiPass = value;
	}

	/**
	 * 是否 HiPass。
	 * Whether HiPass.
	 *
	 * whether
	 */
	public boolean isHiPass() {
		return this.isHiPass;
	}

	/**
	 * 设置回城冷却减免标记。
	 * Sets return cooldown reduce flag.
	 *
	 * whether
	 */
	public void setReturnCoolReduce(boolean value) {
		this.isReturnCoolReduce = value;
	}

	/**
	 * 是否回城冷却减免。
	 * Whether return cooldown reduce.
	 *
	 * whether
	 */
	public boolean isReturnCoolReduce() {
		return this.isReturnCoolReduce;
	}

	/**
	 * 设置奥德拉恢复提升标记。
	 * Sets Odella recover increase flag.
	 *
	 * whether
	 */
	public void setOdellaRecoverIncrease(boolean value) {
		this.isOdellaRecoverIncrease = value;
	}

	/**
	 * 是否奥德拉恢复提升。
	 * Whether Odella recover increase.
	 *
	 * whether
	 */
	public boolean isOdellaRecoverIncrease() {
		return this.isOdellaRecoverIncrease;
	}

	/**
	 * 设置冲刺飞行值消耗减免标记。
	 * Sets sprint FP reduce flag.
	 *
	 * whether
	 */
	public void setSprintFpReduce(boolean value) {
		this.isSprintFpReduce = value;
	}

	/**
	 * 是否冲刺飞行值消耗减免。
	 * Whether sprint FP reduce.
	 *
	 * whether
	 */
	public boolean isSprintFpReduce() {
		return this.isSprintFpReduce;
	}

	/**
	 * 设置死亡惩罚降低标记。
	 * Sets death penalty reduce flag.
	 *
	 * whether
	 */
	public void setDeathPenaltyReduce(boolean value) {
		this.isDeathPenaltyReduce = value;
	}

	/**
	 * 是否死亡惩罚降低。
	 * Whether death penalty reduce.
	 *
	 * whether
	 */
	public boolean isDeathPenaltyReduce() {
		return this.isDeathPenaltyReduce;
	}

	/**
	 * 检查全部在用装备条件。
	 * Check all in-use equipment conditions.
	 *
	 * @return 全部满足则为 true / true if all conditions satisfied
	 */
	private boolean useEquipmentConditionsCheck() {
		Conditions useEquipConditions = skillTemplate.getUseEquipmentconditions();
		return useEquipConditions != null ? useEquipConditions.validate(this) : true;
	}

	/**
	 * 必要时添加卸装观察者以检查装备使用条件。
	 * Check use-equipment conditions by adding Unequip observer if needed
	 */
	private void checkUseEquipmentConditions() {
		// 若技能有使用装备条件 / If skill has use equipment conditions
		// 观察卸装事件，发生时移除效果 / Observe for unequip event and remove effect if event occurs
		if ((getSkillTemplate().getUseEquipmentconditions() != null)
				&& (getSkillTemplate().getUseEquipmentconditions().getConditions().size() > 0)) {
			ActionObserver observer = new ActionObserver(ObserverType.UNEQUIP) {

				@Override
				public void unequip(Item item, Player owner) {
					if (!useEquipmentConditionsCheck()) {
						endEffect();
						if (this != null) {
							effected.getObserveController().removeObserver(this);
						}
					}
				}
			};
			effected.getObserveController().addObserver(observer);
		}
	}

	/**
	 * 添加 Attacked/Dot_Attackedobservers 若此 effectneeds 到为 removed 在 damagereceived 按 effected。 / Add Attacked/Dot_Attacked observers if this effect needs to be removed on damage received by effected
	 */
	private void checkCancelOnDmg() {
		if (isCancelOnDmg()) {
			effected.getObserveController().attach(new ActionObserver(ObserverType.ATTACKED) {

				@Override
				public void attacked(Creature creature) {
					effected.getEffectController().removeEffect(getSkillId());
				}
			});

			effected.getObserveController().attach(new ActionObserver(ObserverType.DOT_ATTACKED) {

				@Override
				public void dotattacked(Creature creature, Effect dotEffect) {
					effected.getEffectController().removeEffect(getSkillId());
				}
			});
		}
	}

	/**
	 * 设置受伤取消标记。
	 * Sets cancel-on-damage flag.
	 *
	 * whether
	 */
	public void setCancelOnDmg(boolean value) {
		this.isCancelOnDmg = value;
	}

	/**
	 * 是否受伤取消。
	 * Whether cancel on damage.
	 *
	 * whether
	 */
	public boolean isCancelOnDmg() {
		return this.isCancelOnDmg;
	}

	/**
	 * 结束全部效果模板。
	 * Ends all effect templates.
	 *
	 */
	public void endEffects() {
		for (EffectTemplate template : successEffects) {
			template.endEffect(this);
		}
	}

	/**
	 * 是否恐惧效果。
	 * Whether fear effect.
	 *
	 * whether
	 */
	public boolean isFearEffect() {
		for (EffectTemplate template : successEffects) {
			if (template instanceof FearEffect) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否延迟伤害。
	 * Whether delayed damage.
	 *
	 * whether
	 */
	public boolean isDelayedDamage() {
		return this.isDelayedDamage;
	}

	/**
	 * 设置延迟伤害标记。
	 * Sets delayed damage flag.
	 *
	 * whether
	 */
	public void setDelayedDamage(boolean value) {
		this.isDelayedDamage = value;
	}

	/**
	 * 是否宠物指令。
	 * Whether pet order.
	 *
	 * whether
	 */
	public boolean isPetOrder() {
		return this.isPetOrder;
	}

	/**
	 * 设置宠物指令标记。
	 * Sets pet order flag.
	 *
	 * whether
	 */
	public void setPetOrder(boolean value) {
		this.isPetOrder = value;
	}

	/**
	 * 是否召唤中。
	 * Whether summoning.
	 *
	 * whether
	 */
	public boolean isSummoning() {
		return this.isSummoning;
	}

	/**
	 * 设置召唤中标记（历史拼写）。
	 * Sets summoning flag (legacy spelling).
	 *
	 * whether
	 */
	public void setSumonning(boolean value) {
		this.isSummoning = value;
	}

	private int initializePower(SkillTemplate skill) {
		return skill.getReqDispelCount();
	}
	/**
	 * 获取强度。
	 * Gets power.
	 *
	 * power
	 */
	public int getPower() {
		return power;
	}
	/**
	 * 设置强度。
	 * Sets power.
	 *
	 * power
	 */
	public void setPower(int power) {
		this.power = power;
	}

	/**
	 * 削减强度。
	 * Removes power amount.
	 *
	 * amount
	 * remaining power
	 */
	public int removePower(int power) {
		this.power -= power;
		return this.power;
	}

	/**
	 * 设置命中修正加成。
	 * Sets accuracy mod boost.
	 *
	 * boost
	 */
	public void setAccModBoost(int accModBoost) {
		this.accModBoost = accModBoost;
	}

	/**
	 * 获取命中修正加成。
	 * Gets accuracy mod boost.
	 *
	 * boost
	 */
	public int getAccModBoost() {
		return this.accModBoost;
	}

	/**
	 * 是否隐身效果。
	 * Whether hide effect.
	 *
	 * whether
	 */
	public boolean isHideEffect() {
		return isHideEffect;
	}

	/**
	 * 是否麻痹效果。
	 * Whether paralyze effect.
	 *
	 * whether
	 */
	public boolean isParalyzeEffect() {
		return isParalyzeEffect;
	}

	/**
	 * 是否庇护效果。
	 * Whether sanctuary effect.
	 *
	 * whether
	 */
	public boolean isSanctuaryEffect() {
		return isSanctuaryEffect;
	}
	/**
	 * 是否伤害效果。
	 * Whether damage effect.
	 *
	 * whether
	 */
	public boolean isDamageEffect() {
		return isDamageEffect;
	}
	/**
	 * 设置伤害效果标记。
	 * Sets damage effect flag.
	 *
	 * whether
	 */
	public void setDamageEffect(boolean isDamageEffect) {
		this.isDamageEffect = isDamageEffect;
	}
	/**
	 * 获取印记爆发数量。
	 * Gets signet burst count.
	 *
	 * count
	 */
	public int getSignetBurstedCount() {
		return signetBurstedCount;
	}
	/**
	 * 设置印记爆发数量。
	 * Sets signet burst count.
	 *
	 * count
	 */
	public void setSignetBurstedCount(int signetBurstedCount) {
		this.signetBurstedCount = signetBurstedCount;
	}

	/**
	 * 获取效果结算结果。
	 * Gets effect result.
	 *
	 * result
	 */
	public final EffectResult getEffectResult() {
		return effectResult;
	}

	/**
	 * 设置效果结算结果。
	 * Sets effect result.
	 *
	 * result
	 */
	public final void setEffectResult(EffectResult effectResult) {
		this.effectResult = effectResult;
	}

	/**
	 * 获取 MP 护盾值。
	 * Gets MP shield value.
	 *
	 * value
	 */
	public int getMpShield() {
		return this.mpShield;
	}

	/**
	 * 设置 MP 护盾值。
	 * Sets MP shield value.
	 *
	 * value
	 */
	public void setMpShield(int mpShield) {
		this.mpShield = mpShield;
	}

	private boolean isPhysicalState = false;
	private boolean isMagicalState = false;

	/**
	 * 是否物理控制状态。
	 * Whether physical state.
	 *
	 * whether
	 */
	public boolean isPhysicalState() {
		return isPhysicalState;
	}

	/**
	 * 设置物理控制状态标记。
	 * Sets physical state flag.
	 *
	 * whether
	 */
	public void setIsPhysicalState(boolean isPhysicalState) {
		this.isPhysicalState = isPhysicalState;
	}

	/**
	 * 是否魔法控制状态。
	 * Whether magical state.
	 *
	 * whether
	 */
	public boolean isMagicalState() {
		return isMagicalState;
	}

	/**
	 * 设置魔法控制状态标记。
	 * Sets magical state flag.
	 *
	 * whether
	 */
	public void setIsMagicalState(boolean isMagicalState) {
		this.isMagicalState = isMagicalState;
	}

	/**
	 * 设置目标位置。
	 * Sets target location.
	 *
	 * @param x X
	 * @param y Y
	 * @param z Z
	 */
	public void setTargetLoc(float x, float y, float z) {
		this.targetX = x;
		this.targetY = y;
		this.targetZ = z;
	}

	private boolean isOpenAerialSkill() {
		switch (getSkillId()) {
		case 8224:
		case 8678:
		case 9173:
		case 19552:
		case 20371:
		case 20680:
		case 20872:
		case 21133:
		case 21476:
		case 21529:
		case 21911:
			return true;
		}
		return false;
	}
}

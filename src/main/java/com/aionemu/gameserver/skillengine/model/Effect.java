package com.aionemu.gameserver.skillengine.model;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

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
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STANCE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_ACTIVATION;
import com.aionemu.gameserver.skillengine.effect.AuthorizeBoostEffect;
import com.aionemu.gameserver.skillengine.effect.DamageEffect;
import com.aionemu.gameserver.skillengine.effect.DelayedSpellAttackInstantEffect;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.Effects;
import com.aionemu.gameserver.skillengine.effect.EnchantBoostEffect;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 运行时效果实例：绑定施法者/目标，驱动效果初始化、应用、周期与结束。
 * Runtime effect instance: binds effector/effected and drives init, apply, tick and end.
 *
 * @author ATracer
 */
@Getter
@Setter
public class Effect implements StatOwner {

	private Skill skill;
	private final SkillTemplate skillTemplate;
	/**
	 * 获取技能等级。
	 * Gets skill level.
	 *
	 */
	private final int skillLevel;
	/**
	 * 获取持续时间。
	 * Gets duration.
	 *
	 */
	private int duration;
	/**
	 * 获取结束时间戳。
	 * Gets end timestamp.
	 *
	 */
	private long endTime;
	private final PeriodicActions periodicActions;
	/**
	 * 获取技能位移类型。
	 * Gets skill move type.
	 *
	 */
	private SkillMoveType skillMoveType = SkillMoveType.DEFAULT;
	/**
	 * 获取受影响者。
	 * Gets the effected creature.
	 *
	 */
	private final Creature effected;
	/**
	 * 获取施法者。
	 * Gets the effector.
	 *
	 */
	private final Creature effector;
	/**
	 * 设置主任务。
	 * Sets main task.
	 *
	 */
	private Future<?> task = null;
	private Future<?>[] periodicTasks = null;
	private Future<?> periodicActionsTask = null;
	/**
	 * 是否隐身效果。
	 * Whether hide effect.
	 *
	 */
	private boolean isHideEffect = false;
	/**
	 * 是否麻痹效果。
	 * Whether paralyze effect.
	 *
	 */
	private boolean isParalyzeEffect = false;
	/**
	 * 是否庇护效果。
	 * Whether sanctuary effect.
	 *
	 */
	private boolean isSanctuaryEffect = false;
	/**
	 * 获取目标 X。
	 * Gets target X.
	 *
	 * @return X
	 */
	private float targetX = 0;
	/**
	 * 获取目标 Y。
	 * Gets target Y.
	 *
	 * @return Y
	 */
	private float targetY = 0;
	/**
	 * 获取目标 Z。
	 * Gets target Z.
	 *
	 * @return Z
	 */
	private float targetZ = 0;
	/**
	 * 获取 MP 护盾值。
	 * Gets MP shield value.
	 *
	 */
	private int mpShield = 0;
	private int reserved1;
	private final IdentityHashMap<EffectTemplate, Integer> reserved1ByEffect = new IdentityHashMap<>();
	private EffectTemplate currentEffectTemplate;
	/**
	 * 获取保留值 2。
	 * Gets reserved value 2.
	 *
	 */
	private int reserved2;
	/**
	 * 获取保留值 3。
	 * Gets reserved value 3.
	 *
	 */
	private int reserved3;
	/**
	 * 获取保留值 4。
	 * Gets reserved value 4.
	 *
	 */
	private int reserved4;
	/**
	 * 获取保留值 5。
	 * Gets reserved value 5.
	 *
	 */
	private int reserved5;
	private int[] reservedInts;
	/**
	 * 获取法术状态。
	 * Gets spell status.
	 *
	 */
	private SpellStatus spellStatus = SpellStatus.NONE;
	/**
	 * 获取冲刺状态。
	 * Gets dash status.
	 *
	 */
	private DashStatus dashStatus = DashStatus.NONE;
	/**
	 * 获取攻击状态。
	 * Gets attack status.
	 *
	 */
	private AttackStatus attackStatus = AttackStatus.NORMALHIT;
	private AttackStatus[] periodicAttackStatuses;
	/**
	 * 获取护盾防御值。
	 * Gets shield defense value.
	 *
	 */
	private int shieldDefense;
	/**
	 * 获取反射伤害。
	 * Gets reflected damage.
	 *
	 */
	private int reflectedDamage = 0;
	/**
	 * 获取反射技能 ID。
	 * Gets reflected skill id.
	 *
	 */
	private int reflectedSkillId = 0;
	/**
	 * 获取保护技能 ID。
	 * Gets protected skill id.
	 *
	 */
	private int protectedSkillId = 0;
	/**
	 * 获取被保护伤害。
	 * Gets protected damage.
	 *
	 */
	private int protectedDamage = 0;
	/**
	 * 获取保护者 ID。
	 * Gets protector id.
	 *
	 * @return ID
	 */
	private int protectorId = 0;
	private boolean addedToController;
	private AttackCalcObserver[] attackStatusObserver;
	private AttackCalcObserver[] attackShieldObserver;
	/**
	 * 是否触发子效果。
	 * Whether to launch sub-effect.
	 *
	 */
	private boolean launchSubEffect = true;
	/**
	 * 获取子效果。
	 * Gets sub-effect.
	 *
	 */
	private Effect subEffect;
	private volatile boolean isStopped;
	private int startedTemplateCount;
	@Getter(AccessLevel.NONE)
	private final EffectTerminationObservers terminationObservers = new EffectTerminationObservers(this);
	/**
	 * 是否延迟伤害。
	 * Whether delayed damage.
	 *
	 */
	private boolean isDelayedDamage;
	/**
	 * 是否伤害效果。
	 * Whether damage effect.
	 *
	 */
	private boolean isDamageEffect;
	/**
	 * 是否宠物指令。
	 * Whether pet order.
	 *
	 */
	private boolean isPetOrder;
	/**
	 * 是否召唤中。
	 * Whether summoning.
	 *
	 */
	private boolean isSummoning;
	// 经验加成。 / Xp Boost.
	/**
	 * 设置经验加成标记。
	 * Sets XP boost flag.
	 *
	 */
	private boolean isXpBoost;
	// AP 加成。 / Ap Boost.
	/**
	 * 设置 AP 加成标记。
	 * Sets AP boost flag.
	 *
	 */
	private boolean isApBoost;
	// Dr 加成。 / Dr Boost.
	/**
	 * 设置掉落加成标记。
	 * Sets drop boost flag.
	 *
	 */
	private boolean isDrBoost;
	// Bdr 加成。 / Bdr Boost.
	/**
	 * 设置 BDR 加成标记。
	 * Sets BDR boost flag.
	 *
	 */
	private boolean isBdrBoost;
	// 授权加成。 / Authorize Boost.
	/**
	 * 设置鉴定加成标记。
	 * Sets authorize boost flag.
	 *
	 */
	private boolean isAuthorizeBoost;
	// 强化加成。 / Enchant Boost.
	/**
	 * 设置附魔加成标记。
	 * Sets enchant boost flag.
	 *
	 */
	private boolean isEnchantBoost;
	// 强化选项加成。 / Enchant Option Boost.
	/**
	 * 设置附魔词条加成标记。
	 * Sets enchant option boost flag.
	 *
	 */
	private boolean isEnchantOptionBoost;
	// 伊顿掉落加成。 / Idun Drop Boost.
	/**
	 * 设置伊顿掉落加成标记。
	 * Sets Idun drop boost flag.
	 *
	 */
	private boolean isIdunDropBoost;
	// 新效果 / New Effect
	/**
	 * 设置冲刺飞行值消耗减免标记。
	 * Sets sprint FP reduce flag.
	 *
	 */
	private boolean isSprintFpReduce;
	/**
	 * 设置回城冷却减免标记。
	 * Sets return cooldown reduce flag.
	 *
	 */
	private boolean isReturnCoolReduce;
	/**
	 * 设置死亡惩罚降低标记。
	 * Sets death penalty reduce flag.
	 *
	 */
	private boolean isDeathPenaltyReduce;
	/**
	 * 设置奥德拉恢复提升标记。
	 * Sets Odella recover increase flag.
	 *
	 */
	private boolean isOdellaRecoverIncrease;
	/**
	 * 设置受伤取消标记。
	 * Sets cancel-on-damage flag.
	 *
	 */
	private boolean isCancelOnDmg;
	/**
	 * 子效果是否因条件中止。
	 * Whether sub-effect aborted by conditions.
	 *
	 */
	private boolean subEffectAbortedBySubConditions;
	/**
	 * 获取关联物品模板。
	 * Gets related item template.
	 *
	 */
	private ItemTemplate itemTemplate;
	/**
	 * 设置 HiPass 标记。
	 * Sets HiPass flag.
	 *
	 */
	private boolean isHiPass;
	/**
	 * 设置无死亡惩罚标记。
	 * Sets no-death-penalty flag.
	 *
	 */
	private boolean isNoDeathPenalty;
	/**
	 * 设置死亡惩罚减免标记。
	 * Sets no-death-penalty-reduce flag.
	 *
	 */
	private boolean isNoDeathPenaltyReduce;
	/**
	 * 设置无复活惩罚标记。
	 * Sets no-resurrect-penalty flag.
	 *
	 */
	private boolean isNoResurrectPenalty;
	/**
	 * 获取嘲讽仇恨。
	 * Gets taunt hate.
	 *
	 */
	private int tauntHate;
	/**
	 * 获取效果仇恨。
	 * Gets effect hate.
	 *
	 */
	private int effectHate;
	@Getter(AccessLevel.NONE)
	private final EffectSuccessSet successEffects = new EffectSuccessSet();
	/**
	 * 获取刻印数量。
	 * Gets carved signet count.
	 *
	 */
	private int carvedSignet = 0;
	/**
	 * 获取印记爆发数量。
	 * Gets signet burst count.
	 *
	 */
	private int signetBurstedCount = 0;
	/**
	 * 获取异常状态掩码。
	 * Gets abnormal state mask.
	 *
	 */
	protected int abnormals;
	private ActionObserver[] actionObserver;
	float x, y, z;
	int worldId, instanceId;
	/**
	 * 设置强制持续标记。
	 * Sets forced duration flag.
	 *
	 */
	private boolean forcedDuration = false;
	private boolean isForcedEffect = false;
	/**
	 * 获取强度。
	 * Gets power.
	 *
	 */
	private int power = 10;
	/**
	 * 设置命中修正加成。
	 * Sets accuracy mod boost.
	 *
	 */
	private int accModBoost = 0;
	private EffectResult effectResult = EffectResult.NORMAL;
	private final AtomicBoolean allowGodstoneActivation = new AtomicBoolean(true);

	public boolean tryActivateGodstone() {
		return allowGodstoneActivation.compareAndSet(true, false);
	}

	/**
	 * 设置异常状态掩码。
	 * Sets abnormal state mask.
	 *
	 */
	public void setAbnormal(int mask) {
		abnormals |= mask;
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
	 * @param worldId 世界 ID / world id
	 * @param instanceId 实例 ID / instance id
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
	 */
	public int getSkillId() {
		return skillTemplate.getSkillId();
	}

	/**
	 * 获取技能名称。
	 * Gets skill name.
	 *
	 */
	public String getSkillName() {
		return skillTemplate.getName();
	}

	public boolean isDamageProtectorEnabled() {
		return !(currentEffectTemplate instanceof ProcAtkInstantEffect proc) || proc.isCheckProtector();
	}

	/**
	 * 获取技能子类型。
	 * Gets skill sub type.
	 *
	 */
	public SkillSubType getSkillSubType() {
		return skillTemplate.getSubType();
	}

	/**
	 * 获取技能组例外。
	 * Gets skill-set exception.
	 *
	 */
	public int getSkillSetException() {
		return skillTemplate.getSkillSetException();
	}

	/**
	 * 获取技能组最大共存。
	 * Gets skill-set max occurrence.
	 *
	 */
	public int getSkillSetMaxOccur() {
		return skillTemplate.getSkillSetMaxOccur();
	}

	/**
	 * 获取堆叠键。
	 * Gets stack key.
	 *
	 */
	public String getStack() {
		return skillTemplate.getStack();
	}

	/**
	 * 获取技能组。
	 * Gets skill group.
	 *
	 */
	public String getGroup() {
		return skillTemplate.getGroup();
	}

	/**
	 * 获取技能堆叠等级。
	 * Gets skill stack level.
	 *
	 */
	public int getSkillStackLvl() {
		return skillTemplate.getLvl();
	}

	/**
	 * 获取技能类型。
	 * Gets skill type.
	 *
	 */
	public SkillType getSkillType() {
		return skillTemplate.getType();
	}

	/**
	 * 是否被动。
	 * Whether passive.
	 *
	 */
	public boolean isPassive() {
		return skillTemplate.isPassive();
	}

	/**
	 * 获取周期任务。
	 * Gets periodic task.
	 *
	 * @param i 索引（从 1 起） / 1-based index
	 * @return 任务 / task
	 */
	public Future<?> getPeriodicTask(int i) {
		return periodicTasks != null ? periodicTasks[i - 1] : null;
	}

	/**
	 * 设置周期任务。
	 * Sets periodic task.
	 *
	 * @param periodicTask 任务 / task
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
	 */
	public int getReserved1() {
		return currentEffectTemplate == null ? reserved1 : reserved1ByEffect.getOrDefault(currentEffectTemplate, reserved1);
	}

	/**
	 * 设置保留值 1。
	 * Sets reserved value 1.
	 *
	 */
	public void setReserved1(int reserved1) {
		this.reserved1 = reserved1;
		if (currentEffectTemplate != null) {
			reserved1ByEffect.put(currentEffectTemplate, reserved1);
		}
	}

	public AttackStatus getPeriodicAttackStatus(int position) {
		return periodicAttackStatuses == null ? null : periodicAttackStatuses[position - 1];
	}

	public void setPeriodicAttackStatus(int position, AttackStatus attackStatus) {
		if (periodicAttackStatuses == null) {
			periodicAttackStatuses = new AttackStatus[4];
		}
		periodicAttackStatuses[position - 1] = attackStatus;
	}

	/**
	 * 获取效果模板列表。
	 * Gets effect template list.
	 *
	 */
	public List<EffectTemplate> getEffectTemplates() {
		return skillTemplate.getEffects().getEffects();
	}

	/**
	 * 是否瞬时 MP 治疗。
	 * Whether MP heal instant.
	 *
	 */
	public boolean isMphealInstant() {
		Effects effects = skillTemplate.getEffects();
		return effects != null && effects.isMpHealInstant();
	}

	/**
	 * 是否切换型。
	 * Whether toggle.
	 *
	 */
	public boolean isToggle() {
		return skillTemplate.getActivationAttribute() == ActivationAttribute.TOGGLE;
	}

	/**
	 * 是否咏唱/战歌。
	 * Whether chant.
	 *
	 */
	public boolean isChant() {
		return skillTemplate.getTargetSlot() == SkillTargetSlot.CHANT;
	}

	/**
	 * 是否增益。
	 * Whether buff.
	 *
	 */
	public boolean isBuff() {
		return skillTemplate.getTargetSlot() == SkillTargetSlot.BUFF;
	}

	/**
	 * 是否弓星类增益。
	 * Whether ranger buff.
	 *
	 */
	public boolean isRangerBuff() {
		int skillId = skillTemplate.getSkillId();
		return switch (skillId) { // 强化之眼 I / Strong Shots.
			// 警戒之眼 I / Dodging.
			// 攻击之眼 I / Focused Shots.
			// 猎人的决心 I / Hunter's Might.
			// 速射之眼 I / Bestial Fury.
			// 集中之眼 I / Aiming.
			case 796, 809, 813, 888, 889, 1053, 1099 -> // 透视之眼 I / Hunter's Eye.
				true;
			default -> false;
		};
	}

	/**
	 * 获取目标槽位 ID。
	 * Gets target slot id.
	 *
	 */
	public int getTargetSlot() {
		return skillTemplate.getTargetSlot().ordinal();
	}

	/**
	 * 获取目标槽位枚举。
	 * Gets target slot enum.
	 *
	 */
	public SkillTargetSlot getTargetSlotEnum() {
		return skillTemplate.getTargetSlot();
	}

	/**
	 * 获取目标槽位等级。
	 * Gets target slot level.
	 *
	 */
	public int getTargetSlotLevel() {
		return skillTemplate.getTargetSlotLevel();
	}

	/**
	 * 获取驱散分类。
	 * Gets dispel category.
	 *
	 */
	public DispelCategoryType getDispelCategory() {
		return skillTemplate.getDispelCategory();
	}

	/**
	 * 获取所需驱散等级。
	 * Gets required dispel level.
	 *
	 */
	public int getReqDispelLevel() {
		return skillTemplate.getReqDispelLevel();
	}
	/**
	 * 获取攻击状态观察者。
	 * Gets attack status observer.
	 *
	 * @param i 索引 / index
	 */
	public AttackCalcObserver getAttackStatusObserver(int i) {
		return attackStatusObserver != null ? attackStatusObserver[i - 1] : null;
	}
	/**
	 * 设置攻击状态观察者。
	 * Sets attack status observer.
	 *
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
	 */
	public AttackCalcObserver getAttackShieldObserver(int i) {
		return attackShieldObserver != null ? attackShieldObserver[i - 1] : null;
	}
	/**
	 * 设置护盾观察者。
	 * Sets attack shield observer.
	 *
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
	 */
	public int getReservedInt(int i) {
		return reservedInts != null ? reservedInts[i - 1] : 0;
	}

	/**
	 * 设置保留整型数组值。
	 * Sets reserved int array value.
	 *
	 * @param i 索引 / index
	 */
	public void setReservedInt(int i, int value) {
		if (this.reservedInts == null) {
			this.reservedInts = new int[4];
		}
		this.reservedInts[i - 1] = value;
	}
	/**
	 * 是否包含指定效果 ID。
	 * Whether contains effect id.
	 *
	 */
	public boolean containsEffectId(int effectId) {
		return successEffects.containsEffectId(effectId);
	}

	/**
	 * 获取变身类型。
	 * Gets transform type.
	 *
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
	 * 设置强制效果标记。
	 * Sets forced effect flag.
	 *
	 */
	public void setIsForcedEffect(boolean isForcedEffect) {
		this.isForcedEffect = isForcedEffect;
	}

	/**
	 * 是否强制效果。
	 * Whether forced effect.
	 *
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
	 * 向所有可见对象广播最终仇恨值 / broadcast final hate to all visible objects
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
	 */
	public synchronized void startEffect(boolean restored) {
		if (isStopped || successEffects.isEmpty()) {
			return;
		}
		try {
			shedulePeriodicActions();
			for (int i = 0; i < successEffects.size(); i++) {
				startedTemplateCount = i + 1;
				successEffects.get(i).startEffect(this);
			}
			terminationObservers.attach();
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
			task = GameThreadPoolServices.threadPoolManager().schedule(() -> endEffect(), duration);
		} catch (RuntimeException | Error failure) {
			try {
				endEffect();
			} catch (RuntimeException | Error cleanupFailure) {
				failure.addSuppressed(cleanupFailure);
			}
			throw failure;
		}
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
		isStopped = true;
		int previousAbnormals = effected.getEffectController().getAbnormals();
		Throwable failure = null;
		for (int i = 0; i < startedTemplateCount; i++) {
			try {
				successEffects.get(i).endEffect(this);
			} catch (RuntimeException | Error templateFailure) {
				failure = collectFailure(failure, templateFailure);
			}
		}
		try {
			// 若效果为姿态，则从玩家移除姿态 / If effect is a stance, remove stance from player
			if (effector instanceof Player player) {
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
		} catch (RuntimeException | Error stateFailure) {
			failure = collectFailure(failure, stateFailure);
		}
		try {
			stopTasks();
		} catch (RuntimeException | Error taskFailure) {
			failure = collectFailure(failure, taskFailure);
		}
		try {
			effected.getEffectController().clearEffect(this);
		} catch (RuntimeException | Error clearFailure) {
			failure = collectFailure(failure, clearFailure);
		}
		addedToController = false;
		failure = terminationObservers.clear(failure);
		int leftAbnormals = previousAbnormals & ~effected.getEffectController().getAbnormals();
		if (leftAbnormals != 0 && effected instanceof Npc npc) {
			try {
				npc.getAi2().onLeaveAbnormalState(effector, leftAbnormals);
			} catch (RuntimeException | Error aiFailure) {
				failure = collectFailure(failure, aiFailure);
			}
		}
		if (failure instanceof RuntimeException runtimeFailure) {
			throw runtimeFailure;
		}
		if (failure instanceof Error errorFailure) {
			throw errorFailure;
		}
	}

	private static Throwable collectFailure(Throwable failure, Throwable nextFailure) {
		if (failure == null) {
			return nextFailure;
		}
		if (failure != nextFailure) {
			failure.addSuppressed(nextFailure);
		}
		return failure;
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
	 */
	public int getRemainingTime() {
		int remainingTime = (int) (endTime - System.currentTimeMillis());
		if (this.getDuration() >= 86400000) {
			remainingTime = -1;
		}
		return remainingTime > 0 ? remainingTime : -1;
	}
	/**
	 * 获取 PvP 伤害系数。
	 * Gets PvP damage factor.
	 *
	 */
	public int getPvpDamage() {
		return skillTemplate.getPvpDamage();
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
	 * 获取动作观察者。
	 * Gets action observer.
	 *
	 * @param i 索引 / index
	 */
	public ActionObserver getActionObserver(int i) {
		return actionObserver != null ? actionObserver[i - 1] : null;
	}
	/**
	 * 设置动作观察者。
	 * Sets action observer.
	 *
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
	 */
	public void addSucessEffect(EffectTemplate effect) {
		successEffects.add(effect);
	}

	/**
	 * 是否在成功效果列表中。
	 * Whether in success effects.
	 *
	 */
	public boolean isInSuccessEffects(int position) {
		return successEffects.containsPosition(position);
	}
	/**
	 * 获取成功效果集合。
	 * Gets success effect collection.
	 *
	 */
	public Collection<EffectTemplate> getSuccessEffect() {
		return successEffects.asCollection();
	}

	/**
	 * 获取成功效果列表（兼容原字段访问器）。
	 * Gets the success effect list (compatibility accessor for the former generated getter).
	 *
	 * @return 成功效果列表 / success effect list
	 */
	public List<EffectTemplate> getSuccessEffects() {
		return successEffects.asList();
	}

	/**
	 * 将全部效果标为成功。
	 * Marks all effects as success.
	 *
	 */
	public void addAllEffectToSucess() {
		successEffects.replaceWith(getEffectTemplates());
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
		periodicActionsTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
			for (PeriodicAction action : periodicActions.getPeriodicActions()) {
				action.act(Effect.this);
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
	 */
	public boolean isDeityAvatar() {
		return skillTemplate.isDeityAvatar();
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
	 */
	public void setSubEffectAborted(boolean value) {
		this.subEffectAbortedBySubConditions = value;
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
	 */
	public boolean isFearEffect() {
		return successEffects.containsFearEffect();
	}

	/**
	 * 设置召唤中标记（历史拼写）。
	 * Sets summoning flag (legacy spelling).
	 *
	 */
	public void setSumonning(boolean value) {
		this.isSummoning = value;
	}

	private int initializePower(SkillTemplate skill) {
		return skill.getReqDispelCount();
	}
	/**
	 * 削减强度。
	 * Removes power amount.
	 *
	 */
	public int removePower(int power) {
		this.power -= power;
		return this.power;
	}

	/**
	 * 设置效果结算结果。
	 * Sets effect result.
	 *
	 */
	public final void setEffectResult(EffectResult effectResult) {
		this.effectResult = effectResult;
	}

	/**
	 * 是否物理控制状态。
	 * Whether physical state.
	 *
	 */
	private boolean isPhysicalState = false;
	/**
	 * 是否魔法控制状态。
	 * Whether magical state.
	 *
	 */
	private boolean isMagicalState = false;

	/**
	 * 设置物理控制状态标记。
	 * Sets physical state flag.
	 *
	 */
	public void setIsPhysicalState(boolean isPhysicalState) {
		this.isPhysicalState = isPhysicalState;
	}

	/**
	 * 设置魔法控制状态标记。
	 * Sets magical state flag.
	 *
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
        return switch (getSkillId()) {
            case 8224, 8678, 9173, 19552, 20371, 20680, 20872, 21133, 21476, 21529, 21911 -> true;
            default -> false;
        };
    }
}

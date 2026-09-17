package com.aionemu.gameserver.skillengine.model;

import com.aionemu.boot.i18n.I18n;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEventBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.handler.ShoutEventHandler;
import com.aionemu.gameserver.ai2.manager.SkillAttackManager;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.observer.StartMovingListener;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skinskill.SkillSkin;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CASTSPELL;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CASTSPELL_RESULT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUIT_RESPONSE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.MotionLoggingService;
import com.aionemu.gameserver.services.abyss.AbyssService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.action.Action;
import com.aionemu.gameserver.skillengine.action.Actions;
import com.aionemu.gameserver.skillengine.condition.Conditions;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.properties.FirstTargetAttribute;
import com.aionemu.gameserver.skillengine.properties.Properties;
import com.aionemu.gameserver.skillengine.properties.TargetRangeAttribute;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 运行时技能实例：驱动施法流程、目标选择、效果应用与结束。
 * Runtime skill instance: drives cast flow, targeting, effect application and end.
 *
 * @author ATracer Modified by Wakzashi
 */
@Slf4j
@Getter
@Setter

public class Skill {

	/**
	 * 设置多段施法标记（历史方法名，保留以兼容既有调用）。
	 * Sets the multi-cast flag (legacy setter name kept for existing callers).
	 *
	 * @param isMultiCast 是否多段施法 / whether multi-cast
	 */
	public void setIsMultiCast(boolean isMultiCast) {
		this.isMultiCast = isMultiCast;
	}

	private SkillMethod skillMethod = SkillMethod.CAST;
	private final List<Creature> effectedList;
	private Creature firstTarget;
	private final Creature effector;
	private final int skillLevel;
	private final int skillStackLvl;
	private final StartMovingListener conditionChangeListener;
	private SkillTemplate skillTemplate;
	/**
	 * -- GETTER --
	 *  是否检查主目标距离。
	 *  Whether first-target range is checked.
	 */
	private boolean firstTargetRangeCheck = true;
	private ItemTemplate itemTemplate;
	private int itemObjectId = 0;
	private int targetType;
	private boolean chainSuccess;
	private boolean blockedPenaltySkill = false;
	private float x;
	private float y;
	private float z;
	private byte h;
	private int boostSkillCost;
	private FirstTargetAttribute firstTargetAttribute;
	private TargetRangeAttribute targetRangeAttribute;
	private int skillskinId = 0;
	private int skillskinHitTIme = 0;
	/** 充能阶段时间缩放的兜底下限，避免速度修正为 0 时阶段窗口塌缩。 / Lower bound for charge-stage time scaling. */
	private static final float MIN_CHARGE_SPEED_RATIO = 0.3f;
	private ChargeSkillTemplate chargeTemplate = null;
	private float chargeTimeMultiplier = 1;
	private volatile Future<?> castingTask = null;
	private final AtomicReference<CastState> castState = new AtomicReference<>(CastState.CASTING);
	private long castStart = 0;
	/**
	 * 依赖 BOOST_CASTING_TIME 的持续时间。
	 * Duration that depends on BOOST_CASTING_TIME
	 */
	private int duration;
	private int hitTime;// 来自 CM_CASTSPELL / from CM_CASTSPELL
	private int serverTime;// 效果生效的服务器时间 / time when effect is applied
	private String chainCategory = null;
	private volatile boolean isMultiCast = false;
	private List<Creature> preselectedTargets;
	private boolean broadcastToPreselectedTargets;

	public enum SkillMethod {
		CAST, ITEM, PASSIVE, PROVOKED
	}

	/**
	 * 施法生命周期状态；取消与完成只能有一方取得所有权。
	 * Cast lifecycle state; only cancellation or completion may acquire ownership.
	 */
	private enum CastState {
		CASTING, CANCELLED, COMPLETING
	}
	/**
	 * 构造运行时技能实例。
	 * Constructs a runtime skill instance.
	 *
	 */
	public Skill(SkillTemplate skillTemplate, Player effector, Creature firstTarget) {
		this(skillTemplate, effector, effector.getSkillList().getSkillLevel(skillTemplate.getSkillId()), firstTarget, null);
	}

	/**
	 * 构造运行时技能实例。
	 * Constructs a runtime skill instance.
	 *
	 */
	public Skill(SkillTemplate skillTemplate, Player effector, Creature firstTarget, int skillLevel) {
		this(skillTemplate, effector, skillLevel, firstTarget, null);
	}
	/**
	 * 构造运行时技能实例。
	 * Constructs a runtime skill instance.
	 *
	 */
	public Skill(SkillTemplate skillTemplate, Creature effector, int skillLvl, Creature firstTarget, ItemTemplate itemTemplate) {
		this.effectedList = new ArrayList<Creature>();
		this.conditionChangeListener = new StartMovingListener();
		this.firstTarget = firstTarget;
		this.skillLevel = skillLvl;
		this.skillStackLvl = skillTemplate.getLvl();
		this.skillTemplate = skillTemplate;
		this.effector = effector;
		this.duration = skillTemplate.getDuration();
		this.itemTemplate = itemTemplate;
		if (skillTemplate.getChargeSetName() != null) {
			this.chargeTemplate = DataManager.CHARGE_SKILL_DATA.getChargeSkillTemplateBySetName(skillTemplate.getChargeSetName());
		}
		if (itemTemplate != null) {
			skillMethod = SkillMethod.ITEM;
		} else if (skillTemplate.isPassive()) {
			skillMethod = SkillMethod.PASSIVE;
		} else if (skillTemplate.isProvoked()) {
			skillMethod = SkillMethod.PROVOKED;
		}
	}
	/**
	 * 校验当前是否可使用该技能。
	 * Validates whether the skill can be used now.
	 *
	 * @return 是否可用 / whether usable
	 */
	public boolean canUseSkill() {
		if (skillTemplate.getType() == SkillType.MAGICAL && !skillTemplate.hasEvadeEffect()
				&& effector.getEffectController().isAbnormalSet(AbnormalState.SILENCE)) {
			return false;
		}
		if (skillTemplate.isStanceUsable()
				&& (!(effector instanceof Player player) || player.getController().getStanceType() != 2)) {
			return false;
		}
		if (skillTemplate.isUltraTransfer()) {
			if (!(effector instanceof Player source) || !(firstTarget instanceof Player target)
					|| source.getPlayerClass() != target.getPlayerClass() || target.getCastingSkill() == null
					|| !target.getCastingSkill().getSkillTemplate().isUltraSkill()) {
				return false;
			}
		}
		Properties properties = skillTemplate.getProperties();
		if (properties != null && !(preselectedTargets == null
				? properties.validate(this) : properties.validatePreselectedTargets(this))) {
			log.debug("properties failed");
			return false;
		}

		if (!preCastCheck()) {
			return false;
		}
		// 检查反击技能 / check for counter skill
		if (effector instanceof Player player) {
			if (this.skillTemplate.getCounterSkill() != null) {
				long time = player.getLastCounterSkill(skillTemplate.getCounterSkill());
				if ((time + 5000) < System.currentTimeMillis()) {
					log.debug("chain skill failed, too late");
					return false;
				}
			}

			if (skillMethod == SkillMethod.ITEM && duration > 0 && player.getMoveController().isInMove()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED(new DescriptionId(getItemTemplate().getNameId())));
				return false;
			}
		}

		return validateEffectedList();
	}

	private boolean validateEffectedList() {
		Iterator<Creature> effectedIter = effectedList.iterator();
		while (effectedIter.hasNext()) {
			Creature effected = effectedIter.next();
			if (effected == null) {
				effected = effector;
			}
			if (effector instanceof Player) {
				if (!RestrictionsManager.canAffectBySkill((Player) effector, effected, this)) {
					effectedIter.remove();
				}
			} else {
				if (effector.getEffectController().isAbnormalState(AbnormalState.CANT_ATTACK_STATE)) {
					effectedIter.remove();
				}
			}
		}

		// 空对象目标施法无效；自身与范围技能仍可在无命中目标时激活。 / Empty object-target casts are invalid; self-targeted and area skills can still activate without a hit target.
		if (targetType == 0 && effectedList.isEmpty() && firstTargetAttribute != FirstTargetAttribute.ME && targetRangeAttribute != TargetRangeAttribute.AREA) {
			log.debug("targettype failed");
			return false;
		}
		return true;
	}
	/**
	 * 使用技能（含动画）。
	 * Uses the skill (with animation).
	 *
	 * @return 是否成功 / whether succeeded
	 */
	public boolean useSkill() {
		return useSkill(true, true);
	}

	/**
	 * 对外部区域预选的目标执行技能；技能模板仍负责关系、物种、状态与效果判定。
	 * Executes the skill against the targets preselected by an external area; the skill template still
	 * governs relation, species, state and effect checks.
	 */
	public boolean useSkillOnPreselectedTargets(Collection<? extends Creature> targets, boolean broadcastToTargets) {
		preselectedTargets = List.copyOf(targets);
		broadcastToPreselectedTargets = broadcastToTargets;
		effectedList.clear();
		effectedList.addAll(preselectedTargets);
		firstTarget = effectedList.isEmpty() ? effector : effectedList.getFirst();
		return useSkill();
	}

	/**
	 * 使用技能（无动画）。
	 * Uses the skill without animation.
	 *
	 * @return 是否成功 / whether succeeded
	 */
	public boolean useNoAnimationSkill() {
		return useSkill(false, true);
	}

	/**
	 * 使用技能（跳过属性校验）。
	 * Uses the skill without property checks.
	 *
	 * @return 是否成功 / whether succeeded
	 */
	public boolean useWithoutPropSkill() {
		return useSkill(false, false);
	}

	private boolean useSkill(boolean checkAnimation, boolean checkproperties) {
		if (checkproperties && !canUseSkill()) {
			return false;
		}

		calculateSkillDuration();
		if (chargeTemplate != null) {
			chargeTimeMultiplier = calculateChargeTimeMultiplier();
		}

		if (SecurityConfig.MOTION_TIME) {
			// 必须在计算技能持续时间之后执行 / must be after calculateskillduration
			if (checkAnimation && !checkAnimationTime()) {
				log.debug("check animation time failed");
				return false;
			}
		}

		boostSkillCost = 0;
		getSkillSkinData();

		// 通知技能使用观察者 / notify skill use observers
		if (skillMethod == SkillMethod.CAST) {
			effector.getObserveController().notifySkilluseObservers(this);
		}
		// 开始施法 / start casting
		effector.setCasting(this);

		// 若施法者为玩家则记录技能时间 / log skill time if effector instance of player
		if (effector instanceof Player) {
			GameFeatureServices.motionLoggingService().logTime((Player) effector, this.getSkillTemplate(), this.getHitTime(), MathUtil.getDistance(effector, firstTarget));
		}
		boolean setCooldowns = true;
		if (effector instanceof Player) {
			if (this.isMulticast() && ((Player) effector).getChainSkills().getChainCount((Player) effector, this.getSkillTemplate(), this.chainCategory) != 0) {
				setCooldowns = false;
			}
		}

		if (setCooldowns) {
			this.setCooldowns();
		}
		// 发送数据包以开始施法 / send packets to start casting
		if ((skillMethod == SkillMethod.CAST || skillMethod == SkillMethod.ITEM) && preselectedTargets == null) {
			startCast();
			if (effector instanceof Npc) {
				((NpcAI2) effector.getAi2()).setSubStateIfNot(AISubState.CAST);
			}
		}
		effector.getObserveController().attach(conditionChangeListener);

		if (chargeTemplate != null) {
			this.duration = 0;
			for (ChargeTemplate charge : chargeTemplate.getCharges()) {
				this.duration += scaleChargeTime(charge.getTime());
			}
		}
		if (preselectedTargets != null) {
			endCast();
		} else if (this.duration > 0) {
			schedule(this.duration);
		} else {
			endCast();
		}
		return true;
	}

	private void setCooldowns() {
		int cooldown = chainCategory == null && skillTemplate.getNonchainedCooldown() > 0
				? Math.max(1, Math.ceilDiv(skillTemplate.getNonchainedCooldown(), 100))
				: effector.getSkillCooldown(skillTemplate);
		if (cooldown != 0) {
			cooldown = calculateCooldown(cooldown);
			effector.setSkillCoolDown(skillTemplate.getDelayId(), cooldown * 100L + this.duration + System.currentTimeMillis());
			effector.setSkillCoolDownBase(skillTemplate.getDelayId(), System.currentTimeMillis());
		}
	}

	public int calculateCooldown(int cooldown) {
		cooldown = StigmaEnchantCoolDown(this, cooldown);
		cooldown = skillTemplate.scaleCooldownByAttackDelay(cooldown,
				effector.getGameStats().getAttackSpeed().getCurrent());
		return SkillConfig.scaleCooldown(cooldown);
	}

	/**
	 * 计算烙印附魔冷却。
	 * Computes stigma enchant cooldown.
	 *
	 * @param skill 技能实例 / skill
	 * @param cooldown 基础冷却 / base cooldown
	 * @return 最终冷却 / final cooldown
	 */
	public int StigmaEnchantCoolDown(Skill skill, int cooldown) {
		if (skill == null) {
			return 0;
		}
		int SkillLevel = skill.getSkillLevel();
		if (skill.getSkillTemplate().getCooldownDelta() != 0) {
			return Math.max(0, cooldown + skill.getSkillTemplate().getCooldownDelta() * SkillLevel);
		}
		switch (skill.getSkillId()) {
		case 564: // 气魄 / Dauntless Spirit
		case 565: // 气魄 / Dauntless Spirit
		case 566: // 气魄 / Dauntless Spirit
		case 567: // 气魄 / Dauntless Spirit
		case 568: // 气魄 / Dauntless Spirit
		case 569: // 气魄 / Dauntless Spirit
		case 570: // 气魄 / Dauntless Spirit
		case 571: // 气魄 / Dauntless Spirit
		case 727: // Wind Lance
		case 728: // Wind Lance
		case 729: // Wind Lance
		case 730: // Wind Lance
		case 731: // Wind Lance
		case 732: // Wind Lance
		case 755: // 回旋一击 / Whirling Strike
		case 756: // 回旋一击 / Whirling Strike
		case 757: // 回旋一击 / Whirling Strike
		case 1100: // 透视陷阱 / Trap Of Clairvoyance
		case 1101: // 透视陷阱 / Trap Of Clairvoyance
		case 1324: // 冰河重击 / Glacial Shard
		case 1325: // 冰河重击 / Glacial Shard
		case 1326: // 冰河重击 / Glacial Shard
		case 1640: // 灭火 / Annihilation
		case 1641: // 灭火 / Annihilation
		case 1642: // 灭火 / Annihilation
		case 1643: // 灭火 / Annihilation
		case 1644: // 灭火 / Annihilation
		case 1645: // 灭火 / Annihilation
		case 1646: // 灭火 / Annihilation
		case 1647: // 灭火 / Annihilation
		case 1727: // 生命之咒语 / Word Of Life
		case 1728: // 生命之咒语 / Word Of Life
		case 1729: // 生命之咒语 / Word Of Life
		case 1730: // 生命之咒语 / Word Of Life
		case 1731: // 生命之咒语 / Word Of Life
		case 1732: // 生命之咒语 / Word Of Life
		case 1733: // 生命之咒语 / Word Of Life
		case 1734: // 生命之咒语 / Word Of Life
		case 1863: // 波动攻击 / Disorienting Blow
		case 1864: // 波动攻击 / Disorienting Blow
		case 1865: // 波动攻击 / Disorienting Blow
		case 1866: // 波动攻击 / Disorienting Blow
		case 1867: // 波动攻击 / Disorienting Blow
		case 1868: // 波动攻击 / Disorienting Blow
		case 1883: // 爆裂 / Burst
		case 1884: // 爆裂 / Burst
		case 1885: // 爆裂 / Burst
		case 1886: // 爆裂 / Burst
		case 1887: // 爆裂 / Burst
		case 1888: // 爆裂 / Burst
		case 1889: // 爆裂 / Burst
		case 1890: // 爆裂 / Burst
		case 1907: // Word of Instigation
		case 1908: // Word of Instigation
		case 1909: // Word of Instigation
		case 2046: // 魔力之恩惠 I / Stopping Power
		case 2054: // 填装魔力弹 I / Autoload
		case 2268: // 必中魔眼 / Sighting
		case 2269: // 必中魔眼 / Sighting
		case 2270: // 必中魔眼 / Sighting
		case 2271: // 必中魔眼 / Sighting
		case 2272: // 必中魔眼 / Sighting
		case 2273: // 必中魔眼 / Sighting
		case 2391: // 盔甲破坏 / Drillbore
		case 2392: // 盔甲破坏 / Drillbore
		case 2393: // 盔甲破坏 / Drillbore
		case 2394: // 盔甲破坏 / Drillbore
		case 2395: // 盔甲破坏 / Drillbore
		case 2396: // 盔甲破坏 / Drillbore
		case 2397: // 盔甲破坏 / Drillbore
		case 2398: // 盔甲破坏 / Drillbore
		case 2409: // 要害戳刺 / Debilitating Blade
		case 2410: // 要害戳刺 / Debilitating Blade
		case 2411: // 要害戳刺 / Debilitating Blade
		case 2412: // 要害戳刺 / Debilitating Blade
		case 2413: // 要害戳刺 / Debilitating Blade
		case 2414: // 要害戳刺 / Debilitating Blade
		case 2464: // Aether Recharge
		case 2467: // Aether Recharge
		case 2470: // Aether Recharge
		case 2473: // Aether Recharge
		case 2476: // Aether Recharge
		case 2479: // Aether Recharge
		case 2482: // Aether Recharge
		case 2485: // Aether Recharge
		case 2711: // 电场束缚 / Convulsion Beam
		case 2712: // 电场束缚 / Convulsion Beam
		case 2713: // 电场束缚 / Convulsion Beam
		case 2714: // 电场束缚 / Convulsion Beam
		case 2715: // 电场束缚 / Convulsion Beam
		case 2716: // 电场束缚 / Convulsion Beam
		case 2919: // Invigorating Strike
		case 2920: // Invigorating Strike
		case 2921: // Invigorating Strike
		case 2945: // 愤怒诱发 / Incite Rage
		case 2946: // 愤怒诱发 / Incite Rage
		case 2947: // 愤怒诱发 / Incite Rage
		case 2948: // 愤怒诱发 / Incite Rage
		case 2949: // 愤怒诱发 / Incite Rage
		case 2950: // 愤怒诱发 / Incite Rage
		case 2951: // 愤怒诱发 / Incite Rage
		case 2952: // 愤怒诱发 / Incite Rage
		case 2961: // 保护之盾 / Holy Shield
		case 2962: // 保护之盾 / Holy Shield
		case 2963: // 保护之盾 / Holy Shield
		case 2964: // 保护之盾 / Holy Shield
		case 2965: // 保护之盾 / Holy Shield
		case 2966: // 保护之盾 / Holy Shield
		case 3147: // 处决一击 / Punishing Thrust
		case 3148: // 处决一击 / Punishing Thrust
		case 3149: // 处决一击 / Punishing Thrust
		case 3150: // 处决一击 / Punishing Thrust
		case 3151: // 处决一击 / Punishing Thrust
		case 3152: // 处决一击 / Punishing Thrust
		case 3153: // 处决一击 / Punishing Thrust
		case 3154: // 处决一击 / Punishing Thrust
		case 3242: // Explosive Rebranding
		case 3243: // Explosive Rebranding
		case 3244: // Explosive Rebranding
		case 3246: // 昏厥之刃 / Quickening Doom
		case 3247: // 昏厥之刃 / Quickening Doom
		case 3248: // 昏厥之刃 / Quickening Doom
		case 3312: // 愤怒之眼 I / Eye Of Wrath
		case 3330: // 影子下坠 / Shadowfall
		case 3731: // 魔力诅咒 I / Magic's Freedom
		case 3796: // 精灵强化:强化甲胄 I / Armor Spirit
		case 3980: // 召唤:治愈之气息 / Summon Healing Servant
		case 3981: // 召唤:治愈之气息 / Summon Healing Servant
		case 3982: // 召唤:治愈之气息 / Summon Healing Servant
		case 3983: // 召唤:治愈之气息 / Summon Healing Servant
		case 3984: // 召唤:治愈之气息 / Summon Healing Servant
		case 3985: // 召唤:治愈之气息 / Summon Healing Servant
		case 3986: // 召唤:治愈之气息 / Summon Healing Servant
		case 3987: // 召唤:治愈之气息 / Summon Healing Servant
		case 3988: // 召唤:治愈之气息 / Summon Healing Servant
		case 3989: // 召唤:治愈之气息 / Summon Healing Servant
		case 3990: // 召唤:治愈之气息 / Summon Healing Servant
		case 3991: // 召唤:治愈之气息 / Summon Healing Servant
		case 3998: // 再生之光辉 / Splendor Of Rebirth
		case 3999: // 再生之光辉 / Splendor Of Rebirth
		case 4000: // 再生之光辉 / Splendor Of Rebirth
		case 4001: // 再生之光辉 / Splendor Of Rebirth
		case 4002: // 再生之光辉 / Splendor Of Rebirth
		case 4003: // 再生之光辉 / Splendor Of Rebirth
		case 4134: // 恢复阻断 I / Festering Wound
		case 4164: // 霹雳 / Call Lightning
		case 4165: // 霹雳 / Call Lightning
		case 4166: // 霹雳 / Call Lightning
		case 4384: // 平稳变奏曲 / Resonant Hymn
		case 4385: // 平稳变奏曲 / Resonant Hymn
		case 4386: // 平稳变奏曲 / Resonant Hymn
		case 4387: // 平稳变奏曲 / Resonant Hymn
		case 4388: // 平稳变奏曲 / Resonant Hymn
		case 4389: // 平稳变奏曲 / Resonant Hymn
		case 4390: // 平稳变奏曲 / Resonant Hymn
		case 4474: // Blazing Requiem
		case 4477: // Blazing Requiem
		case 4480: // Blazing Requiem
		case 4484: // 免罪旋律 / Chorus Of Blessing
		case 4485: // 免罪旋律 / Chorus Of Blessing
		case 4486: // 免罪旋律 / Chorus Of Blessing
		case 4487: // 不和谐音 / Treble Cleave
		case 4488: // 不和谐音 / Treble Cleave
		case 4489: // 不和谐音 / Treble Cleave
		case 4491: // Mvt.2: Summer
		case 4492: // Mvt.2: Summer
		case 4493: // Mvt.2: Summer
		case 4494: // Mvt.2: Summer
		case 4495: // Mvt.2: Summer
		case 4496: // Mvt.2: Summer
		case 4497: // Mvt.3: Autumn
		case 4498: // Mvt.3: Autumn
		case 4499: // Mvt.3: Autumn
		case 4500: // Mvt.3: Autumn
		case 4501: // Mvt.3: Autumn
		case 4502: // Mvt.3: Autumn
		case 4524: // 莫斯奇狂想曲 / Paean Of Pain
		case 4525: // 莫斯奇狂想曲 / Paean Of Pain
		case 4526: // 莫斯奇狂想曲 / Paean Of Pain
		case 4527: // 莫斯奇狂想曲 / Paean Of Pain
		case 4528: // 莫斯奇狂想曲 / Paean Of Pain
		case 4529: // 莫斯奇狂想曲 / Paean Of Pain
		case 4572: // Combustible Cacophony
		case 4573: // Combustible Cacophony
		case 4574: // Combustible Cacophony
		case 4575: // Combustible Cacophony
		case 4576: // Combustible Cacophony
		case 4577: // Combustible Cacophony
		case 4578: // Combustible Cacophony
		case 4579: // Combustible Cacophony
		case 4591: // 影子下坠 / Shadowfall
		case 4592: // 影子下坠 / Shadowfall
		case 4593: // 影子下坠 / Shadowfall
		case 4594: // 影子下坠 / Shadowfall
		case 4595: // 影子下坠 / Shadowfall
		case 4596: // 影子下坠 / Shadowfall
			return cooldown - 6 * SkillLevel;
		case 600: // 魔法防御 / Magical Defense
		case 641: // Unraveling Assault
		case 642: // Unraveling Assault
		case 643: // Unraveling Assault
		case 1351: // 岩石召唤 / Summon Rock
		case 1352: // 岩石召唤 / Summon Rock
		case 1353: // 岩石召唤 / Summon Rock
		case 1354: // 岩石召唤 / Summon Rock
		case 1355: // 岩石召唤 / Summon Rock
		case 1356: // 岩石召唤 / Summon Rock
		case 1801: // 疾行激励 / Acceleration Cheer
		case 1802: // 疾行激励 / Acceleration Cheer
		case 1803: // 疾行激励 / Acceleration Cheer
		case 1804: // 疾行激励 / Acceleration Cheer
		case 1805: // 疾行激励 / Acceleration Cheer
		case 1806: // 疾行激励 / Acceleration Cheer
		case 1807: // 疾行激励 / Acceleration Cheer
		case 1808: // 疾行激励 / Acceleration Cheer
		case 2750: // 功率最大化 / Aethercharged Steel
		case 2751: // 功率最大化 / Aethercharged Steel
		case 2752: // 功率最大化 / Aethercharged Steel
		case 2753: // 功率最大化 / Aethercharged Steel
		case 2754: // 功率最大化 / Aethercharged Steel
		case 2755: // 功率最大化 / Aethercharged Steel
		case 3590: // 精灵强化:治愈 I / Healing Spirit
		case 3903: // 全力疾行 I / Power Sprint
		case 4182: // 弱化之印 / Enfeebling Burst
		case 4183: // 弱化之印 / Enfeebling Burst
		case 4184: // 弱化之印 / Enfeebling Burst
		case 4185: // 弱化之印 / Enfeebling Burst
		case 4186: // 弱化之印 / Enfeebling Burst
		case 4187: // 弱化之印 / Enfeebling Burst
			return cooldown - 9 * SkillLevel;
		case 539: // 枯竭波 / Exhausting Wave
		case 540: // 枯竭波 / Exhausting Wave
		case 541: // 枯竭波 / Exhausting Wave
		case 542: // 枯竭波 / Exhausting Wave
		case 543: // 枯竭波 / Exhausting Wave
		case 544: // 枯竭波 / Exhausting Wave
		case 612: // 斩脚 / Tendon Slice
		case 613: // 斩脚 / Tendon Slice
		case 614: // 斩脚 / Tendon Slice
		case 615: // 斩脚 / Tendon Slice
		case 616: // 斩脚 / Tendon Slice
		case 617: // 斩脚 / Tendon Slice
		case 618: // 抓脚 I / Ankle Snare
		case 698: // 地震波动 / Earthquake Wave
		case 699: // 地震波动 / Earthquake Wave
		case 700: // 地震波动 / Earthquake Wave
		case 701: // 地震波动 / Earthquake Wave
		case 702: // 地震波动 / Earthquake Wave
		case 703: // 地震波动 / Earthquake Wave
		case 704: // 地震波动 / Earthquake Wave
		case 705: // 地震波动 / Earthquake Wave
		case 749: // 重生波 / Revival Wave
		case 750: // 重生波 / Revival Wave
		case 751: // 重生波 / Revival Wave
		case 752: // 重生波 / Revival Wave
		case 753: // 重生波 / Revival Wave
		case 754: // 重生波 / Revival Wave
		case 849: // 减速陷阱 / Trap Of Slowing
		case 850: // 减速陷阱 / Trap Of Slowing
		case 851: // 减速陷阱 / Trap Of Slowing
		case 852: // 减速陷阱 / Trap Of Slowing
		case 853: // 减速陷阱 / Trap Of Slowing
		case 854: // 减速陷阱 / Trap Of Slowing
		case 855: // 减速陷阱 / Trap Of Slowing
		case 856: // 减速陷阱 / Trap Of Slowing
		case 857: // 减速陷阱 / Trap Of Slowing
		case 858: // 减速陷阱 / Trap Of Slowing
		case 859: // 减速陷阱 / Trap Of Slowing
		case 860: // 减速陷阱 / Trap Of Slowing
		case 861: // 减速陷阱 / Trap Of Slowing
		case 862: // 减速陷阱 / Trap Of Slowing
		case 863: // 减速陷阱 / Trap Of Slowing
		case 864: // 减速陷阱 / Trap Of Slowing
		case 888: // 猎人的决心 I / Hunter's Might
		case 962: // 缚天陷阱 / Skybound Trap
		case 963: // 缚天陷阱 / Skybound Trap
		case 964: // 缚天陷阱 / Skybound Trap
		case 965: // 缚天陷阱 / Skybound Trap
		case 966: // 缚天陷阱 / Skybound Trap
		case 967: // 缚天陷阱 / Skybound Trap
		case 968: // 缚天陷阱 / Skybound Trap
		case 969: // 缚天陷阱 / Skybound Trap
		case 970: // 缚天陷阱 / Skybound Trap
		case 971: // 缚天陷阱 / Skybound Trap
		case 972: // 缚天陷阱 / Skybound Trap
		case 973: // 缚天陷阱 / Skybound Trap
		case 974: // 缚天陷阱 / Skybound Trap
		case 975: // 缚天陷阱 / Skybound Trap
		case 976: // 缚天陷阱 / Skybound Trap
		case 977: // 缚天陷阱 / Skybound Trap
		case 1006: // Ripthread Shot
		case 1007: // Ripthread Shot
		case 1008: // Ripthread Shot
		case 1486: // 暴风重击 / Storm Strike
		case 1487: // 暴风重击 / Storm Strike
		case 1488: // 暴风重击 / Storm Strike
		case 1489: // 暴风重击 / Storm Strike
		case 1490: // 暴风重击 / Storm Strike
		case 1491: // 暴风重击 / Storm Strike
		case 1492: // 暴风重击 / Storm Strike
		case 1493: // 暴风重击 / Storm Strike
		case 1901: // Resonant Strike
		case 1902: // Resonant Strike
		case 1903: // Resonant Strike
		case 2109: // 束缚炮 / Paralysis Cannon
		case 2110: // 束缚炮 / Paralysis Cannon
		case 2111: // 束缚炮 / Paralysis Cannon
		case 2112: // 束缚炮 / Paralysis Cannon
		case 2113: // 束缚炮 / Paralysis Cannon
		case 2114: // 束缚炮 / Paralysis Cannon
		case 2274: // 灵魂炮 / Missile Guide
		case 2277: // 灵魂炮 / Missile Guide
		case 2280: // 灵魂炮 / Missile Guide
		case 2283: // 灵魂炮 / Missile Guide
		case 2286: // 灵魂炮 / Missile Guide
		case 2289: // 灵魂炮 / Missile Guide
		case 2292: // 灵魂炮 / Missile Guide
		case 2295: // 灵魂炮 / Missile Guide
		case 2371: // Sequential Fire
		case 2374: // Sequential Fire
		case 2377: // Sequential Fire
		case 2380: // Pulverizer Cannon
		case 2381: // Pulverizer Cannon
		case 2382: // Pulverizer Cannon
		case 2450: // 魔力凝聚 / Life Support Trigger
		case 2451: // 魔力凝聚 / Life Support Trigger
		case 2452: // 魔力凝聚 / Life Support Trigger
		case 2453: // 魔力凝聚 / Life Support Trigger
		case 2454: // 魔力凝聚 / Life Support Trigger
		case 2455: // 魔力凝聚 / Life Support Trigger
		case 2456: // 魔力凝聚 / Life Support Trigger
		case 2457: // 魔力凝聚 / Life Support Trigger
		case 2939: // 精神破坏 / Divine Justice
		case 2940: // 精神破坏 / Divine Justice
		case 2941: // 精神破坏 / Divine Justice
		case 2942: // 精神破坏 / Divine Justice
		case 2943: // 精神破坏 / Divine Justice
		case 2944: // 精神破坏 / Divine Justice
		case 3239: // Fangdrop Stab
		case 3240: // Fangdrop Stab
		case 3241: // Fangdrop Stab
		case 3245: // Scoundrel's Bond
		case 3255: // 雾砂攻击 / Venomous Strike
		case 3256: // 雾砂攻击 / Venomous Strike
		case 3257: // 雾砂攻击 / Venomous Strike
		case 3258: // 雾砂攻击 / Venomous Strike
		case 3259: // 雾砂攻击 / Venomous Strike
		case 3260: // 雾砂攻击 / Venomous Strike
		case 3261: // 雾砂攻击 / Venomous Strike
		case 3327: // 逃跑姿态 I / Break Away
		case 3531: // 命令:守护之墙 I / Spirit Wall Of Protection
		case 3562: // 大地之守护 / Earthen Call
		case 3563: // 大地之守护 / Earthen Call
		case 3564: // 大地之守护 / Earthen Call
		case 3565: // 大地之守护 / Earthen Call
		case 3566: // 大地之守护 / Earthen Call
		case 3567: // 大地之守护 / Earthen Call
		case 3568: // 大地之守护 / Earthen Call
		case 3569: // 大地之守护 / Earthen Call
		case 3575: // 黑暗之诅咒 / Withering Gloom
		case 3576: // 黑暗之诅咒 / Withering Gloom
		case 3577: // 黑暗之诅咒 / Withering Gloom
		case 3578: // 黑暗之诅咒 / Withering Gloom
		case 3579: // 黑暗之诅咒 / Withering Gloom
		case 3580: // 黑暗之诅咒 / Withering Gloom
		case 3581: // 黑暗之诅咒 / Withering Gloom
		case 3924: // 拯救之手 / Saving Grace
		case 3925: // 拯救之手 / Saving Grace
		case 3926: // 拯救之手 / Saving Grace
		case 3927: // 拯救之手 / Saving Grace
		case 3928: // 拯救之手 / Saving Grace
		case 3929: // 拯救之手 / Saving Grace
		case 3930: // 拯救之手 / Saving Grace
		case 3931: // 拯救之手 / Saving Grace
		case 3992: // 净化之水 / Ripple Of Purification
		case 3993: // 净化之水 / Ripple Of Purification
		case 3994: // 净化之水 / Ripple Of Purification
		case 3995: // 净化之水 / Ripple Of Purification
		case 3996: // 净化之水 / Ripple Of Purification
		case 3997: // 净化之水 / Ripple Of Purification
		case 4135: // 闪光 I / Blinding Light
		case 4368: // 吸收之咒语 / Healing Conduit
		case 4490: // 麻痹回声 I / Staggered Rest
		case 4631: // 吸收之咒语 / Healing Conduit
		case 4632: // 吸收之咒语 / Healing Conduit
		case 4633: // 吸收之咒语 / Healing Conduit
		case 4634: // 吸收之咒语 / Healing Conduit
		case 4635: // 吸收之咒语 / Healing Conduit
		case 4636: // 吸收之咒语 / Healing Conduit
		case 4637: // 吸收之咒语 / Healing Conduit
		case 4638: // 吸收之咒语 / Healing Conduit
			return cooldown - 24 * SkillLevel;
		case 657: // Battle Banner
		case 658: // Battle Banner
		case 659: // Battle Banner
		case 660: // Battle Banner
		case 661: // Battle Banner
		case 662: // Battle Banner
		case 1009: // 抵抗的决心 I / Nature's Resolve
		case 1057: // 祝福之弓 I / Bow Of Blessing
		case 1305: // 冰雪甲胄 / Wintry Armor
		case 1306: // 冰雪甲胄 / Wintry Armor
		case 1307: // 冰雪甲胄 / Wintry Armor
		case 1308: // 冰面 / Ice Sheet
		case 1309: // 冰面 / Ice Sheet
		case 1310: // 冰面 / Ice Sheet
		case 1311: // 冰面 / Ice Sheet
		case 1312: // 冰面 / Ice Sheet
		case 1313: // 冰面 / Ice Sheet
		case 1314: // 冰面 / Ice Sheet
		case 1315: // 冰面 / Ice Sheet
		case 1316: // 冰面 / Ice Sheet
		case 1317: // 冰面 / Ice Sheet
		case 1318: // 冰面 / Ice Sheet
		case 1319: // 冰面 / Ice Sheet
		case 1320: // 冰面 / Ice Sheet
		case 1321: // 冰面 / Ice Sheet
		case 1322: // 冰面 / Ice Sheet
		case 1323: // 冰面 / Ice Sheet
		case 1339: // 睡眠暴风 I / Sleeping Storm
		case 1402: // 元素结界 I / Elemental Ward
		case 1460: // 召唤台风 / Manifest Tornado
		case 1461: // 召唤台风 / Manifest Tornado
		case 1462: // 召唤台风 / Manifest Tornado
		case 1463: // 召唤台风 / Manifest Tornado
		case 1464: // 召唤台风 / Manifest Tornado
		case 1465: // 召唤台风 / Manifest Tornado
		case 1466: // 召唤台风 / Manifest Tornado
		case 1467: // 召唤台风 / Manifest Tornado
		case 1468: // 召唤台风 / Manifest Tornado
		case 1469: // 召唤台风 / Manifest Tornado
		case 1470: // 召唤台风 / Manifest Tornado
		case 1471: // 召唤台风 / Manifest Tornado
		case 1472: // 召唤台风 / Manifest Tornado
		case 1473: // 召唤台风 / Manifest Tornado
		case 1540: // Aetherblaze
		case 1541: // Aetherblaze
		case 1542: // Aetherblaze
		case 1550: // 幻影漩涡 / Illusion Storm
		case 1551: // 幻影漩涡 / Illusion Storm
		case 1552: // 幻影漩涡 / Illusion Storm
		case 1553: // 幻影漩涡 / Illusion Storm
		case 1554: // 幻影漩涡 / Illusion Storm
		case 1555: // 幻影漩涡 / Illusion Storm
		case 1607: // 气概 / Rise
		case 1608: // 气概 / Rise
		case 1609: // 气概 / Rise
		case 1610: // 气概 / Rise
		case 1611: // 气概 / Rise
		case 1612: // 气概 / Rise
		case 1613: // 气概 / Rise
		case 1651: // 风之祝福 / Blessing Of Wind
		case 1652: // 风之祝福 / Blessing Of Wind
		case 1653: // 风之祝福 / Blessing Of Wind
		case 1654: // 风之祝福 / Blessing Of Wind
		case 1655: // 风之祝福 / Blessing Of Wind
		case 1656: // 风之祝福 / Blessing Of Wind
		case 1832: // 铁壁之咒语 / Elemental Screen
		case 1833: // 铁壁之咒语 / Elemental Screen
		case 1834: // 铁壁之咒语 / Elemental Screen
		case 1904: // Debilitating Incantation
		case 1905: // Debilitating Incantation
		case 1906: // Debilitating Incantation
		case 2033: // 魔力之息 / Nature's Favor
		case 2034: // 魔力之息 / Nature's Favor
		case 2035: // 魔力之息 / Nature's Favor
		case 2036: // 魔力之息 / Nature's Favor
		case 2037: // 魔力之息 / Nature's Favor
		case 2038: // 魔力之息 / Nature's Favor
		case 2039: // 魔力之息 / Nature's Favor
		case 2040: // 魔力之息 / Nature's Favor
		case 2368: // Pursuit Stance
		case 2369: // Pursuit Stance
		case 2370: // Pursuit Stance
		case 2383: // 灵敏度提升 / Aimbot Assist
		case 2384: // 灵敏度提升 / Aimbot Assist
		case 2385: // 灵敏度提升 / Aimbot Assist
		case 2386: // 灵敏度提升 / Aimbot Assist
		case 2387: // 灵敏度提升 / Aimbot Assist
		case 2388: // 灵敏度提升 / Aimbot Assist
		case 2389: // 灵敏度提升 / Aimbot Assist
		case 2390: // 灵敏度提升 / Aimbot Assist
		case 2458: // 魔力屏障 / Trauma Plate Trigger
		case 2459: // 魔力屏障 / Trauma Plate Trigger
		case 2460: // 魔力屏障 / Trauma Plate Trigger
		case 2461: // 魔力屏障 / Trauma Plate Trigger
		case 2462: // 魔力屏障 / Trauma Plate Trigger
		case 2463: // 魔力屏障 / Trauma Plate Trigger
		case 2825: // 吸收反射膜 / Leeching Steel
		case 2826: // 吸收反射膜 / Leeching Steel
		case 2827: // 吸收反射膜 / Leeching Steel
		case 2828: // 吸收反射膜 / Leeching Steel
		case 2829: // 吸收反射膜 / Leeching Steel
		case 2830: // 吸收反射膜 / Leeching Steel
		case 2831: // 吸收反射膜 / Leeching Steel
		case 2832: // 吸收反射膜 / Leeching Steel
		case 2849: // Nerve Pulse
		case 2850: // Nerve Pulse
		case 2851: // Nerve Pulse
		case 2852: // Explosive Exhaust
		case 2854: // Explosive Exhaust
		case 2858: // Explosive Exhaust
		case 2861: // Powerspike Trigger
		case 2862: // Powerspike Trigger
		case 2863: // Powerspike Trigger
		case 2915: // Eternal Denial
		case 2916: // Eternal Denial
		case 2917: // Eternal Denial
		case 2918: // Shield of Vengeance
		case 2934: // 阻断之甲 / Aether Armor
		case 2935: // 阻断之甲 / Aether Armor
		case 2936: // 阻断之甲 / Aether Armor
		case 2937: // 阻断之甲 / Aether Armor
		case 2938: // 阻断之甲 / Aether Armor
		case 2968: // 束缚波 / Punishing Wave
		case 2969: // 束缚波 / Punishing Wave
		case 2970: // 束缚波 / Punishing Wave
		case 2971: // 束缚波 / Punishing Wave
		case 2972: // 束缚波 / Punishing Wave
		case 2973: // 束缚波 / Punishing Wave
		case 2974: // 坚固的盾牌 I / Shield Of Faith
		case 3035: // 激昂 I / Divine Fury
		case 3155: // 起死回生 / Prayer Of Resilience
		case 3156: // 起死回生 / Prayer Of Resilience
		case 3157: // 起死回生 / Prayer Of Resilience
		case 3158: // 起死回生 / Prayer Of Resilience
		case 3159: // 起死回生 / Prayer Of Resilience
		case 3160: // 起死回生 / Prayer Of Resilience
		case 3236: // Shimmerbomb
		case 3237: // Shimmerbomb
		case 3238: // Shimmerbomb
		case 3319: // 六感最大化 / Sensory Boost
		case 3321: // 涂毒 / Apply Lethal Venom
		case 3322: // 涂毒 / Apply Lethal Venom
		case 3323: // 涂毒 / Apply Lethal Venom
		case 3324: // 涂毒 / Apply Lethal Venom
		case 3325: // 涂毒 / Apply Lethal Venom
		case 3326: // 涂毒 / Apply Lethal Venom
		case 3329: // 影子步行 I / Shadow Walk
		case 3332: // 奇袭斩 / Dash And Slash
		case 3333: // 奇袭斩 / Dash And Slash
		case 3334: // 奇袭斩 / Dash And Slash
		case 3335: // 奇袭斩 / Dash And Slash
		case 3336: // 奇袭斩 / Dash And Slash
		case 3337: // 奇袭斩 / Dash And Slash
		case 3480: // 命中之契约 I / Oath Of Accuracy
		case 3541: // Spirit's Empowerment
		case 3542: // Spirit's Empowerment
		case 3543: // Spirit's Empowerment
		case 3544: // 隐身之光辉 I / Cloaking Word
		case 3545: // 黄泉之诅咒 / Infernal Blight
		case 3546: // 黄泉之诅咒 / Infernal Blight
		case 3547: // 黄泉之诅咒 / Infernal Blight
		case 3836: // 命令:毁灭 I / Spirit Burn-to-Ashes
		case 3849: // Blood Funnel
		case 3850: // Blood Funnel
		case 3851: // Blood Funnel
		case 3932: // Restoration Relief
		case 3933: // Restoration Relief
		case 3934: // Restoration Relief
		case 4144: // 痛苦连锁 / Chain Of Suffering
		case 4145: // 痛苦连锁 / Chain Of Suffering
		case 4146: // 痛苦连锁 / Chain Of Suffering
		case 4147: // 痛苦连锁 / Chain Of Suffering
		case 4148: // 痛苦连锁 / Chain Of Suffering
		case 4149: // 痛苦连锁 / Chain Of Suffering
		case 4188: // 恢复之佑护I / Noble Grace
		case 4189: // 恢复之佑护I / Noble Grace
		case 4190: // 恢复之佑护I / Noble Grace
		case 4191: // 恢复之佑护I / Noble Grace
		case 4192: // 恢复之佑护I / Noble Grace
		case 4614: // 六感最大化 / Sensory Boost
			return cooldown - 36 * SkillLevel;
		case 683: // 威胁的咆哮 / Howl
		case 684: // 威胁的咆哮 / Howl
		case 685: // 威胁的咆哮 / Howl
		case 686: // 威胁的咆哮 / Howl
		case 687: // 威胁的咆哮 / Howl
		case 688: // 威胁的咆哮 / Howl
		case 689: // 威胁的咆哮 / Howl
		case 690: // 威胁的咆哮 / Howl
		case 936: // Night Haze
		case 937: // Night Haze
		case 938: // Night Haze
		case 1060: // 闪电陷阱 / Staggering Trap
		case 1061: // 闪电陷阱 / Staggering Trap
		case 1062: // 闪电陷阱 / Staggering Trap
		case 1063: // 闪电陷阱 / Staggering Trap
		case 1064: // 闪电陷阱 / Staggering Trap
		case 1065: // 闪电陷阱 / Staggering Trap
		case 1327: // 活力交换 I / Exchange Vitality
		case 1329: // 衰弱之诅咒 / Curse Of Weakness
		case 1330: // 衰弱之诅咒 / Curse Of Weakness
		case 1331: // 衰弱之诅咒 / Curse Of Weakness
		case 1332: // 衰弱之诅咒 / Curse Of Weakness
		case 1333: // 衰弱之诅咒 / Curse Of Weakness
		case 1334: // 衰弱之诅咒 / Curse Of Weakness
		case 1335: // 衰弱之诅咒 / Curse Of Weakness
		case 1336: // 衰弱之诅咒 / Curse Of Weakness
		case 1340: // Slumberswept Wind
		case 1341: // Slumberswept Wind
		case 1342: // Slumberswept Wind
		case 1418: // Repulsion Field
		case 1419: // Repulsion Field
		case 1420: // Repulsion Field
		case 2579: // 伊德保护膜 / Kinetic Bulwark
		case 2580: // 伊德保护膜 / Kinetic Bulwark
		case 2581: // 伊德保护膜 / Kinetic Bulwark
		case 2926: // 庇护之盔甲 / Prayer of Victory
		case 2927: // 庇护之盔甲 / Prayer of Victory
		case 2928: // 庇护之盔甲 / Prayer of Victory
		case 2929: // 庇护之盔甲 / Prayer of Victory
		case 2930: // 庇护之盔甲 / Prayer of Victory
		case 2931: // 庇护之盔甲 / Prayer of Victory
		case 3320: // 觉悟 I / Deadly Abandon
		case 3549: // Command: Absorb Wounds
		case 3906: // Summon Vexing Energy
		case 3907: // Summon Vexing Energy
		case 3908: // Summon Vexing Energy
		case 3909: // Summon Vexing Energy
		case 3910: // Summon Vexing Energy
		case 3911: // Summon Vexing Energy
			return cooldown - 60 * SkillLevel;
		case 2922: // 主神的保护 I / Empyrean Providence
		case 3904: // 必死的交换 I / Reverse Condition
			return cooldown - 120 * SkillLevel;
		// 高阶守护者变身 5.1【天族】 / ArchDaeva Transformation 5.1 [Elyos]
		case 4752: // Transformation: Avatar Of Fire.
		case 4757: // Transformation: Avatar Of Water.
		case 4762: // Transformation: Avatar Of Earth.
		case 4768: // Transformation: Avatar Of Wind.
			// 高阶守护者变身 5.1【魔族】 / ArchDaeva Transformation 5.1 [Asmodians]
		case 4804: // Transformation: Avatar Of Fire.
		case 4805: // Transformation: Avatar Of Water.
		case 4806: // Transformation: Avatar Of Earth.
		case 4807: // Transformation: Avatar Of Wind.
			return cooldown - 2500 * SkillLevel;
		}
		return cooldown;
	}

	protected void calculateSkillDuration() {
		// 不受施法速度提升影响的技能 / Skills that are not affected by boost casting time
		duration = 0;
		if (isCastTimeFixed()) {
			duration = skillTemplate.getDuration();
			return;
		}
		duration = effector.getGameStats().getPositiveReverseStat(StatEnum.BOOST_CASTING_TIME, skillTemplate.getDuration());
		SkillSubType subType = skillTemplate.getSubType();
		if (subType == SkillSubType.SUMMON) {
			duration = effector.getGameStats().getPositiveReverseStat(StatEnum.BOOST_CASTING_TIME_SUMMON, duration);
		} else if (subType == SkillSubType.SUMMONHOMING) {
			duration = effector.getGameStats().getPositiveReverseStat(StatEnum.BOOST_CASTING_TIME_SUMMONHOMING, duration);
		} else if (subType == SkillSubType.SUMMONTRAP) {
			duration = effector.getGameStats().getPositiveReverseStat(StatEnum.BOOST_CASTING_TIME_TRAP, duration);
		} else if (subType == SkillSubType.HEAL) {
			duration = effector.getGameStats().getPositiveReverseStat(StatEnum.BOOST_CASTING_TIME_HEAL, duration);
		} else if (subType == SkillSubType.ATTACK) {
			if (skillTemplate.getType() == SkillType.MAGICAL) {
				duration = effector.getGameStats().getPositiveReverseStat(StatEnum.BOOST_CASTING_TIME_ATTACK, duration);
			}
		}

		// 基础技能持续时间上限的 70% / 70% of base skill duration cap
		// 技能召唤迅捷 I（3779）与灵巧手指 I（913）无施法速度上限。 / No cast speed cap for skill Summoning Alacrity I(skillId: 3779) and Nimble Fingers I(skillId: 913)
		if (!effector.getEffectController().hasAbnormalEffect(3779) && !effector.getEffectController().hasAbnormalEffect(913)) { // 4.8
			int baseDurationCap = Math.round(skillTemplate.getDuration() * 0.3f);
			if (duration < baseDurationCap) {
				duration = baseDurationCap;
			}
		}

		if (effector instanceof Player) {
			if (this.isMulticast() && ((Player) effector).getChainSkills().getChainCount((Player) effector, this.getSkillTemplate(), this.chainCategory) != 0) {
				duration = 0;
			}
		}

		if (duration < 0) {
			duration = 0;
		}
	}

	/**
	 * 充能阶段的时间缩放系数，与客户端蓄力条同源。
	 * Charge-stage time factor, shared with the client charge gauge.
	 *
	 * <p>客户端按 {@code SM_CASTSPELL} 下发的（已受速度修正的）施法时长缩放蓄力条，物理充能则按攻速比例缩放；
	 * 因此阶段窗口与最小充能必须按同一比例缩放。旧实现只取该比例的一半（{@code 1 - (1 - ratio) * 0.5}），
	 * 使服务器阶段窗口恒长于客户端，表现为客户端已进入第三阶段、服务器仍按前两阶段结算伤害。</p>
	 *
	 * <p>The client scales its gauge by the speed-corrected cast time sent in {@code SM_CASTSPELL}, and physical
	 * charge skills by the attack-delay ratio, so stage windows must use that same ratio. The previous
	 * half-weighted blend ({@code 1 - (1 - ratio) * 0.5}) kept the server's windows longer than the client's, so a
	 * release the client showed as stage three could still be resolved as stage one or two.</p>
	 *
	 * @return 缩放系数 / scaling factor
	 */
	private float calculateChargeTimeMultiplier() {
		BonusChargeType bonusType = chargeTemplate.getBonusChargeType();
		float speedRatio;
		if (bonusType == BonusChargeType.PHYSICAL) {
			Stat2 attackSpeed = effector.getGameStats().getAttackSpeed();
			speedRatio = attackSpeed.getBase() == 0 ? 1 : attackSpeed.getCurrent() / (float) attackSpeed.getBase();
		} else if (bonusType == BonusChargeType.MAGICAL) {
			speedRatio = skillTemplate.getDuration() == 0 ? 1 : duration / (float) skillTemplate.getDuration();
		} else {
			return 1;
		}
		return Math.max(speedRatio, MIN_CHARGE_SPEED_RATIO);
	}

	private int scaleChargeTime(int time) {
		return (int) (time * chargeTimeMultiplier);
	}

	private boolean checkAnimationTime() {
		if (!(effector instanceof Player player) || skillMethod != SkillMethod.CAST) {
			return true;
		}

		// 玩家无武器时不检查动画时间 / if player is without weapon, dont check animation time
		if (player.getEquipment().getMainHandWeaponType() == null) {
			return true;
		}

		/**
		 * 部分技能例外：药草/法力治疗、陷阱。
		 * Exceptions for certain skills - herb and mana treatment - traps
		 */
		// 不检查草药、法力治疗与专注增强 / dont check herb , mana treatment and concentration enhancement
		switch (this.getSkillId()) { // 4.8
		case 245: // 绷带治疗 / Bandage Heal
		case 246: // Herb Treatment I
		case 247: // Herb Treatment II
		case 251: // Herb Treatment III
		case 253: // Herb Treatment IV
		case 297: // Herb Treatment V
		case 308: // Herb Treatment VI
		case 309: // Herb Treatment VII
		case 310: // Herb Treatment VIII
		case 311: // Herb Treatment IX
		case 312: // Herb Treatment X
		case 313: // Herb Treatment XI
		case 314: // Herb Treatment XII
		case 249: // Mana Treatment I
		case 250: // Mana Treatment II
		case 252: // Mana Treatment III
		case 254: // Mana Treatment IV
		case 298: // Mana Treatment V
		case 315: // Mana Treatment VI
		case 316: // Mana Treatment VII
		case 317: // Mana Treatment VIII
		case 318: // Mana Treatment IX
		case 319: // Mana Treatment X
		case 320: // Mana Treatment XI
		case 321: // Mana Treatment XII
		case 3889: // Prayer Of Focus I
		case 3890: // Prayer Of Focus II
		case 3891: // Prayer Of Focus III
		case 3892: // Prayer Of Focus IV
		case 3893: // Prayer Of Focus V
		case 3894: // Prayer Of Focus VI
		case 4783: // [ArchDaeva] Prayer Of Focus 5.1
		case 11580: // Stigma Prayer Of Focus I
			return true;
		}

		if (this.getSkillTemplate().getSubType() == SkillSubType.SUMMONTRAP) {
			return true;
		}

		Motion motion = this.getSkillTemplate().getMotion();

		if (motion == null || motion.getName() == null) {
			return true;
		}

		if (motion.getInstantSkill() && hitTime != 0) {
			return false;
		} else if (!motion.getInstantSkill() && hitTime == 0) {
			return false;
		}

		MotionTime motionTime = DataManager.MOTION_DATA.getMotionTime(motion.getName());

		if (motionTime == null) {
			return true;
		}

		WeaponTypeWrapper weapons = new WeaponTypeWrapper(player.getEquipment().getMainHandWeaponType(), player.getEquipment().getOffHandWeaponType());
		float serverTime = motionTime.getTimeForWeapon(player.getRace(), player.getGender(), weapons);
		int clientTime = hitTime;

		if (serverTime == 0) {
			return true;
		}

		// 按弹药时间调整客户端时间 / adjust client time with ammotime
		double distance = MathUtil.getDistance(effector, firstTarget);
		int ammoTime = calculateAmmoTime(distance, getSkillTemplate().getAmmoSpeed());
		clientTime -= ammoTime;

		// 按动作播放速度调整服务器时间 / adjust servertime with motion play speed
		if (motion.getSpeed() != 100) {
			serverTime /= 100f;
			serverTime *= (float) motion.getSpeed();
		}

		Stat2 attackSpeed = player.getGameStats().getAttackSpeed();

		// 按攻击速度调整服务器时间 / adjust serverTime with attackSpeed
		if (attackSpeed.getBase() != attackSpeed.getCurrent()) {
			serverTime *= ((float) attackSpeed.getCurrent() / (float) attackSpeed.getBase());
		}

		// 容差 / tolerance
		if (duration == 0) {
			serverTime *= 0.9f;
		} else {
			serverTime *= 0.5f;
		}

		int finalTime = Math.round(serverTime);
		if (motion.getInstantSkill() && hitTime == 0) {
			this.serverTime = ammoTime;
		} else {
			if (clientTime < finalTime) {
				// 检查无动画作弊 / check for no animation Hacks
				if (SecurityConfig.NO_ANIMATION) {
					float clientTme = clientTime;
					float serverTme = serverTime;
					float checkTme = clientTme / serverTme;
					// 检查数值是否过低 / check if values are too low
					if (clientTime < 0 || checkTme < SecurityConfig.NO_ANIMATION_VALUE) {
						if (SecurityConfig.NO_ANIMATION_KICK) {
							player.getClientConnection().close(new SM_QUIT_RESPONSE(), false);
							AuditLogger.info(player, "Modified client_skills:" + this.getSkillId() + " (clientTime<finalTime:" + clientTime + "/" + finalTime + ") Kicking Player: " + player.getName());
						} else {
							AuditLogger.info(player, "Modified client_skills:" + this.getSkillId() + " (clientTime<finalTime:" + clientTime + "/" + finalTime + ")");
						}
						return false;
					}
				}
				log.warn(I18n.get("log.eeb3651188c5", this.getSkillId(), clientTime, finalTime, player.getName()));
			}
			this.serverTime = hitTime;
		}
		player.setNextSkillUse(System.currentTimeMillis() + duration + finalTime);
		return true;
	}

	static int calculateAmmoTime(double distance, int ammoSpeed) {
		return ammoSpeed > 0 ? (int) Math.round(distance / ammoSpeed * 1000) : 0;
	}

	/**
	 * 惩罚成功技能 / Penalty success skill
	 */
	private void startPenaltySkill() {
		int penaltySkill = skillTemplate.getPenaltySkillId();
		if (penaltySkill == 0) {
			return;
		}
		GameEngineServices.skillEngine().applyEffectDirectly(penaltySkill, firstTarget, effector, 0);
		if (skillTemplate.isPenaltySkillMessage() && effector instanceof Player player) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_PENALTY_TITLE);
		}
	}

	/**
	 * 外观技能 / Skin Skill
	 */
	private void getSkillSkinData() {
		if (effector instanceof Player && ((Player) effector).getSkillSkinList() != null) {
			for (SkillSkin skillSkin : ((Player) effector).getSkillSkinList().getSkillSkins()) {
				if (skillSkin.getTemplate() != null) {
					if (skillSkin.getTemplate().getSkillGroup().equalsIgnoreCase(skillTemplate.getSkillGroup()) && skillSkin.getIsActive() == 1) {
						skillskinHitTIme = skillSkin.getTemplate().getAmmoSpeed();
						skillskinId = skillSkin.getId();
						break;
					}
				}
			}
		}
	}

	/**
	 * 开始施放技能 / Start casting of skill
	 */
	private void startCast() {
		int targetObjId = firstTarget != null ? firstTarget.getObjectId() : 0;

		if (skillMethod == SkillMethod.CAST) {
			switch (targetType) {
			case 0: // PlayerObjectId as Target
			case 4:
			case 87:
				PacketSendUtility.broadcastPacketAndReceive(effector, new SM_CASTSPELL(effector.getObjectId(), skillTemplate.getSkillId(), skillLevel, targetType, targetObjId, this.duration, skillskinId));
				if (effector instanceof Npc && firstTarget instanceof Player) {
					NpcAI2 ai = (NpcAI2) effector.getAi2();
					if (ai.poll(AIQuestion.CAN_SHOUT)) {
						ShoutEventHandler.onCast(ai, firstTarget);
					}
				}
				break;

			case 3: // Target not in sight?
				PacketSendUtility.broadcastPacketAndReceive(effector, new SM_CASTSPELL(effector.getObjectId(), skillTemplate.getSkillId(), skillLevel, targetType, 0, this.duration, skillskinId));
				break;

			case 1: // XYZ as Target
				PacketSendUtility.broadcastPacketAndReceive(effector, new SM_CASTSPELL(effector.getObjectId(), skillTemplate.getSkillId(), skillLevel, targetType, x, y, z, this.duration, skillskinId));
				break;
			}
		} else if (skillMethod == SkillMethod.ITEM && duration > 0) {
			PacketSendUtility.broadcastPacketAndReceive(effector, new SM_ITEM_USAGE_ANIMATION(effector.getObjectId(), firstTarget.getObjectId(), (this.itemObjectId == 0 ? 0 : this.itemObjectId), itemTemplate.getTemplateId(), this.duration, 0, 0));
		}
	}
	/**
	 * 取消施法。
	 * Cancels casting.
	 */
	public void cancelCast() {
		tryCancelCast();
	}

	/**
	 * 原子尝试取得施法取消权。
	 * Atomically attempts to acquire cancellation ownership for this cast.
	 *
	 * @return 是否成功取得取消权 / whether cancellation ownership was acquired
	 */
	public boolean tryCancelCast() {
		if (!castState.compareAndSet(CastState.CASTING, CastState.CANCELLED)) {
			return false;
		}
		if (castingTask != null) {
			castingTask.cancel(true);
			castingTask = null;
		}
		return true;
	}

	/**
	 * 应用模板中指定的效果并执行动作。
	 * Apply effects and perform actions specified in skill template.
	 */
	private void endCast() {
		if (castState.get() != CastState.CASTING || effector.getCastingSkill() != this) {
			return;
		}

		// 充能技能 4.3 / charge skill 4.3
		if (chargeTemplate != null) {
			int time = (int) (System.currentTimeMillis() - castStart);
			time += 100; // 100ms leeway

			if (time < scaleChargeTime(chargeTemplate.getMinCharge())) {
				return;
			}

			int skillId = skillTemplate.getSkillId();
			for (ChargeTemplate charge : chargeTemplate.getCharges()) {
				time -= scaleChargeTime(charge.getTime());
				skillId = charge.getSkillId();

				if (time < 0) {
					break;
				}
			}
			skillTemplate = DataManager.SKILL_DATA.getSkillTemplate(skillId);

			if (skillTemplate.getCooldown() != 0) {
				effector.setSkillCoolDown(skillTemplate.getDelayId(), calculateCooldown(skillTemplate.getCooldown()) * 100L + System.currentTimeMillis());
			}
		}

		// 若目标超出范围 / if target out of range
		if (skillTemplate == null) {
			return;
		}

		// 检查目标是否超出技能范围 / Check if target is out of skill range
		Properties properties = skillTemplate.getProperties();
		if (properties != null && !(preselectedTargets == null
				? properties.endCastValidate(this) : properties.validatePreselectedTargets(this))) {
			effector.getController().cancelCurrentSkill(this);
			return;
		}

		if (!validateEffectedList()) {
			effector.getController().cancelCurrentSkill(this);
			return;
		}

		if (!preUsageCheck()) {
			effector.getController().cancelCurrentSkill(this);
			return;
		}
		synchronized (effector) {
			if (effector.getCastingSkill() != this
					|| !castState.compareAndSet(CastState.CASTING, CastState.COMPLETING)) {
				return;
			}
		}
		for (Creature effected : effectedList) {
			if (effected instanceof Npc npc) {
				npc.getAi2().onCasted(effector, skillTemplate.getSkillId(), skillLevel);
			}
		}
		if (skillMethod == SkillMethod.CAST && effector instanceof Player
				&& !GameEventBootstrapServices.minionService().consumeMinionSkillPoints((Player) effector, skillTemplate.getSkillId())) {
			effector.clearCasting(this);
			return;
		}
		effector.clearCasting(this);

		if (this.getSkillTemplate().isBroadcastUseMessage() && effector instanceof Player) {
			AbyssService.rankerSkillAnnounce((Player) effector, this.getSkillTemplate().getNameId());
		}

		/**
		 * 尝试移除物品；若不可行则返回以防止利用。
	 * Try removing item; if not possible return to prevent exploits
		 */
		if (effector instanceof Player && skillMethod == SkillMethod.ITEM) {
			Item item = ((Player) effector).getInventory().getItemByObjId(this.itemObjectId);
			if (item == null)
				return;
			if (item.getActivationCount() > 1) {
				item.setActivationCount(item.getActivationCount() - 1);
			} else {
				if (!((Player) effector).getInventory().decreaseByObjectId(item.getObjectId(), 1, ItemUpdateType.DEC_ITEM_USE)) {
					return;
				}
			}
		}
		/**
		 * 创建效果并预计算结果。
	 * Create effects and precalculate result
		 */

		int spellStatus = 0;
		int dashStatus = 0;
		int resistCount = 0;
		boolean blockedChain = false;
		boolean blockedStance = false;
		final List<Effect> effects = new ArrayList<Effect>();
		if (skillTemplate.getEffects() != null) {
			boolean blockAOESpread = false;
			for (Creature effected : effectedList) {
				Effect effect = new Effect(this, effected, 0, itemTemplate);
				if (effected instanceof Player) {
					if (effect.getEffectResult() == EffectResult.CONFLICT) {
						blockedStance = true;
					}
				}
				// 若须阻止 AOE 法术扩散则强制 RESIST 状态 / Force RESIST status if AOE spell spread must be blocked
				if (blockAOESpread) {
					effect.setAttackStatus(AttackStatus.RESIST);
				}
				effect.initialize();
				final int worldId = effector.getWorldId();
				final int instanceId = effector.getInstanceId();
				effect.setWorldPosition(worldId, instanceId, x, y, z);

				effects.add(effect);
				spellStatus = effect.getSpellStatus().getId();
				dashStatus = effect.getDashStatus().getId();

				// 若首目标抵抗法术则阻止 AOE 传播 / Block AOE propagation if firstTarget resists the spell
				if ((!blockAOESpread) && (effect.getAttackStatus() == AttackStatus.RESIST) && (isTargetAOE())) {
					blockAOESpread = true;
				}

					if (effect.getAttackStatus() == AttackStatus.RESIST || effect.getAttackStatus() == AttackStatus.DODGE) {
						resistCount++;
					}
				}
			if (!effects.isEmpty() && skillTemplate.getHostileType() != HostileType.NONE) {
					if (skillTemplate.getType() == SkillType.PHYSICAL) {
						effector.getObserveController().consumeAlwaysHit();
					} else if (skillTemplate.getType() == SkillType.MAGICAL) {
						effector.getObserveController().consumeAlwaysNoResist();
					}
				}

				if (!effectedList.isEmpty()) {
				if (resistCount == effectedList.size()) {
					blockedChain = true;
					blockedPenaltySkill = true;
				}
			}
			if (skillTemplate.isMaintain() && skillTemplate.getMaxMaintainCount() > 0) {
				List<Effect> successfulEffects = effects.stream()
						.filter(effect -> !effect.getSuccessEffect().isEmpty()).toList();
				effector.getEffectController().registerMaintainEffects(successfulEffects,
						skillTemplate.getMaxMaintainCount());
			}

			// 点对点技能例外（如冰面） / exception for point point skills(example Ice Sheet)
			if (effectedList.isEmpty() && this.isPointPointSkill()) {
				Effect effect = new Effect(this, null, 0, itemTemplate);
				effect.initialize();
				final int worldId = effector.getWorldId();
				final int instanceId = effector.getInstanceId();
				effect.setWorldPosition(worldId, instanceId, x, y, z);
				effects.add(effect);
				spellStatus = effect.getSpellStatus().getId();
			}
		}

		if (effector instanceof Player playerEffector && skillMethod == SkillMethod.CAST) {
			if (playerEffector.getController().isUnderStance() && !skillTemplate.isStanceUsable()) {
				playerEffector.getController().stopStance();
			}
			if (skillTemplate.isStance() && !blockedStance) {
				playerEffector.getController().startStance(skillTemplate.getSkillId(), skillTemplate.getStanceType());
			}
		}

		// 检查连锁技能触发率 / Check Chain Skill Trigger Rate
		if (CustomConfig.SKILL_CHAIN_TRIGGERRATE) {
			int chainProb = skillTemplate.getChainSkillProb();
			if (this.chainCategory != null && !blockedChain) {
				this.chainSuccess = Rnd.get(90) < chainProb;
			}
		} else {
			this.chainSuccess = true;
		}

			/**
		 * 设置连锁条件检查所需变量。
		 * Set variables for chain condition check.
		 */
		if (effector instanceof Player && this.chainSuccess && this.chainCategory != null) {
			((Player) effector).getChainSkills().addChainSkill(this.chainCategory, this.isMulticast());
		}

		/**
	 * 执行必要动作（消耗 MP/DP、物品等）。 / Perform necessary actions (use mp,dp items etc)
	 */
		Actions skillActions = skillTemplate.getActions();
		if (skillActions != null) {
			for (Action action : skillActions.getActions()) {
				action.act(this);
			}
		}

		if (effector instanceof Player) {
			QuestEnv env = new QuestEnv(effector.getTarget(), (Player) effector, 0, 0);
			GameEngineServices.questEngine().onUseSkill(env, skillTemplate.getSkillId());
		}

		if ((skillMethod == SkillMethod.CAST || skillMethod == SkillMethod.ITEM) && getSkillTemplate().getSubType() != SkillSubType.HEAL && hitTime <= 0) {
			int ammoSpeed = skillskinHitTIme > 0 ? skillskinHitTIme : getSkillTemplate().getAmmoSpeed();
			hitTime = calculateAmmoTime(effector.getDistanceToTarget(), ammoSpeed);
		}

		if (hitTime == 0) {
			applyEffect(effects);
		}

		else {
			GameThreadPoolServices.threadPoolManager().schedule(() -> applyEffect(effects), hitTime);
		}
		if (skillMethod == SkillMethod.CAST || skillMethod == SkillMethod.ITEM) {
			sendCastspellEnd(spellStatus, dashStatus, effects);
		}

		if (effector instanceof Npc) {
			SkillAttackManager.afterUseSkill((NpcAI2) effector.getAi2());
		}
	}

	/**
	 * 向受影响对象广播效果并执行惩罚技能。
	 * Broadcasts effects to affected objects and runs the penalty skill.
	 *
	 * @param effects 效果列表 / effect list
	 */
	public void applyEffect(List<Effect> effects) {
		Creature eventTarget = null;
		/**
	 * Apply effects to effected objects
	 */
		for (Effect effect : effects) {
			effect.applyEffect();
			if (isFullyDodgedNpc(effect)) {
				Npc npc = (Npc) effect.getEffected();
				npc.getAggroList().addDamage(effector, 0, effect.getAttackStatus());
			}
			if (!effect.getSuccessEffect().isEmpty() && effect.getEffected() != null) {
				Creature effected = effect.getEffected();
				if (eventTarget == null) {
					eventTarget = effected;
				}
				if (effected instanceof Npc npc) {
					npc.getAi2().onSpelled(effector, skillTemplate.getSkillId(), skillLevel);
				}
				effected.getKnownList().doOnAllNpcs(observer -> {
					observer.getAi2().onSeeSkill(effector, effected, skillTemplate.getSkillId(), skillLevel);
					observer.getAi2().onFriendSpelled(effector, effected, skillTemplate.getSkillId(), skillLevel);
				});
			}
		}
		if (eventTarget != null) {
			Creature target = eventTarget;
			effector.getKnownList().doOnAllNpcs(observer ->
				observer.getAi2().onSeeSpell(effector, target, skillTemplate.getSkillId(), skillLevel));
		}

		/**
	 * 使用惩罚技能（当前 100% 成功）。 / Use penalty skill (now 100% success)
	 */
		if (!blockedPenaltySkill) {
			startPenaltySkill();
		}
	}

	static boolean isFullyDodgedNpc(Effect effect) {
		return effect.getSuccessEffect().isEmpty() && effect.getEffected() instanceof Npc
			&& AttackStatus.getBaseStatus(effect.getAttackStatus()) == AttackStatus.DODGE;
	}

	/**
	 * 广播施法结束结果包（按目标类型分发）。
	 * Broadcasts the cast-end result packet (dispatched by target type).
	 *
	 * @param spellStatus 法术状态码 / spell status code
	 * @param dashStatus 位移状态码 / dash status code
	 * @param effects 已计算的效果列表 / calculated effects
	 */
	private void sendCastspellEnd(int spellStatus, int dashStatus, List<Effect> effects) {
		getSkillSkinData();
		int resultHitTime = serverTime > 0 ? serverTime : hitTime;
		if (skillMethod == SkillMethod.CAST) {
			switch (targetType) {
			case 0: // PlayerObjectId as Target
				broadcastCastResult(new SM_CASTSPELL_RESULT(this, effects, resultHitTime, chainSuccess, spellStatus,
					dashStatus, skillskinId));
				break;

			case 4:
			case 87:
				PacketSendUtility.broadcastPacketAndReceive(effector, new SM_CASTSPELL_RESULT(this, effects, resultHitTime, chainSuccess, spellStatus, dashStatus, targetType, skillskinId));
				break;

			case 3: // Target not in sight?
				PacketSendUtility.broadcastPacketAndReceive(effector, new SM_CASTSPELL_RESULT(this, effects, resultHitTime, chainSuccess, spellStatus, dashStatus, skillskinId));
				break;

			case 1: // XYZ as Target
				PacketSendUtility.broadcastPacketAndReceive(effector, new SM_CASTSPELL_RESULT(this, effects, resultHitTime, chainSuccess, spellStatus, dashStatus, targetType, skillskinId));
				break;
			}
		} else if (skillMethod == SkillMethod.ITEM) {
			PacketSendUtility.broadcastPacketAndReceive(effector, new SM_ITEM_USAGE_ANIMATION(effector.getObjectId(), firstTarget.getObjectId(), (this.itemObjectId == 0 ? 0 : this.itemObjectId), itemTemplate.getTemplateId(), 0, 1, 0));
			if (effector instanceof Player) {
				PacketSendUtility.sendPacket((Player) effector, SM_SYSTEM_MESSAGE.STR_USE_ITEM(new DescriptionId(getItemTemplate().getNameId())));
			}
		}
	}

	private void broadcastCastResult(SM_CASTSPELL_RESULT packet) {
		if (preselectedTargets == null || !broadcastToPreselectedTargets) {
			PacketSendUtility.broadcastPacketAndReceive(effector, packet);
			return;
		}
		for (Creature target : preselectedTargets) {
			if (target instanceof Player player && player.isOnline()) {
				PacketSendUtility.sendPacket(player, packet);
			}
		}
	}

	/**
	 * 调度技能动作/效果（引导类技能）。
	 * Schedule actions/effects of skill (channeled skills).
	 *
	 * @param delay 延迟毫秒 / delay millis
	 */
	private void schedule(int delay) {
		castingTask = GameThreadPoolServices.threadPoolManager().schedule(this::endCast, delay);

		castStart = System.currentTimeMillis();
	}

	/**
	 * 施法前校验全部开始条件。
	 * Check all conditions before starting cast.
	 */
	private boolean preCastCheck() {
		Conditions skillConditions = skillTemplate.getStartconditions();
		return skillConditions == null || skillConditions.validate(this);
	}

	/**
	 * 使用前校验全部使用条件。
	 * Check all conditions before using skill.
	 */
	private boolean preUsageCheck() {
		Conditions skillConditions = skillTemplate.getUseconditions();
		return skillConditions == null || skillConditions.validate(this);
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
	 * 是否被动技能。
	 * Whether passive skill.
	 *
	 */
	public boolean isPassive() {
		return skillTemplate.getActivationAttribute() == ActivationAttribute.PASSIVE;
	}

	/**
	 * 是否非指向 AOE。
	 * Whether non-target AOE.
	 *
	 */
	public boolean checkNonTargetAOE() {
		return (firstTargetAttribute == FirstTargetAttribute.ME && targetRangeAttribute == TargetRangeAttribute.AREA);
	}
	/**
	 * 是否目标 AOE。
	 * Whether target AOE.
	 *
	 */
	public boolean isTargetAOE() {
		return (firstTargetAttribute == FirstTargetAttribute.TARGET && targetRangeAttribute == TargetRangeAttribute.AREA);
	}
	/**
	 * 是否自身增益。
	 * Whether self buff.
	 *
	 */
	public boolean isSelfBuff() {
		return (firstTargetAttribute == FirstTargetAttribute.ME && targetRangeAttribute == TargetRangeAttribute.ONLYONE && skillTemplate.getSubType() == SkillSubType.BUFF && !skillTemplate.isDeityAvatar());
	}
	/**
	 * 主目标是否自身。
	 * Whether first target is self.
	 *
	 */
	public boolean isFirstTargetSelf() {
		return (firstTargetAttribute == FirstTargetAttribute.ME);
	}
	/**
	 * 是否地点技能。
	 * Whether point skill.
	 *
	 */
	public boolean isPointSkill() {
		return (this.firstTargetAttribute == FirstTargetAttribute.POINT);
	}
	/**
	 * 设置目标类型与坐标。
	 * Sets target type and coordinates.
	 *
	 * @param x X
	 * @param y Y
	 * @param z Z
	 */
	public void setTargetType(int targetType, float x, float y, float z) {
		this.targetType = targetType;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	/**
	 * 设置目标位置与朝向。
	 * Sets target position and heading.
	 *
	 * @param x X
	 * @param y Y
	 * @param z Z
	 * @param h 朝向 / heading
	 */
	public void setTargetPosition(float x, float y, float z, byte h) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.h = h;
	}

	/**
	 * 4.8 Version
	 */
	private boolean isCastTimeFixed() {
		if (skillMethod != SkillMethod.CAST) {
			return true;
		}
		if (!skillTemplate.isApplyCastingTimeBonus()) {
			return true;
		}
		switch (this.getSkillId()) {
		case 17: // Sleep: Scarecrow
		case 18: // Sleep: Frightcorn
		case 19: // Fear: Porgus
		case 20: // 灵魂之呼喊：人参 / Fear: Ginseng
		case 243: // 回程 / Return
		case 245: // 绷带治疗 / Bandage Heal
		case 246: // Herb Treatment I
		case 247: // Herb Treatment II
		case 251: // Herb Treatment III
		case 253: // Herb Treatment IV
		case 297: // Herb Treatment V
		case 308: // Herb Treatment VI
		case 309: // Herb Treatment VII
		case 310: // Herb Treatment VIII
		case 311: // Herb Treatment IX
		case 312: // Herb Treatment X
		case 313: // Herb Treatment XI
		case 314: // Herb Treatment XII
		case 249: // Mana Treatment I
		case 250: // Mana Treatment II
		case 252: // Mana Treatment III
		case 254: // Mana Treatment IV
		case 298: // Mana Treatment V
		case 315: // Mana Treatment VI
		case 316: // Mana Treatment VII
		case 317: // Mana Treatment VIII
		case 318: // Mana Treatment IX
		case 319: // Mana Treatment X
		case 320: // Mana Treatment XI
		case 321: // Mana Treatment XII
		case 302: // 紧急返回 / Escape
		case 1337: // 睡眠 / Sleep
		case 1338: // 睡眠之云 I / Tranquilizing Cloud
		case 1339: // 睡眠暴风 I / Sleeping Storm.
		case 1416: // 诅咒:古树 I / Curse Of Old Roots
		case 1417: // 诅咒:树 I / Curse Of Roots
		case 3589: // 恐怖的呼喊 I / Fear Shriek
		case 3775: // 恐惧 / Fear
			// 高阶守护者变身 5.1【天族】 / ArchDaeva Transformation 5.1 [Elyos]
		case 4752: // Transformation: Avatar Of Fire.
		case 4757: // Transformation: Avatar Of Water.
		case 4762: // Transformation: Avatar Of Earth.
		case 4768: // Transformation: Avatar Of Wind.
			// 高阶守护者变身 5.1【魔族】 / ArchDaeva Transformation 5.1 [Asmodians]
		case 4804: // Transformation: Avatar Of Fire.
		case 4805: // Transformation: Avatar Of Water.
		case 4806: // Transformation: Avatar Of Earth.
		case 4807: // Transformation: Avatar Of Wind.
			// 遗忘裂隙 5.1 / Fissure Of Oblivion 5.1
		case 4808: // Transformation: Avatar Of Fire.
		case 4813: // Transformation: Avatar Of Water.
		case 4818: // Transformation: Avatar Of Earth.
		case 4824: // Transformation: Avatar Of Wind.
			// 天族【守护者将军】 / Elyos [Guardian General]
		case 11885: // 变身:守护神将 / Transformation: Guardian General I
		case 11886: // 变身:守护神将 / Transformation: Guardian General II
		case 11887: // 变身:守护神将 / Transformation: Guardian General III
		case 11888: // 变身:守护神将 / Transformation: Guardian General IV
		case 11889: // 变身:守护神将 / Transformation: Guardian General V
			// 魔族【守护者将军】 / Asmodians [Guardian General]
		case 11890: // 变身:守护神将 / Transformation: Guardian General I
		case 11891: // 变身:守护神将 / Transformation: Guardian General II
		case 11892: // 变身:守护神将 / Transformation: Guardian General III
		case 11893: // 变身:守护神将 / Transformation: Guardian General IV
		case 11894: // 变身:守护神将 / Transformation: Guardian General V
			return true;
		}
		return false;
	}

	/**
	 * 是否地面技能。
	 * Whether ground skill.
	 *
	 */
	public boolean isGroundSkill() {
		return skillTemplate.isGroundSkill();
	}

	/**
	 * 判断是否应影响该可见对象。
	 * Whether the visible object should be affected.
	 *
	 */
	public boolean shouldAffectTarget(VisibleObject object) {
		// 生物离地至少 2 米时无法施加地面技能。 / If creature is at least 2 meters above the terrain, ground skill cannot be applied
		if (GeoDataConfig.GEO_ENABLE) {
			if (isGroundSkill()) {
				if ((object.getZ() - GameWorldServices.geoService().getZ(object) > 1.0f) || (object.getZ() - GameWorldServices.geoService().getZ(object) < -2.0f)) {
					return false;
				}
			}
			return GameWorldServices.geoService().canSeeSkill(getFirstTarget(), object, skillTemplate.getObstacle());
		}
		return true;
	}

	/**
	 * 是否点对点技能。
	 * Whether point-to-point skill.
	 *
	 */
	public boolean isPointPointSkill() {
		return this.getSkillTemplate().getProperties().getFirstTarget() == FirstTargetAttribute.POINT && this.getSkillTemplate().getProperties().getTargetType() == TargetRangeAttribute.POINT;
	}

	/**
	 * 是否多重施放。
	 * Whether multicast.
	 *
	 */
	public boolean isMulticast() {
		return this.isMultiCast;
	}

	/**
	 * 停止充能。
	 * Stops charging.
	 *
	 */
	public void stopCharging() {
		if (chargeTemplate == null) {
			return;
		}
		if (castingTask != null) {
			castingTask.cancel(true);
			castingTask = null;
		}
		endCast();
	}
}

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
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.abyss.AbyssService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
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
		this.effectedList = new ArrayList<>();
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
		cooldown = SkillCooldownTables.StigmaEnchantCoolDown(this, cooldown);
		cooldown = skillTemplate.scaleCooldownByAttackDelay(cooldown,
				effector.getGameStats().getAttackSpeed().getCurrent());
		return SkillConfig.scaleCooldown(cooldown);
	}

	/**
	 * 按烙印强化查表缩减技能冷却。
	 * Reduces the skill cooldown via the stigma-enchant table.
	 *
	 * @param skill 技能 / skill
	 * @param cooldown 原始冷却毫秒 / raw cooldown in ms
	 * @return 缩减后的冷却毫秒 / reduced cooldown in ms
	 */
	public int StigmaEnchantCoolDown(Skill skill, int cooldown) {
		return SkillCooldownTables.StigmaEnchantCoolDown(skill, cooldown);
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
		final List<Effect> effects = new ArrayList<>();
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
		return switch (this.getSkillId()) { // Sleep: Scarecrow
			// Sleep: Frightcorn
			// Fear: Porgus
			// 灵魂之呼喊：人参 / Fear: Ginseng
			// 回程 / Return
			// 绷带治疗 / Bandage Heal
			// Herb Treatment I
			// Herb Treatment II
			// Herb Treatment III
			// Herb Treatment IV
			// Herb Treatment V
			// Herb Treatment VI
			// Herb Treatment VII
			// Herb Treatment VIII
			// Herb Treatment IX
			// Herb Treatment X
			// Herb Treatment XI
			// Herb Treatment XII
			// Mana Treatment I
			// Mana Treatment II
			// Mana Treatment III
			// Mana Treatment IV
			// Mana Treatment V
			// Mana Treatment VI
			// Mana Treatment VII
			// Mana Treatment VIII
			// Mana Treatment IX
			// Mana Treatment X
			// Mana Treatment XI
			// Mana Treatment XII
			// 紧急返回 / Escape
			// 睡眠 / Sleep
			// 睡眠之云 I / Tranquilizing Cloud
			// 睡眠暴风 I / Sleeping Storm.
			// 诅咒:古树 I / Curse Of Old Roots
			// 诅咒:树 I / Curse Of Roots
			// 恐怖的呼喊 I / Fear Shriek
			// 恐惧 / Fear
			// 高阶守护者变身 5.1【天族】 / ArchDaeva Transformation 5.1 [Elyos]
			// Transformation: Avatar Of Fire.
			// Transformation: Avatar Of Water.
			// Transformation: Avatar Of Earth.
			// Transformation: Avatar Of Wind.
			// 高阶守护者变身 5.1【魔族】 / ArchDaeva Transformation 5.1 [Asmodians]
			// Transformation: Avatar Of Fire.
			// Transformation: Avatar Of Water.
			// Transformation: Avatar Of Earth.
			// Transformation: Avatar Of Wind.
			// 遗忘裂隙 5.1 / Fissure Of Oblivion 5.1
			// Transformation: Avatar Of Fire.
			// Transformation: Avatar Of Water.
			// Transformation: Avatar Of Earth.
			// Transformation: Avatar Of Wind.
			// 天族【守护者将军】 / Elyos [Guardian General]
			// 变身:守护神将 / Transformation: Guardian General I
			// 变身:守护神将 / Transformation: Guardian General II
			// 变身:守护神将 / Transformation: Guardian General III
			// 变身:守护神将 / Transformation: Guardian General IV
			// 变身:守护神将 / Transformation: Guardian General V
			// 魔族【守护者将军】 / Asmodians [Guardian General]
			// 变身:守护神将 / Transformation: Guardian General I
			// 变身:守护神将 / Transformation: Guardian General II
			// 变身:守护神将 / Transformation: Guardian General III
			// 变身:守护神将 / Transformation: Guardian General IV
			case 17, 18, 19, 20, 243, 245, 246, 247, 251, 253, 297, 308, 309, 310, 311, 312, 313, 314, 249, 250, 252,
			     254, 298, 315, 316, 317, 318, 319, 320, 321, 302, 1337, 1338, 1339, 1416, 1417, 3589, 3775, 4752, 4757,
			     4762, 4768, 4804, 4805, 4806, 4807, 4808, 4813, 4818, 4824, 11885, 11886, 11887, 11888, 11889, 11890,
			     11891, 11892, 11893, 11894 -> // 变身:守护神将 / Transformation: Guardian General V
				true;
			default -> false;
		};
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

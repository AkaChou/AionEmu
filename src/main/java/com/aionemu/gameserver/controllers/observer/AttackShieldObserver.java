package com.aionemu.gameserver.controllers.observer;

import java.util.List;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.attack.AttackResult;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.summons.SummonMode;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;
import com.aionemu.gameserver.skillengine.model.HitType;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 攻击护盾观察者：处理吸收、反射、保护与伤害转治疗等护盾逻辑。
 * Attack shield observer handling absorb, reflect, protect and convert-to-heal shield logic.
 *
 * @author ATracer modified by Sippolo, kecimis, Luzien
 */
public class AttackShieldObserver extends AttackCalcObserver {

	/** 单次吸收/反射命中数值 / Per-hit absorb/reflect value */
	private int hit;
	/** 总吸收量或反射百分比 / Total absorb amount or reflect percent */
	private int totalHit;
	/** 命中是否基于百分比 / Whether hit is percent-based */
	private boolean hitPercent;
	/** 总命中是否基于百分比 / Whether totalHit is percent-based */
	private boolean totalHitPercent;
	/** 关联技能效果 / Associated skill effect */
	private Effect effect;
	/** 命中类型过滤 / Hit type filter */
	private HitType hitType;
	/** 护盾类型（0 转治疗/1 反射/2 吸收/8 保护） / Shield type (0 convert-heal/1 reflect/2 absorb/8 protect) */
	private int shieldType;
	/** 触发概率 / Trigger probability */
	private int probability = 100;
	/** 反射最小半径 / Reflect min radius */
	private int minradius = 0;
	/** 反射最大半径 / Reflect max radius */
	private int maxradius = 100;
	/** 转治疗类型 / Convert-heal type */
	private HealType healType = null;
	/** 保护者承伤比例/数值 / Protector damage share percent/value */
	private int effectorDamage;
	/** 吸收时消耗 MP 百分比 / MP cost percent on absorb */
	private int mpValue;
	/**
	 * 简化构造：百分比总伤、无半径/治疗/保护/MP。
	 * Simplified constructor: percent total hit, no radius/heal/protect/MP.
	 *
	 * @param hit 单次数值 / per-hit value
	 * @param totalHit 总量或反射百分比 / total amount or reflect percent
	 * @param percent 是否百分比 / whether percent
	 * @param effect 关联技能效果 / associated skill effect
	 * @param type 命中类型 / hit type
	 * @param shieldType 护盾类型 / shield type
	 * @param probability 触发概率 / trigger probability
	 */
	public AttackShieldObserver(int hit, int totalHit, boolean percent, Effect effect, HitType type, int shieldType,
			int probability) {
		this(hit, totalHit, percent, false, effect, type, shieldType, probability, 0, 100, null, 0, 0);
	}

	/**
	 * 带保护者承伤的构造。
	 * Constructor with protector damage share.
	 *
	 * @param hit 单次数值 / per-hit value
	 * @param effectorDamage 保护者承伤 / protector damage share
	 * @param totalHit 总量或反射百分比 / total amount or reflect percent
	 * @param percent 是否百分比 / whether percent
	 * @param effect 关联技能效果 / associated skill effect
	 * @param type 命中类型 / hit type
	 * @param shieldType 护盾类型 / shield type
	 * @param probability 触发概率 / trigger probability
	 */
	public AttackShieldObserver(int hit, int effectorDamage, int totalHit, boolean percent, Effect effect, HitType type,
			int shieldType, int probability) {
		this(hit, totalHit, percent, false, effect, type, shieldType, probability, 0, 100, null, effectorDamage, 0);
	}

	/**
	 * 带 MP 消耗的构造。
	 * Constructor with MP cost.
	 *
	 * @param hit 单次数值 / per-hit value
	 * @param totalHit 总量或反射百分比 / total amount or reflect percent
	 * @param percent 是否百分比 / whether percent
	 * @param effect 关联技能效果 / associated skill effect
	 * @param type 命中类型 / hit type
	 * @param shieldType 护盾类型 / shield type
	 * @param probability 触发概率 / trigger probability
	 * @param mpValue 吸收时消耗 MP 百分比 / MP percent on absorb
	 */
	public AttackShieldObserver(int hit, int totalHit, boolean percent, Effect effect, HitType type, int shieldType,
			int probability, int mpValue) {
		this(hit, totalHit, percent, false, effect, type, shieldType, probability, 0, 100, null, 0, mpValue);
	}

	/**
	 * 完整构造。
	 * Full constructor.
	 *
	 * @param hit 单次数值 / per-hit value
	 * @param totalHit 总量或反射百分比 / total amount or reflect percent
	 * @param hitPercent hit 是否百分比 / whether hit is percent
	 * @param totalHitPercent totalHit 是否百分比 / whether totalHit is percent
	 * @param effect 关联技能效果 / associated skill effect
	 * @param type 命中类型 / hit type
	 * @param shieldType 护盾类型 / shield type
	 * @param probability 触发概率 / trigger probability
	 * @param minradius 反射最小半径 / reflect min radius
	 * @param maxradius 反射最大半径 / reflect max radius
	 * @param healType 转治疗类型 / convert-heal type
	 * @param effectorDamage 保护者承伤 / protector damage
	 * @param mpValue 吸收时消耗 MP 百分比 / MP percent on absorb
	 */
	public AttackShieldObserver(int hit, int totalHit, boolean hitPercent, boolean totalHitPercent, Effect effect,
			HitType type, int shieldType, int probability, int minradius, int maxradius, HealType healType,
			int effectorDamage, int mpValue) {
		this.hit = hit;
		this.totalHit = totalHit;// 护盾的总吸收量，反射时为反射百分比 / total absorbed dmg for shield, percentage for reflector
		this.effect = effect;
		this.hitPercent = hitPercent;
		this.totalHitPercent = totalHitPercent;
		this.hitType = type;
		this.shieldType = shieldType;
		this.probability = probability;
		this.minradius = minradius;// 仅用于反射护盾 / only for reflector
		this.maxradius = maxradius;// 仅用于反射护盾 / only for reflector
		this.healType = healType;// 仅用于转治疗护盾 / only for convertheal
		this.effectorDamage = effectorDamage;// 仅用于保护护盾 / only for protect
		this.mpValue = mpValue;
	}

	/**
	 * 按护盾类型处理攻击列表中的伤害。
	 * Process damages in the attack list according to shield type.
	 *
	 * @param attackList 攻击结果列表 / attack result list
	 * @param attackerEffect 攻击方效果 / attacker effect
	 * @param attacker 攻击者 / attacker
	 */
	@Override
	public void checkShield(List<AttackResult> attackList, Effect attackerEffect, Creature attacker) {
		if (shieldType == 8 && attackerEffect != null && !attackerEffect.isDamageProtectorEnabled()) {
			return;
		}
		for (AttackResult attackResult : attackList) {
			if (AttackStatus.getBaseStatus(attackResult.getAttackStatus()) == AttackStatus.DODGE
					|| AttackStatus.getBaseStatus(attackResult.getAttackStatus()) == AttackStatus.RESIST) {
				continue;
			}
			if (this.hitType != HitType.EVERYHIT) {
				if ((attackResult.getDamageType() != null) && (attackResult.getDamageType() != this.hitType))
					continue;
			}
			if (!isTriggered(probability, Rnd.get(1000))) {
				continue;
			}
			if (shieldType == 2) {
				int damage = attackResult.getDamage();
				int currentMp = mpValue > 0 ? effect.getEffected().getLifeStats().getCurrentMp() : Integer.MAX_VALUE;
				int shieldUse = calculateShieldUse(damage, hit, hitPercent, totalHit, currentMp);
				int absorbedDamage = mpValue > 0 && effect.getEffected() instanceof Player
						? calculatePercent(shieldUse, mpValue) : shieldUse;
				totalHit -= shieldUse;
				if (absorbedDamage > 0) {
					attackResult.setShieldType(shieldType);
				}
				attackResult.setDamage(damage - absorbedDamage);
				if (absorbedDamage >= damage && !isPunchShield(attackerEffect)) {
					attackResult.setLaunchSubEffect(false);
				}
				if (mpValue > 0) {
					int mpCost = calculatePercent(shieldUse, mpValue);
					attackResult.setShieldMp(mpCost);
					effect.getEffected().getLifeStats().reduceMp(mpCost);
					attackResult.setReflectedSkillId(effect.getSkillId());
				}
				if (totalHit <= 0 || mpValue > 0 && shieldUse == currentMp) {
					effect.endEffect();
					return;
				}
			} else if (shieldType == 1) {
				if (minradius != 0) {
					if (MathUtil.isIn3dRange(attacker, effect.getEffected(), minradius)) {
						continue;
					}
				}
				if (MathUtil.isIn3dRange(attacker, effect.getEffected(), maxradius)) {
					int reflectedHit = calculateReflectedDamage(attackResult.getDamage(), totalHit, hit);
					attackResult.setShieldType(shieldType);
					if (attacker instanceof Npc) {
						reflectedHit = attacker.getAi2().modifyDamage(reflectedHit);
					}
					attackResult.setReflectedDamage(reflectedHit);
					attackResult.setReflectedSkillId(effect.getSkillId());
					attacker.getController().onAttack(effect.getEffected(), reflectedHit, false);
					if (effect.getEffected() instanceof Player) {
						PacketSendUtility.sendPacket((Player) effect.getEffected(), SM_SYSTEM_MESSAGE
								.STR_SKILL_PROC_EFFECT_OCCURRED(effect.getSkillTemplate().getNameId()));
					}
				}
			} else if (shieldType == 8) {
				if (effect.getEffector() == null || effect.getEffector().getLifeStats().isAlreadyDead()) {
					effect.endEffect();
					break;
				}
				if (effect.getEffector() instanceof Summon
						&& (((Summon) effect.getEffector()).getMode() == SummonMode.RELEASE
								|| ((Summon) effect.getEffector()).getMaster() == null)) {
					effect.endEffect();
					break;
				}
				if (MathUtil.isIn3dRange(effect.getEffector(), effect.getEffected(), totalHit)) {
					int damageProtected = calculateProtectedDamage(attackResult.getDamage(), hit, hitPercent);
					int effectorDamage = calculateProtectorDamage(damageProtected, this.effectorDamage);
					int finalDamage = attackResult.getDamage() - damageProtected;
					attackResult.setDamage((finalDamage <= 0 ? 0 : finalDamage));
					attackResult.setShieldType(shieldType);
					attackResult.setProtectedSkillId(effect.getSkillId());
					attackResult.setProtectedDamage(effectorDamage);
					attackResult.setProtectorId(effect.getEffectorId());
					effect.getEffector().getController().onAttack(attacker, effect.getSkillId(), TYPE.PROTECTDMG,
							effectorDamage, false, LOG.REGULAR);
				}
			} else if (shieldType == 0) {
				int damage = attackResult.getDamage();
				int requestedAbsorption = hitPercent ? calculatePercent(damage, totalHit) : Math.min(damage, totalHit);
				int absorbedDamage = Math.max(0, Math.min(damage, requestedAbsorption));
				attackResult.setDamage(damage - absorbedDamage);
				int conversionBase = calculateConversionBase(damage, absorbedDamage);
				int healValue = totalHitPercent ? calculatePercent(conversionBase, hit) : hit;
				switch (healType) {
				case HP:
					effect.getEffected().getLifeStats().increaseHp(TYPE.HP, healValue, effect.getSkillId(),
							LOG.REGULAR);
					break;
				case MP:
					effect.getEffected().getLifeStats().increaseMp(TYPE.HEAL_MP, healValue, effect.getSkillId(),
							LOG.REGULAR);
					break;
				default:
					break;
				}
				if (absorbedDamage >= damage && !isPunchShield(attackerEffect)) {
					attackResult.setLaunchSubEffect(false);
				}
			}
		}
	}

	static boolean isTriggered(int probability, int roll) {
		return roll < probability;
	}

	static int calculateReflectedDamage(int damage, int percent, int minimum) {
		return damage * percent / 100 + minimum;
	}

	static int calculateShieldUse(int damage, int hit, boolean percent, int remainingShield, int currentMp) {
		int requested = percent ? calculatePercent(damage, hit) : Math.min(damage, hit);
		return Math.max(0, Math.min(requested, Math.min(remainingShield, currentMp)));
	}

	static int calculatePercent(int value, int percent) {
		return (int) ((long) value * percent / 100);
	}

	static int calculateConversionBase(int damage, int absorbedDamage) {
		return Math.min(absorbedDamage, Math.max(0, damage - 1));
	}

	static int calculateProtectedDamage(int damage, int protectedValue, boolean percent) {
		return Math.min(damage, percent ? damage * protectedValue / 100 : protectedValue);
	}

	static int calculateProtectorDamage(int protectedDamage, int protectorPercent) {
		return protectedDamage * (protectorPercent == 0 ? 100 : protectorPercent) / 100;
	}

	public int getShieldPriority() {
		if (shieldType == 2) {
			return mpValue > 0 ? 1 : 0;
		}
		return switch (shieldType) {
			case 1 -> 2;
			case 8 -> 3;
			case 0 -> 4;
			default -> 5;
		};
	}

	/**
	 * 判断攻击效果是否为可穿透护盾的挑衅子效果技能。
	 * Whether the attacker effect is a provoked sub-effect skill that punches through shields.
	 *
	 * @param effect 攻击方效果 / attacker effect
	 * @return 是否可穿透 / whether punches through
	 */
	private boolean isPunchShield(Effect effect) {
		if (effect == null)
			return false;
		for (EffectTemplate template : effect.getEffectTemplates()) {
			if (template.getSubEffect() != null) {
				SkillTemplate skill = DataManager.SKILL_DATA.getSkillTemplate(template.getSubEffect().getSkillId());
				if (skill.isProvoked()) {
					return true;
				}
			}
		}
		return false;
	}
}

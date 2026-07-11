package com.aionemu.gameserver.controllers.effect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.EffectType;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.EffectResult;
import com.aionemu.gameserver.skillengine.model.SkillSubType;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.skillengine.model.TransformType;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster.BroadcastMode;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * 效果控制器，管理生物身上的 buff/debuff、异常状态与驱散逻辑。
 * Effect controller managing creature buffs/debuffs, abnormal states and dispel logic.
 *
 * @author ATracer
 */
public class EffectController {
	private Creature owner;

	/** 被动效果映射。 / Passive effect map. */
	protected Map<String, Effect> passiveEffectMap = Collections.synchronizedMap(new LinkedHashMap<String, Effect>());
	/** 不显示图标的效果映射。 / No-show effect map. */
	protected Map<String, Effect> noshowEffects = Collections.synchronizedMap(new LinkedHashMap<String, Effect>());
	/** 异常效果映射。 / Abnormal effect map. */
	protected Map<String, Effect> abnormalEffectMap = Collections.synchronizedMap(new LinkedHashMap<String, Effect>());

	/** 效果映射互斥锁。 / Mutex for effect maps. */
	private final Lock lock = new ReentrantLock();

	/** 当前异常状态位掩码。 / Current abnormal-state bit mask. */
	protected int abnormals;

	/** 是否处于护盾保护。 / Whether currently under a shield. */
	private boolean isUnderShield = false;

	/**
	 * 为指定生物构造效果控制器。
	 * Constructs an effect controller for the given creature.
	 *
	 * @param owner 所有者生物 / owner creature
	 */
	public EffectController(Creature owner) {
		this.owner = owner;
	}

	/**
	 * 返回效果所属生物。
	 * Returns the creature that owns these effects.
	 *
	 * owner
	 */
	public Creature getOwner() {
		return owner;
	}

	/**
	 * 是否处于护盾效果保护中。
	 * Whether the owner is currently under a shield effect.
	 *
	 * @return true 若处于护盾中 / true if under shield
	 */
	public boolean isUnderShield() {
		return isUnderShield;
	}

	/**
	 * 设置护盾状态标志。
	 * Sets the under-shield flag.
	 *
	 * @param isUnderShield 是否处于护盾中 / whether under shield
	 */
	public void setUnderShield(boolean isUnderShield) {
		this.isUnderShield = isUnderShield;
	}

	/**
	 * 添加效果：处理被动叠层、冲突、开关/吟唱/游侠上限后启动并广播。
	 * Adds an effect: resolves passive stacks, conflicts, toggle/chant/ranger caps, then starts and broadcasts.
	 *
	 * @param nextEffect 待添加的效果 / effect to add
	 */
	public void addEffect(Effect nextEffect) {
		Map<String, Effect> mapToUpdate = getMapForEffect(nextEffect);

		lock.lock();
		try {
			if (nextEffect.isPassive()) {
				boolean useEffectId = true;
				Effect existingEffect = mapToUpdate.get(nextEffect.getStack());
				if (existingEffect != null && existingEffect.isPassive()) {
					// 检查堆叠等级 / check stack level
					if (existingEffect.getSkillStackLvl() > nextEffect.getSkillStackLvl()) {
						return;
					}

					// 检查技能等级（堆叠等级相同时） / check skill level (when stack level same)
					if (existingEffect.getSkillStackLvl() == nextEffect.getSkillStackLvl()
							&& existingEffect.getSkillLevel() > nextEffect.getSkillLevel()) {
						return;
					}
					existingEffect.endEffect();
					useEffectId = false;
				}

				if (useEffectId) {
					/**
	 * idea here is that effects with same effectId shouldnt stack effect with higher basiclvl takes priority
	 */
					for (Effect effect : effectsSnapshot(mapToUpdate)) {
						if (effect.getTargetSlot() == nextEffect.getTargetSlot()) {
							for (EffectTemplate et : effect.getEffectTemplates()) {
								if (et.getEffectid() == 0) {
									continue;
								}
								for (EffectTemplate et2 : nextEffect.getEffectTemplates()) {
									if (et2.getEffectid() == 0) {
										continue;
									}
									if (et.getEffectid() == et2.getEffectid()) {
										if (et.getBasicLvl() > et2.getBasicLvl()) {
											return;
										} else {
											effect.endEffect();
										}
									}
								}
							}
						}
					}
				}
			}

			Effect conflictedEffect = findConflictedEffect(mapToUpdate, nextEffect);
			if (conflictedEffect != null) {
				conflictedEffect.endEffect();
			}
			// 最多 3 个吟唱效果 / Max 3 Chants Effect
			if (nextEffect.isToggle()) {
				int mts = 1;
				if (nextEffect.getSkillSubType() == SkillSubType.CHANT) {
					mts = 3;
				} else if (isAethertechEffect(nextEffect.getSkillId())) {
					mts = 6;
				} else {
					mts = 1;
				}
				if (mapToUpdate.size() >= mts) {
					Iterator<Effect> iter = effectsSnapshot(mapToUpdate).iterator();
					Effect effect = iter.next();
					effect.endEffect();
				}
			}
			// 最多 4 个吟唱效果 / Max 4 Chants Effect
			if (nextEffect.isChant()) {
				Collection<Effect> chants = this.getChantEffects();
				if (chants.size() >= 4) {
					Iterator<Effect> chantIter = chants.iterator();
					chantIter.next().endEffect();
				}
			}
			// 最多 2 个游侠效果 / Max 2 Ranger Effect
			if (nextEffect.isRangerBuff()) {
				Collection<Effect> rangerBuff = this.getRangerEffects();
				if (rangerBuff.size() >= 2) {
					Iterator<Effect> rangerIter = rangerBuff.iterator();
					rangerIter.next().endEffect();
				}
			}
			if (!nextEffect.isPassive()) {
				if (searchConflict(nextEffect)) {
					return;
				}
				checkEffectCooldownId(nextEffect);
			}
			Effect existingEffect = mapToUpdate.get(nextEffect.getStack());
			if (existingEffect != null && existingEffect != nextEffect) {
				existingEffect.endEffect();
			}
			mapToUpdate.put(nextEffect.getStack(), nextEffect);
		} finally {
			lock.unlock();
		}
		nextEffect.startEffect(false);
		if (!nextEffect.isPassive()) {
			broadCastEffects();
		}
	}

	/**
	 * 判断技能是否为以太科技类开关效果（4.8）。
	 * Returns whether the skill is an Aethertech-style toggle effect (4.8).
	 *
	 * skill id
	 *
	 * @param skillId @return true 若为以太科技效果 / true if Aethertech effect
	 */
	public boolean isAethertechEffect(int skillId) { // 4.8
		switch (skillId) {
		// 登船 / Embark
		case 2767:
		case 2768:
		case 2769:
		case 2770:
		case 2771:
		case 2772:
		case 2773:
		case 2774:
		case 2775:
		case 2776:
		case 2777:
		case 2778:
			// 动能电池 / Kinetic Battery
		case 2440:
		case 2441:
		case 2442:
		case 2443:
		case 2444:
		case 2445:
		case 2446:
		case 2447:
		case 2448:
		case 2449:
			// 动能壁垒 / Kinetic Bulwark
		case 2579:
		case 2580:
		case 2581:
			// 机动推进器 / Mobility Thrusters
		case 2421:
		case 2422:
			// 稳定推进器 / Stability Thrusters
		case 2736:
		case 2737:
		case 2738:
		case 2739:
		case 2740:
			// 骑乘挫败 / Mounting Frustration
		case 2838:
		case 2839:
		case 2840:
		case 2841:
		case 2842:
		case 2843:
		case 2844:
		case 2845:
		case 2846:
		case 2847:
		case 2848:
			return true;
		}
		return false;
	}

	/**
	 * 在映射中查找与新效果 conflictId 冲突的已有效果。
	 * Finds an existing effect in the map that conflicts with the new effect by conflictId.
	 *
	 * @param mapToUpdate 待检索的效果映射 / effect map to search
	 * new effect
	 * @return 冲突效果，无则 null / conflicting effect, or null
	 */
	private final Effect findConflictedEffect(Map<String, Effect> mapToUpdate, Effect newEffect) {
		int conflictId = newEffect.getSkillTemplate().getConflictId();
		if (conflictId == 0) {
			return null;
		}
		for (Effect effect : effectsSnapshot(mapToUpdate)) {
			if (effect.getSkillTemplate().getConflictId() == conflictId) {
				return effect;
			}
		}
		return null;
	}

	/**
	 * 按效果类型返回对应存储映射（被动 / 开关 / 异常）。
	 * toggle / abnormal). / toggle / abnormal).
	 *
	 * effect
	 * matching map
	 */
	private Map<String, Effect> getMapForEffect(Effect effect) {

		if (effect.isPassive()) {
			return passiveEffectMap;
		}
		if (effect.isToggle()) {
			return noshowEffects;
		}
		return abnormalEffectMap;
	}

	/**
	 * 按 stack 键获取异常效果。
	 * Returns the abnormal effect for the given stack key.
	 *
	 * effect stack key
	 *
	 * @param stack @return 异常效果或 null / abnormal effect or null
	 */
	public Effect getAnormalEffect(String stack) {
		return abnormalEffectMap.get(stack);
	}

	/**
	 * 是否存在指定技能 ID 的异常效果。
	 * Whether an abnormal effect with the given skill id is present.
	 *
	 * skill id
	 *
	 * @param skillId 存在则为 true / true if present
	 */
	public boolean hasAbnormalEffect(int skillId) {
		Iterator<Effect> localIterator = effectsSnapshot(abnormalEffectMap).iterator();
		while (localIterator.hasNext()) {
			Effect localEffect = localIterator.next();
			if (localEffect.getSkillId() == skillId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 在全部效果映射中是否包含指定效果模板 ID。
	 * Whether any effect map contains the given effect-template id.
	 *
	 * effect template id
	 *
	 * @param effectId 若 contained 则为 true / true if contained
	 */
	public boolean hasEffectById(int effectId) {
		Collection<Effect> allEffects = new ArrayList<>();
		allEffects.addAll(effectsSnapshot(abnormalEffectMap));
		allEffects.addAll(effectsSnapshot(noshowEffects));
		allEffects.addAll(effectsSnapshot(passiveEffectMap));
		for (Effect effect : allEffects) {
			if (effect.containsEffectId(effectId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 将效果广播标记加入所有者，延迟广播异常状态。
	 * Marks the owner for delayed abnormal-effect broadcast.
	 */
	public void broadCastEffects() {
		owner.addPacketBroadcastMask(BroadcastMode.BROAD_CAST_EFFECTS);
	}

	/**
	 * 立即向可见对象广播当前异常效果。
	 * Immediately broadcasts current abnormal effects to visible objects.
	 */
	public void broadCastEffectsImp() {
		List<Effect> effects = getAbnormalEffects();
		PacketSendUtility.broadcastPacket(getOwner(), new SM_ABNORMAL_EFFECT(getOwner(), abnormals, effects));
	}

	/**
	 * 向指定玩家发送当前效果图标（用于新进入视野）。
	 * Sends current effect icons to the given player (when newly seen).
	 *
	 * target player
	 */
	public void sendEffectIconsTo(Player player) {
		List<Effect> effects = getAbnormalEffects();
		PacketSendUtility.sendPacket(player, new SM_ABNORMAL_EFFECT(getOwner(), abnormals, effects));
	}

	/**
	 * 从对应映射移除已结束的效果并广播。
	 * Removes an ended effect from its map and broadcasts.
	 *
	 * @param effect 要清除的效果 / effect to clear
	 */
	public void clearEffect(Effect effect) {
		Map<String, Effect> mapForEffect = getMapForEffect(effect);
		mapForEffect.remove(effect.getStack(), effect);
		broadCastEffects();
	}

	/**
	 * 按技能 ID 结束并移除全部映射中的效果。
	 * Ends and removes effects with the given skill id from all maps.
	 *
	 * skill id
	 */
	public void removeEffect(int skillid) {
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			if (effect.getSkillId() == skillid) {
				effect.endEffect();
			}
		}

		for (Effect effect : effectsSnapshot(passiveEffectMap)) {
			if (effect.getSkillId() == skillid) {
				effect.endEffect();
			}
		}

		for (Effect effect : effectsSnapshot(noshowEffects)) {
			if (effect.getSkillId() == skillid) {
				effect.endEffect();
			}
		}
	}

	/**
	 * 移除隐身类效果（视觉状态较低时）。
	 * Removes hide effects when visual state is below threshold.
	 */
	public void removeHideEffects() {
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			if (effect.isHideEffect() && owner.getVisualState() < 10) {
				effect.endEffect();
				abnormalEffectMap.remove(effect.getStack());
			}
		}
	}

	/**
	 * 移除麻痹类效果。
	 * Removes paralyze effects from the owner.
	 */
	public void removeParalyzeEffects() {
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			if (effect.isParalyzeEffect()) {
				effect.endEffect();
				abnormalEffectMap.remove(effect.getStack());
			}
		}
	}

	/**
	 * 按效果模板 ID 结束异常效果。
	 * Ends abnormal effects that contain the given effect-template id.
	 *
	 * effect template id
	 */
	public void removeEffectByEffectId(int effectId) {
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			if (effect.containsEffectId(effectId)) {
				effect.endEffect();
			}
		}
	}

	/**
	 * 计算可被 DispelBuffCounterAtk 驱散的效果数量。
	 * Counts effects removable by DispelBuffCounterAtk at the given dispel level.
	 *
	 * dispel level
	 *
	 * @param dispelLevel @return 可驱散数量 / removable count
	 */
	public int calculateNumberOfEffects(int dispelLevel) {
		int number = 0;

		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			DispelCategoryType dispelCat = effect.getDispelCategory();
			SkillTargetSlot tragetSlot = effect.getSkillTemplate().getTargetSlot();
			// 持久效果受保护，除非客户端数据将已知遭遇 Buff 标为可驱散。 / Persistent effects are protected unless the client data marks a known encounter buff as dispellable.
			if (effect.getDuration() >= 86400000 && !isDispellableLongDurationEffect(effect)) {
				continue;
			}
			if (effect.isSanctuaryEffect()) {
				continue;
			}
			// 检查目标槽；目标槽 ≥2 的效果不能 / check for targetslot, effects with target slot higher or equal to 2 cant be
			// removed (ex. skillId: 11885)
			if (tragetSlot != SkillTargetSlot.BUFF
					&& (tragetSlot != SkillTargetSlot.DEBUFF && dispelCat != DispelCategoryType.ALL)
					|| effect.getTargetSlotLevel() >= 2) {
				continue;
			}
			switch (dispelCat) {
			case ALL:
			case BUFF: // DispelBuffCounterAtkEffect
				if (effect.getReqDispelLevel() <= dispelLevel) {
					number++;
				}
				break;
			default:
				break;
			}
		}
		return number;
	}

	/**
	 * 按驱散类别、目标槽与驱散等级移除效果。
	 * Removes effects by dispel category, target slot and dispel level.
	 *
	 * dispel category
	 * target slot
	 * @param count 最多移除数量 / max removals
	 * dispel level
	 * @param power 驱散强度 / dispel power
	 * @param itemTriggered 是否由物品触发 / whether triggered by an item
	 */
	public void removeEffectByDispelCat(DispelCategoryType dispelCat, SkillTargetSlot targetSlot, int count,
			int dispelLevel, int power, boolean itemTriggered) {
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			if (count == 0) {
				break;
			}
			// 持久效果受保护，除非客户端数据将已知遭遇 Buff 标为可驱散。 / Persistent effects are protected unless the client data marks a known encounter buff as dispellable.
			if (effect.getDuration() >= 86400000 && !isDispellableLongDurationEffect(effect)) {
				continue;
			}
			if (effect.isSanctuaryEffect()) {
				continue;
			}
			// 若驱散由物品触发（如治疗药水） / If dispel is triggered by an item (ex. Healing Potion)
			// 若减益不可用药驱散，则不驱散 / and debuff is unpottable, do not dispel
			if ((effect.getSkillTemplate().isUndispellableByPotions()) && itemTriggered) {
				continue;
			}
			// 检查目标槽；目标槽等级 ≥2 的效果 / check for targetslot, effects with target slot level higher or equal to 2
			// 无法移除（如 skillId: 11885） / cant be removed (ex. skillId: 11885)
			if (effect.getTargetSlot() != targetSlot.ordinal() || effect.getTargetSlotLevel() >= 2) {
				continue;
			}
			boolean remove = false;
			switch (dispelCat) {
			case ALL:
				if ((effect.getDispelCategory() == DispelCategoryType.ALL
						|| effect.getDispelCategory() == DispelCategoryType.DEBUFF_MENTAL
						|| effect.getDispelCategory() == DispelCategoryType.DEBUFF_PHYSICAL)
						&& effect.getReqDispelLevel() <= dispelLevel) {
					remove = true;
				}
				break;
			case DEBUFF_MENTAL:
				if ((effect.getDispelCategory() == DispelCategoryType.ALL
						|| effect.getDispelCategory() == DispelCategoryType.DEBUFF_MENTAL)
						&& effect.getReqDispelLevel() <= dispelLevel) {
					remove = true;
				}
				break;
			case DEBUFF_PHYSICAL:
				if ((effect.getDispelCategory() == DispelCategoryType.ALL
						|| effect.getDispelCategory() == DispelCategoryType.DEBUFF_PHYSICAL)
						&& effect.getReqDispelLevel() <= dispelLevel) {
					remove = true;
				}
				break;
			case BUFF:
				if (effect.getDispelCategory() == DispelCategoryType.BUFF
						&& effect.getReqDispelLevel() <= dispelLevel) {
					remove = true;
				}
				break;
			case STUN:
				if (effect.getDispelCategory() == DispelCategoryType.STUN) {
					remove = true;
				}
				break;
			case NPC_BUFF:
				if (effect.getDispelCategory() == DispelCategoryType.NPC_BUFF) {
					remove = true;
				}
				break;
			case NPC_DEBUFF_PHYSICAL:
				if (effect.getDispelCategory() == DispelCategoryType.NPC_DEBUFF_PHYSICAL) {
					remove = true;
				}
				break;
			default:
				break;
			}

			if (remove) {
				if (removePower(effect, power)) {
					effect.endEffect();
					abnormalEffectMap.remove(effect.getStack());
				} else if (owner instanceof Player) {
					PacketSendUtility.sendPacket((Player) owner, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_DISPELCOUNT);
				}
				count--;
			} else if (owner instanceof Player) {
				PacketSendUtility.sendPacket((Player) owner, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_DISPELLEVEL);
			}
		}
	}

	/**
	 * 执行 DispelBuffCounterAtk：按数量与等级驱散增益。
	 * Runs DispelBuffCounterAtk: dispels buffs by count and level.
	 *
	 * @param count 最多驱散数 / max dispels
	 * dispel level
	 * @param power 驱散强度 / dispel power
	 */
	public void dispelBuffCounterAtkEffect(int count, int dispelLevel, int power) {
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			DispelCategoryType dispelCat = effect.getDispelCategory();
			SkillTargetSlot tragetSlot = effect.getSkillTemplate().getTargetSlot();
			if (count == 0) {
				break;
			}
			if (effect.getDuration() >= 86400000 && !isDispellableLongDurationEffect(effect)) {
				continue;
			}
			if (effect.isSanctuaryEffect()) {
				continue;
			}
			if (tragetSlot != SkillTargetSlot.BUFF
					&& (tragetSlot != SkillTargetSlot.DEBUFF && dispelCat != DispelCategoryType.ALL)
					|| effect.getTargetSlotLevel() >= 2) {
				continue;
			}
			boolean remove = false;
			switch (dispelCat) {
			case ALL:
			case BUFF:
				if (effect.getReqDispelLevel() <= dispelLevel) {
					remove = true;
				}
				break;
			default:
				break;
			}

			if (remove) {
				if (removePower(effect, power)) {
					effect.endEffect();
					abnormalEffectMap.remove(effect.getStack());
				} else if (owner instanceof Player) {
					PacketSendUtility.sendPacket((Player) owner, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_DISPELCOUNT);
				}
				count--;
			} else if (owner instanceof Player) {
				PacketSendUtility.sendPacket((Player) owner, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_DISPELLEVEL);
			}
		}
	}

	/**
	 * 判断超长时长效果是否仍允许被驱散（白名单技能）。
	 * Whether a long-duration effect is still removable (whitelist skills).
	 *
	 * effect
	 *
	 * @param effect 若 removable 则为 true / true if removable
	 */
	private boolean isDispellableLongDurationEffect(Effect effect) {
		int skillId = effect.getSkillId();
		switch (skillId) {
		case 20941:
		case 20942:
		case 19370:
		case 19371:
		case 19372:
		case 20530:
		case 20531:
		case 19345:
		case 19346:
		case 21438:
			return true;
		default:
			return false;
		}
	}

	/**
	 * 按效果类型结束匹配的异常效果。
	 * Ends abnormal effects matching the given effect type.
	 *
	 * effect type
	 */
	public void removeEffectByEffectType(EffectType effectType) {
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			for (EffectTemplate et : effect.getSuccessEffect()) {
				if (effectType == et.getEffectType()) {
					effect.endEffect();
				}
			}
		}
	}

	/**
	 * 削减效果驱散强度；耗尽则返回 true。
	 * Reduces the effect's remaining dispel power; returns true when exhausted.
	 *
	 * effect
	 *
	 * @param power 驱散强度 / power to apply
	 * @param power @return true 若效果应结束 / true if the effect should end
	 */
	private boolean removePower(Effect effect, int power) {
		int effectPower = effect.removePower(power);

		if (effectPower <= 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 按技能 ID 移除被动效果。
	 * Removes passive effects by skill id.
	 *
	 * skill id
	 */
	public void removePassiveEffect(int skillid) {
		for (Effect effect : effectsSnapshot(passiveEffectMap)) {
			if (effect.getSkillId() == skillid) {
				effect.endEffect();
			}
		}
	}

	/**
	 * 按技能 ID 移除不显示的开关类效果。
	 * Removes no-show (toggle) effects by skill id.
	 *
	 * skill id
	 */
	public void removeNoshowEffect(int skillid) {
		for (Effect effect : effectsSnapshot(noshowEffects)) {
			if (effect.getSkillId() == skillid) {
				effect.endEffect();
			}
		}
	}

	/**
	 * 按目标槽移除异常效果。
	 * Removes abnormal effects matching the target slot.
	 *
	 * target slot
	 */
	public void removeAbnormalEffectsByTargetSlot(SkillTargetSlot targetSlot) {
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			if (effect.getTargetSlot() == targetSlot.ordinal()) {
				effect.endEffect();
			}
		}
	}

	/**
	 * 移除全部非常驻效果（非登出路径）。
	 * Removes all non-persistent effects (non-logout path).
	 */
	public void removeAllEffects() {
		this.removeAllEffects(false);
	}

	/**
	 * 移除效果；登出时清空全部映射，否则保留部分增益。
	 * Removes effects; on logout clears all maps, otherwise keeps selected boosts.
	 *
	 * @param logout 是否登出清理 / whether this is a logout clear
	 */
	public void removeAllEffects(boolean logout) {
		if (!logout) {
			for (Map.Entry<String, Effect> entry : effectEntriesSnapshot(abnormalEffectMap)) {
				if (!entry.getValue().getSkillTemplate().isNoRemoveAtDie() && !entry.getValue().isXpBoost()
						&& !entry.getValue().isApBoost() && !entry.getValue().isDrBoost()
						&& !entry.getValue().isBdrBoost() && !entry.getValue().isEnchantBoost()
						&& !entry.getValue().isIdunDropBoost() && !entry.getValue().isAuthorizeBoost()
						&& !entry.getValue().isSprintFpReduce() && !entry.getValue().isReturnCoolReduce()
						&& !entry.getValue().isEnchantOptionBoost() && !entry.getValue().isDeathPenaltyReduce()
						&& !entry.getValue().isOdellaRecoverIncrease()) {
					entry.getValue().endEffect();
					abnormalEffectMap.remove(entry.getKey());
				}
			}

			for (Effect effect : effectsSnapshot(noshowEffects)) {
				effect.endEffect();
			}
			noshowEffects.clear();
		} else {
			// 登出时移除全部效果 / remove all effects on logout
			for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
				effect.endEffect();
			}
			abnormalEffectMap.clear();
			for (Effect effect : effectsSnapshot(noshowEffects)) {
				effect.endEffect();
			}
			noshowEffects.clear();
			for (Effect effect : effectsSnapshot(passiveEffectMap)) {
				effect.endEffect();
			}
			passiveEffectMap.clear();
		}
	}

	/**
	 * 异常映射中是否存在指定技能 ID。
	 * Whether the abnormal map contains the given skill id.
	 *
	 * skill id
	 *
	 * @param skillId 存在则为 true / true if present
	 */
	public boolean isAbnormalPresentBySkillId(int skillId) {
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			if (effect.getSkillId() == skillId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 不显示效果映射中是否存在指定技能 ID。
	 * Whether the no-show map contains the given skill id.
	 *
	 * skill id
	 *
	 * @param skillId 存在则为 true / true if present
	 */
	public boolean isNoshowPresentBySkillId(int skillId) {
		for (Effect effect : effectsSnapshot(noshowEffects)) {
			if (effect.getSkillId() == skillId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 被动映射中是否存在指定技能 ID。
	 * Whether the passive map contains the given skill id.
	 *
	 * skill id
	 *
	 * @param skillId 存在则为 true / true if present
	 */
	public boolean isPassivePresentBySkillId(int skillId) {
		for (Effect effect : effectsSnapshot(passiveEffectMap)) {
			if (effect.getSkillId() == skillId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否处于恐惧异常状态。
	 * Whether the creature is under a Fear abnormal state.
	 *
	 * @return true 若处于恐惧 / true if under fear
	 */
	public boolean isUnderFear() {
		return isAbnormalSet(AbnormalState.FEAR);
	}

	/**
	 * 是否处于混乱异常状态。
	 * Whether the creature is under a Confuse abnormal state.
	 *
	 * @return true 若处于混乱 / true if confused
	 */
	public boolean isConfused() {
		return isAbnormalSet(AbnormalState.CONFUSE);
	}

	/**
	 * 更新玩家效果图标（基类空实现，子类覆盖）。
	 * Updates player effect icons (no-op base; overridden by subclasses).
	 */
	public void updatePlayerEffectIcons() {
	}

	/**
	 * 实际同步玩家效果图标（基类空实现）。
	 * Actually syncs player effect icons (no-op base).
	 */
	public void updatePlayerEffectIconsImpl() {
	}

	/**
	 * 返回异常效果列表副本。
	 * Returns a copy of the abnormal effects list.
	 *
	 * @return 异常效果列表 / abnormal effects
	 */
	public List<Effect> getAbnormalEffects() {
		List<Effect> effects = new ArrayList<Effect>();
		Iterator<Effect> iterator = iterator();
		while (iterator.hasNext()) {
			Effect effect = iterator.next();
			if (effect != null) {
				effects.add(effect);
			}
		}
		return effects;
	}

	/**
	 * 返回应作为顶部图标显示的效果（排除 NOSHOW）。
	 * Returns effects to show as top icons (excludes NOSHOW).
	 *
	 * @return 可显示效果集合 / displayable effects
	 */
	public Collection<Effect> getAbnormalEffectsToShow() {
		return Collections2.filter(effectsSnapshot(abnormalEffectMap), new Predicate<Effect>() {
			@Override
			public boolean apply(Effect effect) {
				return effect.getSkillTemplate().getTargetSlot() != SkillTargetSlot.NOSHOW;
			}
		});
	}

	/**
	 * 返回当前吟唱类效果。
	 * Returns current chant effects.
	 *
	 * chant effects
	 */
	public Collection<Effect> getChantEffects() {
		return Collections2.filter(effectsSnapshot(abnormalEffectMap), new Predicate<Effect>() {
			@Override
			public boolean apply(Effect effect) {
				return effect.isChant();
			}
		});
	}

	/**
	 * 返回当前游侠增益效果。
	 * Returns current ranger buff effects.
	 *
	 * ranger effects
	 */
	public Collection<Effect> getRangerEffects() {
		return Collections2.filter(effectsSnapshot(abnormalEffectMap), new Predicate<Effect>() {
			@Override
			public boolean apply(Effect effect) {
				return effect.isRangerBuff();
			}
		});
	}

	/**
	 * 返回当前增益效果。
	 * Returns current buff effects.
	 *
	 * buff effects
	 */
	public Collection<Effect> getBuffEffects() {
		return Collections2.filter(effectsSnapshot(abnormalEffectMap), new Predicate<Effect>() {
			@Override
			public boolean apply(Effect effect) {
				return effect.isBuff();
			}
		});
	}

	/**
	 * 设置异常状态位掩码并通知观察者。
	 * Sets abnormal-state bits and notifies observers.
	 *
	 * @param mask 异常状态掩码 / abnormal state mask
	 */
	public void setAbnormal(int mask) {
		owner.getObserveController().notifyAbnormalSettedObservers(AbnormalState.getStateById(mask));
		abnormals |= mask;
	}

	/**
	 * 在无其它效果占用时清除异常状态位。
	 * Clears abnormal-state bits when no other effect still uses them.
	 *
	 * @param mask 异常状态掩码 / abnormal state mask
	 */
	public void unsetAbnormal(int mask) {
		int count = 0;
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			if ((effect.getAbnormals() & mask) == mask) {
				count++;
			}
		}
		if (count <= 1) {
			abnormals &= ~mask;
		}
	}

	/**
	 * 检查是否精确处于指定唯一异常状态。
	 * Checks whether a unique abnormal state is fully set.
	 *
	 * @param id 异常状态 / abnormal state
	 * @return 若 set 则为 true / true if set
	 */
	public boolean isAbnormalSet(AbnormalState id) {
		return (abnormals & id.getId()) == id.getId();
	}

	/**
	 * 检查是否匹配复合异常状态（部分位即可）。
	 * Checks whether a compound abnormal state matches (any subset bits).
	 *
	 * @param id 异常状态 / abnormal state
	 * @return 若 matched 则为 true / true if matched
	 */
	public boolean isAbnormalState(AbnormalState id) {
		int state = abnormals & id.getId();
		return state > 0 && state <= id.getId();
	}

	/**
	 * 返回当前异常状态位掩码。
	 * Returns the current abnormal-state bit mask.
	 *
	 * abnormal mask
	 */
	public int getAbnormals() {
		return abnormals;
	}

	/**
	 * 返回异常效果快照迭代器。
	 * Returns an iterator over a snapshot of abnormal effects.
	 *
	 * @return 效果迭代器 / effect iterator
	 */
	public Iterator<Effect> iterator() {
		return effectsSnapshot(abnormalEffectMap).iterator();
	}

	/**
	 * 根据当前效果推断变身类型。
	 * Infers transform type from current effects.
	 *
	 * transform type
	 */
	public TransformType getTransformType() {
		for (Effect eff : getAbnormalEffects()) {
			if (eff.isDeityAvatar()) {
				return TransformType.AVATAR;
			} else {
				return eff.getTransformType();
			}
		}
		return TransformType.NONE;
	}

	/**
	 * 异常效果映射是否为空。
	 * Whether the abnormal effect map is empty.
	 *
	 * @return 若 empty 则为 true / true if empty
	 */
	public boolean isEmpty() {
		return abnormalEffectMap.isEmpty();
	}

	/**
	 * 按 delayId 限制同类效果数量，必要时结束最早者。
	 * Enforces per-delayId effect caps and ends the earliest when over limit.
	 *
	 * new effect
	 */
	public void checkEffectCooldownId(Effect effect) {
		Collection<Effect> effects = this.getAbnormalEffectsToShow();
		int delayId = effect.getSkillTemplate().getDelayId();
		int rDelay = 0;
		int size = 0;
		if (delayId == 1) {
			return;
		}
		switch (delayId) {
		case 2005:
		case 2022:
		case 2024:
		case 2026:
		case 2028:
			size = 2;
			break;
		}
		rDelay = delayId;

		if (delayId == rDelay && effects.size() >= size) {
			int i = 0;
			Effect toRemove = null;
			Iterator<Effect> iter2 = effects.iterator();
			while (iter2.hasNext()) {
				Effect nextEffect = iter2.next();
				if (nextEffect.getSkillTemplate().getDelayId() == rDelay
						&& nextEffect.getTargetSlot() == effect.getTargetSlot()) {
					i++;
					if (toRemove == null) {
						toRemove = nextEffect;
					}
				}
			}
			if (i >= size && toRemove != null) {
				toRemove.endEffect();
			}
		}
	}

	/**
	 * 处理 EXTRA 驱散类别的叠层替换。
	 * Handles EXTRA dispel-category stack replacement.
	 *
	 * new effect
	 *
	 * @param effect @return true 若已替换已有效果 / true if an existing effect was replaced
	 */
	private boolean checkExtraEffect(Effect effect) {
		Effect existingEffect = getMapForEffect(effect).get(effect.getStack());
		if (existingEffect != null) {
			if (existingEffect.getDispelCategory() == DispelCategoryType.EXTRA
					&& effect.getDispelCategory() == DispelCategoryType.EXTRA) {
				existingEffect.endEffect();
				return true;
			}
		}
		return false;
	}

	/**
	 * 线程安全地复制效果值列表。
	 * Thread-safely copies effect values into a list.
	 *
	 * effect map
	 * snapshot list
	 */
	private static List<Effect> effectsSnapshot(Map<String, Effect> effects) {
		synchronized (effects) {
			return new ArrayList<Effect>(effects.values());
		}
	}

	/**
	 * 线程安全地复制效果条目列表。
	 * Thread-safely copies effect entries into a list.
	 *
	 * effect map
	 * entry snapshot
	 */
	private static List<Map.Entry<String, Effect>> effectEntriesSnapshot(Map<String, Effect> effects) {
		synchronized (effects) {
			return new ArrayList<Map.Entry<String, Effect>>(effects.entrySet());
		}
	}

	/**
	 * 搜索 effectId 冲突；低 basicLvl 被拒绝，高 basicLvl 顶替。
	 * Searches effectId conflicts; lower basicLvl is rejected, higher replaces.
	 *
	 * new effect
	 *
	 * @param nextEffect @return true 若新效果因冲突被拒绝 / true if new effect is rejected
	 */
	private boolean searchConflict(Effect nextEffect) {
		if (priorityStigmaEffect(nextEffect) || checkExtraEffect(nextEffect)) {
			return false;
		}
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			if (effect.getSkillSubType().equals(nextEffect.getSkillSubType())
					|| effect.getTargetSlotEnum().equals(nextEffect.getTargetSlotEnum())) {
				for (EffectTemplate et : effect.getEffectTemplates()) {
					if (et.getEffectid() == 0) {
						continue;
					}
					for (EffectTemplate et2 : nextEffect.getEffectTemplates()) {
						if (et2.getEffectid() == 0) {
							continue;
						}
						if (et.getEffectid() == et2.getEffectid()) {
							if (et.getBasicLvl() > et2.getBasicLvl()) {
								if (nextEffect.getTargetSlotEnum() != SkillTargetSlot.DEBUFF) {
									nextEffect.setEffectResult(EffectResult.CONFLICT);
								}
								return true;
							} else {
								effect.endEffect();
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * 高优先级 Stigma 效果顶替同槽低优先级效果。
	 * Higher-priority stigma effects replace lower ones in the same slot.
	 *
	 * new effect
	 *
	 * @param nextEffect 若 a lower effect was replaced 则为 true / true if a lower effect was replaced
	 */
	private boolean priorityStigmaEffect(Effect nextEffect) {
		for (Effect effect : effectsSnapshot(abnormalEffectMap)) {
			if (effect.getSkillTemplate().getStigmaType().getId() < nextEffect.getSkillTemplate().getStigmaType()
					.getId() && effect.getTargetSlot() == nextEffect.getTargetSlot()
					&& effect.getTargetSlotLevel() == nextEffect.getTargetSlotLevel()) {
				for (EffectTemplate et : effect.getEffectTemplates()) {
					if (et.getEffectid() == 0) {
						continue;
					}
					for (EffectTemplate et2 : nextEffect.getEffectTemplates()) {
						if (et2.getEffectid() == 0) {
							continue;
						}
						if (et.getEffectid() == et2.getEffectid()) {
							effect.endEffect();
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * 是否存在物理控制类异常效果。
	 * Whether a physical-state abnormal effect is present.
	 *
	 * @return 存在则为 true / true if present
	 */
	public boolean hasPhysicalStateEffect() {
		Iterator<Effect> effectIterator = effectsSnapshot(abnormalEffectMap).iterator();
		while (effectIterator.hasNext()) {
			Effect localEffect = effectIterator.next();
			if (localEffect.isPhysicalState()) {
				return true;
			}
		}
		return false;
	}
}

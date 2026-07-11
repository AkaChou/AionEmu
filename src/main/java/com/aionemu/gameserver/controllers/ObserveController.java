package com.aionemu.gameserver.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import com.aionemu.gameserver.controllers.attack.AttackResult;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.controllers.observer.AttackerCriticalStatus;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 行为观察控制器，管理一次性/常驻观察者及攻击计算观察者。
 * Action observe controller managing one-shot/persistent observers and attack-calc observers.
 * <p>
 * Notes: onceUsedObservers 需加锁；迭代前检查集合大小以减少内存分配。
 * Notes: lock onceUsedObservers; check collection size before iteration to reduce allocations.
 *
 * @author ATracer
 * @author Cura
 */
public class ObserveController {

	/** 保护一次性观察者列表的锁。 / Lock protecting the one-shot observer list. */
	private ReentrantLock lock = new ReentrantLock();
	/** 常驻行为观察者集合。 / Persistent action observers. */
	protected Collection<ActionObserver> observers = new CopyOnWriteArrayList<ActionObserver>();
	/** 一次性行为观察者列表。 / One-shot action observers. */
	protected List<ActionObserver> onceUsedObservers = new ArrayList<ActionObserver>(0);
	/** 攻击计算观察者集合。 / Attack calculation observers. */
	protected Collection<AttackCalcObserver> attackCalcObservers = new CopyOnWriteArrayList<AttackCalcObserver>();

	/**
	 * 附加一次性观察者，通知后移除。
	 * Attaches a one-shot observer that is removed after notification.
	 *
	 * observer
	 */
	public void attach(ActionObserver observer) {
		observer.makeOneTimeUse();
		lock.lock();
		try {
			onceUsedObservers.add(observer);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 添加常驻行为观察者。
	 * Adds a persistent action observer.
	 *
	 * observer
	 */
	public void addObserver(ActionObserver observer) {
		observers.add(observer);
	}

	/**
	 * 添加攻击计算观察者。
	 * Adds an attack-calculation observer.
	 *
	 * observer
	 */
	public void addAttackCalcObserver(AttackCalcObserver observer) {
		attackCalcObservers.add(observer);
	}

	/**
	 * 移除行为观察者（含一次性列表）。
	 * Removes an action observer (including from the one-shot list).
	 *
	 * observer
	 */
	public void removeObserver(ActionObserver observer) {
		if (observer == null) {
			return;
		}
		boolean removed = observers.remove(observer);
		lock.lock();
		try {
			removed |= onceUsedObservers.remove(observer);
		} finally {
			lock.unlock();
		}
		if (removed) {
			observer.onRemoved();
		}
	}

	/**
	 * 移除攻击计算观察者。
	 * Removes an attack-calculation observer.
	 *
	 * observer
	 */
	public void removeAttackCalcObserver(AttackCalcObserver observer) {
		attackCalcObservers.remove(observer);
	}

	/**
	 * 按类型通知所有匹配的观察者。
	 * Notifies all matching observers of the given type.
	 *
	 * @param type 观察类型 / observer type
	 * notification arguments
	 */
	public void notifyObservers(ObserverType type, Object... object) {
		List<ActionObserver> tempOnceused = Collections.emptyList();
		lock.lock();
		try {
			if (onceUsedObservers.size() > 0) {
				tempOnceused = new ArrayList<ActionObserver>();
				Iterator<ActionObserver> iterator = onceUsedObservers.iterator();
				while (iterator.hasNext()) {
					ActionObserver observer = iterator.next();
					if (observer.getObserverType().matchesObserver(type)) {
						if (observer.tryUse()) {
							tempOnceused.add(observer);
							iterator.remove();
						}
					}
				}
			}
		} finally {
			lock.unlock();
		}

		// 在锁外通知 / notify outside of lock
		for (ActionObserver observer : tempOnceused) {
			notifyAction(type, observer, object);
			observer.onRemoved();
		}

		if (observers.size() > 0) {
			for (ActionObserver observer : observers) {
				if (observer.getObserverType().matchesObserver(type)) {
					notifyAction(type, observer, object);
				}
			}
		}
	}

	/**
	 * 根据类型分发到观察者对应回调。
	 * Dispatches to the observer callback matching the type.
	 *
	 * @param type 观察类型 / observer type
	 * @param observer 目标观察者 / target observer
	 * notification arguments
	 */
	private void notifyAction(ObserverType type, ActionObserver observer, Object... object) {
		switch (type) {
		case ATTACK:
			observer.attack((Creature) object[0]);
			break;
		case ATTACKED:
			observer.attacked((Creature) object[0]);
			break;
		case DEATH:
			observer.died((Creature) object[0]);
			break;
		case EQUIP:
			observer.equip((Item) object[0], (Player) object[1]);
			break;
		case UNEQUIP:
			observer.unequip((Item) object[0], (Player) object[1]);
			break;
		case MOVE:
			observer.moved();
			break;
		case SKILLUSE:
			observer.skilluse((Skill) object[0]);
			break;
		case DOT_ATTACKED:
			observer.dotattacked((Creature) object[0], (Effect) object[1]);
			break;
		case ITEMUSE:
			observer.itemused((Item) object[0]);
			break;
		case NPCDIALOGREQUEST:
			observer.npcdialogrequested((Npc) object[0]);
			break;
		case ABNORMALSETTED:
			observer.abnormalsetted((AbnormalState) object[0]);
			break;
		case SUMMONRELEASE:
			observer.summonrelease();
			break;
		case LIFE_CHANGED:
			observer.lifeChanged((HealType) object[0], (Integer) object[1]);
			break;
		default:
			break;
		}
	}

	/**
	 * 通知死亡观察者。
	 * Notifies death observers.
	 *
	 * @param creature 死亡的生物 / the creature that died
	 */
	public void notifyDeathObservers(Creature creature) {
		notifyObservers(ObserverType.DEATH, creature);
	}

	/**
	 * 通知移动观察者。
	 * Notifies move observers.
	 */
	public void notifyMoveObservers() {
		notifyObservers(ObserverType.MOVE);
	}

	/**
	 * 通知攻击观察者。
	 * Notifies attack observers.
	 *
	 * @param creature 被攻击目标 / the attacked creature
	 */
	public void notifyAttackObservers(Creature creature) {
		notifyObservers(ObserverType.ATTACK, creature);
	}

	/**
	 * 通知被攻击观察者。
	 * Notifies attacked observers.
	 *
	 * the attacker
	 */
	public void notifyAttackedObservers(Creature creature) {
		notifyObservers(ObserverType.ATTACKED, creature);
	}

	/**
	 * 通知 DoT 命中观察者。
	 * Notifies DoT-hit observers.
	 *
	 * the attacker
	 * related effect
	 */
	public void notifyDotAttackedObservers(Creature creature, Effect effect) {
		notifyObservers(ObserverType.DOT_ATTACKED, creature, effect);
	}

	/**
	 * 通知技能使用观察者。
	 * Notifies skill-use observers.
	 *
	 * @param skill 使用的技能 / skill used
	 */
	public void notifySkilluseObservers(Skill skill) {
		notifyObservers(ObserverType.SKILLUSE, skill);
	}

	/**
	 * 通知生命或魔法值变化观察者。
	 * Notifies HP/MP-change observers.
	 */
	public void notifyLifeChangedObservers(HealType type, int value) {
		notifyObservers(ObserverType.LIFE_CHANGED, type, value);
	}

	/**
	 * 通知装备物品观察者。
	 * Notifies item-equip observers.
	 *
	 * @param item 装备物品 / equipped item
	 * equipping player
	 */
	public void notifyItemEquip(Item item, Player owner) {
		notifyObservers(ObserverType.EQUIP, item, owner);
	}

	/**
	 * 通知卸下物品观察者。
	 * Notifies item-unequip observers.
	 *
	 * @param item 卸下物品 / unequipped item
	 * unequipping player
	 */
	public void notifyItemUnEquip(Item item, Player owner) {
		notifyObservers(ObserverType.UNEQUIP, item, owner);
	}

	/**
	 * 通知物品使用观察者。
	 * Notifies item-use observers.
	 *
	 * @param item 使用的物品 / used item
	 */
	public void notifyItemuseObservers(Item item) {
		notifyObservers(ObserverType.ITEMUSE, item);
	}

	/**
	 * 通知 NPC 对话请求观察者。
	 * Notifies NPC dialog-request observers.
	 *
	 * dialog NPC
	 */
	public void notifyRequestDialogObservers(Npc npc) {
		notifyObservers(ObserverType.NPCDIALOGREQUEST, npc);
	}

	/**
	 * 通知异常状态设置观察者。
	 * Notifies abnormal-state-set observers.
	 *
	 * @param state 异常状态 / abnormal state
	 */
	public void notifyAbnormalSettedObservers(AbnormalState state) {
		notifyObservers(ObserverType.ABNORMALSETTED, state);
	}

	/**
	 * 通知召唤物解除观察者。
	 * Notifies summon-release observers.
	 */
	public void notifySummonReleaseObservers() {
		notifyObservers(ObserverType.SUMMONRELEASE);
	}

	/**
	 * 检查攻击状态是否被观察者改写。
	 * Checks whether an attack status is rewritten by observers.
	 *
	 * attack status
	 *
	 * @param status @return 任一观察者匹配则为 true / true if any observer matches
	 */
	public boolean checkAttackStatus(AttackStatus status) {
		if (attackCalcObservers.size() > 0) {
			for (AttackCalcObserver observer : attackCalcObservers) {
				if (observer.checkStatus(status)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 检查攻击者状态是否被观察者改写。
	 * Checks whether an attacker status is rewritten by observers.
	 *
	 * attack status
	 *
	 * @param status @return 任一观察者匹配则为 true / true if any observer matches
	 */
	public boolean checkAttackerStatus(AttackStatus status) {
		if (attackCalcObservers.size() > 0) {
			for (AttackCalcObserver observer : attackCalcObservers) {
				if (observer.checkAttackerStatus(status)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 检查攻击者暴击状态。
	 * Checks attacker critical status.
	 *
	 * attack status
	 *
	 * @param isSkill 是否技能攻击 / whether it is a skill attack
	 * @param isSkill @return 暴击状态结果 / critical status result
	 */
	public AttackerCriticalStatus checkAttackerCriticalStatus(AttackStatus status, boolean isSkill) {
		if (attackCalcObservers.size() > 0) {
			for (AttackCalcObserver observer : attackCalcObservers) {
				AttackerCriticalStatus acStatus = observer.checkAttackerCriticalStatus(status, isSkill);
				if (acStatus.isResult()) {
					return acStatus;
				}
			}
		}
		return new AttackerCriticalStatus(false);
	}

	/**
	 * 通过观察者处理护盾减免。
	 * Processes shield mitigation via observers.
	 *
	 * @param attackList 攻击结果列表 / attack result list
	 * related effect
	 * attacker
	 */
	public void checkShieldStatus(List<AttackResult> attackList, Effect effect, Creature attacker) {
		if (attackCalcObservers.size() > 0) {
			for (AttackCalcObserver observer : attackCalcObservers) {
				observer.checkShield(attackList, effect, attacker);
			}
		}
	}

	/**
	 * 获取基础物理伤害倍率（各观察者相乘）。
	 * Gets the base physical damage multiplier (product of observers).
	 *
	 * @param isSkill 是否技能攻击 / whether it is a skill attack
	 * damage multiplier
	 */
	public float getBasePhysicalDamageMultiplier(boolean isSkill) {
		float multiplier = 1;
		if (attackCalcObservers.size() > 0) {
			for (AttackCalcObserver observer : attackCalcObservers) {
				multiplier *= observer.getBasePhysicalDamageMultiplier(isSkill);
			}
		}
		return multiplier;
	}

	/**
	 * 获取基础魔法伤害倍率（各观察者相乘）。
	 * Gets the base magical damage multiplier (product of observers).
	 *
	 * damage multiplier
	 */
	public float getBaseMagicalDamageMultiplier() {
		float multiplier = 1;
		if (attackCalcObservers.size() > 0) {
			for (AttackCalcObserver observer : attackCalcObservers) {
				multiplier *= observer.getBaseMagicalDamageMultiplier();
			}
		}
		return multiplier;
	}

	/**
	 * 清空所有观察者。
	 * Clears all observers.
	 */
	public void clear() {
		lock.lock();
		try {
			onceUsedObservers.clear();
		} finally {
			lock.unlock();
		}
		observers.clear();
		attackCalcObservers.clear();
	}
}

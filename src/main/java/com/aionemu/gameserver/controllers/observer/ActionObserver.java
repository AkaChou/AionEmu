package com.aionemu.gameserver.controllers.observer;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 动作观察者基类，响应移动、攻击、装备、技能等事件。
 * Base action observer responding to move, attack, equip, skill and related events.
 *
 * @author ATracer
 */
public class ActionObserver {

	/** 一次性使用标记 / One-time use flag */
	private AtomicBoolean used;

	/** 观察者类型 / Observer type */
	private ObserverType observerType;

	/**
	 * @param observerType 观察者类型 / observer type
	 */
	public ActionObserver(ObserverType observerType) {
		this.observerType = observerType;
	}

	/**
	 * 将本观察者设为仅可使用一次。
	 * Make this observer usable exactly one time.
	 */
	public void makeOneTimeUse() {
		used = new AtomicBoolean(false);
	}

	/**
	 * 尝试使用本观察者，仅首次返回 true。
	 * Try to use this observer; returns true only once.
	 *
	 * @return 是否成功占用 / whether successfully claimed
	 */
	public boolean tryUse() {
		return used.compareAndSet(false, true);
	}

	/**
	 * 获取观察者类型。
	 * Get observer type.
	 *
	 * @return 观察者类型 / observer type
	 */
	public ObserverType getObserverType() {
		return observerType;
	}

	/**
	 * 生物移动时回调。
	 * Callback when the creature moves.
	 */
	public void moved() {
	};

	/**
	 * 受到攻击时回调。
	 * Callback when attacked.
	 *
	 * @param creature 攻击者 / attacker
	 */
	public void attacked(Creature creature) {
	};

	/**
	 * 受到物理或魔法攻击时回调；旧观察者继续走无类型重载。
	 * Callback with the incoming attack category; legacy observers use the untyped overload.
	 */
	public void attacked(Creature creature, boolean magical) {
		attacked(creature);
	};

	/**
	 * 主动攻击时回调。
	 * Callback when attacking.
	 *
	 * @param creature 攻击目标 / target
	 */
	public void attack(Creature creature) {
	};

	/**
	 * 主动攻击时回调并携带当前技能 ID；普通攻击的技能 ID 为 0。
	 * Callback with the current skill id; normal attacks use skill id 0.
	 */
	public void attack(Creature creature, int skillId) {
		attack(creature);
	};

	/**
	 * 装备物品时回调。
	 * Callback when equipping an item.
	 *
	 * @param item 物品 / item
	 * @param owner 装备者 / owner
	 */
	public void equip(Item item, Player owner) {
	};

	/**
	 * 卸下物品时回调。
	 * Callback when unequipping an item.
	 *
	 * @param item 物品 / item
	 * @param owner 卸下者 / owner
	 */
	public void unequip(Item item, Player owner) {
	};

	/**
	 * 使用技能时回调。
	 * Callback when using a skill.
	 *
	 * @param skill 使用的技能 / used skill
	 */
	public void skilluse(Skill skill) {
	};

	/**
	 * 死亡时回调。
	 * Callback on death.
	 *
	 * @param creature 已死亡的生物 / dead creature
	 */
	public void died(Creature creature) {
	};

	/**
	 * 受到持续伤害时回调。
	 * Callback when hit by a DoT.
	 *
	 * @param creature 伤害来源生物 / source creature
	 * @param dotEffect 持续伤害效果 / DoT effect
	 */
	public void dotattacked(Creature creature, Effect dotEffect) {
	};

	/**
	 * 使用物品时回调。
	 * Callback when an item is used.
	 *
	 * @param item 使用的物品 / used item
	 */
	public void itemused(Item item) {
	};

	/**
	 * 请求 NPC 对话时回调。
	 * Callback when an NPC dialog is requested.
	 *
	 * @param npc NPC
	 */
	public void npcdialogrequested(Npc npc) {
	};

	/**
	 * 异常状态被设置时回调。
	 * Callback when an abnormal state is set.
	 *
	 * @param state 异常状态 / abnormal state
	 */
	public void abnormalsetted(AbnormalState state) {
	};

	/**
	 * 召唤物被释放时回调。
	 * Callback when a summon is released.
	 */
	public void summonrelease() {
	};

	/**
	 * 生命或魔法值变化时回调。
	 * Callback when HP or MP changes.
	 */
	public void lifeChanged(HealType type, int value) {
	};

	/**
	 * 观察者被移除时回调。
	 * Callback when the observer is removed.
	 */
	public void onRemoved() {
	};
}

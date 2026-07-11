package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 物品使用观察者：攻击、移动、技能等打断动作时中止使用。
 * Item-use observer: aborts use on interrupt actions such as attack, move, or skill.
 */
public abstract class ItemUseObserver extends ActionObserver {

	/**
	 * 创建监听全部事件的物品使用观察者。
	 * Create an item-use observer listening to all events.
	 */
	public ItemUseObserver() {
		super(ObserverType.ALL);
	}

	@Override
	public final void attack(Creature creature) {
		abort();
	}

	@Override
	public final void attacked(Creature creature) {
		abort();
	}

	@Override
	public final void died(Creature creature) {
		abort();
	}

	@Override
	public final void dotattacked(Creature creature, Effect dotEffect) {
		abort();
	}

	@Override
	public final void equip(Item item, Player owner) {
		abort();
	}

	@Override
	public final void moved() {
		abort();
	}

	@Override
	public final void skilluse(Skill skill) {
		abort();
	}

	/**
	 * 中止物品使用。
	 * Abort item use.
	 */
	public abstract void abort();
}

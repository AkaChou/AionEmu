package com.aionemu.gameserver.world.knownlist;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;

/**
 * 仅感知 {@link Creature} 的已知列表。
 * Known list that is only aware of {@link Creature} objects.
 *
 * @author ATracer
 */
public class CreatureAwareKnownList extends KnownList {

	/**
	 * 创建仅感知生物的已知列表。
	 * Creates a creature-aware known list.
	 *
	 * @param owner 列表所有者 / list owner
	 */
	public CreatureAwareKnownList(VisibleObject owner) {
		super(owner);
	}

	/**
	 * {@inheritDoc}
	 *
	 * 仅当对象为 {@link Creature} 时返回 {@code true}。
	 * Returns {@code true} only when the object is a {@link Creature}.
	 */
	@Override
	protected final boolean isAwareOf(VisibleObject newObject) {
		return newObject instanceof Creature;
	}
}

package com.aionemu.gameserver.world.knownlist;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 仅感知 {@link Player} 的已知列表。
 * Known list that is only aware of {@link Player} objects.
 *
 * @author ATracer
 */
public class PlayerAwareKnownList extends KnownList {

	/**
	 * 创建仅感知玩家的已知列表。
	 * Creates a player-aware known list.
	 *
	 * @param owner 列表所有者 / list owner
	 */
	public PlayerAwareKnownList(VisibleObject owner) {
		super(owner);
	}

	/**
	 * {@inheritDoc}
	 *
	 * 仅当对象为 {@link Player} 时返回 {@code true}。
	 * Returns {@code true} only when the object is a {@link Player}.
	 */
	@Override
	protected final boolean isAwareOf(VisibleObject newObject) {
		return newObject instanceof Player;
	}
}

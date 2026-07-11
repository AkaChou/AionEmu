package com.aionemu.gameserver.model.drop;

import java.util.Collection;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 掉落 Calculator 接口。
 * Drop Calculator interface.
 *
 * @author MrPoke
 */
public interface DropCalculator {
	int dropCalculator(Set<DropItem> result, int index, DropModifiers dropModifiers, Collection<Player> groupMembers);
}

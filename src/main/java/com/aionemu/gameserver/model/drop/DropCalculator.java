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
	/**
	 * 执行一次掉落计算：将生成的掉落物写入结果集并返回下一个可用索引。
	 * Runs a drop calculation: writes produced drops into the result set and returns the next free index.
	 *
	 * @param result 掉落物结果集 / drop-item result set
	 * @param index 当前索引 / current index
	 * @param dropModifiers 掉落修正器 / drop modifiers
	 * @param groupMembers 队伍成员（用于每人一份的掉落）/ group members (for each-member drops)
	 * @return 下一个可用索引 / the next available index
	 */
	int dropCalculator(Set<DropItem> result, int index, DropModifiers dropModifiers, Collection<Player> groupMembers);
}

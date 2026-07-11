package com.aionemu.gameserver.model.gameobjects.player;

import java.util.Iterator;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Block 列表。
 * Block List game object.
 */

public class BlockList implements Iterable<BlockedPlayer> {
	public static final int MAX_BLOCKS = 10;

	private final Map<Integer, BlockedPlayer> blockedList;

	public BlockList() {
		this.blockedList = new ConcurrentHashMap<>();
	}

	public BlockList(Map<Integer, BlockedPlayer> initialList) {
		this.blockedList = new ConcurrentHashMap<>(initialList);
	}

	/** 添加。 / Add. */
	public void add(BlockedPlayer plr) {
		blockedList.put(plr.getObjId(), plr);
	}

	/** 移除。 / Remove. */
	public void remove(int objIdOfPlayer) {
		blockedList.remove(objIdOfPlayer);
	}

	/** 返回 blocked player / Returns the blocked player */
	public BlockedPlayer getBlockedPlayer(String name) {
		Iterator<BlockedPlayer> iterator = blockedList.values().iterator();
		while (iterator.hasNext()) {
			BlockedPlayer entry = iterator.next();
			if (entry.getName().equalsIgnoreCase(name)) {
				return entry;
			}
		}
		return null;
	}

	/** 返回 blocked player / Returns the blocked player */
	public BlockedPlayer getBlockedPlayer(int playerObjId) {
		return blockedList.get(playerObjId);
	}

	/** 是否包含。 / Contains. */
	public boolean contains(int playerObjectId) {
		return blockedList.containsKey(playerObjectId);
	}

	/** 返回大小 / Returns the size*/
	public int getSize() {
		return blockedList.size();
	}

	/** 是否已满。 / Whether Full. */
	public boolean isFull() {
		return getSize() >= MAX_BLOCKS;
	}

	/** 返回迭代器。 / Returns iterator. */
	@Override
	public Iterator<BlockedPlayer> iterator() {
		return blockedList.values().iterator();
	}
}

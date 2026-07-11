package com.aionemu.gameserver.world.container;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 玩家容器：按 objectId 与名称双向索引在线玩家。
 * Container for storing online players by objectId and name.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class PlayerContainer implements Iterable<Player> {


	/**
	 * 按 objectId 索引的玩家 / Players indexed by objectId
	 */
	private final Map<Integer, Player> playersById = new LinkedHashMap<Integer, Player>();

	/**
	 * 按名称索引的玩家 / Players indexed by name
	 */
	private final Map<String, Player> playersByName = new LinkedHashMap<String, Player>();

	/**
	 * 添加玩家；objectId 或名称冲突时抛出 {@link DuplicateAionObjectException}。
	 * Adds a player; throws {@link DuplicateAionObjectException} on objectId or name conflict.
	 *
	 * @param player 待添加玩家 / player to add
	 */
	public synchronized void add(Player player) {
		if (playersById.containsKey(player.getObjectId()) || playersByName.containsKey(player.getName())) {
			throw new DuplicateAionObjectException();
		}
		playersById.put(player.getObjectId(), player);
		playersByName.put(player.getName(), player);
	}

	/**
	 * 从容器中移除玩家。
	 * Removes the player from this container.
	 *
	 * @param player 待移除玩家 / player to remove
	 */
	public synchronized void remove(Player player) {
		playersById.remove(player.getObjectId());
		playersByName.remove(player.getName());
	}

	/**
	 * 按 objectId 获取玩家。
	 * Returns the player with the given objectId.
	 *
	 * player objectId
	 *
	 * @param objectId @return 玩家实例；未登录则返回 null / player, or null if not logged in
	 */
	public synchronized Player get(int objectId) {
		return playersById.get(objectId);
	}

	/**
	 * 按名称获取玩家。
	 * Returns the player with the given name.
	 *
	 * @param name 玩家名称 / player name
	 * @return 玩家实例；未登录则返回 null / player, or null if not logged in
	 */
	public synchronized Player get(String name) {
		return playersByName.get(name);
	}

	/**
	 * 返回在线玩家的快照迭代器。
	 * Returns an iterator over a snapshot of online players.
	 *
	 * @return 玩家迭代器 / player iterator
	 */
	@Override
	public Iterator<Player> iterator() {
		return playersSnapshot().iterator();
	}

	/**
	 * 对所有在线玩家执行访问者逻辑；异常会被捕获并记录日志。
	 * Visits all online players; exceptions are caught and logged.
	 *
	 * @param visitor 玩家访问者 / player visitor
	 */
	@SuppressWarnings("unused")
	public void doOnAllPlayers(Visitor<Player> visitor) {
		try {
			for (Player player : playersSnapshot()) {
				if (player != null) {
					visitor.visit(player);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.cc03391ccf0f", ex));
		}
	}

	/**
	 * 返回所有在线玩家的快照集合。
	 * Returns a snapshot collection of all online players.
	 *
	 * @return 玩家集合副本 / copy of the player collection
	 */
	public Collection<Player> getAllPlayers() {
		return playersSnapshot();
	}

	/**
	 * 创建当前在线玩家的快照列表。
	 * Creates a snapshot list of currently online players.
	 *
	 * @return 玩家列表副本 / copy of the player list
	 */
	private synchronized List<Player> playersSnapshot() {
		return new ArrayList<Player>(playersById.values());
	}
}

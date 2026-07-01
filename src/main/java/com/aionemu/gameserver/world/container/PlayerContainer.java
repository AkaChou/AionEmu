/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.world.container;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Container for storing Players by objectId and name.
 * 
 * @author -Nemesiss-
 */
@Slf4j
public class PlayerContainer implements Iterable<Player> {


	/**
	 * Map<ObjectId,Player>
	 */
	private final Map<Integer, Player> playersById = Collections.synchronizedMap(new LinkedHashMap<Integer, Player>());
	/**
	 * Map<Name,Player>
	 */
	private final Map<String, Player> playersByName = Collections.synchronizedMap(new LinkedHashMap<String, Player>());

	/**
	 * Add Player to this Container.
	 * 
	 * @param player
	 */
	public void add(Player player) {
		if (playersById.put(player.getObjectId(), player) != null) {
			throw new DuplicateAionObjectException();
		}
		if (playersByName.put(player.getName(), player) != null) {
			throw new DuplicateAionObjectException();
		}
	}

	/**
	 * Remove Player from this Container.
	 * 
	 * @param player
	 */
	public void remove(Player player) {
		playersById.remove(player.getObjectId());
		playersByName.remove(player.getName());
	}

	/**
	 * Get Player object by objectId.
	 * 
	 * @param objectId - ObjectId of player.
	 * @return Player with given ojectId or null if Player with given objectId is
	 *         not logged.
	 */
	public Player get(int objectId) {
		return playersById.get(objectId);
	}

	/**
	 * Get Player object by name.
	 * 
	 * @param name - name of player
	 * @return Player with given name or null if Player with given name is not
	 *         logged.
	 */
	public Player get(String name) {
		return playersByName.get(name);
	}

	@Override
	public Iterator<Player> iterator() {
		return playersSnapshot().iterator();
	}

	/**
	 * @param visitor
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
			log.error("Exception when running visitor on all players" + ex);
		}
	}

	public Collection<Player> getAllPlayers() {
		return playersSnapshot();
	}

	private List<Player> playersSnapshot() {
		synchronized (playersById) {
			return new ArrayList<Player>(playersById.values());
		}
	}
}

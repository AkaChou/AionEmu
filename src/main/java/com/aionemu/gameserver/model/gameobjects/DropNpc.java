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
package com.aionemu.gameserver.model.gameobjects;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamMember;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;

/**
 * @author Simple
 */
public class DropNpc {

	private final int objectId;
	private Set<Integer> allowedLooters = new HashSet<>();
	private Collection<Player> inRangePlayers = new ArrayList<Player>();
	private Collection<Player> playerStatus = new ArrayList<Player>();
	private Player lootingPlayer = null;
	private int distributionId = 0;
	private boolean distributionType;
	private int currentIndex = 0;
	private WeakReference<TemporaryPlayerTeam<? extends TeamMember<Player>>> lootingTeam;
	private int lootingTeamId;
	private int maxRoll;
	private LootGroupRules lastLootGroupRules;
	private boolean isFreeForAll = false;
	private long remainingDecayTime;

	public DropNpc(int objectId) {
		this.objectId = objectId;
	}

	public void setAllowedLooters(Set<Integer> allowedLooters) {
		this.allowedLooters = allowedLooters;
	}

	public void setAllowedLooter(Player player) {
		allowedLooters.add(player.getObjectId());
	}

	public Set<Integer> getAllowedLooters() {
		return allowedLooters;
	}

	public boolean isAllowedToLoot(Player player) {
		return isFreeForAll || allowedLooters.contains(player.getObjectId());
	}

	/**
	 * @param player the lootingPlayer to set
	 */
	public void setLootingPlayer(Player player) {
		this.lootingPlayer = player;
	}

	/**
	 * @return lootingPlayer
	 */
	public Player getLootingPlayer() {
		return lootingPlayer;
	}

	/**
	 * @return the beingLooted
	 */
	public boolean isBeingLooted() {
		return lootingPlayer != null;
	}

	/**
	 * @param distributionId
	 */
	public void setDistributionId(int distributionId) {
		this.distributionId = distributionId;
	}

	/**
	 * @return the DistributionId
	 */
	public int getDistributionId() {
		return distributionId;
	}

	/**
	 * @param distributionType
	 */
	public void setDistributionType(boolean distributionType) {
		this.distributionType = distributionType;
	}

	/**
	 * @return the DistributionType
	 */
	public boolean getDistributionType() {
		return distributionType;
	}

	/**
	 * @param currentIndex
	 */
	public void setCurrentIndex(int currentIndex) {
		this.currentIndex = currentIndex;
	}

	/**
	 * @return currentIndex
	 */
	public int getCurrentIndex() {
		return currentIndex;
	}

	public int getLootingTeamId() {
		return lootingTeamId;
	}

	public int getMaxRoll() {
		return maxRoll;
	}

	public LootGroupRules getLootGroupRules() {
		TemporaryPlayerTeam<? extends TeamMember<Player>> team = lootingTeam == null ? null : lootingTeam.get();
		if (team != null) {
			lastLootGroupRules = team.getLootGroupRules();
		}
		return lastLootGroupRules;
	}

	public void setLootingTeam(TemporaryPlayerTeam<? extends TeamMember<Player>> team) {
		lootingTeam = new WeakReference<>(team);
		lootingTeamId = team.getTeamId();
		maxRoll = team instanceof PlayerAlliance alliance && alliance.isInLeague() ? 10000
				: team instanceof PlayerAlliance ? 1000 : 100;
		lastLootGroupRules = team.getLootGroupRules();
	}

	public void setInRangePlayers(Collection<Player> inRangePlayers) {
		this.inRangePlayers = inRangePlayers;
	}

	/**
	 * @return the inRangePlayers
	 */
	public Collection<Player> getInRangePlayers() {
		return inRangePlayers;
	}

	/**
	 * @param player
	 */
	public void addPlayerStatus(Player player) {
		playerStatus.add(player);
	}

	/**
	 * @param player
	 */
	public void delPlayerStatus(Player player) {
		playerStatus.remove(player);
	}

	/**
	 * @return the playerStatus
	 */
	public Collection<Player> getPlayerStatus() {
		return playerStatus;
	}

	/**
	 * @return true if player is found in list
	 */
	public boolean containsPlayerStatus(Player player) {
		return playerStatus.contains(player);
	}

	/**
	 * @return isFreeForAll.
	 */
	public boolean isFreeForAll() {
		return isFreeForAll;
	}

	public void startFreeForAll() {
		isFreeForAll = true;
		distributionId = 0;
		allowedLooters.clear();
	}

	public final int getObjectId() {
		return objectId;
	}

	public long getRemainingDecayTime() {
		return remainingDecayTime;
	}

	public void setRemainingDecayTime(long remainingDecayTime) {
		this.remainingDecayTime = remainingDecayTime;
	}
}

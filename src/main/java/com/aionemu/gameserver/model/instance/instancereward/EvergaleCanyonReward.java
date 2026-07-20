package com.aionemu.gameserver.model.instance.instancereward;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.instance.playerreward.EvergaleCanyonPlayerReward;
import com.aionemu.gameserver.services.teleport.TeleportService2;

public class EvergaleCanyonReward extends InstanceReward<EvergaleCanyonPlayerReward> {
	private static final Point3D ASMODIAN_START = new Point3D(1094.0016f, 752.5455f, 336.30457f);
	private static final Point3D ELYOS_START = new Point3D(402.67786f, 751.9347f, 336.30457f);

	private final MutableInt asmodianPoints = new MutableInt();
	private final MutableInt elyosPoints = new MutableInt();
	private final MutableInt asmodianPvpKills = new MutableInt();
	private final MutableInt elyosPvpKills = new MutableInt();
	private final byte buffId = 10;
	private Race winnerRace;

	public EvergaleCanyonReward(Integer mapId, int instanceId) {
		super(mapId, instanceId);
	}

	public List<EvergaleCanyonPlayerReward> sortPoints() {
		return RewardCollections.sortedByScoreDescending(getInstanceRewards(),
			EvergaleCanyonPlayerReward::getScorePoints);
	}

	public void portToPosition(Player player) {
		Point3D position = player.getRace() == Race.ASMODIANS ? ASMODIAN_START : ELYOS_START;
		TeleportService2.teleportTo(player, mapId, instanceId, position.getX(), position.getY(), position.getZ());
	}

	public MutableInt getPointsByRace(Race race) {
		return race == Race.ELYOS ? elyosPoints : race == Race.ASMODIANS ? asmodianPoints : null;
	}

	public void addPointsByRace(Race race, int points) {
		MutableInt racePoints = getPointsByRace(race);
		racePoints.add(points);
		if (racePoints.intValue() < 0) {
			racePoints.setValue(0);
		}
	}

	public MutableInt getPvpKillsByRace(Race race) {
		return race == Race.ELYOS ? elyosPvpKills : race == Race.ASMODIANS ? asmodianPvpKills : null;
	}

	public void addPvpKillsByRace(Race race, int points) {
		MutableInt raceKills = getPvpKillsByRace(race);
		raceKills.add(points);
		if (raceKills.intValue() < 0) {
			raceKills.setValue(0);
		}
	}

	public void setWinnerRace(Race race) {
		winnerRace = race;
	}

	public Race getWinnerRace() {
		return winnerRace;
	}

	public byte getBuffId() {
		return buffId;
	}

	@Override
	public EvergaleCanyonPlayerReward getPlayerReward(Integer objectId) {
		return (EvergaleCanyonPlayerReward) super.getPlayerReward(objectId);
	}
}

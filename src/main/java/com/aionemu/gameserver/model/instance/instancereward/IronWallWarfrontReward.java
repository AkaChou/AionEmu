package com.aionemu.gameserver.model.instance.instancereward;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.instance.playerreward.IronWallWarfrontPlayerReward;
import com.aionemu.gameserver.services.teleport.TeleportService2;

public class IronWallWarfrontReward extends InstanceReward<IronWallWarfrontPlayerReward> {
	private final MutableInt asmodianPoints = new MutableInt();
	private final MutableInt elyosPoints = new MutableInt();
	private final MutableInt asmodianPvpKills = new MutableInt();
	private final MutableInt elyosPvpKills = new MutableInt();
	private final Point3D asmodianStartPosition = new Point3D(282.16364f, 390.80576f, 238.60538f);
	private final Point3D elyosStartPosition = new Point3D(711.2616f, 630.29364f, 211.9369f);
	private final byte buffId = 12;
	private Race winnerRace;

	public IronWallWarfrontReward(Integer mapId, int instanceId) {
		super(mapId, instanceId);
	}

	public List<IronWallWarfrontPlayerReward> sortPoints() {
		return RewardCollections.sortedByScoreDescending(getInstanceRewards(),
			IronWallWarfrontPlayerReward::getScorePoints);
	}

	public void portToPosition(Player player) {
		Point3D position = player.getRace() == Race.ASMODIANS ? asmodianStartPosition : elyosStartPosition;
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
	public IronWallWarfrontPlayerReward getPlayerReward(Integer objectId) {
		return (IronWallWarfrontPlayerReward) super.getPlayerReward(objectId);
	}
}

package com.aionemu.gameserver.model.instance.instancereward;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.instance.playerreward.EngulfedOphidanBridgePlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * EngulfedOphidanBridge 奖励，用于副本相关逻辑。
 * Engulfed Ophidan Bridge Reward for instance logic.
 */

public class EngulfedOphidanBridgeReward extends InstanceReward<EngulfedOphidanBridgePlayerReward> {
	private int capPoints;
	private MutableInt asmodiansPoints = new MutableInt(0);
	private MutableInt elyosPoins = new MutableInt(0);
	private MutableInt asmodiansPvpKills = new MutableInt(0);
	private MutableInt elyosPvpKills = new MutableInt(0);
	private Race race;
	private Point3D asmodiansStartPosition;
	private Point3D elyosStartPosition;
	protected WorldMapInstance instance;
	private long instanceTime;
	private final byte buffId;

	public EngulfedOphidanBridgeReward(Integer mapId, int instanceId, WorldMapInstance instance) {
		super(mapId, instanceId);
		this.instance = instance;
		capPoints = 30000;
		buffId = 11;
		setStartPositions();
	}

	/** 排序点。 / Sort points. */
	public List<EngulfedOphidanBridgePlayerReward> sortPoints() {
		return RewardCollections.sortedByScoreDescending(getInstanceRewards(),
				EngulfedOphidanBridgePlayerReward::getScorePoints);
	}

	private void setStartPositions() {
		Point3D a = new Point3D(758.2061f, 560.8301f, 576.874f);
		Point3D e = new Point3D(322.03433f, 490.0247f, 596.1155f);
		asmodiansStartPosition = a;
		elyosStartPosition = e;
	}

	/** 传送至坐标 / Port To Position */
	public void portToPosition(Player player) {
		if (player.getRace() == Race.ASMODIANS) {
			TeleportService2.teleportTo(player, mapId, instanceId, asmodiansStartPosition.getX(),
					asmodiansStartPosition.getY(), asmodiansStartPosition.getZ());
		} else {
			TeleportService2.teleportTo(player, mapId, instanceId, elyosStartPosition.getX(), elyosStartPosition.getY(),
					elyosStartPosition.getZ());
		}
	}

	/** 返回点种族 / Returns the points by race*/
	public MutableInt getPointsByRace(Race race) {
		return (race == Race.ELYOS) ? elyosPoins : (race == Race.ASMODIANS) ? asmodiansPoints : null;
	}

	/** 添加 points by race / Adds points by race */
	public void addPointsByRace(Race race, int points) {
		MutableInt racePoints = getPointsByRace(race);
		racePoints.add(points);
		if (racePoints.intValue() < 0) {
			racePoints.setValue(0);
		}
	}

	/** 按 race 返回 pvp kills / Returns the pvp kills by race */
	public MutableInt getPvpKillsByRace(Race race) {
		return (race == Race.ELYOS) ? elyosPvpKills : (race == Race.ASMODIANS) ? asmodiansPvpKills : null;
	}

	/** 添加 pvp kills by race / Adds pvp kills by race */
	public void addPvpKillsByRace(Race race, int points) {
		MutableInt racePoints = getPvpKillsByRace(race);
		racePoints.add(points);
		if (racePoints.intValue() < 0) {
			racePoints.setValue(0);
		}
	}

	/** 设置 winner race / Sets the winner race */
	public void setWinnerRace(Race race) {
		this.race = race;
	}

	/** 返回 winner race / Returns the winner race */
	public Race getWinnerRace() {
		return race;
	}

	/** 按 score 返回 winner race / Returns the winner race by score */
	public Race getWinnerRaceByScore() {
		return asmodiansPoints.compareTo(elyosPoins) > 0 ? Race.ASMODIANS : Race.ELYOS;
	}

	/** 清空。 / Clear. */
	@Override
	public void clear() {
		super.clear();
	}

	/** Reg 玩家 Reward / Reg Player Reward */
	public void regPlayerReward(Player player) {
		if (!containPlayer(player.getObjectId())) {
			addPlayerReward(new EngulfedOphidanBridgePlayerReward(player.getObjectId(), buffId, player.getRace()));
		}
	}

	/** 添加玩家奖励。 / Adds player reward. */
	@Override
	public void addPlayerReward(EngulfedOphidanBridgePlayerReward reward) {
		super.addPlayerReward(reward);
	}

	/** 获取玩家奖励。 / Returns the player reward. */
	@Override
	public EngulfedOphidanBridgePlayerReward getPlayerReward(Integer object) {
		return (EngulfedOphidanBridgePlayerReward) super.getPlayerReward(object);
	}

	/** 发送数据包。 / Send packet. */
	public void sendPacket(final int type, final Integer object) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/** 访问 / visit. */
			@Override
			public void visit(Player player) {
				switch (player.getWorldId()) {
				case 301210000: // Engulfed Ophidan Bridge 4.7
					PacketSendUtility.sendPacket(player,
							new SM_INSTANCE_SCORE(type, getTime(), getInstanceReward(), object));
					break;
				case 301670000: // Ophidan Warpath 5.1
					PacketSendUtility.sendPacket(player,
							new SM_INSTANCE_SCORE(type, getTime2(), getInstanceReward(), object));
					break;
				}
			}
		});
	}

	/** 返回时间 / Returns the time*/
	public int getTime() {
		long result = System.currentTimeMillis() - instanceTime;
		if (result < 90000) {
			return (int) (90000 - result);
		} else if (result < 1800000) { // 30-Mins
			return (int) (1800000 - (result - 90000));
		}
		return 0;
	}

	/** 返回时间2 / Returns the time 2*/
	public int getTime2() {
		long result = System.currentTimeMillis() - instanceTime;
		if (result < 90000) {
			return (int) (90000 - result);
		} else if (result < 1200000) { // 20-Mins
			return (int) (1200000 - (result - 90000));
		}
		return 0;
	}

	/** 返回增益 ID / Returns the buff id */
	public byte getBuffId() {
		return buffId;
	}

	/** 设置 instance start time / Sets the instance start time */
	public void setInstanceStartTime() {
		this.instanceTime = System.currentTimeMillis();
	}

	/** 返回 cap points / Returns the cap points */
	public int getCapPoints() {
		return capPoints;
	}

	/**
	 * @return Whether cap points
	 */
	public boolean hasCapPoints() {
		return RewardCollections.maxPoints(getInstanceRewards()) >= capPoints;
	}
}

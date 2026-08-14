package com.aionemu.gameserver.model.instance.instancereward;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.instance.playerreward.KamarBattlefieldPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * KamarBattlefield 奖励，用于副本相关逻辑。
 * Kamar Battlefield Reward for instance logic.
 */

public class KamarBattlefieldReward extends InstanceReward<KamarBattlefieldPlayerReward> {
	private int capPoints;
	private MutableInt asmodiansPoints = new MutableInt(3800);
	private MutableInt elyosPoins = new MutableInt(3800);
	private MutableInt asmodiansPvpKills = new MutableInt(0);
	private MutableInt elyosPvpKills = new MutableInt(0);
	private Race race;
	private Point3D asmodiansStartPosition;
	private Point3D elyosStartPosition;
	protected WorldMapInstance instance;
	private long instanceTime;
	private int bonusTime;
	private final byte buffId;

	public KamarBattlefieldReward(Integer mapId, int instanceId, WorldMapInstance instance) {
		super(mapId, instanceId);
		this.instance = instance;
		capPoints = 30000;
		bonusTime = 12000;
		buffId = 10;
		setStartPositions();
	}

	/** 欧比斯奖励。 / Abyss Reward. */
	public int AbyssReward(boolean isWin, boolean isVargaKilled) {
		int VargaKilled = 1993;
		int Win = 3163;
		int Loss = 1031;
		if (isVargaKilled) {
			return isWin ? (Win + VargaKilled) : (Loss + VargaKilled);
		} else {
			return isWin ? Win : Loss;
		}
	}

	/** 荣耀奖励 / Glory Reward */
	public int GloryReward(boolean isWin, boolean isVargaKilled) {
		int VargaKilled = 50;
		int Win = 150;
		int Loss = 30;
		if (isVargaKilled) {
			return isWin ? (Win + VargaKilled) : (Loss + VargaKilled);
		} else {
			return isWin ? Win : Loss;
		}
	}

	/** 经验奖励。 / Exp Reward. */
	public int ExpReward(boolean isWin, boolean isVargaKilled) {
		int VargaKilled = 20000;
		int Win = 10000;
		int Loss = 5000;
		if (isVargaKilled) {
			return isWin ? (Win + VargaKilled) : (Loss + VargaKilled);
		} else {
			return isWin ? Win : Loss;
		}
	}

	/** 排序点。 / Sort points. */
	public List<KamarBattlefieldPlayerReward> sortPoints() {
		return RewardCollections.sortedByScoreDescending(getInstanceRewards(),
				KamarBattlefieldPlayerReward::getScorePoints);
	}

	private void setStartPositions() {
		Point3D a = new Point3D(1535.671f, 1573.9156f, 612.4217f); // Habrok Boarding Site.
		Point3D b = new Point3D(1205.2655f, 1350.9125f, 612.91205f); // Griffoen Boarding Site.
		asmodiansStartPosition = a;
		elyosStartPosition = b;
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
			addPlayerReward(
					new KamarBattlefieldPlayerReward(player.getObjectId(), bonusTime, buffId, player.getRace()));
		}
	}

	/** 添加玩家奖励。 / Adds player reward. */
	@Override
	public void addPlayerReward(KamarBattlefieldPlayerReward reward) {
		super.addPlayerReward(reward);
	}

	/** 获取玩家奖励。 / Returns the player reward. */
	@Override
	public KamarBattlefieldPlayerReward getPlayerReward(Integer object) {
		return (KamarBattlefieldPlayerReward) super.getPlayerReward(object);
	}

	/** 发送数据包。 / Send packet. */
	public void sendPacket(final int type, final Integer object) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/** 访问 / visit. */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player,
						new SM_INSTANCE_SCORE(type, getTime(), getInstanceReward(), object));
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
	 * @return 是否达到上限分 / Whether cap points reached
	 */
	public boolean hasCapPoints() {
		return RewardCollections.maxPoints(getInstanceRewards()) >= capPoints;
	}
}

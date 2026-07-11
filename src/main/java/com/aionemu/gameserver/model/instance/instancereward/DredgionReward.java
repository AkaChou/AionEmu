package com.aionemu.gameserver.model.instance.instancereward;

import org.apache.commons.lang3.mutable.MutableInt;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.instance.playerreward.DredgionPlayerReward;
import com.aionemu.gameserver.services.teleport.TeleportService2;

import java.util.ArrayList;
import java.util.List;

/**
 * 无畏舰奖励，用于副本相关逻辑。
 * Dredgion Reward for instance logic.
 */

public class DredgionReward extends InstanceReward<DredgionPlayerReward> {
	private int winnerPoints;
	private int looserPoints;
	@SuppressWarnings("unused")
	private int drawPoins;
	private MutableInt asmodiansPoints = new MutableInt(0);
	private MutableInt elyosPoins = new MutableInt(0);
	private Race race;
	private List<DredgionRooms> dredgionRooms = new ArrayList<DredgionRooms>();
	private Point3D asmodiansStartPosition;
	private Point3D elyosStartPosition;

	public DredgionReward(Integer mapId, int instanceId) {
		super(mapId, instanceId);
		winnerPoints = mapId == 300110000 ? 3000 : 4500;
		looserPoints = mapId == 300110000 ? 1500 : 2500;
		drawPoins = mapId == 300110000 ? 2250 : 3750;
		setStartPositions();
		for (int i = 1; i < 15; i++) {
			dredgionRooms.add(new DredgionRooms(i));
		}
	}

	private void setStartPositions() {
		Point3D a = new Point3D(570.468f, 166.897f, 432.28986f);
		Point3D b = new Point3D(400.741f, 166.713f, 432.290f);
		if (Rnd.get(2) == 0) {
			asmodiansStartPosition = a;
			elyosStartPosition = b;
		} else {
			asmodiansStartPosition = b;
			elyosStartPosition = a;
		}
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

	public class DredgionRooms {
		private int roomId;
		private int state = 0xFF;

		public DredgionRooms(int roomId) {
			this.roomId = roomId;
		}

		/** 返回 room id / Returns the room id */
		public int getRoomId() {
			return roomId;
		}

		/** 占领房间 / capture Room. */
		public void captureRoom(Race race) {
			state = race.equals(Race.ASMODIANS) ? 0x01 : 0x00;
		}

		/** 获取状态。 / Returns the state. */
		public int getState() {
			return state;
		}
	}

	/** 返回 dredgion rooms / Returns the dredgion rooms */
	public List<DredgionRooms> getDredgionRooms() {
		return dredgionRooms;
	}

	/** 按 ID 返回 dredgion room / Returns the dredgion room by id */
	public DredgionRooms getDredgionRoomById(int roomId) {
		for (DredgionRooms dredgionRoom : dredgionRooms) {
			if (dredgionRoom.getRoomId() == roomId) {
				return dredgionRoom;
			}
		}
		return null;
	}

	/** 返回点种族 / Returns the points by race*/
	public MutableInt getPointsByRace(Race race) {
		switch (race) {
		case ELYOS:
			return elyosPoins;
		case ASMODIANS:
			return asmodiansPoints;
		default:
			break;
		}
		return null;
	}

	/** 添加 points by race / Adds points by race */
	public void addPointsByRace(Race race, int points) {
		MutableInt racePoints = getPointsByRace(race);
		racePoints.add(points);
		if (racePoints.intValue() < 0) {
			racePoints.setValue(0);
		}
	}

	/** 返回 looser points / Returns the looser points */
	public int getLooserPoints() {
		return looserPoints;
	}

	/** 返回 winner points / Returns the winner points */
	public int getWinnerPoints() {
		return winnerPoints;
	}

	/** 设置 winning race / Sets the winning race */
	public void setWinningRace(Race race) {
		this.race = race;
	}

	/** 返回 winning race / Returns the winning race */
	public Race getWinningRace() {
		return race;
	}

	/** 按 score 返回 winning race / Returns the winning race by score */
	public Race getWinningRaceByScore() {
		return asmodiansPoints.compareTo(elyosPoins) > 0 ? Race.ASMODIANS : Race.ELYOS;
	}

	/** 清空。 / Clear. */
	@Override
	public void clear() {
		super.clear();
		dredgionRooms.clear();
	}
}

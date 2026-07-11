package com.aionemu.gameserver.model.instance.instancereward;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.instance.instanceposition.GenerealInstancePosition;
import com.aionemu.gameserver.model.instance.instanceposition.HallOfTenacityInstancePosition;
import com.aionemu.gameserver.model.instance.playerreward.HallOfTenacityPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;


/**
 * HallOfTenacity 奖励，用于副本相关逻辑。
 * Hall Of Tenacity Reward for instance logic.
 *
 * @author Ranastic
 */
@Slf4j
public class HallOfTenacityReward extends InstanceReward<HallOfTenacityPlayerReward> {

	protected WorldMapInstance instance;
	private long instanceTime;
	private final byte buffId;
	private Point3D myBattlePosition;
	private Point3D opponentBattlePosition;
	private int bonusTime;
	private GenerealInstancePosition instancePosition;

	public HallOfTenacityReward(Integer mapId, int instanceId, WorldMapInstance instance) {
		super(mapId, instanceId);
		this.instance = instance;
		bonusTime = 0;
		buffId = 0;
		instancePosition = new HallOfTenacityInstancePosition();
		instancePosition.initsialize(mapId, instanceId);
	}

	/** 欧比斯奖励。 / Abyss Reward. */
	public int AbyssReward(boolean isWin) {
		int Win = 3163;
		int Loss = 1031;
		return isWin ? Win : Loss;
	}

	/** 荣耀奖励 / Glory Reward */
	public int GloryReward(boolean isWin) {
		int Win = 150;
		int Loss = 30;
		return isWin ? Win : Loss;
	}

	/** 经验奖励。 / Exp Reward. */
	public int ExpReward(boolean isWin) {
		int Win = 10000;
		int Loss = 5000;
		return isWin ? Win : Loss;
	}

	/** 设置 start positions / Sets the start positions */
	public void setStartPositions() {
		Point3D my = new Point3D(256.12454f, 292.78516f, 74.00548f); // Zone A
		Point3D opponent = new Point3D(256.00023f, 219.35153f, 73.99652f); // Zone B
		myBattlePosition = my;
		opponentBattlePosition = opponent;
	}

	/** 传送至竞技场 / port To Arena. */
	public void portToArena(Player player) {
		if (player.getHOTVSId() == 0) {
			TeleportService2.teleportTo(player, 302310000, instanceId, myBattlePosition.getX(), myBattlePosition.getY(),
					myBattlePosition.getZ());
		} else if (player.getHOTVSId() == 1) {
			TeleportService2.teleportTo(player, 302310000, instanceId, opponentBattlePosition.getX(),
					opponentBattlePosition.getY(), opponentBattlePosition.getZ());
		}
	}

	/** 传送至大厅 / port To Hall. */
	public void portToHall(Player player) {
		regPlayerReward(player.getObjectId());
		HallOfTenacityPlayerReward playerReward = getPlayerReward(player.getObjectId());
		playerReward.setPosition(1);
		if (player.getRace() == Race.ASMODIANS) {
			playerReward.setZone(0);
		} else if (player.getRace() == Race.ELYOS) {
			playerReward.setZone(1);
		}
		instancePosition.port(player, playerReward.getZone(), playerReward.getPosition());
	}

	/** 清空。 / Clear. */
	@Override
	public void clear() {
		super.clear();
	}

	/** Reg 玩家 Reward / Reg Player Reward */
	public void regPlayerReward(Integer object) {
		if (!containPlayer(object)) {
			addPlayerReward(new HallOfTenacityPlayerReward(object, bonusTime, buffId));
		}
	}

	/** 添加玩家奖励。 / Adds player reward. */
	@Override
	public void addPlayerReward(HallOfTenacityPlayerReward reward) {
		super.addPlayerReward(reward);
	}

	/** 获取玩家奖励。 / Returns the player reward. */
	@Override
	public HallOfTenacityPlayerReward getPlayerReward(Integer object) {
		return (HallOfTenacityPlayerReward) super.getPlayerReward(object);
	}

	/** 返回 players inside / Returns the players inside */
	public List<Player> getPlayersInside() {
		List<Player> players = new ArrayList<Player>();
		for (Player playerInside : instance.getPlayersInside()) {
			if (containPlayer(playerInside.getObjectId())) {
				players.add(playerInside);
			}
		}
		return players;
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

	/** 发送数据包。 / Send packet. */
	public void sendPacket() {
		final List<Player> players = instance.getPlayersInside();
		instance.doOnAllPlayers(new Visitor<Player>() {
			/** 访问 / visit. */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), getInstanceReward(), players));
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

	/** 设置 couple slot for battle 32 / Sets the couple slot for battle 32 */
	public void setCoupleSlotForBattle32() {
		int size = 15;// 32/2=16 (packet first slot count from 0 to 15)
		ArrayList<Integer> containRandomPlayerCoupleSlots = new ArrayList<Integer>();
		ArrayList<Integer> totalCoupleSlots = new ArrayList<Integer>(size);

		ArrayList<Player> totalPlayer = new ArrayList<Player>(getPlayersInside());
		ArrayList<Player> matchLeft = new ArrayList<Player>();
		ArrayList<Player> matchRight = new ArrayList<Player>();

		// 打乱玩家 / do shuffle players
		Collections.shuffle(totalPlayer);

		// 将所有玩家分到左或右匹配 / sort all players into left or right match
		for (Player entry : totalPlayer) {
			if (matchLeft.size() > matchRight.size())
				matchRight.add(entry);
			else
				matchLeft.add(entry);
		}

		// 排序成对槽位 / sort couple slot
		for (int i = 0; i <= size; i++) {
			totalCoupleSlots.add(i);
		}

		// 随机玩家成对槽位 / do random player couple slot
		Random rand = new Random();
		while (totalCoupleSlots.size() > 0) {
			int index = rand.nextInt(totalCoupleSlots.size());
			containRandomPlayerCoupleSlots.add(totalCoupleSlots.remove(index));
		}

		// 左侧匹配 / left matching
		Iterator<Player> iterLeft = matchLeft.iterator();
		while (iterLeft.hasNext()) {
			Player player1 = iterLeft.next();
			Player player2 = iterLeft.hasNext() ? iterLeft.next() : player1;

			int rnds = rand.nextInt(containRandomPlayerCoupleSlots.size());
			int coupleId = containRandomPlayerCoupleSlots.remove(rnds);

			player1.setHOTCoupleId(coupleId);
			player2.setHOTCoupleId(coupleId);

			player1.setHOTVSId(0);
			player2.setHOTVSId(1);

			player1.setHOTMyOpponentObjId(player2.getObjectId());
			player2.setHOTMyOpponentObjId(player1.getObjectId());
			log.info(I18n.get("log.ddc33948ded3", player1.getName(), player2.getName()));
		}

		// 右侧匹配 / right matching
		Iterator<Player> iterRight = matchRight.iterator();
		while (iterRight.hasNext()) {
			Player player1 = iterRight.next();
			Player player2 = iterRight.hasNext() ? iterRight.next() : player1;

			int rnds = rand.nextInt(containRandomPlayerCoupleSlots.size());
			int coupleId = containRandomPlayerCoupleSlots.remove(rnds);

			player1.setHOTCoupleId(coupleId);
			player2.setHOTCoupleId(coupleId);

			player1.setHOTVSId(0);
			player2.setHOTVSId(1);

			player1.setHOTMyOpponentObjId(player2.getObjectId());
			player2.setHOTMyOpponentObjId(player1.getObjectId());
			log.info(I18n.get("log.b19f194fecb8", player1.getName(), player2.getName()));
		}
	}
}

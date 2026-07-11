package com.aionemu.gameserver.model.instance.instancereward;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.instanceposition.ChaosInstancePosition;
import com.aionemu.gameserver.model.instance.instanceposition.DisciplineInstancePosition;
import com.aionemu.gameserver.model.instance.instanceposition.GenerealInstancePosition;
import com.aionemu.gameserver.model.instance.instanceposition.GloryInstancePosition;
import com.aionemu.gameserver.model.instance.instanceposition.HarmonyInstancePosition;
import com.aionemu.gameserver.model.instance.playerreward.PvPArenaPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * PvPArena 奖励，用于副本相关逻辑。
 * Pv P Arena Reward for instance logic.
 */

public class PvPArenaReward extends InstanceReward<PvPArenaPlayerReward> {
	private Map<Integer, Boolean> positions = new HashMap<Integer, Boolean>();
	private List<Integer> zones = new ArrayList<Integer>();
	private int round = 1;
	private Integer zone;
	private int bonusTime;
	private int capPoints;
	private long instanceTime;
	private final byte buffId;
	protected WorldMapInstance instance;
	private GenerealInstancePosition instancePosition;

	public PvPArenaReward(Integer mapId, int instanceId, WorldMapInstance instance) {
		super(mapId, instanceId);
		this.instance = instance;
		boolean isSolo = isSoloArena();
		capPoints = isSolo ? 14400 : 50000;
		bonusTime = isSolo ? 8100 : 12000;
		Collections.addAll(zones, isSolo ? new Integer[] { 1, 2, 3, 4 } : new Integer[] { 1, 2, 3, 4, 5, 6 });
		int positionSize;
		if (isSolo) {
			positionSize = 4;
			buffId = 8;
			instancePosition = new DisciplineInstancePosition();
		} else if (isGlory()) {
			buffId = 7;
			positionSize = 8;
			instancePosition = new GloryInstancePosition();
		} else if (mapId == 300450000 || mapId == 300570000 || mapId == 301100000) {
			buffId = 7;
			positionSize = 12;
			instancePosition = new HarmonyInstancePosition();
		} else {
			buffId = 7;
			positionSize = 12;
			instancePosition = new ChaosInstancePosition();
		}
		instancePosition.initsialize(mapId, instanceId);
		for (int i = 1; i <= positionSize; i++) {
			positions.put(i, Boolean.FALSE);
		}
		setRndZone();
	}

	/** 是否单人竞技场 / Whether solo arena*/
	public final boolean isSoloArena() {
		return mapId == 300360000 || mapId == 300430000;
	}

	/**
	 * @return 是否 glory / 是否 glory。 / Whether glory / Whether glory
	 */
	public final boolean isGlory() {
		return mapId == 300550000;
	}

	/** 返回 cap points / Returns the cap points */
	public int getCapPoints() {
		return capPoints;
	}

	/** 设置 rnd zone / Sets the rnd zone */
	public final void setRndZone() {
		int index = Rnd.get(zones.size());
		zone = zones.get(index);
		zones.remove(index);
	}

	private List<Integer> getFreePositions() {
		List<Integer> p = new ArrayList<Integer>();
		for (Integer key : positions.keySet()) {
			if (!positions.get(key)) {
				p.add(key);
			}
		}
		return p;
	}

	/** 设置 rnd position / Sets the rnd position */
	public synchronized void setRndPosition(Integer object) {
		PvPArenaPlayerReward reward = getPlayerReward(object);
		int position = reward.getPosition();
		if (position != 0) {
			clearPosition(position, Boolean.FALSE);
		}
		Integer key = getFreePositions().get(Rnd.get(getFreePositions().size()));
		clearPosition(key, Boolean.TRUE);
		reward.setPosition(key);
	}

	/** 清空坐标。 / Clear position. */
	public synchronized void clearPosition(int position, Boolean result) {
		positions.put(position, result);
	}

	/** 返回 round / Returns the round */
	public int getRound() {
		return round;
	}

	/** 设置 round / Sets the round */
	public void setRound(int round) {
		this.round = round;
	}

	/** Reg 玩家 Reward / Reg Player Reward */
	public void regPlayerReward(Integer object) {
		if (!containPlayer(object)) {
			addPlayerReward(new PvPArenaPlayerReward(object, bonusTime, buffId));
		}
	}

	/** 添加玩家奖励。 / Adds player reward. */
	@Override
	public void addPlayerReward(PvPArenaPlayerReward reward) {
		super.addPlayerReward(reward);
	}

	/** 获取玩家奖励。 / Returns the player reward. */
	@Override
	public PvPArenaPlayerReward getPlayerReward(Integer object) {
		return (PvPArenaPlayerReward) super.getPlayerReward(object);
	}

	/** 传送至坐标 / Port To Position */
	public void portToPosition(Player player) {
		Integer object = player.getObjectId();
		regPlayerReward(object);
		setRndPosition(object);
		PvPArenaPlayerReward playerReward = getPlayerReward(object);
		playerReward.applyBoostMoraleEffect(player);
		instancePosition.port(player, zone, playerReward.getPosition());
	}

	/** 排序点。 / Sort points. */
	public List<PvPArenaPlayerReward> sortPoints() {
		return RewardCollections.sortedByScoreDescending(getInstanceRewards(), PvPArenaPlayerReward::getScorePoints);
	}

	/**
	 * @param rewardedPlayer Whether reward opportunity token / Whether reward opportunity token
	 */
	public boolean canRewardOpportunityToken(PvPArenaPlayerReward rewardedPlayer) {
		if (rewardedPlayer != null) {
			int rank = getRank(rewardedPlayer.getScorePoints());
			return isSoloArena() && rank == 1 || rank > 2;
		}
		return false;
	}

	/** 获取军阶。 / Returns the rank. */
	public int getRank(int points) {
		int rank = -1;
		for (PvPArenaPlayerReward reward : sortPoints()) {
			if (reward.getScorePoints() >= points) {
				rank++;
			}
		}
		return rank;
	}

	/**
	 * @return Whether cap points / Whether cap points
	 */
	public boolean hasCapPoints() {
		if (isSoloArena()
				&& (RewardCollections.maxPoints(getInstanceRewards()) - RewardCollections.minPoints(getInstanceRewards()) >= 1500))
			return true;
		return RewardCollections.maxPoints(getInstanceRewards()) >= capPoints;
	}

	/** 返回 total points / Returns the total points */
	public int getTotalPoints() {
		return RewardCollections.sum(getInstanceRewards(), PvPArenaPlayerReward::getScorePoints);
	}

	/**
	 * @return Whether rewarded / Whether rewarded
	 */
	public boolean canRewarded() {
		return mapId == 300350000 || mapId == 300360000 || mapId == 300420000 || mapId == 300430000
				|| mapId == 300450000 || mapId == 300550000 || mapId == 300570000 || mapId == 301100000;
	}

	/** 返回 npc bonus skill / Returns the npc bonus skill */
	public int getNpcBonusSkill(int npcId) {
		switch (npcId) {
		case 701175:
		case 701176:
		case 701177:
		case 701178:
			return 0x4E5732;
		case 701189:
		case 701190:
		case 701191:
		case 701192:
			return 0x4FDB3C;
		case 701317:
			return 0x4f8532;
		case 701318:
			return 0x4f8537;
		case 701319:
			return 0x4f853C;
		case 701220:
			return 0x4E5537;
		case 207118:
		case 207119:
			return 0x50C101;
		case 207100:
			return 0x50BE01;
		default:
			return 0;
		}
	}

	/** 返回时间 / Returns the time*/
	public int getTime() {
		long result = System.currentTimeMillis() - instanceTime;
		if (isRewarded()) {
			return 0;
		}
		if (result < 120000) {
			return (int) (120000 - result);
		} else {
			return (int) (180000 * getRound() - (result - 120000));
		}
	}

	/** 设置 instance start time / Sets the instance start time */
	public void setInstanceStartTime() {
		this.instanceTime = System.currentTimeMillis();
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

	/** 返回增益 ID / Returns the buff id */
	public byte getBuffId() {
		return buffId;
	}

	/** 清空。 / Clear. */
	@Override
	public void clear() {
		super.clear();
		positions.clear();
	}
}

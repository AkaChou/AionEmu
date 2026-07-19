package com.aionemu.gameserver.model.instance.instancereward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.model.instance.instanceposition.ChaosInstancePosition;
import com.aionemu.gameserver.model.instance.instanceposition.DisciplineInstancePosition;
import com.aionemu.gameserver.model.instance.instanceposition.GenerealInstancePosition;
import com.aionemu.gameserver.model.instance.instanceposition.GloryInstancePosition;
import com.aionemu.gameserver.model.instance.instanceposition.HarmonyInstancePosition;
import com.aionemu.gameserver.model.instance.playerreward.PvPArenaPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
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
	private final Row arenaRow;
	private long instanceTime;
	private final byte buffId;
	protected WorldMapInstance instance;
	private GenerealInstancePosition instancePosition;

	public PvPArenaReward(Integer mapId, int instanceId, WorldMapInstance instance) {
		super(mapId, instanceId);
		this.instance = instance;
		if (instance.getDynamicInstance() == null) {
			throw new IllegalStateException("PvP arena requires a retail dynamic instance");
		}
		arenaRow = InstanceSettlementService.arenaRow(mapId, instance.getDynamicInstance().getSpawnPage());
		boolean isSolo = isSoloArena();
		for (int stage = 1; stage <= arenaRow.requiredInt("stage_count"); stage++) {
			zones.add(stage);
		}
		int positionSize = arenaRow.requiredInt("alias_count");
		if (isSolo) {
			instancePosition = new DisciplineInstancePosition();
		} else if (isGlory()) {
			instancePosition = new GloryInstancePosition();
		} else if (mapId == 300450000 || mapId == 300570000 || mapId == 301100000) {
			instancePosition = new HarmonyInstancePosition();
		} else {
			instancePosition = new ChaosInstancePosition();
		}
		buffId = (byte) arenaRow.requiredInt("rebirthbuff");
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
	 * @return 是否为荣耀竞技场。 / Whether glory
	  */
	public final boolean isGlory() {
		return mapId == 300550000;
	}

	/** 返回 cap points / Returns the cap points */
	public int getCapPoints() {
		return arenaRow.requiredInt("score_limit_top");
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
			int initialScore = isStartProgress() ? arenaRow.requiredInt("basescore_lateenter")
					: arenaRow.requiredInt("basescore_enter");
			addPlayerReward(new PvPArenaPlayerReward(object, initialScore,
					arenaRow.requiredInt("score_limit_bottom"), arenaRow.requiredInt("score_playtime_bonus"), buffId));
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
		playerReward.applyBoostMoraleEffect(player, getRebirthBuffDuration(getRank(playerReward.getPoints())));
		instancePosition.port(player, zone, playerReward.getPosition());
	}

	/** 排序点。 / Sort points. */
	public List<PvPArenaPlayerReward> sortPoints() {
		return RewardCollections.sortedByScoreDescending(getInstanceRewards(), PvPArenaPlayerReward::getScorePoints);
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
	 * @return Whether cap points
	 */
	public boolean hasCapPoints() {
		return InstanceSettlementService.arenaScoreLimitReached(arenaRow,
				RewardCollections.maxPoints(getInstanceRewards()), RewardCollections.minPoints(getInstanceRewards()));
	}

	/** 返回 total points / Returns the total points */
	public int getTotalPoints() {
		return RewardCollections.sum(getInstanceRewards(), PvPArenaPlayerReward::getScorePoints);
	}

	/**
	 * @return Whether rewarded
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
		long waitMillis = arenaRow.requiredInt("wait_time_limit") * 1000L;
		long stageMillis = arenaRow.requiredInt("stage_time_limit") * 1000L;
		if (result < waitMillis) {
			return (int) (waitMillis - result);
		} else {
			return (int) Math.max(0, stageMillis * getRound() - (result - waitMillis));
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

	public int getZone() {
		return zone;
	}

	public Row getArenaRow() {
		return arenaRow;
	}

	public int getWaitTimeSeconds() {
		return arenaRow.requiredInt("wait_time_limit");
	}

	public int getStageTimeSeconds() {
		return arenaRow.requiredInt("stage_time_limit");
	}

	public int getScoreModifierStartStage() {
		return arenaRow.intValue("scoremod_start_stage", 0);
	}

	public int getStageEndBuffId(int stage) {
		return arenaRow.intValue("stageendbuff_" + String.format("%02d", stage), 0);
	}

	public int getStageEndBuffTargetRank(int stage) {
		return arenaRow.intValue("stageendbuff_targetrank_" + String.format("%02d", stage), Integer.MAX_VALUE);
	}

	public int getKillScore() {
		return arenaRow.requiredInt("score_get_pc_kill");
	}

	public int getDeathScore(int rank) {
		return arenaRow.requiredInt("score_lose_pc_die") * getScoreModifier(rank) / 100;
	}

	public int getScoreModifier(int rank) {
		if (round < arenaRow.intValue("scoremod_start_stage", Integer.MAX_VALUE)) {
			return 100;
		}
		return arenaRow.intValue("scoremod_rank" + String.format("%02d", rank + 1), 100);
	}

	public int getRebirthBuffDuration(int rank) {
		return arenaRow.intValue("rebirthbuff_duration_rank" + String.format("%02d", rank + 1),
				arenaRow.requiredInt("rebirthbuff_duration"));
	}

	public long getTotalPlayMillis() {
		return arenaRow.requiredInt("stage_time_limit") * 3000L;
	}

	/** 清空。 / Clear. */
	@Override
	public void clear() {
		super.clear();
		positions.clear();
	}
}

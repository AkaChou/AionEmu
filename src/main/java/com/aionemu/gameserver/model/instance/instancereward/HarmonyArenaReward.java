package com.aionemu.gameserver.model.instance.instancereward;

import java.util.List;

import com.aionemu.gameserver.model.autogroup.AGPlayer;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.playerreward.HarmonyGroupReward;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;

/**
 * HarmonyArena 奖励，用于副本相关逻辑。
 * Harmony Arena Reward for instance logic.
 */

public class HarmonyArenaReward extends PvPArenaReward {
	private List<HarmonyGroupReward> groups = new ArrayList<HarmonyGroupReward>();

	public HarmonyArenaReward(Integer mapId, int instanceId, WorldMapInstance instance) {
		super(mapId, instanceId, instance);
	}

	/** 返回 harmony group reward / Returns the harmony group reward */
	public HarmonyGroupReward getHarmonyGroupReward(Integer object) {
		for (InstancePlayerReward reward : groups) {
			HarmonyGroupReward harmonyReward = (HarmonyGroupReward) reward;
			if (harmonyReward.containPlayer(object)) {
				return harmonyReward;
			}
		}
		return null;
	}

	/** 返回 harmony group inside / Returns the harmony group inside */
	public List<HarmonyGroupReward> getHarmonyGroupInside() {
		List<HarmonyGroupReward> harmonyGroups = new ArrayList<HarmonyGroupReward>();
		for (HarmonyGroupReward group : groups) {
			for (AGPlayer agp : group.getAGPlayers()) {
				if (agp.isInInstance()) {
					harmonyGroups.add(group);
					break;
				}
			}
		}
		return harmonyGroups;
	}

	/** 返回 players inside / Returns the players inside */
	public List<Player> getPlayersInside(HarmonyGroupReward group) {
		List<Player> players = new ArrayList<Player>();
		for (Player playerInside : instance.getPlayersInside()) {
			if (group.containPlayer(playerInside.getObjectId())) {
				players.add(playerInside);
			}
		}
		return players;
	}

	/** 添加 harmony group / Adds harmony group */
	public void addHarmonyGroup(HarmonyGroupReward reward) {
		groups.add(reward);
	}

	/** 返回组 / Returns the groups*/
	public List<HarmonyGroupReward> getGroups() {
		return groups;
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

	/** 获取军阶。 / Returns the rank. */
	@Override
	public int getRank(int points) {
		int rank = -1;
		for (HarmonyGroupReward reward : sortGroupPoints()) {
			if (reward.getPoints() >= points) {
				rank++;
			}
		}
		return rank;
	}

	/** 排序队伍点。 / Sort group points. */
	public List<HarmonyGroupReward> sortGroupPoints() {
		return RewardCollections.sortedByScoreDescending(groups, HarmonyGroupReward::getPoints);
	}

	/** 返回 total points / Returns the total points */
	@Override
	public int getTotalPoints() {
		return RewardCollections.sum(groups, HarmonyGroupReward::getPoints);
	}

	/** 清空。 / Clear. */
	@Override
	public void clear() {
		groups.clear();
		super.clear();
	}
}

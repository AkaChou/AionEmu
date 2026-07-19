package com.aionemu.gameserver.model.autogroup;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.instancereward.HarmonyArenaReward;
import com.aionemu.gameserver.model.instance.playerreward.HarmonyGroupReward;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 自动 Harmony 副本，用于 autogroup 相关逻辑。
 * Auto Harmony Instance for autogroup logic.
 */

public class AutoHarmonyInstance extends AutoInstance {
	private List<AGPlayer> group1 = new ArrayList<AGPlayer>();
	private List<AGPlayer> group2 = new ArrayList<AGPlayer>();

	/** 副本创建 / On Instance Create*/
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		HarmonyArenaReward reward = (HarmonyArenaReward) instance.getInstanceHandler().getInstanceReward();
		reward.addHarmonyGroup(new HarmonyGroupReward(1, reward.getArenaRow(), group1));
		reward.addHarmonyGroup(new HarmonyGroupReward(2, reward.getArenaRow(), group2));
	}

	@Override
	public void restorePlayer(AGPlayer player) {
		super.restorePlayer(player);
		(player.getMatchSide() == 0 ? group1 : group2).add(player);
	}

	/** 添加玩家。 / Adds player. */
	@Override
	public AGQuestion addPlayer(Player player, SearchInstance searchInstance) {
		super.writeLock();
		try {
			if (!satisfyTime(searchInstance) || (players.size() >= agt.getPlayerSize())) {
				return AGQuestion.FAILED;
			}
			AGQuestion result;
			if (searchInstance.getEntryRequestType().isGroupEntry()) {
				result = canAddGroup(group1, player, searchInstance);
				if (result.isFailed()) {
					result = canAddGroup(group2, player, searchInstance);
				}
				return result;
			}
			result = canAddPlayer(group1, player);
			if (result.isFailed()) {
				result = canAddPlayer(group2, player);
			}
			return result;
		} finally {
			super.writeUnlock();
		}
	}

	/** 按下回车时 / on Press Enter. */
	@Override
	public void onPressEnter(Player player) {
		enter(player, () -> {
			((HarmonyArenaReward) instance.getInstanceHandler().getInstanceReward()).portToPosition(player);
			return true;
		});
	}

	/** 进入副本 / On Enter Instance*/
	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		if (player.isInGroup2()) {
			return;
		}
		Integer object = player.getObjectId();
		List<AGPlayer> group = getGroup(object);
		if (group != null) {
			List<Player> _players = getPlayerFromGroup(group);
			_players.remove(player);
			if (_players.size() == 1 && !_players.get(0).isInGroup2()) {
				PlayerGroup newGroup = PlayerGroupService.createGroup(_players.get(0), player, TeamType.AUTO_GROUP);
				int groupId = newGroup.getObjectId();
				if (!instance.isRegistered(groupId)) {
					instance.register(groupId);
				}
			} else if (!_players.isEmpty() && _players.get(0).isInGroup2()) {
				PlayerGroupService.addPlayer(_players.get(0).getPlayerGroup2(), player);
			}
			if (!instance.isRegistered(object)) {
				instance.register(object);
			}
		}
	}

	/** 离开副本 / On Leave Instance*/
	@Override
	public void onLeaveInstance(Player player) {
		AGPlayer matchPlayer = players.get(player.getObjectId());
		if (matchPlayer != null) {
			matchPlayer.setInInstance(false);
			matchPlayer.setOnline(false);
		}
		super.unregister(player);
		PlayerGroupService.removePlayer(player);
	}

	/** 注销。 / Unregister. */
	@Override
	public void unregister(Player player) {
		AGPlayer agp = players.get(player.getObjectId());
		if (agp != null) {
			if (group1.contains(agp)) {
				group1.remove(agp);
			} else if (group2.contains(agp)) {
				group2.remove(agp);
			}
		}
		super.unregister(player);
	}

	/** 清空。 / Clear. */
	@Override
	public void clear() {
		super.clear();
		group1.clear();
		group2.clear();
	}

	private List<Player> getPlayerFromGroup(List<AGPlayer> group) {
		List<Player> _players = new ArrayList<Player>();
		for (AGPlayer agp : group) {
			for (Player p : instance.getPlayersInside()) {
				if (p.getObjectId().equals(agp.getObjectId())) {
					_players.add(p);
					break;
				}
			}
		}
		return _players;
	}

	private List<AGPlayer> getGroup(Integer obj) {
		AGPlayer agp = players.get(obj);
		if (agp != null) {
			if (group1.contains(agp)) {
				return group1;
			} else if (group2.contains(agp)) {
				return group2;
			}
		}
		return null;
	}

	private AGQuestion canAddGroup(List<AGPlayer> group, Player player, SearchInstance searchInstance) {
		if (group.size() > 0) {
			if (!group.get(0).getRace().equals(player.getRace())) {
				return AGQuestion.FAILED;
			}
		}
		if (group.size() + searchInstance.getMembers().size() <= agt.getPlayersPerSide()) {
			for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
				Integer obj = member.getObjectId();
				if (searchInstance.getMembers().contains(obj)) {
					AGPlayer agp = new AGPlayer(member);
					agp.setMatchSide((byte) (group == group1 ? 0 : 1));
					group.add(agp);
					players.put(obj, agp);
				}
			}
			return instance != null ? AGQuestion.ADDED
					: (players.size() == agt.getPlayerSize() ? AGQuestion.READY : AGQuestion.ADDED);
		}
		return AGQuestion.FAILED;
	}

	private AGQuestion canAddPlayer(List<AGPlayer> group, Player player) {
		Integer obj = player.getObjectId();
		AGPlayer agp = new AGPlayer(player);
		agp.setMatchSide((byte) (group == group1 ? 0 : 1));
		if (group.size() < agt.getPlayersPerSide()) {
			if (group.isEmpty()) {
				group.add(agp);
				players.put(obj, agp);
				return AGQuestion.ADDED;
			} else if (getAGPlayerByIndex(group, 0).getRace().equals(player.getRace())) {
				group.add(agp);
				players.put(obj, agp);
				return instance != null ? AGQuestion.ADDED
						: (players.size() == agt.getPlayerSize() ? AGQuestion.READY : AGQuestion.ADDED);
			}
		}
		return AGQuestion.FAILED;
	}

	private AGPlayer getAGPlayerByIndex(List<AGPlayer> group, int index) {
		return group.get(index);
	}
}

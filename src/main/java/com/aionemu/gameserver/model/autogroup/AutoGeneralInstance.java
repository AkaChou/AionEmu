package com.aionemu.gameserver.model.autogroup;

import java.util.List;
import java.util.ArrayList;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.model.templates.portal.PortalLoc;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * 自动通用副本，用于 autogroup 相关逻辑。
 * Auto General Instance for autogroup logic.
 */

public class AutoGeneralInstance extends AutoInstance {
	/** 添加玩家。 / Adds player. */
	@Override
	public AGQuestion addPlayer(Player player, SearchInstance searchInstance) {
		super.writeLock();
		try {
			List<Player> candidates = new ArrayList<>();
			if (searchInstance.getEntryRequestType().isGroupEntry()) {
				for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
					if (searchInstance.getMembers().contains(member.getObjectId())) {
						candidates.add(member);
					}
				}
			} else {
				candidates.add(player);
			}
			if (!satisfyTime(searchInstance) || players.size() + candidates.size() > agt.getPlayerSize()) {
				return AGQuestion.FAILED;
			}
			byte side = allocateSide(candidates.size(), player.getRace());
			if (side < 0) {
				return AGQuestion.FAILED;
			}
			List<AGPlayer> accepted = new ArrayList<>(players.values());
			for (Player candidate : candidates) {
				List<AGPlayer> sidePlayers = accepted.stream()
						.filter(matchPlayer -> matchPlayer.getMatchSide() == side).toList();
				if (!agt.canAdd(candidate.getPlayerClass(), sidePlayers, 1)) {
					return AGQuestion.FAILED;
				}
				AGPlayer matchPlayer = new AGPlayer(candidate);
				matchPlayer.setMatchSide(side);
				accepted.add(matchPlayer);
			}
			for (AGPlayer acceptedPlayer : accepted) {
				players.putIfAbsent(acceptedPlayer.getObjectId(), acceptedPlayer);
			}
			return instance != null ? AGQuestion.ADDED
					: (agt.isCompositionReady(players.values()) ? AGQuestion.READY : AGQuestion.ADDED);
		} finally {
			super.writeUnlock();
		}
	}

	/** 进入副本 / On Enter Instance*/
	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		if (agt.isTeamMatch()) {
			return;
		}
		byte side = players.get(player.getObjectId()).getMatchSide();
		List<Player> sidePlayers = instance.getPlayersInside().stream()
				.filter(member -> member != player && players.containsKey(member.getObjectId())
						&& players.get(member.getObjectId()).getMatchSide() == side)
				.toList();
		if (sidePlayers.size() == 1 && !sidePlayers.get(0).isInGroup2()) {
			PlayerGroup newGroup = PlayerGroupService.createGroup(sidePlayers.get(0), player, TeamType.AUTO_GROUP);
			int groupId = newGroup.getObjectId();
			if (!instance.isRegistered(groupId)) {
				instance.register(groupId);
			}
		} else if (!sidePlayers.isEmpty() && sidePlayers.get(0).isInGroup2()) {
			PlayerGroupService.addPlayer(sidePlayers.get(0).getPlayerGroup2(), player);
		}
		Integer object = player.getObjectId();
		if (!instance.isRegistered(object)) {
			instance.register(object);
		}
	}

	/** 按下回车时 / on Press Enter. */
	@Override
	public void onPressEnter(Player player) {
		int worldId = instance.getMapId();
		PortalPath portal = DataManager.PORTAL2_DATA.getPortalDialog(worldId, 10000, player.getRace());
		if (portal == null) {
			return;
		}
		PortalLoc loc = DataManager.PORTAL_LOC_DATA.getPortalLoc(portal.getLocId());
		if (loc == null) {
			return;
		}
		enter(player, () -> TeleportService2.teleportTo(player, worldId, instance.getInstanceId(), loc.getX(),
				loc.getY(), loc.getZ(), loc.getH()));
	}

	/** 离开副本 / On Leave Instance*/
	@Override
	public void onLeaveInstance(Player player) {
		super.unregister(player);
		PlayerGroupService.removePlayer(player);
	}

}

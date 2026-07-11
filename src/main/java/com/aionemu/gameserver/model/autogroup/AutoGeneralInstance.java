package com.aionemu.gameserver.model.autogroup;

import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.PlayerClass;
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
			if (!satisfyTime(searchInstance) || (players.size() >= agt.getPlayerSize())) {
				return AGQuestion.FAILED;
			}
			PlayerClass playerClass = player.getPlayerClass();
			int clericSize = getPlayersByClass(PlayerClass.CLERIC).size();
			int chanterSize = getPlayersByClass(PlayerClass.CHANTER).size();
			int songweaverSize = getPlayersByClass(PlayerClass.SONGWEAVER).size();
			int templarSize = getPlayersByClass(PlayerClass.TEMPLAR).size();
			int aethertechSize = getPlayersByClass(PlayerClass.AETHERTECH).size();
			if (playerClass.equals(PlayerClass.CLERIC)) {
				if (clericSize > 0) {
					return AGQuestion.FAILED;
				}
			} else if (playerClass.equals(PlayerClass.CHANTER)) {
				if (chanterSize > 0) {
					return AGQuestion.FAILED;
				}
			} else if (playerClass.equals(PlayerClass.SONGWEAVER)) {
				if (songweaverSize > 0) {
					return AGQuestion.FAILED;
				}
			} else if (playerClass.equals(PlayerClass.TEMPLAR)) {
				if (templarSize > 0) {
					return AGQuestion.FAILED;
				}
			} else if (playerClass.equals(PlayerClass.AETHERTECH)) {
				if (aethertechSize > 0) {
					return AGQuestion.FAILED;
				}
			} else {
				int size = players.size();
				size -= clericSize;
				size -= chanterSize;
				size -= templarSize;
				size -= songweaverSize;
				size -= aethertechSize;
				if (size >= 2) {
					return AGQuestion.FAILED;
				}
			}
			players.put(player.getObjectId(), new AGPlayer(player));
			return instance != null ? AGQuestion.ADDED
					: (players.size() == agt.getPlayerSize() ? AGQuestion.READY : AGQuestion.ADDED);
		} finally {
			super.writeUnlock();
		}
	}

	/** 进入副本 / On Enter Instance*/
	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		List<Player> playersByRace = instance.getPlayersInside();
		if (playersByRace.size() == 1 && !playersByRace.get(0).isInGroup2()) {
			PlayerGroup newGroup = PlayerGroupService.createGroup(playersByRace.get(0), player, TeamType.AUTO_GROUP);
			int groupId = newGroup.getObjectId();
			if (!instance.isRegistered(groupId)) {
				instance.register(groupId);
			}
		} else if (!playersByRace.isEmpty() && playersByRace.get(0).isInGroup2()) {
			PlayerGroupService.addPlayer(playersByRace.get(0).getPlayerGroup2(), player);
		}
		Integer object = player.getObjectId();
		if (!instance.isRegistered(object)) {
			instance.register(object);
		}
	}

	/** 按下回车时 / on Press Enter. */
	@Override
	public void onPressEnter(Player player) {
		super.onPressEnter(player);
		int worldId = instance.getMapId();
		PortalPath portal = DataManager.PORTAL2_DATA.getPortalDialog(worldId, 10000, player.getRace());
		if (portal == null) {
			return;
		}
		PortalLoc loc = DataManager.PORTAL_LOC_DATA.getPortalLoc(portal.getLocId());
		if (loc == null) {
			return;
		}
		TeleportService2.teleportTo(player, worldId, instance.getInstanceId(), loc.getX(), loc.getY(), loc.getZ(),
				loc.getH());
		if (player.getPortalCooldownList().getPortalCooldownItem(loc.getWorldId()) != null) {
			player.getPortalCooldownList().addPortalCooldown(loc.getWorldId(), 1,
					DataManager.INSTANCE_COOLTIME_DATA.getInstanceEntranceCooltime(player, worldId));
		} else {
			player.getPortalCooldownList().addEntry(worldId);
		}
	}

	/** 离开副本 / On Leave Instance*/
	@Override
	public void onLeaveInstance(Player player) {
		super.unregister(player);
		PlayerGroupService.removePlayer(player);
	}

}

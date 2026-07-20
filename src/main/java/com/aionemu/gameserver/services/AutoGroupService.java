package com.aionemu.gameserver.services;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.model.autogroup.AGPlayer;
import com.aionemu.gameserver.model.autogroup.AGQuestion;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.autogroup.AutoInstance;
import com.aionemu.gameserver.model.autogroup.EntryRequestType;
import com.aionemu.gameserver.model.autogroup.LookingForParty;
import com.aionemu.gameserver.model.autogroup.SearchInstance;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceLimitService;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.List;
import java.util.Map;
/**
 * 自动组队/匹配服务：管理副本排队、入场确认、实例创建与登录/登出时的匹配状态恢复。
 * matchmaking service: manages instance queues, entry confirmation, instance creation, and match state on login/logout.
 */
@Slf4j

public class AutoGroupService {

	private static volatile ObjectProvider<AutoGroupService> instanceProvider;
	private Map<Integer, LookingForParty> searchers = new ConcurrentHashMap<Integer, LookingForParty>();
	private Map<Integer, AutoInstance> autoInstances = new ConcurrentHashMap<Integer, AutoInstance>();
	private Collection<Integer> penaltys = ConcurrentHashMap.newKeySet();
	private Lock lock = new ReentrantLock();

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public AutoGroupService() {
	}

	/**
	 * 开始为指定副本 mask 排队匹配。
	 * Starts looking/queueing for the given instance mask.
	 *
	 * 玩家 / player
	 * instance mask id
	 * @param ert 入场请求类型 / entry request type
	 */
	public void startLooking(Player player, int instanceMaskId, EntryRequestType ert) {
		MatchDefinition agt = MatchDefinition.getByMaskId(instanceMaskId);
		if (agt == null) {
			return;
		}
		if (!canEnter(player, ert, agt)) {
			return;
		}
		Integer obj = player.getObjectId();
		LookingForParty lfp = searchers.get(obj);
		if (penaltys.contains(obj)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400181, agt.getInstanceMapId()));
			return;
		}
		if (lfp == null) {
			searchers.put(obj, new LookingForParty(player, instanceMaskId, ert));
		} else if (lfp.hasPenalty() || lfp.isRegistredInstance(instanceMaskId)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400181, agt.getInstanceMapId()));
			return;
		} else {
			lfp.addInstanceMaskId(instanceMaskId, ert);
		}
		if (ert.isGroupEntry()) {
			for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
				if (agt.hasHudRegister()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				PacketSendUtility.sendPacket(member, new SM_SYSTEM_MESSAGE(1400194, agt.getInstanceMapId()));
				PacketSendUtility.sendPacket(member,
						new SM_AUTO_GROUP(instanceMaskId, 1, ert.getId(), player.getName()));
			}
		} else {
			if (agt.hasHudRegister()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400194, agt.getInstanceMapId()));
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 1, ert.getId(), player.getName()));
		}
		startSort(ert, instanceMaskId, true);
	}

	/**
	 * 玩家确认进入已匹配的副本。
	 * Player confirms entry into a matched instance.
	 *
	 * 玩家 / player
	 * instance mask id
	 */
	public synchronized void pressEnter(Player player, int instanceMaskId) {
		AutoInstance instance = getAutoInstance(player, instanceMaskId);
		AGPlayer matchPlayer = instance == null ? null : instance.players.get(player.getObjectId());
		if (matchPlayer == null || matchPlayer.isPressedEnter()) {
			return;
		}
		instance.onPressEnter(player);
		if (!matchPlayer.isPressedEnter()) {
			return;
		}
		if (player.isInGroup2()) {
			PlayerGroupService.removePlayer(player);
		}
		if (player.isInAlliance2()) {
			PlayerAllianceService.removePlayer(player);
		}
		PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 5));
	}

	/**
	 * 玩家实际进入实例后的匹配状态处理。
	 * Handles match state after the player actually enters the instance.
	 *
	 * @param player 玩家 / player
	 */
	public void onEnterInstance(Player player) {
		if (player.isInInstance()) {
			Integer obj = player.getObjectId();
			AutoInstance autoInstance = autoInstances.get(player.getInstanceId());
			if (autoInstance != null && autoInstance.players.containsKey(obj)) {
				autoInstance.onEnterInstance(player);
			}
		}
	}

	/**
	 * 取消玩家对指定副本的排队。
	 * Unregisters the player from looking for the given instance.
	 *
	 * 玩家 / player
	 * instance mask id
	 */
	public void unregisterLooking(Player player, byte instanceMaskId) {
		Integer obj = player.getObjectId();
		LookingForParty lfp = searchers.get(obj);
		SearchInstance si;
		if (lfp != null) {
			lfp.setPenaltyTime();
			si = lfp.getSearchInstance(instanceMaskId);
			if (si != null) {
				if (lfp.unregisterInstance(instanceMaskId) == 0) {
					searchers.remove(obj);
					startPenalty(obj);
				}
	/**
	 * 获取服务单例（优先 Spring 提供者）。
	 * Returns the service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
				getInstance().unRegisterSearchInstance(player, si);
			}
		}
	}

	/**
	 * 取消进入已匹配副本。
	 * Cancels entry into a matched instance.
	 *
	 * 玩家 / player
	 * instance mask id
	 */
	public void cancelEnter(Player player, int instanceMaskId) {
		AutoInstance autoInstance = getAutoInstance(player, instanceMaskId);
		if (autoInstance != null) {
			MatchDefinition type = autoInstance.agt;
			Integer obj = player.getObjectId();
			if (!autoInstance.players.get(obj).isInInstance()) {
				autoInstance.unregister(player);
				if (!searchers.containsKey(obj)) {
					startPenalty(obj);
				}
				if (autoInstance.agt.hasRegisterFast()) {
					startSort(EntryRequestType.FAST_GROUP_ENTRY, instanceMaskId, false);
				}
				if (autoInstance.players.isEmpty()) {
					WorldMapInstance instance = autoInstance.instance;
					autoInstance = autoInstances.remove(instance.getInstanceId());
					InstanceService.destroyInstance(instance);
					autoInstance.clear();
				}
			}
			if (type.hasHudRegister() && type.isOpen()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 2));
		}
	}

	/**
	 * 登录时恢复/推送可用自动组队入口。
	 * On login, restores/pushes available auto-group entry points.
	 *
	 * logging-in player
	 */
	public void onPlayerLogin(Player player) {
		for (MatchDefinition type : MatchDefinition.all()) {
			if (type.hasHudRegister() && type.isOpen() && type.hasLevelPermit(player.getLevel())
					&& !hasCoolDown(player, type.getInstanceMapId())) {
				PacketSendUtility.sendPacket(player,
						new SM_AUTO_GROUP(type.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon));
			}
		}
		Integer obj = player.getObjectId();
		LookingForParty lfp = searchers.get(obj);
		if (lfp != null) {
			for (SearchInstance searchInstance : lfp.getSearchInstances()) {
				if (searchInstance.getEntryRequestType().isGroupEntry() && !player.isInGroup2()) {
					int instanceMaskId = searchInstance.getInstanceMaskId();
					lfp.unregisterInstance(instanceMaskId);
					MatchDefinition type = MatchDefinition.getByMaskId(instanceMaskId);
					if (type != null && type.hasHudRegister() && type.isOpen()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 2));
					continue;
				}
				PacketSendUtility.sendPacket(player,
						new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), 8,
								searchInstance.getRemainingTime() + searchInstance.getEntryRequestType().getId(),
								player.getName()));
				MatchDefinition type = MatchDefinition.getByMaskId(searchInstance.getInstanceMaskId());
				if (type != null && type.hasHudRegister()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
			}
			if (lfp.getSearchInstances().isEmpty()) {
				searchers.remove(obj);
				return;
			}
			lfp.setPlayer(player);
			for (SearchInstance si : lfp.getSearchInstances()) {
				startSort(si.getEntryRequestType(), si.getInstanceMaskId(), true);
			}
		}
	}

	/**
	 * 登出时清理排队与自动实例状态。
	 * On logout, cleans queue and auto-instance state.
	 *
	 * logging-out player
	 */
	public void onPlayerLogOut(Player player) {
		Integer obj = player.getObjectId();
		int instanceId = player.getInstanceId();
		LookingForParty lfp = searchers.get(obj);
		if (lfp != null) {
			lfp.setPlayer(null);
			if (lfp.isOnStartEnterTask()) {
				for (AutoInstance autoInstance : autoInstances.values()) {
					if (autoInstance.players.containsKey(obj) && !autoInstance.players.get(obj).isInInstance()) {
	/**
	 * 取消进入已匹配副本。
	 * Cancels entry into a matched instance.
	 *
	 * 玩家 / player
	 * instance mask id
	 */
						cancelEnter(player, autoInstance.agt.getInstanceMaskId());
					}
				}
			}
		}
		if (player.isInInstance()) {
			AutoInstance autoInstance = autoInstances.get(instanceId);
			if (autoInstance != null && autoInstance.players.containsKey(obj)) {
				WorldMapInstance instance = autoInstance.instance;
				if (instance != null) {
					autoInstance.players.get(obj).setOnline(false);
					if (!hasOnlinePlayers(autoInstance)) {
						autoInstance = autoInstances.remove(instanceId);
						InstanceService.destroyInstance(instance);
						autoInstance.clear();
					}
				}
			}
		}
	}

	/**
	 * 离开实例时清理自动组队相关状态。
	 * Cleans auto-group state when leaving an instance.
	 *
	 * @param player 玩家 / player
	 */
	public void onLeaveInstance(Player player) {
		if (player.isInInstance()) {
			Integer obj = player.getObjectId();
			int instanceId = player.getInstanceId();
			AutoInstance autoInstance = autoInstances.get(instanceId);
			if (autoInstance != null && autoInstance.players.containsKey(obj)) {
				autoInstance.onLeaveInstance(player);
				if (!hasOnlinePlayers(autoInstance)) {
					WorldMapInstance instance = autoInstance.instance;
					autoInstances.remove(instanceId);
					if (instance != null) {
						InstanceService.destroyInstance(instance);
					}
				} else if (autoInstance.agt.hasRegisterFast()) {
					startSort(EntryRequestType.FAST_GROUP_ENTRY, autoInstance.agt.getInstanceMaskId(), false);
				}
			}
		}
	}

	private boolean hasOnlinePlayers(AutoInstance autoInstance) {
		for (AGPlayer agPlayer : autoInstance.players.values()) {
			if (agPlayer.isOnline()) {
				return true;
			}
		}
		return false;
	}

	private void startSort(EntryRequestType ert, Integer instanceMaskId, boolean checkNewGroup) {
		lock.lock();
		try {
			Collection<Player> players = new HashSet<Player>();
			if (ert.isFastGroupEntry()) {
				for (LookingForParty lfp : orderedSearchers()) {
					if (lfp.getPlayer() == null || lfp.isOnStartEnterTask()) {
						continue;
					}
					for (AutoInstance autoInstance : autoInstances.values()) {
						int searchMaskId = autoInstance.agt.getInstanceMaskId();
						SearchInstance searchInstance = lfp.getSearchInstance(searchMaskId);
						if (searchInstance != null && searchInstance.getEntryRequestType().isFastGroupEntry()) {
							Player owner = lfp.getPlayer();
							if (autoInstance.addPlayer(owner, searchInstance).isAdded()) {
								lfp.setStartEnterTime();
								if (lfp.unregisterInstance(searchMaskId) == 0) {
									players.add(owner);
								}
								PacketSendUtility.sendPacket(lfp.getPlayer(), new SM_AUTO_GROUP(searchMaskId, 4));
							}
						}
					}
				}
				for (Player p : players) {
					searchers.remove(p.getObjectId());
				}
				players.clear();
			}
			if (checkNewGroup) {
				MatchDefinition agt = MatchDefinition.getByMaskId(instanceMaskId);
				AutoInstance autoInstance = agt.getAutoInstance();
				autoInstance.initsialize(instanceMaskId);
				boolean canCreate = false;
				LookingForParty lfp;
				for (LookingForParty candidate : orderedSearchers(instanceMaskId)) {
					lfp = candidate;
					if (lfp.getPlayer() == null || lfp.isOnStartEnterTask()) {
						continue;
					}
					SearchInstance searchInstance = lfp.getSearchInstance(instanceMaskId);
					if (searchInstance != null) {
						if (searchInstance.getEntryRequestType().isGroupEntry()) {
							if (!lfp.getPlayer().isInGroup2()) {
								if (lfp.unregisterInstance(instanceMaskId) == 0) {
									searchers.remove(lfp.getPlayer().getObjectId(), lfp);
								}
								continue;
							}
						}
						AGQuestion question = autoInstance.addPlayer(lfp.getPlayer(), searchInstance);
						if (!question.isFailed()) {
							if (searchInstance.getEntryRequestType().isGroupEntry()) {
								for (Player member : lfp.getPlayer().getPlayerGroup2().getOnlineMembers()) {
									if (searchInstance.getMembers().contains(member.getObjectId())) {
										players.add(member);
									}
								}
							} else {
								players.add(lfp.getPlayer());
							}
						}
						if (question.isReady()) {
							canCreate = true;
						}
					}
				}
				if (canCreate) {
					WorldMapInstance instance = createInstance(agt);
					autoInstance.onInstanceCreate(instance);
					autoInstances.put(instance.getInstanceId(), autoInstance);
					for (Player player : players) {
						Integer obj = player.getObjectId();
						lfp = searchers.get(obj);
						if (lfp != null) {
							lfp.setStartEnterTime();
							if (lfp.unregisterInstance(autoInstance.agt.getInstanceMaskId()) == 0) {
								searchers.remove(obj);
							}
						}
						PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 4));
					}
				} else {
					autoInstance.clear();
				}
				players.clear();
			}
		} finally {
			lock.unlock();
		}
	}

	private List<LookingForParty> orderedSearchers() {
		List<LookingForParty> result = new ArrayList<>(searchers.values());
		result.sort(Comparator.comparingLong(lfp -> lfp.getSearchInstances().stream()
				.mapToLong(SearchInstance::getRegistrationTime).min().orElse(Long.MAX_VALUE)));
		return result;
	}

	private List<LookingForParty> orderedSearchers(int instanceMaskId) {
		List<LookingForParty> result = new ArrayList<>();
		for (LookingForParty lfp : searchers.values()) {
			if (lfp.getSearchInstance(instanceMaskId) != null) {
				result.add(lfp);
			}
		}
		result.sort(Comparator.comparingLong(lfp -> lfp.getSearchInstance(instanceMaskId).getRegistrationTime()));
		return result;
	}

	private boolean canEnter(Player player, EntryRequestType ert, MatchDefinition agt) {
		int mapId = agt.getInstanceMapId();
		int instanceMaskId = agt.getInstanceMaskId();
		if (!agt.isOpen()) {
			return false;
		}
		if (!agt.hasLevelPermit(player.getLevel())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
			return false;
		}
		if (hasCoolDown(player, mapId)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME);
			return false;
		}
		switch (ert) {
		case NEW_GROUP_ENTRY:
			if (!agt.hasRegisterNew()) {
				return false;
			}
			break;
		case FAST_GROUP_ENTRY:
			if (!agt.hasRegisterFast()) {
				return false;
			}
			break;
		case GROUP_ENTRY:
			if (!agt.hasRegisterGroup()) {
				return false;
			}
			PlayerGroup group = player.getPlayerGroup2();
			if (group == null || !group.isLeader(player)) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_NOT_LEADER);
				return false;
			}
			if (agt.getPlayersPerSide() > 0 && group.getOnlineMembers().size() > agt.getPlayersPerSide()) {
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_TOO_MANY_MEMBERS(agt.getPlayersPerSide(), Integer.toString(mapId)));
				return false;
			}
			for (Player member : group.getMembers()) {
				LookingForParty lfp = searchers.get(member.getObjectId());
				if (member != player && lfp != null && lfp.isRegistredInstance(instanceMaskId)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (hasCoolDown(member, mapId)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (!agt.hasLevelPermit(member.getLevel())) {
					PacketSendUtility.sendPacket(member, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
			}
			break;
		}
		return true;
	}

	private AutoInstance getAutoInstance(Player player, int instanceMaskId) {
		for (AutoInstance autoInstance : autoInstances.values()) {
			if (autoInstance.agt.getInstanceMaskId() == instanceMaskId
					&& autoInstance.players.containsKey(player.getObjectId())) {
				return autoInstance;
			}
		}
		return null;
	}

	private boolean hasCoolDown(Player player, int worldId) {
		return !InstanceLimitService.status(player, worldId).allowed();
	}

	private WorldMapInstance createInstance(MatchDefinition type) {
		return InstanceService.getNextAvailableInstance(type.getInstanceMapId(), 0, type.getCreationId(),
				DynamicInstance.OWNER_MATCH, type.getInstanceMaskId(), type.getDifficultId());
	}

	private void startPenalty(final Integer obj) {
		if (penaltys.contains(obj)) {
			penaltys.remove(obj);
		}
		penaltys.add(obj);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (penaltys.contains(obj)) {
					penaltys.remove(obj);
				}
			}
		}, 10000);
	}

	/**
	 * 注销指定副本 mask 的自动组队排队。
	 * Unregisters auto-group queue entries for the given instance mask.
	 *
	 * instance mask id
	 */
	public void unRegisterInstance(byte instanceMaskId) {
		for (Map.Entry<Integer, LookingForParty> entry : searchers.entrySet()) {
			LookingForParty lfp = entry.getValue();
			if (lfp.isRegistredInstance(instanceMaskId)) {
				if (lfp.getPlayer() != null) {
	/**
	 * 获取服务单例（优先 Spring 提供者）。
	 * Returns the service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
					getInstance().unregisterLooking(lfp.getPlayer(), instanceMaskId);
				} else {
	/**
	 * 获取服务单例（优先 Spring 提供者）。
	 * Returns the service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
					getInstance().unRegisterSearchInstance(null, lfp.getSearchInstance(instanceMaskId));
					if (lfp.unregisterInstance(instanceMaskId) == 0) {
						searchers.remove(entry.getKey(), lfp);
					}
				}
			}
		}
	}

	private void unRegisterSearchInstance(Player player, SearchInstance si) {
		int instanceMaskId = si.getInstanceMaskId();
		MatchDefinition type = MatchDefinition.getByMaskId(instanceMaskId);
		if (si.getEntryRequestType().isGroupEntry() && si.getMembers() != null) {
			for (Integer obj : si.getMembers()) {
				Player member = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(obj);
				if (member != null) {
					if (type != null && type.hasHudRegister() && type.isOpen()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					}
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 2));
				}
			}
		}
		if (player != null) {
			if (type != null && type.hasHudRegister() && type.isOpen()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 2));
		}
	}

	/**
	 * 注销并清理指定实例 ID 的自动组队实例。
	 * Unregisters and cleans up the auto-group instance for the given instance id.
	 *
	 * instance id
	 */
	public void unRegisterInstance(Integer instanceId) {
		AutoInstance autoInstance = autoInstances.remove(instanceId);
		if (autoInstance != null) {
			WorldMapInstance instance = autoInstance.instance;
			if (instance != null) {
				InstanceService.destroyInstance(instance);
			}
			autoInstance.clear();
		}
	}

	public void unRegisterInstance(WorldMapInstance instance) {
		unRegisterInstance(instance.getInstanceId());
	}

	/**
	 * 判断实例是否由自动组队创建。
	 * Returns whether the instance was created by auto-group.
	 *
	 * instance id
	 *
	 * @param instanceId
	 * @return 是否自动实例 / whether auto instance
	 */
	public boolean isAutoInstance(int instanceId) {
		return autoInstances.containsKey(instanceId);
	}

	public boolean isAutoInstance(Player player) {
		return isAutoInstance(player.getInstanceId());
	}

	/**
	 * 获取服务单例（优先 Spring 提供者）。
	 * Returns the service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
	public static AutoGroupService getInstance() {
		ObjectProvider<AutoGroupService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<AutoGroupService> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {
		private static final AutoGroupService INSTANCE = new AutoGroupService();
	}
}

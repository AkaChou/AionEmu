package com.aionemu.gameserver.services;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameBattlefieldServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.model.autogroup.AGPlayer;
import com.aionemu.gameserver.model.autogroup.AGQuestion;
import com.aionemu.gameserver.model.autogroup.AutoGroupType;
import com.aionemu.gameserver.model.autogroup.AutoInstance;
import com.aionemu.gameserver.model.autogroup.EntryRequestType;
import com.aionemu.gameserver.model.autogroup.LookingForParty;
import com.aionemu.gameserver.model.autogroup.SearchInstance;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.model.templates.InstanceCooltime;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.instance.EngulfedOphidanBridgeService;
import com.aionemu.gameserver.services.instance.GrandArenaTrainingCampService;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;
import com.aionemu.gameserver.services.instance.IDRunService;
import com.aionemu.gameserver.services.instance.IdgelDomeLandmarkService;
import com.aionemu.gameserver.services.instance.IdgelDomeService;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.instance.IronWallWarfrontService;
import com.aionemu.gameserver.services.instance.KamarBattlefieldService;
import com.aionemu.gameserver.services.instance.SuspiciousOphidanBridgeService;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldMapInstanceFactory;

import java.util.List;
import java.util.Map;
/**
 * 自动组队/匹配服务：管理副本排队、入场确认、实例创建与登录/登出时的匹配状态恢复。
 * Matchmaking service: manages instance queues, entry confirmation, instance creation, and match state on login/logout.
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
		AutoGroupType agt = AutoGroupType.getAGTByMaskId(instanceMaskId);
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
				if (agt.isDredgion()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				if (agt.isAsyunatar()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				if (agt.isKamar()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				if (agt.isOphidan()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				if (agt.isSuspiciousOphidan()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				if (agt.isBastion()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				if (agt.isIdgelDome()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				if (agt.isIdgelDomeLandmark()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				if (agt.isGrandArenaTrainingCamp()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				if (agt.isIDRun()) {
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6, true));
				}
				PacketSendUtility.sendPacket(member, new SM_SYSTEM_MESSAGE(1400194, agt.getInstanceMapId()));
				PacketSendUtility.sendPacket(member,
						new SM_AUTO_GROUP(instanceMaskId, 1, ert.getId(), player.getName()));
			}
		} else {
			if (agt.isDredgion()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			if (agt.isAsyunatar()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			if (agt.isKamar()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			if (agt.isOphidan()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			if (agt.isSuspiciousOphidan()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			if (agt.isBastion()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			if (agt.isIdgelDome()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			if (agt.isIdgelDomeLandmark()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			if (agt.isHallOfTenacity()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			if (agt.isGrandArenaTrainingCamp()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6, true));
			}
			if (agt.isIDRun()) {
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
		if (instance == null || instance.players.get(player.getObjectId()).isPressedEnter()) {
			return;
		}
		if (player.isInGroup2()) {
			PlayerGroupService.removePlayer(player);
		}
		if (player.isInAlliance2()) {
			PlayerAllianceService.removePlayer(player);
		}
		instance.onPressEnter(player);
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
			if (autoInstance.agt.isDredgion() && GameFeatureServices.dredgionService().isDredgionAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			if (autoInstance.agt.isAsyunatar() && GameFeatureServices.asyunatarService().isAsyunatarAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			if (autoInstance.agt.isKamar() && GameBattlefieldServices.kamarBattlefieldService().isKamarAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			if (autoInstance.agt.isOphidan() && GameBattlefieldServices.engulfedOphidanBridgeService().isOphidanAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			if (autoInstance.agt.isSuspiciousOphidan()
					&& GameBattlefieldServices.suspiciousOphidanBridgeService().isSuspiciousAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			if (autoInstance.agt.isBastion() && GameBattlefieldServices.ironWallWarfrontService().isBastionAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			if (autoInstance.agt.isIdgelDome() && GameBattlefieldServices.idgelDomeService().isIdgelAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			if (autoInstance.agt.isIdgelDomeLandmark()
					&& GameBattlefieldServices.idgelDomeLandmarkService().isLandmarkAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			if (autoInstance.agt.isHallOfTenacity() && GameBattlefieldServices.hallOfTenacityService().isHallAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			if (autoInstance.agt.isGrandArenaTrainingCamp()
					&& GameBattlefieldServices.grandArenaTrainingCampService().isGrandArenaTrainingCampAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			}
			if (autoInstance.agt.isIDRun() && GameBattlefieldServices.idRunService().isIDRunAvailable()) {
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
		if (GameFeatureServices.dredgionService().isDredgionAvailable() && player.getLevel() > DredgionService2.minLevel
				&& player.getLevel() < DredgionService2.capLevel
				&& !GameFeatureServices.dredgionService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(
					GameFeatureServices.dredgionService().getInstanceMaskId(player), SM_AUTO_GROUP.wnd_EntryIcon));
		}
		if (GameFeatureServices.asyunatarService().isAsyunatarAvailable() && player.getLevel() > AsyunatarService.minLevel
				&& player.getLevel() < AsyunatarService.capLevel
				&& !GameFeatureServices.asyunatarService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(AsyunatarService.maskId, SM_AUTO_GROUP.wnd_EntryIcon));
		}
		if (GameBattlefieldServices.kamarBattlefieldService().isKamarAvailable()
				&& player.getLevel() > KamarBattlefieldService.minLevel
				&& player.getLevel() < KamarBattlefieldService.capLevel
				&& !GameBattlefieldServices.kamarBattlefieldService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(KamarBattlefieldService.maskId, SM_AUTO_GROUP.wnd_EntryIcon));
		}
		if (GameBattlefieldServices.engulfedOphidanBridgeService().isOphidanAvailable()
				&& player.getLevel() > EngulfedOphidanBridgeService.minLevel
				&& player.getLevel() < EngulfedOphidanBridgeService.capLevel
				&& !GameBattlefieldServices.engulfedOphidanBridgeService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(EngulfedOphidanBridgeService.maskId, SM_AUTO_GROUP.wnd_EntryIcon));
		}
		if (GameBattlefieldServices.suspiciousOphidanBridgeService().isSuspiciousAvailable()
				&& player.getLevel() > SuspiciousOphidanBridgeService.minLevel
				&& player.getLevel() < SuspiciousOphidanBridgeService.capLevel
				&& !GameBattlefieldServices.suspiciousOphidanBridgeService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(SuspiciousOphidanBridgeService.maskId, SM_AUTO_GROUP.wnd_EntryIcon));
		}
		if (GameBattlefieldServices.ironWallWarfrontService().isBastionAvailable()
				&& player.getLevel() > IronWallWarfrontService.minLevel
				&& player.getLevel() < IronWallWarfrontService.capLevel
				&& !GameBattlefieldServices.ironWallWarfrontService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(IronWallWarfrontService.maskId, SM_AUTO_GROUP.wnd_EntryIcon));
		}
		if (GameBattlefieldServices.idgelDomeService().isIdgelAvailable() && player.getLevel() > IdgelDomeService.minLevel
				&& player.getLevel() < IdgelDomeService.capLevel
				&& !GameBattlefieldServices.idgelDomeService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(IdgelDomeService.maskId, SM_AUTO_GROUP.wnd_EntryIcon));
		}
		if (GameBattlefieldServices.idgelDomeLandmarkService().isLandmarkAvailable()
				&& player.getLevel() > IdgelDomeLandmarkService.minLevel
				&& player.getLevel() < IdgelDomeLandmarkService.capLevel
				&& !GameBattlefieldServices.idgelDomeLandmarkService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(IdgelDomeLandmarkService.maskId, SM_AUTO_GROUP.wnd_EntryIcon));
		}
		if (GameBattlefieldServices.hallOfTenacityService().isHallAvailable() && player.getLevel() > HallOfTenacityService.minLevel
				&& player.getLevel() < HallOfTenacityService.capLevel
				&& !GameBattlefieldServices.hallOfTenacityService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(HallOfTenacityService.maskId, SM_AUTO_GROUP.wnd_EntryIcon));
		}
		if (GameBattlefieldServices.grandArenaTrainingCampService().isGrandArenaTrainingCampAvailable()
				&& player.getLevel() > GrandArenaTrainingCampService.minLevel
				&& player.getLevel() < GrandArenaTrainingCampService.capLevel
				&& !GameBattlefieldServices.grandArenaTrainingCampService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_AUTO_GROUP(GrandArenaTrainingCampService.maskId, SM_AUTO_GROUP.wnd_EntryIcon));
		}
		if (GameBattlefieldServices.idRunService().isIDRunAvailable() && player.getLevel() > IDRunService.minLevel
				&& player.getLevel() < IDRunService.capLevel && !GameBattlefieldServices.idRunService().hasCoolDown(player)) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(IDRunService.maskId, SM_AUTO_GROUP.wnd_EntryIcon));
		}
		Integer obj = player.getObjectId();
		LookingForParty lfp = searchers.get(obj);
		if (lfp != null) {
			for (SearchInstance searchInstance : lfp.getSearchInstances()) {
				if (searchInstance.getEntryRequestType().isGroupEntry() && !player.isInGroup2()) {
					int instanceMaskId = searchInstance.getInstanceMaskId();
					lfp.unregisterInstance(instanceMaskId);
					if (searchInstance.isDredgion() && GameFeatureServices.dredgionService().isDredgionAvailable()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					if (searchInstance.isAsyunatar() && GameFeatureServices.asyunatarService().isAsyunatarAvailable()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					if (searchInstance.isKamar() && GameBattlefieldServices.kamarBattlefieldService().isKamarAvailable()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					if (searchInstance.isOphidan() && GameBattlefieldServices.engulfedOphidanBridgeService().isOphidanAvailable()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					if (searchInstance.isSuspiciousOphidan()
							&& GameBattlefieldServices.suspiciousOphidanBridgeService().isSuspiciousAvailable()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					if (searchInstance.isBastion() && GameBattlefieldServices.ironWallWarfrontService().isBastionAvailable()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					if (searchInstance.isIdgelDome() && GameBattlefieldServices.idgelDomeService().isIdgelAvailable()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					if (searchInstance.isIdgelDomeLandmark()
							&& GameBattlefieldServices.idgelDomeLandmarkService().isLandmarkAvailable()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					if (searchInstance.isHallOfTenacity() && GameBattlefieldServices.hallOfTenacityService().isHallAvailable()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					if (searchInstance.isGrandArenaTrainingCamp()
							&& GameBattlefieldServices.grandArenaTrainingCampService().isGrandArenaTrainingCampAvailable()) {
						PacketSendUtility.sendPacket(player,
								new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					}
					if (searchInstance.isIDRun() && GameBattlefieldServices.idRunService().isIDRunAvailable()) {
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
				if (searchInstance.isDredgion()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
				if (searchInstance.isAsyunatar()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
				if (searchInstance.isKamar()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
				if (searchInstance.isOphidan()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
				if (searchInstance.isSuspiciousOphidan()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
				if (searchInstance.isBastion()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
				if (searchInstance.isIdgelDome()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
				if (searchInstance.isIdgelDomeLandmark()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
				if (searchInstance.isHallOfTenacity()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
				if (searchInstance.isGrandArenaTrainingCamp()) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(searchInstance.getInstanceMaskId(), SM_AUTO_GROUP.wnd_EntryIcon, true));
				}
				if (searchInstance.isIDRun()) {
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
				for (LookingForParty lfp : searchers.values()) {
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
				AutoGroupType agt = AutoGroupType.getAGTByMaskId(instanceMaskId);
				AutoInstance autoInstance = agt.getAutoInstance();
				autoInstance.initsialize(instanceMaskId);
				boolean canCreate = false;
				Iterator<LookingForParty> iter = searchers.values().iterator();
				LookingForParty lfp;
				while (iter.hasNext()) {
					lfp = iter.next();
					if (lfp.getPlayer() == null || lfp.isOnStartEnterTask()) {
						continue;
					}
					SearchInstance searchInstance = lfp.getSearchInstance(instanceMaskId);
					if (searchInstance != null) {
						if (searchInstance.getEntryRequestType().isGroupEntry()) {
							if (!lfp.getPlayer().isInGroup2()) {
								if (lfp.unregisterInstance(instanceMaskId) == 0) {
									iter.remove();
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
							break;
						}
					}
				}
				if (canCreate) {
					WorldMapInstance instance = createInstance(agt.getInstanceMapId(), agt.getDifficultId());
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

	private boolean canEnter(Player player, EntryRequestType ert, AutoGroupType agt) {
		int mapId = agt.getInstanceMapId();
		int instanceMaskId = agt.getInstanceMaskId();
		if (!agt.hasLevelPermit(player.getLevel())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
			return false;
		}
		if (agt.isDredgion() && !GameFeatureServices.dredgionService().isDredgionAvailable()) {
			return false;
		} else if (agt.isAsyunatar() && !GameFeatureServices.asyunatarService().isAsyunatarAvailable()) {
			return false;
		} else if (agt.isKamar() && !GameBattlefieldServices.kamarBattlefieldService().isKamarAvailable()) {
			return false;
		} else if (agt.isOphidan() && !GameBattlefieldServices.engulfedOphidanBridgeService().isOphidanAvailable()) {
			return false;
		} else if (agt.isSuspiciousOphidan() && !GameBattlefieldServices.suspiciousOphidanBridgeService().isSuspiciousAvailable()) {
			return false;
		} else if (agt.isBastion() && !GameBattlefieldServices.ironWallWarfrontService().isBastionAvailable()) {
			return false;
		} else if (agt.isIdgelDome() && !GameBattlefieldServices.idgelDomeService().isIdgelAvailable()) {
			return false;
		} else if (agt.isIdgelDomeLandmark() && !GameBattlefieldServices.idgelDomeLandmarkService().isLandmarkAvailable()) {
			return false;
		} else if (agt.isHallOfTenacity() && !GameBattlefieldServices.hallOfTenacityService().isHallAvailable()) {
			return false;
		} else if (agt.isGrandArenaTrainingCamp()
				&& !GameBattlefieldServices.grandArenaTrainingCampService().isGrandArenaTrainingCampAvailable()) {
			return false;
		} else if (agt.isIDRun() && !GameBattlefieldServices.idRunService().isIDRunAvailable()) {
			return false;
		} else if (hasCoolDown(player, mapId)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME);
			return false;
		}
		switch (ert) {
		case NEW_GROUP_ENTRY:
			if (!agt.hasRegisterNew()) {
				return false;
			}
			if (agt.isHallOfTenacity() && GameBattlefieldServices.hallOfTenacityService().hasCoolDown(player)) {
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(player.getName()));
				return false;
			} else if (hasCoolDown(player, mapId)) {
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(player.getName()));
				return false;
			}
			if (!agt.hasLevelPermit(player.getLevel())) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(player.getName()));
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
			if (agt.isHarmonyArena() || agt.isTrainingHarmonyArena()) {
				if (group.getOnlineMembers().size() > 2) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_TOO_MANY_MEMBERS(3, Integer.toString(mapId)));
					return false;
				}
			}
			for (Player member : group.getMembers()) {
				if (group.getLeaderObject().equals(member)) {
					continue;
				}
				LookingForParty lfp = searchers.get(member.getObjectId());
				if (lfp != null && lfp.isRegistredInstance(instanceMaskId)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isPvPSoloArena() || agt.isTrainingPvPSoloArena() || agt.isPvPFFAArena()
						|| agt.isTrainingPvPFFAArena() || agt.isTrainingHarmonyArena() || agt.isGloryArena()
						|| agt.isHarmonyArena()) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isDredgion() && GameFeatureServices.dredgionService().hasCoolDown(member)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isAsyunatar() && GameFeatureServices.asyunatarService().hasCoolDown(member)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isKamar() && GameBattlefieldServices.kamarBattlefieldService().hasCoolDown(member)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isOphidan() && GameBattlefieldServices.engulfedOphidanBridgeService().hasCoolDown(member)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isSuspiciousOphidan() && GameBattlefieldServices.suspiciousOphidanBridgeService().hasCoolDown(member)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isBastion() && GameBattlefieldServices.ironWallWarfrontService().hasCoolDown(member)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isIdgelDome() && GameBattlefieldServices.idgelDomeService().hasCoolDown(member)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isIdgelDomeLandmark() && GameBattlefieldServices.idgelDomeLandmarkService().hasCoolDown(member)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isGrandArenaTrainingCamp() && GameBattlefieldServices.grandArenaTrainingCampService().hasCoolDown(member)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				}
				if (agt.isIDRun() && GameBattlefieldServices.idRunService().hasCoolDown(member)) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_MEMBER(member.getName()));
					return false;
				} else if (hasCoolDown(member, mapId)) {
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
		int instanceCooldownRate = InstanceService.getInstanceRate(player, worldId);
		int useDelay = 0;
		int instanceCooldown = 0;
		InstanceCooltime clt = DataManager.INSTANCE_COOLTIME_DATA.getInstanceCooltimeByWorldId(worldId);
		if (clt != null) {
			instanceCooldown = clt.getEntCoolTime();
		}
		if (instanceCooldownRate > 0) {
			useDelay = instanceCooldown / instanceCooldownRate;
		}
		return player.getPortalCooldownList().isPortalUseDisabled(worldId) && useDelay > 0;
	}

	private WorldMapInstance createInstance(int worldId, byte difficultId) {
		WorldMap map = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId);
		int nextInstanceId = map.getNextInstanceId();
		WorldMapInstance worldMapInstance = WorldMapInstanceFactory.createWorldMapInstance(map, nextInstanceId);
		map.addInstance(nextInstanceId, worldMapInstance);
		SpawnEngine.spawnInstance(worldId, worldMapInstance.getInstanceId(), difficultId);
		GameEngineServices.instanceEngine().onInstanceCreate(worldMapInstance);
		return worldMapInstance;
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
		if (si.getEntryRequestType().isGroupEntry() && si.getMembers() != null) {
			for (Integer obj : si.getMembers()) {
				Player member = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(obj);
				if (member != null) {
					if (si.isDredgion() && GameFeatureServices.dredgionService().isDredgionAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					} else if (si.isAsyunatar() && GameFeatureServices.asyunatarService().isAsyunatarAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					} else if (si.isKamar() && GameBattlefieldServices.kamarBattlefieldService().isKamarAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					} else if (si.isOphidan() && GameBattlefieldServices.engulfedOphidanBridgeService().isOphidanAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					} else if (si.isSuspiciousOphidan()
							&& GameBattlefieldServices.suspiciousOphidanBridgeService().isSuspiciousAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					} else if (si.isBastion() && GameBattlefieldServices.ironWallWarfrontService().isBastionAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					} else if (si.isIdgelDome() && GameBattlefieldServices.idgelDomeService().isIdgelAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					} else if (si.isIdgelDomeLandmark()
							&& GameBattlefieldServices.idgelDomeLandmarkService().isLandmarkAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					} else if (si.isHallOfTenacity() && GameBattlefieldServices.hallOfTenacityService().isHallAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					} else if (si.isGrandArenaTrainingCamp()
							&& GameBattlefieldServices.grandArenaTrainingCampService().isGrandArenaTrainingCampAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					} else if (si.isIDRun() && GameBattlefieldServices.idRunService().isIDRunAvailable()) {
						PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 6));
					}
					PacketSendUtility.sendPacket(member, new SM_AUTO_GROUP(instanceMaskId, 2));
				}
			}
		}
		if (player != null) {
			if (si.isDredgion() && GameFeatureServices.dredgionService().isDredgionAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			} else if (si.isAsyunatar() && GameFeatureServices.asyunatarService().isAsyunatarAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			} else if (si.isKamar() && GameBattlefieldServices.kamarBattlefieldService().isKamarAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			} else if (si.isOphidan() && GameBattlefieldServices.engulfedOphidanBridgeService().isOphidanAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			} else if (si.isSuspiciousOphidan()
					&& GameBattlefieldServices.suspiciousOphidanBridgeService().isSuspiciousAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			} else if (si.isBastion() && GameBattlefieldServices.ironWallWarfrontService().isBastionAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			} else if (si.isIdgelDome() && GameBattlefieldServices.idgelDomeService().isIdgelAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			} else if (si.isIdgelDomeLandmark() && GameBattlefieldServices.idgelDomeLandmarkService().isLandmarkAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			} else if (si.isHallOfTenacity() && GameBattlefieldServices.hallOfTenacityService().isHallAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			} else if (si.isGrandArenaTrainingCamp()
					&& GameBattlefieldServices.grandArenaTrainingCampService().isGrandArenaTrainingCampAvailable()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 6));
			} else if (si.isIDRun() && GameBattlefieldServices.idRunService().isIDRunAvailable()) {
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

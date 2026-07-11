package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameBattlefieldServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.configs.main.PvPModConfig;
import com.aionemu.gameserver.model.autogroup.EntryRequestType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.instance.EngulfedOphidanBridgeService;
import com.aionemu.gameserver.services.instance.GrandArenaTrainingCampService;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;
import com.aionemu.gameserver.services.instance.IDRunService;
import com.aionemu.gameserver.services.instance.IdgelDomeLandmarkService;
import com.aionemu.gameserver.services.instance.IdgelDomeService;
import com.aionemu.gameserver.services.instance.IronWallWarfrontService;
import com.aionemu.gameserver.services.instance.KamarBattlefieldService;
import com.aionemu.gameserver.services.instance.SuspiciousOphidanBridgeService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 自动匹配/副本排队窗口操作的客户端包。
 * Client packet for auto-group and instance matchmaking window actions.
 */
@Slf4j

public class CM_AUTO_GROUP extends AionClientPacket {
	private byte instanceMaskId;
	private byte windowId;
	private byte entryRequestId;

	public CM_AUTO_GROUP(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		instanceMaskId = (byte) readD();
		windowId = (byte) readC();
		entryRequestId = (byte) readC();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (!AutoGroupConfig.AUTO_GROUP_ENABLED) {
			PacketSendUtility.sendMessage(player, "Auto Group is disabled");
			return;
		}
		switch (windowId) {
		case 100:
			EntryRequestType ert = EntryRequestType.getTypeById(entryRequestId);
			if (ert == null) {
				return;
			}
			GameCoreGameplayServices.autoGroupService().startLooking(player, instanceMaskId, ert);
			break;
		case 101:
			GameCoreGameplayServices.autoGroupService().unregisterLooking(player, instanceMaskId);
			break;
		case 102:
			GameCoreGameplayServices.autoGroupService().pressEnter(player, instanceMaskId);
			break;
		case 103:
			GameCoreGameplayServices.autoGroupService().cancelEnter(player, instanceMaskId);
			break;
		case 104:
			GameFeatureServices.dredgionService().showWindow(player, instanceMaskId);
			GameBattlefieldServices.kamarBattlefieldService().showWindow(player, instanceMaskId);
			GameBattlefieldServices.engulfedOphidanBridgeService().showWindow(player, instanceMaskId);
			GameBattlefieldServices.ironWallWarfrontService().showWindow(player, instanceMaskId);
			GameBattlefieldServices.idgelDomeService().showWindow(player, instanceMaskId);
			// 版本 5.1 / Ver. 5.1
			GameFeatureServices.asyunatarService().showWindow(player, instanceMaskId);
			GameBattlefieldServices.idgelDomeLandmarkService().showWindow(player, instanceMaskId);
			GameBattlefieldServices.suspiciousOphidanBridgeService().showWindow(player, instanceMaskId);
			// 版本 5.3 / Ver. 5.3
			GameBattlefieldServices.hallOfTenacityService().showWindow(player, instanceMaskId);
			// 版本 5.6 / Ver. 5.6
			GameBattlefieldServices.grandArenaTrainingCampService().showWindow(player, instanceMaskId);
			// 版本 5.8 / Ver. 5.8
			GameBattlefieldServices.idRunService().showWindow(player, instanceMaskId);
			break;
		case 105:
			break;
		}
		if (PvPModConfig.BG_ENABLED) {
			GameFeatureServices.ladderService().handleWindow(player, windowId, entryRequestId);
		}
	}
}

package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.configs.main.PvPModConfig;
import com.aionemu.gameserver.model.autogroup.EntryRequestType;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.RetailMatchmakingService;
import com.aionemu.gameserver.services.instance.TournamentService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 自动匹配/副本排队窗口操作的客户端包。
 * Client packet for auto-group and instance matchmaking window actions.
 */
@Slf4j

public class CM_AUTO_GROUP extends AionClientPacket {
	private int instanceMaskId;
	private byte windowId;
	private byte entryRequestId;

	public CM_AUTO_GROUP(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		instanceMaskId = readD();
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
		MatchDefinition definition = MatchDefinition.getByMaskId(instanceMaskId);
		boolean tournament = definition != null && definition.isTournament();
		RetailMatchmakingService matchmaking = (RetailMatchmakingService) GameCoreGameplayServices.autoGroupService();
		switch (windowId) {
		case 100:
			EntryRequestType ert = EntryRequestType.getTypeById(entryRequestId);
			if (ert == null) {
				return;
			}
			if (tournament) {
				TournamentService.startLooking(player, definition, ert);
			} else {
				matchmaking.startLooking(player, instanceMaskId, ert);
			}
			break;
		case 101:
			if (tournament) {
				TournamentService.unregister(player, instanceMaskId);
			} else {
				matchmaking.unregisterLooking(player, instanceMaskId);
			}
			break;
		case 102:
			if (tournament) {
				TournamentService.pressEnter(player, instanceMaskId);
			} else {
				matchmaking.pressEnter(player, instanceMaskId);
			}
			break;
		case 103:
			if (tournament) {
				TournamentService.cancelEnter(player, instanceMaskId);
			} else {
				matchmaking.cancelEnter(player, instanceMaskId);
			}
			break;
		case 104:
			if (tournament) {
				TournamentService.showWindow(player, definition);
				break;
			}
			matchmaking.showWindow(player, definition);
			break;
		case 105:
			break;
		}
		if (PvPModConfig.BG_ENABLED) {
			GameFeatureServices.ladderService().handleWindow(player, windowId, entryRequestId);
		}
	}
}

package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.siegeservice.BattlefieldUnionService;

/**
 * 战场联盟报名的客户端包（要塞攻城期间）。
 * Client packet to register for Battlefield Union during fortress sieges.
 *
 * @author wanke
 */
public class CM_BATTLEFIELD_UNION_REGISTER extends AionClientPacket {
	private int requestId;
	private int fortressId;

	public CM_BATTLEFIELD_UNION_REGISTER(int opcode, AionConnection.State state, AionConnection.State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		requestId = readD();
	}

	@Override
	protected void runImpl() {
		if (GameFeatureServices.siegeService().isSiegeInProgress(1011)) {
			GameCoreGameplayServices.battlefieldUnionService().onRegister(getConnection().getActivePlayer(), requestId, 1011);
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1131)) {
			GameCoreGameplayServices.battlefieldUnionService().onRegister(getConnection().getActivePlayer(), requestId, 1131);
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1132)) {
			GameCoreGameplayServices.battlefieldUnionService().onRegister(getConnection().getActivePlayer(), requestId, 1132);
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1141)) {
			GameCoreGameplayServices.battlefieldUnionService().onRegister(getConnection().getActivePlayer(), requestId, 1141);
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1221)) {
			GameCoreGameplayServices.battlefieldUnionService().onRegister(getConnection().getActivePlayer(), requestId, 1221);
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1231)) {
			GameCoreGameplayServices.battlefieldUnionService().onRegister(getConnection().getActivePlayer(), requestId, 1231);
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(1241)) {
			GameCoreGameplayServices.battlefieldUnionService().onRegister(getConnection().getActivePlayer(), requestId, 1241);
		} else if (GameFeatureServices.siegeService().isSiegeInProgress(7011)) {
			GameCoreGameplayServices.battlefieldUnionService().onRegister(getConnection().getActivePlayer(), requestId, 7011);
		}
	}
}

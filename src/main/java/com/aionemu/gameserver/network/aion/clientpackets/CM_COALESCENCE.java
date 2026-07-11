package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 请求装备融合（凝聚）的客户端包。
 * Client packet requesting equipment coalescence.
 *
 * @author Ranastic
 */
@Slf4j

public class CM_COALESCENCE extends AionClientPacket {
	private int mainItemObjId;
	private int materialCount;
	private List<Integer> materialItemObjId;
	private int ItemSize;
	private int upgradedItemObjectId;
	private int Items;
	private List<Integer> ItemsList = new ArrayList();

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_COALESCENCE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		materialItemObjId = new ArrayList<Integer>();
		mainItemObjId = readD();
		materialCount = readH();
		for (int i = 0; i < materialCount; i++) {
			materialItemObjId.add(readD());
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null || !player.isSpawned()) {
			return;
		}
		if (player.getController().isInShutdownProgress()) {
			return;
		}
		GameFeatureServices.coalescenceService().letsCoalescence(player, mainItemObjId, materialItemObjId);
	}
}

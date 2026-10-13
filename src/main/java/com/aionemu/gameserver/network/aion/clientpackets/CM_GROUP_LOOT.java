package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.services.drop.DropDistributionService;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 小队/联盟掉落分配（掷骰或竞价）的客户端包。
 * Client packet for group/alliance loot distribution (roll or bid).
 * @author Rhys2002
 */
public class CM_GROUP_LOOT extends AionClientPacket {

	@SuppressWarnings("unused")
	private int index;
	@SuppressWarnings("unused")
	private int itemId;
	@SuppressWarnings("unused")
	private int npcId;
	private int distributionId;
	private int roll;
	private long bid;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_GROUP_LOOT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		readD();
		index = readD();
		readD();
		itemId = readD();
		readC();
		readC(); // 3.0
		readC(); // 3.5
		npcId = readD();
		distributionId = readC();// 2：掷骰 3：竞价 / 2: Roll 3: Bid
		roll = readD();// 0：未掷骰 1：已掷骰 / 0: Never Rolled 1: Rolled
		bid = readQ();// 0：无竞价，否则为竞价金额 / 0: No Bid else bid amount
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		if (distributionId == 2) {
			DropDistributionService.getInstance().handleRoll(player, roll, itemId, npcId, index);
		} else if (distributionId == 3) {
			DropDistributionService.getInstance().handleBid(player, bid, itemId, npcId, index);
		}
	}
}

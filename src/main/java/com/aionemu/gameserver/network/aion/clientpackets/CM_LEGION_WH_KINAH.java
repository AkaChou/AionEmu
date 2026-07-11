package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionHistoryType;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionPermissionsMask;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 军团仓库基纳存取的客户端包。
 * Client packet for legion warehouse kinah deposit/withdraw.
 *
 * @author ATracer
 */
public class CM_LEGION_WH_KINAH extends AionClientPacket {
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LEGION_WH_KINAH(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	private long amount;
	private int operation;
	/**
	 * 读取军团仓库基纳操作类型与数量。
	 * Reads legion warehouse kinah operation and amount.
	 */
	@Override
	protected void readImpl() {
		this.amount = readQ();
		this.operation = readC();
	}
	/**
	 * 按权限存入或取出军团仓库基纳。
	 * Deposits or withdraws legion warehouse kinah by rights.
	 */
	@Override
	protected void runImpl() {
		final Player activePlayer = getConnection().getActivePlayer();

		Legion legion = activePlayer.getLegion();
		if (legion != null) {
			LegionMember LM = GameCoreGameplayServices.legionService().getLegionMember(activePlayer.getObjectId());
			switch (operation) {
			case 0:
				if (!LM.hasRights(LegionPermissionsMask.WH_DEPOSIT)) {
					// 你无权使用军团仓库。 / You do not have the authority to use the Legion warehouse.
					PacketSendUtility.sendPacket(activePlayer, new SM_SYSTEM_MESSAGE(1300322));
					return;
				}
				if (activePlayer.getStorage(StorageType.LEGION_WAREHOUSE.getId()).tryDecreaseKinah(amount)) {
					activePlayer.getInventory().increaseKinah(amount);
					GameCoreGameplayServices.legionService().addHistory(legion, activePlayer.getName(),
							LegionHistoryType.KINAH_WITHDRAW, 2, Long.toString(amount));
				}
				break;
			case 1:
				if (!LM.hasRights(LegionPermissionsMask.WH_WITHDRAWAL)) {
					// 你无权使用军团仓库。 / You do not have the authority to use the Legion warehouse.
					PacketSendUtility.sendPacket(activePlayer, new SM_SYSTEM_MESSAGE(1300322));
					return;
				}
				if (activePlayer.getInventory().tryDecreaseKinah(amount)) {
					activePlayer.getStorage(StorageType.LEGION_WAREHOUSE.getId()).increaseKinah(amount);
					GameCoreGameplayServices.legionService().addHistory(legion, activePlayer.getName(),
							LegionHistoryType.KINAH_DEPOSIT, 2, Long.toString(amount));
				}
				break;
			}
		}
	}
}

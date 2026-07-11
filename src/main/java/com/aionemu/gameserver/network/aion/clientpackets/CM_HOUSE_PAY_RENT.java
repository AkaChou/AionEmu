package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;

import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_PAY_RENT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 支付房屋维护/租金的客户端包。
 * Client packet for paying house maintenance rent.
 */
public class CM_HOUSE_PAY_RENT extends AionClientPacket {

	int weekCount;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_HOUSE_PAY_RENT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		weekCount = readC();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (!HousingConfig.ENABLE_HOUSE_PAY) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_F2P_CASH_HOUSE_FEE_FREE);
			return;
		}
		House house = player.getActiveHouse();
		long toPay = house.getLand().getMaintenanceFee() * weekCount;
		if (toPay <= 0) {
			return;
		}
		if (player.getInventory().getKinah() < toPay) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_NOT_ENOUGH_MONEY);
			return;
		}
		long payTime = house.getNextPay() != null ? house.getNextPay().getTime() : (long) GameHousingServices.maintenanceTask().getRunTime() * 1000;
		int counter = weekCount;
		while ((--counter) >= 0) {
			payTime += GameHousingServices.maintenanceTask().getPeriod() * 1000L; // Convert seconds to milliseconds
		}

		// 检查是否试图预付超过 4 周 / Check if trying to pay more than 4 weeks in advance
		long runTimeMillis = (long) GameHousingServices.maintenanceTask().getRunTime() * 1000;
		ZonedDateTime nextRun = ZonedDateTime.ofInstant(Instant.ofEpochMilli(runTimeMillis), java.time.ZoneId.systemDefault());
		ZonedDateTime payLimit = nextRun.plusWeeks(4);
		ZonedDateTime payDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(payTime), java.time.ZoneId.systemDefault());

		if (payLimit.isBefore(payDateTime)) {
			return;
		}

		player.getInventory().decreaseKinah(toPay);
		house.setNextPay(new Timestamp(payTime));
		house.setFeePaid(true);
		house.save();
		PacketSendUtility.sendPacket(player, new SM_HOUSE_PAY_RENT(weekCount));
	}
}

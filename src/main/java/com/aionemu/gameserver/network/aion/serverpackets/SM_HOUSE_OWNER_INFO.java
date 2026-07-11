package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerHouseOwnerFlags;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.model.town.Town;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.TownService;

/**
 * 向客户端同步玩家房屋所有权与维护状态信息的服务端包。
 * Server packet that synchronizes the player's house ownership and maintenance status to the client.
 */
public class SM_HOUSE_OWNER_INFO extends AionServerPacket {

	private Player player;
	private House activeHouse;

	/**
	 * 构造房屋所有者信息包。
	 * Creates a house owner info packet.
	 *
	 * 玩家 / player
	 * @param activeHouse 当前活跃房屋，可为 null / active house, may be null
	 */
	public SM_HOUSE_OWNER_INFO(Player player, House activeHouse) {
		this.player = player;
		this.activeHouse = activeHouse;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		if (activeHouse == null) {
			writeD(0);
			writeD(player.isBuildingInState(PlayerHouseOwnerFlags.BUY_STUDIO_ALLOWED) ? 355000 : 0);
		} else {
			writeD(activeHouse.getAddress().getId());
			writeD(activeHouse.getBuilding().getId());
		}
		writeC(player.getBuildingOwnerStates());
		int townLevel = 1;
		if (activeHouse != null && activeHouse.getAddress().getTownId() != 0) {
			Town town = GameHousingServices.townService().getTownById(activeHouse.getAddress().getTownId());
			townLevel = town.getLevel();
		}
		writeC(townLevel);
		if (activeHouse == null || !activeHouse.isFeePaid() || activeHouse.getHouseType() == HouseType.STUDIO) {
			writeC(0);
		} else {
			Timestamp nextPay = activeHouse.getNextPay();
			float diff;
			if (nextPay == null) {
				diff = GameHousingServices.maintenanceTask().getPeriod();
			} else {
				long paytime = activeHouse.getNextPay().getTime();
				diff = paytime - ((long) GameHousingServices.maintenanceTask().getRunTime() * 1000);
			}
			if (diff < 0) {
				writeC(0);
			} else {
				int weeks = (int) (Math.round(diff / GameHousingServices.maintenanceTask().getPeriod()));

				// 检查今天是否为周日（第 7 天） / Check if today is Sunday (day 7)
				ZonedDateTime now = ZonedDateTime.now();
				if (now.getDayOfWeek() != DayOfWeek.SUNDAY) {
					weeks++;
				}
				writeC(weeks);
			}
		}
		writeD(0);
		writeD(0);
		writeD(0);
		writeC(0);
		writeC(0);
		writeC(0);
		writeC(0);
	}
}

package com.aionemu.gameserver.model.house;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameHousingServices;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_ACQUIRE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_OWNER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.mail.MailFormatter;
import com.aionemu.gameserver.taskmanager.AbstractCronTask;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * Maintenance 任务，用于房屋相关逻辑。
 * Maintenance Task for house logic.
 */
@Slf4j

public class MaintenanceTask extends AbstractCronTask {

	private static volatile ObjectProvider<MaintenanceTask> instanceProvider;
	private static final List<House> maintainedHouses;

	static {
		maintainedHouses = new ArrayList<House>();
	}

	/** 获取副本。 / Returns the instance. */
	public static final MaintenanceTask getInstance() {
		ObjectProvider<MaintenanceTask> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/** 设置实例提供者。 / Sets the instance provider. */
	public static void setInstanceProvider(ObjectProvider<MaintenanceTask> provider) {
		instanceProvider = provider;
	}

	private static MaintenanceTask createLegacyInstance() {
		try {
			return new MaintenanceTask(HousingConfig.HOUSE_MAINTENANCE_TIME);
		} catch (ParseException pe) {
			return null;
		}
	}

	private static class SingletonHolder {
		private static final MaintenanceTask instance = createLegacyInstance();
	}

	public MaintenanceTask(String maintainTime) throws ParseException {
		super(maintainTime);
	}

	@Override
	protected long getRunDelay() {
		int left = (int) (getRunTime() - System.currentTimeMillis() / 1000);
		if (left < 0) {
			return 0;
		}
		return left * 1000;
	}

	@Override
	protected String getServerTimeVariable() {
		return "houseMaintainTime";
	}

	protected boolean canRunOnInit() {
		return false;
	}

	/**
	 * @return 是否已到维护时间 / whether maintain time
	 */
	public boolean isMaintainTime() {
		return (getRunTime() - System.currentTimeMillis() / 1000) <= 0;
	}

	@Override
	protected void preInit() {
		log.info(I18n.get("log.33d70503ba48"));
	}

	@Override
	protected void preRun() {
		updateMaintainedHouses();
		log.info(I18n.get("log.45653a9b8124", maintainedHouses.size()));
	}

	private void updateMaintainedHouses() {
		maintainedHouses.clear();
		if (!HousingConfig.ENABLE_HOUSE_PAY) {
			return;
		}
		Date now = new Date();
		List<House> houses = GameHousingServices.housingService().getCustomHouses();
		for (House house : houses) {
			if (house.getStatus() == HouseStatus.INACTIVE) {
				continue;
			}
			if (house.getOwnerId() == 0) {
				continue;
			}
			if (house.isFeePaid()) {
				if (house.getNextPay() == null || house.getNextPay().before(now)) {
					house.setFeePaid(false);
					if (house.getNextPay() == null) {
						house.setNextPay(new Timestamp((long) getRunTime() * 1000));
					}
					house.save();
				} else {
					continue;
				}
			}
			maintainedHouses.add(house);
		}
	}

	@Override
	protected void executeTask() {
		if (!HousingConfig.ENABLE_HOUSE_PAY) {
			return;
		}
		
		ZonedDateTime now = ZonedDateTime.now();
		long periodMillis = getPeriod() * 1000L;
		
		ZonedDateTime previousRun = now.minusNanos(periodMillis * 1_000_000L);
		ZonedDateTime beforePreviousRun = previousRun.minusNanos(periodMillis * 1_000_000L);
		
		for (House house : maintainedHouses) {
			if (house.isFeePaid()) {
				continue;
			}
			long payTime = house.getNextPay().getTime();
			long impoundTime = 0;
			int warnCount = 0;
			PlayerCommonData pcd = null;
			Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(house.getOwnerId());
			if (player == null) {
				pcd = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(house.getOwnerId());
			} else {
				pcd = player.getCommonData();
			}
			if (pcd == null) {
				putHouseToAuction(house, null);
				continue;
			}
			
			long beforePreviousRunMillis = beforePreviousRun.toInstant().toEpochMilli();
			long previousRunMillis = previousRun.toInstant().toEpochMilli();
			long nowMillis = now.toInstant().toEpochMilli();
			
			if (payTime <= beforePreviousRunMillis) {
				ZonedDateTime plusDay = beforePreviousRun.minusDays(1);
				if (payTime <= plusDay.toInstant().toEpochMilli()) {
					impoundTime = nowMillis;
					warnCount = 3;
					putHouseToAuction(house, pcd);
				} else {
					impoundTime = now.plusDays(1).toInstant().toEpochMilli();
					warnCount = 2;
				}
			} else if (payTime <= previousRunMillis) {
				impoundTime = now.plusNanos(periodMillis * 1_000_000L).plusDays(1).toInstant().toEpochMilli();
				warnCount = 1;
			} else {
				continue;
			}
			
			if (pcd.isOnline()) {
				if (warnCount == 3) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_SEQUESTRATE);
				} else {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OVERDUE);
				}
			}
			MailFormatter.sendHouseMaintenanceMail(house, warnCount, impoundTime);
		}
	}

	private void putHouseToAuction(House house, PlayerCommonData playerCommonData) {
		house.revokeOwner();
		GameHousingServices.housingBidService().addHouseToAuction(house);
		house.save();
		if (playerCommonData == null) {
			return;
		}
		if (playerCommonData.isOnline()) {
			Player player = playerCommonData.getPlayer();
			player.getHouses().remove(house);
			player.setHouseRegistry(null);
			PacketSendUtility.sendPacket(player, new SM_HOUSE_ACQUIRE(player.getObjectId(), house.getAddress().getId(), false));
			PacketSendUtility.sendPacket(player, new SM_HOUSE_OWNER_INFO(player, null));
		}
	}
}

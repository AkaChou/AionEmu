package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.quartz.CronExpression;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.dao.HouseBidsDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.ServerVariablesDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerHouseOwnerFlags;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseBidEntry;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.house.PlayerHouseBid;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.model.templates.housing.HousingLand;
import com.aionemu.gameserver.model.templates.housing.Sale;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_ACQUIRE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_OWNER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RECEIVE_BIDS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.mail.AuctionResult;
import com.aionemu.gameserver.services.mail.MailFormatter;
import com.aionemu.gameserver.taskmanager.AbstractCronTask;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapType;

import java.util.LinkedHashMap;
import java.util.Map;
/**
 * 房屋拍卖服务：按 cron 结算拍卖、管理竞拍条目、出价与登录时竞拍结果通知。
 * Housing auction service: settles auctions on cron, manages bid entries, places bids, and notifies results on login.
 */
@Slf4j(topic = "HOUSE_AUCTION_LOG")

public class HousingBidService extends AbstractCronTask {

	private static volatile ObjectProvider<HousingBidService> instanceProvider;
	private static CronExpression registerDateExpr;
	private static final Map<Integer, HouseBidEntry> houseBids;
	private static final Map<Integer, HouseBidEntry> playerBids;
	private static final Map<Integer, HouseBidEntry> bidsByIndex;
	private static int timeProlonged = 0;
	private static boolean isDataLoaded = false;

	static {
		houseBids = new LinkedHashMap<>();
		playerBids = new LinkedHashMap<>();
		bidsByIndex = new LinkedHashMap<>();
	}

	/**
	 * 使用指定拍卖 cron 表达式初始化。
	 * Initializes with the given auction cron expression.
	 *
	 * @param auctionTime 拍卖 cron 表达式 / auction cron expression
	 */
	public HousingBidService(String auctionTime) throws ParseException {
		super(auctionTime);
	}

	/**
	 * 获取服务单例（优先 Spring 提供者）。
	 * Returns the service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
	public static final HousingBidService getInstance() {
		ObjectProvider<HousingBidService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<HousingBidService> provider) {
		instanceProvider = provider;
	}

	private static HousingBidService createLegacyInstance() {
		try {
			return new HousingBidService(HousingConfig.HOUSE_AUCTION_TIME);
		} catch (ParseException pe) {
			return null;
		}
	}

	private static class SingletonHolder {
		private static final HousingBidService instance = createLegacyInstance();
	}

	@Override
	protected long getRunDelay() {
		return (long) getSecondsTillAuction() * 1000;
	}

	@Override
	protected String getServerTimeVariable() {
		return "auctionTime";
	}

	@Override
	protected boolean canRunOnInit() {
		return true;
	}

	@Override
	protected void postInit() {
		try {
			registerDateExpr = new CronExpression(HousingConfig.HOUSE_REGISTER_END);
		} catch (ParseException e) {
		}
		ServerVariablesDAO dao = DAOManager.getDAO(ServerVariablesDAO.class);
		timeProlonged = dao.load("auctionProlonged");
	}

	/**
	 * 使用房屋配置中的拍卖时间初始化。
	 * Initializes using the auction time from housing config.
	 */
	public HousingBidService() throws ParseException {
		super(HousingConfig.HOUSE_AUCTION_TIME);
	}

	/**
	 * 启动服务：加载竞拍数据，并按配置自动填充拍卖房屋。
	 * Starts the service: loads bid data and auto-fills auction houses when configured.
	 */
	public void start() {
		log.info(I18n.get("log.1abae9a9bb91"));
		loadBidData();
		if (HousingConfig.FILL_HOUSE_BIDS_AUTO) {
			log.info(I18n.get("log.fed09a339fc7"));
			int added = fillBidData();
			log.info(I18n.get("log.71c3da095c6a", added));
		}
		log.info(I18n.get("log.fed01e43e8f9", getMinutesTillAuction()));
		isDataLoaded = true;
	}

	private int fillBidData() {
		int count = 0;
		List<House> houses = GameHousingServices.housingService().getCustomHouses();
		while (!houses.isEmpty()) {
			House house = houses.get(Rnd.get(houses.size()));
			houses.remove(house);
			if (house.getOwnerId() != 0 || houseBids.containsKey(house.getObjectId())) {
				continue;
			}
			if (!checkAutoFillingLimits(house.getPlayerRace(), house.getHouseType())) {
				continue;
			}
	/**
	 * 将房屋加入拍卖（可指定起拍价）。
	 * Adds a house to the auction (optional initial price).
	 *
	 * house
	 * initial price
	 * whether successful
	 */
			addHouseToAuction(house, house.getDefaultAuctionPrice());
			count++;
		}
		return count;
	}

	private boolean checkAutoFillingLimits(Race race, HouseType type) {
		int bidsCount = getBidsCountByType(race, type);
		switch (type) {
		case HOUSE:
			if (bidsCount >= HousingConfig.FILL_AUTO_HOUSES_COUNT) {
				return false;
			}
			break;
		case MANSION:
			if (bidsCount >= HousingConfig.FILL_AUTO_MANSION_COUNT) {
				return false;
			}
			break;
		case ESTATE:
			if (bidsCount >= HousingConfig.FILL_AUTO_ESTATE_COUNT) {
				return false;
			}
			break;
		case PALACE:
			if (bidsCount >= HousingConfig.FILL_AUTO_PALACE_COUNT) {
				return false;
			}
			break;
		default:
			break;
		}
		return true;
	}

	private int getBidsCountByType(Race race, HouseType type) {
		int count = 0;
		for (HouseBidEntry entry : houseBids.values()) {
			HousingLand land = DataManager.HOUSE_DATA.getLand(entry.getLandId());
			Race entryRace = DataManager.NPC_DATA.getNpcTemplate(land.getManagerNpcId()).getTribe() == TribeClass.GENERAL ? Race.ELYOS : Race.ASMODIANS;
			if (entryRace == race && entry.getHouseType() == type) {
				count++;
			}
		}
		return count;
	}

	private void loadBidData() {
		Set<PlayerHouseBid> playerBidData = DAOManager.getDAO(HouseBidsDAO.class).loadBids();
		List<PlayerHouseBid> sortedBids = new ArrayList<PlayerHouseBid>(playerBidData);
		Collections.sort(sortedBids);
		Map<Integer, House> housesById = new LinkedHashMap<>();
		for (House house : GameHousingServices.housingService().getCustomHouses()) {
			housesById.put(house.getObjectId(), house);
		}
		int entryIndex = 1;
		for (PlayerHouseBid playerBid : sortedBids) {
			House house = housesById.get(playerBid.getHouseId());
			if (house == null) {
				continue;
			}
			HouseBidEntry entry = houseBids.get(house.getObjectId());
			if (entry == null) {
				entry = new HouseBidEntry(house, entryIndex, playerBid.getBidOffer());
				houseBids.put(house.getObjectId(), entry);
				bidsByIndex.put(entryIndex++, entry);
			} else if (entry.getBidPrice() < playerBid.getBidOffer()) {
				entry.setBidPrice(playerBid.getBidOffer());
			}
			if (playerBid.getPlayerId() != 0) {
				HouseBidEntry playerEntry = (HouseBidEntry) entry.Clone();
				playerEntry.setBidPrice(playerBid.getBidOffer());
				playerBids.put(playerBid.getPlayerId(), playerEntry);
				entry.setLastBiddingPlayer(playerBid.getPlayerId());
				entry.setLastBidTime(playerBid.getTime().getTime());
				entry.incrementBidCount();
			}
		}
		for (House house : housesById.values()) {
			if (house.getOwnerId() == 0) {
				continue;
			}
			if (house.getStatus() == HouseStatus.SELL_WAIT && !houseBids.containsKey(house.getObjectId())) {
				house.setStatus(HouseStatus.ACTIVE);
				house.setSellStarted(null);
				house.save();
			}
		}
	}

	@Override
	protected void executeTask() {
		if (!HousingConfig.ENABLE_HOUSE_AUCTIONS) {
			return;
		}
		while (!isDataLoaded) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				return;
			}
		}
		Map<HouseBidEntry, Integer> winners = new HashMap<HouseBidEntry, Integer>();
		Map<HouseBidEntry, Integer> successSell = new HashMap<HouseBidEntry, Integer>();
		Map<HouseBidEntry, Integer> failedSell = new HashMap<HouseBidEntry, Integer>();
		for (Entry<Integer, HouseBidEntry> playerBid : playerBids.entrySet()) {
			int playerId = playerBid.getKey();
			HouseBidEntry houseBid = getBidByEntryIndex(playerBid.getValue().getEntryIndex());
			House house = GameHousingServices.housingService().getHouseByAddress(houseBid.getAddress());
			if (playerBid.getValue().getBidPrice() == houseBid.getBidPrice()) {
				if (house.getOwnerId() == 0) {
					winners.put(houseBid, playerId);
				} else {
					successSell.put(houseBid, playerId);
				}
			}
		}
		for (HouseBidEntry houseBid : houseBids.values()) {
			House house = GameHousingServices.housingService().getHouseByAddress(houseBid.getAddress());
			if (houseBid.getBidCount() > 0) {
				continue;
			}
			if (house.getOwnerId() != 0) {
				failedSell.put(houseBid, house.getOwnerId());
			}
		}
		if (LoggingConfig.LOG_HOUSE_AUCTION) {
			log.info(I18n.get("log.19cd043d5e48"));
		}
		for (Entry<HouseBidEntry, Integer> winData : winners.entrySet()) {
			House wonHouse = GameHousingServices.housingService().getHouseByAddress(winData.getKey().getAddress());
			if (getPlayerData(winData.getValue()) == null) {
				log.warn(I18n.get("log.441ce3c83b43", winData.getValue(), winData.getKey().getAddress()));
				continue;
			}
	/**
	 * 完成房屋拍卖成交，处理赢家与房屋所有权。
	 * Completes a house auction sale for the winner and transfers ownership.
	 *
	 * @param winner 赢家公共数据 / winner common data
	 * obtained house
	 * auction result
	 */
			completeHouseSell(getPlayerData(winData.getValue()), wonHouse);
		}
		long time = System.currentTimeMillis();
		if (LoggingConfig.LOG_HOUSE_AUCTION) {
			log.info(I18n.get("log.cfe3329d9edf"));
		}
		for (Entry<HouseBidEntry, Integer> sellData : successSell.entrySet()) {
			// 获取房屋对象 / Get house object
			House soldHouse = GameHousingServices.housingService().getHouseByAddress(sellData.getKey().getAddress());
			// 检查房屋对象是否为空 / Check if house object is null
			if (soldHouse == null) {
				// 记录警告日志，包含未找到的房屋地址 / Log warning with the address of the missing house
				log.warn(I18n.get("log.15485b0c4e93", sellData.getKey().getAddress()));
				// 跳过当前循环 / Skip current iteration
				continue;
			}
			PlayerCommonData buyerPcd = getPlayerData(sellData.getValue());
			PlayerCommonData sellerPcd = getPlayerData(soldHouse.getOwnerId());
			if (buyerPcd.getPlayerObjId() == soldHouse.getOwnerId()) {
				continue;
			}
			if (sellerPcd.isOnline()) {
				PacketSendUtility.sendPacket(sellerPcd.getPlayer(), SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_AUCTION_SUCCESS(sellData.getKey().getAddress()));
			}
			if (buyerPcd.isOnline()) {
				PacketSendUtility.sendPacket(buyerPcd.getPlayer(), SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_BID_WIN(sellData.getKey().getAddress()));
			}
			long returnKinah = sellData.getKey().getBidPrice() + sellData.getKey().getRefundKinah();
			if (soldHouse.isInGracePeriod()) {
				soldHouse.revokeOwner();
				House newHouse = GameHousingServices.housingService().activateBoughtHouse(sellerPcd.getPlayerObjId());
				if (sellerPcd.isOnline()) {
					PacketSendUtility.sendPacket(sellerPcd.getPlayer(), new SM_HOUSE_ACQUIRE(sellerPcd.getPlayerObjId(), newHouse.getAddress().getId(), true));
					PacketSendUtility.sendPacket(sellerPcd.getPlayer(), new SM_HOUSE_OWNER_INFO(sellerPcd.getPlayer(), newHouse));
					sellerPcd.getPlayer().setHouseRegistry(newHouse.getRegistry());
				}
				MailFormatter.sendHouseAuctionMail(newHouse, sellerPcd, AuctionResult.GRACE_SUCCESS, time, returnKinah);
			} else {
				MailFormatter.sendHouseAuctionMail(soldHouse, sellerPcd, AuctionResult.SUCCESS_SALE, time, returnKinah);
				soldHouse.revokeOwner();
			}
	/**
	 * 完成房屋拍卖成交，处理赢家与房屋所有权。
	 * Completes a house auction sale for the winner and transfers ownership.
	 *
	 * @param winner 赢家公共数据 / winner common data
	 * obtained house
	 * auction result
	 */
			completeHouseSell(buyerPcd, soldHouse);
		}
		for (Entry<HouseBidEntry, Integer> notSoldData : failedSell.entrySet()) {
			HouseBidEntry bidEntry = notSoldData.getKey();
			PlayerCommonData sellerPcd = getPlayerData(notSoldData.getValue());
			House bidHouse = GameHousingServices.housingService().getHouseByAddress(bidEntry.getAddress());
			if (sellerPcd.isOnline()) {
				PacketSendUtility.sendPacket(sellerPcd.getPlayer(), SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_AUCTION_FAIL(bidHouse.getAddress().getId()));
			}
			AuctionResult result = AuctionResult.FAILED_SALE;
			long compensation = 0;
			if (bidHouse.isInGracePeriod()) {
				long timePassed = (getAuctionStartTime() - bidHouse.getSellStarted().getTime()) / 1000;
				if (timePassed > 7 * 24 * 3600) {
					bidHouse.revokeOwner();
					House activatedHouse = GameHousingServices.housingService().activateBoughtHouse(sellerPcd.getPlayerObjId());
					if (sellerPcd.isOnline()) {
						if (activatedHouse != null) {
							PacketSendUtility.sendPacket(sellerPcd.getPlayer(), new SM_HOUSE_ACQUIRE(sellerPcd.getPlayerObjId(), activatedHouse.getAddress().getId(), true));
							sellerPcd.getPlayer().setHouseRegistry(activatedHouse.getRegistry());
						}
						PacketSendUtility.sendPacket(sellerPcd.getPlayer(), new SM_HOUSE_OWNER_INFO(sellerPcd.getPlayer(), activatedHouse));
					}
					result = AuctionResult.GRACE_FAIL;
					time = System.currentTimeMillis();
					compensation = bidHouse.getDefaultAuctionPrice();
				}
			} else {
				bidHouse.setStatus(HouseStatus.ACTIVE);
				time = bidHouse.getSellStarted().getTime();
			}
			bidHouse.save();
			MailFormatter.sendHouseAuctionMail(bidHouse, sellerPcd, result, time, compensation);
		}
		List<HouseBidEntry> copy = new ArrayList<HouseBidEntry>();
		copy.addAll(houseBids.values());
		houseBids.clear();
		playerBids.clear();
		bidsByIndex.clear();
		if (LoggingConfig.LOG_HOUSE_AUCTION) {
			log.info(I18n.get("log.f4db49f3f010"));
		}
		for (HouseBidEntry houseBid : copy) {
			House house = GameHousingServices.housingService().getHouseByAddress(houseBid.getAddress());
			DAOManager.getDAO(HouseBidsDAO.class).deleteHouseBids(house.getObjectId());
			if (house.getOwnerId() == 0) {
				house.setStatus(HouseStatus.NOSALE);
	/**
	 * 将房屋加入拍卖（可指定起拍价）。
	 * Adds a house to the auction (optional initial price).
	 *
	 * house
	 * initial price
	 * whether successful
	 */
				addHouseToAuction(house);
				house.save();
			}
		}
	}

	@Override
	protected void postRun() {
		ServerVariablesDAO dao = DAOManager.getDAO(ServerVariablesDAO.class);
		timeProlonged = 0;
		dao.store("auctionProlonged", timeProlonged);
	}

	/**
	 * 获取本轮拍卖开始时间戳（毫秒）。
	 * Returns this auction round start time in milliseconds.
	 *
	 * @return 开始时间毫秒 / start time millis
	 */
	public long getAuctionStartTime() {
		return (long) (getRunTime() - 7 * 24 * 3600) * 1000;
	}

	/**
	 * 距离拍卖结算剩余秒数。
	 * Seconds remaining until auction settlement.
	 *
	 * seconds left
	 */
	public int getSecondsTillAuction() {
		int left = (int) (getRunTime() - System.currentTimeMillis() / 1000);
		left += timeProlonged * 60;
		if (left < 0) {
			return 0;
		}
		return left;
	}

	/**
	 * 距离拍卖结算剩余分钟数。
	 * Minutes remaining until auction settlement.
	 *
	 * minutes left
	 */
	public int getMinutesTillAuction() {
		return (int) (getSecondsTillAuction() / 60);
	}

	/**
	 * 当前是否允许出价。
	 * Whether bidding is currently allowed.
	 *
	 * @return 是否允许出价 / whether bidding allowed
	 */
	public boolean isBiddingAllowed() {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime auctionEnd = ZonedDateTime.ofInstant(Instant.ofEpochMilli(((long) getRunTime() + timeProlonged * 60) * 1000), java.time.ZoneId.systemDefault());
		
		if (now.getDayOfWeek() == auctionEnd.getDayOfWeek() && auctionEnd.minusDays(1).isAfter(now)) {
			return false;
		}
		return true;
	}

	/**
	 * 当前是否允许登记房屋上拍。
	 * Whether registering houses for auction is currently allowed.
	 *
	 * @return 是否允许登记 / whether registering allowed
	 */
	public boolean isRegisteringAllowed() {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime registerEnd = ZonedDateTime.ofInstant(registerDateExpr.getTimeAfter(java.util.Date.from(now.toInstant())).toInstant(), java.time.ZoneId.systemDefault());
		ZonedDateTime auctionEnd = ZonedDateTime.ofInstant(Instant.ofEpochMilli(((long) getRunTime() + timeProlonged * 60) * 1000), java.time.ZoneId.systemDefault());
		
		if (now.getDayOfWeek() == registerEnd.getDayOfWeek() && now.getHour() >= registerEnd.getHour() || (now.getDayOfWeek() == auctionEnd.getDayOfWeek() && now.getHour() <= auctionEnd.getHour())) {
			return false;
		}
		return true;
	}

	private PlayerCommonData getPlayerData(int objectId) {
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(objectId);
		if (player == null) {
			return DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(objectId);
		}
		return player.getCommonData();
	}

	/**
	 * 完成房屋拍卖成交，处理赢家与房屋所有权。
	 * Completes a house auction sale for the winner and transfers ownership.
	 *
	 * @param winner 赢家公共数据 / winner common data
	 * obtained house
	 * auction result
	 */
	public AuctionResult completeHouseSell(PlayerCommonData winner, House obtainedHouse) {
		House winnerHouse = GameHousingServices.housingService().getPlayerStudio(winner.getPlayerObjId());
		AuctionResult result = AuctionResult.WIN_BID;
		long time = System.currentTimeMillis();
		if (winnerHouse != null) {
			winnerHouse.revokeOwner();
		} else {
			int address = GameHousingServices.housingService().getPlayerAddress(winner.getPlayerObjId());
			if (address > 0) {
				winnerHouse = GameHousingServices.housingService().getHouseByAddress(address);
				winnerHouse.setSellStarted(new Timestamp(getAuctionStartTime()));
				obtainedHouse.setStatus(HouseStatus.INACTIVE);
				result = AuctionResult.GRACE_START;
				ZonedDateTime runTimeDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(getRunTime() * 1000L), java.time.ZoneId.systemDefault());
				time = runTimeDate.plusWeeks(2).toInstant().toEpochMilli();
			}
		}
		obtainedHouse.setOwnerId(winner.getPlayerObjId());
		if (result == AuctionResult.WIN_BID) {
			obtainedHouse.setAcquiredTime(new Timestamp(System.currentTimeMillis()));
			obtainedHouse.setStatus(HouseStatus.ACTIVE);
			obtainedHouse.setFeePaid(true);
			obtainedHouse.setNextPay(null);
			obtainedHouse.setSellStarted(null);
			obtainedHouse.reloadHouseRegistry();
			obtainedHouse.save();
		}
		if (winner.isOnline()) {
			if (result == AuctionResult.WIN_BID) {
				winner.getPlayer().setHouseRegistry(obtainedHouse.getRegistry());
				winner.getPlayer().setBuildingOwnerState(PlayerHouseOwnerFlags.HOUSE_OWNER.getId());
				PacketSendUtility.sendPacket(winner.getPlayer(), new SM_HOUSE_ACQUIRE(winner.getPlayerObjId(), obtainedHouse.getAddress().getId(), true));
				PacketSendUtility.sendPacket(winner.getPlayer(), new SM_HOUSE_OWNER_INFO(winner.getPlayer(), obtainedHouse));
			}
			PacketSendUtility.sendPacket(winner.getPlayer(), SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_BID_WIN(obtainedHouse.getAddress().getId()));
		}
		MailFormatter.sendHouseAuctionMail(obtainedHouse, winner, result, time, 0);
		obtainedHouse.getController().kickVisitors(null, true, true);
		return result;
	}

	/**
	 * 将房屋以默认起拍价加入拍卖。
	 * Adds a house to the auction at its default starting price.
	 *
	 * house
	 * whether successful
	 */
	public boolean addHouseToAuction(House house) {
	/**
	 * 将房屋加入拍卖（可指定起拍价）。
	 * Adds a house to the auction (optional initial price).
	 *
	 * house
	 * initial price
	 * whether successful
	 */
		return addHouseToAuction(house, house.getDefaultAuctionPrice());
	}

	/**
	 * 将房屋加入拍卖（可指定起拍价）。
	 * Adds a house to the auction (optional initial price).
	 *
	 * house
	 * initial price
	 * whether successful
	 */
	public boolean addHouseToAuction(House house, long initialPrice) {
		if (house.getStatus() == HouseStatus.SELL_WAIT) {
			return false;
		}
		house.setStatus(HouseStatus.SELL_WAIT);
		int maxIndex = 1;
		HouseBidEntry bidEntry = null;
		synchronized (bidsByIndex) {
			for (Integer index : bidsByIndex.keySet()) {
				if (index > maxIndex) {
					maxIndex = index;
				}
			}
			bidEntry = new HouseBidEntry(house, ++maxIndex, initialPrice);
			bidsByIndex.put(maxIndex, bidEntry);
		}
		synchronized (houseBids) {
			houseBids.put(house.getObjectId(), bidEntry);
		}
		Timestamp time = new Timestamp(System.currentTimeMillis());
		if (house.getSellStarted() == null) {
			house.setSellStarted(time);
		}
		house.save();
		return DAOManager.getDAO(HouseBidsDAO.class).addBid(0, house.getObjectId(), initialPrice, time);
	}

	/**
	 * 将房屋从拍卖中移除。
	 * Removes a house from the auction.
	 *
	 * house
	 * @param noSale 是否流拍处理 / no-sale handling
	 * whether successful
	 */
	public boolean removeHouseFromAuction(House house, boolean noSale) {
		if (house.getStatus() != HouseStatus.SELL_WAIT) {
			return false;
		}

		HouseBidEntry bidEntry = null;
		HouseBidEntry playerBid = null;
		Integer lastPlayer = null;

		synchronized (houseBids) {
			bidEntry = houseBids.remove(house.getObjectId());
			if (bidEntry == null) {
				return false;
			}
			lastPlayer = bidEntry.getLastBiddingPlayer();
			playerBid = playerBids.remove(lastPlayer);
		}

		synchronized (bidsByIndex) {
			bidsByIndex.remove(bidEntry.getEntryIndex());
		}

		PlayerCommonData pcd = null;
		if (house.getOwnerId() != 0) {
			if (house.isInGracePeriod()) {
				house.setSellStarted(null);
			}
			pcd = getPlayerData(house.getOwnerId());
			MailFormatter.sendHouseAuctionMail(house, pcd, AuctionResult.CANCELED_BID, System.currentTimeMillis(), bidEntry.getBidPrice() + bidEntry.getRefundKinah());
			house.setStatus(HouseStatus.ACTIVE);
		} else {
			house.setStatus(noSale ? HouseStatus.NOSALE : HouseStatus.ACTIVE);
		}
		if (lastPlayer != null) {
			pcd = getPlayerData(lastPlayer);
			MailFormatter.sendHouseAuctionMail(house, pcd, AuctionResult.CANCELED_BID, System.currentTimeMillis(), playerBid.getBidPrice());
		}
		DAOManager.getDAO(HouseBidsDAO.class).deleteHouseBids(house.getObjectId());
		house.save();
		return true;
	}

	/**
	 * 玩家对指定拍卖条目出价。
	 * Places a player bid on the given auction entry.
	 *
	 * bidder
	 * @param entryIndex 拍卖条目索引 / bid entry index
	 * bid amount
	 */
	public synchronized void placeBid(Player player, int entryIndex, long bidOffer) {
		if ((player.getBuildingOwnerStates() & PlayerHouseOwnerFlags.BIDDING_ALLOWED.getId()) == 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_OWN_NOT_COMPLETE_QUEST(player.getRace() == Race.ELYOS ? 18802 : 28802));
			return;
		}
		int minutesLeft = getMinutesTillAuction();
		if (minutesLeft == 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_BID_TIMEOUT);
			return;
		}
		HouseBidEntry entry = getBidByEntryIndex(entryIndex);
		if (entry == null) {
			return;
		}
		if (player.getInventory().getKinah() < bidOffer) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_NOT_ENOUGH_MONEY);
			return;
		}
		House bidHouse = GameHousingServices.housingService().getHouseByAddress(entry.getAddress());
		if (player.getObjectId() == bidHouse.getOwnerId()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_BID_MY_HOUSE);
			return;
		}
		int playerAddress = GameHousingServices.housingService().getPlayerAddress(player.getObjectId());
		House playerHouse = GameHousingServices.housingService().getHouseByAddress(playerAddress);
		if (playerHouse != null && playerHouse.isInGracePeriod()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_BID_GRACE_HOUSE);
			return;
		}
		int minLevel = getMinBidLevel(player, entry.getMapId(), entry.getLandId());
		if (minLevel > player.getLevel()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_BID_LOW_LEVEL(minLevel));
			return;
		}
		if (playerHouse != null && !playerHouse.isFeePaid() && HousingConfig.ENABLE_HOUSE_PAY) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_BID_OVERDUE);
			return;
		}
		if (bidOffer - entry.getBidPrice() >= entry.getBidPrice() * HousingConfig.HOUSE_AUCTION_BID_LIMIT / 100f) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_BID_EXCESS_ACCOUNT);
			return;
		}
		HouseBidEntry currentBid = playerBids.get(player.getObjectId());
		if (currentBid != null) {
			if (entry.getLastBiddingPlayer() == player.getObjectId()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_BID_SUCC_BID_HOUSE);
				return;
			}
			HouseBidEntry houseBid = getBidByEntryIndex(currentBid.getEntryIndex());
			if (houseBid.getBidPrice() == currentBid.getBidPrice()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_BID_OTHER_HOUSE);
				return;
			}
		}
		if (minutesLeft < 5 && timeProlonged < 30) {
			timeProlonged += 5;
			GameThreadPoolServices.threadPoolManager().execute(new Runnable() {
				@Override
				public void run() {
					DAOManager.getDAO(ServerVariablesDAO.class).store("auctionProlonged", timeProlonged);
				}
			});
		} else if (!isBiddingAllowed()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_BID_TIMEOUT);
			return;
		}
		if (bidOffer > entry.getBidPrice() || entry.getBidCount() == 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_BID_SUCCESS(entry.getAddress()));
			Timestamp time = new Timestamp(System.currentTimeMillis());
			player.getInventory().decreaseKinah(bidOffer);
			int previousPlayer = entry.getLastBiddingPlayer();
			if (previousPlayer > 0) {
				PlayerCommonData prevPcd = getPlayerData(previousPlayer);
				if (prevPcd.isOnline()) {
					PacketSendUtility.sendPacket(prevPcd.getPlayer(), SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_BID_CANCEL);
				}
				MailFormatter.sendHouseAuctionMail(bidHouse, prevPcd, AuctionResult.FAILED_BID, time.getTime(), entry.getBidPrice());
			}
			entry.incrementBidCount();
			entry.setLastBiddingPlayer(player.getObjectId());
			entry.setLastBidTime(time.getTime());
			entry.setBidPrice(bidOffer);
			HouseBidEntry playerBid = (HouseBidEntry) entry.Clone();
			playerBids.put(player.getObjectId(), playerBid);
			DAOManager.getDAO(HouseBidsDAO.class).addBid(player.getObjectId(), bidHouse.getObjectId(), bidOffer, time);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_PRICE_CHANGE(bidOffer));
			PacketSendUtility.sendPacket(player, new SM_RECEIVE_BIDS(0));
		}
	}

	/**
	 * 玩家登录时处理竞拍相关邮件/通知。
	 * Handles auction-related mail/notifications when a player logs in.
	 *
	 * logging-in player
	 */
	public void onPlayerLogin(Player player) {
		if (player.getMailbox() == null) {
			return;
		}
		List<Letter> letters = player.getMailbox().getNewSystemLetters("$$HS_AUCTION_MAIL");
		boolean needsRefresh = false;
		for (Letter letter : letters) {
			String[] titleParts = letter.getTitle().split(",");
			String[] bodyParts = letter.getMessage().split(",");
			AuctionResult result = AuctionResult.getResultFromId(Integer.parseInt(titleParts[0]));
			if (result == AuctionResult.FAILED_BID) {
				needsRefresh = true;
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_BID_CANCEL);
			} else if (result == AuctionResult.WIN_BID || result == AuctionResult.GRACE_START) {
				needsRefresh = true;
				int address = Integer.parseInt(bodyParts[1]);
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_BID_WIN(address));
			} else if (result == AuctionResult.FAILED_SALE) {
				needsRefresh = true;
				int address = Integer.parseInt(bodyParts[1]);
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_AUCTION_FAIL(address));
			} else if (result == AuctionResult.SUCCESS_SALE || result == AuctionResult.GRACE_SUCCESS) {
				needsRefresh = true;
				int address = Integer.parseInt(bodyParts[1]);
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_AUCTION_SUCCESS(address));
			}
		}
		if (needsRefresh) {
			PacketSendUtility.sendPacket(player, new SM_RECEIVE_BIDS(0));
		}
		letters = player.getMailbox().getNewSystemLetters("$$HS_OVERDUE_");
		for (Letter letter : letters) {
			if (letter.getSenderName().endsWith("FINAL") || letter.getSenderName().endsWith("3RD")) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_SEQUESTRATE);
			} else {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_OVERDUE);
			}
		}
	}

	/**
	 * 按房屋对象 ID 获取拍卖条目。
	 * Gets a house bid entry by house object id.
	 *
	 * house object id
	 * bid entry
	 */
	public HouseBidEntry getHouseBid(int houseObjectId) {
		synchronized (houseBids) {
			return houseBids.get(houseObjectId);
		}
	}

	/**
	 * 获取指定种族可见的拍卖条目列表。
	 * Lists auction entries visible to the given race.
	 *
	 * player race
	 *
	 * @param playerRace @return 拍卖条目列表 / bid entry list
	 */
	public List<HouseBidEntry> getHouseBidEntries(Race playerRace) {
		synchronized (houseBids) {
			List<HouseBidEntry> bids = new ArrayList<HouseBidEntry>();
			for (HouseBidEntry bid : houseBids.values()) {
				HousingLand land = DataManager.HOUSE_DATA.getLand(bid.getLandId());
				boolean isEly = DataManager.NPC_DATA.getNpcTemplate(land.getManagerNpcId()).getTribe() == TribeClass.GENERAL;
				if (isEly && playerRace == Race.ELYOS) {
					bids.add(bid);
				} else if (!isEly && playerRace == Race.ASMODIANS) {
					bids.add(bid);
				}
			}
			return bids;
		}
	}

	/**
	 * 获取玩家最近一次出价条目。
	 * Returns the player's last bid entry.
	 *
	 * player id
	 * bid entry
	 */
	public HouseBidEntry getLastPlayerBid(int playerId) {
		return playerBids.get(playerId);
	}

	/**
	 * 按条目索引获取拍卖条目。
	 * Gets a bid entry by its index.
	 *
	 * @param index 条目索引 / entry index
	 * bid entry
	 */
	public HouseBidEntry getBidByEntryIndex(int index) {
		synchronized (bidsByIndex) {
			return bidsByIndex.get(index);
		}
	}

	private static int getMinBidLevel(Player player, int mapId, int landId) {
		HousingLand land = DataManager.HOUSE_DATA.getLand(landId);
		Sale saleOptions = land.getSaleOptions();
		HouseType houseType = HouseType.fromValue(land.getBuildings().get(0).getSize());
		if (player.getRace() == Race.ELYOS) {
			if (mapId == WorldMapType.HEIRON.getId()) {
				return saleOptions.getMinLevel();
			}
		} else if (mapId == WorldMapType.BELUSLAN.getId()) {
			return saleOptions.getMinLevel();
		}
		switch (houseType) {
		case HOUSE:
			if (HousingConfig.HOUSE_MIN_BID_LEVEL > 0) {
				return HousingConfig.HOUSE_MIN_BID_LEVEL;
			}
			break;
		case MANSION:
			if (HousingConfig.MANSION_MIN_BID_LEVEL > 0) {
				return HousingConfig.MANSION_MIN_BID_LEVEL;
			}
			break;
		case ESTATE:
			if (HousingConfig.ESTATE_MIN_BID_LEVEL > 0) {
				return HousingConfig.ESTATE_MIN_BID_LEVEL;
			}
			break;
		case PALACE:
			if (HousingConfig.PALACE_MIN_BID_LEVEL > 0) {
				return HousingConfig.PALACE_MIN_BID_LEVEL;
			}
			break;
		default:
			break;
		}
		return saleOptions.getMinLevel();
	}

	/**
	 * 判断玩家等级是否达到该地产最低竞拍要求。
	 * Returns whether the player meets the minimum level to bid on the land.
	 *
	 * 玩家 / player
	 * map id
	 * land id
	 *
	 * @return 是否可竞拍 / whether can bid
	 */
	public static boolean canBidHouse(Player player, int mapId, int landId) {
		return player.getLevel() >= getMinBidLevel(player, mapId, landId);
	}
}

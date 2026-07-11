package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 房屋系统相关配置。
 * Housing system related configuration.
 */
public class HousingConfig {
	/**
	 * 房屋可见距离。
	 * Housing visibility distance.
	 */
	@Property(key = "gameserver.housing.visibility.distance", defaultValue = "200")
	public static float VISIBILITY_DISTANCE = 200f;
	/**
	 * 是否显示房屋门 ID。
	 * Whether house door IDs are shown.
	 */
	@Property(key = "gameserver.housedoor.showid", defaultValue = "true")
	public static boolean ENABLE_SHOW_HOUSE_DOORID;
	/**
	 * 进入房屋所需最低权限等级。
	 * Minimum access level required to enter a house.
	 */
	@Property(key = "gameserver.housedoor.accesslevel", defaultValue = "3")
	public static int ENTER_HOUSE_ACCESSLEVEL;
	/**
	 * 是否启用房屋拍卖。
	 * Whether house auctions are enabled.
	 */
	@Property(key = "gameserver.housing.auction.enable", defaultValue = "false")
	public static boolean ENABLE_HOUSE_AUCTIONS;
	/**
	 * 是否启用房屋维护费。
	 * Whether house maintenance payment is enabled.
	 */
	@Property(key = "gameserver.housing.pay.enable", defaultValue = "false")
	public static boolean ENABLE_HOUSE_PAY;
	/**
	 * 房屋拍卖 Cron 时间。
	 * House auction Cron schedule.
	 */
	@Property(key = "gameserver.housing.auction.time", defaultValue = "0 5 12 ? * SUN")
	public static String HOUSE_AUCTION_TIME;
	/**
	 * 房屋拍卖报名截止 Cron。
	 * House auction registration end Cron schedule.
	 */
	@Property(key = "gameserver.housing.auction.registerend", defaultValue = "0 0 0 ? * SAT")
	public static String HOUSE_REGISTER_END;
	/**
	 * 房屋维护 Cron 时间。
	 * House maintenance Cron schedule.
	 */
	@Property(key = "gameserver.housing.maintain.time", defaultValue = "0 0 0 ? * MON")
	public static String HOUSE_MAINTENANCE_TIME;
	/**
	 * 普通房屋默认最低出价。
	 * Default minimum bid for house.
	 */
	@Property(key = "gameserver.housing.auction.default_bid.house", defaultValue = "12000000")
	public static int HOUSE_MIN_BID;
	/**
	 * 宅邸默认最低出价。
	 * Default minimum bid for mansion.
	 */
	@Property(key = "gameserver.housing.auction.default_bid.mansion", defaultValue = "112000000")
	public static int MANSION_MIN_BID;
	/**
	 * 庄园默认最低出价。
	 * Default minimum bid for estate.
	 */
	@Property(key = "gameserver.housing.auction.default_bid.estate", defaultValue = "335000000")
	public static int ESTATE_MIN_BID;
	/**
	 * 宫殿默认最低出价。
	 * Default minimum bid for palace.
	 */
	@Property(key = "gameserver.housing.auction.default_bid.palace", defaultValue = "1000000000")
	public static int PALACE_MIN_BID;
	/**
	 * 普通房屋竞拍最低等级。
	 * Minimum level to bid on a house.
	 */
	@Property(key = "gameserver.housing.auction.bidding.min_level.house", defaultValue = "21")
	public static int HOUSE_MIN_BID_LEVEL;
	/**
	 * 宅邸竞拍最低等级。
	 * Minimum level to bid on a mansion.
	 */
	@Property(key = "gameserver.housing.auction.bidding.min_level.mansion", defaultValue = "30")
	public static int MANSION_MIN_BID_LEVEL;
	/**
	 * 庄园竞拍最低等级。
	 * Minimum level to bid on an estate.
	 */
	@Property(key = "gameserver.housing.auction.bidding.min_level.estate", defaultValue = "40")
	public static int ESTATE_MIN_BID_LEVEL;
	/**
	 * 宫殿竞拍最低等级。
	 * Minimum level to bid on a palace.
	 */
	@Property(key = "gameserver.housing.auction.bidding.min_level.palace", defaultValue = "50")
	public static int PALACE_MIN_BID_LEVEL;
	/**
	 * 竞拍失败退款比例。
	 * Bid refund percent for losing bids.
	 */
	@Property(key = "gameserver.housing.auction.default_refund", defaultValue = "0.3f")
	public static float BID_REFUND_PERCENT;
	/**
	 * 房屋拍卖加价步数上限。
	 * House auction bid step limit.
	 */
	@Property(key = "gameserver.housing.auction.steplimit", defaultValue = "100")
	public static float HOUSE_AUCTION_BID_LIMIT;
	/**
	 * 是否启用房屋脚本调试。
	 * Whether house script debug is enabled.
	 */
	@Property(key = "gameserver.housing.scripts.debug", defaultValue = "false")
	public static boolean HOUSE_SCRIPT_DEBUG;
	/**
	 * 是否自动填充拍卖空缺。
	 * Whether empty house auction slots are auto-filled.
	 */
	@Property(key = "gameserver.housing.auction.fill.auto", defaultValue = "false")
	public static boolean FILL_HOUSE_BIDS_AUTO;
	/**
	 * 自动填充普通房屋数量。
	 * Auto-fill count for house auctions.
	 */
	@Property(key = "gameserver.housing.auction.fill.auto.houses", defaultValue = "20")
	public static int FILL_AUTO_HOUSES_COUNT;
	/**
	 * 自动填充宅邸数量。
	 * Auto-fill count for mansion auctions.
	 */
	@Property(key = "gameserver.housing.auction.fill.auto.mansion", defaultValue = "10")
	public static int FILL_AUTO_MANSION_COUNT;
	/**
	 * 自动填充庄园数量。
	 * Auto-fill count for estate auctions.
	 */
	@Property(key = "gameserver.housing.auction.fill.auto.estate", defaultValue = "5")
	public static int FILL_AUTO_ESTATE_COUNT;
	/**
	 * 自动填充宫殿数量。
	 * Auto-fill count for palace auctions.
	 */
	@Property(key = "gameserver.housing.auction.fill.auto.palace", defaultValue = "1")
	public static int FILL_AUTO_PALACE_COUNT;
}

package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 房屋系统相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Housing system related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
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

	@Getter
	private final HousingProperties housing = new HousingProperties();
	@Getter
	private final HousedoorProperties housedoor = new HousedoorProperties();

	@Getter
	public static class HousedoorProperties {
		public boolean isShowid() {
			return ENABLE_SHOW_HOUSE_DOORID;
		}

		public void setShowid(boolean showid) {
			ENABLE_SHOW_HOUSE_DOORID = showid;
		}

		public int getAccesslevel() {
			return ENTER_HOUSE_ACCESSLEVEL;
		}

		public void setAccesslevel(int accesslevel) {
			ENTER_HOUSE_ACCESSLEVEL = accesslevel;
		}
	}

	@Getter
	public static class HousingProperties {
		private final VisibilityProperties visibility = new VisibilityProperties();
		private final AuctionProperties auction = new AuctionProperties();
		private final PayProperties pay = new PayProperties();
		private final MaintainProperties maintain = new MaintainProperties();
		private final ScriptsProperties scripts = new ScriptsProperties();

		public static class VisibilityProperties {
			public float getDistance() {
				return VISIBILITY_DISTANCE;
			}

			public void setDistance(float distance) {
				VISIBILITY_DISTANCE = distance;
			}
		}

		public static class PayProperties {
			public boolean isEnable() {
				return ENABLE_HOUSE_PAY;
			}

			public void setEnable(boolean enable) {
				ENABLE_HOUSE_PAY = enable;
			}
		}

		public static class MaintainProperties {
			public String getTime() {
				return HOUSE_MAINTENANCE_TIME;
			}

			public void setTime(String time) {
				HOUSE_MAINTENANCE_TIME = time;
			}
		}

		public static class ScriptsProperties {
			public boolean isDebug() {
				return HOUSE_SCRIPT_DEBUG;
			}

			public void setDebug(boolean debug) {
				HOUSE_SCRIPT_DEBUG = debug;
			}
		}

		@Getter
		public static class AuctionProperties {
			private final DefaultBidProperties defaultBid = new DefaultBidProperties();
			private final BiddingProperties bidding = new BiddingProperties();
			private final FillProperties fill = new FillProperties();

			public boolean isEnable() {
				return ENABLE_HOUSE_AUCTIONS;
			}

			public void setEnable(boolean enable) {
				ENABLE_HOUSE_AUCTIONS = enable;
			}

			public String getTime() {
				return HOUSE_AUCTION_TIME;
			}

			public void setTime(String time) {
				HOUSE_AUCTION_TIME = time;
			}

			public String getRegisterend() {
				return HOUSE_REGISTER_END;
			}

			public void setRegisterend(String registerend) {
				HOUSE_REGISTER_END = registerend;
			}

			public float getDefaultRefund() {
				return BID_REFUND_PERCENT;
			}

			public void setDefaultRefund(float defaultRefund) {
				BID_REFUND_PERCENT = defaultRefund;
			}

			public float getSteplimit() {
				return HOUSE_AUCTION_BID_LIMIT;
			}

			public void setSteplimit(float steplimit) {
				HOUSE_AUCTION_BID_LIMIT = steplimit;
			}

			public static class DefaultBidProperties {
				public int getHouse() {
					return HOUSE_MIN_BID;
				}

				public void setHouse(int house) {
					HOUSE_MIN_BID = house;
				}

				public int getMansion() {
					return MANSION_MIN_BID;
				}

				public void setMansion(int mansion) {
					MANSION_MIN_BID = mansion;
				}

				public int getEstate() {
					return ESTATE_MIN_BID;
				}

				public void setEstate(int estate) {
					ESTATE_MIN_BID = estate;
				}

				public int getPalace() {
					return PALACE_MIN_BID;
				}

				public void setPalace(int palace) {
					PALACE_MIN_BID = palace;
				}
			}

			@Getter
			public static class BiddingProperties {
				private final MinLevelProperties minLevel = new MinLevelProperties();

				public static class MinLevelProperties {
					public int getHouse() {
						return HOUSE_MIN_BID_LEVEL;
					}

					public void setHouse(int house) {
						HOUSE_MIN_BID_LEVEL = house;
					}

					public int getMansion() {
						return MANSION_MIN_BID_LEVEL;
					}

					public void setMansion(int mansion) {
						MANSION_MIN_BID_LEVEL = mansion;
					}

					public int getEstate() {
						return ESTATE_MIN_BID_LEVEL;
					}

					public void setEstate(int estate) {
						ESTATE_MIN_BID_LEVEL = estate;
					}

					public int getPalace() {
						return PALACE_MIN_BID_LEVEL;
					}

					public void setPalace(int palace) {
						PALACE_MIN_BID_LEVEL = palace;
					}
				}
			}

			@Getter
			public static class FillProperties {
				private final AutoProperties auto = new AutoProperties();

				public void setAuto(AutoProperties auto) {
				}

				@Getter
				public static class AutoProperties {
					public AutoProperties() {
					}

					/**
					 * 兼容旧版裸标量 gameserver.housing.auction.fill.auto=true 的绑定入口。
					 * Binding entry for the legacy bare scalar gameserver.housing.auction.fill.auto=true.
					 */
					public static AutoProperties valueOf(String value) {
						try {
							FILL_HOUSE_BIDS_AUTO = Boolean.parseBoolean(value);
						} catch (RuntimeException ignored) {
						}
						return new AutoProperties();
					}

					public boolean isValue() {
						return FILL_HOUSE_BIDS_AUTO;
					}

					public void setValue(boolean value) {
						FILL_HOUSE_BIDS_AUTO = value;
					}

					public int getHouses() {
						return FILL_AUTO_HOUSES_COUNT;
					}

					public void setHouses(int houses) {
						FILL_AUTO_HOUSES_COUNT = houses;
					}

					public int getMansion() {
						return FILL_AUTO_MANSION_COUNT;
					}

					public void setMansion(int mansion) {
						FILL_AUTO_MANSION_COUNT = mansion;
					}

					public int getEstate() {
						return FILL_AUTO_ESTATE_COUNT;
					}

					public void setEstate(int estate) {
						FILL_AUTO_ESTATE_COUNT = estate;
					}

					public int getPalace() {
						return FILL_AUTO_PALACE_COUNT;
					}

					public void setPalace(int palace) {
						FILL_AUTO_PALACE_COUNT = palace;
					}
				}
			}
		}
	}
}

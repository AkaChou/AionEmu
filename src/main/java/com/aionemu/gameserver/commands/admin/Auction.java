package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseBidEntry;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员房屋拍卖命令：按区域/类型上架、随机上架或下架房屋。
 * Admin housing-auction command: lists, randomly lists, or removes houses from auction.
 * @author Rolandas
 */
public class Auction extends AdminCommand {

	/**
	 * 注册 {@code //auction} 命令。
	 * Registers the {@code //auction} command.
	 */
	public Auction() {
		super("auction");
	}

	/**
	 * 执行拍卖管理：add/remove/addrandom 子命令。
	 * Executes auction management: add/remove/addrandom subcommands.
	 * @param params 参数：子命令与区域/类型/价格等 / subcommand and zone/type/price args
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 1) {
			onFail(admin, null);
			return;
		}

		switch (params[0]) {
			case "remove": {
				if (params.length < 2) {
					onFail(admin, null);
					return;
				}
				String param = params[1].toUpperCase();
				List<House> housesToRemove = new ArrayList<>();

				if ("HOUSE".equals(param.split("_")[0])) {
					House house = GameHousingServices.housingService().getHouseByName(params[1].toUpperCase());
					if (house == null || house.getStatus() != HouseStatus.SELL_WAIT) {
						PacketSendUtility.sendMessage(admin, "No such house!");
					}
					housesToRemove.add(house);
				} else {
					ZoneName zoneName = ZoneName.get(params[1]);
					if (zoneName.name().equals(ZoneName.NONE)) {
						PacketSendUtility.sendMessage(admin, "No such zone!");
						return;
					}
					for (House house : GameHousingServices.housingService().getCustomHouses()) {
						if (house.getStatus() != HouseStatus.SELL_WAIT) {
							continue;
						}
						float x = house.getX();
						float y = house.getY();
						float z = house.getZ();
						if (house.getPosition().getMapRegion().isInsideZone(zoneName, x, y, z)) {
							housesToRemove.add(house);
						}
					}
				}

				if (housesToRemove.isEmpty()) {
					PacketSendUtility.sendMessage(admin, "Nothing to remove!");
					return;
				}

				boolean noSale = false;
				if (params.length == 3) {
					if (!"nosale".equals(params[2])) {
						onFail(admin, null);
						return;
					}
					noSale = true;
				}

				for (House house : housesToRemove) {
					if (GameHousingServices.housingBidService().removeHouseFromAuction(house, noSale)) {
						PacketSendUtility.sendMessage(admin, "Succesfully removed house " + house.getName());
					} else {
						PacketSendUtility.sendMessage(admin, "Failed to remove house " + house.getName());
					}
				}
				break;
			}
			case "add": {
				if (params.length < 3 || params.length > 4) {
					onFail(admin, null);
					return;
				}

				ZoneName zoneName = ZoneName.get(params[1]);
				if (zoneName.name().equals(ZoneName.NONE)) {
					PacketSendUtility.sendMessage(admin, "No such zone!");
					return;
				}

				HouseType houseType;
				try {
					houseType = HouseType.fromValue(params[2].toUpperCase());
				} catch (Exception e) {
					PacketSendUtility.sendMessage(admin, "No such house type!");
					return;
				}

				long bidPrice = 0;
				if (params.length == 4) {
					try {
						bidPrice = Long.parseLong(params[3]);
						if (bidPrice <= 0) {
							throw new IllegalArgumentException();
						}
					} catch (Exception e) {
						PacketSendUtility.sendMessage(admin, "Only positive numbers for the bid price!");
						return;
					}
				}

				boolean found = false;
				int counter = 0;

				for (House house : GameHousingServices.housingService().getCustomHouses()) {
					if (house.getOwnerId() != 0 || house.getHouseType() != houseType) {
						continue;
					}
					if (!isAvailableForAuction(house)) {
						continue;
					}
					float x = house.getX();
					float y = house.getY();
					float z = house.getZ();
					if (house.getPosition().getMapRegion().isInsideZone(zoneName, x, y, z)) {
						found = true;
						long price = bidPrice > 0 ? bidPrice : house.getDefaultAuctionPrice();
						if (GameHousingServices.housingBidService().addHouseToAuction(house, price)) {
							house.save();
							counter++;
						}
					}
				}

				if (found) {
					PacketSendUtility.sendMessage(admin, "Added " + counter + " houses of type " + houseType);
				} else {
					PacketSendUtility.sendMessage(admin, "No houses, all are occupied or already in auction!");
				}
				break;
			}
			case "addrandom": {
				if (params.length < 4 || params.length > 5) {
					onFail(admin, null);
					return;
				}

				String param = params[1].toUpperCase();
				Race race;
				switch (param) {
					case "ALL":
					case "PC_ALL":
						race = Race.PC_ALL;
						break;
					case "ELYOS":
						race = Race.ELYOS;
						break;
					case "ASMODIANS":
						race = Race.ASMODIANS;
						break;
					default:
						PacketSendUtility.sendMessage(admin, "Race not found! Use ALL | ELYOS | ASMODIANS!");
						return;
				}

				HouseType houseType;
				try {
					houseType = HouseType.fromValue(params[2].toUpperCase());
				} catch (Exception e) {
					PacketSendUtility.sendMessage(admin, "No such house type!");
					return;
				}

				int count;
				try {
					count = Integer.parseInt(params[3]);
					if (count <= 0) {
						throw new IllegalArgumentException();
					}
				} catch (Exception e) {
					PacketSendUtility.sendMessage(admin, "Invalid count. Only positive numbers!");
					return;
				}
				long bidPrice = 0;
				if (params.length == 5) {
					try {
						bidPrice = Long.parseLong(params[4]);
						if (bidPrice <= 0) {
							throw new IllegalArgumentException();
						}
					} catch (Exception e) {
						PacketSendUtility.sendMessage(admin, "Only positive numbers for the bid price!");
						return;
					}
				}

				int counter = 0;
				List<House> houses = GameHousingServices.housingService().getCustomHouses();
				while (!houses.isEmpty() && counter < count) {
					House house = houses.get(Rnd.get(houses.size()));
					houses.remove(house);
					if (house.getOwnerId() != 0 || house.getHouseType() != houseType) {
						continue;
					}
					if (race != Race.PC_ALL) {
						int mapId = house.getAddress().getMapId();
						if (race.equals(Race.ELYOS)) {
							if (mapId != 700010000 && mapId != 210040000) {
								continue;
							}
						} else {
							if (mapId != 710010000 && mapId != 220040000) {
								continue;
							}
						}
					}
					if (!isAvailableForAuction(house)) {
						continue;
					}

					long price = bidPrice > 0 ? bidPrice : house.getDefaultAuctionPrice();
					if (GameHousingServices.housingBidService().addHouseToAuction(house, price)) {
						house.save();
						counter++;
					}
				}

				if (counter > 0) {
					PacketSendUtility.sendMessage(admin, "Added " + counter + " houses of type " + houseType);
				} else {
					PacketSendUtility.sendMessage(admin, "No houses, all are occupied or already in auction!");
				}
				break;
			}
			case null:
			default:
				onFail(admin, null);
				break;
		}
	}

	/**
	 * 校验房屋当前的拍卖资格状态：停用房屋不可用，SELL_WAIT 且已有竞价的房屋不可用，无竞价则重置为激活。
	 * Validates the house's current auction eligibility: inactive houses are unusable, SELL_WAIT houses with an existing bid are unusable, otherwise the status is reset to active.
	 * @return 状态允许上架则为 true / true if the status allows listing
	 */
	private static boolean isAvailableForAuction(House house) {
		if (house.getStatus() == HouseStatus.INACTIVE) {
			return false;
		}
		if (house.getStatus() == HouseStatus.SELL_WAIT) {
			// 检查竞价条目是否存在 / check to see if the bid entry exists
			HouseBidEntry entry = GameHousingServices.housingBidService().getHouseBid(house.getObjectId());
			if (entry == null) {
				// 重置状态 / reset status
				house.setStatus(HouseStatus.ACTIVE);
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * 参数错误时输出 {@code //auction} 用法。
	 * Prints {@code //auction} usage on invalid arguments.
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, """
			syntax:
			 //auction add <zone_name> <house_type> [initial_bid]
			 //auction remove <HOUSE_id|zone_name> [nosale]
			 //auction addrandom <race> <house_type> <count> [initial_bid]
			 //zone_name = from zones xml files
			 //house_type = house, mansion, estate, palace
			 //initial_bid = initial bid price (if omitted, default is used)""");
	}
}

package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseBidEntry;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员房屋拍卖命令：按区域/类型上架、随机上架或下架房屋。
 * Admin housing-auction command: lists, randomly lists, or removes houses from auction.
 *
 * @author Rolandas
 * @modified Luzien
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
	 *
	 * admin
	 * @param params 参数：子命令与区域/类型/价格等 / subcommand and zone/type/price args
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 1) {
			onFail(admin, null);
			return;
		}

		if ("remove".equals(params[0])) {
			if (params.length < 2) {
				onFail(admin, null);
				return;
			}
			String param = params[1].toUpperCase();
			List<House> housesToRemove = new ArrayList<House>();

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

			if (housesToRemove.size() == 0) {
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
		} else if ("add".equals(params[0])) {

			if (params.length < 3 || params.length > 4) {
				onFail(admin, null);
				return;
			}

			ZoneName zoneName = ZoneName.get(params[1]);
			if (zoneName.name().equals(ZoneName.NONE)) {
				PacketSendUtility.sendMessage(admin, "No such zone!");
				return;
			}

			HouseType houseType = null;
			try {
				houseType = HouseType.fromValue(params[2].toUpperCase());
			} catch (Exception e) {
			}

			if (houseType == null) {
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
				if (house.getStatus() == HouseStatus.INACTIVE) {
					continue;
				}
				if (house.getStatus() == HouseStatus.SELL_WAIT) {
					// 检查竞价条目是否存在 / check to see if the bid entry exists
					HouseBidEntry entry = GameHousingServices.housingBidService().getHouseBid(house.getObjectId());
					if (entry == null) {
						// 重置状态 / reset status
						house.setStatus(HouseStatus.ACTIVE);
					} else {
						continue;
					}
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
		} else if ("addrandom".equals(params[0])) {
			if (params.length < 4 || params.length > 5) {
				onFail(admin, null);
				return;
			}
			
			String param = params[1].toUpperCase();
			Race race;
			if ("ALL".equals(param) || "PC_ALL".equals(param))
				race = Race.PC_ALL;
			else if ("ELYOS".equals(param))
				race = Race.ELYOS;
			else if ("ASMODIANS".equals(param))
				race = Race.ASMODIANS;
			else {
				PacketSendUtility.sendMessage(admin, "Race not found! Use ALL | ELYOS | ASMODIANS!");
				return;
			}

			HouseType houseType = null;
			try {
				houseType = HouseType.fromValue(params[2].toUpperCase());
			} catch (Exception e) {
			}

			if (houseType == null) {
				PacketSendUtility.sendMessage(admin, "No such house type!");
				return;
			}

			int count = 0;
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
					}
					else if (race.equals(Race.ASMODIANS)) {
						if (mapId != 710010000 && mapId != 220040000) {
							continue;
						}
					}
				}
				if (house.getStatus() == HouseStatus.INACTIVE) {
					continue;
				}
				if (house.getStatus() == HouseStatus.SELL_WAIT) {
					// 检查竞价条目是否存在 / check to see if the bid entry exists
					HouseBidEntry entry = GameHousingServices.housingBidService().getHouseBid(house.getObjectId());
					if (entry == null) {
						// 重置状态 / reset status
						house.setStatus(HouseStatus.ACTIVE);
					} else {
						continue;
					}
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

		} else {
			onFail(admin, null);
		}
	}

	/**
	 * 参数错误时输出 {@code //auction} 用法。
	 * Prints {@code //auction} usage on invalid arguments.
	 *
	 * admin
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax:\n"
		+ " //auction add <zone_name> <house_type> [initial_bid]\n"
		+ " //auction remove <HOUSE_id|zone_name> [nosale]\n"
	    + " //auction addrandom <race> <house_type> <count> [initial_bid]\n"
		+ " //zone_name = from zones xml files\n"
		+ " //house_type = house, mansion, estate, palace\n"
		+ " //initial_bid = initial bid price (if omitted, default is used)");
	}
}

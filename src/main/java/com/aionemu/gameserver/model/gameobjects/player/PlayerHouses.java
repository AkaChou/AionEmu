package com.aionemu.gameserver.model.gameobjects.player;

import java.util.List;

import com.aionemu.gameserver.lifecycle.GameHousingServices;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseStatus;

/**
 * 玩家房屋域。
 * Player housing domain.
 *
 * <p>该类型只服务 {@link Player}：负责房屋列表惰性加载、活动房屋查询、房屋 owner id
 * 以及建筑 owner 状态位。公开门面仍保留在 {@link Player}，此处为无状态静态策略。
 * This type only serves {@link Player}: it owns lazy house-list loading, active-house lookup,
 * house-owner id resolution and building-owner state bits. Public facades stay on {@link Player};
 * this is a stateless static policy.</p>
 */
final class PlayerHouses {

	private PlayerHouses() {
	}

	/**
	 * 返回玩家房屋列表，必要时从房屋服务加载。
	 * Returns the player house list, loading it from the housing service when needed.
	 *
	 * @param player 玩家 / player
	 * @return 房屋列表 / house list
	 */
	static List<House> getHouses(Player player) {
		List<House> houses = player.getHousesOrNull();
		if (houses != null) {
			return houses;
		}
		List<House> found = GameHousingServices.housingService().searchPlayerHouses(player.getObjectId());
		if (!found.isEmpty()) {
			player.setHouses(found);
		}
		return found;
	}

	/**
	 * 清空并重置房屋列表缓存。
	 * Clears and resets the house-list cache.
	 *
	 * @param player 玩家 / player
	 */
	static void resetHouses(Player player) {
		List<House> houses = player.getHousesOrNull();
		if (houses != null) {
			houses.clear();
			player.setHouses(null);
		}
	}

	/**
	 * 返回当前活动房屋。
	 * Returns the current active house.
	 *
	 * @param player 玩家 / player
	 * @return 活动房屋，没有时为 null / active house, or null when absent
	 */
	static House getActiveHouse(Player player) {
		for (House house : player.getHouses()) {
			if (house.getStatus() == HouseStatus.ACTIVE || house.getStatus() == HouseStatus.SELL_WAIT) {
				return house;
			}
		}
		return null;
	}

	/**
	 * 返回活动房屋地址 ID。
	 * Returns the active house address id.
	 *
	 * @param player 玩家 / player
	 * @return 地址 ID，没有活动房屋时为 0 / address id, or 0 when no active house exists
	 */
	static int getHouseOwnerId(Player player) {
		House house = player.getActiveHouse();
		return house != null ? house.getAddress().getId() : 0;
	}

	/**
	 * 判断建筑 owner 状态位是否包含指定标记。
	 * Checks whether the building-owner state contains the given flag.
	 *
	 * @param player 玩家 / player
	 * @param state 状态标记 / state flag
	 * @return 包含时为 true / true when present
	 */
	static boolean isBuildingInState(Player player, PlayerHouseOwnerFlags state) {
		return (player.getBuildingOwnerStates() & state.getId()) != 0;
	}

	/**
	 * 设置建筑 owner 状态位并刷新活动房屋状态。
	 * Sets a building-owner state bit and refreshes the active house state.
	 *
	 * @param player 玩家 / player
	 * @param state 状态标记 / state flag
	 */
	static void setBuildingOwnerState(Player player, byte state) {
		player.setBuildingOwnerStates((byte) (player.getBuildingOwnerStates() | state));
		refreshActiveHouseState(player);
	}

	/**
	 * 清除建筑 owner 状态位并刷新活动房屋状态。
	 * Clears a building-owner state bit and refreshes the active house state.
	 *
	 * @param player 玩家 / player
	 * @param state 状态标记 / state flag
	 */
	static void unsetBuildingOwnerState(Player player, byte state) {
		player.setBuildingOwnerStates((byte) (player.getBuildingOwnerStates() & ~state));
		refreshActiveHouseState(player);
	}

	private static void refreshActiveHouseState(Player player) {
		House house = player.getActiveHouse();
		if (house != null) {
			house.fixBuildingStates();
		}
	}
}

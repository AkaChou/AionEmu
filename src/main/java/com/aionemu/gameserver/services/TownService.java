package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.TownDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HouseAddress;
import com.aionemu.gameserver.model.templates.housing.HousingLand;
import com.aionemu.gameserver.model.town.Town;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TOWNS_LIST;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 城镇服务，加载/初始化阵营城镇并查询玩家所在城镇。
 * Town service that loads/initializes race towns and resolves a player's town.
 */
@Slf4j
public class TownService {
	private static volatile ObjectProvider<TownService> instanceProvider;
	private Map<Integer, Town> elyosTowns;
	private Map<Integer, Town> asmosTowns;

	private static class SingletonHolder {
		protected static final TownService instance = new TownService();
	}

	/**
	 * 获取城镇服务单例（优先 Spring ObjectProvider）。
	 * Returns the town service singleton (preferring Spring ObjectProvider).
	 *
	 * service instance
	 */
	public static final TownService getInstance() {
		ObjectProvider<TownService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<TownService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 从数据库加载城镇；若为空则根据房屋地块初始化并持久化。
	 * Loads towns from DB; if empty, initializes from housing lands and persists them.
	 */
	public TownService() {
		elyosTowns = DAOManager.getDAO(TownDAO.class).load(Race.ELYOS);
		asmosTowns = DAOManager.getDAO(TownDAO.class).load(Race.ASMODIANS);
		if (elyosTowns.size() == 0 && asmosTowns.size() == 0) {
			for (HousingLand land : DataManager.HOUSE_DATA.getLands()) {
				for (HouseAddress address : land.getAddresses()) {
					if (address.getTownId() == 0)
						continue;
					else {
						Race townRace = DataManager.NPC_DATA.getNpcTemplate(land.getManagerNpcId())
								.getTribe() == TribeClass.GENERAL ? Race.ELYOS : Race.ASMODIANS;
						if ((townRace == Race.ELYOS && !elyosTowns.containsKey(address.getTownId()))
								|| (townRace == Race.ASMODIANS && !asmosTowns.containsKey(address.getTownId()))) {
							Town town = new Town(address.getTownId(), townRace);
							if (townRace == Race.ELYOS) {
								elyosTowns.put(town.getId(), town);
							} else if (townRace == Race.ASMODIANS) {
								asmosTowns.put(town.getId(), town);
							}
							DAOManager.getDAO(TownDAO.class).store(town);
						}
					}
				}
			}
		}
		log.info(I18n.get("log.e7c70a17528c", asmosTowns.size()));
		log.info(I18n.get("log.2ede9f09c843", asmosTowns.size()));
	}

	/**
	 * 按城镇 ID 查询城镇（先天族后魔族）。
	 * Looks up a town by id (Elyos first, then Asmodians).
	 *
	 * town id
	 *
	 * @param townId @return 城镇，可能为 null / town, may be null
	 */
	public Town getTownById(int townId) {
		if (elyosTowns.containsKey(townId)) {
			return elyosTowns.get(townId);
		} else {
			return asmosTowns.get(townId);
		}
	}

	/**
	 * 获取玩家当前活跃房屋所属城镇 ID。
	 * Returns the town id of the player's active house.
	 *
	 * @param player 玩家 / player
	 * @return 城镇 ID，无房屋时为 0 / town id, or 0 if no house
	 */
	public int getTownResidence(Player player) {
		House house = player.getActiveHouse();
		if (house == null) {
			return 0;
		} else {
			return house.getAddress().getTownId();
		}
	}

	/**
	 * 根据生物位置解析所在城镇 ID（NPC 优先用自身 townId）。
	 * Resolves town id from a creature's position (NPC uses its own townId first).
	 *
	 * creature
	 *
	 * @param creature @return 城镇 ID，未命中为 0 / town id, or 0 if none
	 */
	public int getTownIdByPosition(Creature creature) {
		if (creature instanceof Npc) {
			if (((Npc) creature).getTownId() != 0) {
				return ((Npc) creature).getTownId();
			}
		}
		int townId = 0;
		MapRegion region = creature.getPosition().getMapRegion();
		if (region == null) {
			return 0;
		} else {
			List<ZoneInstance> zones = region.getZones(creature);
			for (ZoneInstance zone : zones) {
				townId = zone.getTownId();
				if (townId > 0) {
					break;
				}
			}
		}
		return townId;
	}

	/**
	 * 玩家进入世界时，在对应阵营主城下发城镇列表。
	 * On enter-world, sends the race town list when the player is in the race capital.
	 *
	 * @param player 玩家 / player
	 */
	public void onEnterWorld(Player player) {
		switch (player.getRace()) {
		case ELYOS:
			if (player.getWorldId() == 700010000)
				PacketSendUtility.sendPacket(player, new SM_TOWNS_LIST(elyosTowns));
			break;
		case ASMODIANS:
			if (player.getWorldId() == 710010000)
				PacketSendUtility.sendPacket(player, new SM_TOWNS_LIST(asmosTowns));
			break;
		default:
			break;
		}
	}
}

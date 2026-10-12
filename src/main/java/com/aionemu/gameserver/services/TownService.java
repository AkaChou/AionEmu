package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
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
import com.aionemu.gameserver.persistence.dao.TownDao;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * 城镇服务，加载/初始化阵营城镇并查询玩家所在城镇。
 * Town service that loads/initializes race towns and resolves a player's town.
 */
@Slf4j
public class TownService {
	/**
	 * -- SETTER --
	 * 注入 Spring 实例提供者。
	 * Injects the Spring instance provider.
	 */
	@Setter
	private static volatile ObjectProvider<TownService> instanceProvider;
	private final TownDao townDAO;
	private final Map<Integer, Town> elyosTowns;
	private final Map<Integer, Town> asmosTowns;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *                               when no provider or bean is available
	 */
	public static TownService getInstance() {
		ObjectProvider<TownService> provider = instanceProvider;
		TownService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("TownService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 使用指定的 TownDAO 构造城镇服务。
	 * Constructs the town service with the given TownDAO.
	 *
	 * @param townDAO 城镇数据访问对象 / town DAO
	 */
	public TownService(@NonNull TownDao townDAO) {
		this.townDAO = townDAO;
		Map<Integer, Town> elyos = null;
		Map<Integer, Town> asmos = null;
		try {
			elyos = this.townDAO.load(Race.ELYOS);
			asmos = this.townDAO.load(Race.ASMODIANS);
		} catch (Exception e) {
			log.warn(I18n.get("log.bb988baa7cb4", e.getMessage()), e);
		}
		this.elyosTowns = elyos != null ? elyos : new HashMap<>();
		this.asmosTowns = asmos != null ? asmos : new HashMap<>();

		if (elyosTowns.isEmpty() && asmosTowns.isEmpty()) {
			try {
				if (DataManager.HOUSE_DATA != null && DataManager.HOUSE_DATA.getLands() != null) {
					for (HousingLand land : DataManager.HOUSE_DATA.getLands()) {
						for (HouseAddress address : land.getAddresses()) {
							if (address.getTownId() != 0) {
								var npcTemplate = DataManager.NPC_DATA != null ? DataManager.NPC_DATA.getNpcTemplate(land.getManagerNpcId()) : null;
								Race townRace = npcTemplate != null && npcTemplate.getTribe() == TribeClass.GENERAL ? Race.ELYOS : Race.ASMODIANS;
								Map<Integer, Town> raceTowns = townRace == Race.ELYOS ? elyosTowns : asmosTowns;
								if (!raceTowns.containsKey(address.getTownId())) {
									Town town = new Town(address.getTownId(), townRace);
									raceTowns.put(town.getId(), town);
									this.townDAO.store(town);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				log.warn(I18n.get("log.c43d847b776a", e.getMessage()), e);
			}
		}
		log.info(I18n.get("log.e7c70a17528c", elyosTowns.size()));
		log.info(I18n.get("log.2ede9f09c843", asmosTowns.size()));
	}

	/**
	 * 按城镇 ID 查询城镇（先天族后魔族）。
	 * Looks up a town by id (Elyos first, then Asmodians).
	 *
	 * @param townId 城镇 ID / town id
	 * @return 城镇，可能为 null / town, may be null
	 */
	public Town getTownById(int townId) {
		Town town = elyosTowns.get(townId);
		return town != null ? town : asmosTowns.get(townId);
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
		return house != null ? house.getAddress().getTownId() : 0;
	}

	/**
	 * 根据生物位置解析所在城镇 ID（NPC 优先用自身 townId）。
	 * Resolves town id from a creature's position (NPC uses its own townId first).
	 *
	 * @param creature 生物 / creature
	 * @return 城镇 ID，未命中为 0 / town id, or 0 if none
	 */
	public int getTownIdByPosition(Creature creature) {
		if (creature instanceof Npc npc && npc.getTownId() != 0) {
			return npc.getTownId();
		}
		MapRegion region = creature.getPosition().getMapRegion();
		if (region == null) {
			return 0;
		}
		for (ZoneInstance zone : region.getZones(creature)) {
			int townId = zone.getTownId();
			if (townId > 0) {
				return townId;
			}
		}
		return 0;
	}

	/**
	 * 玩家进入世界时，在对应阵营主城下发城镇列表。
	 * On enter-world, sends the race town list when the player is in the race capital.
	 *
	 * @param player 玩家 / player
	 */
	public void onEnterWorld(Player player) {
		switch (player.getRace()) {
			case ELYOS -> {
				if (player.getWorldId() == 700010000) {
					PacketSendUtility.sendPacket(player, new SM_TOWNS_LIST(elyosTowns));
				}
			}
			case ASMODIANS -> {
				if (player.getWorldId() == 710010000) {
					PacketSendUtility.sendPacket(player, new SM_TOWNS_LIST(asmosTowns));
				}
			}
			default -> {
			}
		}
	}
}

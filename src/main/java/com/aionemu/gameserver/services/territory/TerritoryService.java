package com.aionemu.gameserver.services.territory;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.TreeMap;
import org.springframework.beans.factory.ObjectProvider;

import lombok.NonNull;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionTerritory;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STONESPEAR_SIEGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TERRITORY_LIST;
import com.aionemu.gameserver.persistence.dao.LegionDao;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 领地服务，管理军团领地占领、增益与传送。
 * Territory service managing legion territory conquest, buffs and teleports.
 */
public class TerritoryService {
	private final LegionService legionService;
	private static volatile ObjectProvider<TerritoryService> instanceProvider;
	private static volatile TerritoryService resolvedInstance;
	private final TreeMap<Integer, LegionTerritory> territories = new TreeMap<>();
	private final LegionDao legionDAO;

	/**
	 * 使用指定的 LegionDAO 构造领地服务。
	 * Constructs the territory service with the given LegionDAO.
	 *
	 * @param legionDAO 军团数据访问对象 / Legion DAO
	 */
	public TerritoryService(@NonNull LegionDao legionDAO, @NonNull LegionService legionService) {
		this.legionService = Objects.requireNonNull(legionService, "legionService must not be null");

		this.legionDAO = legionDAO;
	}

	/**
	 * 初始化全部领地槽位并从数据库加载军团占领状态。
	 * Initializes all territory slots and loads legion ownership from DB.
	 */
	public void initTerritory() {
		LegionService ls = legionService;
		Collection<Legion> legions = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			territories.put(i, new LegionTerritory(i));
		}
		for (Integer legionId : legionDAO.getLegionIdsWithTerritories()) {
			legions.add(ls.getLegion(legionId));
		}
		for (Legion legion : legions) {
			LegionTerritory territory = legion.getTerritory();
			territories.remove(territory.getId());
			territories.put(territory.getId(), territory);
		}
	}

	/**
	 * 玩家进世界时下发领地列表。
	 * Sends territory list when a player enters the world.
	 * @param player 玩家 / Player
	 */
	public void onEnterWorld(Player player) {
		PacketSendUtility.sendPacket(player, new SM_TERRITORY_LIST(territories.values()));
	}

	/**
	 * 军团征服指定领地并广播结果。
	 * Assigns a territory to a legion and broadcasts the result.
	 * @param legion 军团 / Legion
	 * @param id 领地 ID / Territory id
	 */
	public void onConquerTerritory(Legion legion, int id) {
		if (legion.ownsTerretory()) {
			onLooseTerritory(legion);
		}
		LegionTerritory territory = new LegionTerritory(id);
		territory.setLegionId(legion.getLegionId());
		territory.setLegionName(legion.getLegionName());
		legion.setTerritory(territory);
		territories.remove(id);
		territories.put(id, territory);
		broadcastTerritoryList(territories);
		broadcastToLegion(legion);
	}

	/**
	 * 向军团成员广播领地/石矛信息。
	 * Broadcasts territory/Stonespear info to legion members.
	 * @param legion 军团 / Legion
	 */
	private void broadcastToLegion(Legion legion) {
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_INFO(legion));
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_STONESPEAR_SIEGE(legion, 0));
	}

	/**
	 * 军团失去领地并广播空置状态。
	 * Clears a legion's territory and broadcasts the vacant state.
	 * @param legion 军团 / Legion
	 */
	public void onLooseTerritory(Legion legion) {
		int oldTerritoryId = legion.getTerritory().getId();
		legion.clearTerritory();
		LegionTerritory fakeTerritory = new LegionTerritory(oldTerritoryId);
		territories.remove(oldTerritoryId);
		territories.put(oldTerritoryId, fakeTerritory);
		TreeMap<Integer, LegionTerritory> lostTerr = new TreeMap<>();
		lostTerr.put(oldTerritoryId, fakeTerritory);
		broadcastTerritoryList(lostTerr);
		broadcastToLegion(legion);
	}

	/**
	 * 向在线玩家广播领地列表。
	 * Broadcasts territory list to online players.
	 * @param terr 领地映射 / Territory map
	 */
	public void broadcastTerritoryList(TreeMap<Integer, LegionTerritory> terr) {
		Collection<Player> players = World.getInstance().getAllPlayers();
		for (Player player : players) {
			if (!player.isOnline()) {
				return;
			}
			PacketSendUtility.sendPacket(player, new SM_TERRITORY_LIST(terr.values()));
		}
	}

	/**
	 * 获取全部领地集合。
	 * Returns all territories.
	 * @return 领地集合 / territory collection
	 */
	public Collection<LegionTerritory> getTerritories() {
		return territories.values();
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static TerritoryService getInstance() {
		TerritoryService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<TerritoryService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("TerritoryService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 * @param instanceProvider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<TerritoryService> instanceProvider) {
		TerritoryService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}
}

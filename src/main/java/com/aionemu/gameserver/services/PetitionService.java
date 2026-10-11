package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PetitionDAO;
import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PETITION;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客服请愿（Petition）服务，管理工单注册、回复与排队。
 * Support petition service managing ticket registration, reply, and queueing.
 *
 * @author zdead
 */
@Slf4j
public record PetitionService(PetitionDAO petitionDAO) {

	@Setter
	private static volatile ObjectProvider<PetitionService> instanceProvider;
	private static SortedMap<Integer, Petition> registeredPetitions = new ConcurrentSkipListMap<>();

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
	public static PetitionService getInstance() {
		ObjectProvider<PetitionService> provider = instanceProvider;
		PetitionService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("PetitionService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 返回请愿 DAO，供命令域复用同一绑定（单一所有权）。
	 * Returns the petition DAO so command-domain callers reuse the same binding (single ownership).
	 *
	 * @return 请愿数据访问对象 / Petition DAO
	 */
	@Override
	public PetitionDAO petitionDAO() {
		return petitionDAO;
	}

	public PetitionService(@NonNull PetitionDAO petitionDAO) {
		this.petitionDAO = petitionDAO;
		log.info(I18n.get("log.f495913b1078"));
		try {
			Set<Petition> petitions = this.petitionDAO.getPetitions();
			if (petitions != null) {
				for (Petition p : petitions) {
					registeredPetitions.put(p.petitionId(), p);
				}
			}
		} catch (Exception e) {
			log.warn(I18n.get("log.62e2494fabb4", e.getMessage()));
		}
		log.info(I18n.get("log.2b3ade613951", registeredPetitions.size()));
	}

	/**
	 * 从数据库加载已有请愿工单。
	 * Loads existing petition tickets from the database.
	 */
	public PetitionService() {
		this(safeDao(PetitionDAO.class, com.aionemu.gameserver.dao.impl.PetitionDAO::new));
	}

	private static <T extends DAO> T safeDao(Class<T> daoClass, Supplier<T> fallback) {
		if (DAOManager.isInitialized()) {
			try {
				return DAOManager.getDAO(daoClass);
			} catch (Exception ignored) {
			}
		}
		return fallback.get();
	}

	/**
	 * 返回当前已注册请愿的快照集合。
	 * Returns a snapshot collection of currently registered petitions.
	 * petition collection
	 */
	public Collection<Petition> getRegisteredPetitions() {
		return new ArrayList<>(registeredPetitions.values());
	}

	/**
	 * 删除指定玩家的全部请愿并通知客户端。
	 * Deletes all petitions for the given player and notifies the client.
	 *
	 * @param playerObjId player object id
	 */
	public void deletePetition(int playerObjId) {
		Set<Petition> petitions = new HashSet<>();
		for (Petition p : registeredPetitions.values()) {
			if (p.playerObjId() == playerObjId) {
				petitions.add(p);
			}
		}
		for (Petition p : petitions) {
			registeredPetitions.remove(p.petitionId());
		}
		petitionDAO.deletePetition(playerObjId);
		if (playerObjId > 0 && GameWorldBootstrapServices.world().findPlayer(playerObjId) != null) {
			Player p = GameWorldBootstrapServices.world().findPlayer(playerObjId);
			PacketSendUtility.sendPacket(p, new SM_PETITION());
		}
		rebroadcastPlayerData();
	}

	/**
	 * 标记请愿已回复并从队列移除。
	 * Marks a petition as replied and removes it from the queue.
	 *
	 * @param petitionId petition id
	 */
	public void setPetitionReplied(int petitionId) {
		int playerObjId = registeredPetitions.get(petitionId).playerObjId();
		petitionDAO.setReplied(petitionId);
		registeredPetitions.remove(petitionId);
		rebroadcastPlayerData();
		if (playerObjId > 0 && GameWorldBootstrapServices.world().findPlayer(playerObjId) != null) {
			Player p = GameWorldBootstrapServices.world().findPlayer(playerObjId);
			PacketSendUtility.sendPacket(p, new SM_PETITION());
		}
	}


	/**
	 * 向所有仍有请愿的在线玩家重发队列状态。
	 * Rebroadcasts queue status to all online players with active petitions.
	 */
	private void rebroadcastPlayerData() {
		for (Petition p : registeredPetitions.values()) {
			Player player = GameWorldBootstrapServices.world().findPlayer(p.playerObjId());
			if (player != null) {
				PacketSendUtility.sendPacket(player, new SM_PETITION(p));
			}
		}
	}

	/**
	 * 向在线 GM 广播新请愿通知。
	 * Broadcasts a new-petition notice to online GMs.
	 *
	 * @param sender     sender
	 * @param petitionId petition id
	 */
	private void broadcastMessageToGM(Player sender, int petitionId) {
		Iterator<Player> players = GameWorldBootstrapServices.world().getPlayersIterator();
		while (players.hasNext()) {
			Player p = players.next();
			if (p.getAccessLevel() > 0) {
				PacketSendUtility.sendBrightYellowMessageOnCenter(p,
					"New Support Petition from: " + sender.getName() + " (#" + petitionId + ")");
			}
		}
	}

	/**
	 * 判断玩家是否已有注册请愿。
	 * Checks whether the player already has a registered petition.
	 * 玩家 / player
	 * whether registered
	 */
	public boolean hasRegisteredPetition(Player player) {
		return hasRegisteredPetition(player.getObjectId());
	}

	/**
	 * 判断玩家对象 ID 是否已有注册请愿。
	 * Checks whether the player object id already has a registered petition.
	 * player object id
	 * whether registered
	 */
	public boolean hasRegisteredPetition(int playerObjId) {
		boolean result = false;
		for (Petition p : registeredPetitions.values()) {
			if (p.playerObjId() == playerObjId) {
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * 获取玩家当前请愿。
	 * Returns the player's current petition.
	 * player object id
	 *
	 * @return 请愿，不存在为 null / petition, or null if none
	 */
	public Petition getPetition(int playerObjId) {
		for (Petition p : registeredPetitions.values()) {
			if (p.playerObjId() == playerObjId) {
				return p;
			}
		}
		return null;
	}


	/**
	 * 计算该玩家前方排队人数。
	 * Counts how many petitioners are waiting ahead of this player.
	 * player object id
	 * waiting count ahead
	 */
	public int getWaitingPlayers(int playerObjId) {
		int counter = 0;
		for (Petition p : registeredPetitions.values()) {
			if (p.playerObjId() == playerObjId) {
				break;
			}
			counter++;
		}
		return counter;
	}

	/**
	 * 估算该玩家的等待时间（分钟相关单位）。
	 * Estimates wait time for the player (time units related to minutes).
	 * player object id
	 *
	 * @return 估算等待时间 / estimated wait time
	 */
	public int calculateWaitTime(int playerObjId) {
		int timePerPetition = 15;
		int timeBetweenPetition = 30;
		int result = timeBetweenPetition;
		for (Petition p : registeredPetitions.values()) {
			if (p.playerObjId() == playerObjId) {
				break;
			}
			result += timePerPetition;
			result += timeBetweenPetition;
		}
		return result;
	}

	/**
	 * 玩家登录时若有请愿则下发状态包。
	 * On player login, sends petition status packet if one is registered.
	 *
	 * @param player 玩家 / player
	 */
	public void onPlayerLogin(Player player) {
		if (hasRegisteredPetition(player)) {
			PacketSendUtility.sendPacket(player, new SM_PETITION(getPetition(player.getObjectId())));
		}
	}
}

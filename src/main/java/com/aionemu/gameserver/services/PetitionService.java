package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PetitionDAO;
import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PETITION;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 客服请愿（Petition）服务，管理工单注册、回复与排队。
 * Support petition service managing ticket registration, reply, and queueing.
 *
 * @author zdead
 */
@Slf4j
public class PetitionService {

	private static volatile ObjectProvider<PetitionService> instanceProvider;

	private static SortedMap<Integer, Petition> registeredPetitions = new ConcurrentSkipListMap<Integer, Petition>();

	/**
	 * 获取请愿服务单例（优先 Spring ObjectProvider）。
	 * Returns the petition service singleton (preferring Spring ObjectProvider).
	 *
	 * service instance
	 */
	public static final PetitionService getInstance() {
		ObjectProvider<PetitionService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<PetitionService> instanceProvider) {
		PetitionService.instanceProvider = instanceProvider;
	}

	/**
	 * 从数据库加载已有请愿工单。
	 * Loads existing petition tickets from the database.
	 */
	public PetitionService() {
		log.info(I18n.get("log.f495913b1078"));
		Set<Petition> petitions = DAOManager.getDAO(PetitionDAO.class).getPetitions();
		if (petitions != null) {
			for (Petition p : petitions) {
				registeredPetitions.put(p.getPetitionId(), p);
			}
		}
		log.info(I18n.get("log.2b3ade613951", registeredPetitions.size()));
	}

	/**
	 * 返回当前已注册请愿的快照集合。
	 * Returns a snapshot collection of currently registered petitions.
	 *
	 * petition collection
	 */
	public Collection<Petition> getRegisteredPetitions() {
		return new ArrayList<Petition>(registeredPetitions.values());
	}

	/**
	 * 删除指定玩家的全部请愿并通知客户端。
	 * Deletes all petitions for the given player and notifies the client.
	 *
	 * player object id
	 */
	public void deletePetition(int playerObjId) {
		Set<Petition> petitions = new HashSet<Petition>();
		for (Petition p : registeredPetitions.values()) {
			if (p.getPlayerObjId() == playerObjId) {
				petitions.add(p);
			}
		}
		for (Petition p : petitions) {
			if (registeredPetitions.containsKey(p.getPetitionId())) {
				registeredPetitions.remove(p.getPetitionId());
			}
		}
		DAOManager.getDAO(PetitionDAO.class).deletePetition(playerObjId);
		if (playerObjId > 0 && com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerObjId) != null) {
			Player p = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerObjId);
			PacketSendUtility.sendPacket(p, new SM_PETITION());
		}
		rebroadcastPlayerData();
	}

	/**
	 * 标记请愿已回复并从队列移除。
	 * Marks a petition as replied and removes it from the queue.
	 *
	 * petition id
	 */
	public void setPetitionReplied(int petitionId) {
		int playerObjId = registeredPetitions.get(petitionId).getPlayerObjId();
		DAOManager.getDAO(PetitionDAO.class).setReplied(petitionId);
		registeredPetitions.remove(petitionId);
		rebroadcastPlayerData();
		if (playerObjId > 0 && com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerObjId) != null) {
			Player p = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerObjId);
			PacketSendUtility.sendPacket(p, new SM_PETITION());
		}
	}

	/**
	 * 注册新请愿并通知在线 GM。
	 * Registers a new petition and notifies online GMs.
	 *
	 * sender
	 * type id
	 * title
	 * content text
	 * additional data
	 * created petition
	 */
	public synchronized Petition registerPetition(Player sender, int typeId, String title, String contentText,
			String additionalData) {
		int id = DAOManager.getDAO(PetitionDAO.class).getNextAvailableId();
		Petition ptt = new Petition(id, sender.getObjectId(), typeId, title, contentText, additionalData, 0);
		DAOManager.getDAO(PetitionDAO.class).insertPetition(ptt);
		registeredPetitions.put(ptt.getPetitionId(), ptt);
		broadcastMessageToGM(sender, ptt.getPetitionId());
		return ptt;
	}

	/**
	 * 向所有仍有请愿的在线玩家重发队列状态。
	 * Rebroadcasts queue status to all online players with active petitions.
	 */
	private void rebroadcastPlayerData() {
		for (Petition p : registeredPetitions.values()) {
			Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(p.getPlayerObjId());
			if (player != null) {
				PacketSendUtility.sendPacket(player, new SM_PETITION(p));
			}
		}
	}

	/**
	 * 向在线 GM 广播新请愿通知。
	 * Broadcasts a new-petition notice to online GMs.
	 *
	 * sender
	 * petition id
	 */
	private void broadcastMessageToGM(Player sender, int petitionId) {
		Iterator<Player> players = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
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
	 *
	 * 玩家 / player
	 * whether registered
	 */
	public boolean hasRegisteredPetition(Player player) {
		return hasRegisteredPetition(player.getObjectId());
	}

	/**
	 * 判断玩家对象 ID 是否已有注册请愿。
	 * Checks whether the player object id already has a registered petition.
	 *
	 * player object id
	 * whether registered
	 */
	public boolean hasRegisteredPetition(int playerObjId) {
		boolean result = false;
		for (Petition p : registeredPetitions.values()) {
			if (p.getPlayerObjId() == playerObjId) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * 获取玩家当前请愿。
	 * Returns the player's current petition.
	 *
	 * player object id
	 *
	 * @param playerObjId
	 * @return 请愿，不存在为 null / petition, or null if none
	 */
	public Petition getPetition(int playerObjId) {
		for (Petition p : registeredPetitions.values()) {
			if (p.getPlayerObjId() == playerObjId) {
				return p;
			}
		}
		return null;
	}

	/**
	 * 获取下一个可用请愿 ID（当前实现固定返回 0）。
	 * Returns the next available petition id (current implementation always returns 0).
	 *
	 * petition id
	 */
	public synchronized int getNextAvailablePetitionId() {
		return 0;
	}

	/**
	 * 计算该玩家前方排队人数。
	 * Counts how many petitioners are waiting ahead of this player.
	 *
	 * player object id
	 * waiting count ahead
	 */
	public int getWaitingPlayers(int playerObjId) {
		int counter = 0;
		for (Petition p : registeredPetitions.values()) {
			if (p.getPlayerObjId() == playerObjId) {
				break;
			}
			counter++;
		}
		return counter;
	}

	/**
	 * 估算该玩家的等待时间（分钟相关单位）。
	 * Estimates wait time for the player (time units related to minutes).
	 *
	 * player object id
	 *
	 * @param playerObjId
	 * @return 估算等待时间 / estimated wait time
	 */
	public int calculateWaitTime(int playerObjId) {
		int timePerPetition = 15;
		int timeBetweenPetition = 30;
		int result = timeBetweenPetition;
		for (Petition p : registeredPetitions.values()) {
			if (p.getPlayerObjId() == playerObjId) {
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

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final PetitionService instance = new PetitionService();
	}
}

package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AnnouncementsDAO;
import com.aionemu.gameserver.model.Announcement;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * 自动公告服务，按间隔向在线玩家广播系统公告。
 * Automatic announcement service broadcasting system messages to online players on a schedule.
 *
 * @author Divinity
 */
@Slf4j
public class AnnouncementService {

	private static volatile ObjectProvider<AnnouncementService> instanceProvider;

	/** 已加载的公告集合。 / Loaded announcements. */
	private Collection<Announcement> announcements;
	/** 定时广播任务列表。 / Scheduled broadcast task list. */
	private List<Future<?>> delays = new ArrayList<Future<?>>();

	/**
	 * 构造服务并加载公告。
	 * Constructs the service and loads announcements.
	 */
	public AnnouncementService() {
		this.load();
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final AnnouncementService getInstance() {
		ObjectProvider<AnnouncementService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<AnnouncementService> instanceProvider) {
		AnnouncementService.instanceProvider = instanceProvider;
	}

	/**
	 * 取消已有定时任务并重新加载公告。
	 * Cancels existing schedules and reloads announcements.
	 */
	public void reload() {
		// 取消全部任务 / Cancel all tasks
		if (delays != null && delays.size() > 0) {
			for (Future<?> delay : delays) {
				delay.cancel(false);
			}
		}
		// 清除全部公告 / Clear all announcements
		announcements.clear();

		// 并重新加载全部公告 / And load again all announcements
		load();
	}

	/**
	 * 从数据库加载公告并注册定时广播。
	 * Loads announcements from DB and schedules fixed-rate broadcasts.
	 */
	private void load() {
		announcements = new HashSet<Announcement>(getDAO().getAnnouncements());

		for (final Announcement announce : announcements) {
			delays.add(GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					final Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
					while (iter.hasNext()) {
						Player player = iter.next();

						if (announce.getFaction().equalsIgnoreCase("ALL"))
							if (announce.getChatType() == ChatType.SHOUT
									|| announce.getChatType() == ChatType.GROUP_LEADER) {
								PacketSendUtility.sendPacket(player, new SM_MESSAGE(1, "Announcement",
										announce.getAnnounce(), announce.getChatType()));
							} else {
								PacketSendUtility.sendPacket(player, new SM_MESSAGE(1, "Announcement",
										"Announcement: " + announce.getAnnounce(), announce.getChatType()));
							}
						else if (announce.getFactionEnum() == player.getRace()) {
							if (announce.getChatType() == ChatType.SHOUT
									|| announce.getChatType() == ChatType.GROUP_LEADER) {
								PacketSendUtility.sendPacket(player,
										new SM_MESSAGE(1,
												(announce.getFaction().equalsIgnoreCase("ELYOS") ? "Elyos" : "Asmodian")
														+ " Announcement",
												announce.getAnnounce(), announce.getChatType()));
							}
						} else {
							PacketSendUtility.sendPacket(player, new SM_MESSAGE(1,
									(announce.getFaction().equalsIgnoreCase("ELYOS") ? "Elyos" : "Asmodian")
											+ " Announcement",
									(announce.getFaction().equalsIgnoreCase("ELYOS") ? "Elyos" : "Asmodian")
											+ " Announcement: " + announce.getAnnounce(),
									announce.getChatType()));
						}
					}
				}
			}, announce.getDelay() * 1000, announce.getDelay() * 1000));
		}
		log.info(I18n.get("log.60ed3a14c1fd", announcements.size()));
	}

	/**
	 * 新增一条公告到数据库。
	 * Adds an announcement to the database.
	 *
	 * announcement
	 */
	public void addAnnouncement(Announcement announce) {
		getDAO().addAnnouncement(announce);
	}

	/**
	 * 按 ID 删除公告。
	 * Deletes an announcement by id.
	 *
	 * announcement id
	 *
	 * @param idAnnounce @return 删除成功返回 true / true if deleted
	 */
	public boolean delAnnouncement(final int idAnnounce) {
		return getDAO().delAnnouncement(idAnnounce);
	}

	/**
	 * 获取数据库中的全部公告。
	 * Returns all announcements from the database.
	 *
	 * announcement set
	 */
	public Set<Announcement> getAnnouncements() {
		return getDAO().getAnnouncements();
	}

	/**
	 * 获取公告 DAO 的快捷方法。
	 * Shortcut to the announcements DAO.
	 *
	 * DAO instance
	 */
	private AnnouncementsDAO getDAO() {
		return DAOManager.getDAO(AnnouncementsDAO.class);
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final AnnouncementService instance = new AnnouncementService();
	}
}

package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import lombok.NonNull;
import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AnnouncementsDAO;
import com.aionemu.gameserver.model.Announcement;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

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

	@Setter
	private static volatile ObjectProvider<AnnouncementService> instanceProvider;

	private final AnnouncementsDAO announcementsDAO;
	/**
	 * 已加载的公告集合。 / Loaded announcements.
	 */
	private Collection<Announcement> announcements;
	/**
	 * 定时广播任务列表。 / Scheduled broadcast task list.
	 */
	private final List<Future<?>> delays = new ArrayList<>();

	/**
	 * 使用指定的 AnnouncementsDAO 构造服务并加载公告。
	 * Constructs the service with the given AnnouncementsDAO and loads announcements.
	 *
	 * @param announcementsDAO 系统公告数据访问对象 / Announcements DAO
	 */
	public AnnouncementService(@NonNull AnnouncementsDAO announcementsDAO) {
		this.announcementsDAO = announcementsDAO;
		this.load();
	}

	/**
	 * 构造服务并加载公告。
	 * Constructs the service and loads announcements.
	 */
	public AnnouncementService() {
		this(safeDao(AnnouncementsDAO.class, com.aionemu.gameserver.dao.impl.AnnouncementsDAO::new));
	}

	private static <T extends com.aionemu.commons.database.dao.DAO> T safeDao(Class<T> daoClass, java.util.function.Supplier<T> fallback) {
		if (DAOManager.isInitialized()) {
			try {
				return DAOManager.getDAO(daoClass);
			} catch (Exception ignored) {
			}
		}
		return fallback.get();
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static AnnouncementService getInstance() {
		ObjectProvider<AnnouncementService> provider = instanceProvider;
		AnnouncementService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("AnnouncementService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 取消已有定时任务并重新加载公告。
	 * Cancels existing schedules and reloads announcements.
	 */
	public void reload() {
		// 取消全部任务 / Cancel all tasks
		if (delays != null && !delays.isEmpty()) {
			for (Future<?> delay : delays) {
				delay.cancel(false);
			}
		}
		// 清除全部公告 / Clear all announcements
		if (announcements != null) {
			announcements.clear();
		}

		// 并重新加载全部公告 / And load again all announcements
		load();
	}

	/**
	 * 从数据库加载公告并注册定时广播。
	 * Loads announcements from DB and schedules fixed-rate broadcasts.
	 */
	private void load() {
		Set<Announcement> loaded = getDAO().getAnnouncements();
		announcements = loaded != null ? new HashSet<>(loaded) : new HashSet<>();

		try {
			for (final Announcement announce : announcements) {
				delays.add(GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
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
				}, announce.getDelay() * 1000L, announce.getDelay() * 1000L));
			}
		} catch (Exception e) {
			log.warn(I18n.get("log.a736bef37f9e", e.getMessage()));
		}
		log.info(I18n.get("log.60ed3a14c1fd", announcements.size()));
	}

	/**
	 * 新增一条公告到数据库。
	 * Adds an announcement to the database.
	 *
	 * @param announce announcement
	 */
	public void addAnnouncement(Announcement announce) {
		getDAO().addAnnouncement(announce);
	}

	/**
	 * 按 ID 删除公告。
	 * Deletes an announcement by id.
	 * announcement id
	 *
	 * @return 删除成功返回 true / true if deleted
	 */
	public boolean delAnnouncement(final int idAnnounce) {
		return getDAO().delAnnouncement(idAnnounce);
	}

	/**
	 * 获取数据库中的全部公告。
	 * Returns all announcements from the database.
	 * announcement set
	 */
	public Set<Announcement> getAnnouncements() {
		return getDAO().getAnnouncements();
	}

	/**
	 * 获取公告 DAO 的快捷方法。
	 * Shortcut to the announcements DAO.
	 * DAO instance
	 */
	private AnnouncementsDAO getDAO() {
		return announcementsDAO;
	}
}

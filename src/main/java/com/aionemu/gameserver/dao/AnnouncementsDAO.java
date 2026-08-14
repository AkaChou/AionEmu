package com.aionemu.gameserver.dao;

import java.util.Set;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.Announcement;

/**
 * 公告数据访问对象，负责管理服务器公告。
 * DAO that manages server announcements.
 *
 * @author Divinity
 */
public abstract class AnnouncementsDAO implements DAO {

	/**
	 * 获取全部公告。
	 * Gets all announcements.
	 *
	 * @return 公告集合 / announcement set
	 */
	public abstract Set<Announcement> getAnnouncements();

	/**
	 * 添加一条公告。
	 * Adds an announcement.
	 *
	 * @param announce 公告 / announcement
	 */
	public abstract void addAnnouncement(final Announcement announce);

	/**
	 * 删除指定 ID 的公告。
	 * Deletes the announcement with the given ID.
	 *
	 * @param idAnnounce 公告 ID / announcement ID
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean delAnnouncement(final int idAnnounce);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public final String getClassName() {
		return AnnouncementsDAO.class.getName();
	}
}

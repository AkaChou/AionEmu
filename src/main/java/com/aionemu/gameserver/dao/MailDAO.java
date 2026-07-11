package com.aionemu.gameserver.dao;

import java.sql.Timestamp;

import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;

/**
 * 邮件数据访问对象，负责玩家邮箱信件的加载、存储与删除。
 * Mail data access object responsible for loading, storing and deleting player mailbox letters.
 *
 * @author kosyachok
 */
public abstract class MailDAO implements IDFactoryAwareDAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * DAO class name
	 */
	@Override
	public String getClassName() {
		return MailDAO.class.getName();
	}

	/**
	 * 将一封信件按指定时间戳持久化到数据库。
	 * Persists a letter into the database with the given timestamp.
	 *
	 * @param time 发信时间 / letter time
	 * letter object
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean storeLetter(Timestamp time, Letter letter);

	/**
	 * 加载玩家邮箱内容。
	 * Loads the player's mailbox contents.
	 *
	 * 玩家 / player
	 * mailbox
	 */
	public abstract Mailbox loadPlayerMailbox(Player player);

	/**
	 * 将玩家邮箱状态整体写回数据库。
	 * Stores the player's mailbox state back to the database.
	 *
	 * 玩家 / player
	 */
	public abstract void storeMailbox(Player player);

	/**
	 * 按信件 ID 删除信件。
	 * Deletes a letter by its ID.
	 *
	 * letter id
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean deleteLetter(int letterId);

	/**
	 * 更新离线收件人的未读邮件计数。
	 * Updates the unread-mail counter for an offline recipient.
	 *
	 * @param recipientCommonData 收件人公共数据 / recipient common data
	 */
	public abstract void updateOfflineMailCounter(PlayerCommonData recipientCommonData);

	/**
	 * 判断玩家是否有未读邮件。
	 * Checks whether the player has any unread mail.
	 *
	 * player id
	 * @return 是否有未读 / true if there is unread mail
	 */
	public abstract boolean haveUnread(int playerId);
}

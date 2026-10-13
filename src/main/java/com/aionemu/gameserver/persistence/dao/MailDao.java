package com.aionemu.gameserver.persistence.dao;

import java.sql.Timestamp;

import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.persistence.entity.MailPo;
import com.mybatisflex.core.service.IService;

/**
 * 邮件数据访问对象，负责玩家邮箱信件的加载、存储与删除。
 * Mail data access object responsible for loading, storing and deleting player mailbox letters.
 * @author kosyachok
 */
public interface MailDao extends IService<MailPo> {

	/**
	 * 将一封信件按指定时间戳持久化到数据库。
	 * Persists a letter into the database with the given timestamp.
	 * @param time 发信时间 / letter time
	 * @param letter 信件对象 / letter object
	 * @return 若成功则为 true / true if successful
	 */
	boolean storeLetter(Timestamp time, Letter letter);

	/**
	 * 在调用方事务内持久化信件。
	 * Stores a letter inside the caller's transaction.
	 * @param time 发信时间 / letter time
	 * @param letter 信件对象 / letter object
	 */
	void storeLetterInTransaction(Timestamp time, Letter letter);

	/**
	 * 在调用方事务内更新离线收件人未读邮件计数。
	 * Updates the offline recipient's unread counter inside the caller's transaction.
	 * @param playerName 玩家名 / player name
	 * @param count 未读邮件计数 / unread mail count
	 */
	void updateMailCounterInTransaction(String playerName, int count);

	/**
	 * 加载玩家邮箱内容。
	 * Loads the player's mailbox contents.
	 * @param player 玩家 / player
	 * @return 邮箱 / mailbox
	 */
	Mailbox loadPlayerMailbox(Player player);

	/**
	 * 将玩家邮箱状态整体写回数据库。
	 * Stores the player's mailbox state back to the database.
	 * @param player 玩家 / player
	 */
	void storeMailbox(Player player);

	/**
	 * 按信件 ID 删除信件。
	 * Deletes a letter by its ID.
	 * @param letterId 信件 ID / letter id
	 */
	void deleteLetter(int letterId);

	/**
	 * 更新离线收件人的未读邮件计数。
	 * Updates the unread-mail counter for an offline recipient.
	 * @param recipientCommonData 收件人公共数据 / recipient common data
	 */
	void updateOfflineMailCounter(PlayerCommonData recipientCommonData);

	/**
	 * 判断玩家是否有未读邮件。
	 * Checks whether the player has any unread mail.
	 * @param playerId 玩家 ID / player id
	 * @return 是否有未读 / true if there is unread mail
	 */
	boolean haveUnread(int playerId);

	/**
	 * 返回本 DAO 已使用的全部邮件 ID（{@code IDFactory} 初始化用）。
	 * Returns an array of all mail ids used by this DAO (for {@code IDFactory} initialization).
	 * @return 已使用 ID 数组 / array of used ids
	 */
	int[] getUsedIDs();
}

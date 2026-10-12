package com.aionemu.gameserver.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.InventoryPo;
import com.aionemu.gameserver.persistence.entity.MailPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 mail 的映射层。
 * Mapper for table mail.
 *
 * <p>邮箱附件读 inventory、离线计数写 players 均跨表（归属其它闭包），保留注解 SQL；
 * 信件插入/更新经 storeLetterInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
 * Mailbox attachments read inventory and the offline counter writes players, both foreign
 * tables owned by other closures, so they stay as annotated SQL; letter insert/update run
 * through the raw PreparedStatement inside storeLetterInTransaction, so `?` is kept.
 */
public interface MailMapper extends BaseMapper<MailPo> {

	/**
	 * 读取邮箱位置（item_location=127）的附件物品（原 SELECT_INVENTORY_QUERY）。
	 * Loads mailbox attachment items (location=127) (formerly SELECT_INVENTORY_QUERY).
	 */
	@Select("SELECT * FROM inventory WHERE `item_owner` = #{playerId} AND `item_location` = 127")
	List<InventoryPo> selectMailboxItems(@Param("playerId") int playerId);

	/**
	 * 更新离线玩家的邮箱信件计数（原 UPDATE_MAIL_COUNTER_QUERY）。
	 * Updates the offline recipient's mailbox letter counter (formerly UPDATE_MAIL_COUNTER_QUERY).
	 */
	@Update("UPDATE players SET mailbox_letters = #{count} WHERE name = #{playerName}")
	int updateMailboxLetters(@Param("playerName") String playerName, @Param("count") int count);

	/**
	 * 插入信件；经 storeLetterInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
	 * Insert statement; executed by raw PreparedStatement inside storeLetterInTransaction, so `?` is kept.
	 */
	String INSERT_MAIL_QUERY_JDBC = "INSERT INTO `mail` (`mail_unique_id`, `mail_recipient_id`, `sender_name`, "
		+ "`mail_title`, `mail_message`, `unread`, `attached_item_id`, `attached_kinah_count`, "
		+ "`express`, `recieved_time`, `attached_ap_count`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * 更新信件；经 storeLetterInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
	 * Update statement; executed by raw PreparedStatement inside storeLetterInTransaction, so `?` is kept.
	 */
	String UPDATE_MAIL_QUERY_JDBC = "UPDATE mail SET unread = ?, attached_item_id = ?, attached_kinah_count = ?, "
		+ "`express` = ?, recieved_time = ?, attached_ap_count = ? WHERE mail_unique_id = ?";

	/**
	 * 更新离线玩家邮箱计数；经 updateMailCounterInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
	 * Counter statement; executed by raw PreparedStatement inside updateMailCounterInTransaction, so `?` is kept.
	 */
	String UPDATE_MAIL_COUNTER_QUERY_JDBC = "UPDATE players SET mailbox_letters = ? WHERE name = ?";
}

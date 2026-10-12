package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.persistence.dao.ItemStoneDao;
import com.aionemu.gameserver.persistence.dao.MailDao;
import com.aionemu.gameserver.persistence.entity.InventoryPo;
import com.aionemu.gameserver.persistence.entity.MailPo;
import com.aionemu.gameserver.persistence.mapper.MailMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 邮件系统数据访问实现。
 * Data access implementation for the mailbox system.
 * @author kosyachok
 */
@Slf4j
@Service
@Lazy
@RequiredArgsConstructor
public class MailDaoImpl extends ServiceImpl<MailMapper, MailPo> implements MailDao {

	/**
	 * 镶嵌石数据访问（跨闭包依赖，注入对方 Dao 而非 Mapper）。
	 * Item-stone data access (cross-closure dependency; the peer Dao is injected, not its Mapper).
	 */
	private final ItemStoneDao itemStoneDao;

	/**
	 * 加载玩家邮箱（含附件物品）。
	 * Loads the player's mailbox including attached items.
	 * @param player 玩家 / player
	 * @return 邮箱 / mailbox
	 */
	@Override
	public Mailbox loadPlayerMailbox(Player player) {
		final Mailbox mailbox = new Mailbox(player);
		final int playerId = player.getObjectId();

		List<Item> mailboxItems = loadMailboxItems(playerId);

		try {
			mapper.selectListByQuery(QueryWrapper.create()
				.where(MailPo::getMailRecipientId).eq(playerId)
				.orderBy(MailPo::getRecievedTime, false)
				.limit(100))
				.forEach(po -> {
					Item attachedItem = findAttachedItem(mailboxItems, po.getAttachedItemId());
					Letter letter = new Letter(po.getMailUniqueId(), po.getMailRecipientId(), attachedItem,
						po.getAttachedKinahCount(), po.getAttachedApCount(), po.getMailTitle(),
						po.getMailMessage(), po.getSenderName(), po.getRecievedTime(), intValue(po.getUnread()) == 1,
						LetterType.getLetterTypeById(intValue(po.getExpress())));
					letter.setPersistState(PersistentState.UPDATED);
					mailbox.putLetterToMailbox(letter);
				});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.e6130c3e4ab9", playerId), e);
		}

		return mailbox;
	}

	/**
	 * 检查玩家是否有未读邮件。
	 * Checks whether the player has unread mail.
	 * @param playerId 玩家 ID / player id
	 * @return 是否有未读邮件 / whether unread mail exists
	 */
	@Override
	public boolean haveUnread(int playerId) {
		try {
			return mapper.selectCountByQuery(QueryWrapper.create()
				.where(MailPo::getMailRecipientId).eq(playerId)
				.and(MailPo::getUnread).eq(1)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.fa6fe5701ebb", playerId), e);
			return false;
		}
	}

	/**
	 * 加载邮箱附件物品列表。
	 * Loads mailbox attachment items for the player.
	 * @param playerId 玩家 ID / player id
	 * @return 附件物品列表 / list of attachment items
	 */
	private List<Item> loadMailboxItems(final int playerId) {
		List<Item> mailboxItems = new ArrayList<>();

		try {
			mapper.selectMailboxItems(playerId).forEach(po -> mailboxItems.add(toItem(po)));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.08b401a571e4", playerId), e);
		}

		return mailboxItems;
	}

	/**
	 * 在邮箱附件列表中定位被引用的附件物品，并按需补载镶嵌石。
	 * Locates the referenced attachment among the mailbox items and loads its stones on demand.
	 * @param mailboxItems 附件物品列表 / attachment item list
	 * @param attachedItemId 附件物品 ID；0 表示无附件 / attachment id, 0 means none
	 * @return 附件物品；未找到为 null / the attachment, or null
	 */
	private Item findAttachedItem(List<Item> mailboxItems, int attachedItemId) {
		if (attachedItemId == 0) {
			return null;
		}
		for (Item item : mailboxItems) {
			if (item.getObjectId() == attachedItemId) {
				if (item.getItemTemplate().isArmor() || item.getItemTemplate().isWeapon()) {
					itemStoneDao.load(Collections.singletonList(item));
				}
				return item;
			}
		}
		return null;
	}

	/**
	 * 由 inventory 行构造邮箱附件物品（列与字段顺序同原 SELECT_INVENTORY_QUERY）。
	 * Builds a mailbox attachment from an inventory row (columns and order as the original query).
	 * @param po inventory 行 / inventory row
	 * @return 物品 / item
	 */
	private static Item toItem(InventoryPo po) {
		Item item = new Item(po.getItemUniqueId(), po.getItemId(), po.getItemCount(), po.getItemColor(),
			intValue(po.getColorExpires()), po.getItemCreator(), po.getExpireTime(), po.getActivationCount(),
			Boolean.TRUE.equals(po.getEquiped()), Boolean.TRUE.equals(po.getSoulBound()), po.getSlot(),
			StorageType.MAILBOX.getId(), po.getEnchant(), po.getEnchantBonus(), po.getItemSkin(),
			po.getFusionedItem(), po.getOptionalSocket(), po.getOptionalFusionSocket(), po.getCharge(),
			intValue(po.getRndBonus()), po.getRndCount(), po.getWrappableCount(), Boolean.TRUE.equals(po.getPacked()),
			po.getTemperingLevel(), Boolean.TRUE.equals(po.getTopped()), po.getStrengthenSkill(),
			intValue(po.getSkinSkill()), Boolean.TRUE.equals(po.getLunaReskin()), po.getReductionLevel(), po.getSeal(),
			Boolean.TRUE.equals(po.getIsEnhance()), po.getEnhanceSkillId(), po.getEnhanceSkillEnchant());
		item.setPersistentState(PersistentState.UPDATED);
		return item;
	}

	/**
	 * 持久化玩家邮箱中全部信件。
	 * Persists all letters currently in the player's mailbox.
	 * @param player 玩家 / player
	 */
	@Override
	public void storeMailbox(Player player) {
		Mailbox mailbox = player.getMailbox();
		if (mailbox == null) {
			return;
		}

		Collection<Letter> letters = mailbox.getLetters();
		for (Letter letter : letters) {
			storeLetter(letter.getTimeStamp(), letter);
		}
	}

	/**
	 * 按持久化状态插入或更新单封信件。
	 * Inserts or updates a single letter according to its persistent state.
	 * @param time 时间戳 / timestamp
	 * @param letter 邮件 / letter
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean storeLetter(Timestamp time, Letter letter) {
		try (Connection con = DatabaseAccess.getConnection()) {
			storeLetterInTransaction(con, time, letter);
			letter.setPersistState(PersistentState.UPDATED);
			return true;
		} catch (SQLException e) {
			log.error(I18n.get("log.2d8239fcf778", letter.getRecipientId()), e);
			return false;
		}
	}

	/**
	 * 在调用方事务连接上按持久化状态插入或更新信件。
	 * Inserts or updates a letter on the caller's transactional connection.
	 * @param con 调用方事务连接 / caller's transactional connection
	 * @param time 时间戳 / timestamp
	 * @param letter 邮件 / letter
	 * @throws SQLException 底层 JDBC 异常 / underlying JDBC failure
	 */
	@Override
	public void storeLetterInTransaction(Connection con, Timestamp time, Letter letter) throws SQLException {
		boolean result = switch (letter.getLetterPersistentState()) {
			case NEW -> saveLetter(con, time, letter);
			case UPDATE_REQUIRED -> updateLetter(con, time, letter);
			default -> true;
		};
		if (!result) {
			throw new SQLException("No mail row changed for letter " + letter.getObjectId());
		}
	}

	/**
	 * 插入新信件。
	 * Inserts a new letter.
	 * @param con 数据库连接 / database connection
	 * @param time 时间戳 / timestamp
	 * @param letter 邮件 / letter
	 * @return 是否成功 / whether successful
	 */
	private boolean saveLetter(Connection con, final Timestamp time, final Letter letter) throws SQLException {
		int attachedItemId = 0;
		if (letter.getAttachedItem() != null) {
			attachedItemId = letter.getAttachedItem().getObjectId();
		}

		try (PreparedStatement stmt = con.prepareStatement(MailMapper.INSERT_MAIL_QUERY_JDBC)) {
			stmt.setInt(1, letter.getObjectId());
			stmt.setInt(2, letter.getRecipientId());
			stmt.setString(3, letter.getSenderName());
			stmt.setString(4, letter.getTitle());
			stmt.setString(5, letter.getMessage());
			stmt.setBoolean(6, letter.isUnread());
			stmt.setInt(7, attachedItemId);
			stmt.setLong(8, letter.getAttachedKinah());
			stmt.setInt(9, letter.getLetterType().getId());
			stmt.setTimestamp(10, time);
			stmt.setLong(11, letter.getAttachedAp());

			return stmt.executeUpdate() > 0;
		}
	}

	/**
	 * 更新已有信件。
	 * Updates an existing letter.
	 * @param con 数据库连接 / database connection
	 * @param time 时间戳 / timestamp
	 * @param letter 邮件 / letter
	 * @return 是否成功 / whether successful
	 */
	private boolean updateLetter(Connection con, final Timestamp time, final Letter letter) throws SQLException {
		int attachedItemId = 0;
		if (letter.getAttachedItem() != null) {
			attachedItemId = letter.getAttachedItem().getObjectId();
		}

		try (PreparedStatement stmt = con.prepareStatement(MailMapper.UPDATE_MAIL_QUERY_JDBC)) {
			stmt.setBoolean(1, letter.isUnread());
			stmt.setInt(2, attachedItemId);
			stmt.setLong(3, letter.getAttachedKinah());
			stmt.setInt(4, letter.getLetterType().getId());
			stmt.setTimestamp(5, time);
			stmt.setLong(6, letter.getAttachedAp());
			stmt.setInt(7, letter.getObjectId());

			return stmt.executeUpdate() > 0;
		}
	}

	/**
	 * 在调用方事务连接上更新离线收件人的邮箱信件计数。
	 * Updates the offline recipient's mailbox counter on the caller's transactional connection.
	 * @param con 调用方事务连接 / caller's transactional connection
	 * @param playerName 玩家名 / player name
	 * @param count 信件计数 / letter count
	 * @throws SQLException 底层 JDBC 异常 / underlying JDBC failure
	 */
	@Override
	public void updateMailCounterInTransaction(Connection con, String playerName, int count) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(MailMapper.UPDATE_MAIL_COUNTER_QUERY_JDBC)) {
			stmt.setInt(1, count);
			stmt.setString(2, playerName);
			if (stmt.executeUpdate() == 0) {
				throw new SQLException("No player row changed for mail counter: " + playerName);
			}
		}
	}

	/**
	 * 删除指定信件。
	 * Deletes the letter with the given id.
	 * @param letterId 邮件 ID / letter id
	 */
	@Override
	public void deleteLetter(final int letterId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create().where(MailPo::getMailUniqueId).eq(letterId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.65c1132a74c3", letterId), e);
		}
	}

	/**
	 * 更新离线收件人的邮箱信件计数。
	 * Updates the offline recipient's mailbox letter counter.
	 * @param recipientCommonData 收件人公共数据 / recipient common data
	 */
	@Override
	public void updateOfflineMailCounter(final PlayerCommonData recipientCommonData) {
		try {
			mapper.updateMailboxLetters(recipientCommonData.getName(), recipientCommonData.getMailboxLetters());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a4015981f3b5", recipientCommonData.getName()), e);
		}
	}

	/**
	 * 返回邮件表中已占用的全部邮件 ID。
	 * Returns all used mail unique ids from the mail table.
	 * @return 已占用 ID 数组；失败时返回空数组 / used id array, or empty array on failure
	 */
	@Override
	public int[] getUsedIDs() {
		try {
			List<MailPo> rows = mapper.selectListByQuery(QueryWrapper.create());
			int[] result = new int[rows.size()];
			for (int i = 0; i < rows.size(); i++) {
				result[i] = rows.get(i).getMailUniqueId();
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.39fd601f311d"), e);
			return new int[0];
		}
	}

	/**
	 * 空值安全取整：与原 rs.getInt 的 NULL→0 语义一致。
	 * Null-safe int, matching the original rs.getInt NULL→0 semantics.
	 */
	private static int intValue(Integer value) {
		return value == null ? 0 : value;
	}
}

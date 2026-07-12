package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.StorageType;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 邮件系统 DAO 的 MySQL 8 实现，已修复连接泄漏。
 * MySQL 8 implementation of MailDAO with connection leak fixes.
 *
 * @author kosyachok
 */
@Slf4j
public class MailDAO extends com.aionemu.gameserver.dao.MailDAO {


    /** 查询玩家邮件（最近 100 封） / Select player mail (latest 100) */
    private static final String SELECT_MAIL_QUERY = "SELECT * FROM mail WHERE mail_recipient_id = ? ORDER BY recieved_time DESC LIMIT 100";
    /** 查询邮箱附件物品 / Select mailbox attachment items */
    private static final String SELECT_INVENTORY_QUERY = "SELECT * FROM inventory WHERE `item_owner` = ? AND `item_location` = 127";
    /** 查询是否存在未读邮件 / Check whether unread mail exists */
    private static final String SELECT_UNREAD_QUERY = "SELECT EXISTS(SELECT 1 FROM mail WHERE mail_recipient_id = ? AND unread = 1 LIMIT 1) as has_unread";
    /** 插入新邮件 / Insert a new mail letter */
    private static final String INSERT_MAIL_QUERY = "INSERT INTO `mail` (`mail_unique_id`, `mail_recipient_id`, `sender_name`, " + "`mail_title`, `mail_message`, `unread`, `attached_item_id`, `attached_kinah_count`, " + "`express`, `recieved_time`, `attached_ap_count`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    /** 更新邮件内容 / Update a mail letter */
    private static final String UPDATE_MAIL_QUERY = "UPDATE mail SET unread = ?, attached_item_id = ?, attached_kinah_count = ?, " + "`express` = ?, recieved_time = ?, attached_ap_count = ? WHERE mail_unique_id = ?";
    /** 删除邮件 / Delete a mail letter */
    private static final String DELETE_MAIL_QUERY = "DELETE FROM mail WHERE mail_unique_id = ?";
    /** 更新离线玩家邮箱信件计数 / Update offline mailbox letter counter */
    private static final String UPDATE_MAIL_COUNTER_QUERY = "UPDATE players SET mailbox_letters = ? WHERE name = ?";
    /** 查询已占用的邮件 ID / Select used mail unique ids */
    private static final String SELECT_USED_IDS_QUERY = "SELECT mail_unique_id FROM mail";

    /**
     * 加载玩家邮箱（含附件物品）。
     * Loads the player's mailbox including attached items.
     *
     * 玩家 / player
     * mailbox
     */
    @Override
    public Mailbox loadPlayerMailbox(Player player) {
        final Mailbox mailbox = new Mailbox(player);
        final int playerId = player.getObjectId();

        List<Item> mailboxItems = loadMailboxItems(playerId);

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_MAIL_QUERY)) {

            stmt.setInt(1, playerId);

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int mailUniqueId = rset.getInt("mail_unique_id");
                    int recipientId = rset.getInt("mail_recipient_id");
                    String senderName = rset.getString("sender_name");
                    String mailTitle = rset.getString("mail_title");
                    String mailMessage = rset.getString("mail_message");
                    int unread = rset.getInt("unread");
                    int attachedItemId = rset.getInt("attached_item_id");
                    long attachedKinahCount = rset.getLong("attached_kinah_count");
                    long attachedApCount = rset.getLong("attached_ap_count");
                    LetterType letterType = LetterType.getLetterTypeById(rset.getInt("express"));
                    Timestamp receivedTime = rset.getTimestamp("recieved_time");

                    Item attachedItem = null;
                    if (attachedItemId != 0) {
                        for (Item item : mailboxItems) {
                            if (item.getObjectId() == attachedItemId) {
                                if (item.getItemTemplate().isArmor() ||
                                    item.getItemTemplate().isWeapon()) {
                                    DAOManager.getDAO(ItemStoneListDAO.class).load(Collections.singletonList(item));
                                }
                                attachedItem = item;
                                break;
                            }
                        }
                    }

                    Letter letter = new Letter(mailUniqueId, recipientId, attachedItem, attachedKinahCount, attachedApCount, mailTitle, mailMessage, senderName, receivedTime, unread == 1, letterType);

                    letter.setPersistState(PersistentState.UPDATED);
                    mailbox.putLetterToMailbox(letter);
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.e6130c3e4ab9", playerId, e));
        }

        return mailbox;
    }

    /**
     * 检查玩家是否有未读邮件。
     * Checks whether the player has unread mail.
     *
     * player id
     *
     * @param playerId @return 是否有未读邮件 / whether unread mail exists
     */
    @Override
    public boolean haveUnread(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_UNREAD_QUERY)) {

            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt("has_unread") == 1;
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.fa6fe5701ebb", playerId, e));
        }

        return false;
    }

    /**
     * 加载邮箱附件物品列表。
     * Loads mailbox attachment items for the player.
     *
     * player id
     *
     * @param playerId @return 附件物品列表 / list of attachment items
     */
    private List<Item> loadMailboxItems(final int playerId) {
        final List<Item> mailboxItems = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_INVENTORY_QUERY)) {

            stmt.setInt(1, playerId);

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int itemUniqueId = rset.getInt("item_unique_id");
                    int itemId = rset.getInt("item_id");
                    long itemCount = rset.getLong("item_count");
                    int itemColor = rset.getInt("item_color");
                    int colorExpireTime = rset.getInt("color_expires");
                    String itemCreator = rset.getString("item_creator");
                    int expireTime = rset.getInt("expire_time");
                    int activationCount = rset.getInt("activation_count");
                    int isEquiped = rset.getInt("is_equiped");
                    int isSoulBound = rset.getInt("is_soul_bound");
                    int slot = rset.getInt("slot");
                    int enchant = rset.getInt("enchant");
                    int enchantBonus = rset.getInt("enchant_bonus");
                    int itemSkin = rset.getInt("item_skin");
                    int fusionedItem = rset.getInt("fusioned_item");
                    int optionalSocket = rset.getInt("optional_socket");
                    int optionalFusionSocket = rset.getInt("optional_fusion_socket");
                    int charge = rset.getInt("charge");
                    Integer randomNumber = rset.getInt("rnd_bonus");
                    int rndCount = rset.getInt("rnd_count");
                    int wrappingCount = rset.getInt("wrappable_count");
                    int isPacked = rset.getInt("is_packed");
                    int temperingLevel = rset.getInt("tempering_level");
                    int isTopped = rset.getInt("is_topped");
                    int strengthenSkill = rset.getInt("strengthen_skill");
                    int skinSkill = rset.getInt("skin_skill");
                    int isLunaReskin = rset.getInt("luna_reskin");
                    int reductionLevel = rset.getInt("reduction_level");
                    int unSeal = rset.getInt("is_seal");
                    boolean isEnhance = rset.getBoolean("isEnhance");
                    int enhanceSkillId = rset.getInt("enhanceSkillId");
                    int enhanceSkillEnchant = rset.getInt("enhanceSkillEnchant");

                    Item item = new Item(itemUniqueId, itemId, itemCount, itemColor, colorExpireTime, itemCreator, expireTime, activationCount, isEquiped == 1, isSoulBound == 1, slot, StorageType.MAILBOX.getId(), enchant, enchantBonus, itemSkin, fusionedItem, optionalSocket, optionalFusionSocket, charge, randomNumber, rndCount, wrappingCount, isPacked == 1, temperingLevel, isTopped == 1, strengthenSkill, skinSkill, isLunaReskin == 1, reductionLevel, unSeal, isEnhance, enhanceSkillId, enhanceSkillEnchant);

                    item.setPersistentState(PersistentState.UPDATED);
                    mailboxItems.add(item);
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.08b401a571e4", playerId, e));
        }

        return mailboxItems;
    }

    /**
     * 持久化玩家邮箱中全部信件。
     * Persists all letters currently in the player's mailbox.
     *
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
     *
     * timestamp
     * letter
     * whether successful
     */
    @Override
    public boolean storeLetter(Timestamp time, Letter letter) {
        boolean result = false;

        switch (letter.getLetterPersistentState()) {
            case NEW:
                result = saveLetter(time, letter);
                break;
            case UPDATE_REQUIRED:
                result = updateLetter(time, letter);
                break;
            default:
                return true;
        }

        if (result) {
            letter.setPersistState(PersistentState.UPDATED);
        }
        return result;
    }

    /**
     * 插入新信件。
     * Inserts a new letter.
     *
     * timestamp
     * letter
     * whether successful
     */
    private boolean saveLetter(final Timestamp time, final Letter letter) {
        int attachedItemId = 0;
        if (letter.getAttachedItem() != null) {
            attachedItemId = letter.getAttachedItem().getObjectId();
        }

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_MAIL_QUERY)) {

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

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            log.error(I18n.get("log.17b9caa7691c", letter.getRecipientId(), e));
            return false;
        }
    }

    /**
     * 更新已有信件。
     * Updates an existing letter.
     *
     * timestamp
     * letter
     * whether successful
     */
    private boolean updateLetter(final Timestamp time, final Letter letter) {
        int attachedItemId = 0;
        if (letter.getAttachedItem() != null) {
            attachedItemId = letter.getAttachedItem().getObjectId();
        }

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_MAIL_QUERY)) {

            stmt.setBoolean(1, letter.isUnread());
            stmt.setInt(2, attachedItemId);
            stmt.setLong(3, letter.getAttachedKinah());
            stmt.setInt(4, letter.getLetterType().getId());
            stmt.setTimestamp(5, time);
            stmt.setLong(6, letter.getAttachedAp());
            stmt.setInt(7, letter.getObjectId());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            log.error(I18n.get("log.f5a95e9e6233", letter.getRecipientId(), e));
            return false;
        }
    }

    /**
     * 删除指定信件。
     * Deletes the letter with the given id.
     *
     * letter id
     * whether successful
     */
    @Override
    public boolean deleteLetter(final int letterId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_MAIL_QUERY)) {

            stmt.setInt(1, letterId);
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            log.error(I18n.get("log.65c1132a74c3", letterId, e));
            return false;
        }
    }

    /**
     * 更新离线收件人的邮箱信件计数。
     * Updates the offline recipient's mailbox letter counter.
     *
     * @param recipientCommonData 收件人公共数据 / recipient common data
     */
    @Override
    public void updateOfflineMailCounter(final PlayerCommonData recipientCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_MAIL_COUNTER_QUERY)) {

            stmt.setInt(1, recipientCommonData.getMailboxLetters());
            stmt.setString(2, recipientCommonData.getName());
            stmt.executeUpdate();

        } catch (Exception e) {
            log.error(I18n.get("log.a4015981f3b5", recipientCommonData.getName(), e));
        }
    }

    /**
     * 返回邮件表中已占用的全部邮件 ID。
     * Returns all used mail unique ids from the mail table.
     *
     * 已占用 ID 数组；失败时返回空数组。
     * used id array, or empty array on failure.
     */
    @Override
    public int[] getUsedIDs() {
        List<Integer> ids = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_USED_IDS_QUERY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                ids.add(rs.getInt("mail_unique_id"));
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.39fd601f311d", e));
            return new int[0];
        }

        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
     *
     * @param databaseName 数据库名称 / database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}

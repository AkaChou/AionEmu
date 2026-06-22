package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author kosyachok
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8MailDAO extends MailDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8MailDAO.class);

    private static final String SELECT_MAIL_QUERY = "SELECT * FROM mail WHERE mail_recipient_id = ? ORDER BY recieved_time DESC LIMIT 100";
    private static final String SELECT_INVENTORY_QUERY = "SELECT * FROM inventory WHERE `item_owner` = ? AND `item_location` = 127";
    private static final String SELECT_UNREAD_QUERY = "SELECT EXISTS(SELECT 1 FROM mail WHERE mail_recipient_id = ? AND unread = 1 LIMIT 1) as has_unread";
    private static final String INSERT_MAIL_QUERY = "INSERT INTO `mail` (`mail_unique_id`, `mail_recipient_id`, `sender_name`, " + "`mail_title`, `mail_message`, `unread`, `attached_item_id`, `attached_kinah_count`, " + "`express`, `recieved_time`, `attached_ap_count`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_MAIL_QUERY = "UPDATE mail SET unread = ?, attached_item_id = ?, attached_kinah_count = ?, " + "`express` = ?, recieved_time = ?, attached_ap_count = ? WHERE mail_unique_id = ?";
    private static final String DELETE_MAIL_QUERY = "DELETE FROM mail WHERE mail_unique_id = ?";
    private static final String UPDATE_MAIL_COUNTER_QUERY = "UPDATE players SET mailbox_letters = ? WHERE name = ?";
    private static final String SELECT_USED_IDS_QUERY = "SELECT mail_unique_id FROM mail";

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
            log.error("Could not load mailbox for player: {}", playerId, e);
        }
        
        return mailbox;
    }

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
            log.error("Could not check unread mail for player: {}", playerId, e);
        }
        
        return false;
    }

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
            log.error("Could not load mailbox items for player: {}", playerId, e);
        }
        
        return mailboxItems;
    }

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
            log.error("Could not save mail for recipient: {}", letter.getRecipientId(), e);
            return false;
        }
    }

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
            log.error("Could not update mail for recipient: {}", letter.getRecipientId(), e);
            return false;
        }
    }

    @Override
    public boolean deleteLetter(final int letterId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_MAIL_QUERY)) {
            
            stmt.setInt(1, letterId);
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            log.error("Could not delete mail: {}", letterId, e);
            return false;
        }
    }

    @Override
    public void updateOfflineMailCounter(final PlayerCommonData recipientCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_MAIL_COUNTER_QUERY)) {
            
            stmt.setInt(1, recipientCommonData.getMailboxLetters());
            stmt.setString(2, recipientCommonData.getName());
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Could not update offline mail counter for player: {}", recipientCommonData.getName(), e);
        }
    }

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
            log.error("Can't get list of id's from mail table", e);
            return new int[0];
        }
        
        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
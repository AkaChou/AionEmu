package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerPetsDAO;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.pet.PetDopingBag;
import com.aionemu.gameserver.services.toypet.PetHungryLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author M@xx, xTz, Rolandas
 */
public class MySQL8PlayerPetsDAO extends PlayerPetsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerPetsDAO.class);

    @Override
    public void saveFeedStatus(Player player, int petId, int hungryLevel, int feedProgress, long reuseTime) {
        String query = "UPDATE player_pets SET hungry_level = ?, feed_progress = ?, reuse_time = ? WHERE player_id = ? AND pet_id = ?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, hungryLevel);
            stmt.setInt(2, feedProgress);
            stmt.setLong(3, reuseTime);
            stmt.setInt(4, player.getObjectId());
            stmt.setInt(5, petId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error update pet #{}", petId, e);
        }
    }

    @Override
    public void saveDopingBag(Player player, int petId, PetDopingBag bag) {
        String query = "UPDATE player_pets SET dopings = ? WHERE player_id = ? AND pet_id = ?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            StringBuilder itemIds = new StringBuilder();
            itemIds.append(bag.getFoodItem()).append(",").append(bag.getDrinkItem());
            
            for (int itemId : bag.getScrollsUsed()) {
                itemIds.append(",").append(itemId);
            }
            
            stmt.setString(1, itemIds.toString());
            stmt.setInt(2, player.getObjectId());
            stmt.setInt(3, petId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error update doping for pet #{}", petId, e);
        }
    }

    @Override
    public void setTime(Player player, int petId, long time) {
        String query = "UPDATE player_pets SET reuse_time = ? WHERE player_id = ? AND pet_id = ?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setLong(1, time);
            stmt.setInt(2, player.getObjectId());
            stmt.setInt(3, petId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error update pet #{}", petId, e);
        }
    }

    @Override
    public void insertPlayerPet(PetCommonData petCommonData) {
        String query = "INSERT INTO player_pets(player_id, pet_id, decoration, name, despawn_time, expire_time) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, petCommonData.getMasterObjectId());
            stmt.setInt(2, petCommonData.getPetId());
            stmt.setInt(3, petCommonData.getDecoration());
            stmt.setString(4, petCommonData.getName());
            stmt.setTimestamp(5, petCommonData.getDespawnTime());
            stmt.setInt(6, petCommonData.getExpireTime());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error inserting new pet #[{}] - {}", 
                petCommonData.getPetId(), petCommonData.getName(), e);
        }
    }

    @Override
    public void removePlayerPet(Player player, int petId) {
        String query = "DELETE FROM player_pets WHERE player_id = ? AND pet_id = ?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, petId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error removing pet #{}", petId, e);
        }
    }

    @Override
    public List<PetCommonData> getPlayerPets(Player player) {
        List<PetCommonData> pets = new ArrayList<>();
        String query = "SELECT * FROM player_pets WHERE player_id = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PetCommonData petCommonData = new PetCommonData(
                        rs.getInt("pet_id"), 
                        player.getObjectId(), 
                        rs.getInt("expire_time")
                    );
                    
                    petCommonData.setName(rs.getString("name"));
                    petCommonData.setDecoration(rs.getInt("decoration"));
                    
                    if (petCommonData.getFeedProgress() != null) {
                        petCommonData.getFeedProgress().setHungryLevel(
                            PetHungryLevel.fromId(rs.getInt("hungry_level"))
                        );
                        petCommonData.getFeedProgress().setData(rs.getInt("feed_progress"));
                        petCommonData.setCurentTime(rs.getLong("reuse_time"));
                    }
                    
                    if (petCommonData.getDopingBag() != null) {
                        String dopings = rs.getString("dopings");
                        if (dopings != null) {
                            String[] ids = dopings.split(",");
                            for (int i = 0; i < ids.length; i++) {
                                if (!ids[i].isEmpty()) {
                                    petCommonData.getDopingBag().setItem(Integer.parseInt(ids[i]), i);
                                }
                            }
                        }
                    }
                    
                    petCommonData.setBirthday(rs.getTimestamp("birthday"));
                    
                    if (petCommonData.getTime() != 0) {
                        petCommonData.setIsFeedingTime(false);
                        petCommonData.setReFoodTime(petCommonData.getTime());
                    }
                    
                    petCommonData.setStartMoodTime(rs.getLong("mood_started"));
                    petCommonData.setShuggleCounter(rs.getInt("counter"));
                    petCommonData.setMoodCdStarted(rs.getLong("mood_cd_started"));
                    petCommonData.setGiftCdStarted(rs.getLong("gift_cd_started"));
                    
                    Timestamp ts = rs.getTimestamp("despawn_time");
                    if (ts == null) {
                        ts = new Timestamp(System.currentTimeMillis());
                    }
                    petCommonData.setDespawnTime(ts);
                    
                    pets.add(petCommonData);
                }
            }
        } catch (SQLException e) {
            log.error("Error getting pets for {}", player.getObjectId(), e);
        }
        return pets;
    }

    @Override
    public void updatePetName(PetCommonData petCommonData) {
        String query = "UPDATE player_pets SET name = ? WHERE player_id = ? AND pet_id = ?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setString(1, petCommonData.getName());
            stmt.setInt(2, petCommonData.getMasterObjectId());
            stmt.setInt(3, petCommonData.getPetId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error update pet #{}", petCommonData.getPetId(), e);
        }
    }

    @Override
    public boolean savePetMoodData(PetCommonData petCommonData) {
        String query = "UPDATE player_pets SET mood_started = ?, counter = ?, mood_cd_started = ?, gift_cd_started = ?, despawn_time = ? WHERE player_id = ? AND pet_id = ?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setLong(1, petCommonData.getMoodStartTime());
            stmt.setInt(2, petCommonData.getShuggleCounter());
            stmt.setLong(3, petCommonData.getMoodCdStarted());
            stmt.setLong(4, petCommonData.getGiftCdStarted());
            stmt.setTimestamp(5, petCommonData.getDespawnTime());
            stmt.setInt(6, petCommonData.getMasterObjectId());
            stmt.setInt(7, petCommonData.getPetId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error updating mood for pet #{}", petCommonData.getPetId(), e);
            return false;
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
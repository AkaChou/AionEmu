package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerPetsDAO;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.pet.PetDopingBag;
import com.aionemu.gameserver.services.toypet.PetHungryLevel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家宠物数据 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerPetsDAO.
 *
 * @author M@xx, xTz, Rolandas
 */
@Slf4j
public class MySQL8PlayerPetsDAO extends PlayerPetsDAO {

    /**
     * 保存宠物喂养状态。
     * Saves the pet feed status.
     *
     * 玩家 / player
     * pet id
     * hungry level
     * feed progress
     * reuse time
     */
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
            log.error(I18n.get("log.8faeec608d56", petId, e));
        }
    }

    /**
     * 保存宠物增益包（食品/饮料/卷轴）。
     * Saves the pet doping bag (food/drink/scrolls).
     *
     * 玩家 / player
     * pet id
     * doping bag
     */
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
            log.error(I18n.get("log.b3af66365ffc", petId, e));
        }
    }

    /**
     * 设置宠物复用时间。
     * Sets the pet reuse time.
     *
     * 玩家 / player
     * pet id
     * @param time 复用时间 / reuse time
     */
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
            log.error(I18n.get("log.8faeec608d56", petId, e));
        }
    }

    /**
     * 插入玩家宠物记录。
     * Inserts a player pet record.
     *
     * @param petCommonData 宠物通用数据 / pet common data
     */
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
            log.error(I18n.get("log.470c078aecdc", petCommonData.getPetId(), petCommonData.getName(), e));
        }
    }

    /**
     * 删除玩家宠物记录。
     * Removes a player pet record.
     *
     * 玩家 / player
     * pet id
     */
    @Override
    public void removePlayerPet(Player player, int petId) {
        String query = "DELETE FROM player_pets WHERE player_id = ? AND pet_id = ?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, petId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.c0204a470d98", petId, e));
        }
    }

    /**
     * 加载玩家全部宠物数据。
     * Loads all pets for the player.
     *
     * @param player 玩家 / player
     * @return 宠物数据列表 / list of pet common data
     */
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
            log.error(I18n.get("log.2e256611ad4a", player.getObjectId(), e));
        }
        return pets;
    }

    /**
     * 更新宠物名称。
     * Updates the pet name.
     *
     * @param petCommonData 宠物通用数据 / pet common data
     */
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
            log.error(I18n.get("log.8faeec608d56", petCommonData.getPetId(), e));
        }
    }

    /**
     * 保存宠物心情相关数据。
     * Saves pet mood related data.
     *
     * @param petCommonData 宠物通用数据 / pet common data
     * whether successful
     */
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
            log.error(I18n.get("log.44d7671006ae", petCommonData.getPetId(), e));
            return false;
        }
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
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}

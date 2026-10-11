package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.pet.PetDopingBag;
import com.aionemu.gameserver.services.toypet.PetHungryLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家宠物数据 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerPetsDAO using JdbcClient.
 * @author M@xx, xTz, Rolandas
 */
@Slf4j
@Repository
public class PlayerPetsDAO extends com.aionemu.gameserver.dao.PlayerPetsDAO {

    private static final String SAVE_FEED_STATUS_QUERY = "UPDATE player_pets SET hungry_level = ?, feed_progress = ?, reuse_time = ? WHERE player_id = ? AND pet_id = ?";
    private static final String SAVE_DOPING_BAG_QUERY = "UPDATE player_pets SET dopings = ? WHERE player_id = ? AND pet_id = ?";
    private static final String SET_TIME_QUERY = "UPDATE player_pets SET reuse_time = ? WHERE player_id = ? AND pet_id = ?";
    private static final String INSERT_QUERY = "INSERT INTO player_pets(player_id, pet_id, decoration, name, despawn_time, expire_time) VALUES(?, ?, ?, ?, ?, ?)";
    private static final String REMOVE_QUERY = "DELETE FROM player_pets WHERE player_id = ? AND pet_id = ?";
    private static final String SELECT_QUERY = "SELECT * FROM player_pets WHERE player_id = ?";
    private static final String UPDATE_NAME_QUERY = "UPDATE player_pets SET name = ? WHERE player_id = ? AND pet_id = ?";
    private static final String SAVE_MOOD_QUERY = "UPDATE player_pets SET mood_started = ?, counter = ?, mood_cd_started = ?, gift_cd_started = ?, despawn_time = ? WHERE player_id = ? AND pet_id = ?";

    private final JdbcClient jdbcClient;

    public PlayerPetsDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public PlayerPetsDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 保存宠物喂养状态。
     * Saves the pet feed status.
     * @param player 玩家 / player
     * @param petId 宠物 ID / pet id
     * @param hungryLevel hungry level
     * @param feedProgress feed progress
     * @param reuseTime 复用时间 / reuse time
     */
    @Override
    public void saveFeedStatus(Player player, int petId, int hungryLevel, int feedProgress, long reuseTime) {
        try {
            jdbcClient.sql(SAVE_FEED_STATUS_QUERY)
                .param(hungryLevel)
                .param(feedProgress)
                .param(reuseTime)
                .param(player.getObjectId())
                .param(petId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.8faeec608d56", petId), e);
        }
    }

    /**
     * 保存宠物增益包（食品/饮料/卷轴）。
     * Saves the pet doping bag (food/drink/scrolls).
     * @param player 玩家 / player
     * @param petId 宠物 ID / pet id
     * @param bag doping bag
     */
    @Override
    public void saveDopingBag(Player player, int petId, PetDopingBag bag) {
        try {
            StringBuilder itemIds = new StringBuilder();
            itemIds.append(bag.getFoodItem()).append(",").append(bag.getDrinkItem());

            for (int itemId : bag.getScrollsUsed()) {
                itemIds.append(",").append(itemId);
            }

            jdbcClient.sql(SAVE_DOPING_BAG_QUERY)
                .param(itemIds.toString())
                .param(player.getObjectId())
                .param(petId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.b3af66365ffc", petId), e);
        }
    }

    /**
     * 设置宠物复用时间。
     * Sets the pet reuse time.
     * @param player 玩家 / player
     * @param petId 宠物 ID / pet id
     * @param time 复用时间 / reuse time
     */
    @Override
    public void setTime(Player player, int petId, long time) {
        try {
            jdbcClient.sql(SET_TIME_QUERY)
                .param(time)
                .param(player.getObjectId())
                .param(petId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.8faeec608d56", petId), e);
        }
    }

    /**
     * 插入玩家宠物记录。
     * Inserts a player pet record.
     * @param petCommonData 宠物通用数据 / pet common data
     */
    @Override
    public void insertPlayerPet(PetCommonData petCommonData) {
        try {
            jdbcClient.sql(INSERT_QUERY)
                .param(petCommonData.getMasterObjectId())
                .param(petCommonData.getPetId())
                .param(petCommonData.getDecoration())
                .param(petCommonData.getName())
                .param(petCommonData.getDespawnTime())
                .param(petCommonData.getExpireTime())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.470c078aecdc", petCommonData.getPetId(), petCommonData.getName()), e);
        }
    }

    /**
     * 删除玩家宠物记录。
     * Removes a player pet record.
     * @param player 玩家 / player
     * @param petId 宠物 ID / pet id
     */
    @Override
    public void removePlayerPet(Player player, int petId) {
        try {
            jdbcClient.sql(REMOVE_QUERY)
                .param(player.getObjectId())
                .param(petId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.c0204a470d98", petId), e);
        }
    }

    /**
     * 加载玩家全部宠物数据。
     * Loads all pets for the player.
     * @param player 玩家 / player
     * @return 宠物数据列表 / list of pet common data
     */
    @Override
    public List<PetCommonData> getPlayerPets(Player player) {
        List<PetCommonData> pets = new ArrayList<>();

        try {
            pets = jdbcClient.sql(SELECT_QUERY)
                .param(player.getObjectId())
                .query((rs, _) -> {
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

                    return petCommonData;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.2e256611ad4a", player.getObjectId()), e);
        }
        return pets;
    }

    /**
     * 更新宠物名称。
     * Updates the pet name.
     * @param petCommonData 宠物通用数据 / pet common data
     */
    @Override
    public void updatePetName(PetCommonData petCommonData) {
        try {
            jdbcClient.sql(UPDATE_NAME_QUERY)
                .param(petCommonData.getName())
                .param(petCommonData.getMasterObjectId())
                .param(petCommonData.getPetId())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.8faeec608d56", petCommonData.getPetId()), e);
        }
    }

    /**
     * 保存宠物心情相关数据。
     * Saves pet mood related data.
     * @param petCommonData 宠物通用数据 / pet common data
     * @return 是否成功 / whether successful
     */
    @Override
    public void savePetMoodData(PetCommonData petCommonData) {
        try {
            jdbcClient.sql(SAVE_MOOD_QUERY)
                .param(petCommonData.getMoodStartTime())
                .param(petCommonData.getShuggleCounter())
                .param(petCommonData.getMoodCdStarted())
                .param(petCommonData.getGiftCdStarted())
                .param(petCommonData.getDespawnTime())
                .param(petCommonData.getMasterObjectId())
                .param(petCommonData.getPetId())
                .update();
            return;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.44d7671006ae", petCommonData.getPetId()), e);
            return;
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
     * @param databaseName 数据库名 / database name
     * @param majorVersion 主版本 / major version
     * @param minorVersion 次版本 / minor version
     * @return 是否支持 / whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}

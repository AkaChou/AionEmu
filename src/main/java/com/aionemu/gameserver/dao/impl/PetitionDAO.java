package com.aionemu.gameserver.dao.impl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.PetitionStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家请愿/客服工单 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PetitionDAO using JdbcClient.
 * @author zdead
 */
@Slf4j
public class PetitionDAO extends com.aionemu.gameserver.dao.PetitionDAO {

    private static final String NEXT_ID_QUERY = "SELECT COALESCE(MAX(id), 0) + 1 as nextid FROM petitions";
    private static final String SELECT_BY_ID_QUERY = "SELECT * FROM petitions WHERE id = ?";
    private static final String SELECT_ALL_QUERY = "SELECT * FROM petitions WHERE status IN ('PENDING', 'IN_PROGRESS') ORDER BY id ASC";
    private static final String DELETE_QUERY = "DELETE FROM petitions WHERE player_id = ? AND status IN ('PENDING', 'IN_PROGRESS')";
    private static final String INSERT_QUERY = "INSERT INTO petitions (id, player_id, type, title, message, add_data, time, status) VALUES(?,?,?,?,?,?,?,?)";
    private static final String SET_REPLIED_QUERY = "UPDATE petitions SET status = 'REPLIED' WHERE id = ?";

    private final JdbcClient jdbcClient;

    public PetitionDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    public PetitionDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 获取下一个可用的请愿工单 ID。
     * Returns the next available petition id.
     * @return 下一个工单 ID / next petition id
     */
    @Override
    public synchronized int getNextAvailableId() {
        try {
            Integer nextId = jdbcClient.sql(NEXT_ID_QUERY)
                .query(Integer.class)
                .optional()
                .orElse(0);
            return nextId != null ? nextId : 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.fb167a91b87e"), e);
            return 0;
        }
    }

    /**
     * 按 ID 查询请愿工单。
     * Loads a petition by its id.
     * @param petitionId 申诉 ID / petition id
     * @return 请愿工单，未找到时返回 null / petition, or null if not found
     */
    @Override
    public Petition getPetitionById(int petitionId) {
        try {
            return jdbcClient.sql(SELECT_BY_ID_QUERY)
                .param(petitionId)
                .query((rs, rowNum) -> {
                    PetitionStatus status = getPetitionStatus(rs.getString("status"));
                    return new Petition(
                        rs.getInt("id"),
                        rs.getInt("player_id"),
                        rs.getInt("type"),
                        rs.getString("title"),
                        rs.getString("message"),
                        rs.getString("add_data"),
                        status.getElementId()
                    );
                })
                .optional()
                .orElse(null);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.72ffd18827d7", petitionId), e);
            return null;
        }
    }

    /**
     * 查询所有待处理/处理中的请愿工单。
     * Loads all pending or in-progress petitions.
     * @return 请愿工单集合 / set of petitions
     */
    @Override
    public Set<Petition> getPetitions() {
        Set<Petition> results = new HashSet<>();
        try {
            jdbcClient.sql(SELECT_ALL_QUERY)
                .query((rs, rowNum) -> {
                    PetitionStatus status = getPetitionStatus(rs.getString("status"));
                    return new Petition(
                        rs.getInt("id"),
                        rs.getInt("player_id"),
                        rs.getInt("type"),
                        rs.getString("title"),
                        rs.getString("message"),
                        rs.getString("add_data"),
                        status.getElementId()
                    );
                })
                .list()
                .forEach(results::add);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.70ecf26917fd"), e);
        }
        return results;
    }

    /**
     * 删除玩家当前待处理/处理中的请愿工单。
     * Deletes the player's pending or in-progress petitions.
     * @param playerObjId 玩家对象 ID / player object id
     */
    @Override
    public void deletePetition(int playerObjId) {
        try {
            jdbcClient.sql(DELETE_QUERY)
                .param(playerObjId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.eb43a2da10f3"), e);
        }
    }

    /**
     * 插入新的请愿工单。
     * Inserts a new petition.
     * @param petition 申诉 / petition
     */
    @Override
    public void insertPetition(Petition petition) {
        try {
            jdbcClient.sql(INSERT_QUERY)
                .param(petition.getPetitionId())
                .param(petition.getPlayerObjId())
                .param(petition.getPetitionType().getElementId())
                .param(petition.getTitle())
                .param(petition.getContentText())
                .param(petition.getAdditionalData())
                .param(System.currentTimeMillis() / 1000)
                .param(petition.getStatus().toString())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.d40ee1b26acb"), e);
        }
    }

    /**
     * 将请愿工单标记为已回复。
     * Marks a petition as replied.
     * @param petitionId 申诉 ID / petition id
     */
    @Override
    public void setReplied(int petitionId) {
        try {
            jdbcClient.sql(SET_REPLIED_QUERY)
                .param(petitionId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.77073357738f"), e);
        }
    }

    /**
     * 将状态字符串解析为请愿状态枚举。
     * Parses a status string into a PetitionStatus enum value.
     * @param statusValue 状态字符串 / status string
     * @return 申诉状态 / petition status
     */
    private PetitionStatus getPetitionStatus(String statusValue) {
        if ("PENDING".equals(statusValue)) {
            return PetitionStatus.PENDING;
        } else if ("IN_PROGRESS".equals(statusValue)) {
            return PetitionStatus.IN_PROGRESS;
        } else {
            return PetitionStatus.PENDING;
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

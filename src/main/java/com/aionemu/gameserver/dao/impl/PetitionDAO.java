package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.PetitionStatus;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * 玩家请愿/客服工单 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PetitionDAO.
 *
 * @author zdead
 */
@Slf4j
public class PetitionDAO extends com.aionemu.gameserver.dao.PetitionDAO {


    /**
     * 获取下一个可用的请愿工单 ID。
     * Returns the next available petition id.
     *
     * @return 下一个工单 ID / next petition id
     */
    @Override
    public synchronized int getNextAvailableId() {
        String query = "SELECT COALESCE(MAX(id), 0) + 1 as nextid FROM petitions";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query);
             ResultSet rset = stmt.executeQuery()) {

            if (rset.next()) {
                return rset.getInt("nextid");
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.fb167a91b87e", e), e);
        }
        return 0;
    }

    /**
     * 按 ID 查询请愿工单。
     * Loads a petition by its id.
     *
     * petition id
     *
     * @param petitionId
     * @return 请愿工单，未找到时返回 null / petition, or null if not found
     */
    @Override
    public Petition getPetitionById(int petitionId) {
        String query = "SELECT * FROM petitions WHERE id = ?";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setInt(1, petitionId);
            try (ResultSet rset = stmt.executeQuery()) {
                if (!rset.next()) {
                    return null;
                }

                PetitionStatus status = getPetitionStatus(rset.getString("status"));
                return new Petition(
                    rset.getInt("id"),
                    rset.getInt("player_id"),
                    rset.getInt("type"),
                    rset.getString("title"),
                    rset.getString("message"),
                    rset.getString("add_data"),
                    status.getElementId()
                );
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.72ffd18827d7", petitionId, e), e);
        }
        return null;
    }

    /**
     * 查询所有待处理/处理中的请愿工单。
     * Loads all pending or in-progress petitions.
     *
     * @return 请愿工单集合 / set of petitions
     */
    @Override
    public Set<Petition> getPetitions() {
        String query = "SELECT * FROM petitions WHERE status IN ('PENDING', 'IN_PROGRESS') ORDER BY id ASC";
        Set<Petition> results = new HashSet<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query);
             ResultSet rset = stmt.executeQuery()) {

            while (rset.next()) {
                PetitionStatus status = getPetitionStatus(rset.getString("status"));
                Petition p = new Petition(
                    rset.getInt("id"),
                    rset.getInt("player_id"),
                    rset.getInt("type"),
                    rset.getString("title"),
                    rset.getString("message"),
                    rset.getString("add_data"),
                    status.getElementId()
                );
                results.add(p);
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.70ecf26917fd", e), e);
            // 表缺失/库未初始化时返回空集，避免启动 NPE。
            return results;
        }
        return results;
    }

    /**
     * 删除玩家当前待处理/处理中的请愿工单。
     * Deletes the player's pending or in-progress petitions.
     *
     * player object id
     */
    @Override
    public void deletePetition(int playerObjId) {
        String query = "DELETE FROM petitions WHERE player_id = ? AND status IN ('PENDING', 'IN_PROGRESS')";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setInt(1, playerObjId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.eb43a2da10f3", e), e);
        }
    }

    /**
     * 插入新的请愿工单。
     * Inserts a new petition.
     *
     * petition
     */
    @Override
    public void insertPetition(Petition petition) {
        String query = "INSERT INTO petitions (id, player_id, type, title, message, add_data, time, status) VALUES(?,?,?,?,?,?,?,?)";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setInt(1, petition.getPetitionId());
            stmt.setInt(2, petition.getPlayerObjId());
            stmt.setInt(3, petition.getPetitionType().getElementId());
            stmt.setString(4, petition.getTitle());
            stmt.setString(5, petition.getContentText());
            stmt.setString(6, petition.getAdditionalData());
            stmt.setLong(7, System.currentTimeMillis() / 1000);
            stmt.setString(8, petition.getStatus().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.d40ee1b26acb", e), e);
        }
    }

    /**
     * 将请愿工单标记为已回复。
     * Marks a petition as replied.
     *
     * petition id
     */
    @Override
    public void setReplied(int petitionId) {
        String query = "UPDATE petitions SET status = 'REPLIED' WHERE id = ?";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setInt(1, petitionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.77073357738f", e), e);
        }
    }

    /**
     * 将状态字符串解析为请愿状态枚举。
     * Parses a status string into a PetitionStatus enum value.
     *
     * @param statusValue 状态字符串 / status string
     * petition status
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

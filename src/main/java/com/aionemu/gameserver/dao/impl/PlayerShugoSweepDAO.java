package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSweep;
import java.sql.*;

/**
 * 玩家修勾扫荡 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerShugoSweepDAO.
 *
 * Created by Wnkrz on 24/10/2017.
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
public class PlayerShugoSweepDAO extends com.aionemu.gameserver.dao.PlayerShugoSweepDAO {


    /** 插入扫荡记录 SQL / Insert sweep record SQL */
    private static final String ADD_QUERY = "INSERT INTO `player_shugo_sweep` (`player_id`, `free_dice`, `sweep_step`, `board_id`) VALUES (?,?,?,?)";
    /** 查询扫荡 SQL / Select sweep SQL */
    private static final String SELECT_QUERY = "SELECT * FROM `player_shugo_sweep` WHERE `player_id`=?";
    /**
	 * 删除全部术古清扫记录。 / Delete all Shugo Sweep records.
	 */
    private static final String DELETE_QUERY = "DELETE FROM `player_shugo_sweep`";
    /** 更新扫荡 SQL / Update sweep SQL */
    private static final String UPDATE_QUERY = "UPDATE player_shugo_sweep SET `free_dice`=?, `sweep_step`=?, `board_id`=? WHERE `player_id`=?";

    /**
     * 加载玩家修勾扫荡数据。
     * Loads player Shugo sweep data.
     *
     * @param player 玩家 / player
     */
    @Override
    public void load(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    int dice = rset.getInt("free_dice");
                    int step = rset.getInt("sweep_step");
                    int boardId = rset.getInt("board_id");

                    PlayerSweep ps = new PlayerSweep(step, dice, boardId);
                    ps.setPersistentState(PersistentState.UPDATED);
                    player.setPlayerShugoSweep(ps);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.400851d054d8", player.getObjectId(), e));
        }
    }

    /**
     * 新增扫荡记录。
     * Adds a sweep record.
     *
     * @param playerId 玩家 ID / player id
     * @param dice 免费骰子 / free dice
     * @param step 扫荡步数 / sweep step
     * @param boardId 公告板 ID / board id
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean add(final int playerId, final int dice, final int step, final int boardId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(ADD_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, dice);
            stmt.setInt(3, step);
            stmt.setInt(4, boardId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.81d2b7934965", playerId, e));
            return false;
        }
    }

    /**
     * 清空全部扫荡记录。
     * Deletes all sweep records.
     *
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean delete() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.b01e2acd651b", e));
            return false;
        }
    }

    /**
     * 持久化玩家扫荡数据。
     * Stores player sweep data.
     *
     * @param player 玩家 / player
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean store(Player player) {
        boolean success = false;

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            PlayerSweep bind = player.getPlayerShugoSweep();
            if (bind != null) {
                switch (bind.getPersistentState()) {
                    case UPDATE_REQUIRED:
                    case NEW:
                        success = updatePlayerSweep(con, player);
                        log.debug("DB updated for player {}", player.getObjectId());
                        break;
                    default:
                        success = true;
                        break;
                }
                if (success) {
                    bind.setPersistentState(PersistentState.UPDATED);
                }
            }
            con.commit();
        } catch (SQLException e) {
            log.error(I18n.get("log.3330e011e639", player.getObjectId(), e));
        }
        return success;
    }

    /**
     * 更新玩家扫荡记录。
     * Updates player sweep record.
     *
     * @param con 数据库连接 / database connection
     * @param player 玩家 / player
     * @return 是否成功 / whether succeeded
     */
    private boolean updatePlayerSweep(Connection con, Player player) {
        try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            PlayerSweep lr = player.getPlayerShugoSweep();
            if (lr == null) {
                return false;
            }

            stmt.setInt(1, lr.getFreeDice());
            stmt.setInt(2, lr.getStep());
            stmt.setInt(3, lr.getBoardId());
            stmt.setInt(4, player.getObjectId());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.ee22970ca710", player.getObjectId(), e));
            return false;
        }
    }

    /**
     * 按对象 ID 设置扫荡数据。
     * Sets sweep data by object id.
     *
     * @param obj 玩家对象 ID / player object id
     * @param freeDice free dice
     * @param step 扫荡步数 / sweep step
     * @param boardId 公告板 ID / board id
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean setShugoSweepByObjId(int obj, final int freeDice, final int step, int boardId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, freeDice);
            stmt.setInt(2, step);
            stmt.setInt(3, boardId);
            stmt.setInt(4, obj);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.88e530b6f3ac", obj, e));
            return false;
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
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

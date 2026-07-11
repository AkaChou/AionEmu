package com.aionemu.loginserver.dao;

import java.util.List;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferTask;

/**
 * 角色跨服转移任务数据访问抽象层。
 * DAO that manages player transfer tasks.
 *
 * @author KID
 */
public abstract class PlayerTransferDAO implements DAO {

    /**
     * 查询尚未处理的转移任务。
     * Loads new (pending) player transfer tasks.
     *
     * @return 待处理任务列表 / List of new tasks
     */
    public abstract List<PlayerTransferTask> getNew();

    /**
     * 更新转移任务状态与备注。
     * Updates a player transfer task status and comment.
     *
     * Task to update
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean update(PlayerTransferTask task);

    /**
     * 返回实现唯一类名标识。
     * Returns unique class name for all implementations.
     *
     * Fully qualified class name
     */
    @Override
    public final String getClassName() {
        return PlayerTransferDAO.class.getName();
    }
}

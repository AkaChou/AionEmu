package com.aionemu.loginserver.dao;

import com.aionemu.commons.database.dao.DAO;

/**
 * 游戏服在线统计数据访问抽象层。
 * DAO that manages game server online statistics.
 */
public abstract class SvStatsDAO implements DAO {

    /**
     * 更新指定服务器为在线状态及人数。
     * Updates a server as online with current and max players.
     *
     * Server id
     * Status code
     * Current players
     * @param max 最大人数 / Max players
     */
    public abstract void update_SvStats_Online(int server, int status, int current, int max);

    /**
     * 更新指定服务器为离线状态及人数。
     * Updates a server as offline with current players.
     *
     * Server id
     * Status code
     * Current players
     */
    public abstract void update_SvStats_Offline(int server, int status, int current);

    /**
     * 将全部服务器标记为离线。
     * Marks all servers offline with the given status and current count.
     *
     * Status code
     * Current players
     */
    public abstract void update_SvStats_All_Offline(int status, int current);

    /**
     * 返回实现唯一类名标识。
     * Returns unique class name for all implementations.
     *
     * Fully qualified class name
     */
    @Override
    public final String getClassName() {
        return SvStatsDAO.class.getName();
    }
}

package com.aionemu.loginserver.dao;

import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.loginserver.GameServerInfo;

/**
 * 游戏服注册信息数据访问抽象层。
 * DAO that manages game servers.
 *
 * @author -Nemesiss-
 */
public abstract class GameServersDAO implements DAO {

    /**
     * 加载全部游戏服配置。
     * Returns all game servers from database.
     *
     * @return 服务器 ID → 游戏服信息 / Map of server id to GameServerInfo
     */
    public abstract Map<Byte, GameServerInfo> getAllGameServers();

    /**
     * 返回实现唯一类名标识。
     * Returns unique class name for all implementations.
     *
     * Fully qualified class name
     */
    @Override
    public final String getClassName() {
        return GameServersDAO.class.getName();
    }
}

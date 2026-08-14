package com.aionemu.chatserver.service;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.network.gameserver.GsAuthResponse;

/**
 * 游戏服接入认证服务：处理游戏服注册、口令校验与离线状态。
 * Game-server auth service: handles game-server registration, password check, and offline state.
 *
 * @author ATracer, KID
 */
@Slf4j
public class GameServerService {


    /**
     * 获取单例（已废弃，迁移至 Boot 后请使用注入）。
     * Return the singleton (deprecated; prefer injection after Boot migration).
     *
     * @return 单例实例 / singleton instance
     * @deprecated boot-migration
     */
    @Deprecated(since = "boot-migration")
    public static GameServerService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static byte GAMESERVER_ID;
    private boolean isOnline = false;

    /**
     * 注册游戏服；已在线则拒绝重复注册。
     * Register a game server; reject when one is already online.
     *
     * @param gameServerId 游戏服 ID / game server id
     * @param defaultAddress 默认地址字节 / default address bytes
     * @param password 接入口令 / access password
     * @return 认证响应 / auth response
     */
    public GsAuthResponse registerGameServer(byte gameServerId, byte[] defaultAddress, String password) {
        GAMESERVER_ID = gameServerId;
        if (isOnline) {
            return GsAuthResponse.ALREADY_REGISTERED;
        }

        return passwordConfigAuth(password);
    }

    /**
     * 按配置口令校验并标记在线。
     * Authenticate against the configured password and mark online.
     *
     * @param password 接入口令 / access password
     * @return 认证响应 / auth response
     */
    private GsAuthResponse passwordConfigAuth(String password) {
        if (password.equals(Config.GAME_SERVER_PASSWORD)) {
            isOnline = true;
            return GsAuthResponse.AUTHED;
        }

        log.warn(I18n.get("log.b384a8e5934b", GAMESERVER_ID));
        return GsAuthResponse.NOT_AUTHED;
    }

    /**
     * 将游戏服标记为离线。
     * Mark the game server as offline.
     */
    public void setOffline() {
        log.info(I18n.get("log.3b0e82fbd42f", GAMESERVER_ID));
        isOnline = false;
    }

    /**
     * 单例持有者。
     * Singleton holder.
     */
    private static final class SingletonHolder {

        private static final GameServerService INSTANCE = new GameServerService();
    }
}

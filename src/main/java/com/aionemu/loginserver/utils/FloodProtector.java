package com.aionemu.loginserver.utils;


import com.aionemu.boot.i18n.I18n;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.service.LoginProtectionServices;

/**
 * 连接洪泛防护：限制同一 IP 过快重连，必要时写入封禁。
 * Connection flood protector: limits too-fast reconnects from the same IP and may ban it.
 *
 * @author Mr. Poke
 */
@Slf4j(topic = "com.aionemu.loginserver.network.aion.clientpackets.CM_LOGIN")
public class FloodProtector {

    private final Map<String, Long> flood = new ConcurrentHashMap<>();
    private final Map<String, Long> ban = new ConcurrentHashMap<>();

    /**
     * 获取单例实例（已弃用，请走 boot 注入）。
     * Returns the singleton instance (deprecated; prefer boot injection).
     *
     * @return 单例实例 / singleton instance
     */
    @Deprecated(since = "boot-migration")
    public static final FloodProtector getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 旧版 IP 洪泛检测：过快重连则写入 BannedIp 表（已弃用）。
     * Legacy IP flood check: bans the IP via BannedIp service when reconnects are too fast (deprecated).
     *
     * @param ip 客户端 IP / client IP
     * @return 该 IP 是否已被或应被封禁 / whether the IP is or should be banned
     */
    @Deprecated
    public boolean addIp_nn(String ip) {
        Long time = flood.get(ip);
        if (time == null || System.currentTimeMillis() - time > Config.FAST_RECONNECTION_TIME) {
            flood.put(ip, System.currentTimeMillis());
            return false;
        }
        Timestamp newTime = new Timestamp(System.currentTimeMillis() + Config.WRONG_LOGIN_BAN_TIME * 60000);
        if (!LoginProtectionServices.bannedIpService().isBanned(ip)) {
            log.info(I18n.get("log.63a8519d9835", ip, Config.WRONG_LOGIN_BAN_TIME));
            return LoginProtectionServices.bannedIpService().banIp(ip, newTime);
        }
        // 此种情况下该 IP 已被封禁。 / in this case this ip is already banned

        return true;
    }

    /**
     * 判断 IP 是否重连过快；过快则加入内存临时封禁。
     * Returns whether the IP reconnects too fast; if so, places it into an in-memory temporary ban.
     *
     * @param ip 客户端 IP / client IP
     * @return 是否重连过快或已临时封禁 / true if too fast or currently banned
     */
    public boolean tooFast(String ip) {
        String[] exclIps = Config.EXCLUDED_IP.split(",");
        for (String exclIp : exclIps) {
            if (ip.equals(exclIp)) {
                return false;
            }
        }
        Long banned = ban.get(ip);
        if (banned != null) {
            if (System.currentTimeMillis() < banned) {
                return true;
            } else {
                ban.remove(ip);
                return false;
            }
        }
        Long time = flood.get(ip);
        if (time == null) {
            flood.put(ip, System.currentTimeMillis() + Config.FAST_RECONNECTION_TIME * 1000);
            return false;
        } else {
            if (time > System.currentTimeMillis()) {
                log.info(I18n.get("log.1cb757ad8b6b", ip, Config.WRONG_LOGIN_BAN_TIME));
                ban.put(ip, System.currentTimeMillis() + Config.WRONG_LOGIN_BAN_TIME * 60000);
                return true;
            } else {
                return false;
            }
        }
    }

    @SuppressWarnings("synthetic-access")
    private static class SingletonHolder {

        protected static final FloodProtector instance = new FloodProtector();
    }
}

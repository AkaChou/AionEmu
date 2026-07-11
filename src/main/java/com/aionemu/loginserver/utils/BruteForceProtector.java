package com.aionemu.loginserver.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.loginserver.configs.Config;

/**
 * 暴力破解防护：按 IP 记录登录失败次数，达到阈值时触发封禁判定。
 * Brute-force protector: tracks failed login attempts per IP and signals ban when the threshold is reached.
 *
 * @author Mr. Poke
 */
public class BruteForceProtector {

    private final Map<String, FailedLoginInfo> failedConnections = new ConcurrentHashMap<>();

    /**
     * 单 IP 的登录失败统计信息。
     * Failed-login statistics for a single IP.
     */
    class FailedLoginInfo {

        private int count;
        private long time;

        /**
         * @param count 失败次数 / failure count
         * @param time 记录时间戳 / record timestamp
         */
        public FailedLoginInfo(int count, long time) {
            super();
            this.count = count;
            this.time = time;
        }

        public void increseCount() {
            count++;
        }

        /**
         * the count
         */
        public int getCount() {
            return count;
        }

        /**
         * @return 记录时间戳 / the time
         */
        public long getTime() {
            return time;
        }
    }

    /**
     * 获取单例实例（已弃用，请走 boot 注入）。
     * Returns the singleton instance (deprecated; prefer boot injection).
     *
     * singleton instance
     */
    @Deprecated(since = "boot-migration")
    public static final BruteForceProtector getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 记录一次失败连接；若失败次数达到配置阈值则返回 true 表示应封禁。
     * Records a failed connection; returns true when the failure count reaches the ban threshold.
     *
     * @param ip 客户端 IP / client IP
     * @return 是否应封禁该 IP / whether the IP should be banned
     */
    public boolean addFailedConnect(String ip) {
        FailedLoginInfo failed = failedConnections.get(ip);
        if (failed == null || System.currentTimeMillis() - failed.getTime() > Config.WRONG_LOGIN_BAN_TIME * 1000 * 60) {
            failedConnections.put(ip, new FailedLoginInfo(1, System.currentTimeMillis()));
        } else {
            if (failed.getCount() >= Config.LOGIN_TRY_BEFORE_BAN) {
                failedConnections.remove(ip);
                return true;
            } else {
                failed.increseCount();
            }
        }
        return false;
    }

    @SuppressWarnings("synthetic-access")
    private static class SingletonHolder {

        protected static final BruteForceProtector instance = new BruteForceProtector();
    }
}

package com.aionemu.loginserver.service;

import com.aionemu.loginserver.controller.BannedIpController;
import com.aionemu.loginserver.model.BannedIP;
import java.sql.Timestamp;

/**
 * 登录服 IP 封禁门面，委托 {@link BannedIpController}。
 * Login-server IP ban facade delegating to {@link BannedIpController}.
 */
public class LoginBannedIpService {

    /**
     * 启动 IP 封禁控制器（加载缓存、调度等）。
     * Start the banned-IP controller (load cache, schedule, etc.).
     */
    public void start() {
        BannedIpController.start();
    }

    /**
     * 判断 IP 是否处于封禁中。
     * Whether the given IP is currently banned.
     *
     * @param ip IP 地址 / IP address
     * @return 已封禁返回 true / true if banned
     */
    public boolean isBanned(String ip) {
        return BannedIpController.isBanned(ip);
    }

    /**
     * 永久封禁 IP。
     * Ban an IP permanently.
     *
     * @param ip IP 地址 / IP address
     * @return 操作是否成功 / whether the ban succeeded
     */
    public boolean banIp(String ip) {
        return BannedIpController.banIp(ip);
    }

    /**
     * 在指定过期时间前封禁 IP。
     * Ban an IP until the given expire time.
     *
     * @param ip IP 地址 / IP address
     * @param expireTime 过期时间戳 / expire timestamp
     *
     * @return 操作是否成功 / whether the ban succeeded
     */
    public boolean banIp(String ip, Timestamp expireTime) {
        return BannedIpController.banIp(ip, expireTime);
    }

    /**
     * 新增或更新一条 IP 封禁记录。
     * Add or update an IP ban record.
     *
     * @param ipBan 封禁实体 / ban entity
     * @return 操作是否成功 / whether the write succeeded
     */
    public boolean addOrUpdateBan(BannedIP ipBan) {
        return BannedIpController.addOrUpdateBan(ipBan);
    }

    /**
     * 解除 IP 封禁。
     * Unban an IP.
     *
     * @param ip IP 地址 / IP address
     * @return 操作是否成功 / whether the unban succeeded
     */
    public boolean unbanIp(String ip) {
        return BannedIpController.unbanIp(ip);
    }
}

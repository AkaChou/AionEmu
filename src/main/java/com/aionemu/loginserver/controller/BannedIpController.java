package com.aionemu.loginserver.controller;

import com.aionemu.boot.i18n.I18n;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Set;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.NetworkUtils;
import com.aionemu.loginserver.dao.BannedIpDAO;
import com.aionemu.loginserver.model.BannedIP;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * IP 封禁活动总控。
 * Controller for all IP banning activity.
 *
 * @author SoulKeeper
 */
@Slf4j
@UtilityClass
public class BannedIpController {

    /**
     * 当前封禁 IP / 掩码集合。
     * Set of banned IP addresses or masks.
     */
    private Set<BannedIP> banList;

    /**
     * 启动：清理过期封禁并加载列表。
     * Starts controller: cleans expired bans then loads list.
     */
    public void start() {
        clean();
        load();
    }

    /**
     * 清理数据库中已过期的封禁。
     * Cleans expired bans from database.
     */
    private void clean() {
        getDAO().cleanExpiredBans();
    }

    /**
     * 加载封禁列表（委托 {@link #reload()}）。
     * Loads banned IP list (delegates to {@link #reload()}).
     */
    public void load() {
        reload();
    }

    /**
     * 从数据库重新加载封禁列表。
     * Reloads banned IP list from database.
     */
    public void reload() {
        // 不会每分钟都做 IP 封禁，可适当简化并发代码。 / we are not going to make ip ban every minute, so it's ok to simplify a concurrent code a bit
        banList = getDAO().getAllBans();
        log.info(I18n.get("log.28d603c6c74b", banList.size()));
    }

    /**
     * 检查 IP（或是否命中掩码）是否被封禁。
     * Checks if IP (or matching mask) is banned.
     *
     * @param ip 待检查 IP / IP address to check
     * @return 是否被封禁 / whether banned
     */
    public boolean isBanned(String ip) {
        for (BannedIP ipBan : banList) {
            if (ipBan.isActive() && NetworkUtils.checkIPMatching(ipBan.getMask(), ip)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 永久封禁 IP 或掩码。
     * Bans IP or mask for an infinite period.
     *
     * @param ip 待封 IP / IP to ban
     * @return 是否封禁成功 / Whether ban succeeded
     */
    public boolean banIp(String ip) {
        return banIp(ip, null);
    }

    /**
     * 封禁 IP（或掩码），可指定到期时间。
     * Bans IP (or mask) with optional expiration.
     *
     * @param ip 待封 IP / IP to ban
     * @param expireTime 到期时间，null 表示永不过期 / Expiration time, null = never expires
     * @return 是否封禁成功 / Whether ban succeeded
     */
    public boolean banIp(String ip, Timestamp expireTime) {
        if (ip.equals("127.0.0.1")) {
            return false;
        }

        BannedIP ipBan = new BannedIP();
        ipBan.setMask(ip);
        ipBan.setTimeEnd(expireTime);
        banList.add(ipBan);
        try {
            getDAO().insert(ipBan);
            return true;
        } catch (Exception e) {
            log.warn(I18n.get("log.79fba389e8b1", ip));
            return false;
        }
    }

    /**
     * 新增或更新 IP 封禁，变更会写库。
     * Adds or updates IP ban; changes are reflected in DB.
     *
     * @param ipBan 封禁记录 / Banned IP entry
     * @return 是否更新成功 / Whether update succeeded
     */
    public boolean addOrUpdateBan(BannedIP ipBan) {
        if (ipBan.getId() == null) {
            if (getDAO().insert(ipBan)) {
                banList.add(ipBan);
                return true;
            }
            return false;
        }
        return getDAO().update(ipBan);
    }

    /**
     * 解除 IP 封禁。
     * Removes IP ban.
     *
     * @param ip 待解封 IP / IP to unban
     * @return 是否解封成功 / Whether unban succeeded
     */
    public boolean unbanIp(String ip) {
        Iterator<BannedIP> it = banList.iterator();
        while (it.hasNext()) {
            BannedIP ipBan = it.next();
            if (ipBan.getMask().equals(ip)) {
                if (getDAO().remove(ipBan)) {
                    it.remove();
                    return true;
                }
                break;
            }
        }
        return false;
    }

    /**
     * 获取 {@link BannedIpDAO} 快捷方法。
     * Shortcut for {@link BannedIpDAO}.
     *
     * @return BannedIpDAO 实例 / Banned IP DAO
     */
    private BannedIpDAO getDAO() {
        return DAOManager.getDAO(BannedIpDAO.class);
    }
}

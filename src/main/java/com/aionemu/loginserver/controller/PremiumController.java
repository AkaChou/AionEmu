package com.aionemu.loginserver.controller;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.dao.PremiumDAO;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_PREMIUM_RESPONSE;

/**
 * 高级点数（Toll）消费与充值控制。
 * Premium points (toll) spend and credit controller.
 *
 * @author KID
 */
@Slf4j(topic = "PREMIUM_CTRL")
public class PremiumController {

    /**
     * 操作失败。
     * Operation failed.
     */
    public static byte RESULT_FAIL = 1;

    /**
     * 点数不足。
     * Insufficient points.
     */
    public static byte RESULT_LOW_POINTS = 2;

    /**
     * 操作成功。
     * Operation succeeded.
     */
    public static byte RESULT_OK = 3;

    /**
     * 点数增加。
     * Points added.
     */
    public static byte RESULT_ADD = 4;

    private PremiumDAO dao;

    /**
     * 获取单例（遗留入口，启动迁移后弃用）。
     * Returns singleton (legacy entry, deprecated after boot migration).
     *
     * @return 控制器实例 / Controller instance
     * @deprecated 优先使用注入 / prefer injection
     */
    @Deprecated(since = "boot-migration")
    public static PremiumController getController() {
        return SingletonHolder.CONTROLLER;
    }

    /**
     * 构造并初始化 Premium DAO。
     * Constructs controller and initializes Premium DAO.
     */
    public PremiumController() {
        dao = DAOManager.getDAO(PremiumDAO.class);
        log.info(I18n.get("log.f446666082da"));
    }

    /**
     * 处理游戏服发起的点数消费/增加请求。
     * Handles spend/credit request from a gameserver.
     *
     * @param accountId 账号 ID / Account id
     * @param requestId 请求 ID / Request id
     * @param cost 消耗点数；负数表示增加 / Cost; negative means add
     * @param serverId 游戏服 ID / GameServer id
     */
    public void requestBuy(int accountId, int requestId, long cost, byte serverId) {
        long points = this.dao.getPoints(accountId);
        long luna = this.dao.getLuna(accountId);

        GameServerInfo server = GameServerTable.getGameServerInfo(serverId);
        if (server == null || server.getConnection() == null || !server.isAccountOnGameServer(accountId)) {
            log.error(I18n.get("log.d07fbea7e8f2", accountId, requestId, serverId));
            return;
        }

        if (cost < 0) {
            long ncnt = points + (cost * -1);
            dao.updatePoints(accountId, ncnt, 0);
            server.getConnection().sendPacket(new SM_PREMIUM_RESPONSE(requestId, RESULT_ADD, ncnt, luna));
            return;
        }

        if (points < cost) {
            server.getConnection().sendPacket(new SM_PREMIUM_RESPONSE(requestId, RESULT_LOW_POINTS, points, luna));
            return;
        }

        if (dao.updatePoints(accountId, points, cost)) {
            points -= cost;
            server.getConnection().sendPacket(new SM_PREMIUM_RESPONSE(requestId, RESULT_OK, points, luna));
            log.info(I18n.get("log.8a35da9adb14", accountId, requestId, cost, serverId));
        } else {
            server.getConnection().sendPacket(new SM_PREMIUM_RESPONSE(requestId, RESULT_FAIL, points, luna));
            log.info(I18n.get("log.50f36dc349ef", accountId, requestId, cost, serverId));
        }
    }

    private static final class SingletonHolder {

        private static final PremiumController CONTROLLER = new PremiumController();
    }
}

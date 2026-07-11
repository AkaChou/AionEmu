package com.aionemu.loginserver.network.gameserver.clientpackets;

import com.aionemu.loginserver.network.gameserver.GsClientPacket;
import com.aionemu.loginserver.service.LoginPremiumServices;

/**
 * GS→LS：请求消耗 premium 点数购买。
 * GS→LS: request premium-point purchase/deduction.
 *
 * @author KID
 */
public class CM_PREMIUM_CONTROL extends GsClientPacket {

    /**
     * 账号 ID。
     * Account id.
     */
    private int accountId;
    /**
     * 请求 ID。
     * Request id.
     */
    private int requestId;
    /**
     * 所需费用。
     * Required cost.
     */
    private long requiredCost;
    /**
     * 游戏服 ID。
     * GameServer id.
     */
    private byte serverId;

    /**
     * 读取账号、请求 ID、费用与服务器 ID。
     * Reads account, request id, cost, and server id.
     */
    @Override
    protected void readImpl() {
        accountId = readD();
        requestId = readD();
        requiredCost = readQ();
        serverId = (byte) readC();
    }

    /**
     * 委托 PremiumController 处理购买请求。
     * Delegates purchase request to PremiumController.
     */
    @Override
    protected void runImpl() {
        LoginPremiumServices.premiumController().requestBuy(accountId, requestId, requiredCost, serverId);
    }
}

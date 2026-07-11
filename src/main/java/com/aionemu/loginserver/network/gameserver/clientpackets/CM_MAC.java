package com.aionemu.loginserver.network.gameserver.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;

/**
 * GS→LS：上报/刷新账号最近一次 MAC 地址。
 * GS→LS: report/refresh the last MAC address of an account.
 *
 * @author nrg
 */
@Slf4j
public class CM_MAC extends GsClientPacket {

    /**
     * 账号 ID。
     * Account id.
     */
    private int accountId;
    /**
     * MAC 地址。
     * MAC address.
     */
    private String address;

    /**
     * 读取账号 ID 与 MAC 地址。
     * Reads account id and MAC address.
     */
    @Override
    protected void readImpl() {
        accountId = readD();
        address = readS();
    }

    /**
     * 刷新账号 last MAC；失败则记错误日志。
     * Refreshes account last MAC; logs error on failure.
     */
    @Override
    protected void runImpl() {
        if (!AccountController.refreshAccountsLastMac(accountId, address)) {
            log.error(I18n.get("log.816eb08301ce", accountId));
        }
    }
}

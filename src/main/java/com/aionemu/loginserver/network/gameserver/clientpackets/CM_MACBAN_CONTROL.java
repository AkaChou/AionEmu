package com.aionemu.loginserver.network.gameserver.clientpackets;

import com.aionemu.loginserver.network.gameserver.GsClientPacket;
import com.aionemu.loginserver.service.LoginProtectionServices;

/**
 * GS→LS：MAC 封禁/解封控制。
 * GS→LS: MAC ban/unban control.
 *
 * @author KID
 */
public class CM_MACBAN_CONTROL extends GsClientPacket {

    /**
     * 操作类型：0=解封，1=封禁。
     * Action type: 0 = unban, 1 = ban.
     */
    private byte type;
    /**
     * MAC 地址。
     * MAC address.
     */
    private String address;
    /**
     * 备注/详情。
     * Details/notes.
     */
    private String details;
    /**
     * 封禁到期时间戳。
     * Ban expiry timestamp.
     */
    private long time;

    /**
     * 读取操作类型、地址、详情与时间。
     * Reads action type, address, details, and time.
     */
    @Override
    protected void readImpl() {
        type = (byte) readC();
        address = readS();
        details = readS();
        time = readQ();
    }

    /**
     * 按类型执行 MAC 封禁或解封。
     * Performs MAC ban or unban by type.
     */
    @Override
    protected void runImpl() {
        switch (type) {
            case 0://unban
                LoginProtectionServices.bannedMacManager().unban(address, details);
                break;
            case 1://ban
                LoginProtectionServices.bannedMacManager().ban(address, time, details);
                break;
        }
    }
}

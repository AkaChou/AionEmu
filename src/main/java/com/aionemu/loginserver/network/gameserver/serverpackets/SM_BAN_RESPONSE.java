package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;
import lombok.AllArgsConstructor;

/**
 * LS→GS：封禁请求处理结果响应。
 * LS→GS: ban request result response.
 * @author Watson
 */
@AllArgsConstructor
public class SM_BAN_RESPONSE extends GsServerPacket {

    /**
     * 封禁类型。
     * Ban type.
     */
    private final byte type;
    /**
     * 被封禁账号 ID。
     * Banned account id.
     */
    private final int accountId;
    /**
     * 被封禁 IP。
     * Banned IP address.
     */
    private final String ip;
    /**
     * 封禁时长。
     * Ban duration.
     */
    private final int time;
    /**
     * 操作管理员对象 ID。
     * Admin object id who issued the ban.
     */
    private final int adminObjId;
    /**
     * 封禁是否成功。
     * Whether the ban operation succeeded.
     */
    private final boolean result;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(5);
        writeC(type);
        writeD(accountId);
        writeS(ip);
        writeD(time);
        writeD(adminObjId);
        writeC(result ? 1 : 0);
    }
}

package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;
import lombok.AllArgsConstructor;

/**
 * LS→GS：高级/付费相关操作结果响应（请求 ID、结果码、点数与露娜币）。
 * LS→GS: premium operation result response (request id, result code, points and luna).
 * @author KID
 */
@AllArgsConstructor
public class SM_PREMIUM_RESPONSE extends GsServerPacket {

    /**
     * 请求 ID。
     * Request id.
     */
    private final int requestId;
    /**
     * 结果码。
     * Result code.
     */
    private final int result;
    /**
     * 点数余额。
     * Point balance.
     */
    private final long points;
    /**
     * 露娜币余额。
     * Luna balance.
     */
    private final long luna;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(10);
        writeD(requestId);
        writeD(result);
        writeQ(points);
        writeQ(luna);
    }
}

package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;

/**
 * LS→GS：高级/付费相关操作结果响应（请求 ID、结果码、点数与露娜币）。
 * LS→GS: premium operation result response (request id, result code, points and luna).
 *
 * @author KID
 */
public class SM_PREMIUM_RESPONSE extends GsServerPacket {

    /**
     * 请求 ID。
     * Request id.
     */
    private int requestId;
    /**
     * 结果码。
     * Result code.
     */
    private int result;
    /**
     * 点数余额。
     * Point balance.
     */
    private long points;
    /**
     * 露娜币余额。
     * Luna balance.
     */
    private long luna;

    /**
     * 构造高级操作结果响应包。
     * Constructs a premium operation result response packet.
     *
     * request id
     * result code
     * points
     * luna
     */
    public SM_PREMIUM_RESPONSE(int requestId, int result, long points, long luna) {
        this.requestId = requestId;
        this.result = result;
        this.points = points;
        this.luna = luna;
    }

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

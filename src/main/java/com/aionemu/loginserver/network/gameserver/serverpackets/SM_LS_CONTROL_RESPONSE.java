package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;

/**
 * LS→GS：登录服控制指令执行结果响应。
 * LS→GS: login-server control command result response.
 *
 * @author Aionchs-Wylovech
 */
public class SM_LS_CONTROL_RESPONSE extends GsServerPacket {

    /**
     * 控制指令类型。
     * Control command type.
     */
    private int type;
    /**
     * 执行是否成功。
     * Whether the operation succeeded.
     */
    private boolean result;
    /**
     * 目标玩家名。
     * Target player name.
     */
    private String playerName;
    /**
     * 附加参数。
     * Extra parameter.
     */
    private int param;
    /**
     * 操作管理员名。
     * Admin name who issued the command.
     */
    private String adminName;
    /**
     * 目标账号 ID。
     * Target account id.
     */
    private int accountId;

    /**
     * 构造登录服控制结果响应包。
     * Constructs a login-server control result response packet.
     *
     * @param type 控制类型 / control type
     * @param result 是否成功 / whether succeeded
     * @param playerName 玩家名 / player name
     * @param accountId 账号 ID / account id
     * @param param 附加参数 / extra parameter
     * @param adminName 管理员名 / admin name
     */
    public SM_LS_CONTROL_RESPONSE(int type, boolean result, String playerName, int accountId, int param, String adminName) {
        this.type = type;
        this.result = result;
        this.playerName = playerName;
        this.param = param;
        this.adminName = adminName;
        this.accountId = accountId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(4);
        writeC(type);
        writeC(result ? 1 : 0);
        writeS(adminName);
        writeS(playerName);
        writeC(param);
        writeD(accountId);
    }
}

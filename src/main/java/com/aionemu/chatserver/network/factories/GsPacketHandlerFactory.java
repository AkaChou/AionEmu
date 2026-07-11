package com.aionemu.chatserver.network.factories;

import com.aionemu.boot.i18n.I18n;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;

import com.aionemu.chatserver.network.gameserver.GsClientPacket;
import com.aionemu.chatserver.network.gameserver.GsConnection;
import com.aionemu.chatserver.network.gameserver.GsConnection.State;
import com.aionemu.chatserver.network.gameserver.clientpackets.CM_CS_AUTH;
import com.aionemu.chatserver.network.gameserver.clientpackets.CM_PLAYER_AUTH;
import com.aionemu.chatserver.network.gameserver.clientpackets.CM_PLAYER_GAG;
import com.aionemu.chatserver.network.gameserver.clientpackets.CM_PLAYER_LOGOUT;

/**
 * 游戏服连接的数据包工厂（按状态/操作码分发）。
 * Packet factory for game-server connections (dispatches by state/opcode).
 *
 * @author -Nemesiss-
 */
@Slf4j
@UtilityClass
public class GsPacketHandlerFactory {

    /**
     * 从给定缓冲区读取并构造一个游戏服客户端包。
     * Reads and constructs one game-server client packet from the given buffer.
     *
     * @param data 原始数据缓冲区 / raw data buffer
     * @param client 游戏服连接 / game-server connection
     * @return 解析出的客户端包，未知时为 {@code null} / parsed client packet, or {@code null} if unknown
     */
    public static GsClientPacket handle(ByteBuffer data, GsConnection client) {
        GsClientPacket msg = null;
        State state = client.getState();
        int id = data.get() & 0xff;

        switch (state) {
            case CONNECTED: {
                switch (id) {
                    case 0x00:
                        msg = new CM_CS_AUTH(data, client);
                        break;
                    default:
                        unknownPacket(state, id);
                }
                break;
            }
            case AUTHED: {
                switch (id) {
                    case 0x01:
                        msg = new CM_PLAYER_AUTH(data, client);
                        break;
                    case 0x02:
                        msg = new CM_PLAYER_LOGOUT(data, client);
                        break;
                    case 0x03:
                        msg = new CM_PLAYER_GAG(data, client);
                        break;
                    default:
                        unknownPacket(state, id);
                }
                break;
            }
        }

        if (msg != null) {
            msg.setConnection(client);
            msg.setBuffer(data);
        }

        return msg;
    }

    /**
     * 记录未知数据包警告。
     * Logs a warning for an unknown packet.
     *
     * @param state 当前连接状态 / current connection state
     * @param id 包操作码 / packet opcode
     */
    private static void unknownPacket(State state, int id) {
        log.warn(I18n.get("log.2585d962bf33", String.format("%02X", id), state));
    }
}

package com.aionemu.loginserver.network.factories;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;

import com.aionemu.loginserver.network.aion.AionClientPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;
import com.aionemu.loginserver.network.aion.LoginConnection.State;
import com.aionemu.loginserver.network.aion.clientpackets.CM_AUTH_GG;
import com.aionemu.loginserver.network.aion.clientpackets.CM_LOGIN;
import com.aionemu.loginserver.network.aion.clientpackets.CM_PLAY;
import com.aionemu.loginserver.network.aion.clientpackets.CM_SERVER_LIST;
import com.aionemu.loginserver.network.aion.clientpackets.CM_UPDATE_SESSION;

/**
 * 登录服 Aion 客户端包工厂：按连接状态与 opcode 分发客户端包。
 * Login-server Aion client-packet factory: dispatches packets by connection state and opcode.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class AionPacketHandlerFactory {


    /**
     * 从 ByteBuffer 读取并构造一个客户端包。
     * Reads one client packet from the given ByteBuffer.
     *
     * @param data 原始包数据 / raw packet data
     * @param client 登录连接 / login connection
     * @return 解析出的 AionClientPacket，未知包返回 null
     *         Parsed AionClientPacket, or null for unknown packets
     */
    public static AionClientPacket handle(ByteBuffer data, LoginConnection client) {
        AionClientPacket msg = null;
        State state = client.getState();
        int id = data.get() & 0xff;

        switch (state) {
            case CONNECTED: {
                switch (id) {
                    case 0x07:
                        msg = new CM_AUTH_GG(data, client);
                        break;
                    case 0x08:
                        msg = new CM_UPDATE_SESSION(data, client);
                        break;
                    default:
                        unknownPacket(state, id);
                }
                break;
            }
            case AUTHED_GG: {
                switch (id) {
                    case 0x0B:
                        msg = new CM_LOGIN(data, client);
                        break;
                    default:
                        unknownPacket(state, id);
                }
                break;
            }
            case AUTHED_LOGIN: {
                switch (id) {
                    case 0x05:
                        msg = new CM_SERVER_LIST(data, client);
                        break;
                    case 0x02:
                        msg = new CM_PLAY(data, client);
                        break;
                    default:
                        unknownPacket(state, id);
                }
                break;
            }
        }

        return msg;
    }

    /**
     * 记录未知客户端包。
     * Logs an unknown client packet.
     *
     * @param state 当前连接状态 / current connection state
     * @param id 包 opcode / packet opcode
     */
    private static void unknownPacket(State state, int id) {
        log.warn(I18n.get("log.1d0c01d73a77", String.format("%02X", id), state));
    }
}

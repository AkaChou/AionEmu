package com.aionemu.loginserver.network.factories;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;

import org.springframework.stereotype.Service;

import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.controller.AccountTimeController;
import com.aionemu.loginserver.controller.BannedIpController;
import com.aionemu.loginserver.controller.BannedMacManager;
import com.aionemu.loginserver.controller.PremiumController;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsConnection.State;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_ACCOUNT_AUTH;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_ACCOUNT_DISCONNECTED;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_ACCOUNT_LIST;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_ACCOUNT_RECONNECT_KEY;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_ACCOUNT_TOLL_INFO;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_BAN;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_GS_AUTH;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_GS_CHARACTER;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_GS_PONG;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_LS_CONTROL;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_MAC;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_MACBAN_CONTROL;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_PREMIUM_CONTROL;
import com.aionemu.loginserver.network.gameserver.clientpackets.CM_PTRANSFER_CONTROL;
import com.aionemu.loginserver.persistence.dao.AccountDataDao;
import com.aionemu.loginserver.persistence.dao.AccountTimeDao;
import com.aionemu.loginserver.service.PlayerTransferService;
import com.aionemu.loginserver.utils.ThreadPoolManager;

import lombok.RequiredArgsConstructor;

/**
 * 登录服游戏服包工厂：按 GS 连接状态与 opcode 分发游戏服客户端包，并注入各包所需服务。
 * Login-server game-server packet factory: dispatches GS client packets by state and opcode,
 * injecting each packet with the services it needs.
 * @author -Nemesiss-
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GsPacketHandlerFactory {

    private final GameServerTable gameServerTable;
    private final AccountController accountController;
    private final AccountTimeController accountTimeController;
    private final AccountTimeDao accountTimeDao;
    private final AccountDataDao accountDataDao;
    private final BannedIpController bannedIpController;
    private final BannedMacManager bannedMacManager;
    private final PremiumController premiumController;
    private final PlayerTransferService playerTransferService;
    private final ThreadPoolManager threadPoolManager;

    /**
     * 从 ByteBuffer 读取并构造一个游戏服客户端包。
     * Reads one game-server client packet from the given ByteBuffer.
     * @param data 原始包数据 / raw packet data
     * @param client 游戏服连接 / game-server connection
     * @return 解析出的 GsClientPacket，未知包返回 null
     *         Parsed GsClientPacket, or null for unknown packets
     */
    public GsClientPacket handle(ByteBuffer data, GsConnection client) {
        GsClientPacket msg = null;
        State state = client.getState();
        int id = data.get() & 0xff;

        switch (state) {
            case CONNECTED: {
                switch (id) {
                    case 0:
                        msg = new CM_GS_AUTH(gameServerTable, bannedMacManager, threadPoolManager);
                        break;
                    case 13:
                        msg = new CM_MAC(accountController);
                        break;
                    default:
                        unknownPacket(state, id);
                }
                break;
            }
            case AUTHED: {
                switch (id) {
                    case 1:
                        msg = new CM_ACCOUNT_AUTH(accountController);
                        break;
                    case 2:
                        msg = new CM_ACCOUNT_RECONNECT_KEY(accountController);
                        break;
                    case 3:
                        msg = new CM_ACCOUNT_DISCONNECTED(accountTimeController);
                        break;
                    case 4:
                        msg = new CM_ACCOUNT_LIST(accountController, gameServerTable);
                        break;
                    case 5:
                        msg = new CM_LS_CONTROL(accountDataDao);
                        break;
                    case 6:
                        msg = new CM_BAN(accountTimeDao, accountDataDao, bannedIpController, accountController,
                            gameServerTable);
                        break;
                    case 8:
                        msg = new CM_GS_CHARACTER(accountController);
                        break;
                    case 9:
                        msg = new CM_ACCOUNT_TOLL_INFO(accountDataDao);
                        break;
                    case 10:
                        msg = new CM_MACBAN_CONTROL(bannedMacManager);
                        break;
                    case 11:
                        msg = new CM_PREMIUM_CONTROL(premiumController);
                        break;
                    case 12:
                        msg = new CM_GS_PONG(gameServerTable);
                        break;
                    case 13:
                        msg = new CM_MAC(accountController);
                        break;
                    case 14:
                        msg = new CM_PTRANSFER_CONTROL(playerTransferService);
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
     * 记录未知游戏服包。
     * Logs an unknown game-server packet.
     * @param state 当前连接状态 / current connection state
     * @param id 包 opcode / packet opcode
     */
    private static void unknownPacket(State state, int id) {
        log.warn(I18n.get("log.2585d962bf33", String.format("%02X", id), state));
    }
}

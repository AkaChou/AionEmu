package com.aionemu.chatserver.common.netty;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象网络包处理器。
 * Abstract network packet handler.
 *
 * @author ATracer
 */
@Slf4j
public abstract class AbstractPacketHandler {

    /**
     * 记录未知数据包。
     * Logs an unknown packet.
     *
     * @param id 数据包 ID / Packet id
     * @param state 当前状态 / Current state
     */
    protected void unknownPacket(int id, String state) {
        log.warn(I18n.get("log.2585d962bf33", String.format("%02X", id), state));
    }
}

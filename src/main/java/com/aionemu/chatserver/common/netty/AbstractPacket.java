package com.aionemu.chatserver.common.netty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 抽象网络包。
 * Abstract network packet.
 *
 * @author ATracer
 */
@Getter
@RequiredArgsConstructor
public abstract class AbstractPacket {

    /**
     * 操作码。
     * Opcode.
     */
    protected final int opCode;
}

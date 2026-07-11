package com.aionemu.loginserver.network.gameserver;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.network.packet.BaseClientPacket;
import lombok.extern.slf4j.Slf4j;

/**
 * 所有 GameServer → LoginServer 客户端封包的基类。
 * Base class for every GameServer → LoginServer client packet.
 *
 * @author -Nemesiss-
 */
@Slf4j
public abstract class GsClientPacket extends BaseClientPacket<GsConnection> {

    /**
     * 构造空 opcode 的 GS 客户端封包。
     * Construct a GS client packet with empty opcode.
     */
    public GsClientPacket() {
        super(0);
    }

    /**
     * 执行 {@link #runImpl()} 并捕获、记录异常。
     * Run {@link #runImpl()} while catching and logging any throwable.
     */
    @Override
    public final void run() {
        try {
            runImpl();
        } catch (Throwable e) {
            log.warn(I18n.get("log.8fd86409bd46", getConnection().getIP(), this, e));
        }
    }

    /**
     * 向本封包所属连接发送服务端封包，等价于 {@code getConnection().sendPacket(msg)}。
     * Send a server packet to the owning connection; equivalent to {@code getConnection().sendPacket(msg)}.
     *
     * @param msg 待发送的服务端封包 / Server packet to send
     */
    protected void sendPacket(GsServerPacket msg) {
        getConnection().sendPacket(msg);
    }
}

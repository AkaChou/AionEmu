package com.aionemu.loginserver.network.aion;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.network.packet.BaseClientPacket;
import com.aionemu.loginserver.model.Account;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;

/**
 * 所有 Aion 客户端 → 登录服数据包的基类。
 * Base class for every Aion client → login-server packet.
 *
 * @author -Nemesiss-
 */
@Slf4j
public abstract class AionClientPacket extends BaseClientPacket<LoginConnection> {

    /**
     * 构造客户端包并绑定连接。
     * Construct client packet and bind the connection.
     *
     * @param buf 包体数据 / Packet data
     * Login connection
     * Packet opcode
     */
    protected AionClientPacket(ByteBuffer buf, LoginConnection client, int opcode) {
        super(buf, opcode);
        setConnection(client);
    }

    /**
     * 执行 {@link #runImpl()}，捕获并记录异常。
     * Run {@link #runImpl()}, catching and logging any throwable.
     */
    @Override
    public final void run() {
        try {
            runImpl();
        } catch (Throwable e) {
            String name;
            Account account = getConnection().getAccount();
            if (account != null) {
                name = account.getName();
            } else {
                name = getConnection().getIP();
            }

            log.error(I18n.get("log.de8b49a0a205", name, this, e), e);
        }
    }

    /**
     * 向本包所属连接发送服务端包（等价于 getConnection().sendPacket(msg)）。
     * Send a server packet on this packet's connection (same as getConnection().sendPacket(msg)).
     *
     * @param msg 待发送的服务端包 / Server packet to send
     */
    protected void sendPacket(AionServerPacket msg) {
        getConnection().sendPacket(msg);
    }
}

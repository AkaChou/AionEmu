package com.aionemu.gameserver.network.loginserver;

/**
 * 游戏服与登录服消息分发器抽象接口。
 * Abstract dispatcher interface for messaging between GameServer and LoginServer.
 */
public interface LoginMessageDispatcher {

    /**
     * 发送服务端封包给登录服。
     * Send server packet to login server.
     *
     * @param packet 待发送封包 / packet to send
     * @return 是否发送成功 / whether sending succeeded
     */
    boolean sendPacket(LsServerPacket packet);

    /**
     * 当前与登录服连接状态是否已认证就绪。
     * Whether connection to login server is authenticated and ready.
     *
     * @return 就绪状态 / ready state
     */
    boolean isAuthed();

    /**
     * 关闭与登录服的通道。
     * Close the channel with login server.
     */
    void close();
}

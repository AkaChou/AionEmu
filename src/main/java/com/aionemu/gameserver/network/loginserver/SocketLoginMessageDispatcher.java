package com.aionemu.gameserver.network.loginserver;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection.State;

/**
 * 基于 TCP Socket 的登录服消息分发适配器，保留分布式部署能力。
 * Socket-backed login message dispatcher adapter, preserving distributed deployment support.
 */
final class SocketLoginMessageDispatcher implements LoginMessageDispatcher {

    private final LoginServerConnection connection;

    SocketLoginMessageDispatcher(LoginServerConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean sendPacket(LsServerPacket packet) {
        if (!isAuthed()) {
            return false;
        }
        connection.sendPacket(packet);
        return true;
    }

    @Override
    public boolean isAuthed() {
        return connection != null && connection.getState() == State.AUTHED;
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.close(true);
        }
    }
}

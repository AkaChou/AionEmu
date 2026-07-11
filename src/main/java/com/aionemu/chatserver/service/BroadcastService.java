package com.aionemu.chatserver.service;

import java.util.Map;

import com.aionemu.chatserver.model.ChatClient;
import com.aionemu.chatserver.model.message.Message;
import com.aionemu.chatserver.network.aion.serverpackets.SM_CHANNEL_MESSAGE;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 频道消息广播服务：维护在线客户端集合并向同频道客户端投递消息。
 * Channel message broadcast service: tracks online clients and delivers messages to co-channel clients.
 *
 * @author ATracer
 */
public class BroadcastService {

    /**
     * 获取单例（已废弃，迁移至 Boot 后请使用注入）。
     * Return the singleton (deprecated; prefer injection after Boot migration).
     *
     * Singleton instance
     * @deprecated boot-migration
     */
    @Deprecated(since = "boot-migration")
    public static BroadcastService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private Map<Integer, ChatClient> clients = new ConcurrentHashMap<>();

    /**
     * 将客户端加入广播集合。
     * Add a client to the broadcast set.
     *
     * @param client 聊天客户端 / Chat client
     */
    public void addClient(ChatClient client) {
        clients.put(client.getClientId(), client);
    }

    /**
     * 从广播集合移除客户端。
     * Remove a client from the broadcast set.
     *
     * @param client 聊天客户端 / Chat client
     */
    public void removeClient(ChatClient client) {
        clients.remove(client.getClientId());
    }

    /**
     * 向处于消息频道内的所有客户端广播。
     * Broadcast a message to all clients present in the message channel.
     *
     * Message
     */
    public void broadcastMessage(Message message) {
        for (ChatClient client : clients.values()) {
            if (client.isInChannel(message.getChannel())) {
                sendMessage(client, message);
            }
        }
    }

    /**
     * 向指定客户端发送频道消息包。
     * Send a channel message packet to the given client.
     *
     * @param chatClient 目标客户端 / Target chat client
     * Message
     */
    public void sendMessage(ChatClient chatClient, Message message) {
        ClientChannelHandler cch = chatClient.getChannelHandler();
        cch.sendPacket(new SM_CHANNEL_MESSAGE(message));
    }

    /**
     * 单例持有者。
     * Singleton holder.
     */
    private static final class SingletonHolder {

        private static final BroadcastService INSTANCE = new BroadcastService();
    }
}

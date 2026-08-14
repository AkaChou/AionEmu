package com.aionemu.chatserver.model.channel;

import java.nio.charset.Charset;

import com.aionemu.chatserver.model.ChannelType;
import com.aionemu.chatserver.service.ChatCoreServices;
import lombok.Getter;

/**
 * 聊天频道抽象基类，持有类型、标识与运行时频道 ID。
 * Abstract chat channel base holding type, identifier and runtime channel id.
 *
 * @author ATracer
 */
public abstract class Channel {

    /**
     * 频道类型。
     * Channel type.
     */
    @Getter
    private final ChannelType channelType;
    /**
     * UTF-16LE 编码的标识字节。
     * Identifier bytes encoded as UTF-16LE.
     */
    @Getter
    private final byte[] identifierBytes;
    /**
     * 频道字符串标识。
     * Channel string identifier.
     */
    @Getter
    private final String identifier;
    /**
     * 运行时分配的频道 ID。
     * Runtime-assigned channel id.
     */
    @Getter
    private final int channelId;

    /**
     * 创建频道并分配运行时 ID。
     * Creates a channel and assigns a runtime id.
     *
     * @param channelType 频道类型 / channel type
     * @param identifier 字符串标识 / string identifier
     */
    public Channel(ChannelType channelType, String identifier) {
        this.channelType = channelType;
        this.identifier = identifier;
        this.channelId = ChatCoreServices.idFactory().nextId();
        this.identifierBytes = identifier.getBytes(Charset.forName("UTF-16le"));
    }

    /**
     * 获取字符串形式的频道标识。
     * Returns the channel identifier as string.
     *
     * @return 频道字符串标识 / channel identifier
     */
    public String getStringIdentifier() {
        return identifier;
    }

}

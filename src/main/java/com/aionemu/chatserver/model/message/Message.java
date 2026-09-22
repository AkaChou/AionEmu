package com.aionemu.chatserver.model.message;


import java.nio.charset.StandardCharsets;

import com.aionemu.chatserver.model.ChatClient;
import com.aionemu.chatserver.model.channel.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;

/**
 * 聊天消息模型，包含频道、文本字节与发送者。
 * Chat message model containing channel, text bytes and sender.
 * @author ATracer
 */
@Slf4j
@AllArgsConstructor
public class Message {

    /**
     * 消息所属频道。
     * Channel this message belongs to.
     */
    @Getter
    private final Channel channel;
    /**
     * 消息文本字节（UTF-16LE）。
     * Message text bytes (UTF-16LE).
     */
    @Getter
    private byte[] text;
    /**
     * 消息发送者。
     * Message sender.
     */
    @Getter
    private final ChatClient sender;

    /**
     * 以 UTF-16LE 编码设置消息文本。
     * Sets message text encoded as UTF-16LE.
     * @param str 文本内容 / text content
     */
    public void setText(String str) {
		this.text = str.getBytes(StandardCharsets.UTF_16LE);
	}

    /**
     * 返回文本字节长度。
     * Returns the text byte length.
     * @return 文本字节长度 / Returns the text byte length.
     */
    public int size() {
        return text.length;
    }

    /**
     * 解析发送者标识中的昵称字符串。
     * Parses the nickname string from the sender identifier.
     * @return 发送者昵称；失败返回空串 / sender nickname, or empty string on failure
     */
    public String getSenderString() {
        try {
            String s = new String(sender.getIdentifier(), StandardCharsets.UTF_16LE);
            int pos = s.indexOf('@');
            s = s.substring(0, pos);
            return s;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 将文本字节解码为字符串。
     * Decodes text bytes into a string.
     * @return 消息文本；失败返回空串 / message text, or empty string on failure
     */
    public String getTextString() {
        try {
            String s = new String(text, StandardCharsets.UTF_16LE);
            return s;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取频道类型名称。
     * Returns the channel type name.
     * @return 频道类型名 / channel type name
     */
    public String getChannelString() {
        return channel.getChannelType().name();
    }
}

/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package com.aionemu.chatserver.model.message;

import java.io.UnsupportedEncodingException;

import com.aionemu.chatserver.model.ChatClient;
import com.aionemu.chatserver.model.channel.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ATracer
 */
@Slf4j
public class Message {

    @Getter
    private Channel channel;
    @Getter
    private byte[] text;
    @Getter
    private ChatClient sender;

    /**
     * @param channel
     * @param text
     */
    public Message(Channel channel, byte[] text, ChatClient sender) {
        this.channel = channel;
        this.text = text;
        this.sender = sender;
    }

    public void setText(String str) {
        try {
            this.text = str.getBytes("utf-16le");
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to encode chat message text", e);
        }
    }

    public int size() {
        return text.length;
    }

    public String getSenderString() {
        try {
            String s = new String(sender.getIdentifier(), "UTF-16le");
            int pos = s.indexOf('@');
            s = s.substring(0, pos);
            return s;
        } catch (Exception e) {
            return "";
        }
    }

    public String getTextString() {
        try {
            String s = new String(text, "UTF-16le");
            return s;
        } catch (Exception e) {
            return "";
        }
    }

    public String getChannelString() {
        return channel.getChannelType().name();
    }
}

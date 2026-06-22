package com.aionl.slf4j.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 聊天日志过滤器
 * Chat log filter for filtering log messages with [MESSAGE] prefix
 *
 * 该过滤器用于筛选带有[MESSAGE]前缀的日志消息，通常用于记录游戏中的聊天信息
 * This filter is used to filter log messages with [MESSAGE] prefix, typically used for recording in-game chat messages
 */
public class ChatLogFilter extends Filter<ILoggingEvent> {

    /**
     * 决定是否接受或拒绝日志事件
     * Decide whether to accept or deny the logging event
     *
     * @param loggingEvent 日志事件对象 / The logging event object
     * @return FilterReply.ACCEPT 如果消息以[MESSAGE]开头，否则返回FilterReply.DENY
     *         FilterReply.ACCEPT if message starts with [MESSAGE], otherwise FilterReply.DENY
     */
    public FilterReply decide(ILoggingEvent loggingEvent) {
        Object message = loggingEvent.getMessage();
        return ((String)message).startsWith("[MESSAGE]") ? FilterReply.ACCEPT : FilterReply.DENY;
    }
}

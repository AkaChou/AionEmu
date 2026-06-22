package com.aionl.slf4j.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 自动组队服务日志过滤器
 * Auto Group Service Log Filter
 * 
 * 该过滤器用于筛选带有[AUTOGROUPSERVICE]前缀的日志消息
 * This filter is used to filter log messages with [AUTOGROUPSERVICE] prefix
 */
public class AutoGroupFilter extends Filter<ILoggingEvent> {

    /**
     * 决定是否接受日志事件
     * Decide whether to accept the logging event
     *
     * @param loggingEvent 日志事件 (The logging event)
     * @return FilterReply.ACCEPT 如果消息以[AUTOGROUPSERVICE]开头，否则返回FilterReply.DENY
     *         (FilterReply.ACCEPT if message starts with [AUTOGROUPSERVICE], otherwise FilterReply.DENY)
     */
    public FilterReply decide(ILoggingEvent loggingEvent) {
        Object message = loggingEvent.getMessage();
        return ((String)message).startsWith("[AUTOGROUPSERVICE]") ? FilterReply.ACCEPT : FilterReply.DENY;
    }
}

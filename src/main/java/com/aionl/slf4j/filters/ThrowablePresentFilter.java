package com.aionl.slf4j.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 异常日志过滤器
 * Throwable log filter that filters exception-related messages
 */
public class ThrowablePresentFilter extends Filter<ILoggingEvent> {

    /**
     * 决定是否接受或拒绝日志事件
     * Decides whether to accept or deny a logging event
     *
     * @param loggingEvent 待处理的日志事件 (The logging event to be processed)
     * @return FilterReply.ACCEPT 如果消息是Throwable类型；FilterReply.DENY 如果不是
     *         (FilterReply.ACCEPT if message is instance of Throwable; FilterReply.DENY if not)
     */
    public FilterReply decide(ILoggingEvent loggingEvent) {
        Object message = loggingEvent.getMessage();
        return message instanceof Throwable ? FilterReply.ACCEPT : FilterReply.DENY;
    }
}

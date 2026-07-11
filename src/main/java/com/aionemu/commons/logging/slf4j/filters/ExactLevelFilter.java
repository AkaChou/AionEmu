package com.aionemu.commons.logging.slf4j.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 精确日志级别过滤器，仅匹配指定 Level
 * Exact log level filter that matches only a specific Level
 */
public class ExactLevelFilter extends AbstractMatcherFilter<ILoggingEvent> {

	private Level targetLevel;

	/**
	 * 在目标级别已配置时启动过滤器
	 * Start the filter when the target level is configured
	 */
	@Override
	public void start() {
		if (targetLevel != null) {
			super.start();
		}
	}

	/**
	 * 按目标级别决定是否接受日志事件
	 * Decide whether to accept the logging event by target level
	 *
	 * @param event 日志事件 / Logging event
	 * @return 匹配时返回 onMatch，否则返回 onMismatch；未启动时返回 NEUTRAL / onMatch when level matches, onMismatch otherwise; NEUTRAL when not started
	 */
	@Override
	public FilterReply decide(ILoggingEvent event) {
		if (!isStarted() || event == null) {
			return FilterReply.NEUTRAL;
		}
		return targetLevel.equals(event.getLevel()) ? onMatch : onMismatch;
	}

	/**
	 * 设置需要精确匹配的日志级别
	 * Set the exact target log level
	 *
	 * Target level
	 */
	public void setTargetLevel(Level targetLevel) {
		this.targetLevel = targetLevel;
	}
}

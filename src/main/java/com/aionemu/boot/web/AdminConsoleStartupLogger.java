package com.aionemu.boot.web;

import com.aionemu.boot.i18n.I18n;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 管理控制台启动日志：在上下文就绪后报告控制台是否启用、监听地址与端口。
 * Admin console startup log: reports whether the console is enabled, its bind address and its port once the
 * context is ready.
 * <p>
 * 判定「是否启动 Web 层」必须发生在 Spring 上下文之前，因此报告动作放在这里：此时日志与本地化消息都已
 * 就绪。
 * <p>
 * The decision to start the web layer must happen before the Spring context exists, so the report happens
 * here, where both the logging and the localized messages are ready.
 *
 * @author AionEmu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminConsoleStartupLogger {

	/**
	 * 控制台设置。
	 * Console settings.
	 */
	private final AdminConsoleSettings settings;

	/**
	 * 上下文就绪后报告控制台状态。
	 * Reports the console state once the context is ready.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void logConsoleState() {
		if (!settings.enabled()) {
			log.info(I18n.get("console.web.disabled"));
			return;
		}
		log.info(I18n.get("console.web.config_loaded", settings.enabled(), settings.address(), settings.port()));
	}
}

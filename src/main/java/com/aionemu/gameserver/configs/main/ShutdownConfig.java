package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 服务器关闭/重启钩子相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Server shutdown/reboot hook related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 *
 * @author lord_rex
 */
@Component
@ConfigurationProperties(prefix = "gameserver.shutdown")
public class ShutdownConfig {

	/**
	 * 关闭钩子模式。
	 * Shutdown hook mode.
	 */
	@Property(key = "gameserver.shutdown.mode", defaultValue = "1")
	public static int HOOK_MODE;

	/**
	 * 关闭钩子延迟（秒）。
	 * Shutdown hook delay in seconds.
	 */
	@Property(key = "gameserver.shutdown.delay", defaultValue = "60")
	public static int HOOK_DELAY;

	/**
	 * 关闭公告广播间隔（秒）。
	 * Shutdown announce interval in seconds.
	 */
	@Property(key = "gameserver.shutdown.interval", defaultValue = "1")
	public static int ANNOUNCE_INTERVAL;

	/**
	 * 是否启用安全重启模式。
	 * Whether safe reboot mode is enabled.
	 */
	@Property(key = "gameserver.shutdown.safereboot", defaultValue = "true")
	public static boolean SAFE_REBOOT;

	/**
	 * 关闭时是否清理全部 NPC。
	 * Whether to despawn all NPCs during shutdown.
	 */
	@Property(key = "gameserver.shutdown.despnpcs", defaultValue = "false")
	public static boolean DESPAWN_NPCS;

	public int getMode() {
		return HOOK_MODE;
	}

	public void setMode(int mode) {
		HOOK_MODE = mode;
	}

	public int getDelay() {
		return HOOK_DELAY;
	}

	public void setDelay(int delay) {
		HOOK_DELAY = delay;
	}

	public int getInterval() {
		return ANNOUNCE_INTERVAL;
	}

	public void setInterval(int interval) {
		ANNOUNCE_INTERVAL = interval;
	}

	public boolean isSafereboot() {
		return SAFE_REBOOT;
	}

	public void setSafereboot(boolean safereboot) {
		SAFE_REBOOT = safereboot;
	}

	public boolean isDespnpcs() {
		return DESPAWN_NPCS;
	}

	public void setDespnpcs(boolean despnpcs) {
		DESPAWN_NPCS = despnpcs;
	}
}

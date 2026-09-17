package com.aionemu.gameserver.network;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.PropertiesUtils;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.SecurityConfig;

/**
 * 客户端包洪泛过滤器：按 opcode 配置最小间隔，支持热重载。
 * Client packet flood filter: per-opcode min intervals with hot reload support.
 *
 * @author KID
 */
@Slf4j
public class PacketFloodFilter {

	private static volatile ObjectProvider<PacketFloodFilter> instanceProvider;


	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static PacketFloodFilter getInstance() {
		ObjectProvider<PacketFloodFilter> provider = instanceProvider;
		PacketFloodFilter provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("PacketFloodFilter 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring ObjectProvider，供 DI 覆盖静态单例。
	 * Injects Spring ObjectProvider to override the static singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<PacketFloodFilter> provider) {
		instanceProvider = provider;
	}

	/**
	 * 按 opcode 索引的最小请求间隔（毫秒）；0 表示不限制。
	 * Min request interval ms by opcode; 0 means unlimited.
	 */
	private volatile int[] packets = new int[0x2ff];

	/**
	 * 构造并加载配置。
	 * Constructs and loads configuration.
	 */
	public PacketFloodFilter() {
		reload();
	}

	/**
	 * 重新加载 pff.properties；加载失败时保留原表。
	 * Reloads pff.properties; keeps previous table on load failure.
	 */
	public void reload() {
		if (SecurityConfig.PFF_ENABLE) {
			int cnt = 0;
			int[] reloadedPackets = new int[0x2ff];
			try {
				java.util.Properties props = PropertiesUtils.load(Config.configFile("administration/pff.properties").getPath());
				for (Object key : props.keySet()) {
					String str = (String) key;
					reloadedPackets[Integer.decode(str)] = Integer.valueOf(props.getProperty(str).trim());
					cnt++;
				}
			} catch (IOException e) {
				log.error(I18n.get("log.ee4945d40d71", e));
				return;
			}
			packets = reloadedPackets;
			log.info(I18n.get("log.20b577511b63", cnt));
		} else {
			packets = new int[0x2ff];
			// ponytail: 0 条可能是配置为空也可能 PFF_ENABLE=false，INFO 无信息量，移除
		}
	}

	/**
	 * 返回当前 opcode 间隔表（热重载后引用可能变化）。
	 * Returns the current opcode interval table (reference may change after reload).
	 *
	 * interval array
	 */
	public final int[] getPackets() {
		return this.packets;
	}
}

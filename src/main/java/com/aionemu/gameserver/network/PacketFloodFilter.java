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
	 * 获取过滤器单例（优先 Spring Provider）。
	 * Returns the filter singleton (prefers Spring provider).
	 *
	 * @return 过滤器实例 / filter instance
	 */
	public static PacketFloodFilter getInstance() {
		ObjectProvider<PacketFloodFilter> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
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
				log.error(I18n.get("log.ee4945d40d71", e), e);
				return;
			}
			packets = reloadedPackets;
			log.info(I18n.get("log.20b577511b63", cnt));
		} else {
			packets = new int[0x2ff];
			log.info(I18n.get("log.c044ce589649"));
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

	private static final class SingletonHolder {
		private static final PacketFloodFilter instance = new PacketFloodFilter();
	}
}

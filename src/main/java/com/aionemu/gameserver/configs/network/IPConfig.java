package com.aionemu.gameserver.configs.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import com.aionemu.commons.network.IPRange;

/**
 * 加载游戏服对外地址。
 * Loads the game-server public address.
 *
 * @author Taran, SoulKeeper
 */
public class IPConfig {
	/**
	 * 默认对外地址字节。
	 * Default public address bytes.
	 */
	private static byte[] defaultAddress;

	/**
	 * 加载 IP 配置（支持启动覆盖项）。
	 * Loads IP configuration (supports boot overrides).
	 */
	public static void load() {
		String address = firstNonBlank(
			NetworkConfig.PUBLIC_ADDRESS,
			"127.0.0.1"
		);
		try {
			defaultAddress = InetAddress.getByName(address).getAddress();
		} catch (UnknownHostException e) {
			throw new Error("Can't resolve game server address: " + address, e);
		}
	}

	/**
	 * 返回首个非空白字符串。
	 * Returns the first non-blank string.
	 */
	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return null;
	}

	/**
	 * 返回 IP 段映射列表。
	 * Returns the list of IP ranges.
	 *
	 * @return IP 段映射列表 / List of IP ranges
	 */
	public static List<IPRange> getRanges() {
		return List.of();
	}

	/**
	 * 返回默认对外地址。
	 * Returns the default public address.
	 *
	 * @return 默认地址字节 / Default address bytes
	 */
	public static byte[] getDefaultAddress() {
		return defaultAddress;
	}
}

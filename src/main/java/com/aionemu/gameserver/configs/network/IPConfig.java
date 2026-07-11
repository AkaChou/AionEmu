package com.aionemu.gameserver.configs.network;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.aionemu.commons.network.IPRange;
import com.aionemu.gameserver.configs.Config;

/**
 * 读取 IPConfig.xml，按客户端来源网段映射游戏服对外地址。
 * Loads IPConfig.xml and maps game-server public addresses by client IP ranges.
 *
 * @author Taran, SoulKeeper
 */
@Slf4j
public class IPConfig {
	/**
	 * 全部 IP 段映射列表。
	 * List of all IP ranges.
	 */
	private static final List<IPRange> ranges = new ArrayList<IPRange>();
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
		try {
			ranges.clear();
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(Config.configFile("network/ipconfig.xml"), new DefaultHandler() {

				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes)
						throws SAXException {

					if (qName.equals("ipconfig")) {
						String defaultAddressValue = firstNonBlank(
							Config.bootOverride("gameserver.network.address"),
							Config.bootOverride("gameserver.network.ipconfig.default"),
							attributes.getValue("default")
						);
						try {
							defaultAddress = InetAddress.getByName(defaultAddressValue).getAddress();
						} catch (UnknownHostException e) {
							throw new RuntimeException(
									"Failed to resolve DSN for address: " + defaultAddressValue, e);
						}
					} else if (qName.equals("iprange")) {
						String min = attributes.getValue("min");
						String max = attributes.getValue("max");
						String address = attributes.getValue("address");
						IPRange ipRange = new IPRange(min, max, address);
						ranges.add(ipRange);
					}
				}
			});
		} catch (Exception e) {
			log.error(I18n.get("log.2fb10c85ccbb", e));
			throw new Error("Can't load ipConfig", e);
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
	 * list of IP ranges
	 */
	public static List<IPRange> getRanges() {
		return ranges;
	}

	/**
	 * 返回默认对外地址。
	 * Returns the default public address.
	 *
	 * @return 默认地址字节 / default address
	 */
	public static byte[] getDefaultAddress() {
		return defaultAddress;
	}
}

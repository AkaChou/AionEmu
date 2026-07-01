/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.configs.network;

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
 * @author Taran, SoulKeeper Class that is designed to read IPConfig.xml
 */
@Slf4j
public class IPConfig {

	/**
	 * Location of config file
	 */
	/**
	 * List of all ip ranges
	 */
	private static final List<IPRange> ranges = new ArrayList<IPRange>();
	/**
	 * Default address
	 */
	private static byte[] defaultAddress;

	/**
	 * Method that loads IPConfig
	 */
	public static void load() {
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(Config.configFile("network/ipconfig.xml"), new DefaultHandler() {

				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes)
						throws SAXException {

					if (qName.equals("ipconfig")) {
						String defaultAddressValue = Config.bootOverride("gameserver.network.ipconfig.default");
						if (defaultAddressValue == null || defaultAddressValue.isBlank()) {
							defaultAddressValue = attributes.getValue("default");
						}
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
			log.error("Critical error while parsing ipConfig", e);
			throw new Error("Can't load ipConfig", e);
		}
	}

	/**
	 * Returns list of ip ranges
	 * 
	 * @return list of ip ranges
	 */
	public static List<IPRange> getRanges() {
		return ranges;
	}

	/**
	 * Returns default address
	 * 
	 * @return default address
	 */
	public static byte[] getDefaultAddress() {
		return defaultAddress;
	}
}

package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.aionemu.gameserver.dataholders.PetDopingData;
import com.aionemu.gameserver.dataholders.PetMerchandData;
import com.aionemu.gameserver.model.templates.pet.PetDopingEntry;
import com.aionemu.gameserver.model.templates.pet.PetMerchandEntry;

/**
 * 从 XML 加载宠物定义（药物强化与商品价格倍率）。
 * Loads pet definitions (doping items and merchant price rates) from XML.
 */
final class PetDefinitionLoader {

	static Result load(File file) {
		List<PetDopingEntry> dopings = new ArrayList<>();
		List<PetMerchandEntry> merchants = new ArrayList<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			int record = 0;
			int id = 0;
			boolean drink = false;
			boolean food = false;
			int scroll = 0;
			int ratePrice = 0;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					String name = reader.getLocalName();
					if (name.equals("toypet_doping")) {
						record = 1;
						id = scroll = 0;
						drink = food = false;
					} else if (name.equals("toypet_merchant")) {
						record = 2;
						id = ratePrice = 0;
					} else if (record != 0 && name.equals("id")) {
						id = parseInt(reader.getElementText(), "pet definition id");
					} else if (record == 1 && name.equals("use_doping_drink")) {
						drink = parseBoolean(reader.getElementText());
					} else if (record == 1 && name.equals("use_doping_food")) {
						food = parseBoolean(reader.getElementText());
					} else if (record == 1 && name.equals("use_doping_scroll")) {
						scroll = parseInt(reader.getElementText(), "pet doping scroll count");
					} else if (record == 2 && name.equals("rate_price")) {
						ratePrice = parseInt(reader.getElementText(), "pet merchant price rate");
					}
				} else if (event == XMLStreamConstants.END_ELEMENT) {
					if (reader.getLocalName().equals("toypet_doping")) {
						dopings.add(new PetDopingEntry((short) id, drink, food, scroll));
						record = 0;
					} else if (reader.getLocalName().equals("toypet_merchant")) {
						merchants.add(new PetMerchandEntry(id, ratePrice));
						record = 0;
					}
				}
			}
			reader.close();
			if (dopings.isEmpty() || merchants.isEmpty()) {
				throw new IllegalStateException("Missing pet doping or merchant definitions");
			}
			return new Result(new PetDopingData(dopings), new PetMerchandData(merchants));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load pet definitions from " + file.getPath(), e);
		}
	}

	private static int parseInt(String value, String field) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Invalid " + field + ": " + value, e);
		}
	}

	private static boolean parseBoolean(String value) {
		return value.equalsIgnoreCase("true") || value.equals("1");
	}

	record Result(PetDopingData doping, PetMerchandData merchant) {
	}
}

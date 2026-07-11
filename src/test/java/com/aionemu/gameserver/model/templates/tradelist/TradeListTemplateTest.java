package com.aionemu.gameserver.model.templates.tradelist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Test;

class TradeListTemplateTest {

	@Test
	void supportsLegacyAndExplicitApBuyPriceRates() throws Exception {
		assertEquals(290, read("<purchase_list_template npc_id=\"1\" buy_price_rate=\"290\"/>").getApBuyPriceRate());
		assertEquals(420,
				read("<purchase_list_template npc_id=\"1\" buy_price_rate=\"290\" ap_buy_price_rate=\"420\"/>")
						.getApBuyPriceRate());
	}

	private static TradeListTemplate read(String xml) throws Exception {
		return JAXBContext.newInstance(TradeListTemplate.class).createUnmarshaller()
				.unmarshal(new StreamSource(new StringReader(xml)), TradeListTemplate.class).getValue();
	}
}

package com.aionemu.gameserver.model.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;

class DropModifiersTest {

	@Test
	void appliesBoostOnceAndLevelReductionOnlyWhenRequested() {
		DropModifiers modifiers = new DropModifiers();
		modifiers.setBoostDropRate(2f);
		modifiers.setReductionDropRate(0.5f);

		assertEquals(10f, modifiers.calculateDropChance(10f, true));
		assertEquals(20f, modifiers.calculateDropChance(10f, false));
	}

	@Test
	void capsBoostedChanceAtOneHundredPercent() {
		DropModifiers modifiers = new DropModifiers();
		modifiers.setBoostDropRate(30f);

		assertEquals(100f, modifiers.calculateDropChance(20f, true));
		assertEquals(100f, modifiers.calculateDropChance(7.8f, true));
	}

	@Test
	void itemNoReduceOverridesGroupLevelReduction() throws Exception {
		DropGroup group = JAXBContext.newInstance(DropGroup.class)
				.createUnmarshaller()
				.unmarshal(new StreamSource(new StringReader("""
						<dropGroup>
							<drop item_id="1" chance="25"/>
							<drop item_id="2" chance="25" no_reduce="true"/>
						</dropGroup>
						""")), DropGroup.class)
				.getValue();
		DropModifiers modifiers = new DropModifiers();
		modifiers.setBoostDropRate(1f);
		modifiers.setReductionDropRate(0f);

		assertEquals(0f, group.calculateEffectiveChance(group.getDrop().get(0), modifiers));
		assertEquals(25f, group.calculateEffectiveChance(group.getDrop().get(1), modifiers));
	}
}

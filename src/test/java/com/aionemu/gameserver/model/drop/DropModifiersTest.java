package com.aionemu.gameserver.model.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

	@Test
	void appliesNpcCommonDropAdjustmentBeforeDropModifiers() throws Exception {
		DropGroup group = JAXBContext.newInstance(DropGroup.class)
				.createUnmarshaller()
				.unmarshal(new StreamSource(new StringReader("""
						<dropGroup drop_group_adjustment="200">
							<drop item_id="1" chance="10"/>
						</dropGroup>
						""")), DropGroup.class)
				.getValue();
		group.setChanceMultiplier(1.25f);
		DropModifiers modifiers = new DropModifiers();
		modifiers.setBoostDropRate(1f);
		modifiers.setReductionDropRate(1f);

		assertEquals(25f, group.calculateEffectiveChance(group.getDrop().getFirst(), modifiers));
	}

	@Test
	void kinahCombinesDedicatedRateWithOnlyTheExtraOrdinaryBoost() {
		DropModifiers modifiers = new DropModifiers();
		modifiers.setReductionDropRate(0f);

		modifiers.setBoostDropRate(1f);
		assertEquals(1_000, modifiers.calculateKinahAmount(1_000, 1f));

		modifiers.setBoostDropRate(2f);
		assertEquals(31_000, modifiers.calculateKinahAmount(1_000, 30f));

		modifiers.setBoostDropRate(30f);
		assertEquals(30_000, modifiers.calculateKinahAmount(1_000, 1f));
	}

	@Test
	void kinahRoundsBothSharesSeparatelyAndNeverSubtractsForRatesBelowOne() {
		DropModifiers modifiers = new DropModifiers();
		modifiers.setBoostDropRate(0.5f);
		assertEquals(1_000, modifiers.calculateKinahAmount(1_000, 1f));

		modifiers.setBoostDropRate(1.5f);
		assertEquals(4, modifiers.calculateKinahAmount(3, 0.5f));
	}

	@Test
	void rejectsInvalidDedicatedKinahRates() {
		DropModifiers modifiers = new DropModifiers();
		modifiers.setBoostDropRate(1f);

		assertThrows(IllegalArgumentException.class, () -> modifiers.calculateKinahAmount(1_000, 0f));
		assertThrows(IllegalArgumentException.class, () -> modifiers.calculateKinahAmount(1_000, -1f));
		assertThrows(IllegalArgumentException.class, () -> modifiers.calculateKinahAmount(1_000, Float.NaN));
	}
}

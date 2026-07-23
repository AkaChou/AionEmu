package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringReader;
import java.nio.file.Path;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;

class ScalingDropDataTest {

	@Test
	void loadsNpcSetsAndWeightedItems() throws Exception {
		ScalingDropData data = (ScalingDropData) JAXBContext.newInstance(ScalingDropData.class)
			.createUnmarshaller().unmarshal(new StringReader("""
				<scaling_drops>
				  <npc id="835733" source="retail">
				    <set rate="200" min_level="1" max_level="75">
				      <item id="10" count="1" weight="1000"/>
				      <item id="11" count="2" weight="5000"/>
				    </set>
				  </npc>
				</scaling_drops>
				"""));
		data.rebuildIndex();

		ScalingDropData.NpcScalingDrop npc = data.getDrop(835733);
		assertNotNull(npc);
		assertEquals(1, npc.getSets().size());
		assertEquals(200, npc.getSets().getFirst().getRate());
		assertEquals(5000, npc.getSets().getFirst().getItems().get(1).getWeight());
	}

	@Test
	void generatedRetailDataContainsAllReferencedNpcs() {
		NpcDropData data = NpcDropData.loadEager(
			Path.of("src/main/resources/aion/definitions/compact/npc_drops").toFile());

		assertNotNull(data.getScalingDrop(219640));
		assertEquals(4, data.getScalingDrop(835733).getSets().size());
		assertEquals(1000, data.getScalingDrop(835733).getSets().get(0).getRate());
		assertEquals(1000, data.getScalingDrop(835733).getSets().get(1).getRate());
		assertEquals(1000, data.getScalingDrop(835733).getSets().get(2).getRate());
		assertEquals(200, data.getScalingDrop(835733).getSets().get(3).getRate());
		assertEquals(185000295, data.getScalingDrop(246327).getSets().getFirst().getItems().getFirst().getId());
	}
}

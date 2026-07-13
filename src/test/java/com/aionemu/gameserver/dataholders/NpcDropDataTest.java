package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;

import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropGroup;
import com.aionemu.gameserver.model.drop.NpcDrop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NpcDropDataTest {

	@TempDir
	Path tempDir;

	@Test
	void eagerLoaderExpandsCommonDropGroupsFromSharedDefinitions() throws Exception {
		writeDrops("common_drop_groups.xml", """
			<common_drop_groups>
				<group name="COMMON_A" use_category="false">
					<drop item_id="222" min_amount="1" max_amount="1" chance="100"/>
				</group>
			</common_drop_groups>
			""");
		writeDrops("npc_drops_part_001.xml", """
			<npc_drops>
				<npc_drop npc_id="100">
					<drop_group name="base">
						<drop item_id="111" min_amount="1" max_amount="1" chance="100"/>
					</drop_group>
					<common_drop_group name="COMMON_A"/>
				</npc_drop>
			</npc_drops>
			""");
		NpcDropData data = NpcDropData.loadEager(tempDir.toFile());

		assertEquals(List.of(111, 222), itemIds(data.getDrop(100)));
		assertTrue(data.getDrop(100).getDropGroup().get(1).isUseLevelBasedChanceReduction());
	}

	@Test
	void commonDropAdjustmentIsAppliedWhenExpandingNpcReference() throws Exception {
		writeDrops("common_drop_groups.xml", """
			<common_drop_groups>
				<group name="COMMON_A">
					<drop item_id="222" min_amount="1" max_amount="1" chance="10"/>
				</group>
			</common_drop_groups>
			""");
		writeDrops("npc_drops_part_001.xml", """
			<npc_drops>
				<npc_drop npc_id="100">
					<common_drop_group name="COMMON_A" common_drop_adjustment="250"/>
				</npc_drop>
			</npc_drops>
			""");

		NpcDropData data = NpcDropData.loadEager(tempDir.toFile());

		assertEquals(2.5f, data.getDrop(100).getDropGroup().getFirst().getChanceMultiplier());
	}

	@Test
	void ragnarokKeepsRetailRingAdjustmentInCompactData() {
		NpcDropData data = NpcDropData.loadEager(
			Path.of("src/main/resources/aion/definitions/compact/npc_drops").toFile());
		NpcDrop ragnarok = data.getDrop(216576);
		assertNotNull(ragnarok);

		DropGroup ring = ragnarok.getDropGroup().stream()
			.filter(group -> "DF4_ACCESSORY_RING_D_N_E1_55A".equals(group.getGroupName()))
			.findFirst()
			.orElseThrow();
		Drop ringDrop = ring.getDrop().getFirst();
		assertEquals(0.00946f, ringDrop.getChance(), 0.000001f);
		assertEquals(2664f, ring.getChanceMultiplier(), 0.0001f);
		assertEquals(25.20144f, ring.getAdjustedChance(ringDrop), 0.0001f);
	}

	@Test
	void eagerLoaderIndexesNpcIdsForLookup() throws Exception {
		writeDrops("npc_drops_part_001.xml", """
			<npc_drops>
				<npc_drop npc_id="100">
					<drop_group name="base">
						<drop item_id="111" min_amount="1" max_amount="1" chance="100"/>
					</drop_group>
				</npc_drop>
				<npc_drop npc_id="200">
					<drop_group name="base">
						<drop item_id="222" min_amount="1" max_amount="2" chance="50"/>
					</drop_group>
				</npc_drop>
			</npc_drops>
			""");
		NpcDropData data = NpcDropData.loadEager(tempDir.toFile());

		assertEquals(2, data.size());
		assertEquals(100, data.getDrop(100).getNpcId());
		assertEquals(List.of(111), itemIds(data.getDrop(100)));
		assertSame(data.getDrop(100), indexedDrops(data).get(100));
	}

	@Test
	void eagerLoaderCreatesJaxbContextWhenThreadContextClassLoaderCannotSeeJaxbRuntime() throws Exception {
		writeDrops("npc_drops_part_001.xml", """
			<npc_drops>
				<npc_drop npc_id="100">
					<drop_group name="base">
						<drop item_id="111" min_amount="1" max_amount="1" chance="100"/>
					</drop_group>
				</npc_drop>
			</npc_drops>
			""");
		Thread thread = Thread.currentThread();
		ClassLoader originalClassLoader = thread.getContextClassLoader();
		thread.setContextClassLoader(ClassLoader.getPlatformClassLoader());
		try {
			NpcDropData data = NpcDropData.loadEager(tempDir.toFile());

			assertEquals(100, data.getDrop(100).getNpcId());
		} finally {
			thread.setContextClassLoader(originalClassLoader);
		}
	}

	@Test
	void eagerJaxbLoadingBuildsNpcIdLookupMap() throws Exception {
		NpcDropData data = (NpcDropData) JAXBContext.newInstance(NpcDropData.class)
			.createUnmarshaller()
			.unmarshal(new StringReader("""
				<npc_drops>
					<npc_drop npc_id="100">
						<drop_group name="base">
							<drop item_id="111" each_member="true"/>
						</drop_group>
					</npc_drop>
				</npc_drops>
				"""));

		assertEquals(1, data.size());
		assertEquals(List.of(111), itemIds(data.getDrop(100)));
		assertSame(data.getDrop(100), indexedDrops(data).get(100));
	}

	@Test
	void dropGroupsUseAionServerSelectionAttributes() throws Exception {
		NpcDropData data = (NpcDropData) JAXBContext.newInstance(NpcDropData.class)
			.createUnmarshaller()
			.unmarshal(new StringReader("""
				<npc_drops>
					<npc_drop npc_id="100">
						<drop_group max_items="2" level_based_chance_reduction="true" use_category="true">
							<drop item_id="111" each_member="true"/>
						</drop_group>
					</npc_drop>
				</npc_drops>
				"""));

		DropGroup group = data.getDrop(100).getDropGroup().getFirst();
		assertEquals(2, group.getMaxItems());
		assertTrue(group.isUseLevelBasedChanceReduction());
		assertEquals(1, group.getDrop().getFirst().getMinAmount());
		assertEquals(1, group.getDrop().getFirst().getMaxAmount());
		assertTrue(group.getDrop().getFirst().isEachMember());
	}

	@Test
	void npcDropSchemaAcceptsAionServerSelectionAttributes() throws Exception {
		var schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(Path.of("src/main/resources/aion/definitions/schemas/npc_drops.xsd").toFile());

		schema.newValidator().validate(new StreamSource(new StringReader("""
				<npc_drops>
				<npc_drop npc_id="100">
					<drop_group max_items="2" level_based_chance_reduction="true" drop_group_adjustment="250"/>
					<common_drop_group name="COMMON_A" common_drop_adjustment="250"/>
				</npc_drop>
				</npc_drops>
				""")));
	}

	@Test
	void legacyDropGroupsWithoutUseCategoryStillReduceChanceByLevel() throws Exception {
		NpcDropData data = (NpcDropData) JAXBContext.newInstance(NpcDropData.class)
			.createUnmarshaller()
			.unmarshal(new StringReader("""
				<npc_drops>
					<npc_drop npc_id="100">
						<drop_group>
							<drop item_id="111"/>
						</drop_group>
					</npc_drop>
				</npc_drops>
				"""));

		assertTrue(data.getDrop(100).getDropGroup().getFirst().isUseLevelBasedChanceReduction());
	}

	@Test
	void eagerJaxbLoadingExpandsCommonDropGroups() throws Exception {
		NpcDropData data = (NpcDropData) JAXBContext.newInstance(NpcDropData.class)
			.createUnmarshaller()
			.unmarshal(new StringReader("""
				<npc_drops>
					<group name="COMMON_A" use_category="false">
						<drop item_id="222" min_amount="1" max_amount="1" chance="100"/>
					</group>
					<npc_drop npc_id="100">
						<drop_group name="base">
							<drop item_id="111" min_amount="1" max_amount="1" chance="100"/>
						</drop_group>
						<common_drop_group name="COMMON_A"/>
					</npc_drop>
				</npc_drops>
				"""));

		assertEquals(List.of(111, 222), itemIds(data.getDrop(100)));
	}

	@Test
	void duplicateNpcDropsAreMergedWithLaterItemsReplacingEarlierDuplicates() throws Exception {
		writeDrops("npc_drops_part_001.xml", """
			<npc_drops>
				<npc_drop npc_id="100">
					<drop_group name="base" use_category="false">
						<drop item_id="111" min_amount="1" max_amount="1" chance="10"/>
						<drop item_id="222" min_amount="1" max_amount="1" chance="20"/>
					</drop_group>
				</npc_drop>
			</npc_drops>
			""");
		writeDrops("npc_drops_part_002.xml", """
			<npc_drops>
				<npc_drop npc_id="100">
					<drop_group name="base" use_category="false">
						<drop item_id="222" min_amount="9" max_amount="9" chance="90"/>
						<drop item_id="333" min_amount="1" max_amount="1" chance="30"/>
					</drop_group>
					<drop_group name="extra">
						<drop item_id="444" min_amount="1" max_amount="1" chance="40"/>
					</drop_group>
				</npc_drop>
			</npc_drops>
			""");
		NpcDropData data = NpcDropData.loadEager(tempDir.toFile());

		NpcDrop drop = data.getDrop(100);

		assertEquals(List.of(111, 222, 333, 444), itemIds(drop));
		Drop replacement = drop.getDropGroup().get(0).getDrop().get(1);
		assertEquals(9, replacement.getMinAmount());
		assertEquals(90, replacement.getChance());
	}

	private Path writeDrops(String fileName, String xml) throws Exception {
		Path file = tempDir.resolve(fileName);
		Files.writeString(file, xml, StandardCharsets.UTF_8);
		return file;
	}

	private static List<Integer> itemIds(NpcDrop drop) {
		return drop.getDropGroup().stream()
			.map(DropGroup::getDrop)
			.flatMap(List::stream)
			.map(Drop::getItemId)
			.toList();
	}

	@SuppressWarnings("unchecked")
	private static Map<Integer, NpcDrop> indexedDrops(NpcDropData data) throws Exception {
		Field field = NpcDropData.class.getDeclaredField("dropsByNpcId");
		field.setAccessible(true);
		return (Map<Integer, NpcDrop>) field.get(data);
	}
}

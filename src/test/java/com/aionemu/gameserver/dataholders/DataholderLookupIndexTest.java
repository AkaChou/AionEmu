package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import org.junit.jupiter.api.Test;

class DataholderLookupIndexTest {

	@Test
	void challengeQuestLookupsUseQuestIdIndexes() throws Exception {
		ChallengeData data = unmarshal(ChallengeData.class, """
			<challenge_tasks>
				<task id="1">
					<quest id="101" score="1" repeat_count="1"/>
				</task>
				<task id="2">
					<quest id="202" score="1" repeat_count="1"/>
				</task>
			</challenge_tasks>
			""");

		assertEquals(2, data.getTaskByQuestId(202).getId());
		assertEquals(202, data.getQuestByQuestId(202).getId());
		assertEquals(2, mapSize(data, "tasksByQuestId"));
		assertEquals(2, mapSize(data, "questsById"));
	}

	@Test
	void assemblyItemLookupUsesIdIndex() throws Exception {
		AssemblyItemsData data = unmarshal(AssemblyItemsData.class, """
			<assembly_items>
				<item id="100"/>
				<item id="200"/>
			</assembly_items>
			""");

		assertEquals(200, data.getAssemblyItem(200).getId());
		assertEquals(2, intMapSize(data, "itemsById"));
	}

	@Test
	void itemSkillEnhanceLookupUsesIdIndex() throws Exception {
		ItemSkillEnhanceData data = unmarshal(ItemSkillEnhanceData.class, """
			<item_skill_enhances>
				<item_skill_enhance id="100"/>
				<item_skill_enhance id="200"/>
			</item_skill_enhances>
			""");

		assertEquals(200, data.getSkillEnhance(200).getId());
		assertEquals(2, intMapSize(data, "enhanceSkillsById"));
	}

	@Test
	void itemSkillEnhanceLookupUsesClassWhenIdsRepeat() throws Exception {
		ItemSkillEnhanceData data = unmarshal(ItemSkillEnhanceData.class, """
			<item_skill_enhances>
				<item_skill_enhance id="100" player_class="TEMPLAR" skill_id="3116"/>
				<item_skill_enhance id="100" player_class="CLERIC" skill_id="4037"/>
				<item_skill_enhance id="200" player_class="ALL" skill_id="784"/>
			</item_skill_enhances>
			""");

		assertEquals(4037, data.getSkillEnhance(100, PlayerClass.CLERIC).getSkillId().get(0));
		assertEquals(784, data.getSkillEnhance(200, PlayerClass.RANGER).getSkillId().get(0));
		assertNull(data.getSkillEnhance(100, PlayerClass.RANGER));
	}

	@Test
	void multiReturnLookupUsesIdIndex() throws Exception {
		MultiReturnItemData data = unmarshal(MultiReturnItemData.class, """
			<multi_returns>
				<item id="100"/>
				<item id="200"/>
			</multi_returns>
			""");

		assertEquals(200, data.getMultiReturnById(200).getId());
		assertEquals(2, intMapSize(data, "itemsById"));
	}

	@Test
	void vortexLookupUsesInvasionWorldIdIndex() throws Exception {
		VortexData data = unmarshal(VortexData.class, """
			<dimensional_vortex>
				<vortex_location id="1" defends_race="ELYOS" offence_race="ASMODIANS">
					<home_point map="1001" x="1" y="1" z="1" h="0"/>
					<resurrection_point map="1001" x="1" y="1" z="1" h="0"/>
					<start_point map="6001" x="1" y="1" z="1" h="0"/>
				</vortex_location>
				<vortex_location id="2" defends_race="ASMODIANS" offence_race="ELYOS">
					<home_point map="1002" x="1" y="1" z="1" h="0"/>
					<resurrection_point map="1002" x="1" y="1" z="1" h="0"/>
					<start_point map="6002" x="1" y="1" z="1" h="0"/>
				</vortex_location>
			</dimensional_vortex>
			""");

		assertEquals(2, data.getVortexLocation(6002).getId());
		assertEquals(2, mapSize(data, "vortexByInvasionWorldId"));
	}

	@Test
	void shugoSweepRewardLookupUsesCompositeIndex() throws Exception {
		ShugoSweepRewardData data = unmarshal(ShugoSweepRewardData.class, """
			<shugo_sweeps>
				<shugo_sweep board_id="1" reward_num="1" item_id="100" count="1"/>
				<shugo_sweep board_id="2" reward_num="3" item_id="200" count="1"/>
			</shugo_sweeps>
			""");

		assertEquals(200, data.getRewardBoard(2, 3).getItemId());
		assertEquals(2, mapSize(data, "rewardsByBoardAndNum"));
	}

	@Test
	void instanceExitLookupUsesWorldIdIndex() throws Exception {
		InstanceExitData data = unmarshal(InstanceExitData.class, """
			<instance_exits>
				<instance_exit instance_id="100" exit_world="1" race="ELYOS" x="1" y="1" z="1" h="0"/>
				<instance_exit instance_id="200" exit_world="2" race="PC_ALL" x="1" y="1" z="1" h="0"/>
			</instance_exits>
			""");

		assertEquals(2, data.getInstanceExit(200, Race.ASMODIANS).getExitWorld());
		assertEquals(2, mapSize(data, "exitsByWorldId"));
	}

	@Test
	void enteredInstancesHaveConfiguredExits() throws Exception {
		String xml = Files.readString(Path.of("src/main/resources/aion/definitions/instances/instance_exit/instance_exit.xml"));
		InstanceExitData data = unmarshal(InstanceExitData.class, xml);

		assertExit(data, 310100000, Race.ELYOS, 210040000);
		assertExit(data, 310040000, Race.ELYOS, 210020000);
		assertExit(data, 320040000, Race.ASMODIANS, 220020000);
		assertExit(data, 310070000, Race.ELYOS, 210030000);
		assertExit(data, 320070000, Race.ASMODIANS, 220010000);
		assertExit(data, 301340000, Race.ELYOS, 600090000);
		assertExit(data, 301340000, Race.ASMODIANS, 600090000);
		assertExit(data, 301690000, Race.ELYOS, 210100000);
		assertExit(data, 301690000, Race.ASMODIANS, 220110000);
		assertExit(data, 310010000, Race.ELYOS, 210010000);
		assertExit(data, 310120000, Race.ELYOS, 110010000);
		assertExit(data, 320010000, Race.ASMODIANS, 220010000);
		assertExit(data, 320020000, Race.ASMODIANS, 220010000);
		assertExit(data, 320140000, Race.ASMODIANS, 120010000);
		assertExit(data, 301700000, Race.ELYOS, 110010000);
		assertExit(data, 301700000, Race.ASMODIANS, 120010000);
		assertExit(data, 600080000, Race.ELYOS, 110010000);
		assertExit(data, 600080000, Race.ASMODIANS, 120010000);
	}

	private static void assertExit(InstanceExitData data, int instanceId, Race race, int exitWorld) {
		assertNotNull(data.getInstanceExit(instanceId, race), () -> "Missing instance exit for " + instanceId + " " + race);
		assertEquals(exitWorld, data.getInstanceExit(instanceId, race).getExitWorld());
	}

	private static <T> T unmarshal(Class<T> type, String xml) throws Exception {
		return type.cast(JAXBContext.newInstance(type).createUnmarshaller().unmarshal(new StringReader(xml)));
	}

	private static int mapSize(Object target, String fieldName) throws Exception {
		return ((Map<?, ?>) fieldValue(target, fieldName)).size();
	}

	private static int intMapSize(Object target, String fieldName) throws Exception {
		return ((IntObjectHashMap<?>) fieldValue(target, fieldName)).size();
	}

	private static Object fieldValue(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}
}

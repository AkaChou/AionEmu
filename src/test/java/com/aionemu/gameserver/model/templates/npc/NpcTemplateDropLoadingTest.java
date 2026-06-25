package com.aionemu.gameserver.model.templates.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcDropData;
import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropGroup;
import com.aionemu.gameserver.model.drop.NpcDrop;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NpcTemplateDropLoadingTest {

	@TempDir
	Path tempDir;
	private final NpcDropData originalDropData = DataManager.NPC_DROP_DATA;

	@AfterEach
	void restoreDropData() {
		DataManager.NPC_DROP_DATA = originalDropData;
	}

	@Test
	void getNpcDropLoadsDropFromLazyDataWithoutStoringItOnTemplate() throws Exception {
		Files.writeString(tempDir.resolve("drops.xml"), """
			<npc_drops>
				<npc_drop npc_id="100">
					<drop_group name="base">
						<drop item_id="111" min_amount="1" max_amount="1" chance="100"/>
					</drop_group>
				</npc_drop>
			</npc_drops>
			""", StandardCharsets.UTF_8);
		DataManager.NPC_DROP_DATA = NpcDropData.loadLazy(tempDir.toFile(), 10, TimeUnit.MINUTES.toMillis(5), () -> 1_000L);
		NpcTemplate template = template(100);

		NpcDrop first = template.getNpcDrop();
		NpcDrop second = template.getNpcDrop();

		assertNotNull(first);
		assertEquals(List.of(111), itemIds(first));
		assertEquals(first, second);
		assertNull(npcDropField(template));
	}

	private static NpcTemplate template(int npcId) throws Exception {
		NpcTemplate template = new NpcTemplate();
		Field field = NpcTemplate.class.getDeclaredField("npcId");
		field.setAccessible(true);
		field.setInt(template, npcId);
		return template;
	}

	private static NpcDrop npcDropField(NpcTemplate template) throws Exception {
		Field field = NpcTemplate.class.getDeclaredField("npcDrop");
		field.setAccessible(true);
		return (NpcDrop) field.get(template);
	}

	private static List<Integer> itemIds(NpcDrop drop) {
		return drop.getDropGroup().stream()
			.map(DropGroup::getDrop)
			.flatMap(List::stream)
			.map(Drop::getItemId)
			.toList();
	}
}

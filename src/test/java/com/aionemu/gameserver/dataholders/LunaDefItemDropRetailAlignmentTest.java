package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.event.EventDrop;
import com.aionemu.gameserver.model.templates.event.EventTemplate;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

/**
 * 污染暗道 Q80881 任务道具掉落的真端对齐闸门。
 * Regression gate for the Contaminated Underpath Q80881 quest-item drop against retail.
 * <p>真端 {@code Map/event/Events/IDLuna_Def_Item_Drop/eventsetup.txt} 把 {@code quest_80881a}（182216110）挂在
 * MAAD-S 死亡后生成的 7 个等级奖励箱上，概率 10000000/10000000（必掉）；任务表本身没有 drop_* 字段。
 * The retail {@code IDLuna_Def_Item_Drop} event script binds {@code quest_80881a} (182216110) to the seven level
 * reward boxes spawned after MAAD-S dies at 10000000/10000000 (guaranteed); the quest table carries no drop_* fields.
 */
class LunaDefItemDropRetailAlignmentTest {

	private static final Path EVENTS_CONFIG = Path.of(
		"src/main/resources/aion/data/static_data/events_config/events_config.xml");
	private static final int QUEST_ITEM = 182216110;
	private static final Set<Integer> BOSS_REWARD_BOXES = Set.of(246159, 246166, 246167, 246168, 246169, 246170, 246171);

	@Test
	void everyBossRewardBoxDropsTheQuestItem() throws Exception {
		List<EventDrop> questDrops = new ArrayList<>();
		for (EventDrop drop : event("Luna Event").EventDrop().getEventDrops()) {
			if (drop.getItemId() == QUEST_ITEM) {
				questDrops.add(drop);
			}
		}
		assertEquals(BOSS_REWARD_BOXES.size(), questDrops.size(),
			"七个等级奖励箱都必须掉落任务道具 / all seven level reward boxes must drop the quest item");
		for (EventDrop drop : questDrops) {
			assertTrue(BOSS_REWARD_BOXES.contains(drop.getNpcId()),
				() -> "任务道具挂到了非奖励箱 NPC 上 / quest item bound to a non-reward-box npc: " + drop.getNpcId());
			// 真端 count 1 / 概率 10000000 满值即必掉。
			// Retail count 1 at the 10000000 cap means guaranteed.
			assertEquals(1, drop.getCount());
			assertEquals(100, drop.getChance());
		}
	}

	private static EventTemplate event(String name) throws Exception {
		// EventData 在 afterUnmarshal 会清空事件列表，这里按流式逐块解组。
		// EventData clears its event list in afterUnmarshal; unmarshal event blocks by hand.
		Unmarshaller unmarshaller = JAXBContext.newInstance(EventTemplate.class).createUnmarshaller();
		try (InputStream in = Files.newInputStream(EVENTS_CONFIG)) {
			XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(in);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && "event".equals(reader.getLocalName())) {
					EventTemplate event = unmarshaller.unmarshal(reader, EventTemplate.class).getValue();
					if (name.equals(event.getName())) {
						reader.close();
						return event;
					}
				}
			}
			reader.close();
		}
		throw new AssertionError("missing event " + name);
	}
}

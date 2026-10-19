package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.templates.event.EventDrop;
import com.aionemu.gameserver.model.templates.event.EventTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.templates.spawns.SpawnMap;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

/**
 * 埃及活动（Egypt Event / Secret of the Ancients Returns）的真端对齐回归闸门。
 * Regression gate for the Egypt event (Egypt Event / Secret of the Ancients Returns) against retail data.
 * <p>真端值取自 {@code 58Server/Map/event/Events/Event_CN_Egypt_*} 与 {@code 58Server/Map/XML/npcs*.xml}；
 * 测试只读仓库内已提交的数据，不依赖工程外部路径。
 * Retail values were read from {@code 58Server/Map/event/Events/Event_CN_Egypt_*} and {@code 58Server/Map/XML/npcs*.xml};
 * the test only reads committed data inside the repository.
 */
class EgyptEventRetailAlignmentTest {

	private static final Path NPC_SHARDS = Path.of("src/main/resources/aion/data/static_data/npcs");
	private static final Path EVENTS_CONFIG = Path.of("src/main/resources/aion/data/static_data/events_config/events_config.xml");
	private static final int SPIRIT_STONE = 186000479;
	private static final int ASSEMBLY_KEY = 188100438;
	private static final int REMOVED_KEY = 188057812;
	private static final int ANUBIS = 247233;
	private static final int ANCIENT_BOX_02 = 247234;
	private static final int ANCIENT_BOX_03 = 247235;
	private static final int EGYPT_PORTAL = 835556;

	@Test
	void questNpcsCanBeTalkedTo() throws Exception {
		// 真端 talking_distance 5.0；缺 talk_info 时本地 getTalkDistance() 回落为 2。
		// Retail talking_distance is 5.0; without talk_info the local getTalkDistance() falls back to 2.
		for (int npcId : new int[] { 835551, 835552, 835553 }) {
			assertEquals(5, npc(npcId).getTalkDistance(), () -> "talking distance of " + npcId);
		}
	}

	@Test
	void fieldObjectsStayUntargetable() throws Exception {
		// 真端 unattackable=0，但部落 Field_Object_All 对 PC 无敌对关系，语义上仍是不可攻击对象。
		// Retail ships unattackable=0, yet Field_Object_All has no hostile relation with PC, i.e. not attackable.
		for (int npcId : new int[] { 835554, 835555, EGYPT_PORTAL }) {
			assertEquals(NpcType.NON_ATTACKABLE, npc(npcId).getNpcType(), () -> "field object " + npcId);
		}
	}

	@Test
	void anubisCarriesRetailBossGauge() throws Exception {
		NpcTemplate anubis = npc(ANUBIS);
		assertEquals(20, anubis.getHpGaugeLevel(), "retail hpgauge_level 20 of 247233");
		assertEquals(1_000_000, anubis.getStatsTemplate().getMaxHp(), "retail max_hp of 247233");
	}

	@Test
	void returnEventDropsOnlyTheSpiritStone() throws Exception {
		// 真端事件表只保留全服随机碎片掉落；安努比斯增补与 minLvl 门槛均不存在。
		// The retail table keeps a single server-wide shard drop; the Anubis add-on and minLvl gates do not exist.
		List<EventDrop> drops = event("Secret of the Ancients Returns").EventDrop().getEventDrops();
		assertEquals(1, drops.size(), "retail return-event drops");

		EventDrop common = drops.get(0);
		assertEquals(SPIRIT_STONE, common.getItemId());
		assertEquals(0, common.getNpcId(), "the shard drop is not bound to Anubis");
		assertEquals(2, common.getChance());
		assertEquals(66, common.getMinDiff());
		assertEquals(75, common.getMaxDiff());
		assertEquals(0, common.getMinLvl(), "the retail drop has no level gate");
	}

	@Test
	void egyptEventDropsFollowTheRetailChain() throws Exception {
		Map<Integer, EventDrop> shards = new HashMap<>();
		Set<Integer> chainSources = new HashSet<>();
		for (EventDrop drop : event("Egypt Event").EventDrop().getEventDrops()) {
			assertTrue(drop.getItemId() != REMOVED_KEY,
				() -> "retail Event_CN_Egypt_Item_Drop never drops " + REMOVED_KEY);
			if (drop.getItemId() == SPIRIT_STONE) {
				shards.put(drop.getNpcId(), drop);
			} else {
				assertEquals(ASSEMBLY_KEY, drop.getItemId(), () -> "unexpected Egypt drop item " + drop.getItemId());
				chainSources.add(drop.getNpcId());
			}
		}
		// 链尾的 NPC 只掉碎片、不再给出下一把钥匙 / the chain tail only drops the shard.
		assertTrue(shards.keySet().containsAll(chainSources), "every chain source also drops the shard");
		assertEquals(chainSources.size() + 1, shards.size(), "the head and every hop drop a shard, the tail has no key");
		assertEquals(37, shards.size(), "retail chain length");
		for (EventDrop shard : shards.values()) {
			// 真端 count 10000000 / 10000000 即必掉 / retail 10000000 out of 10000000 means guaranteed.
			assertEquals(10_000_000L, shard.getCount());
			assertEquals(100, shard.getChance());
		}
	}

	@Test
	void monsterSpawnsBelongToTheEgyptEvent() throws Exception {
		// 真端 Event_CN_Egypt_Monster_Spawn 把安努比斯与古代箱子放在 5 月窗口，本地对应 Egypt Event。
		// Retail Event_CN_Egypt_Monster_Spawn keeps Anubis and the ancient boxes in the May window, i.e. the Egypt Event.
		Map<Integer, Integer> egypt = spawnCounts("Egypt Event");
		assertEquals(2, egypt.getOrDefault(ANUBIS, 0), "Anubis spawn groups");
		assertEquals(2, egypt.getOrDefault(ANCIENT_BOX_02, 0), "Ancient Box 02 spawn groups");
		assertEquals(2, egypt.getOrDefault(ANCIENT_BOX_03, 0), "Ancient Box 03 spawn groups");
		assertEquals(2, egypt.getOrDefault(EGYPT_PORTAL, 0), "Egypt portal spawn groups");

		Map<Integer, Integer> returns = spawnCounts("Secret of the Ancients Returns");
		assertEquals(0, returns.getOrDefault(ANUBIS, 0), "the return event no longer borrows the Egypt monsters");
		assertEquals(0, returns.getOrDefault(ANCIENT_BOX_02, 0));
		assertEquals(0, returns.getOrDefault(ANCIENT_BOX_03, 0));
		assertEquals(0, returns.getOrDefault(EGYPT_PORTAL, 0));
	}

	private static Map<Integer, Integer> spawnCounts(String eventName) throws Exception {
		Map<Integer, Integer> counts = new HashMap<>();
		for (SpawnMap map : event(eventName).getSpawns().getTemplates()) {
			if (map.getSpawns() == null) {
				continue;
			}
			for (Spawn spawn : map.getSpawns()) {
				counts.merge(spawn.getNpcId(), 1, Integer::sum);
			}
		}
		return counts;
	}

	private static EventTemplate event(String name) throws Exception {
		// EventData 会在 afterUnmarshal 里清空事件列表，这里只借用它的 XML 视图，改按流式逐块解组。
		// EventData clears its event list in afterUnmarshal; borrow only its XML shape and unmarshal event blocks by hand.
		Unmarshaller unmarshaller = unmarshaller(EventTemplate.class);
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

	private static NpcTemplate npc(int npcId) throws Exception {
		Path shard = shardOf(npcId);
		try (InputStream in = Files.newInputStream(shard)) {
			NpcData data = (NpcData) unmarshaller(NpcData.class).unmarshal(in);
			NpcTemplate template = data.getNpcTemplate(npcId);
			assertNotNull(template, () -> "missing npc template " + npcId);
			return template;
		}
	}

	private static Unmarshaller unmarshaller(Class<?> type) {
		try {
			return JAXBContext.newInstance(type).createUnmarshaller();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/** 按分片区间定位 NPC 模板文件 / Locates the NPC template shard covering the given id. */
	private static Path shardOf(int npcId) throws Exception {
		try (var files = Files.list(NPC_SHARDS)) {
			return files
				.filter(p -> p.getFileName().toString().matches("npc_template_\\d+_\\d+\\.xml"))
				.filter(p -> {
					String[] bounds = p.getFileName().toString().replace("npc_template_", "").replace(".xml", "").split("_");
					return npcId >= Integer.parseInt(bounds[0]) && npcId <= Integer.parseInt(bounds[1]);
				})
				.findFirst()
				.orElseThrow(() -> new AssertionError("missing npc shard for " + npcId));
		}
	}
}

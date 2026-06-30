package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
	void lazyLoaderIndexesNpcIdsWithoutCachingDrops() throws Exception {
		writeDrops("poeta.xml", """
			<npc_drops>
				<npc_drop npc_id="100">
					<drop_group name="base" use_category="false">
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
		MutableClock clock = new MutableClock(1_000);
		NpcDropData data = NpcDropData.loadLazy(tempDir.toFile(), 10, TimeUnit.MINUTES.toMillis(5), clock::millis);

		assertEquals(2, data.size());
		assertEquals(0, data.cachedDropCount());

		NpcDrop drop = data.getDrop(100);

		assertEquals(100, drop.getNpcId());
		assertEquals(1, data.cachedDropCount());
		assertEquals(List.of(111), itemIds(drop));
	}

	@Test
	void lazyLoaderCreatesJaxbContextWhenThreadContextClassLoaderCannotSeeJaxbRuntime() throws Exception {
		writeDrops("poeta.xml", """
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
			NpcDropData data = NpcDropData.loadLazy(tempDir.toFile(), 10, TimeUnit.MINUTES.toMillis(5), () -> 1_000L);

			assertEquals(100, data.getDrop(100).getNpcId());
		} finally {
			thread.setContextClassLoader(originalClassLoader);
		}
	}

	@Test
	void eagerJaxbLoadingStillIgnoresLazyRuntimeFields() throws Exception {
		NpcDropData data = (NpcDropData) JAXBContext.newInstance(NpcDropData.class)
			.createUnmarshaller()
			.unmarshal(new StringReader("""
				<npc_drops>
					<npc_drop npc_id="100">
						<drop_group name="base">
							<drop item_id="111" min_amount="1" max_amount="1" chance="100"/>
						</drop_group>
					</npc_drop>
				</npc_drops>
				"""));

		assertEquals(1, data.size());
		assertEquals(List.of(111), itemIds(data.getDrop(100)));
	}

	@Test
	void duplicateNpcDropsAreMergedWithLaterItemsReplacingEarlierDuplicates() throws Exception {
		writeDrops("a.xml", """
			<npc_drops>
				<npc_drop npc_id="100">
					<drop_group name="base" use_category="false">
						<drop item_id="111" min_amount="1" max_amount="1" chance="10"/>
						<drop item_id="222" min_amount="1" max_amount="1" chance="20"/>
					</drop_group>
				</npc_drop>
			</npc_drops>
			""");
		writeDrops("b.xml", """
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
		NpcDropData data = NpcDropData.loadLazy(tempDir.toFile(), 10, TimeUnit.MINUTES.toMillis(5), () -> 1_000L);

		NpcDrop drop = data.getDrop(100);

		assertEquals(List.of(111, 222, 333, 444), itemIds(drop));
		Drop replacement = drop.getDropGroup().get(0).getDrop().get(1);
		assertEquals(9, replacement.getMinAmount());
		assertEquals(90, replacement.getChance());
	}

	@Test
	void cacheEvictsByCapacityAndExpireAfterAccess() throws Exception {
		writeDrops("drops.xml", """
			<npc_drops>
				<npc_drop npc_id="100">
					<drop_group name="base">
						<drop item_id="111" min_amount="1" max_amount="1" chance="100"/>
					</drop_group>
				</npc_drop>
				<npc_drop npc_id="200">
					<drop_group name="base">
						<drop item_id="222" min_amount="1" max_amount="1" chance="100"/>
					</drop_group>
				</npc_drop>
			</npc_drops>
			""");
		MutableClock clock = new MutableClock(1_000);
		NpcDropData data = NpcDropData.loadLazy(tempDir.toFile(), 1, 500, clock::millis);

		NpcDrop first = data.getDrop(100);
		data.getDrop(200);
		NpcDrop reloadedAfterCapacityEviction = data.getDrop(100);

		assertNotSame(first, reloadedAfterCapacityEviction);
		assertEquals(1, data.cachedDropCount());

		clock.advance(501);
		data.cleanupExpiredDrops();
		assertEquals(0, data.cachedDropCount());

		NpcDrop reloadedAfterTtlEviction = data.getDrop(100);
		assertNotSame(reloadedAfterCapacityEviction, reloadedAfterTtlEviction);
	}

	private void writeDrops(String fileName, String xml) throws Exception {
		Files.writeString(tempDir.resolve(fileName), xml, StandardCharsets.UTF_8);
	}

	private static List<Integer> itemIds(NpcDrop drop) {
		return drop.getDropGroup().stream()
			.map(DropGroup::getDrop)
			.flatMap(List::stream)
			.map(Drop::getItemId)
			.toList();
	}

	private static final class MutableClock {
		private long millis;

		private MutableClock(long millis) {
			this.millis = millis;
		}

		private long millis() {
			return millis;
		}

		private void advance(long millis) {
			this.millis += millis;
		}
	}
}

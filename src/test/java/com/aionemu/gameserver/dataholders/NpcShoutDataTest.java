package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.npcshout.NpcShout;
import com.aionemu.gameserver.model.templates.npcshout.ShoutEventType;
import com.aionemu.gameserver.model.templates.npcshout.ShoutGroup;
import com.aionemu.gameserver.model.templates.npcshout.ShoutList;

/**
 * 守护 {@link NpcShoutData} 的冻结索引：全局（world 0）与世界限定喊话的合并、按事件类型的判定、
 * 以及 {@code getNpcShouts} 的防御性复制语义。
 * Guards the frozen index of {@link NpcShoutData}: global (world 0) plus world-specific merging, event-type
 * checks, and the defensive-copy contract of {@code getNpcShouts}.
 */
class NpcShoutDataTest {

	@Test
	void mergesGlobalAndWorldShoutsWithoutLeakingAcrossWorlds() throws Exception {
		NpcShoutData data = data(
				shoutList(0, List.of(100), shout(1, ShoutEventType.SEE, null, null)),
				shoutList(1, List.of(100), shout(2, ShoutEventType.ATTACKED, "p2", 5)),
				shoutList(1, List.of(200), shout(3, ShoutEventType.SEE, null, null)),
				shoutList(2, List.of(300), shout(4, ShoutEventType.SEE, null, null)));

		assertEquals(4, data.size());

		// 全局条目对所有世界可见 / global entries are visible in every world
		assertTrue(data.hasAnyShout(1, 100));
		assertTrue(data.hasAnyShout(3, 100));

		// 世界限定条目不得跨世界泄漏 / world-specific entries must not leak into other worlds
		assertTrue(data.hasAnyShout(1, 100, ShoutEventType.ATTACKED));
		assertFalse(data.hasAnyShout(2, 100, ShoutEventType.ATTACKED));
		assertTrue(data.hasAnyShout(1, 200, ShoutEventType.SEE));
		assertFalse(data.hasAnyShout(2, 200, ShoutEventType.SEE));
		assertTrue(data.hasAnyShout(2, 300, ShoutEventType.SEE));
		assertFalse(data.hasAnyShout(1, 300, ShoutEventType.SEE));

		// 类型判定 / event-type matching
		assertTrue(data.hasAnyShout(1, 100, ShoutEventType.SEE));
		assertFalse(data.hasAnyShout(1, 100, ShoutEventType.WALK_WAYPOINT));
		assertFalse(data.hasAnyShout(1, 999, ShoutEventType.SEE));

		// 合并结果包含全局 + 世界限定两条 / merged result holds the global and the world-specific entry
		List<NpcShout> merged = data.getNpcShouts(1, 100);
		assertNotNull(merged);
		assertEquals(2, merged.size());
		assertEquals(1, merged.get(0).getStringId());
		assertEquals(2, merged.get(1).getStringId());
	}

	@Test
	void getNpcShoutsKeepsReturningAnIndependentCopy() throws Exception {
		NpcShoutData data = data(shoutList(0, List.of(100), shout(1, ShoutEventType.SEE, null, null)));

		List<NpcShout> first = data.getNpcShouts(1, 100);
		assertNotNull(first);
		first.clear();

		// 调用方清理返回列表不得影响容器内部数据 / callers may clear their copy without touching the holder
		assertEquals(1, data.getNpcShouts(1, 100).size());
		assertNull(data.getNpcShouts(1, 999));
	}

	@Test
	void filtersByEventTypePatternAndSkillNumber() throws Exception {
		NpcShoutData data = data(
				shoutList(1, List.of(100), shout(1, ShoutEventType.ATTACKED, "p2", 5)),
				shoutList(1, List.of(100), shout(2, ShoutEventType.ATTACKED, "p3", 7)));

		assertEquals(2, data.getNpcShouts(1, 100).size());
		assertEquals(2, data.getNpcShouts(1, 100, ShoutEventType.ATTACKED, null, 0).size());
		assertEquals(1, data.getNpcShouts(1, 100, ShoutEventType.ATTACKED, "p2", 0).size());
		assertEquals(1, data.getNpcShouts(1, 100, ShoutEventType.ATTACKED, "p3", 7).size());
		assertNull(data.getNpcShouts(1, 100, ShoutEventType.ATTACKED, "p3", 5));
		assertNull(data.getNpcShouts(1, 100, ShoutEventType.SEE, null, 0));
	}

	@Test
	void findsEveryIndexedNpcId() throws Exception {
		List<ShoutList> lists = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			lists.add(shoutList(1, List.of(1_000 + i * 37), shout(i, ShoutEventType.SEE, null, null)));
		}
		NpcShoutData data = data(lists.toArray(new ShoutList[0]));

		for (int i = 0; i < 50; i++) {
			assertTrue(data.hasAnyShout(1, 1_000 + i * 37, ShoutEventType.SEE), "npcId index " + i);
			assertFalse(data.hasAnyShout(1, 1_000 + i * 37 + 1, ShoutEventType.SEE), "npcId index " + i);
		}
	}

	private static NpcShoutData data(ShoutList... lists) {
		ShoutGroup group = new ShoutGroup();
		group.getShoutNpcs().addAll(List.of(lists));
		NpcShoutData data = new NpcShoutData();
		data.shoutGroups = new ArrayList<>(List.of(group));
		data.afterUnmarshal(null, null);
		return data;
	}

	private static ShoutList shoutList(int worldId, List<Integer> npcIds, NpcShout... shouts) throws Exception {
		ShoutList list = new ShoutList();
		list.getNpcIds().addAll(npcIds);
		list.getNpcShouts().addAll(List.of(shouts));
		set(list, "restrictWorld", worldId);
		return list;
	}

	private static NpcShout shout(int stringId, ShoutEventType when, String pattern, Integer skillNo) throws Exception {
		NpcShout shout = new NpcShout();
		set(shout, "stringId", stringId);
		set(shout, "when", when);
		set(shout, "pattern", pattern);
		set(shout, "skillNo", skillNo);
		return shout;
	}

	private static void set(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}

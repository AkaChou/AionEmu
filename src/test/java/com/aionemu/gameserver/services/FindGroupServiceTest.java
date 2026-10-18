package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.FindGroup;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class FindGroupServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void listenersCaptureTheConstructingInstance() throws IOException {
		String source = Files.readString(
			Path.of("src/main/java/com/aionemu/gameserver/services/FindGroupService.java"));
		int constructorStart = source.indexOf("public FindGroupService()");
		int bodyStart = source.indexOf('{', constructorStart);
		int depth = 0;
		int bodyEnd = bodyStart;
		for (int i = bodyStart; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}' && --depth == 0) {
				bodyEnd = i;
				break;
			}
		}
		String constructor = source.substring(bodyStart, bodyEnd);

		// 监听闭包必须捕获构造中的实例；getInstance() 在容器外构造（如测试、启动早期）会 fail-fast。
		// The listener closures must capture the constructing instance; getInstance() fails fast when
		// constructed outside the container (tests, early startup).
		assertTrue(constructor.contains("final FindGroupService self = this;"));
		assertFalse(constructor.contains("FindGroupService.getInstance()"));
	}

	@Test
	void cleanRemovesExpiredGroupsWithoutConcurrentModification() throws ReflectiveOperationException {
		TestFindGroupService service = objenesis.newInstance(TestFindGroupService.class);
		Map<Integer, FindGroup> groups = new LinkedHashMap<>();
		groups.put(1, expiredGroup(1));
		groups.put(2, expiredGroup(2));
		service.mapToMutate = groups;
		setField(service, "elyosRecruitFindGroups", groups);
		setField(service, "elyosApplyFindGroups", new LinkedHashMap<Integer, FindGroup>());
		setField(service, "asmodianRecruitFindGroups", new LinkedHashMap<Integer, FindGroup>());
		setField(service, "asmodianApplyFindGroups", new LinkedHashMap<Integer, FindGroup>());

		assertDoesNotThrow(service::clean);
		assertTrue(groups.isEmpty());
	}

	@Test
	void getFindGroupsReturnsSnapshotInsteadOfLiveMapValues() throws ReflectiveOperationException {
		TestFindGroupService service = objenesis.newInstance(TestFindGroupService.class);
		Map<Integer, FindGroup> groups = new LinkedHashMap<>();
		groups.put(1, activeGroup(1));
		groups.put(2, activeGroup(2));
		setField(service, "elyosRecruitFindGroups", groups);
		setField(service, "elyosApplyFindGroups", new LinkedHashMap<Integer, FindGroup>());
		setField(service, "asmodianRecruitFindGroups", new LinkedHashMap<Integer, FindGroup>());
		setField(service, "asmodianApplyFindGroups", new LinkedHashMap<Integer, FindGroup>());

		Collection<FindGroup> snapshot = service.getFindGroups(Race.ELYOS, 0x00);
		groups.clear();

		assertEquals(2, snapshot.size());
	}

	@Test
	void updatesApplyListingAndIgnoresMissingEntries() throws ReflectiveOperationException {
		FindGroupService service = objenesis.newInstance(FindGroupService.class);
		FindGroup application = activeGroup(1);
		Map<Integer, FindGroup> applications = new LinkedHashMap<>();
		applications.put(1, application);
		setField(service, "elyosApplyFindGroups", applications);

		Player player = objenesis.newInstance(Player.class);
		PlayerCommonData commonData = new PlayerCommonData(1);
		commonData.setRace(Race.ELYOS);
		setField(player, "playerCommonData", commonData);

		service.updateFindGroupList(player, 0x07, "updated", 1);

		assertEquals("updated", application.getMessage());
		assertDoesNotThrow(() -> service.updateFindGroupList(player, 0x07, "missing", 2));
	}

	private FindGroup expiredGroup(int objectId) throws ReflectiveOperationException {
		FindGroup group = activeGroup(objectId);
		Field field = FindGroup.class.getDeclaredField("lastUpdate");
		field.setAccessible(true);
		field.set(group, 0);
		return group;
	}

	private FindGroup activeGroup(int objectId) {
		return new FindGroup(new TestAionObject(objectId), "test", 0);
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Class<?> type = target instanceof FindGroupService ? FindGroupService.class : target.getClass();
		Field field = type.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static class TestFindGroupService extends FindGroupService {

		private Map<Integer, FindGroup> mapToMutate;

		@Override
		public FindGroup removeFindGroup(Race race, int action, int playerObjId) {
			return mapToMutate.remove(playerObjId);
		}
	}

	private static final class TestAionObject extends AionObject {

		private TestAionObject(Integer objId) {
			super(objId);
		}

		@Override
		public String getName() {
			return "test";
		}
	}
}

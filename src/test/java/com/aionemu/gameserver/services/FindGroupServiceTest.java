package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.FindGroup;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import java.util.Collection;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class FindGroupServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void cleanRemovesExpiredGroupsWithoutConcurrentModification() throws ReflectiveOperationException {
		TestFindGroupService service = objenesis.newInstance(TestFindGroupService.class);
		Map<Integer, FindGroup> groups = new LinkedHashMap<Integer, FindGroup>();
		groups.put(1, expiredGroup(1));
		groups.put(2, expiredGroup(2));
		service.mapToMutate = groups;
		setField(service, "elyosRecruitFindGroups", groups);
		setField(service, "elyosApplyFindGroups", new LinkedHashMap<Integer, FindGroup>());
		setField(service, "asmodianRecruitFindGroups", new LinkedHashMap<Integer, FindGroup>());
		setField(service, "asmodianApplyFindGroups", new LinkedHashMap<Integer, FindGroup>());
		setField(service, "instanceGroups", new LinkedHashMap<Integer, FindGroup>());

		assertDoesNotThrow(service::clean);
		assertTrue(groups.isEmpty());
	}

	@Test
	void getFindGroupsReturnsSnapshotInsteadOfLiveMapValues() throws ReflectiveOperationException {
		TestFindGroupService service = objenesis.newInstance(TestFindGroupService.class);
		Map<Integer, FindGroup> groups = new LinkedHashMap<Integer, FindGroup>();
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
		Map<Integer, FindGroup> applications = new LinkedHashMap<Integer, FindGroup>();
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

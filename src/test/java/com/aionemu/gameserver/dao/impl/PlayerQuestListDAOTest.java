package com.aionemu.gameserver.dao.impl;

import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestListDAOTest {

	@Test
	void persistsAnUnselectedRewardAsSqlNull() throws Exception {
		Map<Integer, Object> parameters = new HashMap<>();
		Map<Integer, Integer> nullTypes = new HashMap<>();
		PreparedStatement statement = recordingStatement(parameters, nullTypes);
		QuestState state = new QuestState(1101, QuestStatus.START, 0, 0, null, null, null);

		PlayerQuestListDAO.setRewardParameter(statement, 7, state);

		assertTrue(parameters.containsKey(7));
		assertNull(parameters.get(7));
		assertEquals(Types.INTEGER, nullTypes.get(7));
	}

	@Test
	void persistsASelectedRewardAsItsIndex() throws Exception {
		Map<Integer, Object> parameters = new HashMap<>();
		PreparedStatement statement = recordingStatement(parameters, new HashMap<>());
		QuestState state = new QuestState(1101, QuestStatus.COMPLETE, 0, 1, null, 3, null);

		PlayerQuestListDAO.setRewardParameter(statement, 5, state);

		assertEquals(3, parameters.get(5));
	}

	private static PreparedStatement recordingStatement(Map<Integer, Object> parameters,
			Map<Integer, Integer> nullTypes) {
		return (PreparedStatement) Proxy.newProxyInstance(PlayerQuestListDAOTest.class.getClassLoader(),
			new Class<?>[] { PreparedStatement.class }, (proxy, method, args) -> {
				if (method.getName().equals("setNull")) {
					parameters.put((Integer) args[0], null);
					nullTypes.put((Integer) args[0], (Integer) args[1]);
				} else if (method.getName().equals("setInt")) {
					parameters.put((Integer) args[0], args[1]);
				}
				return null;
			});
	}
}

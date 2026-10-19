package com.aionemu.gameserver.questEngine;

import java.lang.reflect.Constructor;
import java.util.Map;

import com.aionemu.gameserver.questEngine.tablelane.NativeQuestStartPort;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;

/**
 * 接取端口测试支撑：通过反射访问包内私有的合成表构造与单例替换口。
 * <p>
 * Test support for the acquire port: reaches the package-private synthetic-table constructor and the
 * instance override through reflection, because this test lives outside the {@code tablelane} package.
 */
final class NativeQuestStartPortTestSupport {

	private NativeQuestStartPortTestSupport() {
	}

	static NativeQuestXmlTable table(Map<Integer, NativeQuestXmlTable.QuestRow> rows,
			Map<String, Integer> byName) {
		try {
			Constructor<NativeQuestXmlTable> ctor = NativeQuestXmlTable.class
				.getDeclaredConstructor(Map.class, Map.class);
			ctor.setAccessible(true);
			return ctor.newInstance(rows, byName);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	static void override(NativeQuestStartPort port) {
		try {
			var method = NativeQuestStartPort.class.getDeclaredMethod("overrideInstanceForTest",
				NativeQuestStartPort.class);
			method.setAccessible(true);
			method.invoke(null, port);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}

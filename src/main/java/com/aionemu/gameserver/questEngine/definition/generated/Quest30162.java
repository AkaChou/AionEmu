package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest30162 {
	private Quest30162() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(30162)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Group] Better Our Hands Than Yours", 1114285, 55, 2147483647, Set.of("ASMODIANS"), "QUEST", new RepeatPolicy(200, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 3453546L), new QuestReward("AP", 0, 306L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
		configureNodeBatch1(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("var1", 6, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("var2", 12, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 0))));
		builder.node("a0b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 0))));
		builder.node("a0b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 0))));
		builder.node("a0b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 0))));
		builder.node("a0b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 0))));
		builder.node("a0b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 0))));
		builder.node("a0b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 0))));
		builder.node("a0b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 0))));
		builder.node("a0b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 0))));
		builder.node("a0b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 0))));
		builder.node("a0b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 0))));
		builder.node("a0b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 0))));
		builder.node("a0b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 0))));
		builder.node("a0b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 0))));
		builder.node("a0b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 0))));
		builder.node("a0b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 0))));
		builder.node("a0b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 0))));
		builder.node("a0b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 0))));
		builder.node("a1b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 1))));
		builder.node("a1b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 1))));
		builder.node("a1b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 1))));
		builder.node("a1b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 1))));
		builder.node("a1b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 1))));
		builder.node("a1b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 1))));
		builder.node("a1b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 1))));
		builder.node("a1b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 1))));
		builder.node("a1b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 1))));
		builder.node("a1b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 1))));
		builder.node("a1b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 1))));
		builder.node("a1b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 1))));
		builder.node("a1b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 1))));
		builder.node("a1b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 1))));
		builder.node("a1b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 1))));
		builder.node("a1b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 1))));
		builder.node("a1b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 1))));
		builder.node("a1b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 1))));
		builder.node("a2b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 2))));
		builder.node("a2b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 2))));
		builder.node("a2b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 2))));
		builder.node("a2b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 2))));
		builder.node("a2b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 2))));
		builder.node("a2b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 2))));
		builder.node("a2b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 2))));
		builder.node("a2b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 2))));
		builder.node("a2b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 2))));
		builder.node("a2b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 2))));
		builder.node("a2b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 2))));
		builder.node("a2b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 2))));
		builder.node("a2b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 2))));
		builder.node("a2b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 2))));
		builder.node("a2b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 2))));
		builder.node("a2b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 2))));
		builder.node("a2b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 2))));
		builder.node("a2b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 2))));
		builder.node("a2b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 2))));
		builder.node("a2b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 2))));
		builder.node("a2b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 2))));
		builder.node("a2b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 2))));
		builder.node("a3b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 3))));
		builder.node("a3b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 3))));
		builder.node("a3b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 3))));
		builder.node("a3b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 3))));
		builder.node("a3b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 3))));
		builder.node("a3b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 3))));
		builder.node("a3b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 3))));
		builder.node("a3b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 3))));
		builder.node("a3b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 3))));
		builder.node("a3b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 3))));
		builder.node("a3b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 3))));
		builder.node("a3b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 3))));
		builder.node("a3b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 3))));
		builder.node("a3b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 3))));
		builder.node("a3b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 3))));
		builder.node("a3b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 3))));
		builder.node("a3b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 3))));
		builder.node("a3b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 3))));
		builder.node("a3b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 3))));
		builder.node("a3b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 3))));
		builder.node("a3b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 3))));
		builder.node("a3b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 3))));
		builder.node("a4b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 4))));
		builder.node("a4b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 4))));
		builder.node("a4b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 4))));
		builder.node("a4b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 4))));
		builder.node("a4b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 4))));
		builder.node("a4b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 4))));
		builder.node("a4b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 4))));
		builder.node("a4b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 4))));
		builder.node("a4b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 4))));
		builder.node("a4b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 4))));
		builder.node("a4b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 4))));
		builder.node("a4b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 4))));
		builder.node("a4b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 4))));
		builder.node("a4b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 4))));
		builder.node("a4b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 4))));
		builder.node("a4b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 4))));
		builder.node("a4b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 4))));
		builder.node("a4b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 4))));
		builder.node("a4b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 4))));
		builder.node("a4b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 4))));
		builder.node("a4b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 4))));
		builder.node("a4b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 4))));
		builder.node("a5b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 5))));
		builder.node("a5b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 5))));
		builder.node("a5b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 5))));
		builder.node("a5b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 5))));
		builder.node("a5b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 5))));
		builder.node("a5b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 5))));
		builder.node("a5b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 5))));
		builder.node("a5b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 5))));
		builder.node("a5b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 5))));
		builder.node("a5b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 5))));
		builder.node("a5b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 5))));
		builder.node("a5b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 5))));
		builder.node("a5b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 5))));
		builder.node("a5b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 5))));
		builder.node("a5b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 5))));
		builder.node("a5b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 5))));
		builder.node("a5b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 5))));
	}

	private static void configureNodeBatch1(QuestDsl.QuestBuilder builder) {
		builder.node("a5b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 5))));
		builder.node("a5b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 5))));
		builder.node("a5b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 5))));
		builder.node("a5b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 5))));
		builder.node("a5b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 5))));
		builder.node("a6b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 6))));
		builder.node("a6b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 6))));
		builder.node("a6b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 6))));
		builder.node("a6b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 6))));
		builder.node("a6b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 6))));
		builder.node("a6b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 6))));
		builder.node("a6b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 6))));
		builder.node("a6b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 6))));
		builder.node("a6b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 6))));
		builder.node("a6b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 6))));
		builder.node("a6b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 6))));
		builder.node("a6b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 6))));
		builder.node("a6b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 6))));
		builder.node("a6b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 6))));
		builder.node("a6b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 6))));
		builder.node("a6b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 6))));
		builder.node("a6b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 6))));
		builder.node("a6b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 6))));
		builder.node("a6b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 6))));
		builder.node("a6b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 6))));
		builder.node("a6b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 6))));
		builder.node("a6b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 6))));
		builder.node("a7b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 7))));
		builder.node("a7b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 7))));
		builder.node("a7b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 7))));
		builder.node("a7b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 7))));
		builder.node("a7b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 7))));
		builder.node("a7b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 7))));
		builder.node("a7b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 7))));
		builder.node("a7b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 7))));
		builder.node("a7b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 7))));
		builder.node("a7b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 7))));
		builder.node("a7b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 7))));
		builder.node("a7b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 7))));
		builder.node("a7b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 7))));
		builder.node("a7b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 7))));
		builder.node("a7b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 7))));
		builder.node("a7b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 7))));
		builder.node("a7b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 7))));
		builder.node("a7b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 7))));
		builder.node("a7b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 7))));
		builder.node("a7b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 7))));
		builder.node("a7b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 7))));
		builder.node("a7b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 7))));
		builder.node("a8b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 8))));
		builder.node("a8b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 8))));
		builder.node("a8b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 8))));
		builder.node("a8b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 8))));
		builder.node("a8b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 8))));
		builder.node("a8b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 8))));
		builder.node("a8b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 8))));
		builder.node("a8b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 8))));
		builder.node("a8b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 8))));
		builder.node("a8b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 8))));
		builder.node("a8b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 8))));
		builder.node("a8b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 8))));
		builder.node("a8b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 8))));
		builder.node("a8b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 8))));
		builder.node("a8b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 8))));
		builder.node("a8b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 8))));
		builder.node("a8b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 8))));
		builder.node("a8b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 8))));
		builder.node("a8b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 8))));
		builder.node("a8b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 8))));
		builder.node("a8b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 8))));
		builder.node("a8b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 8))));
		builder.node("a9b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 9))));
		builder.node("a9b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 9))));
		builder.node("a9b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 9))));
		builder.node("a9b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 9))));
		builder.node("a9b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 9))));
		builder.node("a9b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 9))));
		builder.node("a9b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 9))));
		builder.node("a9b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 9))));
		builder.node("a9b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 9))));
		builder.node("a9b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 9))));
		builder.node("a9b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 9))));
		builder.node("a9b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 9))));
		builder.node("a9b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 9))));
		builder.node("a9b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 9))));
		builder.node("a9b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 9))));
		builder.node("a9b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 9))));
		builder.node("a9b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 9))));
		builder.node("a9b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 9))));
		builder.node("a9b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 9))));
		builder.node("a9b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 9))));
		builder.node("a9b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 9))));
		builder.node("a9b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 9))));
		builder.node("a10b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 10))));
		builder.node("a10b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 10))));
		builder.node("a10b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 10))));
		builder.node("a10b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 10))));
		builder.node("a10b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 10))));
		builder.node("a10b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 10))));
		builder.node("a10b3c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 3), Map.entry("var0", 10))));
		builder.node("a10b3c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 3), Map.entry("var0", 10))));
		builder.node("a10b4c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 4), Map.entry("var0", 10))));
		builder.node("a10b4c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 4), Map.entry("var0", 10))));
		builder.node("a10b5c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 5), Map.entry("var0", 10))));
		builder.node("a10b5c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 5), Map.entry("var0", 10))));
		builder.node("a10b6c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 6), Map.entry("var0", 10))));
		builder.node("a10b6c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 6), Map.entry("var0", 10))));
		builder.node("a10b7c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 7), Map.entry("var0", 10))));
		builder.node("a10b7c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 7), Map.entry("var0", 10))));
		builder.node("a10b8c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 8), Map.entry("var0", 10))));
		builder.node("a10b8c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 8), Map.entry("var0", 10))));
		builder.node("a10b9c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 9), Map.entry("var0", 10))));
		builder.node("a10b9c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 9), Map.entry("var0", 10))));
		builder.node("a10b10c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 10), Map.entry("var0", 10))));
		builder.node("a10b10c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 10))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 10), Map.entry("var0", 10))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 0))));
	}

	private static void addTransitions(QuestDsl.QuestBuilder builder) {
		addTransitionBatch0(builder);
		addTransitionBatch1(builder);
		addTransitionBatch2(builder);
		addTransitionBatch3(builder);
		addTransitionBatch4(builder);
		addTransitionBatch5(builder);
		addTransitionBatch6(builder);
		addTransitionBatch7(builder);
		addTransitionBatch8(builder);
		addTransitionBatch9(builder);
		addTransitionBatch10(builder);
		addTransitionBatch11(builder);
		addTransitionBatch12(builder);
		addTransitionBatch13(builder);
		addTransitionBatch14(builder);
		addTransitionBatch15(builder);
		addTransitionBatch16(builder);
		addTransitionBatch17(builder);
		addTransitionBatch18(builder);
	}

	private static void addTransitionBatch0(QuestDsl.QuestBuilder builder) {
		addTransition0(builder);
		addTransition1(builder);
		addTransition2(builder);
		addTransition3(builder);
		addTransition4(builder);
		addTransition5(builder);
		addTransition6(builder);
		addTransition7(builder);
		addTransition8(builder);
		addTransition9(builder);
		addTransition10(builder);
		addTransition11(builder);
		addTransition12(builder);
		addTransition13(builder);
		addTransition14(builder);
		addTransition15(builder);
		addTransition16(builder);
		addTransition17(builder);
		addTransition18(builder);
		addTransition19(builder);
		addTransition20(builder);
		addTransition21(builder);
		addTransition22(builder);
		addTransition23(builder);
		addTransition24(builder);
		addTransition25(builder);
		addTransition26(builder);
		addTransition27(builder);
		addTransition28(builder);
		addTransition29(builder);
		addTransition30(builder);
		addTransition31(builder);
		addTransition32(builder);
		addTransition33(builder);
		addTransition34(builder);
		addTransition35(builder);
		addTransition36(builder);
		addTransition37(builder);
		addTransition38(builder);
		addTransition39(builder);
		addTransition40(builder);
		addTransition41(builder);
		addTransition42(builder);
		addTransition43(builder);
		addTransition44(builder);
		addTransition45(builder);
		addTransition46(builder);
		addTransition47(builder);
		addTransition48(builder);
		addTransition49(builder);
		addTransition50(builder);
		addTransition51(builder);
		addTransition52(builder);
		addTransition53(builder);
		addTransition54(builder);
		addTransition55(builder);
		addTransition56(builder);
		addTransition57(builder);
		addTransition58(builder);
		addTransition59(builder);
		addTransition60(builder);
		addTransition61(builder);
		addTransition62(builder);
		addTransition63(builder);
		addTransition64(builder);
		addTransition65(builder);
		addTransition66(builder);
		addTransition67(builder);
		addTransition68(builder);
		addTransition69(builder);
		addTransition70(builder);
		addTransition71(builder);
		addTransition72(builder);
		addTransition73(builder);
		addTransition74(builder);
		addTransition75(builder);
		addTransition76(builder);
		addTransition77(builder);
		addTransition78(builder);
		addTransition79(builder);
		addTransition80(builder);
		addTransition81(builder);
		addTransition82(builder);
		addTransition83(builder);
		addTransition84(builder);
		addTransition85(builder);
		addTransition86(builder);
		addTransition87(builder);
		addTransition88(builder);
		addTransition89(builder);
		addTransition90(builder);
		addTransition91(builder);
		addTransition92(builder);
		addTransition93(builder);
		addTransition94(builder);
		addTransition95(builder);
		addTransition96(builder);
		addTransition97(builder);
		addTransition98(builder);
		addTransition99(builder);
		addTransition100(builder);
		addTransition101(builder);
		addTransition102(builder);
		addTransition103(builder);
		addTransition104(builder);
		addTransition105(builder);
		addTransition106(builder);
		addTransition107(builder);
		addTransition108(builder);
		addTransition109(builder);
		addTransition110(builder);
		addTransition111(builder);
		addTransition112(builder);
		addTransition113(builder);
		addTransition114(builder);
		addTransition115(builder);
		addTransition116(builder);
		addTransition117(builder);
		addTransition118(builder);
		addTransition119(builder);
		addTransition120(builder);
		addTransition121(builder);
		addTransition122(builder);
		addTransition123(builder);
		addTransition124(builder);
		addTransition125(builder);
		addTransition126(builder);
		addTransition127(builder);
	}

	private static void addTransitionBatch1(QuestDsl.QuestBuilder builder) {
		addTransition128(builder);
		addTransition129(builder);
		addTransition130(builder);
		addTransition131(builder);
		addTransition132(builder);
		addTransition133(builder);
		addTransition134(builder);
		addTransition135(builder);
		addTransition136(builder);
		addTransition137(builder);
		addTransition138(builder);
		addTransition139(builder);
		addTransition140(builder);
		addTransition141(builder);
		addTransition142(builder);
		addTransition143(builder);
		addTransition144(builder);
		addTransition145(builder);
		addTransition146(builder);
		addTransition147(builder);
		addTransition148(builder);
		addTransition149(builder);
		addTransition150(builder);
		addTransition151(builder);
		addTransition152(builder);
		addTransition153(builder);
		addTransition154(builder);
		addTransition155(builder);
		addTransition156(builder);
		addTransition157(builder);
		addTransition158(builder);
		addTransition159(builder);
		addTransition160(builder);
		addTransition161(builder);
		addTransition162(builder);
		addTransition163(builder);
		addTransition164(builder);
		addTransition165(builder);
		addTransition166(builder);
		addTransition167(builder);
		addTransition168(builder);
		addTransition169(builder);
		addTransition170(builder);
		addTransition171(builder);
		addTransition172(builder);
		addTransition173(builder);
		addTransition174(builder);
		addTransition175(builder);
		addTransition176(builder);
		addTransition177(builder);
		addTransition178(builder);
		addTransition179(builder);
		addTransition180(builder);
		addTransition181(builder);
		addTransition182(builder);
		addTransition183(builder);
		addTransition184(builder);
		addTransition185(builder);
		addTransition186(builder);
		addTransition187(builder);
		addTransition188(builder);
		addTransition189(builder);
		addTransition190(builder);
		addTransition191(builder);
		addTransition192(builder);
		addTransition193(builder);
		addTransition194(builder);
		addTransition195(builder);
		addTransition196(builder);
		addTransition197(builder);
		addTransition198(builder);
		addTransition199(builder);
		addTransition200(builder);
		addTransition201(builder);
		addTransition202(builder);
		addTransition203(builder);
		addTransition204(builder);
		addTransition205(builder);
		addTransition206(builder);
		addTransition207(builder);
		addTransition208(builder);
		addTransition209(builder);
		addTransition210(builder);
		addTransition211(builder);
		addTransition212(builder);
		addTransition213(builder);
		addTransition214(builder);
		addTransition215(builder);
		addTransition216(builder);
		addTransition217(builder);
		addTransition218(builder);
		addTransition219(builder);
		addTransition220(builder);
		addTransition221(builder);
		addTransition222(builder);
		addTransition223(builder);
		addTransition224(builder);
		addTransition225(builder);
		addTransition226(builder);
		addTransition227(builder);
		addTransition228(builder);
		addTransition229(builder);
		addTransition230(builder);
		addTransition231(builder);
		addTransition232(builder);
		addTransition233(builder);
		addTransition234(builder);
		addTransition235(builder);
		addTransition236(builder);
		addTransition237(builder);
		addTransition238(builder);
		addTransition239(builder);
		addTransition240(builder);
		addTransition241(builder);
		addTransition242(builder);
		addTransition243(builder);
		addTransition244(builder);
		addTransition245(builder);
		addTransition246(builder);
		addTransition247(builder);
		addTransition248(builder);
		addTransition249(builder);
		addTransition250(builder);
		addTransition251(builder);
		addTransition252(builder);
		addTransition253(builder);
		addTransition254(builder);
		addTransition255(builder);
	}

	private static void addTransitionBatch2(QuestDsl.QuestBuilder builder) {
		addTransition256(builder);
		addTransition257(builder);
		addTransition258(builder);
		addTransition259(builder);
		addTransition260(builder);
		addTransition261(builder);
		addTransition262(builder);
		addTransition263(builder);
		addTransition264(builder);
		addTransition265(builder);
		addTransition266(builder);
		addTransition267(builder);
		addTransition268(builder);
		addTransition269(builder);
		addTransition270(builder);
		addTransition271(builder);
		addTransition272(builder);
		addTransition273(builder);
		addTransition274(builder);
		addTransition275(builder);
		addTransition276(builder);
		addTransition277(builder);
		addTransition278(builder);
		addTransition279(builder);
		addTransition280(builder);
		addTransition281(builder);
		addTransition282(builder);
		addTransition283(builder);
		addTransition284(builder);
		addTransition285(builder);
		addTransition286(builder);
		addTransition287(builder);
		addTransition288(builder);
		addTransition289(builder);
		addTransition290(builder);
		addTransition291(builder);
		addTransition292(builder);
		addTransition293(builder);
		addTransition294(builder);
		addTransition295(builder);
		addTransition296(builder);
		addTransition297(builder);
		addTransition298(builder);
		addTransition299(builder);
		addTransition300(builder);
		addTransition301(builder);
		addTransition302(builder);
		addTransition303(builder);
		addTransition304(builder);
		addTransition305(builder);
		addTransition306(builder);
		addTransition307(builder);
		addTransition308(builder);
		addTransition309(builder);
		addTransition310(builder);
		addTransition311(builder);
		addTransition312(builder);
		addTransition313(builder);
		addTransition314(builder);
		addTransition315(builder);
		addTransition316(builder);
		addTransition317(builder);
		addTransition318(builder);
		addTransition319(builder);
		addTransition320(builder);
		addTransition321(builder);
		addTransition322(builder);
		addTransition323(builder);
		addTransition324(builder);
		addTransition325(builder);
		addTransition326(builder);
		addTransition327(builder);
		addTransition328(builder);
		addTransition329(builder);
		addTransition330(builder);
		addTransition331(builder);
		addTransition332(builder);
		addTransition333(builder);
		addTransition334(builder);
		addTransition335(builder);
		addTransition336(builder);
		addTransition337(builder);
		addTransition338(builder);
		addTransition339(builder);
		addTransition340(builder);
		addTransition341(builder);
		addTransition342(builder);
		addTransition343(builder);
		addTransition344(builder);
		addTransition345(builder);
		addTransition346(builder);
		addTransition347(builder);
		addTransition348(builder);
		addTransition349(builder);
		addTransition350(builder);
		addTransition351(builder);
		addTransition352(builder);
		addTransition353(builder);
		addTransition354(builder);
		addTransition355(builder);
		addTransition356(builder);
		addTransition357(builder);
		addTransition358(builder);
		addTransition359(builder);
		addTransition360(builder);
		addTransition361(builder);
		addTransition362(builder);
		addTransition363(builder);
		addTransition364(builder);
		addTransition365(builder);
		addTransition366(builder);
		addTransition367(builder);
		addTransition368(builder);
		addTransition369(builder);
		addTransition370(builder);
		addTransition371(builder);
		addTransition372(builder);
		addTransition373(builder);
		addTransition374(builder);
		addTransition375(builder);
		addTransition376(builder);
		addTransition377(builder);
		addTransition378(builder);
		addTransition379(builder);
		addTransition380(builder);
		addTransition381(builder);
		addTransition382(builder);
		addTransition383(builder);
	}

	private static void addTransitionBatch3(QuestDsl.QuestBuilder builder) {
		addTransition384(builder);
		addTransition385(builder);
		addTransition386(builder);
		addTransition387(builder);
		addTransition388(builder);
		addTransition389(builder);
		addTransition390(builder);
		addTransition391(builder);
		addTransition392(builder);
		addTransition393(builder);
		addTransition394(builder);
		addTransition395(builder);
		addTransition396(builder);
		addTransition397(builder);
		addTransition398(builder);
		addTransition399(builder);
		addTransition400(builder);
		addTransition401(builder);
		addTransition402(builder);
		addTransition403(builder);
		addTransition404(builder);
		addTransition405(builder);
		addTransition406(builder);
		addTransition407(builder);
		addTransition408(builder);
		addTransition409(builder);
		addTransition410(builder);
		addTransition411(builder);
		addTransition412(builder);
		addTransition413(builder);
		addTransition414(builder);
		addTransition415(builder);
		addTransition416(builder);
		addTransition417(builder);
		addTransition418(builder);
		addTransition419(builder);
		addTransition420(builder);
		addTransition421(builder);
		addTransition422(builder);
		addTransition423(builder);
		addTransition424(builder);
		addTransition425(builder);
		addTransition426(builder);
		addTransition427(builder);
		addTransition428(builder);
		addTransition429(builder);
		addTransition430(builder);
		addTransition431(builder);
		addTransition432(builder);
		addTransition433(builder);
		addTransition434(builder);
		addTransition435(builder);
		addTransition436(builder);
		addTransition437(builder);
		addTransition438(builder);
		addTransition439(builder);
		addTransition440(builder);
		addTransition441(builder);
		addTransition442(builder);
		addTransition443(builder);
		addTransition444(builder);
		addTransition445(builder);
		addTransition446(builder);
		addTransition447(builder);
		addTransition448(builder);
		addTransition449(builder);
		addTransition450(builder);
		addTransition451(builder);
		addTransition452(builder);
		addTransition453(builder);
		addTransition454(builder);
		addTransition455(builder);
		addTransition456(builder);
		addTransition457(builder);
		addTransition458(builder);
		addTransition459(builder);
		addTransition460(builder);
		addTransition461(builder);
		addTransition462(builder);
		addTransition463(builder);
		addTransition464(builder);
		addTransition465(builder);
		addTransition466(builder);
		addTransition467(builder);
		addTransition468(builder);
		addTransition469(builder);
		addTransition470(builder);
		addTransition471(builder);
		addTransition472(builder);
		addTransition473(builder);
		addTransition474(builder);
		addTransition475(builder);
		addTransition476(builder);
		addTransition477(builder);
		addTransition478(builder);
		addTransition479(builder);
		addTransition480(builder);
		addTransition481(builder);
		addTransition482(builder);
		addTransition483(builder);
		addTransition484(builder);
		addTransition485(builder);
		addTransition486(builder);
		addTransition487(builder);
		addTransition488(builder);
		addTransition489(builder);
		addTransition490(builder);
		addTransition491(builder);
		addTransition492(builder);
		addTransition493(builder);
		addTransition494(builder);
		addTransition495(builder);
		addTransition496(builder);
		addTransition497(builder);
		addTransition498(builder);
		addTransition499(builder);
		addTransition500(builder);
		addTransition501(builder);
		addTransition502(builder);
		addTransition503(builder);
		addTransition504(builder);
		addTransition505(builder);
		addTransition506(builder);
		addTransition507(builder);
		addTransition508(builder);
		addTransition509(builder);
		addTransition510(builder);
		addTransition511(builder);
	}

	private static void addTransitionBatch4(QuestDsl.QuestBuilder builder) {
		addTransition512(builder);
		addTransition513(builder);
		addTransition514(builder);
		addTransition515(builder);
		addTransition516(builder);
		addTransition517(builder);
		addTransition518(builder);
		addTransition519(builder);
		addTransition520(builder);
		addTransition521(builder);
		addTransition522(builder);
		addTransition523(builder);
		addTransition524(builder);
		addTransition525(builder);
		addTransition526(builder);
		addTransition527(builder);
		addTransition528(builder);
		addTransition529(builder);
		addTransition530(builder);
		addTransition531(builder);
		addTransition532(builder);
		addTransition533(builder);
		addTransition534(builder);
		addTransition535(builder);
		addTransition536(builder);
		addTransition537(builder);
		addTransition538(builder);
		addTransition539(builder);
		addTransition540(builder);
		addTransition541(builder);
		addTransition542(builder);
		addTransition543(builder);
		addTransition544(builder);
		addTransition545(builder);
		addTransition546(builder);
		addTransition547(builder);
		addTransition548(builder);
		addTransition549(builder);
		addTransition550(builder);
		addTransition551(builder);
		addTransition552(builder);
		addTransition553(builder);
		addTransition554(builder);
		addTransition555(builder);
		addTransition556(builder);
		addTransition557(builder);
		addTransition558(builder);
		addTransition559(builder);
		addTransition560(builder);
		addTransition561(builder);
		addTransition562(builder);
		addTransition563(builder);
		addTransition564(builder);
		addTransition565(builder);
		addTransition566(builder);
		addTransition567(builder);
		addTransition568(builder);
		addTransition569(builder);
		addTransition570(builder);
		addTransition571(builder);
		addTransition572(builder);
		addTransition573(builder);
		addTransition574(builder);
		addTransition575(builder);
		addTransition576(builder);
		addTransition577(builder);
		addTransition578(builder);
		addTransition579(builder);
		addTransition580(builder);
		addTransition581(builder);
		addTransition582(builder);
		addTransition583(builder);
		addTransition584(builder);
		addTransition585(builder);
		addTransition586(builder);
		addTransition587(builder);
		addTransition588(builder);
		addTransition589(builder);
		addTransition590(builder);
		addTransition591(builder);
		addTransition592(builder);
		addTransition593(builder);
		addTransition594(builder);
		addTransition595(builder);
		addTransition596(builder);
		addTransition597(builder);
		addTransition598(builder);
		addTransition599(builder);
		addTransition600(builder);
		addTransition601(builder);
		addTransition602(builder);
		addTransition603(builder);
		addTransition604(builder);
		addTransition605(builder);
		addTransition606(builder);
		addTransition607(builder);
		addTransition608(builder);
		addTransition609(builder);
		addTransition610(builder);
		addTransition611(builder);
		addTransition612(builder);
		addTransition613(builder);
		addTransition614(builder);
		addTransition615(builder);
		addTransition616(builder);
		addTransition617(builder);
		addTransition618(builder);
		addTransition619(builder);
		addTransition620(builder);
		addTransition621(builder);
		addTransition622(builder);
		addTransition623(builder);
		addTransition624(builder);
		addTransition625(builder);
		addTransition626(builder);
		addTransition627(builder);
		addTransition628(builder);
		addTransition629(builder);
		addTransition630(builder);
		addTransition631(builder);
		addTransition632(builder);
		addTransition633(builder);
		addTransition634(builder);
		addTransition635(builder);
		addTransition636(builder);
		addTransition637(builder);
		addTransition638(builder);
		addTransition639(builder);
	}

	private static void addTransitionBatch5(QuestDsl.QuestBuilder builder) {
		addTransition640(builder);
		addTransition641(builder);
		addTransition642(builder);
		addTransition643(builder);
		addTransition644(builder);
		addTransition645(builder);
		addTransition646(builder);
		addTransition647(builder);
		addTransition648(builder);
		addTransition649(builder);
		addTransition650(builder);
		addTransition651(builder);
		addTransition652(builder);
		addTransition653(builder);
		addTransition654(builder);
		addTransition655(builder);
		addTransition656(builder);
		addTransition657(builder);
		addTransition658(builder);
		addTransition659(builder);
		addTransition660(builder);
		addTransition661(builder);
		addTransition662(builder);
		addTransition663(builder);
		addTransition664(builder);
		addTransition665(builder);
		addTransition666(builder);
		addTransition667(builder);
		addTransition668(builder);
		addTransition669(builder);
		addTransition670(builder);
		addTransition671(builder);
		addTransition672(builder);
		addTransition673(builder);
		addTransition674(builder);
		addTransition675(builder);
		addTransition676(builder);
		addTransition677(builder);
		addTransition678(builder);
		addTransition679(builder);
		addTransition680(builder);
		addTransition681(builder);
		addTransition682(builder);
		addTransition683(builder);
		addTransition684(builder);
		addTransition685(builder);
		addTransition686(builder);
		addTransition687(builder);
		addTransition688(builder);
		addTransition689(builder);
		addTransition690(builder);
		addTransition691(builder);
		addTransition692(builder);
		addTransition693(builder);
		addTransition694(builder);
		addTransition695(builder);
		addTransition696(builder);
		addTransition697(builder);
		addTransition698(builder);
		addTransition699(builder);
		addTransition700(builder);
		addTransition701(builder);
		addTransition702(builder);
		addTransition703(builder);
		addTransition704(builder);
		addTransition705(builder);
		addTransition706(builder);
		addTransition707(builder);
		addTransition708(builder);
		addTransition709(builder);
		addTransition710(builder);
		addTransition711(builder);
		addTransition712(builder);
		addTransition713(builder);
		addTransition714(builder);
		addTransition715(builder);
		addTransition716(builder);
		addTransition717(builder);
		addTransition718(builder);
		addTransition719(builder);
		addTransition720(builder);
		addTransition721(builder);
		addTransition722(builder);
		addTransition723(builder);
		addTransition724(builder);
		addTransition725(builder);
		addTransition726(builder);
		addTransition727(builder);
		addTransition728(builder);
		addTransition729(builder);
		addTransition730(builder);
		addTransition731(builder);
		addTransition732(builder);
		addTransition733(builder);
		addTransition734(builder);
		addTransition735(builder);
		addTransition736(builder);
		addTransition737(builder);
		addTransition738(builder);
		addTransition739(builder);
		addTransition740(builder);
		addTransition741(builder);
		addTransition742(builder);
		addTransition743(builder);
		addTransition744(builder);
		addTransition745(builder);
		addTransition746(builder);
		addTransition747(builder);
		addTransition748(builder);
		addTransition749(builder);
		addTransition750(builder);
		addTransition751(builder);
		addTransition752(builder);
		addTransition753(builder);
		addTransition754(builder);
		addTransition755(builder);
		addTransition756(builder);
		addTransition757(builder);
		addTransition758(builder);
		addTransition759(builder);
		addTransition760(builder);
		addTransition761(builder);
		addTransition762(builder);
		addTransition763(builder);
		addTransition764(builder);
		addTransition765(builder);
		addTransition766(builder);
		addTransition767(builder);
	}

	private static void addTransitionBatch6(QuestDsl.QuestBuilder builder) {
		addTransition768(builder);
		addTransition769(builder);
		addTransition770(builder);
		addTransition771(builder);
		addTransition772(builder);
		addTransition773(builder);
		addTransition774(builder);
		addTransition775(builder);
		addTransition776(builder);
		addTransition777(builder);
		addTransition778(builder);
		addTransition779(builder);
		addTransition780(builder);
		addTransition781(builder);
		addTransition782(builder);
		addTransition783(builder);
		addTransition784(builder);
		addTransition785(builder);
		addTransition786(builder);
		addTransition787(builder);
		addTransition788(builder);
		addTransition789(builder);
		addTransition790(builder);
		addTransition791(builder);
		addTransition792(builder);
		addTransition793(builder);
		addTransition794(builder);
		addTransition795(builder);
		addTransition796(builder);
		addTransition797(builder);
		addTransition798(builder);
		addTransition799(builder);
		addTransition800(builder);
		addTransition801(builder);
		addTransition802(builder);
		addTransition803(builder);
		addTransition804(builder);
		addTransition805(builder);
		addTransition806(builder);
		addTransition807(builder);
		addTransition808(builder);
		addTransition809(builder);
		addTransition810(builder);
		addTransition811(builder);
		addTransition812(builder);
		addTransition813(builder);
		addTransition814(builder);
		addTransition815(builder);
		addTransition816(builder);
		addTransition817(builder);
		addTransition818(builder);
		addTransition819(builder);
		addTransition820(builder);
		addTransition821(builder);
		addTransition822(builder);
		addTransition823(builder);
		addTransition824(builder);
		addTransition825(builder);
		addTransition826(builder);
		addTransition827(builder);
		addTransition828(builder);
		addTransition829(builder);
		addTransition830(builder);
		addTransition831(builder);
		addTransition832(builder);
		addTransition833(builder);
		addTransition834(builder);
		addTransition835(builder);
		addTransition836(builder);
		addTransition837(builder);
		addTransition838(builder);
		addTransition839(builder);
		addTransition840(builder);
		addTransition841(builder);
		addTransition842(builder);
		addTransition843(builder);
		addTransition844(builder);
		addTransition845(builder);
		addTransition846(builder);
		addTransition847(builder);
		addTransition848(builder);
		addTransition849(builder);
		addTransition850(builder);
		addTransition851(builder);
		addTransition852(builder);
		addTransition853(builder);
		addTransition854(builder);
		addTransition855(builder);
		addTransition856(builder);
		addTransition857(builder);
		addTransition858(builder);
		addTransition859(builder);
		addTransition860(builder);
		addTransition861(builder);
		addTransition862(builder);
		addTransition863(builder);
		addTransition864(builder);
		addTransition865(builder);
		addTransition866(builder);
		addTransition867(builder);
		addTransition868(builder);
		addTransition869(builder);
		addTransition870(builder);
		addTransition871(builder);
		addTransition872(builder);
		addTransition873(builder);
		addTransition874(builder);
		addTransition875(builder);
		addTransition876(builder);
		addTransition877(builder);
		addTransition878(builder);
		addTransition879(builder);
		addTransition880(builder);
		addTransition881(builder);
		addTransition882(builder);
		addTransition883(builder);
		addTransition884(builder);
		addTransition885(builder);
		addTransition886(builder);
		addTransition887(builder);
		addTransition888(builder);
		addTransition889(builder);
		addTransition890(builder);
		addTransition891(builder);
		addTransition892(builder);
		addTransition893(builder);
		addTransition894(builder);
		addTransition895(builder);
	}

	private static void addTransitionBatch7(QuestDsl.QuestBuilder builder) {
		addTransition896(builder);
		addTransition897(builder);
		addTransition898(builder);
		addTransition899(builder);
		addTransition900(builder);
		addTransition901(builder);
		addTransition902(builder);
		addTransition903(builder);
		addTransition904(builder);
		addTransition905(builder);
		addTransition906(builder);
		addTransition907(builder);
		addTransition908(builder);
		addTransition909(builder);
		addTransition910(builder);
		addTransition911(builder);
		addTransition912(builder);
		addTransition913(builder);
		addTransition914(builder);
		addTransition915(builder);
		addTransition916(builder);
		addTransition917(builder);
		addTransition918(builder);
		addTransition919(builder);
		addTransition920(builder);
		addTransition921(builder);
		addTransition922(builder);
		addTransition923(builder);
		addTransition924(builder);
		addTransition925(builder);
		addTransition926(builder);
		addTransition927(builder);
		addTransition928(builder);
		addTransition929(builder);
		addTransition930(builder);
		addTransition931(builder);
		addTransition932(builder);
		addTransition933(builder);
		addTransition934(builder);
		addTransition935(builder);
		addTransition936(builder);
		addTransition937(builder);
		addTransition938(builder);
		addTransition939(builder);
		addTransition940(builder);
		addTransition941(builder);
		addTransition942(builder);
		addTransition943(builder);
		addTransition944(builder);
		addTransition945(builder);
		addTransition946(builder);
		addTransition947(builder);
		addTransition948(builder);
		addTransition949(builder);
		addTransition950(builder);
		addTransition951(builder);
		addTransition952(builder);
		addTransition953(builder);
		addTransition954(builder);
		addTransition955(builder);
		addTransition956(builder);
		addTransition957(builder);
		addTransition958(builder);
		addTransition959(builder);
		addTransition960(builder);
		addTransition961(builder);
		addTransition962(builder);
		addTransition963(builder);
		addTransition964(builder);
		addTransition965(builder);
		addTransition966(builder);
		addTransition967(builder);
		addTransition968(builder);
		addTransition969(builder);
		addTransition970(builder);
		addTransition971(builder);
		addTransition972(builder);
		addTransition973(builder);
		addTransition974(builder);
		addTransition975(builder);
		addTransition976(builder);
		addTransition977(builder);
		addTransition978(builder);
		addTransition979(builder);
		addTransition980(builder);
		addTransition981(builder);
		addTransition982(builder);
		addTransition983(builder);
		addTransition984(builder);
		addTransition985(builder);
		addTransition986(builder);
		addTransition987(builder);
		addTransition988(builder);
		addTransition989(builder);
		addTransition990(builder);
		addTransition991(builder);
		addTransition992(builder);
		addTransition993(builder);
		addTransition994(builder);
		addTransition995(builder);
		addTransition996(builder);
		addTransition997(builder);
		addTransition998(builder);
		addTransition999(builder);
		addTransition1000(builder);
		addTransition1001(builder);
		addTransition1002(builder);
		addTransition1003(builder);
		addTransition1004(builder);
		addTransition1005(builder);
		addTransition1006(builder);
		addTransition1007(builder);
		addTransition1008(builder);
		addTransition1009(builder);
		addTransition1010(builder);
		addTransition1011(builder);
		addTransition1012(builder);
		addTransition1013(builder);
		addTransition1014(builder);
		addTransition1015(builder);
		addTransition1016(builder);
		addTransition1017(builder);
		addTransition1018(builder);
		addTransition1019(builder);
		addTransition1020(builder);
		addTransition1021(builder);
		addTransition1022(builder);
		addTransition1023(builder);
	}

	private static void addTransitionBatch8(QuestDsl.QuestBuilder builder) {
		addTransition1024(builder);
		addTransition1025(builder);
		addTransition1026(builder);
		addTransition1027(builder);
		addTransition1028(builder);
		addTransition1029(builder);
		addTransition1030(builder);
		addTransition1031(builder);
		addTransition1032(builder);
		addTransition1033(builder);
		addTransition1034(builder);
		addTransition1035(builder);
		addTransition1036(builder);
		addTransition1037(builder);
		addTransition1038(builder);
		addTransition1039(builder);
		addTransition1040(builder);
		addTransition1041(builder);
		addTransition1042(builder);
		addTransition1043(builder);
		addTransition1044(builder);
		addTransition1045(builder);
		addTransition1046(builder);
		addTransition1047(builder);
		addTransition1048(builder);
		addTransition1049(builder);
		addTransition1050(builder);
		addTransition1051(builder);
		addTransition1052(builder);
		addTransition1053(builder);
		addTransition1054(builder);
		addTransition1055(builder);
		addTransition1056(builder);
		addTransition1057(builder);
		addTransition1058(builder);
		addTransition1059(builder);
		addTransition1060(builder);
		addTransition1061(builder);
		addTransition1062(builder);
		addTransition1063(builder);
		addTransition1064(builder);
		addTransition1065(builder);
		addTransition1066(builder);
		addTransition1067(builder);
		addTransition1068(builder);
		addTransition1069(builder);
		addTransition1070(builder);
		addTransition1071(builder);
		addTransition1072(builder);
		addTransition1073(builder);
		addTransition1074(builder);
		addTransition1075(builder);
		addTransition1076(builder);
		addTransition1077(builder);
		addTransition1078(builder);
		addTransition1079(builder);
		addTransition1080(builder);
		addTransition1081(builder);
		addTransition1082(builder);
		addTransition1083(builder);
		addTransition1084(builder);
		addTransition1085(builder);
		addTransition1086(builder);
		addTransition1087(builder);
		addTransition1088(builder);
		addTransition1089(builder);
		addTransition1090(builder);
		addTransition1091(builder);
		addTransition1092(builder);
		addTransition1093(builder);
		addTransition1094(builder);
		addTransition1095(builder);
		addTransition1096(builder);
		addTransition1097(builder);
		addTransition1098(builder);
		addTransition1099(builder);
		addTransition1100(builder);
		addTransition1101(builder);
		addTransition1102(builder);
		addTransition1103(builder);
		addTransition1104(builder);
		addTransition1105(builder);
		addTransition1106(builder);
		addTransition1107(builder);
		addTransition1108(builder);
		addTransition1109(builder);
		addTransition1110(builder);
		addTransition1111(builder);
		addTransition1112(builder);
		addTransition1113(builder);
		addTransition1114(builder);
		addTransition1115(builder);
		addTransition1116(builder);
		addTransition1117(builder);
		addTransition1118(builder);
		addTransition1119(builder);
		addTransition1120(builder);
		addTransition1121(builder);
		addTransition1122(builder);
		addTransition1123(builder);
		addTransition1124(builder);
		addTransition1125(builder);
		addTransition1126(builder);
		addTransition1127(builder);
		addTransition1128(builder);
		addTransition1129(builder);
		addTransition1130(builder);
		addTransition1131(builder);
		addTransition1132(builder);
		addTransition1133(builder);
		addTransition1134(builder);
		addTransition1135(builder);
		addTransition1136(builder);
		addTransition1137(builder);
		addTransition1138(builder);
		addTransition1139(builder);
		addTransition1140(builder);
		addTransition1141(builder);
		addTransition1142(builder);
		addTransition1143(builder);
		addTransition1144(builder);
		addTransition1145(builder);
		addTransition1146(builder);
		addTransition1147(builder);
		addTransition1148(builder);
		addTransition1149(builder);
		addTransition1150(builder);
		addTransition1151(builder);
	}

	private static void addTransitionBatch9(QuestDsl.QuestBuilder builder) {
		addTransition1152(builder);
		addTransition1153(builder);
		addTransition1154(builder);
		addTransition1155(builder);
		addTransition1156(builder);
		addTransition1157(builder);
		addTransition1158(builder);
		addTransition1159(builder);
		addTransition1160(builder);
		addTransition1161(builder);
		addTransition1162(builder);
		addTransition1163(builder);
		addTransition1164(builder);
		addTransition1165(builder);
		addTransition1166(builder);
		addTransition1167(builder);
		addTransition1168(builder);
		addTransition1169(builder);
		addTransition1170(builder);
		addTransition1171(builder);
		addTransition1172(builder);
		addTransition1173(builder);
		addTransition1174(builder);
		addTransition1175(builder);
		addTransition1176(builder);
		addTransition1177(builder);
		addTransition1178(builder);
		addTransition1179(builder);
		addTransition1180(builder);
		addTransition1181(builder);
		addTransition1182(builder);
		addTransition1183(builder);
		addTransition1184(builder);
		addTransition1185(builder);
		addTransition1186(builder);
		addTransition1187(builder);
		addTransition1188(builder);
		addTransition1189(builder);
		addTransition1190(builder);
		addTransition1191(builder);
		addTransition1192(builder);
		addTransition1193(builder);
		addTransition1194(builder);
		addTransition1195(builder);
		addTransition1196(builder);
		addTransition1197(builder);
		addTransition1198(builder);
		addTransition1199(builder);
		addTransition1200(builder);
		addTransition1201(builder);
		addTransition1202(builder);
		addTransition1203(builder);
		addTransition1204(builder);
		addTransition1205(builder);
		addTransition1206(builder);
		addTransition1207(builder);
		addTransition1208(builder);
		addTransition1209(builder);
		addTransition1210(builder);
		addTransition1211(builder);
		addTransition1212(builder);
		addTransition1213(builder);
		addTransition1214(builder);
		addTransition1215(builder);
		addTransition1216(builder);
		addTransition1217(builder);
		addTransition1218(builder);
		addTransition1219(builder);
		addTransition1220(builder);
		addTransition1221(builder);
		addTransition1222(builder);
		addTransition1223(builder);
		addTransition1224(builder);
		addTransition1225(builder);
		addTransition1226(builder);
		addTransition1227(builder);
		addTransition1228(builder);
		addTransition1229(builder);
		addTransition1230(builder);
		addTransition1231(builder);
		addTransition1232(builder);
		addTransition1233(builder);
		addTransition1234(builder);
		addTransition1235(builder);
		addTransition1236(builder);
		addTransition1237(builder);
		addTransition1238(builder);
		addTransition1239(builder);
		addTransition1240(builder);
		addTransition1241(builder);
		addTransition1242(builder);
		addTransition1243(builder);
		addTransition1244(builder);
		addTransition1245(builder);
		addTransition1246(builder);
		addTransition1247(builder);
		addTransition1248(builder);
		addTransition1249(builder);
		addTransition1250(builder);
		addTransition1251(builder);
		addTransition1252(builder);
		addTransition1253(builder);
		addTransition1254(builder);
		addTransition1255(builder);
		addTransition1256(builder);
		addTransition1257(builder);
		addTransition1258(builder);
		addTransition1259(builder);
		addTransition1260(builder);
		addTransition1261(builder);
		addTransition1262(builder);
		addTransition1263(builder);
		addTransition1264(builder);
		addTransition1265(builder);
		addTransition1266(builder);
		addTransition1267(builder);
		addTransition1268(builder);
		addTransition1269(builder);
		addTransition1270(builder);
		addTransition1271(builder);
		addTransition1272(builder);
		addTransition1273(builder);
		addTransition1274(builder);
		addTransition1275(builder);
		addTransition1276(builder);
		addTransition1277(builder);
		addTransition1278(builder);
		addTransition1279(builder);
	}

	private static void addTransitionBatch10(QuestDsl.QuestBuilder builder) {
		addTransition1280(builder);
		addTransition1281(builder);
		addTransition1282(builder);
		addTransition1283(builder);
		addTransition1284(builder);
		addTransition1285(builder);
		addTransition1286(builder);
		addTransition1287(builder);
		addTransition1288(builder);
		addTransition1289(builder);
		addTransition1290(builder);
		addTransition1291(builder);
		addTransition1292(builder);
		addTransition1293(builder);
		addTransition1294(builder);
		addTransition1295(builder);
		addTransition1296(builder);
		addTransition1297(builder);
		addTransition1298(builder);
		addTransition1299(builder);
		addTransition1300(builder);
		addTransition1301(builder);
		addTransition1302(builder);
		addTransition1303(builder);
		addTransition1304(builder);
		addTransition1305(builder);
		addTransition1306(builder);
		addTransition1307(builder);
		addTransition1308(builder);
		addTransition1309(builder);
		addTransition1310(builder);
		addTransition1311(builder);
		addTransition1312(builder);
		addTransition1313(builder);
		addTransition1314(builder);
		addTransition1315(builder);
		addTransition1316(builder);
		addTransition1317(builder);
		addTransition1318(builder);
		addTransition1319(builder);
		addTransition1320(builder);
		addTransition1321(builder);
		addTransition1322(builder);
		addTransition1323(builder);
		addTransition1324(builder);
		addTransition1325(builder);
		addTransition1326(builder);
		addTransition1327(builder);
		addTransition1328(builder);
		addTransition1329(builder);
		addTransition1330(builder);
		addTransition1331(builder);
		addTransition1332(builder);
		addTransition1333(builder);
		addTransition1334(builder);
		addTransition1335(builder);
		addTransition1336(builder);
		addTransition1337(builder);
		addTransition1338(builder);
		addTransition1339(builder);
		addTransition1340(builder);
		addTransition1341(builder);
		addTransition1342(builder);
		addTransition1343(builder);
		addTransition1344(builder);
		addTransition1345(builder);
		addTransition1346(builder);
		addTransition1347(builder);
		addTransition1348(builder);
		addTransition1349(builder);
		addTransition1350(builder);
		addTransition1351(builder);
		addTransition1352(builder);
		addTransition1353(builder);
		addTransition1354(builder);
		addTransition1355(builder);
		addTransition1356(builder);
		addTransition1357(builder);
		addTransition1358(builder);
		addTransition1359(builder);
		addTransition1360(builder);
		addTransition1361(builder);
		addTransition1362(builder);
		addTransition1363(builder);
		addTransition1364(builder);
		addTransition1365(builder);
		addTransition1366(builder);
		addTransition1367(builder);
		addTransition1368(builder);
		addTransition1369(builder);
		addTransition1370(builder);
		addTransition1371(builder);
		addTransition1372(builder);
		addTransition1373(builder);
		addTransition1374(builder);
		addTransition1375(builder);
		addTransition1376(builder);
		addTransition1377(builder);
		addTransition1378(builder);
		addTransition1379(builder);
		addTransition1380(builder);
		addTransition1381(builder);
		addTransition1382(builder);
		addTransition1383(builder);
		addTransition1384(builder);
		addTransition1385(builder);
		addTransition1386(builder);
		addTransition1387(builder);
		addTransition1388(builder);
		addTransition1389(builder);
		addTransition1390(builder);
		addTransition1391(builder);
		addTransition1392(builder);
		addTransition1393(builder);
		addTransition1394(builder);
		addTransition1395(builder);
		addTransition1396(builder);
		addTransition1397(builder);
		addTransition1398(builder);
		addTransition1399(builder);
		addTransition1400(builder);
		addTransition1401(builder);
		addTransition1402(builder);
		addTransition1403(builder);
		addTransition1404(builder);
		addTransition1405(builder);
		addTransition1406(builder);
		addTransition1407(builder);
	}

	private static void addTransitionBatch11(QuestDsl.QuestBuilder builder) {
		addTransition1408(builder);
		addTransition1409(builder);
		addTransition1410(builder);
		addTransition1411(builder);
		addTransition1412(builder);
		addTransition1413(builder);
		addTransition1414(builder);
		addTransition1415(builder);
		addTransition1416(builder);
		addTransition1417(builder);
		addTransition1418(builder);
		addTransition1419(builder);
		addTransition1420(builder);
		addTransition1421(builder);
		addTransition1422(builder);
		addTransition1423(builder);
		addTransition1424(builder);
		addTransition1425(builder);
		addTransition1426(builder);
		addTransition1427(builder);
		addTransition1428(builder);
		addTransition1429(builder);
		addTransition1430(builder);
		addTransition1431(builder);
		addTransition1432(builder);
		addTransition1433(builder);
		addTransition1434(builder);
		addTransition1435(builder);
		addTransition1436(builder);
		addTransition1437(builder);
		addTransition1438(builder);
		addTransition1439(builder);
		addTransition1440(builder);
		addTransition1441(builder);
		addTransition1442(builder);
		addTransition1443(builder);
		addTransition1444(builder);
		addTransition1445(builder);
		addTransition1446(builder);
		addTransition1447(builder);
		addTransition1448(builder);
		addTransition1449(builder);
		addTransition1450(builder);
		addTransition1451(builder);
		addTransition1452(builder);
		addTransition1453(builder);
		addTransition1454(builder);
		addTransition1455(builder);
		addTransition1456(builder);
		addTransition1457(builder);
		addTransition1458(builder);
		addTransition1459(builder);
		addTransition1460(builder);
		addTransition1461(builder);
		addTransition1462(builder);
		addTransition1463(builder);
		addTransition1464(builder);
		addTransition1465(builder);
		addTransition1466(builder);
		addTransition1467(builder);
		addTransition1468(builder);
		addTransition1469(builder);
		addTransition1470(builder);
		addTransition1471(builder);
		addTransition1472(builder);
		addTransition1473(builder);
		addTransition1474(builder);
		addTransition1475(builder);
		addTransition1476(builder);
		addTransition1477(builder);
		addTransition1478(builder);
		addTransition1479(builder);
		addTransition1480(builder);
		addTransition1481(builder);
		addTransition1482(builder);
		addTransition1483(builder);
		addTransition1484(builder);
		addTransition1485(builder);
		addTransition1486(builder);
		addTransition1487(builder);
		addTransition1488(builder);
		addTransition1489(builder);
		addTransition1490(builder);
		addTransition1491(builder);
		addTransition1492(builder);
		addTransition1493(builder);
		addTransition1494(builder);
		addTransition1495(builder);
		addTransition1496(builder);
		addTransition1497(builder);
		addTransition1498(builder);
		addTransition1499(builder);
		addTransition1500(builder);
		addTransition1501(builder);
		addTransition1502(builder);
		addTransition1503(builder);
		addTransition1504(builder);
		addTransition1505(builder);
		addTransition1506(builder);
		addTransition1507(builder);
		addTransition1508(builder);
		addTransition1509(builder);
		addTransition1510(builder);
		addTransition1511(builder);
		addTransition1512(builder);
		addTransition1513(builder);
		addTransition1514(builder);
		addTransition1515(builder);
		addTransition1516(builder);
		addTransition1517(builder);
		addTransition1518(builder);
		addTransition1519(builder);
		addTransition1520(builder);
		addTransition1521(builder);
		addTransition1522(builder);
		addTransition1523(builder);
		addTransition1524(builder);
		addTransition1525(builder);
		addTransition1526(builder);
		addTransition1527(builder);
		addTransition1528(builder);
		addTransition1529(builder);
		addTransition1530(builder);
		addTransition1531(builder);
		addTransition1532(builder);
		addTransition1533(builder);
		addTransition1534(builder);
		addTransition1535(builder);
	}

	private static void addTransitionBatch12(QuestDsl.QuestBuilder builder) {
		addTransition1536(builder);
		addTransition1537(builder);
		addTransition1538(builder);
		addTransition1539(builder);
		addTransition1540(builder);
		addTransition1541(builder);
		addTransition1542(builder);
		addTransition1543(builder);
		addTransition1544(builder);
		addTransition1545(builder);
		addTransition1546(builder);
		addTransition1547(builder);
		addTransition1548(builder);
		addTransition1549(builder);
		addTransition1550(builder);
		addTransition1551(builder);
		addTransition1552(builder);
		addTransition1553(builder);
		addTransition1554(builder);
		addTransition1555(builder);
		addTransition1556(builder);
		addTransition1557(builder);
		addTransition1558(builder);
		addTransition1559(builder);
		addTransition1560(builder);
		addTransition1561(builder);
		addTransition1562(builder);
		addTransition1563(builder);
		addTransition1564(builder);
		addTransition1565(builder);
		addTransition1566(builder);
		addTransition1567(builder);
		addTransition1568(builder);
		addTransition1569(builder);
		addTransition1570(builder);
		addTransition1571(builder);
		addTransition1572(builder);
		addTransition1573(builder);
		addTransition1574(builder);
		addTransition1575(builder);
		addTransition1576(builder);
		addTransition1577(builder);
		addTransition1578(builder);
		addTransition1579(builder);
		addTransition1580(builder);
		addTransition1581(builder);
		addTransition1582(builder);
		addTransition1583(builder);
		addTransition1584(builder);
		addTransition1585(builder);
		addTransition1586(builder);
		addTransition1587(builder);
		addTransition1588(builder);
		addTransition1589(builder);
		addTransition1590(builder);
		addTransition1591(builder);
		addTransition1592(builder);
		addTransition1593(builder);
		addTransition1594(builder);
		addTransition1595(builder);
		addTransition1596(builder);
		addTransition1597(builder);
		addTransition1598(builder);
		addTransition1599(builder);
		addTransition1600(builder);
		addTransition1601(builder);
		addTransition1602(builder);
		addTransition1603(builder);
		addTransition1604(builder);
		addTransition1605(builder);
		addTransition1606(builder);
		addTransition1607(builder);
		addTransition1608(builder);
		addTransition1609(builder);
		addTransition1610(builder);
		addTransition1611(builder);
		addTransition1612(builder);
		addTransition1613(builder);
		addTransition1614(builder);
		addTransition1615(builder);
		addTransition1616(builder);
		addTransition1617(builder);
		addTransition1618(builder);
		addTransition1619(builder);
		addTransition1620(builder);
		addTransition1621(builder);
		addTransition1622(builder);
		addTransition1623(builder);
		addTransition1624(builder);
		addTransition1625(builder);
		addTransition1626(builder);
		addTransition1627(builder);
		addTransition1628(builder);
		addTransition1629(builder);
		addTransition1630(builder);
		addTransition1631(builder);
		addTransition1632(builder);
		addTransition1633(builder);
		addTransition1634(builder);
		addTransition1635(builder);
		addTransition1636(builder);
		addTransition1637(builder);
		addTransition1638(builder);
		addTransition1639(builder);
		addTransition1640(builder);
		addTransition1641(builder);
		addTransition1642(builder);
		addTransition1643(builder);
		addTransition1644(builder);
		addTransition1645(builder);
		addTransition1646(builder);
		addTransition1647(builder);
		addTransition1648(builder);
		addTransition1649(builder);
		addTransition1650(builder);
		addTransition1651(builder);
		addTransition1652(builder);
		addTransition1653(builder);
		addTransition1654(builder);
		addTransition1655(builder);
		addTransition1656(builder);
		addTransition1657(builder);
		addTransition1658(builder);
		addTransition1659(builder);
		addTransition1660(builder);
		addTransition1661(builder);
		addTransition1662(builder);
		addTransition1663(builder);
	}

	private static void addTransitionBatch13(QuestDsl.QuestBuilder builder) {
		addTransition1664(builder);
		addTransition1665(builder);
		addTransition1666(builder);
		addTransition1667(builder);
		addTransition1668(builder);
		addTransition1669(builder);
		addTransition1670(builder);
		addTransition1671(builder);
		addTransition1672(builder);
		addTransition1673(builder);
		addTransition1674(builder);
		addTransition1675(builder);
		addTransition1676(builder);
		addTransition1677(builder);
		addTransition1678(builder);
		addTransition1679(builder);
		addTransition1680(builder);
		addTransition1681(builder);
		addTransition1682(builder);
		addTransition1683(builder);
		addTransition1684(builder);
		addTransition1685(builder);
		addTransition1686(builder);
		addTransition1687(builder);
		addTransition1688(builder);
		addTransition1689(builder);
		addTransition1690(builder);
		addTransition1691(builder);
		addTransition1692(builder);
		addTransition1693(builder);
		addTransition1694(builder);
		addTransition1695(builder);
		addTransition1696(builder);
		addTransition1697(builder);
		addTransition1698(builder);
		addTransition1699(builder);
		addTransition1700(builder);
		addTransition1701(builder);
		addTransition1702(builder);
		addTransition1703(builder);
		addTransition1704(builder);
		addTransition1705(builder);
		addTransition1706(builder);
		addTransition1707(builder);
		addTransition1708(builder);
		addTransition1709(builder);
		addTransition1710(builder);
		addTransition1711(builder);
		addTransition1712(builder);
		addTransition1713(builder);
		addTransition1714(builder);
		addTransition1715(builder);
		addTransition1716(builder);
		addTransition1717(builder);
		addTransition1718(builder);
		addTransition1719(builder);
		addTransition1720(builder);
		addTransition1721(builder);
		addTransition1722(builder);
		addTransition1723(builder);
		addTransition1724(builder);
		addTransition1725(builder);
		addTransition1726(builder);
		addTransition1727(builder);
		addTransition1728(builder);
		addTransition1729(builder);
		addTransition1730(builder);
		addTransition1731(builder);
		addTransition1732(builder);
		addTransition1733(builder);
		addTransition1734(builder);
		addTransition1735(builder);
		addTransition1736(builder);
		addTransition1737(builder);
		addTransition1738(builder);
		addTransition1739(builder);
		addTransition1740(builder);
		addTransition1741(builder);
		addTransition1742(builder);
		addTransition1743(builder);
		addTransition1744(builder);
		addTransition1745(builder);
		addTransition1746(builder);
		addTransition1747(builder);
		addTransition1748(builder);
		addTransition1749(builder);
		addTransition1750(builder);
		addTransition1751(builder);
		addTransition1752(builder);
		addTransition1753(builder);
		addTransition1754(builder);
		addTransition1755(builder);
		addTransition1756(builder);
		addTransition1757(builder);
		addTransition1758(builder);
		addTransition1759(builder);
		addTransition1760(builder);
		addTransition1761(builder);
		addTransition1762(builder);
		addTransition1763(builder);
		addTransition1764(builder);
		addTransition1765(builder);
		addTransition1766(builder);
		addTransition1767(builder);
		addTransition1768(builder);
		addTransition1769(builder);
		addTransition1770(builder);
		addTransition1771(builder);
		addTransition1772(builder);
		addTransition1773(builder);
		addTransition1774(builder);
		addTransition1775(builder);
		addTransition1776(builder);
		addTransition1777(builder);
		addTransition1778(builder);
		addTransition1779(builder);
		addTransition1780(builder);
		addTransition1781(builder);
		addTransition1782(builder);
		addTransition1783(builder);
		addTransition1784(builder);
		addTransition1785(builder);
		addTransition1786(builder);
		addTransition1787(builder);
		addTransition1788(builder);
		addTransition1789(builder);
		addTransition1790(builder);
		addTransition1791(builder);
	}

	private static void addTransitionBatch14(QuestDsl.QuestBuilder builder) {
		addTransition1792(builder);
		addTransition1793(builder);
		addTransition1794(builder);
		addTransition1795(builder);
		addTransition1796(builder);
		addTransition1797(builder);
		addTransition1798(builder);
		addTransition1799(builder);
		addTransition1800(builder);
		addTransition1801(builder);
		addTransition1802(builder);
		addTransition1803(builder);
		addTransition1804(builder);
		addTransition1805(builder);
		addTransition1806(builder);
		addTransition1807(builder);
		addTransition1808(builder);
		addTransition1809(builder);
		addTransition1810(builder);
		addTransition1811(builder);
		addTransition1812(builder);
		addTransition1813(builder);
		addTransition1814(builder);
		addTransition1815(builder);
		addTransition1816(builder);
		addTransition1817(builder);
		addTransition1818(builder);
		addTransition1819(builder);
		addTransition1820(builder);
		addTransition1821(builder);
		addTransition1822(builder);
		addTransition1823(builder);
		addTransition1824(builder);
		addTransition1825(builder);
		addTransition1826(builder);
		addTransition1827(builder);
		addTransition1828(builder);
		addTransition1829(builder);
		addTransition1830(builder);
		addTransition1831(builder);
		addTransition1832(builder);
		addTransition1833(builder);
		addTransition1834(builder);
		addTransition1835(builder);
		addTransition1836(builder);
		addTransition1837(builder);
		addTransition1838(builder);
		addTransition1839(builder);
		addTransition1840(builder);
		addTransition1841(builder);
		addTransition1842(builder);
		addTransition1843(builder);
		addTransition1844(builder);
		addTransition1845(builder);
		addTransition1846(builder);
		addTransition1847(builder);
		addTransition1848(builder);
		addTransition1849(builder);
		addTransition1850(builder);
		addTransition1851(builder);
		addTransition1852(builder);
		addTransition1853(builder);
		addTransition1854(builder);
		addTransition1855(builder);
		addTransition1856(builder);
		addTransition1857(builder);
		addTransition1858(builder);
		addTransition1859(builder);
		addTransition1860(builder);
		addTransition1861(builder);
		addTransition1862(builder);
		addTransition1863(builder);
		addTransition1864(builder);
		addTransition1865(builder);
		addTransition1866(builder);
		addTransition1867(builder);
		addTransition1868(builder);
		addTransition1869(builder);
		addTransition1870(builder);
		addTransition1871(builder);
		addTransition1872(builder);
		addTransition1873(builder);
		addTransition1874(builder);
		addTransition1875(builder);
		addTransition1876(builder);
		addTransition1877(builder);
		addTransition1878(builder);
		addTransition1879(builder);
		addTransition1880(builder);
		addTransition1881(builder);
		addTransition1882(builder);
		addTransition1883(builder);
		addTransition1884(builder);
		addTransition1885(builder);
		addTransition1886(builder);
		addTransition1887(builder);
		addTransition1888(builder);
		addTransition1889(builder);
		addTransition1890(builder);
		addTransition1891(builder);
		addTransition1892(builder);
		addTransition1893(builder);
		addTransition1894(builder);
		addTransition1895(builder);
		addTransition1896(builder);
		addTransition1897(builder);
		addTransition1898(builder);
		addTransition1899(builder);
		addTransition1900(builder);
		addTransition1901(builder);
		addTransition1902(builder);
		addTransition1903(builder);
		addTransition1904(builder);
		addTransition1905(builder);
		addTransition1906(builder);
		addTransition1907(builder);
		addTransition1908(builder);
		addTransition1909(builder);
		addTransition1910(builder);
		addTransition1911(builder);
		addTransition1912(builder);
		addTransition1913(builder);
		addTransition1914(builder);
		addTransition1915(builder);
		addTransition1916(builder);
		addTransition1917(builder);
		addTransition1918(builder);
		addTransition1919(builder);
	}

	private static void addTransitionBatch15(QuestDsl.QuestBuilder builder) {
		addTransition1920(builder);
		addTransition1921(builder);
		addTransition1922(builder);
		addTransition1923(builder);
		addTransition1924(builder);
		addTransition1925(builder);
		addTransition1926(builder);
		addTransition1927(builder);
		addTransition1928(builder);
		addTransition1929(builder);
		addTransition1930(builder);
		addTransition1931(builder);
		addTransition1932(builder);
		addTransition1933(builder);
		addTransition1934(builder);
		addTransition1935(builder);
		addTransition1936(builder);
		addTransition1937(builder);
		addTransition1938(builder);
		addTransition1939(builder);
		addTransition1940(builder);
		addTransition1941(builder);
		addTransition1942(builder);
		addTransition1943(builder);
		addTransition1944(builder);
		addTransition1945(builder);
		addTransition1946(builder);
		addTransition1947(builder);
		addTransition1948(builder);
		addTransition1949(builder);
		addTransition1950(builder);
		addTransition1951(builder);
		addTransition1952(builder);
		addTransition1953(builder);
		addTransition1954(builder);
		addTransition1955(builder);
		addTransition1956(builder);
		addTransition1957(builder);
		addTransition1958(builder);
		addTransition1959(builder);
		addTransition1960(builder);
		addTransition1961(builder);
		addTransition1962(builder);
		addTransition1963(builder);
		addTransition1964(builder);
		addTransition1965(builder);
		addTransition1966(builder);
		addTransition1967(builder);
		addTransition1968(builder);
		addTransition1969(builder);
		addTransition1970(builder);
		addTransition1971(builder);
		addTransition1972(builder);
		addTransition1973(builder);
		addTransition1974(builder);
		addTransition1975(builder);
		addTransition1976(builder);
		addTransition1977(builder);
		addTransition1978(builder);
		addTransition1979(builder);
		addTransition1980(builder);
		addTransition1981(builder);
		addTransition1982(builder);
		addTransition1983(builder);
		addTransition1984(builder);
		addTransition1985(builder);
		addTransition1986(builder);
		addTransition1987(builder);
		addTransition1988(builder);
		addTransition1989(builder);
		addTransition1990(builder);
		addTransition1991(builder);
		addTransition1992(builder);
		addTransition1993(builder);
		addTransition1994(builder);
		addTransition1995(builder);
		addTransition1996(builder);
		addTransition1997(builder);
		addTransition1998(builder);
		addTransition1999(builder);
		addTransition2000(builder);
		addTransition2001(builder);
		addTransition2002(builder);
		addTransition2003(builder);
		addTransition2004(builder);
		addTransition2005(builder);
		addTransition2006(builder);
		addTransition2007(builder);
		addTransition2008(builder);
		addTransition2009(builder);
		addTransition2010(builder);
		addTransition2011(builder);
		addTransition2012(builder);
		addTransition2013(builder);
		addTransition2014(builder);
		addTransition2015(builder);
		addTransition2016(builder);
		addTransition2017(builder);
		addTransition2018(builder);
		addTransition2019(builder);
		addTransition2020(builder);
		addTransition2021(builder);
		addTransition2022(builder);
		addTransition2023(builder);
		addTransition2024(builder);
		addTransition2025(builder);
		addTransition2026(builder);
		addTransition2027(builder);
		addTransition2028(builder);
		addTransition2029(builder);
		addTransition2030(builder);
		addTransition2031(builder);
		addTransition2032(builder);
		addTransition2033(builder);
		addTransition2034(builder);
		addTransition2035(builder);
		addTransition2036(builder);
		addTransition2037(builder);
		addTransition2038(builder);
		addTransition2039(builder);
		addTransition2040(builder);
		addTransition2041(builder);
		addTransition2042(builder);
		addTransition2043(builder);
		addTransition2044(builder);
		addTransition2045(builder);
		addTransition2046(builder);
		addTransition2047(builder);
	}

	private static void addTransitionBatch16(QuestDsl.QuestBuilder builder) {
		addTransition2048(builder);
		addTransition2049(builder);
		addTransition2050(builder);
		addTransition2051(builder);
		addTransition2052(builder);
		addTransition2053(builder);
		addTransition2054(builder);
		addTransition2055(builder);
		addTransition2056(builder);
		addTransition2057(builder);
		addTransition2058(builder);
		addTransition2059(builder);
		addTransition2060(builder);
		addTransition2061(builder);
		addTransition2062(builder);
		addTransition2063(builder);
		addTransition2064(builder);
		addTransition2065(builder);
		addTransition2066(builder);
		addTransition2067(builder);
		addTransition2068(builder);
		addTransition2069(builder);
		addTransition2070(builder);
		addTransition2071(builder);
		addTransition2072(builder);
		addTransition2073(builder);
		addTransition2074(builder);
		addTransition2075(builder);
		addTransition2076(builder);
		addTransition2077(builder);
		addTransition2078(builder);
		addTransition2079(builder);
		addTransition2080(builder);
		addTransition2081(builder);
		addTransition2082(builder);
		addTransition2083(builder);
		addTransition2084(builder);
		addTransition2085(builder);
		addTransition2086(builder);
		addTransition2087(builder);
		addTransition2088(builder);
		addTransition2089(builder);
		addTransition2090(builder);
		addTransition2091(builder);
		addTransition2092(builder);
		addTransition2093(builder);
		addTransition2094(builder);
		addTransition2095(builder);
		addTransition2096(builder);
		addTransition2097(builder);
		addTransition2098(builder);
		addTransition2099(builder);
		addTransition2100(builder);
		addTransition2101(builder);
		addTransition2102(builder);
		addTransition2103(builder);
		addTransition2104(builder);
		addTransition2105(builder);
		addTransition2106(builder);
		addTransition2107(builder);
		addTransition2108(builder);
		addTransition2109(builder);
		addTransition2110(builder);
		addTransition2111(builder);
		addTransition2112(builder);
		addTransition2113(builder);
		addTransition2114(builder);
		addTransition2115(builder);
		addTransition2116(builder);
		addTransition2117(builder);
		addTransition2118(builder);
		addTransition2119(builder);
		addTransition2120(builder);
		addTransition2121(builder);
		addTransition2122(builder);
		addTransition2123(builder);
		addTransition2124(builder);
		addTransition2125(builder);
		addTransition2126(builder);
		addTransition2127(builder);
		addTransition2128(builder);
		addTransition2129(builder);
		addTransition2130(builder);
		addTransition2131(builder);
		addTransition2132(builder);
		addTransition2133(builder);
		addTransition2134(builder);
		addTransition2135(builder);
		addTransition2136(builder);
		addTransition2137(builder);
		addTransition2138(builder);
		addTransition2139(builder);
		addTransition2140(builder);
		addTransition2141(builder);
		addTransition2142(builder);
		addTransition2143(builder);
		addTransition2144(builder);
		addTransition2145(builder);
		addTransition2146(builder);
		addTransition2147(builder);
		addTransition2148(builder);
		addTransition2149(builder);
		addTransition2150(builder);
		addTransition2151(builder);
		addTransition2152(builder);
		addTransition2153(builder);
		addTransition2154(builder);
		addTransition2155(builder);
		addTransition2156(builder);
		addTransition2157(builder);
		addTransition2158(builder);
		addTransition2159(builder);
		addTransition2160(builder);
		addTransition2161(builder);
		addTransition2162(builder);
		addTransition2163(builder);
		addTransition2164(builder);
		addTransition2165(builder);
		addTransition2166(builder);
		addTransition2167(builder);
		addTransition2168(builder);
		addTransition2169(builder);
		addTransition2170(builder);
		addTransition2171(builder);
		addTransition2172(builder);
		addTransition2173(builder);
		addTransition2174(builder);
		addTransition2175(builder);
	}

	private static void addTransitionBatch17(QuestDsl.QuestBuilder builder) {
		addTransition2176(builder);
		addTransition2177(builder);
		addTransition2178(builder);
		addTransition2179(builder);
		addTransition2180(builder);
		addTransition2181(builder);
		addTransition2182(builder);
		addTransition2183(builder);
		addTransition2184(builder);
		addTransition2185(builder);
		addTransition2186(builder);
		addTransition2187(builder);
		addTransition2188(builder);
		addTransition2189(builder);
		addTransition2190(builder);
		addTransition2191(builder);
		addTransition2192(builder);
		addTransition2193(builder);
		addTransition2194(builder);
		addTransition2195(builder);
		addTransition2196(builder);
		addTransition2197(builder);
		addTransition2198(builder);
		addTransition2199(builder);
		addTransition2200(builder);
		addTransition2201(builder);
		addTransition2202(builder);
		addTransition2203(builder);
		addTransition2204(builder);
		addTransition2205(builder);
		addTransition2206(builder);
		addTransition2207(builder);
		addTransition2208(builder);
		addTransition2209(builder);
		addTransition2210(builder);
		addTransition2211(builder);
		addTransition2212(builder);
		addTransition2213(builder);
		addTransition2214(builder);
		addTransition2215(builder);
		addTransition2216(builder);
		addTransition2217(builder);
		addTransition2218(builder);
		addTransition2219(builder);
		addTransition2220(builder);
		addTransition2221(builder);
		addTransition2222(builder);
		addTransition2223(builder);
		addTransition2224(builder);
		addTransition2225(builder);
		addTransition2226(builder);
		addTransition2227(builder);
		addTransition2228(builder);
		addTransition2229(builder);
		addTransition2230(builder);
		addTransition2231(builder);
		addTransition2232(builder);
		addTransition2233(builder);
		addTransition2234(builder);
		addTransition2235(builder);
		addTransition2236(builder);
		addTransition2237(builder);
		addTransition2238(builder);
		addTransition2239(builder);
		addTransition2240(builder);
		addTransition2241(builder);
		addTransition2242(builder);
		addTransition2243(builder);
		addTransition2244(builder);
		addTransition2245(builder);
		addTransition2246(builder);
		addTransition2247(builder);
		addTransition2248(builder);
		addTransition2249(builder);
		addTransition2250(builder);
		addTransition2251(builder);
		addTransition2252(builder);
		addTransition2253(builder);
		addTransition2254(builder);
		addTransition2255(builder);
		addTransition2256(builder);
		addTransition2257(builder);
		addTransition2258(builder);
		addTransition2259(builder);
		addTransition2260(builder);
		addTransition2261(builder);
		addTransition2262(builder);
		addTransition2263(builder);
		addTransition2264(builder);
		addTransition2265(builder);
		addTransition2266(builder);
		addTransition2267(builder);
		addTransition2268(builder);
		addTransition2269(builder);
		addTransition2270(builder);
		addTransition2271(builder);
		addTransition2272(builder);
		addTransition2273(builder);
		addTransition2274(builder);
		addTransition2275(builder);
		addTransition2276(builder);
		addTransition2277(builder);
		addTransition2278(builder);
		addTransition2279(builder);
		addTransition2280(builder);
		addTransition2281(builder);
		addTransition2282(builder);
		addTransition2283(builder);
		addTransition2284(builder);
		addTransition2285(builder);
		addTransition2286(builder);
		addTransition2287(builder);
		addTransition2288(builder);
		addTransition2289(builder);
		addTransition2290(builder);
		addTransition2291(builder);
		addTransition2292(builder);
		addTransition2293(builder);
		addTransition2294(builder);
		addTransition2295(builder);
		addTransition2296(builder);
		addTransition2297(builder);
		addTransition2298(builder);
		addTransition2299(builder);
		addTransition2300(builder);
		addTransition2301(builder);
		addTransition2302(builder);
		addTransition2303(builder);
	}

	private static void addTransitionBatch18(QuestDsl.QuestBuilder builder) {
		addTransition2304(builder);
		addTransition2305(builder);
		addTransition2306(builder);
		addTransition2307(builder);
		addTransition2308(builder);
		addTransition2309(builder);
		addTransition2310(builder);
		addTransition2311(builder);
		addTransition2312(builder);
		addTransition2313(builder);
		addTransition2314(builder);
		addTransition2315(builder);
		addTransition2316(builder);
		addTransition2317(builder);
		addTransition2318(builder);
		addTransition2319(builder);
		addTransition2320(builder);
		addTransition2321(builder);
		addTransition2322(builder);
		addTransition2323(builder);
		addTransition2324(builder);
		addTransition2325(builder);
		addTransition2326(builder);
		addTransition2327(builder);
		addTransition2328(builder);
		addTransition2329(builder);
		addTransition2330(builder);
		addTransition2331(builder);
		addTransition2332(builder);
		addTransition2333(builder);
		addTransition2334(builder);
		addTransition2335(builder);
		addTransition2336(builder);
		addTransition2337(builder);
		addTransition2338(builder);
		addTransition2339(builder);
		addTransition2340(builder);
		addTransition2341(builder);
		addTransition2342(builder);
		addTransition2343(builder);
		addTransition2344(builder);
		addTransition2345(builder);
		addTransition2346(builder);
		addTransition2347(builder);
		addTransition2348(builder);
		addTransition2349(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("a0b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("a0b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 1008, 0)).from("a0b0c0").goTo("a0b0c0");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b0c0").goTo("a1b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b0c0").goTo("a1b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b0c0").goTo("a1b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b0c0").goTo("a1b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b0c1").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b0c1").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b0c1").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b0c1").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b1c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b1c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b1c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b1c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b1c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b1c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b1c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b1c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b2c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b2c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b2c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b2c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b2c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b2c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b2c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b2c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b3c0").goTo("a1b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b3c0").goTo("a1b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b3c0").goTo("a1b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b3c0").goTo("a1b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b3c1").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b3c1").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b3c1").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b3c1").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b4c0").goTo("a1b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b4c0").goTo("a1b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b4c0").goTo("a1b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b4c0").goTo("a1b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b4c1").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b4c1").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b4c1").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b4c1").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b5c0").goTo("a1b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b5c0").goTo("a1b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b5c0").goTo("a1b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b5c0").goTo("a1b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b5c1").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b5c1").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b5c1").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b5c1").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b6c0").goTo("a1b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b6c0").goTo("a1b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b6c0").goTo("a1b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b6c0").goTo("a1b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b6c1").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b6c1").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b6c1").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b6c1").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b7c0").goTo("a1b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b7c0").goTo("a1b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b7c0").goTo("a1b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b7c0").goTo("a1b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b7c1").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b7c1").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b7c1").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b7c1").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b8c0").goTo("a1b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b8c0").goTo("a1b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition75(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b8c0").goTo("a1b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition76(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b8c0").goTo("a1b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition77(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b8c1").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition78(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b8c1").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition79(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b8c1").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition80(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b8c1").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition81(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b9c0").goTo("a1b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition82(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b9c0").goTo("a1b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition83(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b9c0").goTo("a1b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition84(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b9c0").goTo("a1b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition85(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b9c1").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition86(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b9c1").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition87(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b9c1").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition88(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b9c1").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition89(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b10c0").goTo("a1b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition90(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b10c0").goTo("a1b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition91(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b10c0").goTo("a1b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition92(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b10c0").goTo("a1b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition93(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a0b10c1").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition94(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a0b10c1").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition95(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a0b10c1").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition96(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a0b10c1").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition97(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b0c0").goTo("a2b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition98(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b0c0").goTo("a2b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition99(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b0c0").goTo("a2b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition100(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b0c0").goTo("a2b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition101(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b0c1").goTo("a2b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition102(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b0c1").goTo("a2b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition103(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b0c1").goTo("a2b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition104(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b0c1").goTo("a2b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition105(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b1c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition106(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b1c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition107(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b1c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition108(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b1c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition109(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b1c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition110(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b1c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition111(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b1c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition112(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b1c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition113(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b2c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition114(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b2c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition115(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b2c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition116(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b2c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition117(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b2c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition118(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b2c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition119(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b2c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition120(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b2c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition121(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b3c0").goTo("a2b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition122(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b3c0").goTo("a2b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition123(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b3c0").goTo("a2b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition124(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b3c0").goTo("a2b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition125(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b3c1").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition126(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b3c1").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition127(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b3c1").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition128(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b3c1").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition129(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b4c0").goTo("a2b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition130(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b4c0").goTo("a2b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition131(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b4c0").goTo("a2b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition132(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b4c0").goTo("a2b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition133(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b4c1").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition134(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b4c1").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition135(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b4c1").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition136(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b4c1").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition137(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b5c0").goTo("a2b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition138(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b5c0").goTo("a2b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition139(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b5c0").goTo("a2b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition140(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b5c0").goTo("a2b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition141(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b5c1").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition142(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b5c1").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition143(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b5c1").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition144(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b5c1").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition145(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b6c0").goTo("a2b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition146(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b6c0").goTo("a2b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition147(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b6c0").goTo("a2b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition148(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b6c0").goTo("a2b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition149(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b6c1").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition150(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b6c1").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition151(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b6c1").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition152(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b6c1").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition153(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b7c0").goTo("a2b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition154(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b7c0").goTo("a2b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition155(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b7c0").goTo("a2b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition156(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b7c0").goTo("a2b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition157(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b7c1").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition158(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b7c1").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition159(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b7c1").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition160(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b7c1").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition161(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b8c0").goTo("a2b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition162(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b8c0").goTo("a2b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition163(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b8c0").goTo("a2b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition164(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b8c0").goTo("a2b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition165(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b8c1").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition166(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b8c1").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition167(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b8c1").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition168(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b8c1").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition169(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b9c0").goTo("a2b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition170(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b9c0").goTo("a2b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition171(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b9c0").goTo("a2b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition172(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b9c0").goTo("a2b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition173(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b9c1").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition174(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b9c1").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition175(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b9c1").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition176(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b9c1").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition177(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b10c0").goTo("a2b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition178(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b10c0").goTo("a2b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition179(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b10c0").goTo("a2b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition180(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b10c0").goTo("a2b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition181(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a1b10c1").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition182(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a1b10c1").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition183(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a1b10c1").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition184(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a1b10c1").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition185(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b0c0").goTo("a3b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition186(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b0c0").goTo("a3b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition187(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b0c0").goTo("a3b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition188(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b0c0").goTo("a3b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition189(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b0c1").goTo("a3b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition190(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b0c1").goTo("a3b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition191(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b0c1").goTo("a3b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition192(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b0c1").goTo("a3b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition193(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b1c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition194(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b1c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition195(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b1c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition196(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b1c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition197(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b1c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition198(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b1c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition199(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b1c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition200(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b1c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition201(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b2c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition202(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b2c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition203(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b2c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition204(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b2c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition205(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b2c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition206(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b2c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition207(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b2c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition208(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b2c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition209(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b3c0").goTo("a3b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition210(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b3c0").goTo("a3b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition211(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b3c0").goTo("a3b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition212(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b3c0").goTo("a3b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition213(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b3c1").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition214(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b3c1").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition215(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b3c1").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition216(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b3c1").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition217(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b4c0").goTo("a3b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition218(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b4c0").goTo("a3b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition219(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b4c0").goTo("a3b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition220(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b4c0").goTo("a3b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition221(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b4c1").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition222(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b4c1").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition223(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b4c1").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition224(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b4c1").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition225(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b5c0").goTo("a3b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition226(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b5c0").goTo("a3b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition227(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b5c0").goTo("a3b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition228(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b5c0").goTo("a3b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition229(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b5c1").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition230(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b5c1").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition231(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b5c1").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition232(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b5c1").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition233(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b6c0").goTo("a3b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition234(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b6c0").goTo("a3b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition235(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b6c0").goTo("a3b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition236(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b6c0").goTo("a3b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition237(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b6c1").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition238(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b6c1").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition239(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b6c1").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition240(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b6c1").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition241(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b7c0").goTo("a3b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition242(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b7c0").goTo("a3b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition243(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b7c0").goTo("a3b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition244(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b7c0").goTo("a3b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition245(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b7c1").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition246(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b7c1").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition247(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b7c1").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition248(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b7c1").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition249(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b8c0").goTo("a3b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition250(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b8c0").goTo("a3b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition251(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b8c0").goTo("a3b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition252(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b8c0").goTo("a3b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition253(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b8c1").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition254(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b8c1").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition255(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b8c1").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition256(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b8c1").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition257(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b9c0").goTo("a3b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition258(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b9c0").goTo("a3b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition259(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b9c0").goTo("a3b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition260(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b9c0").goTo("a3b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition261(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b9c1").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition262(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b9c1").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition263(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b9c1").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition264(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b9c1").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition265(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b10c0").goTo("a3b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition266(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b10c0").goTo("a3b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition267(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b10c0").goTo("a3b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition268(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b10c0").goTo("a3b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition269(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a2b10c1").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition270(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a2b10c1").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition271(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a2b10c1").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition272(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a2b10c1").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition273(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b0c0").goTo("a4b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition274(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b0c0").goTo("a4b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition275(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b0c0").goTo("a4b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition276(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b0c0").goTo("a4b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition277(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b0c1").goTo("a4b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition278(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b0c1").goTo("a4b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition279(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b0c1").goTo("a4b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition280(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b0c1").goTo("a4b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition281(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b1c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition282(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b1c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition283(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b1c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition284(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b1c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition285(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b1c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition286(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b1c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition287(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b1c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition288(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b1c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition289(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b2c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition290(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b2c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition291(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b2c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition292(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b2c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition293(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b2c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition294(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b2c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition295(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b2c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition296(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b2c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition297(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b3c0").goTo("a4b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition298(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b3c0").goTo("a4b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition299(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b3c0").goTo("a4b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition300(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b3c0").goTo("a4b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition301(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b3c1").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition302(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b3c1").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition303(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b3c1").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition304(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b3c1").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition305(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b4c0").goTo("a4b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition306(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b4c0").goTo("a4b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition307(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b4c0").goTo("a4b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition308(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b4c0").goTo("a4b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition309(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b4c1").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition310(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b4c1").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition311(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b4c1").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition312(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b4c1").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition313(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b5c0").goTo("a4b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition314(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b5c0").goTo("a4b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition315(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b5c0").goTo("a4b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition316(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b5c0").goTo("a4b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition317(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b5c1").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition318(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b5c1").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition319(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b5c1").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition320(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b5c1").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition321(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b6c0").goTo("a4b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition322(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b6c0").goTo("a4b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition323(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b6c0").goTo("a4b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition324(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b6c0").goTo("a4b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition325(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b6c1").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition326(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b6c1").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition327(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b6c1").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition328(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b6c1").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition329(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b7c0").goTo("a4b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition330(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b7c0").goTo("a4b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition331(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b7c0").goTo("a4b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition332(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b7c0").goTo("a4b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition333(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b7c1").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition334(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b7c1").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition335(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b7c1").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition336(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b7c1").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition337(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b8c0").goTo("a4b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition338(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b8c0").goTo("a4b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition339(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b8c0").goTo("a4b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition340(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b8c0").goTo("a4b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition341(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b8c1").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition342(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b8c1").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition343(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b8c1").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition344(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b8c1").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition345(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b9c0").goTo("a4b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition346(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b9c0").goTo("a4b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition347(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b9c0").goTo("a4b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition348(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b9c0").goTo("a4b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition349(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b9c1").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition350(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b9c1").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition351(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b9c1").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition352(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b9c1").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition353(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b10c0").goTo("a4b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition354(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b10c0").goTo("a4b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition355(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b10c0").goTo("a4b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition356(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b10c0").goTo("a4b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition357(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a3b10c1").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition358(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a3b10c1").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition359(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a3b10c1").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition360(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a3b10c1").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition361(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b0c0").goTo("a5b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition362(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b0c0").goTo("a5b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition363(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b0c0").goTo("a5b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition364(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b0c0").goTo("a5b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition365(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b0c1").goTo("a5b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition366(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b0c1").goTo("a5b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition367(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b0c1").goTo("a5b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition368(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b0c1").goTo("a5b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition369(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b1c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition370(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b1c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition371(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b1c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition372(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b1c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition373(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b1c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition374(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b1c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition375(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b1c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition376(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b1c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition377(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b2c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition378(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b2c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition379(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b2c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition380(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b2c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition381(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b2c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition382(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b2c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition383(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b2c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition384(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b2c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition385(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b3c0").goTo("a5b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition386(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b3c0").goTo("a5b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition387(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b3c0").goTo("a5b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition388(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b3c0").goTo("a5b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition389(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b3c1").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition390(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b3c1").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition391(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b3c1").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition392(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b3c1").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition393(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b4c0").goTo("a5b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition394(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b4c0").goTo("a5b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition395(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b4c0").goTo("a5b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition396(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b4c0").goTo("a5b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition397(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b4c1").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition398(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b4c1").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition399(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b4c1").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition400(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b4c1").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition401(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b5c0").goTo("a5b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition402(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b5c0").goTo("a5b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition403(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b5c0").goTo("a5b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition404(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b5c0").goTo("a5b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition405(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b5c1").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition406(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b5c1").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition407(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b5c1").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition408(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b5c1").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition409(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b6c0").goTo("a5b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition410(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b6c0").goTo("a5b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition411(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b6c0").goTo("a5b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition412(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b6c0").goTo("a5b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition413(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b6c1").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition414(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b6c1").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition415(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b6c1").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition416(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b6c1").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition417(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b7c0").goTo("a5b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition418(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b7c0").goTo("a5b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition419(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b7c0").goTo("a5b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition420(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b7c0").goTo("a5b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition421(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b7c1").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition422(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b7c1").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition423(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b7c1").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition424(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b7c1").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition425(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b8c0").goTo("a5b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition426(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b8c0").goTo("a5b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition427(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b8c0").goTo("a5b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition428(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b8c0").goTo("a5b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition429(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b8c1").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition430(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b8c1").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition431(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b8c1").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition432(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b8c1").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition433(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b9c0").goTo("a5b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition434(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b9c0").goTo("a5b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition435(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b9c0").goTo("a5b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition436(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b9c0").goTo("a5b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition437(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b9c1").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition438(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b9c1").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition439(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b9c1").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition440(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b9c1").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition441(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b10c0").goTo("a5b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition442(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b10c0").goTo("a5b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition443(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b10c0").goTo("a5b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition444(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b10c0").goTo("a5b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition445(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a4b10c1").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition446(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a4b10c1").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition447(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a4b10c1").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition448(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a4b10c1").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition449(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b0c0").goTo("a6b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition450(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b0c0").goTo("a6b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition451(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b0c0").goTo("a6b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition452(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b0c0").goTo("a6b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition453(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b0c1").goTo("a6b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition454(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b0c1").goTo("a6b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition455(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b0c1").goTo("a6b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition456(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b0c1").goTo("a6b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition457(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b1c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition458(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b1c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition459(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b1c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition460(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b1c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition461(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b1c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition462(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b1c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition463(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b1c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition464(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b1c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition465(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b2c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition466(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b2c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition467(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b2c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition468(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b2c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition469(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b2c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition470(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b2c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition471(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b2c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition472(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b2c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition473(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b3c0").goTo("a6b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition474(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b3c0").goTo("a6b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition475(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b3c0").goTo("a6b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition476(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b3c0").goTo("a6b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition477(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b3c1").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition478(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b3c1").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition479(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b3c1").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition480(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b3c1").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition481(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b4c0").goTo("a6b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition482(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b4c0").goTo("a6b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition483(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b4c0").goTo("a6b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition484(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b4c0").goTo("a6b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition485(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b4c1").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition486(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b4c1").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition487(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b4c1").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition488(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b4c1").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition489(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b5c0").goTo("a6b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition490(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b5c0").goTo("a6b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition491(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b5c0").goTo("a6b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition492(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b5c0").goTo("a6b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition493(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b5c1").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition494(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b5c1").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition495(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b5c1").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition496(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b5c1").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition497(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b6c0").goTo("a6b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition498(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b6c0").goTo("a6b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition499(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b6c0").goTo("a6b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition500(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b6c0").goTo("a6b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition501(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b6c1").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition502(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b6c1").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition503(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b6c1").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition504(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b6c1").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition505(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b7c0").goTo("a6b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition506(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b7c0").goTo("a6b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition507(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b7c0").goTo("a6b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition508(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b7c0").goTo("a6b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition509(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b7c1").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition510(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b7c1").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition511(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b7c1").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition512(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b7c1").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition513(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b8c0").goTo("a6b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition514(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b8c0").goTo("a6b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition515(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b8c0").goTo("a6b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition516(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b8c0").goTo("a6b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition517(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b8c1").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition518(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b8c1").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition519(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b8c1").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition520(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b8c1").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition521(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b9c0").goTo("a6b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition522(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b9c0").goTo("a6b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition523(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b9c0").goTo("a6b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition524(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b9c0").goTo("a6b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition525(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b9c1").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition526(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b9c1").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition527(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b9c1").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition528(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b9c1").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition529(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b10c0").goTo("a6b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition530(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b10c0").goTo("a6b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition531(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b10c0").goTo("a6b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition532(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b10c0").goTo("a6b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition533(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a5b10c1").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition534(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a5b10c1").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition535(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a5b10c1").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition536(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a5b10c1").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition537(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b0c0").goTo("a7b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition538(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b0c0").goTo("a7b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition539(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b0c0").goTo("a7b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition540(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b0c0").goTo("a7b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition541(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b0c1").goTo("a7b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition542(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b0c1").goTo("a7b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition543(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b0c1").goTo("a7b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition544(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b0c1").goTo("a7b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition545(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b1c0").goTo("a7b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition546(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b1c0").goTo("a7b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition547(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b1c0").goTo("a7b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition548(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b1c0").goTo("a7b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition549(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b1c1").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition550(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b1c1").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition551(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b1c1").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition552(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b1c1").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition553(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b2c0").goTo("a7b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition554(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b2c0").goTo("a7b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition555(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b2c0").goTo("a7b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition556(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b2c0").goTo("a7b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition557(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b2c1").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition558(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b2c1").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition559(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b2c1").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition560(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b2c1").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition561(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b3c0").goTo("a7b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition562(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b3c0").goTo("a7b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition563(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b3c0").goTo("a7b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition564(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b3c0").goTo("a7b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition565(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b3c1").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition566(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b3c1").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition567(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b3c1").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition568(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b3c1").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition569(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b4c0").goTo("a7b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition570(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b4c0").goTo("a7b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition571(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b4c0").goTo("a7b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition572(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b4c0").goTo("a7b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition573(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b4c1").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition574(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b4c1").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition575(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b4c1").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition576(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b4c1").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition577(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b5c0").goTo("a7b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition578(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b5c0").goTo("a7b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition579(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b5c0").goTo("a7b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition580(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b5c0").goTo("a7b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition581(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b5c1").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition582(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b5c1").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition583(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b5c1").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition584(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b5c1").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition585(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b6c0").goTo("a7b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition586(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b6c0").goTo("a7b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition587(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b6c0").goTo("a7b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition588(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b6c0").goTo("a7b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition589(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b6c1").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition590(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b6c1").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition591(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b6c1").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition592(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b6c1").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition593(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b7c0").goTo("a7b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition594(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b7c0").goTo("a7b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition595(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b7c0").goTo("a7b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition596(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b7c0").goTo("a7b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition597(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b7c1").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition598(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b7c1").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition599(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b7c1").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition600(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b7c1").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition601(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b8c0").goTo("a7b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition602(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b8c0").goTo("a7b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition603(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b8c0").goTo("a7b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition604(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b8c0").goTo("a7b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition605(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b8c1").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition606(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b8c1").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition607(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b8c1").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition608(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b8c1").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition609(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b9c0").goTo("a7b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition610(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b9c0").goTo("a7b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition611(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b9c0").goTo("a7b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition612(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b9c0").goTo("a7b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition613(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b9c1").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition614(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b9c1").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition615(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b9c1").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition616(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b9c1").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition617(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b10c0").goTo("a7b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition618(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b10c0").goTo("a7b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition619(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b10c0").goTo("a7b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition620(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b10c0").goTo("a7b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition621(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a6b10c1").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition622(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a6b10c1").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition623(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a6b10c1").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition624(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a6b10c1").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition625(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b0c0").goTo("a8b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition626(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b0c0").goTo("a8b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition627(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b0c0").goTo("a8b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition628(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b0c0").goTo("a8b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition629(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b0c1").goTo("a8b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition630(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b0c1").goTo("a8b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition631(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b0c1").goTo("a8b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition632(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b0c1").goTo("a8b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition633(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b1c0").goTo("a8b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition634(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b1c0").goTo("a8b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition635(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b1c0").goTo("a8b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition636(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b1c0").goTo("a8b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition637(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b1c1").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition638(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b1c1").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition639(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b1c1").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition640(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b1c1").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition641(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b2c0").goTo("a8b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition642(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b2c0").goTo("a8b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition643(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b2c0").goTo("a8b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition644(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b2c0").goTo("a8b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition645(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b2c1").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition646(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b2c1").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition647(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b2c1").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition648(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b2c1").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition649(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b3c0").goTo("a8b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition650(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b3c0").goTo("a8b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition651(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b3c0").goTo("a8b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition652(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b3c0").goTo("a8b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition653(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b3c1").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition654(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b3c1").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition655(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b3c1").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition656(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b3c1").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition657(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b4c0").goTo("a8b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition658(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b4c0").goTo("a8b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition659(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b4c0").goTo("a8b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition660(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b4c0").goTo("a8b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition661(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b4c1").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition662(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b4c1").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition663(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b4c1").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition664(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b4c1").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition665(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b5c0").goTo("a8b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition666(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b5c0").goTo("a8b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition667(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b5c0").goTo("a8b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition668(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b5c0").goTo("a8b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition669(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b5c1").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition670(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b5c1").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition671(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b5c1").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition672(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b5c1").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition673(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b6c0").goTo("a8b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition674(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b6c0").goTo("a8b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition675(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b6c0").goTo("a8b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition676(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b6c0").goTo("a8b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition677(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b6c1").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition678(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b6c1").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition679(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b6c1").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition680(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b6c1").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition681(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b7c0").goTo("a8b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition682(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b7c0").goTo("a8b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition683(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b7c0").goTo("a8b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition684(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b7c0").goTo("a8b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition685(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b7c1").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition686(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b7c1").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition687(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b7c1").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition688(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b7c1").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition689(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b8c0").goTo("a8b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition690(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b8c0").goTo("a8b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition691(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b8c0").goTo("a8b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition692(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b8c0").goTo("a8b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition693(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b8c1").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition694(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b8c1").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition695(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b8c1").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition696(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b8c1").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition697(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b9c0").goTo("a8b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition698(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b9c0").goTo("a8b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition699(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b9c0").goTo("a8b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition700(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b9c0").goTo("a8b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition701(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b9c1").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition702(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b9c1").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition703(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b9c1").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition704(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b9c1").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition705(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b10c0").goTo("a8b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition706(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b10c0").goTo("a8b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition707(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b10c0").goTo("a8b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition708(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b10c0").goTo("a8b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition709(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a7b10c1").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition710(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a7b10c1").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition711(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a7b10c1").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition712(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a7b10c1").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition713(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b0c0").goTo("a9b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition714(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b0c0").goTo("a9b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition715(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b0c0").goTo("a9b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition716(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b0c0").goTo("a9b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition717(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b0c1").goTo("a9b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition718(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b0c1").goTo("a9b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition719(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b0c1").goTo("a9b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition720(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b0c1").goTo("a9b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition721(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b1c0").goTo("a9b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition722(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b1c0").goTo("a9b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition723(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b1c0").goTo("a9b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition724(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b1c0").goTo("a9b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition725(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b1c1").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition726(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b1c1").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition727(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b1c1").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition728(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b1c1").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition729(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b2c0").goTo("a9b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition730(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b2c0").goTo("a9b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition731(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b2c0").goTo("a9b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition732(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b2c0").goTo("a9b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition733(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b2c1").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition734(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b2c1").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition735(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b2c1").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition736(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b2c1").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition737(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b3c0").goTo("a9b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition738(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b3c0").goTo("a9b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition739(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b3c0").goTo("a9b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition740(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b3c0").goTo("a9b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition741(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b3c1").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition742(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b3c1").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition743(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b3c1").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition744(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b3c1").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition745(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b4c0").goTo("a9b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition746(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b4c0").goTo("a9b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition747(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b4c0").goTo("a9b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition748(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b4c0").goTo("a9b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition749(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b4c1").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition750(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b4c1").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition751(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b4c1").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition752(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b4c1").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition753(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b5c0").goTo("a9b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition754(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b5c0").goTo("a9b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition755(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b5c0").goTo("a9b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition756(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b5c0").goTo("a9b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition757(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b5c1").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition758(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b5c1").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition759(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b5c1").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition760(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b5c1").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition761(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b6c0").goTo("a9b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition762(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b6c0").goTo("a9b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition763(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b6c0").goTo("a9b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition764(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b6c0").goTo("a9b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition765(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b6c1").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition766(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b6c1").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition767(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b6c1").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition768(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b6c1").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition769(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b7c0").goTo("a9b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition770(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b7c0").goTo("a9b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition771(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b7c0").goTo("a9b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition772(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b7c0").goTo("a9b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition773(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b7c1").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition774(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b7c1").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition775(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b7c1").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition776(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b7c1").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition777(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b8c0").goTo("a9b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition778(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b8c0").goTo("a9b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition779(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b8c0").goTo("a9b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition780(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b8c0").goTo("a9b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition781(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b8c1").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition782(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b8c1").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition783(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b8c1").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition784(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b8c1").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition785(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b9c0").goTo("a9b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition786(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b9c0").goTo("a9b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition787(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b9c0").goTo("a9b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition788(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b9c0").goTo("a9b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition789(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b9c1").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition790(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b9c1").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition791(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b9c1").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition792(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b9c1").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition793(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b10c0").goTo("a9b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition794(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b10c0").goTo("a9b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition795(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b10c0").goTo("a9b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition796(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b10c0").goTo("a9b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition797(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a8b10c1").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition798(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a8b10c1").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition799(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a8b10c1").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition800(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a8b10c1").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition801(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b0c0").goTo("a10b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition802(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b0c0").goTo("a10b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition803(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b0c0").goTo("a10b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition804(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b0c0").goTo("a10b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition805(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b0c1").goTo("a10b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition806(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b0c1").goTo("a10b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition807(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b0c1").goTo("a10b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition808(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b0c1").goTo("a10b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition809(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b1c0").goTo("a10b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition810(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b1c0").goTo("a10b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition811(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b1c0").goTo("a10b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition812(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b1c0").goTo("a10b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition813(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b1c1").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition814(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b1c1").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition815(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b1c1").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition816(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b1c1").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition817(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b2c0").goTo("a10b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition818(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b2c0").goTo("a10b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition819(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b2c0").goTo("a10b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition820(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b2c0").goTo("a10b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition821(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b2c1").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition822(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b2c1").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition823(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b2c1").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition824(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b2c1").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition825(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b3c0").goTo("a10b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition826(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b3c0").goTo("a10b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition827(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b3c0").goTo("a10b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition828(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b3c0").goTo("a10b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition829(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b3c1").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition830(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b3c1").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition831(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b3c1").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition832(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b3c1").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition833(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b4c0").goTo("a10b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition834(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b4c0").goTo("a10b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition835(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b4c0").goTo("a10b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition836(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b4c0").goTo("a10b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition837(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b4c1").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition838(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b4c1").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition839(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b4c1").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition840(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b4c1").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition841(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b5c0").goTo("a10b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition842(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b5c0").goTo("a10b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition843(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b5c0").goTo("a10b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition844(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b5c0").goTo("a10b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition845(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b5c1").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition846(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b5c1").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition847(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b5c1").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition848(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b5c1").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition849(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b6c0").goTo("a10b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition850(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b6c0").goTo("a10b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition851(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b6c0").goTo("a10b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition852(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b6c0").goTo("a10b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition853(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b6c1").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition854(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b6c1").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition855(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b6c1").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition856(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b6c1").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition857(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b7c0").goTo("a10b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition858(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b7c0").goTo("a10b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition859(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b7c0").goTo("a10b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition860(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b7c0").goTo("a10b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition861(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b7c1").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition862(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b7c1").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition863(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b7c1").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition864(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b7c1").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition865(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b8c0").goTo("a10b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition866(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b8c0").goTo("a10b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition867(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b8c0").goTo("a10b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition868(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b8c0").goTo("a10b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition869(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b8c1").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition870(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b8c1").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition871(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b8c1").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition872(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b8c1").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition873(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b9c0").goTo("a10b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition874(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b9c0").goTo("a10b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition875(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b9c0").goTo("a10b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition876(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b9c0").goTo("a10b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition877(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b9c1").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition878(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b9c1").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition879(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b9c1").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition880(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b9c1").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition881(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b10c0").goTo("a10b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition882(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b10c0").goTo("a10b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition883(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b10c0").goTo("a10b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition884(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b10c0").goTo("a10b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition885(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(219604, 219603, 219602, 219601, 219600, 219638, 219637, 219636, 219635, 219634))).from("a9b10c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition886(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219606)).from("a9b10c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition887(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219608)).from("a9b10c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition888(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219611)).from("a9b10c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition889(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b0c0").goTo("a0b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition890(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b0c0").goTo("a0b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition891(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b0c0").goTo("a0b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition892(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b0c0").goTo("a0b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition893(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b0c0").goTo("a0b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition894(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b0c0").goTo("a0b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition895(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b0c1").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition896(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b0c1").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition897(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b0c1").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition898(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b0c1").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition899(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b0c1").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition900(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b0c1").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition901(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b1c0").goTo("a0b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition902(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b1c0").goTo("a0b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition903(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b1c0").goTo("a0b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition904(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b1c0").goTo("a0b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition905(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b1c0").goTo("a0b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition906(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b1c0").goTo("a0b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition907(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b1c1").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition908(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b1c1").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition909(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b1c1").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition910(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b1c1").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition911(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b1c1").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition912(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b1c1").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition913(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b2c0").goTo("a0b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition914(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b2c0").goTo("a0b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition915(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b2c0").goTo("a0b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition916(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b2c0").goTo("a0b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition917(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b2c0").goTo("a0b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition918(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b2c0").goTo("a0b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition919(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b2c1").goTo("a0b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition920(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b2c1").goTo("a0b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition921(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b2c1").goTo("a0b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition922(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b2c1").goTo("a0b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition923(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b2c1").goTo("a0b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition924(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b2c1").goTo("a0b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition925(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b3c0").goTo("a0b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition926(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b3c0").goTo("a0b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition927(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b3c0").goTo("a0b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition928(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b3c0").goTo("a0b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition929(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b3c0").goTo("a0b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition930(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b3c0").goTo("a0b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition931(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b3c1").goTo("a0b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition932(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b3c1").goTo("a0b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition933(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b3c1").goTo("a0b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition934(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b3c1").goTo("a0b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition935(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b3c1").goTo("a0b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition936(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b3c1").goTo("a0b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition937(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b4c0").goTo("a0b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition938(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b4c0").goTo("a0b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition939(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b4c0").goTo("a0b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition940(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b4c0").goTo("a0b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition941(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b4c0").goTo("a0b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition942(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b4c0").goTo("a0b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition943(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b4c1").goTo("a0b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition944(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b4c1").goTo("a0b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition945(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b4c1").goTo("a0b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition946(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b4c1").goTo("a0b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition947(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b4c1").goTo("a0b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition948(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b4c1").goTo("a0b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition949(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b5c0").goTo("a0b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition950(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b5c0").goTo("a0b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition951(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b5c0").goTo("a0b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition952(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b5c0").goTo("a0b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition953(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b5c0").goTo("a0b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition954(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b5c0").goTo("a0b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition955(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b5c1").goTo("a0b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition956(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b5c1").goTo("a0b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition957(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b5c1").goTo("a0b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition958(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b5c1").goTo("a0b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition959(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b5c1").goTo("a0b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition960(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b5c1").goTo("a0b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition961(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b6c0").goTo("a0b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition962(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b6c0").goTo("a0b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition963(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b6c0").goTo("a0b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition964(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b6c0").goTo("a0b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition965(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b6c0").goTo("a0b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition966(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b6c0").goTo("a0b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition967(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b6c1").goTo("a0b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition968(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b6c1").goTo("a0b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition969(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b6c1").goTo("a0b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition970(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b6c1").goTo("a0b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition971(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b6c1").goTo("a0b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition972(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b6c1").goTo("a0b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition973(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b7c0").goTo("a0b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition974(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b7c0").goTo("a0b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition975(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b7c0").goTo("a0b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition976(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b7c0").goTo("a0b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition977(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b7c0").goTo("a0b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition978(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b7c0").goTo("a0b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition979(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b7c1").goTo("a0b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition980(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b7c1").goTo("a0b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition981(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b7c1").goTo("a0b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition982(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b7c1").goTo("a0b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition983(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b7c1").goTo("a0b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition984(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b7c1").goTo("a0b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition985(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b8c0").goTo("a0b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition986(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b8c0").goTo("a0b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition987(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b8c0").goTo("a0b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition988(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b8c0").goTo("a0b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition989(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b8c0").goTo("a0b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition990(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b8c0").goTo("a0b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition991(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b8c1").goTo("a0b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition992(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b8c1").goTo("a0b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition993(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b8c1").goTo("a0b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition994(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b8c1").goTo("a0b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition995(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b8c1").goTo("a0b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition996(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b8c1").goTo("a0b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition997(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b9c0").goTo("a0b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition998(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b9c0").goTo("a0b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition999(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b9c0").goTo("a0b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1000(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b9c0").goTo("a0b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1001(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b9c0").goTo("a0b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1002(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b9c0").goTo("a0b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1003(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a0b9c1").goTo("a0b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1004(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a0b9c1").goTo("a0b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1005(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a0b9c1").goTo("a0b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1006(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a0b9c1").goTo("a0b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1007(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a0b9c1").goTo("a0b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1008(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a0b9c1").goTo("a0b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1009(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b0c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1010(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b0c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1011(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b0c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1012(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b0c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1013(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b0c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1014(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b0c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1015(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b0c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1016(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b0c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1017(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b0c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1018(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b0c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1019(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b0c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1020(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b0c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1021(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b1c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1022(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b1c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1023(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b1c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1024(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b1c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1025(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b1c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1026(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b1c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1027(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b1c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1028(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b1c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1029(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b1c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1030(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b1c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1031(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b1c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1032(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b1c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1033(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b2c0").goTo("a1b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1034(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b2c0").goTo("a1b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1035(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b2c0").goTo("a1b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1036(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b2c0").goTo("a1b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1037(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b2c0").goTo("a1b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1038(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b2c0").goTo("a1b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1039(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b2c1").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1040(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b2c1").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1041(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b2c1").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1042(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b2c1").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1043(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b2c1").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1044(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b2c1").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1045(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b3c0").goTo("a1b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1046(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b3c0").goTo("a1b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1047(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b3c0").goTo("a1b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1048(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b3c0").goTo("a1b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1049(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b3c0").goTo("a1b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1050(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b3c0").goTo("a1b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1051(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b3c1").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1052(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b3c1").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1053(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b3c1").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1054(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b3c1").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1055(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b3c1").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1056(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b3c1").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1057(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b4c0").goTo("a1b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1058(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b4c0").goTo("a1b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1059(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b4c0").goTo("a1b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1060(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b4c0").goTo("a1b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1061(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b4c0").goTo("a1b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1062(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b4c0").goTo("a1b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1063(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b4c1").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1064(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b4c1").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1065(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b4c1").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1066(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b4c1").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1067(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b4c1").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1068(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b4c1").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1069(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b5c0").goTo("a1b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1070(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b5c0").goTo("a1b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1071(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b5c0").goTo("a1b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1072(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b5c0").goTo("a1b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1073(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b5c0").goTo("a1b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1074(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b5c0").goTo("a1b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1075(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b5c1").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1076(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b5c1").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1077(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b5c1").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1078(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b5c1").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1079(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b5c1").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1080(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b5c1").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1081(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b6c0").goTo("a1b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1082(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b6c0").goTo("a1b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1083(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b6c0").goTo("a1b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1084(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b6c0").goTo("a1b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1085(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b6c0").goTo("a1b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1086(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b6c0").goTo("a1b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1087(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b6c1").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1088(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b6c1").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1089(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b6c1").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1090(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b6c1").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1091(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b6c1").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1092(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b6c1").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1093(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b7c0").goTo("a1b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1094(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b7c0").goTo("a1b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1095(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b7c0").goTo("a1b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1096(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b7c0").goTo("a1b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1097(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b7c0").goTo("a1b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1098(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b7c0").goTo("a1b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1099(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b7c1").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1100(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b7c1").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1101(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b7c1").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1102(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b7c1").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1103(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b7c1").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1104(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b7c1").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1105(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b8c0").goTo("a1b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1106(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b8c0").goTo("a1b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1107(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b8c0").goTo("a1b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1108(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b8c0").goTo("a1b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1109(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b8c0").goTo("a1b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1110(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b8c0").goTo("a1b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1111(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b8c1").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1112(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b8c1").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1113(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b8c1").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1114(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b8c1").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1115(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b8c1").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1116(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b8c1").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1117(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b9c0").goTo("a1b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1118(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b9c0").goTo("a1b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1119(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b9c0").goTo("a1b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1120(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b9c0").goTo("a1b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1121(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b9c0").goTo("a1b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1122(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b9c0").goTo("a1b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1123(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a1b9c1").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1124(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a1b9c1").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1125(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a1b9c1").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1126(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a1b9c1").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1127(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a1b9c1").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1128(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a1b9c1").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1129(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b0c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1130(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b0c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1131(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b0c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1132(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b0c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1133(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b0c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1134(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b0c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1135(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b0c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1136(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b0c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1137(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b0c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1138(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b0c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1139(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b0c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1140(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b0c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1141(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b1c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1142(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b1c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1143(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b1c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1144(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b1c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1145(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b1c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1146(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b1c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1147(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b1c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1148(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b1c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1149(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b1c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1150(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b1c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1151(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b1c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1152(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b1c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1153(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b2c0").goTo("a2b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1154(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b2c0").goTo("a2b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1155(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b2c0").goTo("a2b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1156(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b2c0").goTo("a2b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1157(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b2c0").goTo("a2b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1158(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b2c0").goTo("a2b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1159(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b2c1").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1160(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b2c1").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1161(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b2c1").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1162(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b2c1").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1163(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b2c1").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1164(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b2c1").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1165(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b3c0").goTo("a2b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1166(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b3c0").goTo("a2b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1167(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b3c0").goTo("a2b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1168(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b3c0").goTo("a2b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1169(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b3c0").goTo("a2b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1170(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b3c0").goTo("a2b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1171(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b3c1").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1172(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b3c1").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1173(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b3c1").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1174(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b3c1").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1175(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b3c1").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1176(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b3c1").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1177(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b4c0").goTo("a2b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1178(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b4c0").goTo("a2b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1179(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b4c0").goTo("a2b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1180(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b4c0").goTo("a2b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1181(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b4c0").goTo("a2b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1182(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b4c0").goTo("a2b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1183(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b4c1").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1184(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b4c1").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1185(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b4c1").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1186(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b4c1").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1187(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b4c1").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1188(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b4c1").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1189(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b5c0").goTo("a2b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1190(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b5c0").goTo("a2b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1191(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b5c0").goTo("a2b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1192(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b5c0").goTo("a2b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1193(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b5c0").goTo("a2b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1194(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b5c0").goTo("a2b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1195(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b5c1").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1196(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b5c1").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1197(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b5c1").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1198(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b5c1").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1199(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b5c1").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1200(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b5c1").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1201(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b6c0").goTo("a2b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1202(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b6c0").goTo("a2b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1203(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b6c0").goTo("a2b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1204(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b6c0").goTo("a2b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1205(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b6c0").goTo("a2b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1206(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b6c0").goTo("a2b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1207(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b6c1").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1208(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b6c1").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1209(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b6c1").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1210(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b6c1").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1211(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b6c1").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1212(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b6c1").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1213(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b7c0").goTo("a2b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1214(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b7c0").goTo("a2b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1215(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b7c0").goTo("a2b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1216(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b7c0").goTo("a2b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1217(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b7c0").goTo("a2b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1218(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b7c0").goTo("a2b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1219(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b7c1").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1220(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b7c1").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1221(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b7c1").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1222(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b7c1").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1223(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b7c1").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1224(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b7c1").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1225(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b8c0").goTo("a2b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1226(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b8c0").goTo("a2b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1227(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b8c0").goTo("a2b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1228(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b8c0").goTo("a2b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1229(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b8c0").goTo("a2b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1230(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b8c0").goTo("a2b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1231(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b8c1").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1232(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b8c1").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1233(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b8c1").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1234(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b8c1").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1235(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b8c1").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1236(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b8c1").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1237(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b9c0").goTo("a2b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1238(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b9c0").goTo("a2b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1239(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b9c0").goTo("a2b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1240(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b9c0").goTo("a2b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1241(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b9c0").goTo("a2b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1242(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b9c0").goTo("a2b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1243(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a2b9c1").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1244(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a2b9c1").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1245(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a2b9c1").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1246(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a2b9c1").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1247(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a2b9c1").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1248(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a2b9c1").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1249(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b0c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1250(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b0c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1251(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b0c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1252(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b0c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1253(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b0c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1254(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b0c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1255(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b0c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1256(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b0c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1257(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b0c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1258(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b0c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1259(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b0c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1260(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b0c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1261(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b1c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1262(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b1c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1263(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b1c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1264(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b1c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1265(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b1c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1266(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b1c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1267(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b1c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1268(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b1c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1269(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b1c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1270(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b1c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1271(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b1c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1272(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b1c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1273(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b2c0").goTo("a3b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1274(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b2c0").goTo("a3b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1275(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b2c0").goTo("a3b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1276(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b2c0").goTo("a3b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1277(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b2c0").goTo("a3b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1278(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b2c0").goTo("a3b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1279(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b2c1").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1280(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b2c1").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1281(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b2c1").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1282(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b2c1").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1283(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b2c1").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1284(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b2c1").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1285(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b3c0").goTo("a3b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1286(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b3c0").goTo("a3b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1287(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b3c0").goTo("a3b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1288(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b3c0").goTo("a3b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1289(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b3c0").goTo("a3b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1290(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b3c0").goTo("a3b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1291(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b3c1").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1292(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b3c1").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1293(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b3c1").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1294(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b3c1").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1295(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b3c1").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1296(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b3c1").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1297(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b4c0").goTo("a3b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1298(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b4c0").goTo("a3b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1299(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b4c0").goTo("a3b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1300(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b4c0").goTo("a3b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1301(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b4c0").goTo("a3b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1302(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b4c0").goTo("a3b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1303(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b4c1").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1304(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b4c1").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1305(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b4c1").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1306(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b4c1").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1307(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b4c1").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1308(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b4c1").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1309(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b5c0").goTo("a3b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1310(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b5c0").goTo("a3b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1311(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b5c0").goTo("a3b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1312(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b5c0").goTo("a3b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1313(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b5c0").goTo("a3b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1314(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b5c0").goTo("a3b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1315(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b5c1").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1316(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b5c1").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1317(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b5c1").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1318(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b5c1").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1319(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b5c1").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1320(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b5c1").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1321(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b6c0").goTo("a3b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1322(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b6c0").goTo("a3b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1323(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b6c0").goTo("a3b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1324(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b6c0").goTo("a3b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1325(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b6c0").goTo("a3b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1326(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b6c0").goTo("a3b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1327(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b6c1").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1328(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b6c1").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1329(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b6c1").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1330(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b6c1").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1331(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b6c1").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1332(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b6c1").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1333(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b7c0").goTo("a3b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1334(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b7c0").goTo("a3b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1335(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b7c0").goTo("a3b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1336(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b7c0").goTo("a3b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1337(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b7c0").goTo("a3b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1338(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b7c0").goTo("a3b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1339(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b7c1").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1340(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b7c1").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1341(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b7c1").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1342(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b7c1").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1343(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b7c1").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1344(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b7c1").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1345(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b8c0").goTo("a3b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1346(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b8c0").goTo("a3b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1347(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b8c0").goTo("a3b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1348(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b8c0").goTo("a3b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1349(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b8c0").goTo("a3b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1350(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b8c0").goTo("a3b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1351(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b8c1").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1352(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b8c1").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1353(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b8c1").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1354(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b8c1").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1355(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b8c1").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1356(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b8c1").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1357(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b9c0").goTo("a3b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1358(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b9c0").goTo("a3b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1359(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b9c0").goTo("a3b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1360(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b9c0").goTo("a3b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1361(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b9c0").goTo("a3b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1362(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b9c0").goTo("a3b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1363(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a3b9c1").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1364(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a3b9c1").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1365(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a3b9c1").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1366(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a3b9c1").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1367(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a3b9c1").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1368(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a3b9c1").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1369(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b0c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1370(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b0c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1371(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b0c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1372(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b0c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1373(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b0c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1374(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b0c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1375(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b0c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1376(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b0c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1377(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b0c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1378(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b0c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1379(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b0c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1380(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b0c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1381(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b1c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1382(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b1c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1383(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b1c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1384(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b1c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1385(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b1c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1386(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b1c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1387(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b1c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1388(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b1c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1389(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b1c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1390(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b1c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1391(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b1c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1392(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b1c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1393(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b2c0").goTo("a4b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1394(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b2c0").goTo("a4b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1395(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b2c0").goTo("a4b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1396(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b2c0").goTo("a4b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1397(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b2c0").goTo("a4b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1398(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b2c0").goTo("a4b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1399(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b2c1").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1400(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b2c1").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1401(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b2c1").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1402(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b2c1").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1403(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b2c1").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1404(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b2c1").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1405(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b3c0").goTo("a4b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1406(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b3c0").goTo("a4b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1407(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b3c0").goTo("a4b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1408(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b3c0").goTo("a4b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1409(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b3c0").goTo("a4b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1410(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b3c0").goTo("a4b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1411(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b3c1").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1412(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b3c1").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1413(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b3c1").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1414(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b3c1").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1415(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b3c1").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1416(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b3c1").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1417(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b4c0").goTo("a4b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1418(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b4c0").goTo("a4b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1419(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b4c0").goTo("a4b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1420(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b4c0").goTo("a4b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1421(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b4c0").goTo("a4b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1422(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b4c0").goTo("a4b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1423(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b4c1").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1424(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b4c1").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1425(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b4c1").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1426(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b4c1").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1427(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b4c1").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1428(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b4c1").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1429(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b5c0").goTo("a4b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1430(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b5c0").goTo("a4b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1431(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b5c0").goTo("a4b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1432(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b5c0").goTo("a4b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1433(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b5c0").goTo("a4b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1434(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b5c0").goTo("a4b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1435(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b5c1").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1436(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b5c1").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1437(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b5c1").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1438(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b5c1").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1439(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b5c1").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1440(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b5c1").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1441(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b6c0").goTo("a4b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1442(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b6c0").goTo("a4b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1443(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b6c0").goTo("a4b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1444(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b6c0").goTo("a4b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1445(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b6c0").goTo("a4b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1446(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b6c0").goTo("a4b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1447(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b6c1").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1448(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b6c1").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1449(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b6c1").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1450(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b6c1").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1451(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b6c1").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1452(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b6c1").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1453(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b7c0").goTo("a4b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1454(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b7c0").goTo("a4b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1455(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b7c0").goTo("a4b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1456(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b7c0").goTo("a4b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1457(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b7c0").goTo("a4b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1458(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b7c0").goTo("a4b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1459(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b7c1").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1460(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b7c1").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1461(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b7c1").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1462(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b7c1").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1463(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b7c1").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1464(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b7c1").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1465(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b8c0").goTo("a4b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1466(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b8c0").goTo("a4b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1467(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b8c0").goTo("a4b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1468(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b8c0").goTo("a4b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1469(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b8c0").goTo("a4b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1470(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b8c0").goTo("a4b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1471(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b8c1").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1472(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b8c1").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1473(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b8c1").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1474(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b8c1").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1475(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b8c1").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1476(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b8c1").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1477(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b9c0").goTo("a4b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1478(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b9c0").goTo("a4b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1479(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b9c0").goTo("a4b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1480(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b9c0").goTo("a4b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1481(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b9c0").goTo("a4b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1482(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b9c0").goTo("a4b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1483(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a4b9c1").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1484(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a4b9c1").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1485(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a4b9c1").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1486(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a4b9c1").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1487(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a4b9c1").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1488(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a4b9c1").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1489(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b0c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1490(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b0c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1491(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b0c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1492(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b0c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1493(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b0c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1494(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b0c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1495(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b0c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1496(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b0c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1497(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b0c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1498(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b0c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1499(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b0c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1500(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b0c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1501(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b1c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1502(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b1c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1503(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b1c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1504(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b1c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1505(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b1c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1506(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b1c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1507(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b1c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1508(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b1c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1509(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b1c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1510(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b1c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1511(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b1c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1512(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b1c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1513(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b2c0").goTo("a5b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1514(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b2c0").goTo("a5b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1515(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b2c0").goTo("a5b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1516(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b2c0").goTo("a5b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1517(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b2c0").goTo("a5b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1518(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b2c0").goTo("a5b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1519(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b2c1").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1520(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b2c1").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1521(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b2c1").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1522(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b2c1").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1523(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b2c1").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1524(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b2c1").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1525(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b3c0").goTo("a5b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1526(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b3c0").goTo("a5b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1527(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b3c0").goTo("a5b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1528(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b3c0").goTo("a5b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1529(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b3c0").goTo("a5b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1530(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b3c0").goTo("a5b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1531(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b3c1").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1532(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b3c1").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1533(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b3c1").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1534(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b3c1").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1535(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b3c1").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1536(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b3c1").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1537(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b4c0").goTo("a5b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1538(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b4c0").goTo("a5b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1539(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b4c0").goTo("a5b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1540(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b4c0").goTo("a5b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1541(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b4c0").goTo("a5b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1542(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b4c0").goTo("a5b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1543(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b4c1").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1544(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b4c1").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1545(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b4c1").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1546(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b4c1").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1547(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b4c1").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1548(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b4c1").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1549(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b5c0").goTo("a5b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1550(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b5c0").goTo("a5b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1551(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b5c0").goTo("a5b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1552(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b5c0").goTo("a5b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1553(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b5c0").goTo("a5b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1554(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b5c0").goTo("a5b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1555(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b5c1").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1556(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b5c1").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1557(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b5c1").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1558(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b5c1").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1559(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b5c1").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1560(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b5c1").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1561(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b6c0").goTo("a5b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1562(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b6c0").goTo("a5b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1563(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b6c0").goTo("a5b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1564(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b6c0").goTo("a5b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1565(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b6c0").goTo("a5b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1566(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b6c0").goTo("a5b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1567(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b6c1").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1568(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b6c1").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1569(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b6c1").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1570(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b6c1").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1571(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b6c1").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1572(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b6c1").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1573(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b7c0").goTo("a5b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1574(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b7c0").goTo("a5b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1575(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b7c0").goTo("a5b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1576(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b7c0").goTo("a5b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1577(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b7c0").goTo("a5b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1578(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b7c0").goTo("a5b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1579(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b7c1").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1580(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b7c1").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1581(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b7c1").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1582(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b7c1").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1583(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b7c1").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1584(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b7c1").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1585(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b8c0").goTo("a5b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1586(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b8c0").goTo("a5b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1587(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b8c0").goTo("a5b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1588(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b8c0").goTo("a5b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1589(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b8c0").goTo("a5b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1590(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b8c0").goTo("a5b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1591(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b8c1").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1592(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b8c1").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1593(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b8c1").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1594(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b8c1").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1595(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b8c1").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1596(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b8c1").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1597(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b9c0").goTo("a5b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1598(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b9c0").goTo("a5b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1599(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b9c0").goTo("a5b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1600(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b9c0").goTo("a5b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1601(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b9c0").goTo("a5b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1602(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b9c0").goTo("a5b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1603(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a5b9c1").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1604(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a5b9c1").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1605(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a5b9c1").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1606(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a5b9c1").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1607(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a5b9c1").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1608(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a5b9c1").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1609(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b0c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1610(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b0c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1611(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b0c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1612(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b0c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1613(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b0c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1614(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b0c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1615(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b0c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1616(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b0c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1617(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b0c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1618(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b0c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1619(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b0c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1620(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b0c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1621(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b1c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1622(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b1c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1623(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b1c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1624(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b1c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1625(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b1c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1626(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b1c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1627(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b1c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1628(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b1c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1629(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b1c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1630(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b1c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1631(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b1c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1632(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b1c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1633(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b2c0").goTo("a6b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1634(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b2c0").goTo("a6b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1635(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b2c0").goTo("a6b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1636(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b2c0").goTo("a6b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1637(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b2c0").goTo("a6b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1638(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b2c0").goTo("a6b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1639(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b2c1").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1640(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b2c1").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1641(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b2c1").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1642(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b2c1").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1643(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b2c1").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1644(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b2c1").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1645(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b3c0").goTo("a6b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1646(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b3c0").goTo("a6b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1647(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b3c0").goTo("a6b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1648(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b3c0").goTo("a6b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1649(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b3c0").goTo("a6b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1650(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b3c0").goTo("a6b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1651(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b3c1").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1652(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b3c1").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1653(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b3c1").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1654(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b3c1").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1655(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b3c1").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1656(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b3c1").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1657(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b4c0").goTo("a6b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1658(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b4c0").goTo("a6b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1659(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b4c0").goTo("a6b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1660(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b4c0").goTo("a6b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1661(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b4c0").goTo("a6b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1662(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b4c0").goTo("a6b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1663(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b4c1").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1664(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b4c1").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1665(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b4c1").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1666(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b4c1").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1667(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b4c1").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1668(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b4c1").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1669(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b5c0").goTo("a6b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1670(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b5c0").goTo("a6b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1671(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b5c0").goTo("a6b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1672(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b5c0").goTo("a6b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1673(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b5c0").goTo("a6b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1674(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b5c0").goTo("a6b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1675(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b5c1").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1676(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b5c1").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1677(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b5c1").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1678(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b5c1").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1679(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b5c1").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1680(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b5c1").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1681(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b6c0").goTo("a6b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1682(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b6c0").goTo("a6b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1683(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b6c0").goTo("a6b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1684(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b6c0").goTo("a6b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1685(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b6c0").goTo("a6b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1686(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b6c0").goTo("a6b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1687(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b6c1").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1688(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b6c1").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1689(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b6c1").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1690(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b6c1").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1691(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b6c1").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1692(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b6c1").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1693(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b7c0").goTo("a6b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1694(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b7c0").goTo("a6b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1695(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b7c0").goTo("a6b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1696(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b7c0").goTo("a6b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1697(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b7c0").goTo("a6b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1698(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b7c0").goTo("a6b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1699(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b7c1").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1700(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b7c1").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1701(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b7c1").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1702(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b7c1").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1703(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b7c1").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1704(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b7c1").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1705(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b8c0").goTo("a6b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1706(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b8c0").goTo("a6b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1707(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b8c0").goTo("a6b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1708(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b8c0").goTo("a6b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1709(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b8c0").goTo("a6b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1710(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b8c0").goTo("a6b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1711(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b8c1").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1712(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b8c1").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1713(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b8c1").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1714(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b8c1").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1715(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b8c1").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1716(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b8c1").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1717(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b9c0").goTo("a6b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1718(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b9c0").goTo("a6b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1719(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b9c0").goTo("a6b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1720(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b9c0").goTo("a6b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1721(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b9c0").goTo("a6b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1722(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b9c0").goTo("a6b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1723(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a6b9c1").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1724(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a6b9c1").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1725(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a6b9c1").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1726(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a6b9c1").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1727(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a6b9c1").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1728(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a6b9c1").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1729(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b0c0").goTo("a7b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1730(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b0c0").goTo("a7b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1731(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b0c0").goTo("a7b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1732(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b0c0").goTo("a7b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1733(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b0c0").goTo("a7b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1734(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b0c0").goTo("a7b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1735(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b0c1").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1736(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b0c1").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1737(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b0c1").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1738(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b0c1").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1739(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b0c1").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1740(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b0c1").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1741(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b1c0").goTo("a7b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1742(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b1c0").goTo("a7b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1743(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b1c0").goTo("a7b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1744(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b1c0").goTo("a7b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1745(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b1c0").goTo("a7b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1746(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b1c0").goTo("a7b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1747(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b1c1").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1748(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b1c1").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1749(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b1c1").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1750(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b1c1").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1751(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b1c1").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1752(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b1c1").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1753(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b2c0").goTo("a7b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1754(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b2c0").goTo("a7b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1755(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b2c0").goTo("a7b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1756(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b2c0").goTo("a7b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1757(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b2c0").goTo("a7b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1758(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b2c0").goTo("a7b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1759(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b2c1").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1760(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b2c1").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1761(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b2c1").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1762(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b2c1").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1763(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b2c1").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1764(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b2c1").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1765(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b3c0").goTo("a7b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1766(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b3c0").goTo("a7b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1767(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b3c0").goTo("a7b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1768(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b3c0").goTo("a7b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1769(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b3c0").goTo("a7b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1770(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b3c0").goTo("a7b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1771(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b3c1").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1772(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b3c1").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1773(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b3c1").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1774(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b3c1").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1775(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b3c1").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1776(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b3c1").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1777(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b4c0").goTo("a7b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1778(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b4c0").goTo("a7b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1779(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b4c0").goTo("a7b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1780(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b4c0").goTo("a7b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1781(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b4c0").goTo("a7b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1782(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b4c0").goTo("a7b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1783(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b4c1").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1784(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b4c1").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1785(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b4c1").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1786(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b4c1").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1787(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b4c1").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1788(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b4c1").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1789(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b5c0").goTo("a7b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1790(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b5c0").goTo("a7b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1791(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b5c0").goTo("a7b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1792(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b5c0").goTo("a7b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1793(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b5c0").goTo("a7b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1794(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b5c0").goTo("a7b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1795(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b5c1").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1796(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b5c1").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1797(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b5c1").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1798(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b5c1").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1799(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b5c1").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1800(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b5c1").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1801(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b6c0").goTo("a7b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1802(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b6c0").goTo("a7b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1803(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b6c0").goTo("a7b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1804(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b6c0").goTo("a7b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1805(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b6c0").goTo("a7b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1806(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b6c0").goTo("a7b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1807(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b6c1").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1808(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b6c1").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1809(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b6c1").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1810(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b6c1").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1811(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b6c1").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1812(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b6c1").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1813(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b7c0").goTo("a7b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1814(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b7c0").goTo("a7b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1815(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b7c0").goTo("a7b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1816(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b7c0").goTo("a7b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1817(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b7c0").goTo("a7b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1818(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b7c0").goTo("a7b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1819(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b7c1").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1820(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b7c1").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1821(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b7c1").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1822(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b7c1").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1823(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b7c1").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1824(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b7c1").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1825(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b8c0").goTo("a7b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1826(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b8c0").goTo("a7b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1827(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b8c0").goTo("a7b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1828(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b8c0").goTo("a7b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1829(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b8c0").goTo("a7b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1830(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b8c0").goTo("a7b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1831(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b8c1").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1832(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b8c1").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1833(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b8c1").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1834(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b8c1").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1835(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b8c1").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1836(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b8c1").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1837(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b9c0").goTo("a7b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1838(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b9c0").goTo("a7b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1839(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b9c0").goTo("a7b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1840(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b9c0").goTo("a7b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1841(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b9c0").goTo("a7b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1842(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b9c0").goTo("a7b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1843(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a7b9c1").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1844(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a7b9c1").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1845(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a7b9c1").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1846(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a7b9c1").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1847(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a7b9c1").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1848(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a7b9c1").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1849(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b0c0").goTo("a8b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1850(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b0c0").goTo("a8b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1851(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b0c0").goTo("a8b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1852(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b0c0").goTo("a8b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1853(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b0c0").goTo("a8b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1854(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b0c0").goTo("a8b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1855(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b0c1").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1856(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b0c1").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1857(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b0c1").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1858(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b0c1").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1859(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b0c1").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1860(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b0c1").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1861(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b1c0").goTo("a8b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1862(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b1c0").goTo("a8b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1863(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b1c0").goTo("a8b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1864(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b1c0").goTo("a8b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1865(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b1c0").goTo("a8b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1866(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b1c0").goTo("a8b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1867(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b1c1").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1868(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b1c1").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1869(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b1c1").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1870(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b1c1").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1871(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b1c1").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1872(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b1c1").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1873(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b2c0").goTo("a8b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1874(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b2c0").goTo("a8b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1875(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b2c0").goTo("a8b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1876(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b2c0").goTo("a8b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1877(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b2c0").goTo("a8b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1878(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b2c0").goTo("a8b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1879(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b2c1").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1880(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b2c1").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1881(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b2c1").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1882(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b2c1").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1883(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b2c1").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1884(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b2c1").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1885(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b3c0").goTo("a8b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1886(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b3c0").goTo("a8b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1887(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b3c0").goTo("a8b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1888(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b3c0").goTo("a8b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1889(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b3c0").goTo("a8b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1890(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b3c0").goTo("a8b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1891(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b3c1").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1892(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b3c1").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1893(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b3c1").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1894(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b3c1").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1895(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b3c1").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1896(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b3c1").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1897(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b4c0").goTo("a8b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1898(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b4c0").goTo("a8b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1899(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b4c0").goTo("a8b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1900(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b4c0").goTo("a8b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1901(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b4c0").goTo("a8b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1902(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b4c0").goTo("a8b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1903(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b4c1").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1904(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b4c1").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1905(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b4c1").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1906(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b4c1").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1907(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b4c1").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1908(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b4c1").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1909(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b5c0").goTo("a8b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1910(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b5c0").goTo("a8b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1911(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b5c0").goTo("a8b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1912(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b5c0").goTo("a8b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1913(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b5c0").goTo("a8b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1914(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b5c0").goTo("a8b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1915(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b5c1").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1916(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b5c1").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1917(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b5c1").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1918(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b5c1").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1919(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b5c1").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1920(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b5c1").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1921(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b6c0").goTo("a8b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1922(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b6c0").goTo("a8b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1923(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b6c0").goTo("a8b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1924(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b6c0").goTo("a8b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1925(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b6c0").goTo("a8b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1926(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b6c0").goTo("a8b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1927(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b6c1").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1928(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b6c1").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1929(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b6c1").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1930(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b6c1").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1931(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b6c1").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1932(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b6c1").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1933(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b7c0").goTo("a8b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1934(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b7c0").goTo("a8b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1935(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b7c0").goTo("a8b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1936(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b7c0").goTo("a8b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1937(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b7c0").goTo("a8b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1938(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b7c0").goTo("a8b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1939(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b7c1").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1940(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b7c1").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1941(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b7c1").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1942(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b7c1").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1943(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b7c1").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1944(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b7c1").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1945(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b8c0").goTo("a8b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1946(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b8c0").goTo("a8b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1947(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b8c0").goTo("a8b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1948(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b8c0").goTo("a8b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1949(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b8c0").goTo("a8b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1950(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b8c0").goTo("a8b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1951(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b8c1").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1952(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b8c1").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1953(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b8c1").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1954(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b8c1").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1955(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b8c1").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1956(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b8c1").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1957(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b9c0").goTo("a8b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1958(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b9c0").goTo("a8b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1959(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b9c0").goTo("a8b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1960(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b9c0").goTo("a8b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1961(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b9c0").goTo("a8b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1962(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b9c0").goTo("a8b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1963(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a8b9c1").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1964(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a8b9c1").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1965(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a8b9c1").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1966(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a8b9c1").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1967(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a8b9c1").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1968(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a8b9c1").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1969(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b0c0").goTo("a9b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1970(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b0c0").goTo("a9b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1971(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b0c0").goTo("a9b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1972(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b0c0").goTo("a9b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1973(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b0c0").goTo("a9b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1974(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b0c0").goTo("a9b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1975(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b0c1").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1976(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b0c1").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1977(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b0c1").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1978(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b0c1").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1979(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b0c1").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1980(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b0c1").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1981(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b1c0").goTo("a9b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1982(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b1c0").goTo("a9b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1983(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b1c0").goTo("a9b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1984(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b1c0").goTo("a9b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1985(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b1c0").goTo("a9b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1986(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b1c0").goTo("a9b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1987(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b1c1").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1988(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b1c1").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1989(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b1c1").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1990(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b1c1").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1991(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b1c1").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1992(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b1c1").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1993(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b2c0").goTo("a9b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1994(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b2c0").goTo("a9b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1995(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b2c0").goTo("a9b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1996(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b2c0").goTo("a9b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1997(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b2c0").goTo("a9b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1998(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b2c0").goTo("a9b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition1999(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b2c1").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2000(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b2c1").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2001(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b2c1").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2002(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b2c1").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2003(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b2c1").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2004(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b2c1").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2005(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b3c0").goTo("a9b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2006(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b3c0").goTo("a9b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2007(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b3c0").goTo("a9b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2008(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b3c0").goTo("a9b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2009(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b3c0").goTo("a9b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2010(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b3c0").goTo("a9b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2011(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b3c1").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2012(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b3c1").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2013(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b3c1").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2014(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b3c1").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2015(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b3c1").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2016(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b3c1").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2017(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b4c0").goTo("a9b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2018(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b4c0").goTo("a9b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2019(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b4c0").goTo("a9b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2020(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b4c0").goTo("a9b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2021(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b4c0").goTo("a9b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2022(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b4c0").goTo("a9b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2023(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b4c1").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2024(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b4c1").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2025(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b4c1").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2026(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b4c1").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2027(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b4c1").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2028(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b4c1").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2029(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b5c0").goTo("a9b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2030(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b5c0").goTo("a9b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2031(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b5c0").goTo("a9b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2032(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b5c0").goTo("a9b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2033(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b5c0").goTo("a9b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2034(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b5c0").goTo("a9b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2035(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b5c1").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2036(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b5c1").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2037(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b5c1").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2038(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b5c1").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2039(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b5c1").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2040(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b5c1").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2041(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b6c0").goTo("a9b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2042(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b6c0").goTo("a9b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2043(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b6c0").goTo("a9b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2044(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b6c0").goTo("a9b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2045(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b6c0").goTo("a9b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2046(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b6c0").goTo("a9b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2047(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b6c1").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2048(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b6c1").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2049(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b6c1").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2050(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b6c1").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2051(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b6c1").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2052(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b6c1").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2053(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b7c0").goTo("a9b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2054(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b7c0").goTo("a9b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2055(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b7c0").goTo("a9b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2056(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b7c0").goTo("a9b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2057(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b7c0").goTo("a9b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2058(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b7c0").goTo("a9b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2059(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b7c1").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2060(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b7c1").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2061(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b7c1").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2062(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b7c1").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2063(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b7c1").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2064(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b7c1").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2065(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b8c0").goTo("a9b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2066(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b8c0").goTo("a9b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2067(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b8c0").goTo("a9b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2068(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b8c0").goTo("a9b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2069(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b8c0").goTo("a9b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2070(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b8c0").goTo("a9b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2071(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b8c1").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2072(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b8c1").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2073(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b8c1").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2074(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b8c1").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2075(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b8c1").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2076(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b8c1").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2077(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b9c0").goTo("a9b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2078(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b9c0").goTo("a9b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2079(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b9c0").goTo("a9b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2080(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b9c0").goTo("a9b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2081(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b9c0").goTo("a9b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2082(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b9c0").goTo("a9b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2083(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a9b9c1").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2084(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a9b9c1").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2085(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a9b9c1").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2086(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a9b9c1").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2087(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a9b9c1").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2088(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a9b9c1").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2089(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b0c0").goTo("a10b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2090(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b0c0").goTo("a10b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2091(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b0c0").goTo("a10b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2092(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b0c0").goTo("a10b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2093(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b0c0").goTo("a10b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2094(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b0c0").goTo("a10b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2095(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b0c1").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2096(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b0c1").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2097(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b0c1").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2098(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b0c1").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2099(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b0c1").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2100(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b0c1").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2101(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b1c0").goTo("a10b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2102(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b1c0").goTo("a10b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2103(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b1c0").goTo("a10b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2104(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b1c0").goTo("a10b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2105(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b1c0").goTo("a10b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2106(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b1c0").goTo("a10b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2107(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b1c1").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2108(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b1c1").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2109(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b1c1").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2110(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b1c1").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2111(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b1c1").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2112(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b1c1").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2113(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b2c0").goTo("a10b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2114(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b2c0").goTo("a10b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2115(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b2c0").goTo("a10b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2116(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b2c0").goTo("a10b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2117(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b2c0").goTo("a10b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2118(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b2c0").goTo("a10b3c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2119(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b2c1").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2120(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b2c1").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2121(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b2c1").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2122(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b2c1").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2123(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b2c1").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2124(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b2c1").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2125(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b3c0").goTo("a10b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2126(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b3c0").goTo("a10b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2127(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b3c0").goTo("a10b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2128(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b3c0").goTo("a10b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2129(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b3c0").goTo("a10b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2130(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b3c0").goTo("a10b4c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2131(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b3c1").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2132(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b3c1").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2133(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b3c1").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2134(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b3c1").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2135(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b3c1").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2136(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b3c1").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2137(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b4c0").goTo("a10b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2138(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b4c0").goTo("a10b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2139(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b4c0").goTo("a10b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2140(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b4c0").goTo("a10b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2141(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b4c0").goTo("a10b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2142(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b4c0").goTo("a10b5c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2143(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b4c1").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2144(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b4c1").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2145(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b4c1").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2146(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b4c1").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2147(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b4c1").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2148(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b4c1").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2149(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b5c0").goTo("a10b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2150(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b5c0").goTo("a10b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2151(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b5c0").goTo("a10b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2152(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b5c0").goTo("a10b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2153(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b5c0").goTo("a10b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2154(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b5c0").goTo("a10b6c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2155(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b5c1").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2156(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b5c1").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2157(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b5c1").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2158(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b5c1").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2159(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b5c1").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2160(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b5c1").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2161(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b6c0").goTo("a10b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2162(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b6c0").goTo("a10b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2163(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b6c0").goTo("a10b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2164(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b6c0").goTo("a10b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2165(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b6c0").goTo("a10b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2166(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b6c0").goTo("a10b7c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2167(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b6c1").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2168(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b6c1").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2169(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b6c1").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2170(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b6c1").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2171(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b6c1").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2172(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b6c1").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2173(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b7c0").goTo("a10b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2174(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b7c0").goTo("a10b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2175(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b7c0").goTo("a10b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2176(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b7c0").goTo("a10b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2177(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b7c0").goTo("a10b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2178(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b7c0").goTo("a10b8c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2179(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b7c1").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2180(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b7c1").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2181(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b7c1").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2182(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b7c1").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2183(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b7c1").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2184(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b7c1").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2185(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b8c0").goTo("a10b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2186(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b8c0").goTo("a10b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2187(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b8c0").goTo("a10b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2188(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b8c0").goTo("a10b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2189(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b8c0").goTo("a10b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2190(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b8c0").goTo("a10b9c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2191(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b8c1").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2192(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b8c1").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2193(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b8c1").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2194(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b8c1").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2195(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b8c1").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2196(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b8c1").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2197(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b9c0").goTo("a10b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2198(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b9c0").goTo("a10b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2199(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b9c0").goTo("a10b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2200(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b9c0").goTo("a10b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2201(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b9c0").goTo("a10b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2202(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b9c0").goTo("a10b10c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2203(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219605)).from("a10b9c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2204(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219607)).from("a10b9c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2205(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219612)).from("a10b9c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2206(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219613)).from("a10b9c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2207(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219614)).from("a10b9c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2208(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219616)).from("a10b9c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2209(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b0c0").goTo("a0b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2210(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b1c0").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2211(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b2c0").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2212(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b3c0").goTo("a0b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2213(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b4c0").goTo("a0b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2214(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b5c0").goTo("a0b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2215(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b6c0").goTo("a0b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2216(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b7c0").goTo("a0b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2217(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b8c0").goTo("a0b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2218(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b9c0").goTo("a0b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2219(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a0b10c0").goTo("a0b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2220(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b0c0").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2221(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b1c0").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2222(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b2c0").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2223(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b3c0").goTo("a1b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2224(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b4c0").goTo("a1b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2225(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b5c0").goTo("a1b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2226(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b6c0").goTo("a1b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2227(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b7c0").goTo("a1b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2228(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b8c0").goTo("a1b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2229(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b9c0").goTo("a1b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2230(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a1b10c0").goTo("a1b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2231(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b0c0").goTo("a2b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2232(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b1c0").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2233(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b2c0").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2234(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b3c0").goTo("a2b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2235(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b4c0").goTo("a2b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2236(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b5c0").goTo("a2b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2237(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b6c0").goTo("a2b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2238(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b7c0").goTo("a2b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2239(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b8c0").goTo("a2b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2240(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b9c0").goTo("a2b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2241(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a2b10c0").goTo("a2b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2242(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b0c0").goTo("a3b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2243(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b1c0").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2244(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b2c0").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2245(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b3c0").goTo("a3b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2246(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b4c0").goTo("a3b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2247(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b5c0").goTo("a3b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2248(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b6c0").goTo("a3b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2249(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b7c0").goTo("a3b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2250(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b8c0").goTo("a3b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2251(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b9c0").goTo("a3b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2252(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a3b10c0").goTo("a3b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2253(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b0c0").goTo("a4b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2254(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b1c0").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2255(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b2c0").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2256(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b3c0").goTo("a4b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2257(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b4c0").goTo("a4b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2258(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b5c0").goTo("a4b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2259(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b6c0").goTo("a4b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2260(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b7c0").goTo("a4b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2261(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b8c0").goTo("a4b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2262(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b9c0").goTo("a4b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2263(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a4b10c0").goTo("a4b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2264(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b0c0").goTo("a5b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2265(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b1c0").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2266(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b2c0").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2267(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b3c0").goTo("a5b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2268(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b4c0").goTo("a5b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2269(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b5c0").goTo("a5b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2270(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b6c0").goTo("a5b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2271(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b7c0").goTo("a5b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2272(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b8c0").goTo("a5b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2273(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b9c0").goTo("a5b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2274(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a5b10c0").goTo("a5b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2275(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b0c0").goTo("a6b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2276(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b1c0").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2277(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b2c0").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2278(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b3c0").goTo("a6b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2279(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b4c0").goTo("a6b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2280(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b5c0").goTo("a6b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2281(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b6c0").goTo("a6b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2282(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b7c0").goTo("a6b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2283(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b8c0").goTo("a6b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2284(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b9c0").goTo("a6b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2285(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a6b10c0").goTo("a6b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2286(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b0c0").goTo("a7b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2287(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b1c0").goTo("a7b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2288(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b2c0").goTo("a7b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2289(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b3c0").goTo("a7b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2290(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b4c0").goTo("a7b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2291(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b5c0").goTo("a7b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2292(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b6c0").goTo("a7b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2293(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b7c0").goTo("a7b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2294(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b8c0").goTo("a7b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2295(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b9c0").goTo("a7b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2296(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a7b10c0").goTo("a7b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2297(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b0c0").goTo("a8b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2298(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b1c0").goTo("a8b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2299(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b2c0").goTo("a8b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2300(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b3c0").goTo("a8b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2301(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b4c0").goTo("a8b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2302(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b5c0").goTo("a8b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2303(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b6c0").goTo("a8b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2304(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b7c0").goTo("a8b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2305(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b8c0").goTo("a8b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2306(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b9c0").goTo("a8b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2307(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a8b10c0").goTo("a8b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2308(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b0c0").goTo("a9b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2309(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b1c0").goTo("a9b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2310(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b2c0").goTo("a9b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2311(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b3c0").goTo("a9b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2312(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b4c0").goTo("a9b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2313(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b5c0").goTo("a9b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2314(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b6c0").goTo("a9b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2315(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b7c0").goTo("a9b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2316(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b8c0").goTo("a9b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2317(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b9c0").goTo("a9b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2318(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a9b10c0").goTo("a9b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2319(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b0c0").goTo("a10b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2320(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b1c0").goTo("a10b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2321(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b2c0").goTo("a10b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2322(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b3c0").goTo("a10b3c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2323(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b4c0").goTo("a10b4c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2324(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b5c0").goTo("a10b5c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2325(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b6c0").goTo("a10b6c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2326(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b7c0").goTo("a10b7c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2327(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b8c0").goTo("a10b8c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2328(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b9c0").goTo("a10b9c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2329(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219610)).from("a10b10c0").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2330(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 31, 0)).from("a10b10c1").goTo("a10b10c1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition2331(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 1009, 0)).from("a10b10c1").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition2332(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition2333(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition2334(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2335(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2336(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2337(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2338(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2339(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2340(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2341(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2342(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2343(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2344(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2345(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2346(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2347(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2348(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition2349(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799384, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3453546L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 306L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}

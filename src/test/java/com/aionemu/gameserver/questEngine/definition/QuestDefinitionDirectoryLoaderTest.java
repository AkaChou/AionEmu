package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDefinitionDirectoryLoaderTest {
	@Test
	void packagedProductionQuestsCompileFromDirectory() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		// Every quests/<id>.xml compiles and duplicate owners fail in ImmutableQuestCatalog.
		assertFalse(catalog.executables().isEmpty());
		assertTrue(catalog.executables().stream().map(CompiledQuestDefinition::id).allMatch(id -> id > 0));
		assertEquals(QuestCatalogEntryMode.METADATA_ONLY, catalog.findEntry(50032).orElseThrow().mode());
		assertTrue(catalog.findExecutable(50032).isEmpty());
	}

	@Test
	void executableQuestsHaveNoReachableDeadEndNodes() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		java.util.List<String> deadEnds = new java.util.ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			int qid = compiled.id();
			if (qid == 89999) {
				continue; // 89999 是纯交互发道具活动任务，本身不推进任务状态
			}
			QuestDefinition def = compiled.definition();
			java.util.Map<String, java.util.Set<String>> adj = new java.util.HashMap<>();
			java.util.Map<String, com.aionemu.gameserver.questEngine.model.QuestStatus> nodeStatus = new java.util.HashMap<>();

			for (QuestNode n : def.nodes()) {
				nodeStatus.put(n.label(), n.projection().status());
				adj.put(n.label(), new java.util.HashSet<>());
			}

			for (QuestTransition t : def.transitions()) {
				String src = t.sourceNode();
				String tgt = t.targetNode();
				if (src != null && tgt != null) {
					adj.computeIfAbsent(src, k -> new java.util.HashSet<>()).add(tgt);
				}
			}

			java.util.Queue<String> q = new java.util.ArrayDeque<>();
			java.util.Set<String> visited = new java.util.HashSet<>();
			if (nodeStatus.containsKey("unaccepted")) {
				q.add("unaccepted");
				visited.add("unaccepted");
			}

			while (!q.isEmpty()) {
				String cur = q.poll();
				for (String nxt : adj.getOrDefault(cur, java.util.Set.of())) {
					if (visited.add(nxt)) {
						q.add(nxt);
					}
				}
			}

			for (String n : visited) {
				com.aionemu.gameserver.questEngine.model.QuestStatus st = nodeStatus.get(n);
				if (st == com.aionemu.gameserver.questEngine.model.QuestStatus.COMPLETE) {
					continue;
				}
				java.util.Set<String> out = adj.getOrDefault(n, java.util.Set.of());
				boolean hasNext = out.stream().anyMatch(next -> !next.equals(n));
				if (!hasNext) {
					boolean completes = def.transitions().stream()
						.filter(t -> n.equals(t.sourceNode()))
						.anyMatch(t -> t.actions().stream().anyMatch(com.aionemu.gameserver.questEngine.definition.QuestAction.CompleteQuest.class::isInstance));
					if (!completes) {
						deadEnds.add(qid + ":" + n + "(status=" + st + ")");
					}
				}
			}
		}

		assertTrue(deadEnds.isEmpty(), "reachable dead end nodes found: " + deadEnds);
	}
}

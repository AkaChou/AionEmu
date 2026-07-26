package com.aionemu.gameserver.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

class RetailInstanceOwnershipTest {

	private static final Path HANDLERS = Path.of("src/main/java/com/aionemu/gameserver/instance/handlers/scripts");
	private static final Path COVERAGE = Path.of("src/main/resources/aion/definitions/compact/instance/coverage.xml");
	private static final Path ENTRY_GUIDE = Path.of("docs/RETAIL_INSTANCE_GM_ACCEPTANCE_GUIDE.md");
	private static final int DISABLED_WORLD = 300260000;
	private static final List<String> OWNERSHIP_DIMENSIONS = List.of(
		"entry", "spawn", "ai", "path", "door", "stage", "score", "reward", "exit", "recovery");
	private static final Set<String> OWNERSHIP_OWNERS = Set.of(
		"EVENT_AI", "HANDLER", "HOUSING_SERVICE", "INSTANCE_EXIT_DATA", "INSTANCE_SYSTEM", "MATCHMAKER",
		"NON_PRODUCTION", "NOT_APPLICABLE", "PORTAL_REJECTED", "RETAIL_DATA", "RETAIL_DROPS", "RETAIL_PATTERN",
		"RETAIL_PORTAL", "RUNTIME_PATHING", "QUEST_STATE", "RECOVERY_REJECTED", "SCRIPT_AI", "SCRIPT_QUEST", "SETTLEMENT_SERVICE",
		"STATELESS", "TOURNAMENT_SERVICE");
	private static final int DECLARED_OWNERSHIP_WORLDS = 139;

	@Test
	void coverageMatchesRegisteredInstanceHandlers() throws Exception {
		Map<Integer, Coverage> coverage = loadCoverage();
		Map<Integer, List<Path>> handlers = loadHandlers();
		Set<Integer> expectedHandlers = new HashSet<>();
		coverage.forEach((worldId, entry) -> {
			if (entry.behavior().equals("HANDLER")) {
				expectedHandlers.add(worldId);
			}
		});

		Set<Integer> actualHandlers = new HashSet<>(handlers.keySet());
		actualHandlers.removeIf(worldId -> {
			Coverage entry = coverage.get(worldId);
			return entry != null && !"HANDLER".equals(entry.behavior());
		});
		assertEquals(expectedHandlers, actualHandlers, "HANDLER coverage must match @InstanceID registrations");

		List<Integer> handlersNotCovered = handlers.keySet().stream()
			.filter(worldId -> !coverage.containsKey(worldId))
			.sorted()
			.toList();
		assertTrue(handlersNotCovered.isEmpty(), "Registered handlers are missing coverage: " + handlersNotCovered);

		Set<String> handlerBehaviors = Set.of("HANDLER", "EVENT", "TOURNAMENT", "EXCLUDED_NON_PRODUCTION");
		List<Integer> invalidOwnership = handlers.keySet().stream()
			.filter(worldId -> !handlerBehaviors.contains(coverage.get(worldId).behavior()))
			.sorted()
			.toList();
		assertTrue(invalidOwnership.isEmpty(), "Registered handlers have incompatible coverage: " + invalidOwnership);

		List<String> duplicates = handlers.entrySet().stream()
			.filter(entry -> entry.getValue().size() > 1)
			.map(entry -> entry.getKey() + "=" + entry.getValue())
			.toList();
		assertTrue(duplicates.isEmpty(), "Multiple handlers own the same world: " + duplicates);
	}

	@Test
	void coverageDefinesThe139MapProductionScopeWithConcreteOwners() throws Exception {
		Map<Integer, Coverage> coverage = loadCoverage();
		Coverage disabled = coverage.get(DISABLED_WORLD);
		assertEquals("EXCLUDED_NON_PRODUCTION", disabled.behavior());
		assertTrue(disabled.source().contains("world_maps.xml disabled"));
		assertEquals(139, coverage.size() - 1, "Production scope excludes only the disabled Elementis Forest world");

		List<Integer> bareHandlerOwners = coverage.entrySet().stream()
			.filter(entry -> entry.getKey() != DISABLED_WORLD)
			.filter(entry -> entry.getValue().source().isBlank()
				|| entry.getValue().source().matches("(?:.*/)?src/main/java/com/aionemu/gameserver/instance/handlers/scripts/.+\\.java"))
			.map(Map.Entry::getKey)
			.sorted()
			.toList();
		assertTrue(bareHandlerOwners.isEmpty(), "Each production map must describe its actual behavior owners: " + bareHandlerOwners);
	}

	@Test
	void coverageDeclaresOrderedTenDimensionOwners() throws Exception {
		Map<Integer, Coverage> coverage = loadCoverage();
		List<String> invalid = new ArrayList<>();
		long declared = coverage.entrySet().stream().filter(entry -> {
			String ownership = entry.getValue().dimensionOwners();
			if (ownership.isBlank()) {
				return false;
			}
			String[] assignments = ownership.split(",", -1);
			if (assignments.length != OWNERSHIP_DIMENSIONS.size()) {
				invalid.add(entry.getKey() + "=" + ownership);
				return true;
			}
			for (int i = 0; i < assignments.length; i++) {
				String[] parts = assignments[i].split(":", -1);
				if (parts.length != 2 || !parts[0].equals(OWNERSHIP_DIMENSIONS.get(i))
					|| !OWNERSHIP_OWNERS.contains(parts[1])) {
					invalid.add(entry.getKey() + "=" + assignments[i]);
				}
			}
			return true;
		}).count();
		assertTrue(invalid.isEmpty(), "Invalid ten-dimension ownership: " + invalid);
		assertEquals(DECLARED_OWNERSHIP_WORLDS, declared, "Ten-dimension ownership progress must be monotonic");
	}

	@Test
	void entryGuideCoversTheSame139MapsWithoutInteriorShortcuts() throws Exception {
		Map<Integer, String> entrySources = new HashMap<>();
		int expectedSequence = 1;
		for (String line : Files.readAllLines(ENTRY_GUIDE)) {
			if (!line.matches("^\\|\\s*\\d+\\s*\\|\\s*\\d{9}\\s*\\|.*")) {
				continue;
			}
			String[] columns = line.split("\\|", -1);
			assertEquals(expectedSequence++, Integer.parseInt(columns[1].trim()), "Entry guide sequence");
			int worldId = Integer.parseInt(columns[2].trim());
			String command = columns[8].trim();
			String npc = columns[9].trim();
			String source = columns[10].trim();
			assertNull(entrySources.put(worldId, source), "Duplicate entry guide world: " + worldId);
			switch (source) {
				case "外部入口 NPC 静态出生" -> {
					assertTrue(command.contains("//moveto "), Integer.toString(worldId));
					assertFalse(npc.equals("-"), Integer.toString(worldId));
				}
				case "portal 入口定义存在，外部出生缺失" -> {
					assertTrue(command.contains("坐标缺失"), Integer.toString(worldId));
					assertFalse(npc.equals("-"), Integer.toString(worldId));
				}
				case "无 instance=true 实体入口定义" -> {
					assertFalse(command.contains("//moveto "), Integer.toString(worldId));
					assertEquals("-", npc, Integer.toString(worldId));
				}
				default -> throw new IllegalArgumentException("Unknown entry source for " + worldId + ": " + source);
			}
		}

		Set<Integer> scope = new HashSet<>(loadCoverage().keySet());
		scope.remove(DISABLED_WORLD);
		assertEquals(scope, entrySources.keySet());
		assertEquals(75, entrySources.values().stream().filter("外部入口 NPC 静态出生"::equals).count());
		assertEquals(4, entrySources.values().stream().filter("portal 入口定义存在，外部出生缺失"::equals).count());
		assertEquals(60, entrySources.values().stream().filter("无 instance=true 实体入口定义"::equals).count());
	}

	@Test
	void logoutCallbacksNeverRemoveItemsOrForceInstanceExit() throws Exception {
		List<Path> sources;
		try (var paths = Files.walk(HANDLERS)) {
			sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
		}
		List<String> violations = new ArrayList<>();
		var compiler = ToolProvider.getSystemJavaCompiler();
		try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
			JavacTask task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null,
				files.getJavaFileObjectsFromPaths(sources));
			for (var unit : task.parse()) {
				Map<String, List<MethodTree>> methods = new HashMap<>();
				new TreeScanner<Void, Void>() {
					@Override
					public Void visitMethod(MethodTree method, Void unused) {
						methods.computeIfAbsent(method.getName().toString(), key -> new ArrayList<>()).add(method);
						return super.visitMethod(method, unused);
					}
				}.scan(unit, null);

				var pending = new ArrayDeque<>(methods.getOrDefault("onPlayerLogOut", List.of()));
				Set<MethodTree> visited = Collections.newSetFromMap(new IdentityHashMap<>());
				while (!pending.isEmpty()) {
					MethodTree method = pending.removeFirst();
					if (!visited.add(method)) {
						continue;
					}
					new TreeScanner<Void, Void>() {
						@Override
						public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
							String call = invocation.getMethodSelect().toString();
							String name = call.substring(call.lastIndexOf('.') + 1);
							if (name.equals("decreaseByItemId") || name.equals("moveToInstanceExit")) {
								violations.add(unit.getSourceFile().getName() + ": " + method.getName() + " -> " + name);
							}
							pending.addAll(methods.getOrDefault(name, List.of()));
							return super.visitMethodInvocation(invocation, unused);
						}
					}.scan(method.getBody(), null);
				}
			}
		}
		assertTrue(violations.isEmpty(), "Logout lifecycle violations: " + violations);
	}

	private static Map<Integer, Coverage> loadCoverage() throws Exception {
		Map<Integer, Coverage> coverage = new HashMap<>();
		var worlds = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(COVERAGE.toFile())
			.getElementsByTagName("world");
		for (int i = 0; i < worlds.getLength(); i++) {
			var world = worlds.item(i).getAttributes();
			int worldId = Integer.parseInt(world.getNamedItem("id").getNodeValue());
				Coverage entry = new Coverage(world.getNamedItem("behavior").getNodeValue(),
					world.getNamedItem("behavior_source").getNodeValue(), attribute(world, "dimension_owners"));
			if (coverage.put(worldId, entry) != null) {
				throw new IllegalArgumentException("Duplicate coverage world: " + worldId);
			}
		}
		return coverage;
	}

	private static String attribute(org.w3c.dom.NamedNodeMap attributes, String name) {
		var value = attributes.getNamedItem(name);
		return value == null ? "" : value.getNodeValue();
	}

	private record Coverage(String behavior, String source, String dimensionOwners) {
	}

	private static Map<Integer, List<Path>> loadHandlers() throws Exception {
		List<Path> sources;
		try (var paths = Files.walk(HANDLERS)) {
			sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
		}
		Map<Integer, List<Path>> handlers = new HashMap<>();
		var compiler = ToolProvider.getSystemJavaCompiler();
		try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
			JavacTask task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null,
				files.getJavaFileObjectsFromPaths(sources));
			for (var unit : task.parse()) {
				Path source = Path.of(unit.getSourceFile().toUri());
				new TreeScanner<Void, Void>() {
					@Override
					public Void visitAnnotation(AnnotationTree annotation, Void unused) {
						if (annotation.getAnnotationType().toString().endsWith("InstanceID")) {
							new TreeScanner<Void, Void>() {
								@Override
								public Void visitLiteral(LiteralTree literal, Void ignored) {
									if (literal.getValue() instanceof Integer worldId) {
										handlers.computeIfAbsent(worldId, key -> new ArrayList<>()).add(source);
									}
									return super.visitLiteral(literal, ignored);
								}
							}.scan(annotation.getArguments(), null);
						}
						return super.visitAnnotation(annotation, unused);
					}
				}.scan(unit, null);
			}
		}
		return handlers;
	}
}

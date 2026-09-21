package com.aionemu.gameserver.ai2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.aionemu.commons.scripting.CompiledScriptLoader;

class AI2ProductionRegistrationContractTest {

	private static final Pattern AI_ATTRIBUTE_PATTERN = Pattern.compile("\\bai\\s*=\\s*\"([^\"]+)\"");
	private static final Set<String> ENGINE_SELECTED_AI_NAMES = Set.of(
		"fearful_beast", "siege_teleporter", "dummy", "retail_pattern", "retail_direct_portal");

	@Test
	void productionAiRegistrationsCoverNpcReferencesAndNoArgumentConstruction() throws Exception {
		AI2Engine engine = new AI2Engine();
		int registeredAiCount = 0;
		for (Class<?> loadedClass : CompiledScriptLoader.load("com.aionemu.gameserver.ai")) {
			if (AbstractAI.class.isAssignableFrom(loadedClass)) {
				engine.registerAI(loadedClass.asSubclass(AbstractAI.class));
				registeredAiCount++;
			}
		}

		Set<String> referencedAiNames = readNpcTemplateAiNames();
		referencedAiNames.addAll(ENGINE_SELECTED_AI_NAMES);
		engine.validateScripts(referencedAiNames);
		assertTrue(registeredAiCount > 0);
		assertFalse(referencedAiNames.isEmpty());
	}

	private Set<String> readNpcTemplateAiNames() throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Set<String> aiNames = new HashSet<>();
		for (Resource resource : resolver.getResources("classpath*:aion/data/static_data/npcs/npc_template_*.xml")) {
			String xml;
			try (var input = resource.getInputStream()) {
				xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
			}
			var matcher = AI_ATTRIBUTE_PATTERN.matcher(xml);
			while (matcher.find()) {
				aiNames.add(matcher.group(1));
			}
		}
		return aiNames;
	}
}

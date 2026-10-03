package com.aionemu.gameserver.ai2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

class AI2EngineRegistrationValidationTest {

	@Test
	void rejectsDifferentClassesRegisteringTheSameAiName() {
		AI2Engine engine = new AI2Engine();
		assertDoesNotThrow(() -> engine.registerAI(FirstDuplicateAi.class));
		assertDoesNotThrow(() -> engine.registerAI(FirstDuplicateAi.class));

		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> engine.registerAI(SecondDuplicateAi.class));
		assertEquals("log.ai_engine.duplicate_name", exception.getMessage());
	}

	@Test
	void rejectsNpcReferenceWithoutRegisteredAi() {
		AI2Engine engine = new AI2Engine();
		engine.registerAI(ValidAi.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> engine.validateScripts(Set.of(ValidAi.class.getAnnotation(AIName.class).value(), "missing-ai")));
		assertEquals("log.ai_engine.missing_npc_ai", exception.getMessage());
	}

	@Test
	void rejectsRegisteredAiWithoutNoArgumentConstructor() {
		AI2Engine engine = new AI2Engine();
		engine.registerAI(MissingNoArgumentConstructorAi.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> engine.validateScripts(Set.of()));
		assertEquals("log.ai_engine.constructor_failed", exception.getMessage());
	}

	@Test
	void registersMultipleCommaSeparatedAiNamesForSameClass() {
		AI2Engine engine = new AI2Engine();
		assertDoesNotThrow(() -> engine.registerAI(MultiAliasAi.class));
		assertDoesNotThrow(() -> engine.validateScripts(Set.of("primary-alias", "secondary-alias", "third-alias")));
	}

	@Test
	void rejectsDuplicateAcrossAliasesFromDifferentClasses() {
		AI2Engine engine = new AI2Engine();
		engine.registerAI(MultiAliasAi.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> engine.registerAI(ConflictingAliasAi.class));
		assertEquals("log.ai_engine.duplicate_name", exception.getMessage());
	}

	@Test
	void instanceReturnsConfiguredNameOrFirstAlias() {
		AI2Engine engine = new AI2Engine();
		engine.registerAI(MultiAliasAi.class);

		MultiAliasAi defaultInstance = new MultiAliasAi();
		assertEquals("primary-alias", defaultInstance.getName());

		AI2 customInstance = engine.setupAI("secondary-alias", null);
		assertEquals("secondary-alias", customInstance.getName());
	}

	@AIName("registration-valid")
	public static class ValidAi extends NpcAI2 {
	}

	@AIName("duplicate-registration")
	public static class FirstDuplicateAi extends NpcAI2 {
	}

	@AIName("duplicate-registration")
	public static class SecondDuplicateAi extends NpcAI2 {
	}

	@AIName("missing-no-argument-constructor")
	public static class MissingNoArgumentConstructorAi extends NpcAI2 {

		public MissingNoArgumentConstructorAi(String ignored) {
		}
	}

	@AIName("primary-alias, secondary-alias, third-alias")
	public static class MultiAliasAi extends NpcAI2 {
	}

	@AIName("other-alias, secondary-alias")
	public static class ConflictingAliasAi extends NpcAI2 {
	}
}

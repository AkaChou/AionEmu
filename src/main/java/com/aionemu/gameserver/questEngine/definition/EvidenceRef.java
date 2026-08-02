package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;

/** A precise, human-auditable source reference for a definition. */
public record EvidenceRef(String source, String locator, String statement) {
	public EvidenceRef {
		source = requireText(source, "source");
		locator = requireText(locator, "locator");
		statement = requireText(statement, "statement");
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}
}

package com.aionemu.gameserver.questEngine.definition;

/** Fail-closed compiler error with a stable machine-readable code. */
public final class QuestCompilationException extends IllegalArgumentException {
	private final String code;

	public QuestCompilationException(String code, String message) {
		super(code + ": " + message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}

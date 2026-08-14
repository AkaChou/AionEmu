package com.aionemu.gameserver.questEngine.definition;

/**
 * 带稳定机器可读错误码、默认失败关闭的编译错误。
 * Fail-closed compiler error with a stable machine-readable code.
 */
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

package com.aionemu.gameserver.questEngine.e2e;

/**
 * 任务端到端审计的封闭结果分类；只有已执行且满足确定性不变量的场景才能标记为 PASS。
 * Closed result taxonomy for quest end-to-end audits; PASS is reserved for executed scenarios that satisfy
 * deterministic invariants.
 */
public enum QuestE2eStatus {
	PASS,
	CLICK_NO_RESPONSE,
	NO_ROUTE,
	NO_MATCH,
	AMBIGUOUS_ROUTE,
	STATE_MISMATCH,
	TRANSACTION_FAILURE,
	STATE_CHANGED_WITHOUT_RESPONSE,
	PAGE_NOT_IN_CLIENT,
	INVALID_INTERACTION_OBJECT,
	INVALID_DIALOG_PACKET,
	INVALID_PACKET_ORDER,
	AFTER_COMMIT_FAILURE,
	RUNTIME_REQUIRED,
	EVIDENCE_REQUIRED
}

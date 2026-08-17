package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;

import java.util.List;

/**
 * 独立校验任务出站包的身份字段和提交后顺序，不把当前 IR 的页面值当作协议正确性的证明。
 * Independently validates outbound packet identity fields and post-commit ordering without treating current-IR
 * page values as proof of protocol correctness.
 */
public final class QuestE2ePacketValidator {
	private QuestE2ePacketValidator() {
	}

	/** 一个包合同校验结果。 / Result of one packet-contract validation. */
	public record Result(QuestE2eStatus status, String reason) {
		public Result {
			status = java.util.Objects.requireNonNull(status, "status");
			reason = java.util.Objects.requireNonNullElse(reason, "");
		}

		public boolean valid() {
			return status == QuestE2eStatus.PASS;
		}
	}

	/**
	 * 校验对话目标、questId、任务动作字段及 sync-before-page 顺序。
	 * Validates dialog target, questId, quest-action fields, and sync-before-page ordering.
	 */
	public static Result validate(CompiledQuestDefinition definition, QuestTransition transition,
			int expectedDialogTargetObjectId, List<ServerPacketObservation> packets) {
		if (expectedDialogTargetObjectId < 0) {
			throw new IllegalArgumentException("expectedDialogTargetObjectId must be non-negative");
		}
		if (packets == null) {
			return new Result(QuestE2eStatus.INVALID_PACKET_ORDER, "packet observation list is missing");
		}
		for (ServerPacketObservation packet : packets) {
			if (packet.type() == ServerPacketObservation.Type.QUEST_ACTION
					&& packet.questId() != definition.id()) {
				return new Result(QuestE2eStatus.INVALID_DIALOG_PACKET,
					"SM_QUEST_ACTION questId does not match owner " + definition.id());
			}
			if (packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW) {
				if (packet.questId() != 0 && packet.questId() != definition.id()) {
						return new Result(QuestE2eStatus.INVALID_DIALOG_PACKET,
							"SM_DIALOG_WINDOW questId does not match owner " + definition.id());
					}
					if (packet.dialogId() == 0) {
						continue;
					}
					if (expectedDialogTargetObjectId > 0 && packet.targetObjectId() <= 0) {
						return new Result(QuestE2eStatus.INVALID_INTERACTION_OBJECT,
							"SM_DIALOG_WINDOW has no authoritative targetObjectId");
					}
					if (packet.targetObjectId() != expectedDialogTargetObjectId) {
						return new Result(QuestE2eStatus.INVALID_DIALOG_PACKET,
							"SM_DIALOG_WINDOW targetObjectId does not match interaction object");
				}
			}
		}
		int syncIndex = firstIndex(packets, ServerPacketObservation.Type.QUEST_ACTION);
		int pageIndex = firstIndex(packets, ServerPacketObservation.Type.DIALOG_WINDOW);
		boolean syncDeclared = transition.afterCommit().stream()
			.anyMatch(action -> action instanceof AfterCommitAction.SyncQuestState);
		boolean pageDeclared = transition.afterCommit().stream().anyMatch(action ->
			action instanceof AfterCommitAction.ShowQuestDialog
				|| action instanceof AfterCommitAction.ShowQuestSelectionDialog);
		if (syncDeclared && pageDeclared && syncIndex >= 0 && pageIndex >= 0 && syncIndex > pageIndex) {
			return new Result(QuestE2eStatus.INVALID_PACKET_ORDER,
				"state synchronization packet was emitted after dialog page");
		}
		return new Result(QuestE2eStatus.PASS, "");
	}

	private static int firstIndex(List<ServerPacketObservation> packets, ServerPacketObservation.Type type) {
		for (int index = 0; index < packets.size(); index++) {
			if (packets.get(index).type() == type) {
				return index;
			}
		}
		return -1;
	}
}

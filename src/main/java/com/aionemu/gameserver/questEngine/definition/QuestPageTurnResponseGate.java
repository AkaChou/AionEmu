package com.aionemu.gameserver.questEngine.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 「翻页动作完全无回包」编译期门禁：客户端按钮点击不会得到任何包的转换属于必现死按钮。
 * Compile-time gate for page-turn actions that answer nothing at all: a transition that sends no packet
 * for a client button press is an always-dead button.
 * <p>判定条件：事件是 {@link QuestEvent.TalkToNpc} 且动作 ID 在客户端合同里对应「有按钮的页面」，同时
 * {@code after-commit} 完全为空。此时服务端不回任何包，客户端停在原页并反复重发
 * {@code CM_DIALOG_SELECT}。影片类静默形态（after-commit 只有 {@code play-movie}）由
 * {@code MOVIE_WITHOUT_CONTINUATION} 覆盖，本门禁不重复报告。</p>
 * <p>The rule matches a {@link QuestEvent.TalkToNpc} whose action id maps to a client page with buttons
 * while the {@code after-commit} block is completely empty: the server answers nothing, so the client
 * stays on the page and keeps resending {@code CM_DIALOG_SELECT}. Silent movie forms (an after-commit
 * that only plays a movie) are covered by {@code MOVIE_WITHOUT_CONTINUATION} and are not reported here.</p>
 */
public final class QuestPageTurnResponseGate {
	private QuestPageTurnResponseGate() {
	}

	/**
	 * 枚举违反门禁的转换。
	 * Lists the transitions that violate the gate.
	 * @param definition 已解析的任务定义 / parsed quest definition
	 * @param contract 客户端页面契约；空契约（单元测试构造的 IR）不参与判定 /
	 *                 client page contract; an empty contract (unit-test IR) disables the rule
	 * @return 违规清单，按转换声明顺序 / violations in transition declaration order
	 */
	public static List<Violation> violations(QuestDefinition definition, QuestDialogContract contract) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(contract, "contract");
		if (contract.isEmpty()) {
			return List.of();
		}
		List<Violation> result = new ArrayList<>();
		for (QuestTransition transition : definition.transitions()) {
			if (!(transition.event() instanceof QuestEvent.TalkToNpc talk) || talk.dialogId() == null) {
				continue;
			}
			if (!contract.hasButtonPage(definition.id(), talk.dialogId())) {
				continue;
			}
			if (!transition.afterCommit().isEmpty()) {
				continue;
			}
			result.add(new Violation(definition.id(), transition.sourceNode(), talk.dialogId()));
		}
		return List.copyOf(result);
	}

	/** 一条「翻页动作无任何回包」违规。 / One page-turn action that answers nothing. */
	public record Violation(int questId, String sourceNode, int dialogId) {
		public Violation {
			if (questId <= 0) {
				throw new IllegalArgumentException("questId must be positive");
			}
			sourceNode = Objects.requireNonNull(sourceNode, "sourceNode");
			if (dialogId <= 0) {
				throw new IllegalArgumentException("dialogId must be positive");
			}
		}
	}
}

package com.aionemu.gameserver.questEngine.e2e.client;

import java.util.ArrayList;
import java.util.List;

/**
 * 按实际调用顺序保存客户端、事务、发布和提交后动作的场景轨迹。
 * Scenario trace preserving the actual order of client, transaction, publication, and after-commit actions.
 */
public final class QuestTrace {
	private final List<Entry> entries = new ArrayList<>();

	/** 单条有序轨迹记录。 / One ordered trace entry. */
	public record Entry(int sequence, String phase, String detail) {
	}

	/** 追加一个阶段记录并分配稳定序号。 / Appends a phase entry and assigns a stable sequence number. */
	public void add(String phase, String detail) {
		if (phase == null || phase.isBlank()) {
			throw new IllegalArgumentException("phase must not be blank");
		}
		entries.add(new Entry(entries.size(), phase, detail == null ? "" : detail));
	}

	/** 返回当前轨迹的不可变快照。 / Returns an immutable snapshot of the current trace. */
	public List<Entry> entries() {
		return List.copyOf(entries);
	}

	/** 清除轨迹以开始独立场景。 / Clears the trace before an independent scenario. */
	public void clear() {
		entries.clear();
	}
}

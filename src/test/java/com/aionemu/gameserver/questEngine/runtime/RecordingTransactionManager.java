package com.aionemu.gameserver.questEngine.runtime;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * 记录 begin/commit/rollback 的事务管理器测试替身，替代旧测试中的 JDBC 连接代理。
 * Recording transaction-manager test double, replacing the legacy JDBC connection proxies.
 */
final class RecordingTransactionManager implements PlatformTransactionManager {
	private final Consumer<String> calls;
	private final String beginLabel;
	private boolean failCommit;

	RecordingTransactionManager(List<String> calls) {
		this(calls::add, "begin");
	}

	RecordingTransactionManager(Consumer<String> calls) {
		this(calls, "begin");
	}

	RecordingTransactionManager(Consumer<String> calls, String beginLabel) {
		this.calls = calls;
		this.beginLabel = beginLabel;
	}

	/** 让下一次 commit 抛出异常，模拟提交失败。 / Makes the next commit throw, simulating a failed commit. */
	void failNextCommit() {
		failCommit = true;
	}

	@Override
	public TransactionStatus getTransaction(TransactionDefinition definition) {
		calls.accept(beginLabel);
		return new SimpleTransactionStatus();
	}

	@Override
	public void commit(TransactionStatus status) {
		calls.accept("commit");
		if (failCommit) {
			failCommit = false;
			throw new IllegalStateException("injected commit failure");
		}
	}

	@Override
	public void rollback(TransactionStatus status) {
		calls.accept("rollback");
	}
}

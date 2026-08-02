package com.aionemu.gameserver.questEngine.runtime;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared transaction boundary for required quest mutations and durable rewards. */
public final class QuestUnitOfWork implements AutoCloseable {
	private final Connection connection;
	private final List<Runnable> afterCommit = new ArrayList<>();
	private final List<RuntimeException> afterCommitFailures = new ArrayList<>();
	private boolean completed;
	private boolean committed;

	private QuestUnitOfWork(Connection connection) throws SQLException {
		this.connection = Objects.requireNonNull(connection, "connection");
		if (connection.getAutoCommit()) {
			connection.setAutoCommit(false);
		}
	}

	public static QuestUnitOfWork open(Connection connection) throws SQLException {
		return new QuestUnitOfWork(connection);
	}

	public Connection connection() {
		return connection;
	}

	public void afterCommit(Runnable action) {
		if (completed) {
			throw new IllegalStateException("cannot register afterCommit after completion");
		}
		afterCommit.add(Objects.requireNonNull(action, "action"));
	}

	public void commit() throws SQLException {
		ensureOpen();
		connection.commit();
		committed = true;
		completed = true;
		// afterCommit 由 runAfterCommit() 显式执行,使调用方能在提交成功后、
		// 协议包发出之前先发布内存状态。
	}

	/**
	 * Runs the registered after-commit actions best-effort. Must be called only
	 * after a successful {@link #commit()}; failures are collected without
	 * throwing so the committed mutation is never rolled back.
	 */
	public void runAfterCommit() {
		if (!committed) {
			throw new IllegalStateException("after-commit actions require a committed unit of work");
		}
		for (Runnable action : afterCommit) {
			try {
				action.run();
			} catch (RuntimeException failure) {
				afterCommitFailures.add(failure);
			}
		}
	}

	public void rollback() throws SQLException {
		if (completed) {
			return;
		}
		connection.rollback();
		completed = true;
	}

	public boolean committed() {
		return committed;
	}

	public List<RuntimeException> afterCommitFailures() {
		return List.copyOf(afterCommitFailures);
	}

	private void ensureOpen() {
		if (completed) {
			throw new IllegalStateException("quest unit of work is already complete");
		}
	}

	@Override
	public void close() throws SQLException {
		if (!completed) {
			rollback();
		}
	}
}

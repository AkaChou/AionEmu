package com.aionemu.gameserver.questEngine.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.aionemu.gameserver.persistence.DatabaseAccess;

/**
 * 必需任务变更与耐久奖励的共享事务边界。
 * Shared transaction boundary for required quest mutations and durable rewards.
 * <p>边界由 Spring 事务管理器持有，因此边界内的领域端口通过 MyBatis-Flex 访问数据库时
 * 自动加入同一事务，无需再把连接逐级传参。
 * The boundary is owned by a Spring transaction manager, so the domain ports inside it reach the
 * database through MyBatis-Flex and join the same transaction without threading a connection
 * through every call.</p>
 */
public final class QuestUnitOfWork implements AutoCloseable {
	private final PlatformTransactionManager transactionManager;
	private final TransactionStatus status;
	private final List<Runnable> afterCommit = new ArrayList<>();
	private final List<RuntimeException> afterCommitFailures = new ArrayList<>();
	private boolean completed;
	private boolean committed;

	private QuestUnitOfWork(PlatformTransactionManager transactionManager) {
		this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager");
		this.status = transactionManager.getTransaction(new DefaultTransactionDefinition());
	}

	/** 以 {@link DatabaseAccess} 绑定的生产事务管理器开启工作单元。/ Opens a unit of work on the production transaction manager bound by {@link DatabaseAccess}. */
	public static QuestUnitOfWork open() {
		return new QuestUnitOfWork(DatabaseAccess.transactionManager());
	}

	/** 以调用方给出的事务管理器开启工作单元（供测试注入）。/ Opens a unit of work on a caller-supplied transaction manager (for tests). */
	public static QuestUnitOfWork open(PlatformTransactionManager transactionManager) {
		return new QuestUnitOfWork(transactionManager);
	}

	public void afterCommit(Runnable action) {
		if (completed) {
			throw new IllegalStateException("cannot register afterCommit after completion");
		}
		afterCommit.add(Objects.requireNonNull(action, "action"));
	}

	public void commit() {
		ensureOpen();
		transactionManager.commit(status);
		committed = true;
		completed = true;
		// afterCommit 由 runAfterCommit() 显式执行，使调用方能在提交成功后、
		// 协议包发出之前先发布内存状态。
		// afterCommit runs explicitly via runAfterCommit(), letting the caller publish
		// in-memory state after a successful commit but before protocol packets are sent.
	}

	/**
	 * best-effort 运行已注册的提交后动作。只能在成功 {@link #commit()} 后调用；
	 * 失败被收集而不抛出，已提交的变更绝不会回滚。
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

	public void rollback() {
		if (completed) {
			return;
		}
		transactionManager.rollback(status);
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
	public void close() {
		if (!completed) {
			rollback();
		}
	}
}

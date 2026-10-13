package com.aionemu.gameserver.utils.idfactory;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.GenericValidator;

/**
 * 线程安全的可回收整数 ID 分配器；非法操作抛出 {@link IDFactoryError}。
 * Thread-safe recyclable integer ID allocator; illegal operations throw {@link IDFactoryError}.
 *
 * @author SoulKeeper
 */
@Slf4j
public class IDFactory {

	/**
	 * Spring ObjectProvider，优先于静态单例。
	 * Spring ObjectProvider preferred over the static singleton.
	 */
	private static volatile ObjectProvider<IDFactory> instanceProvider;
	private static volatile IDFactory resolvedInstance;
	/**
	 * 已占用 ID 的 BitSet（可增长至 {@link Integer#MAX_VALUE}）。
	 * BitSet of taken IDs (may grow up to {@link Integer#MAX_VALUE}).
	 */
	private final BitSet idList;

	/**
	 * BitSet 同步锁。
	 * Lock for BitSet synchronization.
	 */
	private final ReentrantLock lock;

	/**
	 * 下次分配时的搜索起点。
	 * Search start for the next free id.
	 */
	private volatile int nextMinId = 1;

	/**
	 * 构造并锁定数据库中已使用的 ID。
	 * Construct and lock IDs already used in the database.
	 */
	public IDFactory(List<Supplier<int[]>> usedIdSources) {
		idList = new BitSet();
		lock = new ReentrantLock();
		lock.lock();
		try {
			doLockIds(0);
			usedIdSources.forEach(source -> doLockIds(source.get()));
		} finally {
			lock.unlock();
		}
		log.info(I18n.get("log.b00d0337832b", getUsedCount()));
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static IDFactory getInstance() {
		IDFactory resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<IDFactory> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("IDFactory 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / Provider
	 */
	public static void setInstanceProvider(ObjectProvider<IDFactory> instanceProvider) {
		IDFactory.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

	/**
	 * 分配下一个空闲 ID。
	 * Allocate the next free id.
	 *
	 * @return 下一个空闲 ID / Next free id
	 * @throws IDFactoryError 无空闲 ID 时 / If no free ids remain
	 */
	public int nextId() {
		try {
			lock.lock();

			int id;
			if (nextMinId == Integer.MIN_VALUE) {
				// 稍后几行会抛错：已无空闲 ID。 / Error will be thrown few lines later, we have no more free id's.
				// 若 nextMinId 为负，BitSet 将抛出 IllegalArgumentException / BitSet will throw IllegalArgumentException if nextMinId is negative
				id = Integer.MIN_VALUE;
			} else {
				id = idList.nextClearBit(nextMinId);
			}

			// 若 BitSet 达到 Integer.MAX_VALUE 并返回最后一个空闲 ID—— / If BitSet reached Integer.MAX_VALUE size and returned last free id before -
			// 它将返回 / it will return
			// Integer.MIN_VALUE 作为下一 ID 时须捕获并抛错。 / Intger.MIN_VALUE as the next id, so we must catch such case and throw error
			// （无空闲 ID） / (no free id's left)
			if (id == Integer.MIN_VALUE) {
				throw new IDFactoryError("All id's are used, please clear your database");
			}
			idList.set(id);

			// 此处 Integer 溢出可接受；下次申请 ID 时 IDFactory 会抛错。 / It ok to have Integer OverFlow here, on next ID request IDFactory will throw
			// 错误 / error
			nextMinId = id + 1;
			return id;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 锁定给定 ID（无锁内核，调用方须已持有 {@link #lock}）。
	 * Lock the given ids (lock-free core; the caller must already hold {@link #lock}).
	 *
	 * @param ids 要锁定的 ID / Ids to lock
	 * @throws IDFactoryError 有 ID 已占用时 / If some ids were already locked
	 */
	private void doLockIds(int... ids) {
		for (int id : ids) {
			if (idList.get(id)) {
				throw new IDFactoryError("ID " + id + " is already taken, fatal error!!!");
			}
			idList.set(id);
		}
	}

	/**
	 * 锁定给定 ID 集合（已占用则抛错）。
	 * Lock the given id collection (throws if already taken).
	 *
	 * @param ids 要锁定的 ID / Ids to lock
	 * @throws IDFactoryError 有 ID 已占用时 / If some ids were already locked
	 */
	public void lockIds(Iterable<Integer> ids) {
		lock.lock();
		try {
			for (int id : ids) {
				doLockIds(id);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 释放给定 ID（无锁内核，调用方须已持有 {@link #lock}）。
	 * Release the given id (lock-free core; the caller must already hold {@link #lock}).
	 *
	 * @param id 要释放的 ID / Id to release
	 * @return 是否成功释放（未占用返回 false）/ whether the id was released (false if it was not taken)
	 */
	private boolean release(int id) {
		if (!idList.get(id)) {
			return false;
		}
		idList.clear(id);
		if (id < nextMinId || nextMinId == Integer.MIN_VALUE) {
			nextMinId = id;
		}
		return true;
	}

	/**
	 * 释放给定 ID。
	 * Release the given id.
	 *
	 * @param id 要释放的 ID / Id to release
	 * @throws IDFactoryError ID 未被占用时 / If the id was not taken
	 */
	public void releaseId(int id) {
		lock.lock();
		try {
			if (!release(id)) {
				throw new IDFactoryError("ID " + id + " is not taken, can't release it.");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 批量释放 ID。
	 * Release a collection of ids.
	 *
	 * @param ids 要释放的 ID 集合 / Ids to release
	 * @throws IDFactoryError 有 ID 未被占用时 / If any id was not taken
	 */
	public void releaseIds(Collection<Integer> ids) {
		if (GenericValidator.isBlankOrNull(ids)) {
			return;
		}

		lock.lock();
		try {
			for (Integer id : ids) {
				if (!release(id)) {
					throw new IDFactoryError("ID " + id + " is not taken, can't release it.");
				}
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 返回已占用 ID 数量。
	 * Amount of used ids.
	 *
	 * @return 已占用数量 / Used count
	 */
	public int getUsedCount() {
		lock.lock();
		try {
			return idList.cardinality();
		} finally {
			lock.unlock();
		}
	}
}

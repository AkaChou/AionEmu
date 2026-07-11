package com.aionemu.gameserver.utils.idfactory;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.BitSet;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.dao.GuideDAO;
import com.aionemu.gameserver.dao.HousesDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO;

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
	public IDFactory() {
		idList = new BitSet();
		lock = new ReentrantLock();
		lockIds(0);
		// 此处应调用所有 IDFactoryAwareDAO 实现以初始化。 / Here should be calls to all IDFactoryAwareDAO implementations to initialize
		// IDFactory 中使用的值 / used values in IDFactory
		lockIds(DAOManager.getDAO(PlayerDAO.class).getUsedIDs());
		lockIds(DAOManager.getDAO(InventoryDAO.class).getUsedIDs());
		lockIds(DAOManager.getDAO(PlayerRegisteredItemsDAO.class).getUsedIDs());
		lockIds(DAOManager.getDAO(LegionDAO.class).getUsedIDs());
		lockIds(DAOManager.getDAO(MailDAO.class).getUsedIDs());
		lockIds(DAOManager.getDAO(GuideDAO.class).getUsedIDs());
		lockIds(DAOManager.getDAO(HousesDAO.class).getUsedIDs());
		log.info(I18n.get("log.b00d0337832b", getUsedCount()));
	}

	/**
	 * 获取 IDFactory 实例（优先 Spring 提供者）。
	 * Get the IDFactory instance (Spring provider preferred).
	 *
	 * Instance
	 */
	public static final IDFactory getInstance() {
		ObjectProvider<IDFactory> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * Provider
	 */
	public static void setInstanceProvider(ObjectProvider<IDFactory> instanceProvider) {
		IDFactory.instanceProvider = instanceProvider;
	}

	/**
	 * 分配下一个空闲 ID。
	 * Allocate the next free id.
	 *
	 * Next free id
	 * If no free ids remain
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
	 * 锁定给定 ID（已占用则抛错）。
	 * Lock the given ids (throws if already taken).
	 *
	 * @param ids 要锁定的 ID / Ids to lock
	 * If some ids were already locked
	 */
	private void lockIds(int... ids) {
		try {
			lock.lock();
			for (int id : ids) {
				boolean status = idList.get(id);
				if (status) {
					throw new IDFactoryError("ID " + id + " is already taken, fatal error!!!");
				}
				idList.set(id);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 锁定给定 ID 集合（已占用则抛错）。
	 * Lock the given id collection (throws if already taken).
	 *
	 * @param ids 要锁定的 ID / Ids to lock
	 * If some ids were already locked
	 */
	public void lockIds(Iterable<Integer> ids) {
		try {
			lock.lock();
			for (int id : ids) {
				boolean status = idList.get(id);
				if (status) {
					throw new IDFactoryError("ID " + id + " is already taken, fatal error!!!");
				}
				idList.set(id);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 释放给定 ID。
	 * Release the given id.
	 *
	 * @param id 要释放的 ID / Id to release
	 * If the id was not taken。 / If the id was not taken.
	 */
	public void releaseId(int id) {
		try {
			lock.lock();
			boolean status = idList.get(id);
			if (!status) {
				throw new IDFactoryError("ID " + id + " is not taken, can't release it.");
			}
			idList.clear(id);
			if (id < nextMinId || nextMinId == Integer.MIN_VALUE) {
				nextMinId = id;
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
	 * If any id was not taken。 / If any id was not taken.
	 */
	public void releaseIds(Collection<Integer> ids) {
		if (GenericValidator.isBlankOrNull(ids)) {
			return;
		}

		try {
			lock.lock();
			for (Integer id : ids) {
				boolean status = idList.get(id);
				if (!status) {
					throw new IDFactoryError("ID " + id + " is not taken, can't release it.");
				}
				idList.clear(id);
				if (id < nextMinId || nextMinId == Integer.MIN_VALUE) {
					nextMinId = id;
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
	 * Used count
	 */
	public int getUsedCount() {
		try {
			lock.lock();
			return idList.cardinality();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		/**
		 * 单例实例。
		 * Singleton instance.
		 */
		protected static final IDFactory instance = new IDFactory();
	}
}

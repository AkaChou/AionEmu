package com.aionemu.gameserver.taskmanager.parallel;

import com.aionemu.boot.i18n.I18n;
import java.util.Collection;

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinTask;
import com.google.common.base.Predicate;

/**
 * 基于 {@link CountedCompleter} 的并行 for-each：对集合/数组元素应用谓词。
 * Parallel for-each over a collection/array via {@link CountedCompleter}, applying a predicate to each element.
 *
 * @param <E> 元素类型 / Element type
 */
public final class ForEach<E> extends CountedCompleter<E> {

	/**
	 * 序列化版本。
	 * Serialization version.
	 */
	private static final long serialVersionUID = 7902148320917998146L;

	/**
	 * 对集合元素构建并行 for-each 任务；空集合返回 null。
	 * Build a parallel for-each task over a collection; returns null if empty.
	 *
	 * @param list      元素集合 / Element collection
	 * @param operation 对每个元素执行的谓词 / Predicate applied to each element
	 * @param <E>       元素类型 / Element type
	 * @return ForkJoin 任务；空则为 null / ForkJoin task, or null if empty
	 */
	public static <E> ForkJoinTask<E> forEach(Collection<E> list, Predicate<E> operation) {
		if (list.size() > 0) {
			@SuppressWarnings("unchecked")
			E[] objects = list.toArray((E[]) new Object[list.size()]);
			CountedCompleter<E> completer = new ForEach<E>(null, operation, 0, objects.length, objects);
			return completer;
		}
		return null;
	}

	/**
	 * 对变长参数数组构建并行 for-each 任务；空/null 返回 null。
	 * Build a parallel for-each task over a vararg array; returns null if empty/null.
	 *
	 * @param operation 对每个元素执行的谓词 / Predicate applied to each element
	 * @param list      元素数组 / Element array
	 * @param <E>       元素类型 / Element type
	 * @return ForkJoin 任务；空则为 null / ForkJoin task, or null if empty
	 */
	@SafeVarargs
	public static <E> ForkJoinTask<E> forEach(Predicate<E> operation, E... list) {
		if (list != null && list.length > 0) {
			CountedCompleter<E> completer = new ForEach<E>(null, operation, 0, list.length, list);
			return completer;
		}
		return null;
	}

	/**
	 * 待处理元素数组。
	 * Elements to process.
	 */
	final E[] list;

	/**
	 * 应用于每个元素的操作。
	 * Operation applied to each element.
	 */
	final Predicate<E> operation;

	/**
	 * 当前分片下界（含）。
	 * Inclusive lower bound of this slice.
	 */
	final int lo;

	/**
	 * 当前分片上界（不含）。
	 * Exclusive upper bound of this slice.
	 */
	final int hi;

	/**
	 * 构造指定分片的并行 for-each 任务。
	 * Construct a parallel for-each task for the given slice.
	 *
	 * @param rootTask  父完成器 / Parent completer
	 * @param operation 元素操作谓词 / Predicate operation
	 * @param lo        下界（含）/ Inclusive lower bound
	 * @param hi        上界（不含）/ Exclusive upper bound
	 * @param list      元素数组 / Element array
	 */
	@SafeVarargs
	private ForEach(CountedCompleter<E> rootTask, Predicate<E> operation, int lo, int hi, E... list) {
		super(rootTask);
		this.list = list;
		this.operation = operation;
		this.lo = lo;
		this.hi = hi;
	}

	/**
	 * 二分 fork 子任务，叶子节点对单个元素应用操作。
	 * Binary-split fork of child tasks; leaf applies the operation to one element.
	 */
	@Override
	public void compute() {
		int l = lo, h = hi;
		while (h - l >= 2) {
			int mid = (l + h) >>> 1;
			addToPendingCount(1);
			new ForEach<E>(this, operation, mid, h, list).fork();
			h = mid;
		}
		if (h > l) {
			try {
				operation.apply(list[l]);
			} catch (Throwable ex) {
				onExceptionalCompletion(ex, this);
			}
		}
		propagateCompletion();
	}

	/**
	 * 异常完成时继续传播（当前实现始终返回 true）。
	 * On exceptional completion, keep propagating (always returns true here).
	 *
	 * @param ex 异常 / Exception
	 * @param caller 调用方完成器 / Caller completer
	 * @return 是否继续处理 / Whether to continue handling
	 */
	@Override
	public boolean onExceptionalCompletion(Throwable ex, CountedCompleter<?> caller) {
		// log.warn(I18n.get("log.da39a3ee5e6b", ex));
		return true;
	}
}

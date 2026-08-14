package com.aionemu.gameserver.world;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.AionObject;

/**
 * 可见对象集合执行器：对集合中每个对象调用 {@link #run(AionObject)}。
 * Executor over a collection of visible objects, invoking {@link #run(AionObject)} for each.
 *
 * @param <T> 对象类型，须为 {@link AionObject} 子类 / object type, must extend {@link AionObject}
 * @author xavier
 */
@Slf4j
public abstract class Executor<T extends AionObject> {

	/**
	 * 对单个对象执行业务逻辑；返回 false 时中止后续遍历。
	 * Run business logic for a single object; return false to abort further iteration.
	 *
	 * @param object 目标对象 / the target object
	 * @return 是否继续遍历 / whether to continue iterating
	 */
	public abstract boolean run(T object);

	/**
	 * 同步遍历并执行集合。
	 * Synchronously iterate and run over the collection.
	 *
	 * @param objects 对象集合 / the object collection
	 */
	private final void runImpl(Collection<T> objects) {
		try {
			for (T o : objects) {
				if (o != null) {
					if (!Executor.this.run(o)) {
						break;
					}
				}
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}

	/**
	 * 执行集合遍历；{@code now=true} 时同步执行，否则提交线程池异步执行。
	 * Execute over the collection; synchronous when {@code now=true}, otherwise async via thread pool.
	 *
	 * @param objects 对象集合 / the object collection
	 * @param now 是否立即同步执行 / whether to run immediately on the calling thread
	 */
	public final void execute(final Collection<T> objects, boolean now) {
		if (now) {
			runImpl(objects);
		} else {
			GameThreadPoolServices.threadPoolManager().execute(new Runnable() {

				@Override
				public void run() {
					runImpl(objects);
				}
			});
		}
	}

	/**
	 * 异步执行集合遍历（默认走线程池）。
	 * Asynchronously execute over the collection (default via thread pool).
	 *
	 * @param objects 对象集合 / the object collection
	 */
	public final void execute(final Collection<T> objects) {
		execute(objects, false);
	}
}

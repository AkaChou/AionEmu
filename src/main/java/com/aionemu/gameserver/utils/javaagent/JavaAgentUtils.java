package com.aionemu.gameserver.utils.javaagent;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.commons.callbacks.EnhancedObject;
import com.aionemu.commons.callbacks.metadata.GlobalCallback;
import com.aionemu.commons.callbacks.metadata.ObjectCallback;
import com.aionemu.commons.callbacks.util.GlobalCallbackHelper;

/**
 * 校验 Java Agent 回调字节码织入是否已正确配置。
 * Utility to verify that Java-agent callback bytecode weaving is configured.
 */
public class JavaAgentUtils {
	static {
		GlobalCallbackHelper.addCallback(new CheckCallback());
	}

	/**
	 * 检测全局/对象回调织入是否可用；失败则抛 Error。
	 * Verify global/object callback weaving; throws Error if misconfigured.
	 *
	 * @return 配置正确则为 true / True if configured correctly
	 */
	public static boolean isConfigured() {
		JavaAgentUtils jau = new JavaAgentUtils();
		if (!(jau instanceof EnhancedObject)) {
			throw new Error("Callback bytecode weaving is not configured. Run Maven process-classes before startup.");
		}

		if (!checkGlobalCallback()) {
			throw new Error("Global callbacks are not working correctly!");
		}

		((EnhancedObject) jau).addCallback(new CheckCallback());
		if (!jau.checkObjectCallback()) {
			throw new Error("Object callbacks are not working correctly!");
		}
		return true;
	}

	/**
	 * 全局回调探针方法（织入后应被拦截）。
	 * Global-callback probe method (should be intercepted when woven).
	 *
	 * @return 未拦截时返回 false / False when not intercepted
	 */
	@GlobalCallback(CheckCallback.class)
	private static boolean checkGlobalCallback() {
		return false;
	}

	/**
	 * 对象回调探针方法（织入后应被拦截）。
	 * Object-callback probe method (should be intercepted when woven).
	 *
	 * @return 未拦截时返回 false / False when not intercepted
	 */
	@ObjectCallback(CheckCallback.class)
	private boolean checkObjectCallback() {
		return false;
	}

	/**
	 * 探测用回调：beforeCall 完全拦截并返回 true。
	 * Probe callback that full-blocks beforeCall and returns true.
	 */
	@SuppressWarnings("rawtypes")
	public static class CheckCallback implements Callback {

		/**
		 * 调用前完全拦截，返回 true。
		 * Fully block before call and return true.
		 *
		 * @param obj 目标对象 / Target object
		 * Arguments
		 * Blocking result
		 */
		@Override
		public CallbackResult<Boolean> beforeCall(Object obj, Object[] args) {
			return CallbackResult.newFullBlocker(true);
		}

		/**
		 * 调用后继续。
		 * Continue after call.
		 *
		 * @param obj 目标对象 / Target object
		 * Arguments
		 * @param methodResult 方法返回值 / Method result
		 * Continue result
		 */
		@Override
		public CallbackResult<Boolean> afterCall(Object obj, Object[] args, Object methodResult) {
			return CallbackResult.newContinue();
		}

		/**
		 * 回调基类。
		 * Callback base class.
		 *
		 * Base class
		 */
		@Override
		public Class<? extends Callback> getBaseClass() {
			return CheckCallback.class;
		}
	}
}

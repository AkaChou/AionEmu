package com.aionemu.commons.callbacks.fixture;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.commons.callbacks.metadata.GlobalCallback;
import com.aionemu.commons.callbacks.metadata.ObjectCallback;

public class CallbackWeavingFixture {

	@ObjectCallback(ObjectBlockerCallback.class)
	public int objectValue(int value) {
		return value;
	}

	@GlobalCallback(GlobalBlockerCallback.class)
	public static int globalValue(int value) {
		return value;
	}

	public static class ObjectBlockerCallback implements Callback<Object> {

		@Override
		public CallbackResult<Integer> beforeCall(Object obj, Object[] args) {
			return CallbackResult.newFullBlocker(77);
		}

		@Override
		public CallbackResult<Integer> afterCall(Object obj, Object[] args, Object methodResult) {
			return CallbackResult.newContinue();
		}

		@Override
		public Class<? extends Callback> getBaseClass() {
			return ObjectBlockerCallback.class;
		}
	}

	public static class GlobalBlockerCallback implements Callback<Object> {

		@Override
		public CallbackResult<Integer> beforeCall(Object obj, Object[] args) {
			return CallbackResult.newFullBlocker(88);
		}

		@Override
		public CallbackResult<Integer> afterCall(Object obj, Object[] args, Object methodResult) {
			return CallbackResult.newContinue();
		}

		@Override
		public Class<? extends Callback> getBaseClass() {
			return GlobalBlockerCallback.class;
		}
	}
}

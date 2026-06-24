package com.aionemu.gameserver.lifecycle;

interface StartupProgressReporter {

	void start(String groupName);

	void stepStarted(String stepName);

	void stepFinished(String stepName);

	void finish(String groupName, long elapsedMillis);

	void failed();

	static StartupProgressReporter noop() {
		return new StartupProgressReporter() {
			@Override
			public void start(String groupName) {
			}

			@Override
			public void stepStarted(String stepName) {
			}

			@Override
			public void stepFinished(String stepName) {
			}

			@Override
			public void finish(String groupName, long elapsedMillis) {
			}

			@Override
			public void failed() {
			}
		};
	}
}

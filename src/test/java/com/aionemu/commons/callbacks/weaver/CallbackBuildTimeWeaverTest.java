package com.aionemu.commons.callbacks.weaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.callbacks.EnhancedObject;
import com.aionemu.commons.callbacks.fixture.CallbackWeavingFixture;
import com.aionemu.commons.callbacks.util.GlobalCallbackHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CallbackBuildTimeWeaverTest {

	private static final String FIXTURE_CLASS_NAME = "com.aionemu.commons.callbacks.fixture.CallbackWeavingFixture";
	private static final Path FIXTURE_CLASS_FILE = Path.of("target/test-classes",
		"com/aionemu/commons/callbacks/fixture/CallbackWeavingFixture.class");

	@Test
	void weavesObjectAndGlobalCallbacksWithoutJavaagent() throws Exception {
		byte[] originalBytes = Files.readAllBytes(FIXTURE_CLASS_FILE);
		byte[] wovenBytes = CallbackBuildTimeWeaver.weaveClassBytes(getClass().getClassLoader(), originalBytes);
		Class<?> fixtureType = new SingleClassLoader(getClass().getClassLoader(), FIXTURE_CLASS_NAME, wovenBytes).loadClass(FIXTURE_CLASS_NAME);

		Object fixture = fixtureType.getConstructor().newInstance();
		assertTrue(EnhancedObject.class.isAssignableFrom(fixtureType));

		((EnhancedObject) fixture).addCallback(new CallbackWeavingFixture.ObjectBlockerCallback());
		assertEquals(77, fixtureType.getMethod("objectValue", int.class).invoke(fixture, 5));

		CallbackWeavingFixture.GlobalBlockerCallback callback = new CallbackWeavingFixture.GlobalBlockerCallback();
		GlobalCallbackHelper.addCallback(callback);
		try {
			assertEquals(88, fixtureType.getMethod("globalValue", int.class).invoke(null, 5));
		} finally {
			GlobalCallbackHelper.removeCallback(callback);
		}
	}

	private static class SingleClassLoader extends ClassLoader {
		private final String className;
		private final byte[] classBytes;

		private SingleClassLoader(ClassLoader parent, String className, byte[] classBytes) {
			super(parent);
			this.className = className;
			this.classBytes = classBytes;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (!name.equals(className)) {
				return super.loadClass(name, resolve);
			}
			Class<?> loadedClass = findLoadedClass(name);
			if (loadedClass == null) {
				loadedClass = defineClass(name, classBytes, 0, classBytes.length);
			}
			if (resolve) {
				resolveClass(loadedClass);
			}
			return loadedClass;
		}
	}
}

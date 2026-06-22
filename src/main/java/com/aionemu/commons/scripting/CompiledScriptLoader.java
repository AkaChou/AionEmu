package com.aionemu.commons.scripting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

public final class CompiledScriptLoader {

	private static final String CLASS_RESOURCE_PATTERN = "classpath*:%s/**/*.class";

	private CompiledScriptLoader() {
	}

	public static Class<?>[] load(String... packageNames) throws IOException, ClassNotFoundException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			classLoader = CompiledScriptLoader.class.getClassLoader();
		}

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
		MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
		Set<String> classNames = new LinkedHashSet<String>();
		for (String packageName : packageNames) {
			String packagePath = packageName.replace('.', '/');
			Resource[] resources = resolver.getResources(String.format(CLASS_RESOURCE_PATTERN, packagePath));
			for (Resource resource : resources) {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
				String className = metadataReader.getClassMetadata().getClassName();
				if (className.indexOf('$') < 0) {
					classNames.add(className);
				}
			}
		}

		List<String> sortedClassNames = new ArrayList<String>(classNames);
		Collections.sort(sortedClassNames);

		List<Class<?>> classes = new ArrayList<Class<?>>(sortedClassNames.size());
		for (String className : sortedClassNames) {
			classes.add(Class.forName(className, false, classLoader));
		}
		return classes.toArray(new Class<?>[classes.size()]);
	}
}

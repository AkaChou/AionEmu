package com.aionemu.commons.utils.xml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * 基于 JAXB 注解元数据的直接 StAX 绑定器。
 * Direct StAX binder using JAXB annotations as metadata only.
 *
 * <p>The binder never creates a {@code JAXBContext} or {@code Unmarshaller}. XML is consumed once and
 * model objects are constructed directly. Annotations remain the source of element and attribute names,
 * which keeps the existing XML contracts intact while avoiding the JAXB binding runtime.</p>
 *
 * <p>绑定器不创建 JAXB 运行时，XML 只读取一次并直接构造模型对象；JAXB 注解仅用于描述 XML 名称与结构。</p>
 */
public final class StreamingXmlBinder {

	private static final String DEFAULT_XML_NAME = "##default";
	private static final Map<Class<?>, TypeMetadata> METADATA = new ConcurrentHashMap<>();
	private static final Map<Class<?>, AdapterMetadata> CLASS_ADAPTERS = new ConcurrentHashMap<>();
	private static final Map<Class<?>, EnumMetadata> ENUM_METADATA = new ConcurrentHashMap<>();
	private static final AdapterMetadata NO_ADAPTER = new AdapterMetadata(null, null);
	private static final ThreadLocal<XMLInputFactory> INPUT_FACTORY = ThreadLocal.withInitial(() -> {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		factory.setProperty(XMLInputFactory.IS_COALESCING, false);
		return factory;
	});
	private static final DatatypeFactory DATATYPE_FACTORY = createDatatypeFactory();

	private StreamingXmlBinder() {
	}

	/**
	 * 从文件流式读取 XML 并绑定为指定类型。
	 * Streams XML from a file and binds it to the requested type.
	 *
	 * @param file XML 文件 / XML file
	 * @param type 目标类型 / target type
	 * @param <T> 目标类型 / target type
	 * @return 绑定后的对象 / bound object
	 */
	public static <T> T unmarshal(File file, Class<T> type) {
		try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
			return unmarshal(input, type);
		} catch (Exception e) {
			throw failure("Failed to stream XML file " + file.getPath(), e);
		}
	}

	/**
	 * 从输入流式读取 XML 并绑定为指定类型。
	 * Streams XML from an input stream and binds it to the requested type.
	 *
	 * @param input XML 输入流 / XML input stream
	 * @param type 目标类型 / target type
	 * @param <T> 目标类型 / target type
	 * @return 绑定后的对象 / bound object
	 */
	public static <T> T unmarshal(InputStream input, Class<T> type) {
		try {
			XMLStreamReader reader = INPUT_FACTORY.get().createXMLStreamReader(input);
			return unmarshal(reader, type);
		} catch (XMLStreamException e) {
			throw failure("Failed to create XML stream reader for " + type.getName(), e);
		}
	}

	/**
	 * 从字符流式读取 XML 并绑定为指定类型。
	 * Streams XML from a reader and binds it to the requested type.
	 *
	 * @param input XML 字符流 / XML reader
	 * @param type 目标类型 / target type
	 * @param <T> 目标类型 / target type
	 * @return 绑定后的对象 / bound object
	 */
	public static <T> T unmarshal(Reader input, Class<T> type) {
		try {
			XMLStreamReader reader = INPUT_FACTORY.get().createXMLStreamReader(input);
			return unmarshal(reader, type);
		} catch (XMLStreamException e) {
			throw failure("Failed to create XML stream reader for " + type.getName(), e);
		}
	}

	/**
	 * 从已创建的 StAX 读取器绑定对象，读取器由调用方关闭。
	 * Binds an object from an existing StAX reader; ownership of the reader remains with the caller.
	 *
	 * @param reader StAX 读取器 / StAX reader
	 * @param type 目标类型 / target type
	 * @param <T> 目标类型 / target type
	 * @return 绑定后的对象 / bound object
	 */
	public static <T> T unmarshal(XMLStreamReader reader, Class<T> type) {
		try {
			moveToStartElement(reader);
			return type.cast(readObject(reader, type, null, true, adapterMetadata(type)));
		} catch (Exception e) {
			throw failure("Failed to stream XML into " + type.getName(), e);
		}
	}

	private static Object readObject(XMLStreamReader reader, Class<?> requestedType, Object parent, boolean root,
		AdapterMetadata adapter) throws Exception {
		Class<?> modelType = adapter == null ? requestedType : adapter.boundType();
		TypeMetadata metadata = metadata(modelType);
		if (root) {
			validateRootName(reader, metadata);
		}
		if (isSimpleType(modelType)) {
			return convert(readText(reader), requestedType, null);
		}

		Object instance = instantiate(modelType, metadata);
		invokeLifecycle(instance, metadata.beforeUnmarshal(), parent);
		readAttributes(reader, metadata, instance);
		readChildren(reader, metadata, instance);
		invokeLifecycle(instance, metadata.afterUnmarshal(), parent);

		if (adapter == null) {
			return instance;
		}
		@SuppressWarnings("unchecked")
		XmlAdapter<Object, Object> xmlAdapter = (XmlAdapter<Object, Object>) instantiate(adapter.adapterType());
		Object result = xmlAdapter.unmarshal(instance);
		invokeLifecycle(result, "afterUnmarshal", parent);
		return result;
	}

	private static void readAttributes(XMLStreamReader reader, TypeMetadata metadata, Object instance)
		throws Exception {
		for (int index = 0; index < reader.getAttributeCount(); index++) {
			String name = localName(reader.getAttributeLocalName(index));
			AttributeMetadata attribute = metadata.attributes().get(name);
			if (attribute == null) {
				continue;
			}
			String text = reader.getAttributeValue(index);
			Object value = attribute.converter().convert(text);
			attribute.accessor().write(instance, value);
		}
	}

	private static void readChildren(XMLStreamReader reader, TypeMetadata metadata, Object instance)
		throws Exception {
		StringBuilder text = metadata.value() == null ? null : new StringBuilder();
		while (reader.hasNext()) {
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				String name = localName(reader.getLocalName());
				ElementMetadata element = metadata.elements().get(name);
				if (element == null) {
					skipElement(reader);
					continue;
				}
				if (element.xmlList()) {
					List<Object> values = parseList(element.type(), element.genericType(), readText(reader));
					element.accessor().writeAll(instance, values);
				} else if (element.wrapperName() != null) {
					readWrappedElement(reader, element, instance);
				} else {
					Object value = readValueElement(reader, element, instance);
					element.accessor().write(instance, value);
				}
			} else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
				if (metadata.value() != null) {
					text.append(reader.getText());
				}
			} else if (event == XMLStreamConstants.END_ELEMENT) {
				if (metadata.value() != null) {
					Object value = metadata.value().converter().convert(text.toString());
					metadata.value().accessor().write(instance, value);
				}
				return;
			}
		}
		throw new XMLStreamException("Unexpected end of XML while reading " + metadata.type().getName());
	}

	private static void readWrappedElement(XMLStreamReader reader, ElementMetadata element, Object parent)
		throws Exception {
		while (reader.hasNext()) {
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				if (element.wrapperName().equals(localName(reader.getLocalName()))) {
					Object value = readValueElement(reader, element, parent);
					element.accessor().write(parent, value);
				} else {
					skipElement(reader);
				}
			} else if (event == XMLStreamConstants.END_ELEMENT) {
				return;
			}
		}
		throw new XMLStreamException("Unexpected end of wrapped XML element " + element.xmlName());
	}

	private static Object readValueElement(XMLStreamReader reader, ElementMetadata element, Object parent)
		throws Exception {
		AdapterMetadata adapter = element.adapter();
		if (adapter != null) {
			Object bound = isSimpleType(adapter.boundType())
				? convert(readText(reader), adapter.boundType(), null)
				: readObject(reader, adapter.boundType(), parent, false, adapterMetadata(adapter.boundType()));
			@SuppressWarnings("unchecked")
			XmlAdapter<Object, Object> xmlAdapter = (XmlAdapter<Object, Object>) instantiate(adapter.adapterType());
			Object result = xmlAdapter.unmarshal(bound);
			invokeLifecycle(result, "afterUnmarshal", parent);
			return result;
		}
		if (element.converter() != null) {
			return element.converter().convert(readText(reader));
		}
		return readObject(reader, element.type(), parent, false, null);
	}

	private static String readText(XMLStreamReader reader) throws XMLStreamException {
		StringBuilder text = new StringBuilder();
		int depth = 0;
		while (reader.hasNext()) {
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				depth++;
			} else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
				text.append(reader.getText());
			} else if (event == XMLStreamConstants.END_ELEMENT) {
				if (depth == 0) {
					return text.toString();
				}
				depth--;
			}
		}
		throw new XMLStreamException("Unexpected end of XML while reading element text");
	}

	private static void skipElement(XMLStreamReader reader) throws XMLStreamException {
		int depth = 0;
		while (reader.hasNext()) {
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				depth++;
			} else if (event == XMLStreamConstants.END_ELEMENT) {
				if (depth == 0) {
					return;
				}
				depth--;
			}
		}
		throw new XMLStreamException("Unexpected end of XML while skipping element");
	}

	private static void moveToStartElement(XMLStreamReader reader) throws XMLStreamException {
		while (reader.hasNext() && reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
			reader.next();
		}
		if (reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
			throw new XMLStreamException("XML contains no root element");
		}
	}

	private static void validateRootName(XMLStreamReader reader, TypeMetadata metadata) {
		if (metadata.rootName() == null) {
			return;
		}
		String actual = localName(reader.getLocalName());
		if (!metadata.rootName().equals(actual)) {
			throw new IllegalArgumentException(
				"Expected root element '" + metadata.rootName() + "' but found '" + actual + "'");
		}
	}

	private static List<Object> parseList(Class<?> type, Type genericType, String value) throws Exception {
		Class<?> itemType = collectionItemType(type, genericType);
		String trimmed = value == null ? "" : value.trim();
		if (trimmed.isEmpty()) {
			return List.of();
		}
		String[] tokens = trimmed.split("[\\s,]+");
		List<Object> values = new ArrayList<>(tokens.length);
		for (String token : tokens) {
			if (!token.isEmpty()) {
				values.add(convert(token, itemType, null));
			}
		}
		return values;
	}

	private static ValueConverter converter(Class<?> type, Type genericType, boolean xmlList) {
		if (xmlList || Collection.class.isAssignableFrom(type)) {
			return new ValueConverter(type, genericType, ValueConverter.COLLECTION);
		}
		if (type.isArray()) {
			return new ValueConverter(type, genericType, ValueConverter.ARRAY);
		}
		if (type == String.class || type == CharSequence.class) {
			return new ValueConverter(type, genericType, ValueConverter.STRING);
		}
		if (type == boolean.class || type == Boolean.class) {
			return new ValueConverter(type, genericType, ValueConverter.BOOLEAN);
		}
		if (type == byte.class || type == Byte.class) {
			return new ValueConverter(type, genericType, ValueConverter.BYTE);
		}
		if (type == short.class || type == Short.class) {
			return new ValueConverter(type, genericType, ValueConverter.SHORT);
		}
		if (type == int.class || type == Integer.class) {
			return new ValueConverter(type, genericType, ValueConverter.INT);
		}
		if (type == long.class || type == Long.class) {
			return new ValueConverter(type, genericType, ValueConverter.LONG);
		}
		if (type == float.class || type == Float.class) {
			return new ValueConverter(type, genericType, ValueConverter.FLOAT);
		}
		if (type == double.class || type == Double.class) {
			return new ValueConverter(type, genericType, ValueConverter.DOUBLE);
		}
		if (type == char.class || type == Character.class) {
			return new ValueConverter(type, genericType, ValueConverter.CHAR);
		}
		if (type.isEnum()) {
			return new ValueConverter(type, genericType, ValueConverter.ENUM);
		}
		return isSimpleType(type) ? new ValueConverter(type, genericType, ValueConverter.OTHER) : null;
	}

	private static Object convert(String raw, Class<?> type, Type genericType) throws Exception {
		if (type == null || type == Object.class) {
			return raw;
		}
		if (Collection.class.isAssignableFrom(type)) {
			return newCollection(type, parseList(type, genericType, raw));
		}
		if (type.isArray()) {
			List<Object> values = parseList(type, genericType, raw);
			return toArray(values, type.getComponentType());
		}
		String value = normalize(raw, type);
		if (value == null) {
			return null;
		}
		if (type == String.class || type == CharSequence.class) {
			return value;
		}
		if (type == boolean.class || type == Boolean.class) {
			return parseBoolean(value);
		}
		if (type == byte.class || type == Byte.class) {
			return (byte) Integer.parseInt(value);
		}
		if (type == short.class || type == Short.class) {
			return (short) Integer.parseInt(value);
		}
		if (type == int.class || type == Integer.class) {
			return Integer.valueOf(value);
		}
		if (type == long.class || type == Long.class) {
			return Long.valueOf(value);
		}
		if (type == float.class || type == Float.class) {
			return Float.valueOf(value);
		}
		if (type == double.class || type == Double.class) {
			return Double.valueOf(value);
		}
		if (type == char.class || type == Character.class) {
			return value.charAt(0);
		}
		if (type == BigDecimal.class) {
			return new BigDecimal(value);
		}
		if (type == BigInteger.class) {
			return new BigInteger(value);
		}
		if (type == byte[].class) {
			return Base64.getDecoder().decode(value);
		}
		if (type == XMLGregorianCalendar.class) {
			return DATATYPE_FACTORY.newXMLGregorianCalendar(value);
		}
		if (type == Instant.class) {
			return Instant.parse(value);
		}
		if (type == LocalDate.class) {
			return LocalDate.parse(value);
		}
		if (type == LocalDateTime.class) {
			return LocalDateTime.parse(value);
		}
		if (type == OffsetDateTime.class) {
			return OffsetDateTime.parse(value);
		}
		if (type == ZonedDateTime.class) {
			return ZonedDateTime.parse(value);
		}
		if (type == Duration.class) {
			return Duration.parse(value);
		}
		if (type.isEnum()) {
			return parseEnum(type, value);
		}
		Method fromString = findStaticFactory(type, "fromValue", String.class);
		if (fromString != null) {
			return invokeFactory(fromString, value);
		}
		throw new IllegalArgumentException("Unsupported XML scalar type: " + type.getName());
	}

	private static Object parseEnum(Class<?> type, String value) throws Exception {
		return parseEnum(ENUM_METADATA.computeIfAbsent(type, StreamingXmlBinder::buildEnumMetadata), value);
	}

	private static Object parseEnum(EnumMetadata metadata, String value) throws Exception {
		Object exact = metadata.values().get(value);
		if (exact != null) {
			return exact;
		}
		String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
		Object normalizedValue = metadata.values().get(normalized);
		if (normalizedValue != null) {
			return normalizedValue;
		}
		if (metadata.fromValue() != null) {
			try {
				return invokeFactory(metadata.fromValue(), value);
			} catch (IllegalArgumentException unknownValue) {
				return null;
			}
		}
		return null;
	}

	private static EnumMetadata buildEnumMetadata(Class<?> type) {
		Map<String, Object> values = new HashMap<>();
		for (Object constant : type.getEnumConstants()) {
			String name = ((Enum<?>) constant).name();
			try {
				Field field = type.getDeclaredField(name);
				field.trySetAccessible();
				XmlEnumValue annotation = field.getAnnotation(XmlEnumValue.class);
				if (annotation != null) {
					values.putIfAbsent(annotation.value(), constant);
				}
			} catch (NoSuchFieldException ignored) {
				// Keep the binder resilient to synthetic enum implementations.
			}
		}
		for (Object constant : type.getEnumConstants()) {
			values.putIfAbsent(((Enum<?>) constant).name(), constant);
		}
		return new EnumMetadata(Map.copyOf(values), findStaticFactory(type, "fromValue", String.class));
	}

	private static Boolean parseBoolean(String value) {
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "true", "1", "yes" -> Boolean.TRUE;
			case "false", "0", "no" -> Boolean.FALSE;
			default -> throw new IllegalArgumentException("Invalid boolean value: " + value);
		};
	}

	private static String normalize(String raw, Class<?> type) {
		if (raw == null) {
			return null;
		}
		if (type == String.class || type == CharSequence.class) {
			return raw;
		}
		String value = raw.trim();
		return value.isEmpty() && type.isPrimitive() ? null : value;
	}

	private static Method findStaticFactory(Class<?> type, String name, Class<?> parameterType) {
		try {
			Method method = type.getDeclaredMethod(name, parameterType);
			if (Modifier.isStatic(method.getModifiers())) {
				method.trySetAccessible();
				return method;
			}
		} catch (NoSuchMethodException ignored) {
			// Fall through to the public lookup below.
		}
		try {
			Method method = type.getMethod(name, parameterType);
			return Modifier.isStatic(method.getModifiers()) ? method : null;
		} catch (NoSuchMethodException ignored) {
			return null;
		}
	}

	private static Object invokeFactory(Method factory, String value) throws Exception {
		try {
			return factory.invoke(null, value);
		} catch (InvocationTargetException e) {
			throw rethrow(e);
		}
	}

	private static Object instantiate(Class<?> type) throws Exception {
		if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
			throw new IllegalArgumentException("Cannot instantiate abstract XML type: " + type.getName());
		}
		Constructor<?> constructor = type.getDeclaredConstructor();
		constructor.trySetAccessible();
		try {
			return constructor.newInstance();
		} catch (InvocationTargetException e) {
			throw rethrow(e);
		}
	}

	private static Object instantiate(Class<?> type, TypeMetadata metadata) throws Exception {
		if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
			throw new IllegalArgumentException("Cannot instantiate abstract XML type: " + type.getName());
		}
		if (metadata.constructor() == null) {
			throw new IllegalArgumentException("XML type has no no-arg constructor: " + type.getName());
		}
		try {
			return metadata.constructor().newInstance();
		} catch (InvocationTargetException e) {
			throw rethrow(e);
		}
	}

	private static Constructor<?> findNoArgConstructor(Class<?> type) {
		if (type.isInterface() || type.isPrimitive() || Modifier.isAbstract(type.getModifiers())) {
			return null;
		}
		try {
			Constructor<?> constructor = type.getDeclaredConstructor();
			constructor.trySetAccessible();
			return constructor;
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("XML type has no no-arg constructor: " + type.getName(), e);
		}
	}

	private static void invokeLifecycle(Object instance, String methodName, Object parent) throws Exception {
		if (instance == null) {
			return;
		}
		for (Class<?> type = instance.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
			for (Method method : type.getDeclaredMethods()) {
				if (!method.getName().equals(methodName) || Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				method.trySetAccessible();
				try {
					if (method.getParameterCount() == 2) {
						method.invoke(instance, null, parent);
					} else if (method.getParameterCount() == 1) {
						method.invoke(instance, new Object[] { null });
					} else if (method.getParameterCount() == 0) {
						method.invoke(instance);
					} else {
						continue;
					}
					return;
				} catch (InvocationTargetException e) {
					throw rethrow(e);
				}
			}
		}
	}

	private static void invokeLifecycle(Object instance, Method method, Object parent) throws Exception {
		if (instance == null || method == null) {
			return;
		}
		try {
			if (method.getParameterCount() == 2) {
				method.invoke(instance, null, parent);
			} else if (method.getParameterCount() == 1) {
				method.invoke(instance, new Object[] { null });
			} else {
				method.invoke(instance);
			}
		} catch (InvocationTargetException e) {
			throw rethrow(e);
		}
	}

	private static Method findLifecycleMethod(Class<?> type, String methodName) {
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			for (Method method : current.getDeclaredMethods()) {
				if (!method.getName().equals(methodName) || Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				if (method.getParameterCount() <= 2) {
					method.trySetAccessible();
					return method;
				}
			}
		}
		return null;
	}

	private static RuntimeException rethrow(InvocationTargetException e) {
		Throwable cause = e.getCause();
		if (cause instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		if (cause instanceof Error error) {
			throw error;
		}
		return new IllegalStateException(cause);
	}

	private static IllegalStateException failure(String message, Throwable cause) {
		return new IllegalStateException(message + ": " + cause.getMessage(), cause);
	}

	private static TypeMetadata metadata(Class<?> type) {
		return METADATA.computeIfAbsent(type, StreamingXmlBinder::buildMetadata);
	}

	private static TypeMetadata buildMetadata(Class<?> type) {
		Map<String, AttributeMetadata> attributes = new ConcurrentHashMap<>();
		Map<String, ElementMetadata> elements = new ConcurrentHashMap<>();
		ValueMetadata value = null;
		XmlAccessType accessorType = findAccessorType(type);
		List<Class<?>> hierarchy = hierarchy(type);

		for (Class<?> current : hierarchy) {
			for (Field field : current.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()
					|| field.isAnnotationPresent(XmlTransient.class)) {
					continue;
				}
				field.trySetAccessible();
				ValueAccessor accessor = new FieldAccessor(field);
				boolean annotated = field.isAnnotationPresent(XmlAttribute.class)
					|| field.isAnnotationPresent(XmlElement.class)
					|| field.isAnnotationPresent(XmlElements.class)
					|| field.isAnnotationPresent(XmlValue.class);
				if (accessorType == XmlAccessType.NONE && !annotated) {
					continue;
				}
				boolean defaultField = accessorType == XmlAccessType.FIELD
					|| accessorType == XmlAccessType.PUBLIC_MEMBER && Modifier.isPublic(field.getModifiers());
				registerField(field, accessor, defaultField, attributes, elements);
				if (field.isAnnotationPresent(XmlValue.class)) {
					value = new ValueMetadata(accessor, converter(accessor.type(), accessor.genericType(),
						field.isAnnotationPresent(XmlList.class)));
				}
			}
			registerAnnotatedMethods(current, attributes, elements);
		}

		XmlRootElement root = type.getAnnotation(XmlRootElement.class);
		String rootName = root == null || DEFAULT_XML_NAME.equals(root.name())
			? null : root.name();
		return new TypeMetadata(type, rootName, Map.copyOf(attributes), Map.copyOf(elements), value,
			findNoArgConstructor(type), findLifecycleMethod(type, "beforeUnmarshal"),
			findLifecycleMethod(type, "afterUnmarshal"));
	}

	private static void registerField(Field field, ValueAccessor accessor, boolean defaultField,
			Map<String, AttributeMetadata> attributes, Map<String, ElementMetadata> elements) {
		AdapterMetadata adapter = adapterMetadata(field.getAnnotation(XmlJavaTypeAdapter.class));
		XmlAttribute attribute = field.getAnnotation(XmlAttribute.class);
		if (attribute != null) {
			String name = xmlName(attribute.name(), field.getName());
			attributes.put(name, new AttributeMetadata(name, accessor,
				converter(accessor.type(), accessor.genericType(), field.isAnnotationPresent(XmlList.class))));
		}
		XmlElements xmlElements = field.getAnnotation(XmlElements.class);
		if (xmlElements != null) {
			for (XmlElement element : xmlElements.value()) {
				registerElement(element.name(), element.type(), accessor, field.isAnnotationPresent(XmlList.class),
					adapter, elements);
			}
			return;
		}
		XmlElement element = field.getAnnotation(XmlElement.class);
		XmlElementWrapper wrapper = field.getAnnotation(XmlElementWrapper.class);
		if (wrapper != null) {
			String wrapperName = xmlName(wrapper.name(), field.getName());
			String itemName = element == null ? field.getName() : xmlName(element.name(), field.getName());
			Class<?> itemType = element == null || element.type() == XmlElement.DEFAULT.class
				? accessor.type() : element.type();
			registerElement(wrapperName, itemName, itemType, accessor, false, adapter, elements);
		} else if (element != null) {
			Class<?> type = element.type() == XmlElement.DEFAULT.class
				? accessor.type() : element.type();
			registerElement(xmlName(element.name(), field.getName()), type, accessor,
				field.isAnnotationPresent(XmlList.class), adapter, elements);
		} else if (defaultField && !field.isAnnotationPresent(XmlValue.class)) {
			registerElement(field.getName(), accessor.type(), accessor,
				field.isAnnotationPresent(XmlList.class), adapter, elements);
		}
	}

	private static void registerElement(String configuredName, Class<?> configuredType, ValueAccessor accessor,
		boolean xmlList, AdapterMetadata adapter, Map<String, ElementMetadata> elements) {
		registerElement(configuredName, null, configuredType, accessor, xmlList, adapter, elements);
	}

	private static void registerElement(String configuredName, String wrapperName, Class<?> configuredType,
		ValueAccessor accessor, boolean xmlList, AdapterMetadata adapter,
		Map<String, ElementMetadata> elements) {
		String name = xmlName(configuredName, accessor.name());
		Class<?> type = configuredType == XmlElement.DEFAULT.class ? accessor.type() : configuredType;
		Class<?> itemType = collectionItemTypeOrSelf(type, accessor.genericType());
		Type itemGenericType = itemType == type ? accessor.genericType() : itemType;
		AdapterMetadata resolvedAdapter = adapter == null ? adapterMetadata(itemType) : adapter;
		elements.put(name, new ElementMetadata(name, wrapperName, accessor, itemType, itemGenericType, xmlList,
			resolvedAdapter, converter(itemType, itemGenericType, xmlList)));
	}

	private static void registerAnnotatedMethods(Class<?> type,
			Map<String, AttributeMetadata> attributes, Map<String, ElementMetadata> elements) {
		for (Method method : type.getDeclaredMethods()) {
			if (Modifier.isStatic(method.getModifiers()) || method.isSynthetic() || method.isBridge()
				|| method.isAnnotationPresent(XmlTransient.class)) {
				continue;
			}
			XmlAttribute attribute = method.getAnnotation(XmlAttribute.class);
			if (attribute != null) {
				Method setter = method.getParameterCount() == 1 ? method : findPropertySetter(type, method);
				if (setter != null) {
					setter.trySetAccessible();
					ValueAccessor accessor = new MethodAccessor(propertyName(method), setter);
					String name = xmlName(attribute.name(), accessor.name());
					attributes.put(name, new AttributeMetadata(name, accessor,
						converter(accessor.type(), accessor.genericType(), method.isAnnotationPresent(XmlList.class))));
				}
				continue;
			}
			XmlElement element = method.getAnnotation(XmlElement.class);
			if (element != null) {
				Method setter = method.getParameterCount() == 1 ? method : findPropertySetter(type, method);
				if (setter != null) {
					setter.trySetAccessible();
					ValueAccessor accessor = new MethodAccessor(propertyName(method), setter);
					registerElement(element.name(), element.type(), accessor,
						method.isAnnotationPresent(XmlList.class),
						adapterMetadata(method.getAnnotation(XmlJavaTypeAdapter.class)), elements);
				}
			}
		}
	}

	private static Method findPropertySetter(Class<?> type, Method getter) {
		String property = propertyName(getter);
		String setterName = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			for (Method method : current.getDeclaredMethods()) {
				if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
					method.trySetAccessible();
					return method;
				}
			}
		}
		return null;
	}

	private static String propertyName(Method method) {
		String name = method.getName();
		if (name.startsWith("get") && name.length() > 3) {
			return decapitalize(name.substring(3));
		}
		if (name.startsWith("is") && name.length() > 2) {
			return decapitalize(name.substring(2));
		}
		return decapitalize(name);
	}

	private static String decapitalize(String value) {
		return value.isEmpty() ? value : Character.toLowerCase(value.charAt(0)) + value.substring(1);
	}

	private static XmlAccessType findAccessorType(Class<?> type) {
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			XmlAccessorType annotation = current.getDeclaredAnnotation(XmlAccessorType.class);
			if (annotation != null) {
				return annotation.value();
			}
		}
		return XmlAccessType.PUBLIC_MEMBER;
	}

	private static List<Class<?>> hierarchy(Class<?> type) {
		List<Class<?>> hierarchy = new ArrayList<>();
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			hierarchy.add(0, current);
		}
		return hierarchy;
	}

	private static String xmlName(String configured, String fallback) {
		return configured == null || configured.isEmpty() || DEFAULT_XML_NAME.equals(configured)
			? fallback : configured;
	}

	private static String localName(String name) {
		int colon = name.indexOf(':');
		return colon < 0 ? name : name.substring(colon + 1);
	}

	private static AdapterMetadata adapterMetadata(Class<?> type) {
		AdapterMetadata adapter = CLASS_ADAPTERS.computeIfAbsent(type, current -> {
			XmlJavaTypeAdapter annotation = current.getDeclaredAnnotation(XmlJavaTypeAdapter.class);
			return annotation == null ? NO_ADAPTER : adapterMetadata(annotation);
		});
		return adapter == NO_ADAPTER ? null : adapter;
	}

	private static AdapterMetadata adapterMetadata(XmlJavaTypeAdapter annotation) {
		if (annotation == null) {
			return null;
		}
		Class<?> boundType = adapterBoundType(annotation.value(), annotation.value());
		return new AdapterMetadata(annotation.value(), boundType);
	}

	private static Class<?> adapterBoundType(Class<?> adapterType, Class<?> fallback) {
		Class<?> current = adapterType;
		while (current != null && current != Object.class) {
			Type generic = current.getGenericSuperclass();
			if (generic instanceof ParameterizedType parameterized
				&& parameterized.getRawType() == XmlAdapter.class) {
				Type bound = parameterized.getActualTypeArguments()[0];
				if (bound instanceof Class<?> boundClass) {
					return boundClass;
				}
			}
			current = current.getSuperclass();
		}
		for (Method method : adapterType.getMethods()) {
			if (method.getName().equals("unmarshal") && method.getParameterCount() == 1) {
				return method.getParameterTypes()[0];
			}
		}
		throw new IllegalArgumentException("Cannot resolve adapter value type for " + fallback.getName());
	}

	private static boolean isSimpleType(Class<?> type) {
		return type.isPrimitive() || type.isEnum() || type == String.class || type == CharSequence.class
			|| Number.class.isAssignableFrom(type) || type == Boolean.class || type == Character.class
			|| type == XMLGregorianCalendar.class || type == Instant.class || type == LocalDate.class
			|| type == LocalDateTime.class || type == OffsetDateTime.class || type == ZonedDateTime.class
			|| type == Duration.class || type == byte[].class;
	}

	private static Class<?> collectionItemType(Class<?> type, Type genericType) {
		if (type.isArray()) {
			return type.getComponentType();
		}
		if (!Collection.class.isAssignableFrom(type)) {
			return type;
		}
		if (genericType instanceof ParameterizedType parameterized) {
			Type argument = parameterized.getActualTypeArguments()[0];
			if (argument instanceof Class<?> argumentClass) {
				return argumentClass;
			}
			if (argument instanceof GenericArrayType arrayType
				&& arrayType.getGenericComponentType() instanceof Class<?> componentType) {
				return Array.newInstance(componentType, 0).getClass();
			}
		}
		return String.class;
	}

	private static Class<?> collectionItemTypeOrSelf(Class<?> type, Type genericType) {
		if (Collection.class.isAssignableFrom(type) || type.isArray()) {
			return collectionItemType(type, genericType);
		}
		return type;
	}

	private static Object newCollection(Class<?> requestedType, List<Object> values) {
		if (requestedType.isAssignableFrom(ArrayList.class) || requestedType == Collection.class
			|| requestedType == List.class) {
			return new ArrayList<>(values);
		}
		if (requestedType.isAssignableFrom(LinkedHashSet.class) || requestedType == Set.class) {
			return new LinkedHashSet<>(values);
		}
		throw new IllegalArgumentException("Unsupported XML collection type: " + requestedType.getName());
	}

	private static Object toArray(List<Object> values, Class<?> componentType) {
		Object array = Array.newInstance(componentType, values.size());
		for (int index = 0; index < values.size(); index++) {
			Array.set(array, index, values.get(index));
		}
		return array;
	}

	private static DatatypeFactory createDatatypeFactory() {
		try {
			return DatatypeFactory.newInstance();
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private record TypeMetadata(Class<?> type, String rootName, Map<String, AttributeMetadata> attributes,
			Map<String, ElementMetadata> elements, ValueMetadata value, Constructor<?> constructor,
			Method beforeUnmarshal, Method afterUnmarshal) {
	}

	private record AttributeMetadata(String xmlName, ValueAccessor accessor, ValueConverter converter) {
	}

	private record ElementMetadata(String xmlName, String wrapperName, ValueAccessor accessor, Class<?> type,
			Type genericType, boolean xmlList, AdapterMetadata adapter, ValueConverter converter) {
	}

	private record ValueMetadata(ValueAccessor accessor, ValueConverter converter) {
	}

	private record AdapterMetadata(Class<?> adapterType, Class<?> boundType) {
	}

	private record EnumMetadata(Map<String, Object> values, Method fromValue) {
	}

	private static final class ValueConverter {
		private static final int COLLECTION = 0;
		private static final int ARRAY = 1;
		private static final int STRING = 2;
		private static final int BOOLEAN = 3;
		private static final int BYTE = 4;
		private static final int SHORT = 5;
		private static final int INT = 6;
		private static final int LONG = 7;
		private static final int FLOAT = 8;
		private static final int DOUBLE = 9;
		private static final int CHAR = 10;
		private static final int ENUM = 11;
		private static final int OTHER = 12;

		private final Class<?> type;
		private final Type genericType;
		private final int kind;
		private final EnumMetadata enumMetadata;

		private ValueConverter(Class<?> type, Type genericType, int kind) {
			this.type = type;
			this.genericType = genericType;
			this.kind = kind;
			this.enumMetadata = kind == ENUM
				? ENUM_METADATA.computeIfAbsent(type, StreamingXmlBinder::buildEnumMetadata) : null;
		}

		private Object convert(String raw) throws Exception {
			if (kind == STRING) {
				return raw;
			}
			if (kind >= BOOLEAN) {
				String value = normalize(raw, type);
				if (value == null) {
					return null;
				}
				return switch (kind) {
					case BOOLEAN -> parseBoolean(value);
					case BYTE -> (byte) Integer.parseInt(value);
					case SHORT -> (short) Integer.parseInt(value);
					case INT -> Integer.valueOf(value);
					case LONG -> Long.valueOf(value);
					case FLOAT -> Float.valueOf(value);
					case DOUBLE -> Double.valueOf(value);
					case CHAR -> value.charAt(0);
					case ENUM -> parseEnum(enumMetadata, value);
					case OTHER -> StreamingXmlBinder.convert(value, type, genericType);
					default -> throw new IllegalStateException("Unknown XML converter kind: " + kind);
				};
			}
			return switch (kind) {
				case COLLECTION -> newCollection(type, parseList(type, genericType, raw));
				case ARRAY -> toArray(parseList(type, genericType, raw), type.getComponentType());
				default -> throw new IllegalStateException("Unknown XML converter kind: " + kind);
			};
		}
	}

	private interface ValueAccessor {
		String name();

		Class<?> type();

		Type genericType();

		boolean isCollection();

		boolean isArray();

		Object get(Object target) throws IllegalAccessException;

		void set(Object target, Object value) throws IllegalAccessException;

		default void write(Object target, Object value) throws IllegalAccessException {
			if (isCollection()) {
				Collection<Object> collection = ensureCollection(target);
				if (value instanceof Collection<?> values) {
					collection.addAll(values);
				} else if (value != null) {
					collection.add(value);
				}
				return;
			}
			if (isArray()) {
				appendArray(target, value);
				return;
			}
			set(target, value);
		}

		default void writeAll(Object target, Collection<?> values) throws IllegalAccessException {
			if (isCollection()) {
				ensureCollection(target).addAll(values);
				return;
			}
			if (isArray()) {
				for (Object value : values) {
					appendArray(target, value);
				}
				return;
			}
			throw new IllegalArgumentException("Expected a collection or array for " + name());
		}

		@SuppressWarnings("unchecked")
		default Collection<Object> ensureCollection(Object target) throws IllegalAccessException {
			Object current = get(target);
			if (current == null) {
				Collection<Object> created = Set.class.isAssignableFrom(type())
					? new LinkedHashSet<>() : new ArrayList<>();
				set(target, created);
				return created;
			}
			return (Collection<Object>) current;
		}

		default void appendArray(Object target, Object value) throws IllegalAccessException {
			Object current = get(target);
			int currentLength = current == null ? 0 : Array.getLength(current);
			Class<?> componentType = type().getComponentType();
			Object expanded = Array.newInstance(componentType, currentLength + (value instanceof Collection<?> c
				? c.size() : 1));
			if (current != null) {
				System.arraycopy(current, 0, expanded, 0, currentLength);
			}
			if (value instanceof Collection<?> values) {
				int index = currentLength;
				for (Object item : values) {
					Array.set(expanded, index++, item);
				}
			} else if (value != null) {
				Array.set(expanded, currentLength, value);
			}
			set(target, expanded);
		}
	}

	private static final class FieldAccessor implements ValueAccessor {
		private final Field field;
		private final Class<?> type;
		private final Type genericType;
		private final boolean collection;
		private final boolean array;

		private FieldAccessor(Field field) {
			this.field = field;
			this.type = field.getType();
			this.genericType = field.getGenericType();
			this.collection = Collection.class.isAssignableFrom(type);
			this.array = type.isArray();
		}

		@Override
		public String name() {
			return field.getName();
		}

		@Override
		public Class<?> type() {
			return type;
		}

		@Override
		public Type genericType() {
			return genericType;
		}

		@Override
		public boolean isCollection() {
			return collection;
		}

		@Override
		public boolean isArray() {
			return array;
		}

		@Override
		public Object get(Object target) throws IllegalAccessException {
			return field.get(target);
		}

		@Override
		public void set(Object target, Object value) throws IllegalAccessException {
			field.set(target, value);
		}
	}

	private static final class MethodAccessor implements ValueAccessor {
		private final String name;
		private final Method setter;
		private final Class<?> type;
		private final Type genericType;
		private final boolean collection;
		private final boolean array;

		private MethodAccessor(String name, Method setter) {
			this.name = name;
			this.setter = setter;
			this.type = setter.getParameterTypes()[0];
			this.genericType = setter.getGenericParameterTypes()[0];
			this.collection = Collection.class.isAssignableFrom(type);
			this.array = type.isArray();
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public Class<?> type() {
			return type;
		}

		@Override
		public Type genericType() {
			return genericType;
		}

		@Override
		public boolean isCollection() {
			return collection;
		}

		@Override
		public boolean isArray() {
			return array;
		}

		@Override
		public Object get(Object target) {
			return null;
		}

		@Override
		public void set(Object target, Object value) {
			try {
				setter.invoke(target, value);
			} catch (InvocationTargetException e) {
				throw rethrow(e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Cannot set XML property " + name, e);
			}
		}
	}
}

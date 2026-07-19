package com.aionemu.gameserver.model.instance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InstanceRuntimeState {
	private static final String EMPTY = "{\"version\":1,\"data\":\"\"}";
	private final Map<String, String> values = new LinkedHashMap<>();
	private Runnable changeListener = () -> { };

	public static InstanceRuntimeState decode(String json) {
		InstanceRuntimeState state = new InstanceRuntimeState();
		if (json == null || json.isBlank() || EMPTY.equals(json)) {
			return state;
		}
		int marker = json.indexOf("\"data\":\"");
		if (marker < 0) {
			throw new IllegalStateException("Invalid instance runtime state JSON");
		}
		int start = marker + 8;
		int end = json.indexOf('"', start);
		if (end < 0) {
			throw new IllegalStateException("Invalid instance runtime state payload");
		}
		byte[] bytes = Base64.getDecoder().decode(json.substring(start, end));
		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
			int size = input.readInt();
			for (int i = 0; i < size; i++) {
				state.values.put(read(input), read(input));
			}
			if (input.available() != 0) {
				throw new IllegalStateException("Trailing instance runtime state bytes");
			}
			return state;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to decode instance runtime state", e);
		}
	}

	public synchronized String encode() {
		if (values.isEmpty()) {
			return EMPTY;
		}
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(bytes)) {
				output.writeInt(values.size());
				for (Map.Entry<String, String> entry : values.entrySet()) {
					write(output, entry.getKey());
					write(output, entry.getValue());
				}
			}
			return "{\"version\":1,\"data\":\"" + Base64.getEncoder().encodeToString(bytes.toByteArray()) + "\"}";
		} catch (IOException e) {
			throw new IllegalStateException("Failed to encode instance runtime state", e);
		}
	}

	public synchronized void onChange(Runnable listener) {
		changeListener = listener == null ? () -> { } : listener;
	}

	public synchronized String get(String key) {
		return values.get(key);
	}

	public synchronized String get(String key, String defaultValue) {
		return values.getOrDefault(key, defaultValue);
	}

	public synchronized int getInt(String key, int defaultValue) {
		String value = values.get(key);
		return value == null ? defaultValue : Integer.parseInt(value);
	}

	public synchronized long getLong(String key, long defaultValue) {
		String value = values.get(key);
		return value == null ? defaultValue : Long.parseLong(value);
	}

	public synchronized boolean getBoolean(String key, boolean defaultValue) {
		String value = values.get(key);
		return value == null ? defaultValue : Boolean.parseBoolean(value);
	}

	public void put(String key, Object value) {
		Runnable listener;
		synchronized (this) {
			String text = String.valueOf(value);
			if (text.equals(values.put(key, text))) {
				return;
			}
			listener = changeListener;
		}
		listener.run();
	}

	public void remove(String key) {
		Runnable listener;
		synchronized (this) {
			if (values.remove(key) == null) {
				return;
			}
			listener = changeListener;
		}
		listener.run();
	}

	public synchronized Map<String, String> snapshot() {
		return new LinkedHashMap<>(values);
	}

	public synchronized Map<String, String> snapshot(String prefix) {
		Map<String, String> result = new LinkedHashMap<>();
		values.forEach((key, value) -> {
			if (key.startsWith(prefix)) {
				result.put(key, value);
			}
		});
		return result;
	}

	public void removePrefix(String prefix) {
		Runnable listener;
		synchronized (this) {
			if (!values.keySet().removeIf(key -> key.startsWith(prefix))) {
				return;
			}
			listener = changeListener;
		}
		listener.run();
	}

	private static void write(DataOutputStream output, String value) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		output.writeInt(bytes.length);
		output.write(bytes);
	}

	private static String read(DataInputStream input) throws IOException {
		int length = input.readInt();
		if (length < 0 || length > 16 * 1024 * 1024) {
			throw new IOException("Invalid runtime state string length " + length);
		}
		return new String(input.readNBytes(length), StandardCharsets.UTF_8);
	}
}

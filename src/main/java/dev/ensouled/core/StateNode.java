package dev.ensouled.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Small neutral tree used to persist a mind without letting the cognition core depend on any
 * Minecraft class. The body adapter maps this tree to NBT and back.
 */
public final class StateNode {
	private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

	public StateNode putString(String key, String value) {
		values.put(key, value == null ? "" : value);
		return this;
	}

	public StateNode putInt(String key, int value) {
		values.put(key, value);
		return this;
	}

	public StateNode putLong(String key, long value) {
		values.put(key, value);
		return this;
	}

	public StateNode putBoolean(String key, boolean value) {
		values.put(key, value);
		return this;
	}

	public StateNode putNode(String key, StateNode node) {
		values.put(key, node);
		return this;
	}

	public StateNode putNodeList(String key, List<StateNode> nodes) {
		values.put(key, new ArrayList<>(nodes));
		return this;
	}

	public StateNode putStringList(String key, List<String> list) {
		values.put(key, new ArrayList<>(list));
		return this;
	}

	public String string(String key, String fallback) {
		Object v = values.get(key);
		return v instanceof String s ? s : fallback;
	}

	public int integer(String key, int fallback) {
		Object v = values.get(key);
		return v instanceof Integer i ? i : fallback;
	}

	public long lng(String key, long fallback) {
		Object v = values.get(key);
		return v instanceof Long l ? l : fallback;
	}

	public boolean bool(String key, boolean fallback) {
		Object v = values.get(key);
		return v instanceof Boolean b ? b : fallback;
	}

	public StateNode node(String key) {
		Object v = values.get(key);
		return v instanceof StateNode n ? n : new StateNode();
	}

	@SuppressWarnings("unchecked")
	public List<StateNode> nodes(String key) {
		Object v = values.get(key);
		return v instanceof List<?> list ? new ArrayList<>((List<StateNode>) list) : List.of();
	}

	@SuppressWarnings("unchecked")
	public List<String> strings(String key) {
		Object v = values.get(key);
		return v instanceof List<?> list ? new ArrayList<>((List<String>) list) : List.of();
	}

	public Set<String> keys() {
		return values.keySet();
	}

	public Map<String, Object> raw() {
		return values;
	}
}

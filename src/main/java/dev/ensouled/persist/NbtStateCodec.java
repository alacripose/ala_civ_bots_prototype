package dev.ensouled.persist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.ensouled.core.StateNode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

/** Maps the cognition core's neutral tree to NBT so the core stays free of Minecraft types. */
public final class NbtStateCodec {
	private NbtStateCodec() {
	}

	public static NbtCompound toNbt(StateNode node) {
		NbtCompound nbt = new NbtCompound();
		for (Map.Entry<String, Object> entry : node.raw().entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof String s) {
				nbt.putString(key, s);
			} else if (value instanceof Integer i) {
				nbt.putInt(key, i);
			} else if (value instanceof Long l) {
				nbt.putLong(key, l);
			} else if (value instanceof Boolean b) {
				nbt.putBoolean(key, b);
			} else if (value instanceof StateNode child) {
				nbt.put(key, toNbt(child));
			} else if (value instanceof List<?> list) {
				NbtList nbtList = new NbtList();
				for (Object element : list) {
					if (element instanceof StateNode child) {
						nbtList.add(toNbt(child));
					} else {
						nbtList.add(NbtString.of(String.valueOf(element)));
					}
				}
				nbt.put(key, nbtList);
			}
		}
		return nbt;
	}

	public static StateNode fromNbt(NbtCompound nbt) {
		StateNode node = new StateNode();
		for (String key : nbt.getKeys()) {
			NbtElement element = nbt.get(key);
			if (element instanceof NbtString s) {
				node.putString(key, s.asString());
			} else if (element instanceof net.minecraft.nbt.NbtInt i) {
				node.putInt(key, i.intValue());
			} else if (element instanceof net.minecraft.nbt.NbtLong l) {
				node.putLong(key, l.longValue());
			} else if (element instanceof net.minecraft.nbt.NbtByte b) {
				node.putBoolean(key, b.byteValue() != 0);
			} else if (element instanceof NbtCompound compound) {
				node.putNode(key, fromNbt(compound));
			} else if (element instanceof NbtList list) {
				if (!list.isEmpty() && list.get(0) instanceof NbtCompound) {
					List<StateNode> children = new ArrayList<>();
					for (int i = 0; i < list.size(); i++) {
						if (list.get(i) instanceof NbtCompound child) {
							children.add(fromNbt(child));
						}
					}
					node.putNodeList(key, children);
				} else {
					List<String> values = new ArrayList<>();
					for (int i = 0; i < list.size(); i++) {
						values.add(list.getString(i));
					}
					node.putStringList(key, values);
				}
			}
		}
		return node;
	}
}

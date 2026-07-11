package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 宏列表。
 * Macro list.
 *
 * @author Aquanox, nrg
 */
@Slf4j
public class MacroList {

	/**
	 * 宏容器：位置到 XML 的映射。
	 * Container of macros: position to XML.
	 */
	private final Map<Integer, String> macrosses;

	/**
	 * 创建空宏列表。
	 * Creates an empty macro list.
	 */
	public MacroList() {
		this.macrosses = new HashMap<Integer, String>(12);
	}

	/**
	 * 用已有映射创建宏列表。
	 * Creates a macro list from an existing map.
	 *
	 * @param arg 位置到宏 XML 的映射 / map of position to macro XML
	 */
	public MacroList(Map<Integer, String> arg) {
		this.macrosses = arg;
	}

	/**
	 * 返回全部宏的不可修改映射。
	 * Returns an unmodifiable map of all macros.
	 *
	 * @return 全部宏 / all macros
	 */
	public Map<Integer, String> getMacrosses() {
		return Collections.unmodifiableMap(macrosses);
	}

	/**
	 * 向集合添加宏。
	 * Adds a macro to the collection.
	 *
	 * @param macroPosition 宏槽位 / macro slot
	 * @param macroXML 宏 XML 内容 / macro XML contents
	 * @return 新增成功且可入库则为 true；覆盖已有槽位则为 false
	 *         / true if newly added and storable; false if an existing slot was replaced
	 */
	public synchronized boolean addMacro(int macroPosition, String macroXML) {
		if (macrosses.containsKey(macroPosition)) {
			macrosses.remove(macroPosition);
			macrosses.put(macroPosition, macroXML);
			return false;
		}
		macrosses.put(macroPosition, macroXML);
		return true;
	}

	/**
	 * 从列表移除宏。
	 * Removes a macro from the list.
	 *
	 * @param macroPosition 宏槽位 / macro slot
	 * @return 删除成功则为 true / true if deletion succeeded
	 */
	public synchronized boolean removeMacro(int macroPosition) {
		String m = macrosses.remove(macroPosition);
		if (m == null)//
		{
			log.warn(I18n.get("log.78191e395095"));
			return false;
		}
		return true;
	}

	/**
	 * 返回可用宏数量。
	 * Returns the number of available macros.
	 *
	 * @return 宏数量 / macro count
	 */
	public int getSize() {
		return macrosses.size();
	}

	/**
	 * 返回不可修改的宏 ID→内容映射片段。
	 * 注意：零售端每包最多发 6 个宏，并保留原始槽位号。
	 * Returns an unmodifiable map of macro id to macro contents.
	 * NOTE: Retail sends at most 6 macros per packet, retaining their original slot numbers.
	 *
	 * @param packet 分包序号（1–4） / packet part index (1–4)
	 * @return 该包内的宏片段 / macros for this packet part
	 */
	public Map<Integer, String> getMarcosPart(int packet) {
		Map<Integer, String> macrosPart = new LinkedHashMap<Integer, String>();
		int currentIndex;
		int endIndex;
		if (packet == 1) {
			currentIndex = 1;
			endIndex = 6;
		} else if (packet == 2) {
			currentIndex = 7;
			endIndex = 12;
		} else if (packet == 3) {
			currentIndex = 13;
			endIndex = 18;
		} else { // packet == 4
			currentIndex = 19;
			endIndex = 24;
		}

		for (; currentIndex <= endIndex; currentIndex++) {
			if (macrosses.containsKey(currentIndex)) {
				macrosPart.put(currentIndex, macrosses.get(currentIndex));
			}
		}
		return Collections.unmodifiableMap(macrosPart);
	}
}

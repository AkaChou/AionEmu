package com.aionemu.gameserver.model.templates.zone;

import com.aionemu.gameserver.model.geometry.Area;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 区域信息模板（静态数据/XML）。
 * XML template.
 * @author MrPoke
 */
@Getter
@AllArgsConstructor
public class ZoneInfo {
	private final Area area;
	private final ZoneTemplate zoneTemplate;
}

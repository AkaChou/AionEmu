package com.aionemu.gameserver.model.templates.zone;

import com.aionemu.gameserver.model.geometry.Area;

/**
 * 区域信息模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
public record ZoneInfo(Area area, ZoneTemplate zoneTemplate) {
}

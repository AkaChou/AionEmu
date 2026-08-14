package com.aionemu.gameserver.model.templates.survey;

/**
 * 调查问卷物品模板（静态数据/XML）。
 * Survey item template (static data / XML).
 *
 * @author KID
 */
public class SurveyItem {

	/** 问卷拥有者 ID / owner id */
	public int ownerId;
	/** 唯一 ID / unique id */
	public int uniqueId;
	/** 物品 ID / item id */
	public int itemId;
	/** 数量 / count */
	public long count;
	/** 问卷 HTML 与单选键 / survey html and radio keys */
	public String html, radio;

}

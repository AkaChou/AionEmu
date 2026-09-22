package com.aionemu.gameserver.model.templates.anoha;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 阿诺哈模板（静态数据/XML）。
 * Anoha template (static data/XML).
 * @author Rinzler (Encom)
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Anoha")
public class AnohaTemplate {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	protected int id;
}

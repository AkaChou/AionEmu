package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.TitleTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 玩家称号数据容器，按称号 ID 索引称号模板。
 * Player title data holder, indexing title templates by title id.
 *
 * @author xavier
 */
@XmlRootElement(name = "player_titles")
@XmlAccessorType(XmlAccessType.FIELD)
public class TitleData {

	@XmlElement(name = "title")
	private List<TitleTemplate> tts;

	private IntObjectHashMap<TitleTemplate> titles;

	/**
	 * JAXB 反序列化完成后，按称号 ID 索引模板并释放原始列表。
	 * After JAXB unmarshalling, indexes templates by title id and clears the source list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		titles = new IntObjectHashMap<TitleTemplate>();
		for (TitleTemplate tt : tts) {
			titles.put(tt.getTitleId(), tt);
		}
		tts = null;
	}

	/**
	 * 按称号 ID 获取称号模板。
	 * Returns the title template for the given title id.
	 *
	 * title id
	 *
	 * @param titleId @return 称号模板，不存在则为 null / title template or null
	 */
	public TitleTemplate getTitleTemplate(int titleId) {
		return titles.get(titleId);
	}

	/**
	 * 返回已加载的称号数量。
	 * Returns the number of loaded titles.
	 *
	 * template count
	 */
	public int size() {
		return titles.size();
	}
}

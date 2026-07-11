package com.aionemu.gameserver.configs.ingameshop;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.commons.utils.xml.JAXBUtil;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.templates.ingameshop.IGCategory;

/**
 * 游戏内商城分类属性（由 in_game_shop.xml 加载）。
 * In-game shop category properties loaded from in_game_shop.xml.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "in_game_shop")
public class InGameShopProperty {

	/**
	 * 商城分类列表。
	 * In-game shop category list.
	 */
	@XmlElement(name = "category", required = true)
	private List<IGCategory> categories;

	/**
	 * 获取商城分类列表（懒初始化）。
	 * Returns the category list (lazily initialized).
	 */
	public List<IGCategory> getCategories() {
		if (categories == null) {
			categories = new ArrayList<IGCategory>();
		}
		return categories;
	}

	/**
	 * 返回分类数量。
	 * Returns the number of categories.
	 */
	public int size() {
		return getCategories().size();
	}

	/**
	 * 清空已加载的分类。
	 * Clears loaded categories.
	 */
	public void clear() {
		if (categories != null) {
			categories.clear();
		}
	}

	/**
	 * 从配置文件加载游戏内商城属性。
	 * Loads in-game shop properties from the config file.
	 */
	public static InGameShopProperty load() {
		InGameShopProperty ing = null;
		try {
			String xml = Files.readString(Config.configFile("ingameshop/in_game_shop.xml").toPath(), StandardCharsets.UTF_8);
			ing = (InGameShopProperty) JAXBUtil.deserialize(xml, InGameShopProperty.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize ingameshop", e);
		}
		return ing;
	}
}

package com.aionemu.webshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 网页商城配置（前缀 {@code webshop}）。
 * Web shop configuration (prefix {@code webshop}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "webshop")
public class WebShopProperties {

	/**
	 * 物品图标外置目录（aion-shop 的 icons_webp 产物）。
	 * External directory holding item icons (the icons_webp output of aion-shop).
	 */
	private String iconDir = "aion/web/icons";

	/**
	 * 图标在页面上的 URL 前缀，必须与资源映射一致。
	 * URL prefix of icons on pages; must match the resource mapping.
	 */
	private String iconUrlPrefix = "/icons";
}

package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * HTML 欢迎页与缓存相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * HTML welcome and cache related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class HTMLConfig {
	/**
	 * 是否启用欢迎 HTML。
	 * Whether welcome HTML is enabled.
	 */
	@Property(key = "gameserver.html.welcome.enable", defaultValue = "false")
	public static boolean ENABLE_HTML_WELCOME;
	/**
	 * 是否启用新手指引 HTML。
	 * Whether guide HTML pages are enabled.
	 */
	@Property(key = "gameserver.html.guides.enable", defaultValue = "true")
	public static boolean ENABLE_GUIDES;
	/**
	 * HTML 静态资源根目录。
	 * HTML static resources root directory.
	 */
	@Property(key = "gameserver.html.root", defaultValue = "./data/static_data/HTML/")
	public static String HTML_ROOT;
	/**
	 * HTML 文件编码。
	 * HTML file encoding.
	 */
	@Property(key = "gameserver.html.encoding", defaultValue = "UTF-8")
	public static String HTML_ENCODING;

	@Getter
	private final HtmlProperties html = new HtmlProperties();

	@Getter
	public static class HtmlProperties {
		private final WelcomeProperties welcome = new WelcomeProperties();
		private final GuidesProperties guides = new GuidesProperties();

		public String getRoot() {
			return HTML_ROOT;
		}

		public void setRoot(String root) {
			HTML_ROOT = root;
		}

		public String getEncoding() {
			return HTML_ENCODING;
		}

		public void setEncoding(String encoding) {
			HTML_ENCODING = encoding;
		}

		public static class WelcomeProperties {
			public boolean isEnable() {
				return ENABLE_HTML_WELCOME;
			}

			public void setEnable(boolean enable) {
				ENABLE_HTML_WELCOME = enable;
			}
		}

		public static class GuidesProperties {
			public boolean isEnable() {
				return ENABLE_GUIDES;
			}

			public void setEnable(boolean enable) {
				ENABLE_GUIDES = enable;
			}
		}
	}
}

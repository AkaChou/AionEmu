package com.aionemu.boot.web.config;

/**
 * 配置键的备注：配置文件里键上方注释块解析出的双语说明与默认值。
 * The comment of a configuration key: the bilingual description and the default value parsed out of the
 * comment block above the key in the configuration file.
 * <p>
 * 配置文件里的注释块形如 {@code # GM 面板 / GM Panel} 与 {@code # 默认值: 3 / Default: 3}：
 * 前者是键的说明（中文在前，英文在后），后者是该键的默认值。
 * <p>
 * The comment blocks in the configuration files look like {@code # GM 面板 / GM Panel} and
 * {@code # 默认值: 3 / Default: 3}: the former describes the key (Chinese first, English after the slash), the
 * latter carries its default value.
 *
 * @param description 键的中文说明；没有则为 {@code null} / the Chinese description, {@code null} when absent
 * @param descriptionEn 键的英文说明；没有则为 {@code null} / the English description, {@code null} when absent
 * @param defaultValue 键的默认值；没有则为 {@code null} / the default value, {@code null} when absent
 */
public record ConfigKeyComment(String description, String descriptionEn, String defaultValue) {

	/**
	 * 是否带有任何说明信息。
	 * Whether any description is present.
	 * @return 有说明则为 true / true when a description is present
	 */
	public boolean isPresent() {
		return description != null || descriptionEn != null || defaultValue != null;
	}
}

package com.aionemu.loginserver.configs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.aionemu.commons.configuration.Property;

/**
 * 独立账号 VIP 配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * <p>Binder 路径经 {@code @Validated} + JSR-380 约束在绑定期声明式校验；
 * 遗留装配路径（{@code Config.load()} 直写静态字段）不经 Binder，仍由
 * {@link #validate()} 及早失败守护。</p>
 * Independent account VIP configuration supporting Spring Boot ConfigurationProperties and static
 * facade access. The Binder path is validated declaratively at binding time via {@code @Validated}
 * plus JSR-380 constraints; the legacy assembly path ({@code Config.load()} writing static fields
 * directly) bypasses the Binder and stays guarded by {@link #validate()} fail-fast.
 */
@Getter
@Validated
@Component
@ConfigurationProperties(prefix = "loginserver.vip")
public class VipConfig {

	/**
	 * 是否自动启用账号 VIP。 / Whether to auto-enable account VIP.
	 */
	@Property(key = "loginserver.vip.auto-enable", defaultValue = "false")
	public static boolean AUTO_ENABLE;

	/**
	 * 自动启用 VIP 的等级。 / Level granted when auto-enabling VIP.
	 */
	@Property(key = "loginserver.vip.auto-enable.level", defaultValue = "1")
	public static int AUTO_ENABLE_LEVEL;

	@Valid
	private final AutoEnableProperties autoEnable = new AutoEnableProperties();

	/**
	 * no-op：绑定值经 {@code valueOf} 或嵌套属性直接写入静态字段，防止重复绑定覆盖。
	 * 本方法缺失会让 JavaBeanBinder 无法下沉 {@code loginserver.vip.auto-enable.*}，
	 * 直接抛 BindException（AionBootApplicationTest 钉住），严禁作为死代码删除。
	 * no-op: bound values reach the static fields via {@code valueOf} or the nested properties,
	 * so re-binding must not overwrite them. Without it JavaBeanBinder cannot descend into
	 * {@code loginserver.vip.auto-enable.*} and binding throws BindException (pinned by
	 * AionBootApplicationTest) — never remove it as dead code.
	 *
	 * @param autoEnable 绑定产生的嵌套实例 / nested instance produced by binding
	 */
	public void setAutoEnable(AutoEnableProperties autoEnable) {
	}

	/**
	 * 校验配置值范围（VIP 等级）。
	 * Validates configuration value ranges (VIP level).
	 */
	public static void validate() {
		if (AUTO_ENABLE && (AUTO_ENABLE_LEVEL < 1 || AUTO_ENABLE_LEVEL > 6)) {
			throw new IllegalArgumentException("loginserver.vip.auto-enable.level must be between 1 and 6");
		}
	}

	/**
	 * 「标量+子键」共存节点：{@code value} 承载规范化后的布尔标量，{@code level} 承载子键。
	 * Scalar-plus-sub-key node: {@code value} carries the normalized boolean scalar, {@code level} the sub-key.
	 */
	public static class AutoEnableProperties {

		/**
		 * 裸键值形态兼容入口：直接解析布尔并写入静态字段。
		 * Bare-key value-shape entry: parses the boolean and writes the static field directly.
		 *
		 * @param value 裸键布尔值 / bare-key boolean value
		 * @return 嵌套实例 / nested instance
		 */
		public static AutoEnableProperties valueOf(String value) {
			AUTO_ENABLE = Boolean.parseBoolean(value);
			return new AutoEnableProperties();
		}

		public boolean isValue() {
			return AUTO_ENABLE;
		}

		public void setValue(boolean value) {
			AUTO_ENABLE = value;
		}

		/**
		 * 越界绑定在绑定期即被 JSR-380 拒绝（遗留直写路径由 {@link VipConfig#validate()} 守护）。
		 * Out-of-range bindings are rejected declaratively by JSR-380 at binding time (the legacy
		 * direct-write path stays guarded by {@link VipConfig#validate()}).
		 */
		@Min(1)
		@Max(6)
		public int getLevel() {
			return AUTO_ENABLE_LEVEL;
		}

		public void setLevel(int level) {
			AUTO_ENABLE_LEVEL = level;
		}
	}
}

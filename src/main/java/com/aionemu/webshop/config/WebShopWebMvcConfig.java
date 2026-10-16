package com.aionemu.webshop.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.webshop.auth.AdminOnlyInterceptor;
import com.aionemu.webshop.auth.SignInRequiredInterceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * 网页商城的 Spring MVC 装配：图标外置目录的资源映射与接口鉴权拦截器。
 * Spring MVC wiring of the web shop: the resource mapping of the external icon directory and the
 * authorisation interceptors of the API.
 */
@Slf4j
@Configuration
public class WebShopWebMvcConfig implements WebMvcConfigurer {

	private final WebShopProperties properties;

	private final AdminOnlyInterceptor adminOnlyInterceptor;

	private final SignInRequiredInterceptor signInRequiredInterceptor;

	public WebShopWebMvcConfig(WebShopProperties properties, AdminOnlyInterceptor adminOnlyInterceptor,
			SignInRequiredInterceptor signInRequiredInterceptor) {
		this.properties = properties;
		this.adminOnlyInterceptor = adminOnlyInterceptor;
		this.signInRequiredInterceptor = signInRequiredInterceptor;
	}

	/**
	 * 把图标目录映射到 {@code /icons/**}：图标约 18MB，刻意放在 jar 之外。
	 * Maps the icon directory to {@code /icons/**}: roughly 18MB of icons, deliberately kept outside the jar.
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path iconDir = Path.of(properties.getIconDir()).toAbsolutePath();
		if (!iconDir.toFile().isDirectory()) {
			log.warn(I18n.get("log.e16a3b527c90", iconDir));
		}
		registry.addResourceHandler(properties.getIconUrlPrefix() + "/**")
			.addResourceLocations(iconDir.toUri().toString());
	}

	/**
	 * 注册鉴权拦截器：后台接口要求 GM，个人接口要求登录。
	 * Registers the authorisation interceptors: admin endpoints require a GM, personal endpoints require a session.
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(adminOnlyInterceptor).addPathPatterns("/api/admin/**");
		registry.addInterceptor(signInRequiredInterceptor).addPathPatterns("/api/me/**");
	}
}

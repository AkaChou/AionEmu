package com.aionemu.webshop.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import com.aionemu.webshop.auth.WebShopSession;

/**
 * 后台面板注册表：后台功能的扩展点。
 * Admin panel registry: the extension point of the admin console.
 * <p>后续功能（例如掉落倍率实时设置）只需在自己的配置类里向本注册表 {@link #register(AdminPanel)} 一个描述符，
 * 并提供一个同前缀的 {@code @RestController}，后台菜单与权限过滤会自动生效，无需修改既有后台代码。
 * A later capability (for example live drop-rate tuning) only registers a descriptor through
 * {@link #register(AdminPanel)} from its own configuration class and ships a {@code @RestController} on the
 * matching prefix; the menu and its permission filter then follow automatically, with no change to the
 * existing console code.</p>
 */
@Component
public class AdminPanelRegistry {

	private final List<AdminPanel> panels = new CopyOnWriteArrayList<>();

	/**
	 * 构造即注册内置面板。
	 * Registers the built-in panels on construction.
	 */
	public AdminPanelRegistry() {
		register(new AdminPanel("products", "webshop.panel.products", "/api/admin/products",
			WebShopSession.ADMIN_ACCESS_LEVEL));
		register(new AdminPanel("orders", "webshop.panel.orders", "/api/admin/orders",
			WebShopSession.ADMIN_ACCESS_LEVEL));
		register(new AdminPanel("catalog", "webshop.panel.catalog", "/api/admin/catalog",
			WebShopSession.ADMIN_ACCESS_LEVEL));
	}

	/**
	 * 注册一个面板；id 重复时后者覆盖前者，便于后续功能替换内置面板。
	 * Registers a panel; a duplicate id replaces the earlier entry so a later capability can supersede a
	 * built-in panel.
	 *
	 * @param panel 面板描述符 / panel descriptor
	 */
	public void register(AdminPanel panel) {
		panels.removeIf(existing -> existing.id().equals(panel.id()));
		panels.add(panel);
	}

	/**
	 * 返回给定会话可见的面板（按注册顺序）。
	 * Returns the panels visible to the given session, in registration order.
	 *
	 * @param session 当前会话 / current session
	 * @return 可见面板 / visible panels
	 */
	public List<AdminPanel> visibleTo(WebShopSession session) {
		List<AdminPanel> visible = new ArrayList<>();
		for (AdminPanel panel : panels) {
			if (panel.visibleTo(session)) {
				visible.add(panel);
			}
		}
		return visible;
	}
}

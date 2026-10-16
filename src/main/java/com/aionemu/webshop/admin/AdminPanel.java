package com.aionemu.webshop.admin;

import com.aionemu.webshop.auth.WebShopSession;

/**
 * 后台面板描述符：后台前端按此列表渲染菜单，新增功能只需注册一个面板而无需改动既有页面。
 * Admin panel descriptor: the admin front end renders its menu from this list, so a new capability only
 * registers a panel instead of touching existing pages.
 *
 * @param id              面板唯一标识（前端路由片段）/ unique panel id (the front-end route segment)
 * @param titleKey        面板标题的 i18n 键 / i18n key of the panel title
 * @param apiPrefix       面板 API 的路径前缀 / path prefix of the panel API
 * @param minAccessLevel  可见所需的最低权限等级 / minimum access level required to see the panel
 */
public record AdminPanel(String id, String titleKey, String apiPrefix, int minAccessLevel) {

	/**
	 * 判断给定会话是否可见该面板。
	 * Whether the given session may see this panel.
	 *
	 * @param session 当前会话 / current session
	 * @return 可见返回 true / true when visible
	 */
	public boolean visibleTo(WebShopSession session) {
		return session != null && session.accessLevel() >= minAccessLevel;
	}
}

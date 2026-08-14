package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ServerCommandProcessor;
import com.aionemu.gameserver.configs.main.GSConfig;
import org.springframework.stereotype.Component;

/**
 * 管理面板网关：判断开关并启动管理面板。
 * Admin-panel gateway: checks the switch and starts the admin panel.
 */
@Component
public class GameAdminPanelGateway {

    /**
     * 管理面板是否已启用。
     * Whether the admin panel is enabled.
     *
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    public boolean isAdminPanelEnabled() {
        return GSConfig.SERVER_YAADMINPANEL_SWITCH_ON;
    }

    /**
     * 启动管理面板。
     * Start the admin panel.
     */
    public void startAdminPanel() {
        new ServerCommandProcessor().startAdminPanel();
    }

    /**
     * 返回当前时间毫秒数。
     * Return the current time in milliseconds.
     *
     * @return 当前时间毫秒 / Current time millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}

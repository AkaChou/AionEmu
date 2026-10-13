package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ServerCommandProcessor;
import com.aionemu.gameserver.configs.main.GSConfig;
import org.springframework.stereotype.Component;

/**
 * 管理面板生命周期：若启用则启动管理面板，加载状态由基类承载。
 * Lifecycle for admin panel: starts the admin panel when enabled; load state is hosted by the
 * base class.
 */
@Component
public class GameAdminPanelLifecycle extends GameStartupStage {

    /**
     * 启动本阶段：若启用则启动管理面板。
     * Start this stage: start the admin panel when enabled.
     */
    @Override
    protected void runStage() {
        if (isAdminPanelEnabled()) {
            startAdminPanel();
        }
    }

    /**
     * 管理面板是否已启用。
     * Whether the admin panel is enabled.
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    protected boolean isAdminPanelEnabled() {
        return GSConfig.SERVER_YAADMINPANEL_SWITCH_ON;
    }

    /**
     * 启动管理面板。
     * Start the admin panel.
     */
    protected void startAdminPanel() {
        new ServerCommandProcessor().startAdminPanel();
    }
}

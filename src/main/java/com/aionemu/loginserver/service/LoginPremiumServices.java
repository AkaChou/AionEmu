package com.aionemu.loginserver.service;

import com.aionemu.loginserver.controller.PremiumController;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoginPremiumServices implements DisposableBean {

    private static volatile ObjectProvider<PremiumController> premiumControllerProvider;
    private static volatile PremiumController resolvedPremiumController;

    public LoginPremiumServices(ObjectProvider<PremiumController> premiumControllerProvider) {
        LoginPremiumServices.premiumControllerProvider = premiumControllerProvider;
    }

    public static PremiumController premiumController() {
        ObjectProvider<PremiumController> provider = premiumControllerProvider;
        if (provider == null) {
            PremiumController resolved = resolvedPremiumController;
            if (resolved != null) {
                return resolved;
            }
            return remember(fallbackPremiumController());
        }
        return remember(provider.getIfAvailable(LoginPremiumServices::fallbackPremiumController));
    }

    @Override
    public void destroy() {
        premiumControllerProvider = null;
    }

    private static PremiumController fallbackPremiumController() {
        return Fallbacks.PREMIUM_CONTROLLER;
    }

    private static PremiumController remember(PremiumController premiumController) {
        resolvedPremiumController = premiumController;
        return premiumController;
    }

    static void resetForTests() {
        premiumControllerProvider = null;
        resolvedPremiumController = null;
    }

    private static final class Fallbacks {

        private static final PremiumController PREMIUM_CONTROLLER = new PremiumController();
    }
}

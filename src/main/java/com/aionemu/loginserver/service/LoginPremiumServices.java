package com.aionemu.loginserver.service;

import com.aionemu.loginserver.controller.PremiumController;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoginPremiumServices implements DisposableBean {

    private static volatile ObjectProvider<PremiumController> premiumControllerProvider;

    public LoginPremiumServices(ObjectProvider<PremiumController> premiumControllerProvider) {
        LoginPremiumServices.premiumControllerProvider = premiumControllerProvider;
    }

    public static PremiumController premiumController() {
        ObjectProvider<PremiumController> provider = premiumControllerProvider;
        if (provider == null) {
            return fallbackPremiumController();
        }
        return provider.getIfAvailable(LoginPremiumServices::fallbackPremiumController);
    }

    @Override
    public void destroy() {
        premiumControllerProvider = null;
    }

    private static PremiumController fallbackPremiumController() {
        return Fallbacks.PREMIUM_CONTROLLER;
    }

    private static final class Fallbacks {

        private static final PremiumController PREMIUM_CONTROLLER = new PremiumController();
    }
}

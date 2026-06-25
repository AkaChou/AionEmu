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
            return PremiumController.getController();
        }
        return provider.getIfAvailable(PremiumController::getController);
    }

    @Override
    public void destroy() {
        premiumControllerProvider = null;
    }
}

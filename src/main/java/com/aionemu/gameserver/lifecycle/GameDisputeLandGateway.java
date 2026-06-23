package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameDisputeLandGateway {

    public void start() {
        Util.printSection(" *** Dispute Land initialization *** ");
        DisputeLandService.getInstance().initDisputeLand();
        OutpostService.getInstance().initOutposts();
    }
}

package com.aionemu.commons.network;

import java.io.IOException;

public interface NettyConnectionFactory {

    AConnection create(ConnectionTransport transport) throws IOException;
}

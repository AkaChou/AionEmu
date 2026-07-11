package com.aionemu.boot.transport;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.AionServicesProperties.TransportMode;
import com.aionemu.boot.i18n.I18n;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 传输边界：按配置选择并启动唯一活跃传输，容器销毁时停止。
 * Transport boundary: selects and starts the single active transport by config, stops on destroy.
 */
@Slf4j
@Component
public class AionTransportBoundary implements DisposableBean {

    private final AionServicesProperties services;
    private final Map<TransportMode, AionTransportLifecycle> transportsByMode;
    private AionTransportLifecycle activeTransport;

    /**
     * Spring 注入：聚合全部传输生命周期实现。
     * Spring injection: aggregate all transport lifecycle implementations.
     *
     * Service properties
     * @param transportLifecycles 传输生命周期列表 / Transport lifecycle list
     */
    @Autowired
    public AionTransportBoundary(
        AionServicesProperties services,
        List<AionTransportLifecycle> transportLifecycles
    ) {
        this.services = services;
        this.transportsByMode = indexTransportLifecycles(transportLifecycles);
    }

    /**
     * 测试/手动装配：仅注入 Netty 传输实现。
     * Test/manual wiring: inject Netty transport only.
     *
     * Service properties
     * @param nettyTransport Netty 传输生命周期 / Netty transport lifecycle
     */
    public AionTransportBoundary(
        AionServicesProperties services,
        NettyTransportLifecycle nettyTransport
    ) {
        this(services, List.of(nettyTransport));
    }

    /**
     * 按配置模式解析并启动活跃传输。
     * Resolve and start the active transport for the configured mode.
     */
    public void prepare() {
        TransportMode mode = services.getTransport().getMode();
        AionTransportLifecycle transport = transport(mode);
        activeTransport = transport;
        transport.start();
        if (mode == TransportMode.NETTY) {
            log.info(I18n.get("log.21d63d44ffa6"));
        }
    }

    /**
     * 停止活跃传输并清空引用。
     * Stop the active transport and clear the reference.
     */
    @Override
    public void destroy() {
        if (activeTransport != null) {
            activeTransport.stop();
        }
        activeTransport = null;
    }

    /**
     * 按模式查找已注册的传输生命周期。
     * Look up the registered transport lifecycle by mode.
     *
     * @param mode 传输模式 / Transport mode
     * @return 对应生命周期 / Matching lifecycle
     * No implementation registered for the mode。 / No implementation registered for the mode.
     */
    private AionTransportLifecycle transport(TransportMode mode) {
        AionTransportLifecycle transport = transportsByMode.get(mode);
        if (transport == null) {
            throw new IllegalStateException("No transport lifecycle registered for mode " + mode);
        }
        return transport;
    }

    /**
     * 将生命周期列表索引为模式映射，并拒绝重复注册。
     * Index lifecycle list by mode and reject duplicate registrations.
     *
     * @param transportLifecycles 生命周期列表 / Lifecycle list
     * @return 模式到实现的映射 / Mode-to-implementation map
     * Same mode registered more than once。 / Same mode registered more than once.
     */
    private static Map<TransportMode, AionTransportLifecycle> indexTransportLifecycles(
        List<AionTransportLifecycle> transportLifecycles
    ) {
        Map<TransportMode, AionTransportLifecycle> indexed = new EnumMap<>(TransportMode.class);
        for (AionTransportLifecycle transportLifecycle : transportLifecycles) {
            AionTransportLifecycle previous = indexed.put(transportLifecycle.mode(), transportLifecycle);
            if (previous != null) {
                throw new IllegalStateException(
                    "Duplicate transport lifecycle registered for mode " + transportLifecycle.mode()
                );
            }
        }
        return indexed;
    }
}

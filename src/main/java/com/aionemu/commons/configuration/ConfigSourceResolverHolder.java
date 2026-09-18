package com.aionemu.commons.configuration;

/**
 * 全局配置键解析器持有者，由启动层在 Spring 上下文刷新后发布一次。
 *
 * <p>存在目的：遗留 {@link ConfigurableProcessor} 以静态字段为唯一权威，而 Spring 的
 * {@code @ConfigurationProperties} 绑定读的是 {@code Environment}。若不打通，命令行/环境变量覆盖
 * 只会写到 Bean 属性，随后会被遗留加载器用文件值覆盖，形成"Bean 与静态字段各看一套值"的分裂。
 * 因此 {@code ConfigurableProcessor} 在回退到 {@code Properties} 之前，先向本持有者查询同键的有效值。</p>
 *
 * Global holder for the configuration-key resolver, published once by the bootstrap layer after the
 * Spring context is refreshed.
 *
 * <p>Why it exists: the legacy {@link ConfigurableProcessor} treats the static fields as the single
 * authority, while Spring {@code @ConfigurationProperties} binding reads the {@code Environment}.
 * Without a bridge, a command-line or environment override would only reach bean properties and then
 * be overwritten by the legacy loader using the file value, leaving beans and static fields split.
 * {@code ConfigurableProcessor} therefore consults this holder for the same key before falling back
 * to the {@code Properties} array.</p>
 */
public final class ConfigSourceResolverHolder {

    /**
     * 已发布的解析器；未发布时为 {@code null}。
     * Published resolver; {@code null} when the bootstrap layer has not published one yet.
     */
    private static volatile ConfigSourceResolver resolver;

    /**
     * 工具类禁止实例化。
     * Utility class; not instantiable.
     */
    private ConfigSourceResolverHolder() {
    }

    /**
     * 发布解析器。
     * Publishes the resolver.
     *
     * @param configSourceResolver 解析器；{@code null} 表示清除 / resolver; {@code null} clears it
     */
    public static void publish(ConfigSourceResolver configSourceResolver) {
        resolver = configSourceResolver;
    }

    /**
     * 解析键的有效值。
     * Resolves the effective value for a key.
     *
     * @param key 配置键 / configuration key
     * @return 有效值；未发布或无覆盖时为 {@code null} / effective value, or {@code null}
     *         when nothing was published or no override exists
     */
    public static String resolve(String key) {
        ConfigSourceResolver active = resolver;
        return active == null ? null : active.resolve(key);
    }
}

package com.aionemu.commons.configuration;

/**
 * 配置键解析器：由启动层提供，用于在遗留 {@code .properties} 组装前查询更高优先级的配置来源
 * （Spring {@code Environment}：命令行参数、环境变量、application.yml）。
 * Resolver for configuration keys, supplied by the bootstrap layer, used to consult a
 * higher-precedence configuration source (the Spring {@code Environment}: command-line arguments,
 * environment variables, application.yml) before the legacy {@code .properties} assembly runs.
 */
@FunctionalInterface
public interface ConfigSourceResolver {

    /**
     * 解析指定键的当前有效值。
     * Resolves the currently effective value for the given key.
     * @param key 配置键 / configuration key
     * @return 有效值；无覆盖时为 {@code null} / effective value, or {@code null} when absent
     */
    String resolve(String key);
}

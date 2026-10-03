# 2026-10-03 构建配置清理：删 `checkout-resources` 与 `combine.self="override"`

## 问题
1. `pom.xml` 的 `checkout-resources`（`activeByDefault`）是否还有意义？
2. `external-runtime-resources` 上 `<resources combine.self="override">` 是否必要？

## 结论
1. **删除 `checkout-resources`**：它唯一的内容是 `<resource>src/main/resources</resource>`，
   而该目录本就是 Maven 超级 POM 的默认资源目录（主 `<build>` 无 `<resources>`），
   删除对默认构建与 `-Daion.external-resources=true` 构建均无行为影响。
   （它诞生于 P4d `5413a8262`，用于追加 `target/generated-resources`；生成器在 `87027f82e` 退役后已是空壳。）
2. **删除 `combine.self="override"`**：实测对 effective POM 完全无影响，属冗余属性。
   `external-runtime-resources` 的 `exclude aion/**` 不依赖它。

## 证据（2026-10-03，Maven 3.9.16 / JDK 25；只跑模型类 goal，未编译未测试）
- A/B 对照（真实 `pom.xml` 副本，唯一差异是该属性；`-Daion.external-resources=true`）：
  `mvn -o -q help:effective-pom -Doutput=<file>`。
  两者 `<build><resources>` 输出完全一致：**单条** `src/main/resources` + `exclude aion/**`。
- 删除属性后的真实 `pom.xml` 复核（同上命令）：
  `resources blocks = 1`；`dirs = [<repo>/src/main/resources]`；`excludes = [aion/**]`。
- 机制口径：Maven 对 `build/resources` 的 profile 合并不是普通 list 追加（同目录条目按
  `directory` 归并、profile 侧生效），因此 `combine.self` 不参与排除效果。
- 旧台账 `2026-09-28-p4d-monster-tables-generation.zh-CN.md` §4 曾判定“等价冗余”但未做 A/B 对照，
  本次补齐；memory-bank QE-099 里“不要指望 `<resources combine.self="override">` 生效”的告诫保持成立。

## 涉及文件
- `pom.xml`：删 `checkout-resources` profile；`<resources combine.self="override">` → `<resources>`；
  更新 profile 注释为“部署打包形态 + 默认形态”说明。

## 未执行（PENDING，按仓库规则需授权）
- `mvn -DskipTests package`
- `mvn -DskipTests -Daion.external-resources=true package`
- `MavenPackageRuntimeResourcesTest` 未运行；其断言为 POM 文本匹配（`<exclude>aion/**</exclude>`、
  `<id>external-runtime-resources</id>`、`<name>aion.external-resources</name>`），静态比对仍满足。

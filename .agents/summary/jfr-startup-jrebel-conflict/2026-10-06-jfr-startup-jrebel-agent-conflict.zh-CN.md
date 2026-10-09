# JFR 启动录制与 JRebel 冲突导致 JVM 初始化中止（2026-10-06）

## 症状 / Symptom

Spring Boot 运行配置（`.idea/workspace.xml` → `Spring Boot.AionBootApplication` 的 `VM_PARAMETERS`）
带 `-XX:StartFlightRecording=name=AionStartup,settings=profile,filename=$PROJECT_DIR$/startup-%p-%t.jfr,duration=45s,dumponexit=true`
时，若同一次运行同时启用 JRebel，JVM 在初始化阶段直接退出，退出码 1，应用日志一行都没有：

```
[8.805s][error][jfr,startup] JfrJvmtiAgent::retransformClasses failed: JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED
Error occurred during initialization of VM
Failure when starting JFR on_create_vm_3
```

## 根因 / Root cause

`-XX:StartFlightRecording` 使 JFR 在 VM 初始化末段（`on_create_vm_3`）创建自己的 JVMTI agent，
并对已加载类执行一次 `RetransformClasses`。同一次运行里的 JRebel agent 也在改写类：
它用 griffin bootstrap（`-Drebel.griffin.bootstrap_*`、把 `jrebel.jar` 追加进 bootstrap classpath、
开启 boot class path 仿冒）对 JDK 类做 `BootBuild` 变换，日志中可见它连 `jdk/jfr/internal/**`
（`Logger`、`dcmd/DCmdStart`、`util/Output$*`）、`jdk/management/jfr/**`、`sun/instrument/InstrumentationImpl`
都被改写。重转换链产出的字节码相对当前类定义新增了方法，触犯 JVMTI 重转换约束
（retransform 不得增删方法/字段）→ JFR agent 初始化失败 → JVM 初始化中止。

JFR 启动录制失败是致命错误，无法降级为警告。

## 证据 / Evidence（同一 JDK：Azul Zulu 26.0.2.1+1）

对照仓库内历史 `startup-*.jfr`（`jfr print --events jdk.JVMInformation` 提取的 agent 集合与主类）：

| 运行 | JVMTI/java agent 组合 | main class | 录制时长 |
|---|---|---|---|
| 09-28 ~ 10-06 08:39（30+ 次） | mybatis-log-agent + JFR | `com.aionemu.AionBootApplication` | 45 s（3.5–4 MB，完整） |
| 10-06 08:58:17 | JRebel + JFR | `com.zeroturnaround.javarebel.Install` | 3 s（截断） |
| 10-06 08:58:36 | JRebel + async-profiler + IntelliJ captureAgent + JFR | 同上 | 4 s（截断，JVM 报错退出） |

- 变量只有 JRebel：不带 JRebel 时，同样的 JFR 启动录制在同一 JDK 上一直正常。
- JRebel trace 日志：`~/.jrebel/bootcache/jrebel-bootstrap-<hash>.tmp.jar.log`（启动配置带 `-Drebel.log=trace`）。
- JRebel 启动器日志：`~/.jrebel/bootcache/jrebel.boot.log`（可见其用同一份参数 exec 子 JVM）。
- 失败运行的 JFR 文件为 0 字节/截断（VM 初始化期即死）。

## 处置 / Workarounds

1. 要录启动性能：**不启用 JRebel** 再跑（历史 dump 证明该组合干净）。
2. 要留着 JRebel：从运行配置 VM options 去掉 `-XX:StartFlightRecording=...`，改为启动后 attach：
   `jcmd <pid> JFR.start name=AionStartup settings=profile filename=startup.jfr duration=45s`，
   或用 IDEA Profiler（async-profiler）。运行期启动不走 VM init 的 retransform，可绕开冲突。
3. 两者不可同时叠加；本机没有可绕开 JVMTI 约束的开关。

## 备注

- 报错发生在 VM 初始化阶段，与 AionEmu 代码/Spring 配置无关。
- `-XX:StartFlightRecording` 是手工加在该运行配置的 VM options 里的，不是 IDEA Profiler 自动注入。

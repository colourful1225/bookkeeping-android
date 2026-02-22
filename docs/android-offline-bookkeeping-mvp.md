# Android 离线记账 MVP 技术说明文档（Room + WorkManager）

## 1. 文档目标

本文档用于指导 Android 离线记账 MVP 的工程落地，定义：

- 功能边界与非目标。
- 模块职责、数据模型、同步协议与状态机。
- 可直接实施的 Android 文件清单（含路径、职责、落地顺序）。

## 2. 范围与非目标

### 2.1 MVP 范围

- 离线新增支出（Expense）并立即可见。
- 本地持久化（Room）与列表读取（Flow）。
- Outbox 模式异步补传（WorkManager）。
- 同步状态可观测：`PENDING`、`SYNCED`、`FAILED`、`CONFLICT`。
- 幂等提交：请求头 `Idempotency-Key`。

### 2.2 非目标（本阶段不实现）

- 冲突解决 UI（仅记录 `CONFLICT`）。
- 多端实时合并策略。
- 批量导入导出与复杂统计报表。

## 3. 技术选型与版本

- Kotlin + Coroutines + Flow
- Room `2.6.1`
- WorkManager `2.9.1`
- Retrofit `2.11.0` + Moshi Converter
- OkHttp Logging Interceptor `4.12.0`

Gradle 依赖（示例）：

```kotlin
dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
```

## 4. 架构设计

## 4.1 总体原则

- 本地优先：所有读写先落本地数据库。
- 双写事务：业务数据与 outbox 操作同事务提交。
- 最终一致：网络恢复后后台补传。
- 幂等防重：客户端产生稳定 key，服务端去重。

## 4.2 模块分层

```text
app/src/main/java/com/example/bookkeeping/
  data/
    local/       # Room 实体、DAO、Database
    remote/      # Retrofit API 与 DTO
    repo/        # 本地写入 + outbox 入队
  sync/          # Worker、调度器、映射器
  domain/        # UseCase（可后续补充）
  di/            # Room/Retrofit/Worker 注入
  ui/            # 页面与状态展示
```

## 5. 数据模型与状态机

## 5.1 交易表 `transactions`

字段约束：

- `id`：本地 UUID，主键。
- `serverId`：服务端 ID，可空。
- `amount`：金额分（`Long`）。
- `type`：`INCOME` / `EXPENSE` / `TRANSFER`。
- `syncStatus`：`PENDING` / `SYNCED` / `FAILED` / `CONFLICT`。

## 5.2 Outbox 表 `outbox_ops`

字段约束：

- `opId`：操作主键 UUID。
- `entityId`：关联 `transactions.id`。
- `opType`：`CREATE` / `UPDATE` / `DELETE`。
- `payloadJson`：操作快照。
- `idempotencyKey`：幂等键（建议唯一）。
- `retryCount`、`nextRetryAt`：退避控制。
- `status`：`PENDING` / `PROCESSING` / `DONE` / `DEAD`。

索引建议：

- 复合索引：`(status, nextRetryAt, createdAt)`。
- 唯一索引（建议）：`idempotencyKey`。

## 5.3 同步状态表 `sync_state`

- `lastSyncAt`：最后成功同步时间。
- `lastError`：最近一次错误摘要。

## 5.4 状态迁移

- 交易：`PENDING -> SYNCED`；失败可标记 `FAILED`；冲突标记 `CONFLICT`。
- Outbox：`PENDING -> PROCESSING -> (删除|DONE)`；失败回到 `PENDING`；超限 `DEAD`。

## 6. 关键流程

## 6.1 写入流程（离线可用）

1. 生成 `txId` 与 `idempotencyKey`。
2. 构建 `TransactionEntity(syncStatus=PENDING)`。
3. 构建 `OutboxOpEntity(status=PENDING)`。
4. 在同一个 `db.runInTransaction {}` 中写入 `transactions` + `outbox_ops`。
5. UI 通过 Room Flow 立即看到新交易。

## 6.2 同步流程（网络恢复）

1. Worker 拉取 `outbox_ops` 中到期的 `PENDING` 记录（分页）。
2. 标记为 `PROCESSING` 后调用远端 upsert。
3. 成功：回写 `transactions.syncStatus=SYNCED` 与 `serverId`，删除 outbox 记录。
4. 失败：`retryCount+1`，按指数退避更新时间；达到阈值后置 `DEAD`。

## 6.3 退避策略

- 退避公式：`2^n` 秒（`n` 取 `retryCount`，并设置上限）。
- 建议最大重试：10 次。
- 建议区分错误类型：
  - 网络错误 / 5xx：可重试。
  - 4xx（业务不可恢复）：直接 `DEAD` 或 `CONFLICT`。

## 7. API 契约（最小）

- Endpoint：`POST /v1/transactions/upsert`
- Header：`Idempotency-Key: <key>`
- Request：`id, amount, type, categoryId, note, occurredAt`
- Response：`serverId`

服务端要求：

- 对相同 `Idempotency-Key` 保证幂等，不重复入账。
- 冲突返回可识别错误码，客户端映射为 `CONFLICT`。

## 8. Android 工程文件清单（可落地）

以下清单按“必须创建（MVP）/ 可选增强”拆分，路径可直接用于建文件。

## 8.1 必须创建（MVP）

| 路径                                                          | 文件                           | 职责                  | 关键实现点                      |
| ------------------------------------------------------------- | ------------------------------ | --------------------- | ------------------------------- |
| `app/src/main/java/com/example/bookkeeping/data/local/entity` | `TransactionEntity.kt`         | 交易主数据实体        | 含 `syncStatus`、`serverId`     |
| `app/src/main/java/com/example/bookkeeping/data/local/entity` | `OutboxOpEntity.kt`            | 同步操作队列实体      | 含重试字段与索引                |
| `app/src/main/java/com/example/bookkeeping/data/local/entity` | `SyncStateEntity.kt`           | 全局同步状态          | 记录最近同步结果                |
| `app/src/main/java/com/example/bookkeeping/data/local/dao`    | `TransactionDao.kt`            | 交易增改查            | Flow 列表、同步回写             |
| `app/src/main/java/com/example/bookkeeping/data/local/dao`    | `OutboxDao.kt`                 | Outbox 拉取与状态迁移 | `fetchPending` + `updateStatus` |
| `app/src/main/java/com/example/bookkeeping/data/local/dao`    | `SyncStateDao.kt`              | 同步状态读写          | upsert 最近同步时间/错误        |
| `app/src/main/java/com/example/bookkeeping/data/local`        | `AppDatabase.kt`               | Room 数据库定义       | 暴露 3 个 DAO                   |
| `app/src/main/java/com/example/bookkeeping/data/remote`       | `BookkeepingApi.kt`            | 远端接口定义          | upsert + 幂等头                 |
| `app/src/main/java/com/example/bookkeeping/data/remote/dto`   | `UpsertTransactionRequest.kt`  | 请求 DTO              | 与实体解耦                      |
| `app/src/main/java/com/example/bookkeeping/data/remote/dto`   | `UpsertTransactionResponse.kt` | 响应 DTO              | 包含 `serverId`                 |
| `app/src/main/java/com/example/bookkeeping/data/repo`         | `TransactionRepository.kt`     | 离线写入与入队        | 事务双写                        |
| `app/src/main/java/com/example/bookkeeping/sync`              | `SyncMapper.kt`                | payload 与 DTO 转换   | 序列化/反序列化                 |
| `app/src/main/java/com/example/bookkeeping/sync`              | `SyncWorker.kt`                | 批量同步执行器        | 分页、回写、重试                |
| `app/src/main/java/com/example/bookkeeping/sync`              | `SyncScheduler.kt`             | 周期 + one-shot 调度  | 唯一周期任务                    |
| `app/src/main/java/com/example/bookkeeping/di`                | `DatabaseModule.kt`            | Room 注入             | 提供 `AppDatabase` 与 DAO       |
| `app/src/main/java/com/example/bookkeeping/di`                | `NetworkModule.kt`             | 网络注入              | Retrofit / OkHttp / Api         |
| `app/src/main/java/com/example/bookkeeping/di`                | `WorkerBindingModule.kt`       | Worker 依赖注入       | 自定义 `WorkerFactory`          |
| `app/src/main/java/com/example/bookkeeping`                   | `BookkeepingApp.kt`            | Application 初始化    | 初始化 WorkManager              |
| `app/src/main/AndroidManifest.xml`                            | `AndroidManifest.xml`          | 声明 Application      | 注册自定义 App 类               |
| `app/build.gradle.kts`                                        | `build.gradle.kts`             | 依赖与插件            | Room/Work/Retrofit/ksp          |

## 8.2 可选增强（下一阶段）

| 路径                                                             | 文件                            | 目的               |
| ---------------------------------------------------------------- | ------------------------------- | ------------------ |
| `app/src/main/java/com/example/bookkeeping/domain/usecase`       | `AddExpenseUseCase.kt`          | 收敛业务规则       |
| `app/src/main/java/com/example/bookkeeping/domain/usecase`       | `ObserveTransactionsUseCase.kt` | 统一读取口径       |
| `app/src/main/java/com/example/bookkeeping/ui/sync`              | `SyncStatusViewModel.kt`        | 展示同步状态与错误 |
| `app/src/main/java/com/example/bookkeeping/data/local/migration` | `Migration_1_2.kt`              | 数据库版本演进     |

## 9. 实施顺序（建议）

1. 建立 Room：实体、DAO、`AppDatabase`、基础查询。
2. 完成 `TransactionRepository` 的事务双写。
3. 打通 Retrofit API 与 DTO。
4. 实现 `SyncWorker` 与 `SyncMapper`。
5. 接入 `SyncScheduler`（启动周期 + 写后 one-shot）。
6. 完成 DI（含 `WorkerFactory`）并在 `Application` 初始化。
7. 最后接 UI：仅订阅本地 Flow 与 `syncStatus`。

## 10. 验收标准（MVP）

- 断网新增 3 条交易后，本地列表立即可见且均为 `PENDING`。
- 恢复网络后可自动补传，成功记录变更为 `SYNCED`。
- Outbox 成功项被删除，失败项按退避重试，超阈值进入 `DEAD`。
- 服务端对重复 `Idempotency-Key` 不重复入账。

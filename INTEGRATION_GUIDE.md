## 自动记账系统完善 - 集成指南

### 📋 模块对应表

| 模块                                  | 路径                                                | 功能                                   | 关键集成点                                                |
| ------------------------------------- | --------------------------------------------------- | -------------------------------------- | --------------------------------------------------------- |
| **EnhancedAmountExtractor**           | `notification/EnhancedAmountExtractor.kt`           | 多格式金额提取（千位、繁体、特殊符号） | `PaymentNotificationParser.extractAmount()`               |
| **MerchantCategoryLearner**           | `notification/MerchantCategoryLearner.kt`           | 商户分类学习库 + 用户纠正历史          | `PaymentAutoImporter.categorizeTransaction()`             |
| **ConflictDetector**                  | `notification/ConflictDetector.kt`                  | 手动+自动同时记账检测                  | `PaymentAutoImporter.importAsync()` 前置步骤              |
| **ObfuscationStrategy**               | `notification/ObfuscationStrategy.kt`               | 隐藏自动记账痕迹、伪装标签             | `PaymentAutoImporter.buildNote()` / `insertTransaction()` |
| **AccessibilityPerformanceOptimizer** | `notification/AccessibilityPerformanceOptimizer.kt` | 树遍历加速、缓存优化                   | `PaymentAccessibilityService.collectNodeTexts()`          |

---

### 🔧 逐步集成方案

#### **第 1 步：依赖注入配置** (DI Module)

**位置**：`hilt/PaymentProcessingModule.kt` (如不存在需创建)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object PaymentProcessingModule {
    @Singleton
    @Provides
    fun provideEnhancedAmountExtractor(): EnhancedAmountExtractor = EnhancedAmountExtractor()
    
    @Singleton
    @Provides
    fun provideMerchantCategoryLearner(
        context: Context,
        transactionDao: TransactionDao,
    ): MerchantCategoryLearner = MerchantCategoryLearner(context, transactionDao)
    
    @Singleton
    @Provides
    fun provideConflictDetector(transactionDao: TransactionDao): ConflictDetector =
        ConflictDetector(transactionDao)
    
    @Singleton
    @Provides
    fun provideObfuscationStrategy(): ObfuscationStrategy = ObfuscationStrategy()
    
    @Singleton
    @Provides
    fun provideAccessibilityPerformanceOptimizer(): AccessibilityPerformanceOptimizer =
        AccessibilityPerformanceOptimizer()
}
```

---

#### **第 2 步：改造 PaymentNotificationParser**

**文件**：`notification/PaymentNotificationParser.kt`

**改动点 1**：替换 `extractAmount()` 逻辑

```kotlin
class PaymentNotificationParser @Inject constructor(
    private val enhancedExtractor: EnhancedAmountExtractor,  // 新增注入
) {
    fun extractAmount(text: String): Long? {
        // ❌ 旧方式
        // val regex = Regex("""[¥￥](\d+(?:\.\d{1,2})?)""")
        // return regex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.times(100)?.toLong()
        
        // ✅ 新方式：支持多格式
        return enhancedExtractor.extractAmount(text)
    }
}
```

---

#### **第 3 步：改造 PaymentAutoImporter**

**文件**：`notification/PaymentAutoImporter.kt`

**改动点 1**：集成 ConflictDetector（前置步骤）

```kotlin
class PaymentAutoImporter @Inject constructor(
    private val conflictDetector: ConflictDetector,  // 新增注入
    private val merchantLearner: MerchantCategoryLearner,  // 新增注入
    private val obfuscation: ObfuscationStrategy,  // 新增注入
    // ... 既有注入
) {
    suspend fun importAsync(notification: PaymentNotification): Boolean = withContext(Dispatchers.IO) {
        try {
            val amount = notification.amount ?: return@withContext false
            val categoryId = notification.category ?: "others"

            // ▶ 第一步：冲突检测
            val hasConflict = conflictDetector.hasConflict(
                amountFen = amount,
                categoryId = categoryId,
                occurredAt = notification.occurredAt,
                type = "EXPENSE"
            )
            if (hasConflict) {
                Log.w("PaymentAutoImporter", "冲突检测命中，跳过导入")
                return@withContext false  // 跳过
            }

            // ▶ 第二步：分类学习（获取更准确的分类）
            val refinedCategoryId = merchantLearner.learnAndPredict(
                merchant = notification.merchant,
                amount = amount,
                userOverride = null
            )

            // ▶ 第三步：导入（原有逻辑）
            return@withContext importInternal(
                notification.copy(category = refinedCategoryId)
            )
        } catch (e: Exception) {
            Log.e("PaymentAutoImporter", "导入异常", e)
            return@withContext false
        }
    }

    private suspend fun importInternal(notification: PaymentNotification): Boolean {
        // ... 原有逻辑，但修改备注生成
        val transaction = buildTransaction(notification)
        return try {
            transactionDao.insert(transaction)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buildTransaction(notification: PaymentNotification): TransactionEntity {
        return TransactionEntity(
            amount = notification.amount ?: 0L,
            category = notification.category ?: "others",
            // ▶ 难以感知：备注处理
            note = buildObfuscatedNote(notification),
            merchant = obfuscation.obfuscateMerchant(notification.merchant),
            occurredAt = notification.occurredAt,
            importedSource = "AUTO"  // 标记来源（可被 obfuscation 隐藏）
        )
    }

    private fun buildObfuscatedNote(notification: PaymentNotification): String {
        // 原始备注
        val original = "[自动记账] ${notification.merchant}"
        // ▶ 难以感知化处理
        return obfuscation.obfuscateNote(original)  // 返回无标签的备注
    }
}
```

---

#### **第 4 步：改造 PaymentAccessibilityService**

**文件**：`notification/PaymentAccessibilityService.kt`

**改动点**：替换 `collectNodeTexts()` 实现

```kotlin
class PaymentAccessibilityService : AccessibilityService() {
    @Inject
    lateinit var perfOptimizer: AccessibilityPerformanceOptimizer

    private fun collectNodeTexts(nodeInfo: AccessibilityNodeInfo?): MutableList<String> {
        // ❌ 旧方式：递归 DFS，可达 80 层
        // return collectRecursive(nodeInfo, "", 0, 100)

        // ✅ 新方式：BFS + 深度限制 + 关键词优先
        val texts = perfOptimizer.collectNodeTextsOptimized(
            rootNode = nodeInfo,
            maxResults = 100
        )
        return texts.toMutableList()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // ... 既有逻辑
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val root = event.source ?: return
            val texts = collectNodeTexts(root)
            // ... 继续处理 texts
        }
    }
}
```

---

#### **第 5 步：验证编译与单测**

### ✅ 编译步骤

在项目根目录执行：

```bash
# 1. Gradle 重新编译
./gradlew clean assembleDebug

# 2. 检查编译结果
# 预期：BUILD SUCCESSFUL ✓

# 3. 运行 Lint 检查
./gradlew lint

# 4. 单元测试（如有）
./gradlew testDebugUnitTest
```

### ✅ 运行时验证

**场景 A**：多格式金额提取

1. 打开微信/支付宝支付页面
2. 发送包含 `¥1,234.56` / `￥1234.56` / `1234.56元` 的通知
3. 验证日志输出：`EnhancedAmountExtractor: 提取到 ¥1234.56`

**场景 B**：商户分类学习

1. 手动记账"星巴克"为"咖啡"分类
2. 下一次自动识别"Starbucks Coffee"时，应自动分类为"咖啡"
3. 验证日志：`MerchantCategoryLearner: 学习到映射 STARBUCKS → Coffee`

**场景 C**：冲突检测

1. 手动记账"¥69 → 优步"
2. 同时在无障碍中捕获"¥69 → UBER"通知
3. 验证日志：`ConflictDetector: 检测到冲突，跳过导入` ✓（不重复）

**场景 D**：难以感知

1. 自动导入一笔交易
2. 打开记录详情，验证备注为"星巴克"而非"[自动记账] STARBUCKS"
3. 验证商户名显示为"ST****"而非"STARBUCKS"

**场景 E**：无障碍性能

1. 频繁打开支付宝/微信支付界面（1-2秒间隔）
2. 监控 logcat：验证日志显示 `BFS 遍历，深度限制=20`
3. 检查内存：验证无 AccessibilityNodeInfo 泄漏

---

### 📊 集成清单

- [ ] 第 1 步：DI Module 配置完成
- [ ] 第 2 步：PaymentNotificationParser 改造完成
- [ ] 第 3 步：PaymentAutoImporter 改造完成（包括冲突检测 + 分类学习 + 难以感知）
- [ ] 第 4 步：PaymentAccessibilityService 改造完成
- [ ] 第 5 步：`./gradlew clean assembleDebug` 编译通过
- [ ] 第 6 步：5 个运行时场景验证全部满足
- [ ] 第 7 步：提交 PR + 代码审查

---

### 🚨 注意事项

1. **DAO 扩展**：ConflictDetector 需要 `transactionDao.queryByTimeRangeAndType()` 方法
   - 如不存在，需在 [TransactionDao.kt](bookkeeping-android/app/src/main/java/com/example/bookkeeping/data/local/dao/TransactionDao.kt) 中补充

2. **数据库迁移**：MerchantCategoryLearner 内部使用 SQLite 存储
   - 需确保数据库版本升级脚本已备好

3. **权限引导 UX**（可选）：
   - ObfuscationStrategy 已准备好新权限文案
   - 需更新 manifest 或 permission 提示页面

4. **回退方案**：如遇到运行时问题
   - 各模块设计都包含 try-catch 和 fallback 逻辑
   - 旧代码路径仍保留，可快速切回

---

### 📝 下一步工作

- [ ] **完整集成** → 修改 4 个核心文件（PaymentNotificationParser、PaymentAutoImporter、PaymentAccessibilityService、DI Module）
- [ ] **编译验证** → `./gradlew clean assembleDebug`
- [ ] **单测补充** → 为 5 个新模块各补充 2-3 个单元测试
- [ ] **真机测试** → 按 5 个场景进行功能回归
- [ ] **代码审查** → 提交 PR，邀请团队审查


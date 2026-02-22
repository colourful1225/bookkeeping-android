# 📱 记账本 - 功能实现总结 (v1.1)

> **更新日期**: 2026年2月22日  
> **项目状态**: ✅ MVP整合完毕，编译通过

---

## 📋 新增功能概览

### **需求1️⃣ - CSV 导入功能** ✅

完整实现了从其他记账软件导出的CSV文件导入功能。

#### 系统架构

```
CSV 文件 (*.csv)
    ↓
[CsvParser] - 解析 CSV 行 + 智能列匹配
    ↓
[ImportCsvUseCase] - 金额单位转换、分类映射、日期解析
    ↓
[Room 事务] - 原子批量入库 (Transaction + Outbox)
    ↓
[SyncWorker] - 异步后台补传
```

#### 支持的 CSV 格式

自动支持以下列名（**不区分大小写**）：

| 字段     | 支持的列名                                 | 示例值                           |
| -------- | ------------------------------------------ | -------------------------------- |
| **金额** | amount, 金额, price, 金钱, 消费金额        | 99.99                            |
| **分类** | category, 分类, type, 类别, 消费分类       | "购物"                           |
| **日期** | date, 日期, time, 时间, 发生日期, 交易日期 | "2026-02-22 14:30", "2026-02-22" |
| **备注** | note, 备注, memo, description, 说明, 摘要  | "超市购物"                       |

**示例 CSV 文件**:
```csv
金额,分类,日期,备注
99.50,购物,2026-02-20 10:30,服装
45.80,餐饮,2026-02-21,下午茶
128.00,交通,2026-02-21 18:00,出租车
```

#### 导入流程

1. **打开设置** 🔧 → 底部导航 > "设置"
2. **点击导入按钮** 📂 → "选择 CSV 文件"
3. **文件选择器打开** → 选择 CSV 文件
4. **验证 & 入库** → 系统自动解析、验证、转换货币单位
5. **导入结果** → 显示成功数、失败数及错误详情
6. **后台同步** → WorkManager 随后将其提交到服务端同步

#### 错误处理

| 错误类型     | 处理方式                               | 示例            |
| ------------ | -------------------------------------- | --------------- |
| 金额格式错误 | 跳过该行，记录错误                     | "不是数字"      |
| 金额 ≤ 0     | 跳过该行，记录错误                     | "-10"           |
| 分类不存在   | 自动模糊匹配；无结果时默认分配为"其他" | "购衣" → "购物" |
| 日期格式错误 | 使用导入时刻作为fallback               | 无效格式        |

---

### **需求2️⃣ - 完整记账输入界面** ✅

创建了专业的记账表单页面（`AddTransactionScreen`），支持快速、准确地录入交易。

#### UI 布局

```
┌─────────────────────────────────────┐
│ ◀ 新增支出                            │
├─────────────────────────────────────┤
│                                     │
│ 📊 金额（元）                        │
│ ┌─────────────────────────────────┐ │
│ │ 0.00                            │ │
│ └─────────────────────────────────┘ │
│ 总计: ¥0.00 (实时计算)              │
│                                     │
│ 🏷️  分类                             │
│ ┌────┐ ┌────┐ ┌────┐ ...          │
│ │🛍️   │ │🍽️   │ │🚗   │           │
│ │购物  │ │餐饮  │ │交通  │          │
│ └────┘ └────┘ └────┘ ...          │
│                                     │
│ 📅 日期                              │
│ ┌─────────────────────────────────┐ │
│ │ 2026年02月22日 14:30 → 选择日期 │ │
│ └─────────────────────────────────┘ │
│                                     │
│ 📝 备注（可选）                      │
│ ┌─────────────────────────────────┐ │
│ │ 添加相关备注...                  │ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
│                                     │
│ [   确认添加   ]                    │
│                                     │
│ ✅ 添加成功 (条件显示)               │
│                                     │
└─────────────────────────────────────┘
```

#### 字段详解

| 字段     | 类型              | 必需 | 功能                                                       |
| -------- | ----------------- | ---- | ---------------------------------------------------------- |
| **金额** | InputField        | ✅    | 数值输入，支持小数；实时显示转换后的金额                   |
| **分类** | LazyRow (8种预置) | ✅    | 滑动选择：购物、餐饮、交通、娱乐、生活费、医疗、教育、其他 |
| **日期** | DatePickerDialog  | ❌    | 三元组选择（年/月/日），默认当前时刻                       |
| **备注** | OutlinedTextField | ❌    | 自由文本，最多5行                                          |

#### 分类信息

系统预置 8 个分类，每个都带有 emoji 图标和颜色标签：

```kotlin
🛍️  购物      (primaryColor: #FF9C27B0)
🍽️  餐饮      (primaryColor: #FFFF9800)
🚗  交通      (primaryColor: #FF2196F3)
🎬  娱乐      (primaryColor: #FFF44336)
🏠  生活费    (primaryColor: #FF4CAF50)
⚕️  医疗      (primaryColor: #FF2196F3)
📚  教育      (primaryColor: #FF673AB7)
📌  其他      (primaryColor: #FF757575)
```

#### 表单验证

| 条件         | 验证结果 | 错误提示         |
| ------------ | -------- | ---------------- |
| 金额为空     | ❌        | "请输入金额"     |
| 金额 ≤ 0     | ❌        | "金额必须大于 0" |
| 金额非数字   | ❌        | "金额格式无效"   |
| 所有字段有效 | ✅        | 按钮启用，可提交 |

#### 交互流程

```
用户点击"记账"FAB
  ↓
导航到 AddTransactionScreen
  ↓
输入金额（必需） → 输入分类（必需） → 选择日期（可选） → 输入备注（可选）
  ↓
点击"确认添加"
  ↓
验证表单 → ❌ 显示错误提示（不关闭）
         → ✅ 锁定表单 + 显示"提交中..."
  ↓
AddExpenseUseCase 执行
  ↓
Repository 写入本地 DB + 生成 Outbox
  ↓
✅ 成功提示 → 自动返回列表
┌─ ❌ 失败提示 → 保留表单，重试
```

---

## 🏛️ 系统架构改动

### 数据模型层

#### 新增: CategoryEntity

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,              // "shopping", "dining", etc.
    val name: String,                         // "购物", "餐饮"
    val icon: String?,                        // "🛍️", "🍽️" (可选emoji)
    val color: String?,                       // "#FFXXXXXX" (ARGB)
    val isDefault: Boolean = true,            // 预置分类?
)
```

#### 新增: CsvImportResult

```kotlin
data class CsvImportResult(
    val successCount: Int,      // 成功导入行数
    val failureCount: Int,      // 失败行数
    val errors: List<String>,   // 错误详情清单
    val importedRowCount: Int,  // 总处理行数
)
```

### 数据库

- **数据库版本**: 1 → 2 (自动迁移，fallbackToDestructiveMigration)
- **新增表**: `categories`
- **新建索引**: 无
- **初始化**: DatabaseInitializer 在 AppDatabase 创建时自动加载 8 个预置分类

### DAO 层

#### CategoryDao

```kotlin
@Dao interface CategoryDao {
    fun observeAll(): Flow<List<CategoryEntity>>    // 监听所有分类
    suspend fun getAll(): List<CategoryEntity>      // 同步获取全部
    suspend fun getById(id: String): CategoryEntity? // 单条查询
    suspend fun insertAll(categories: List<...>)    // 批量插入
}
```

#### TransactionDao + OutboxDao

- 新增 `insertAll()` 方法支持 CSV 批量导入

### 业务逻辑层

#### ImportCsvUseCase

```kotlin
suspend operator fun invoke(uri: Uri): CsvImportResult

// 职责:
// 1. 读取 URI 指向的 CSV 文件 (via ContentProvider)
// 2. 解析行数据，识别列名（智能匹配）
// 3. 验证金额 > 0，映射分类（模糊匹配 → 默认"其他"）
// 4. 解析日期（多格式支持: yyyy-MM-dd HH:mm:ss, yyyy-MM-dd等）
// 5. 原子批量写入 (transactions + outbox_ops 同事务)
// 6. 返回导入统计结果
```

### 导航层

#### MainScreen 路由扩展

```
底部导航新增: "设置" (Settings)
┌─ TRANSACTIONS  → TransactionListScreen
├─ REPORT        → ReportScreen
├─ SETTINGS      → SettingsScreen
└─ ADD_TRANSACTION (嵌套导航) → AddTransactionScreen
   (从 TransactionListScreen FAB 触发)
```

#### TransactionListScreen

- FAB 点击不再调用 `viewModel.addSampleExpense()`
- 改为 `onAddClick()` 回调，触发导航 → AddTransactionScreen

---

## 🔧 技术实现细节

### CSV 解析算法

```kotlin
// 1. 读取第一行作为头
// 2. 对每一行逐字符解析，处理引号escaping
// 3. 构建 Map<columnName, value>
// 4. 按优先级列表智能映射字段
//    a) 精确匹配 (case-insensitive)
//    b) 模糊包含匹配
//    c) 返回 null (字段可选)
```

### 金额转换

- **输入**: "99.99" (元) → 用户界面
- **内部存储**: `99.99 * 100 = 9999` (分) → 避免浮点精度问题
- **显示**: `9999 / 100.0 = 99.99` → 格式化输出 "¥99.99"

### 日期处理

支持多种格式自动识别:
- `yyyy-MM-dd HH:mm:ss` ✅
- `yyyy-MM-dd` ✅ (时间默认为 00:00:00)
- `yyyy/MM/dd HH:mm:ss` ✅
- `yyyy/MM/dd` ✅
- 其他格式 → 使用导入时刻 fallback

### 分类映射策略

```
输入分类: "购衣" → 数据库中查确匹配
                 ↓ 无结果，尝试模糊匹配
                 → 寻找包含"购"的分类
                 → 匹配到"购物" ✅
                 
输入分类: "未知类型" → 精确匹配失败
                      ↓ 模糊匹配失败
                      → 默认分配"其他" ✅
```

---

## 📸 UI 流程图

### 记账输入流程
```
记账本 (列表)
    ↓ 点击 FAB (+)
新增支出 (表单)
    ├─ 输入金额 → 实时计算
    ├─ 选择分类 (LazyRow滑动)
    ├─ 选择日期 (DatePicker弹窗)
    └─ 输入备注
    ↓ 点击"确认添加"
验证 ← ❌ 显示错误，保留表单
  ↓ ✅
写入本地 DB
    ↓
✅ 成功提示
    ↓
返回列表 (列表刷新)
```

### CSV 导入流程
```
记账本
    ↓ 底部导航 > "设置"
设置 (SettingsScreen)
    ├─ 📂 [选择 CSV 文件]
    ↓ 文件选择器开打开
    ↓ 用户选择文件
    ↓ ImportCsvUseCase.invoke(uri)
    ├─ 解析...
    ├─ 验证...
    ├─ 转换...
    ↓ 入库 + 生成 Outbox
⚙️ 导入结果卡片 (显示统计)
    ├─ ✅ 成功: N 条
    ├─ ❌ 失败: M 条
    └─ 📋 错误列表 (前3条)
    
    ↓ (后台)
    WorkManager 异步同步 → 服务端
```

---

## 🚀 使用指南

### 场景1: 手工新增支出

```
1. 打开"记账本"  
2. 点击右下角浮动按钮 (+)
3. 填写表单:
   - 金额: 45.50
   - 分类: 🍽️ 餐饮 (swipe选择)
   - 日期: 保持当前或修改
   - 备注: 中午聚餐
4. 点击"确认添加"
5. ✅ 添加成功，列表自动刷新
```

### 场景2: 批量导入历史账单

```
1. 打开支付宝/微信/其他APP，导出消费记录为 CSV
2. 打开"记账本" > 底部"设置"
3. 点击"选择 CSV 文件"
4. 选择文件，等待导入完成
5. 查看导入统计:
   ✅ 成功: 98 条
   ❌ 失败: 2 条
   📋 错误: ["行5: 金额格式无效", ...]
6. 底部返回"记账本"，观察列表已包含新数据
7. WorkManager 后台运行，自动同步到服务端
```

### 场景3: 修改/删除支出

> 当前 MVP 尚未实现修改/删除功能，future work

---

## 📊 数据流

### 从输入到同步

```
┌──────────────────┐
│ AddTransactionScreen
│ 用户输入表单
└────────┬─────────┘
         │ "confirmsure"
         ↓
┌──────────────────────────┐
│ AddExpenseUseCase
│ .invoke(amount, categoryId, note)
│ - 验证 amount > 0
└────────┬─────────────────┘
         │
         ↓
┌──────────────────────────────┐
│ TransactionRepository
│ .addExpense(...)
│ - 生成 UUID + 时间戳
│ - 构建 TransactionEntity
│ - 生成 OutboxOpEntity (CREATE 操作)
└────────┬───────────────────────┘
         │ withTransaction
         ↓
┌──────────────────────────────┐
│ Room Database (事务)
│ ├─ INSERT into transactions
│ ├─ INSERT into outbox_ops
│ └─ COMMIT
└────────┬───────────────────────┘
         │
         ↓
┌──────────────────────┐
│ Flow emit
│ UI 订阅 observeAll()
│ 列表自动刷新
└──────────────────────┘

         (后台 - WorkManager)
         │
         ↓
┌──────────────────────────────┐
│ SyncWorker (周期性触发)
│ - 拉取 PENDING outbox_ops
│ - 调用 /api/transactions/upsert
│ - 含 Idempotency-Key 幂等头
└────────┬───────────────────────┘
         │
    Success ← Failure
    │           │
    ↓           ↓
 ┌─────────┐  ┌──────────────┐
 │ UPDATE  │  │ 指数退避重试 │
 │ SYNCED  │  │ (max 10次)   │
 │ & 删除  │  │ → DEAD       │
 │ outbox  │  └──────────────┘
 └─────────┘
```

---

## 🔐 安全性

### CSV 导入安全措施

1. **文件来源控制**: 用户通过标准 Android 文件选择器，避免任意文件访问
2. **行限制**: 解析时逐行处理，自动跳过格式错误行
3. **数据验证**: 金额必须 > 0；日期格式白名单；分类安全映射
4. **事务一致性**: 所有行失败时整体回滚，无部分成功状态

### 网络同步安全

- `Idempotency-Key` 幂等头防重复提交
- Release 构建仅支持 HTTPS + 系统 CA
- Debug 支持 HTTP (仅本地开发环境)

---

## 🎯 后续扩展建议

### Phase 2

- [ ] **支持修改/删除** → 在 DAO 层添加 update/delete，UI 添加 swipe-to-delete
- [ ] **图表统计** → 按分类、时间段聚合，使用 Material Chart
- [ ] **预算提醒** → 本地通知 + WorkManager 定时检查
- [ ] **数据备份** → App Backup / Drive Sync 集成
- [ ] **多用户** → User 表 + 多设备同步方案

### Phase 3

- [ ] **OCR 识别** → 扫描纸质票据自动解析
- [ ] **AI 分类** → ML Kit 本地推理，自动分类未知项
- [ ] **订阅管理** → 周期支出追踪
- [ ] **多种货币** → 汇率转换 + 本地缓存

---

## 📱 编译 & 运行

### 前置条件

- Android SDK 36 (API level 的定)
- JDK 17+
- Gradle 8.7+
- Kotlin 2.0.0

### 编译命令

```bash
cd bookkeeping-android

#Debug
./gradlew.bat :app:assembleDebug

# Release
./gradlew.bat :app:assembleRelease

# 清空并重新编译
./gradlew.bat clean :app:assembleDebug
```

### 运行

```bash
# 部署到模拟器/真机 (需配置 Android Studio)
Android Studio > Run App

# 或命令行
./gradlew.bat :app:installDebug
```

---

## 📦 依赖清单

| 库                     | 版本   | 用途                   |
| ---------------------- | ------ | ---------------------- |
| androidx.room          | 2.6.1  | 本地数据库             |
| androidx.work          | 2.9.1  | 后台同步               |
| com.google.dagger:hilt | 2.51.1 | 依赖注入               |
| com.squareup.retrofit2 | 2.11.0 | HTTP 客户端            |
| com.squareup.moshi     | 1.15.2 | JSON 序列化 (反射模式) |
| androidx.compose       | 1.7.0  | UI 框架                |

---

## ✅ 测试检查清单

- [ ] 新增支出，列表即时刷新
- [ ] 金额验证（≤0 时显示错误提示）
- [ ] 分类选择器正常工作，选中状态正确显示
- [ ] 日期选择器（当前日期 vs 修改后日期）
- [ ] CSV 文件导入，解析无误
- [ ] CSV 导进入成功率统计准确
- [ ] 后台同步日志正常（adb logcat | grep SyncWorker）
- [ ] 网络断开时，outbox 正常排队；恢复时自动重试
- [ ] release 构建仅支持 HTTPS；debug 支持 HTTP

---

**项目完成日期**: 2026-02-22  
**编译状态**: ✅ BUILD SUCCESSFUL  
**推荐下一步**: 真机测试 + targetSdk 36 兼容性体检

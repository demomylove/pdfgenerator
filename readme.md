# PDF生成安卓应用设计方案与计划

## 1. 需求总结

### 1.1 核心功能
*   通过应用内相机拍照生成PDF（支持离线）。
*   从设备图库选择图片转换为PDF（支持离线）。

### 1.2 内购模式
*   **免费版：**
    *   基础PDF生成功能。
    *   可能包含广告。
    *   PDF生成可能有数量或页数限制（具体细节待定，初步考虑不限制，通过广告和高级功能付费区分）。
*   **高级版（付费解锁）：**
    *   更高的图片质量选项。
    *   云存储集成（例如 Google Drive, Dropbox）用于导入图片。
    *   可自定义的PDF设置（例如页面大小、方向、压缩级别）。
    *   无广告体验。

### 1.3 技术规格
*   最低安卓版本：Android 6.0 (Marshmallow, API 23)。
*   目标安卓版本：Android 13.0 (Tiramisu, API 33)。

### 1.4 设计与体验
*   遵循最新的 Material You Design 设计规范，界面现代、美观。
*   核心PDF生成功能必须支持完全离线操作。

## 2. 初步设计方案

应用将包含以下主要模块：

*   **用户界面 (UI) 设计：** 基于 Material You Design，设计直观易用的界面流程，使用 Jetpack Compose 构建。
*   **核心PDF生成模块：** 实现拍照转PDF和图片转PDF的功能。
*   **图像处理模块：** 处理图片选择、预览、编辑（如裁剪、旋转）、质量调整等。
*   **PDF自定义模块 (高级版)：** 实现页面大小、方向、压缩级别、元数据编辑等设置。
*   **内购模块：** 集成 Google Play Billing Library 实现内购功能，管理用户订阅状态。
*   **云存储集成模块 (高级版)：** 实现与Google Drive、Dropbox等服务的API对接，进行文件授权和选择。
*   **权限管理模块：** 优雅地处理相机、存储（Scoped Storage）、网络等权限请求。
*   **广告模块（免费版）：** 集成广告SDK（如 Google AdMob），合理展示广告。

## 3. 技术选型初步考虑

*   **编程语言：** Kotlin
*   **架构模式：** MVVM (Model-View-ViewModel) 或 MVI (Model-View-Intent)
*   **UI框架：** Jetpack Compose
*   **异步处理：** Kotlin Coroutines + Flow
*   **依赖注入：** Hilt
*   **图像加载与处理：** Coil (Kotlin优先) 或 Glide
*   **PDF生成库：**
    *   Android `PdfDocument` API (原生，适用于基础功能，无额外授权费用)
    *   iText 7 Core (功能强大，但需注意其 AGPL 授权或购买商业授权)
    *   Apache PDFBox - Android Port (开源，功能较全面)
    *   *初步倾向于先使用原生 `PdfDocument` API 满足核心需求，高级自定义功能再评估引入第三方库的可行性与成本。*
*   **相机：** CameraX (Jetpack库，简化相机开发)
*   **文件选择：** Photo Picker (Android 13+), Storage Access Framework (兼容旧版本)
*   **网络请求 (云服务集成)：** Retrofit + OkHttp
*   **数据存储 (用户偏好等)：** Jetpack DataStore (替代SharedPreferences)

## 4. Mermaid 图表 - 高层架构

```mermaid
graph TD
    A[用户界面 (Jetpack Compose, Material You)] --> B{功能选择};
    B -- 拍照 (CameraX) --> C[图像预览/编辑];
    B -- 图库选择 (Photo Picker/SAF) --> C;
    B -- 云存储导入 (高级版, Retrofit) --> C;

    C --> D{图像处理};
    D -- 基础调整 --> E[PDF生成引擎 (PdfDocument API)];
    D -- 高级质量 (高级版) --> E;

    E --> F[PDF参数设置];
    F -- 基础参数 --> G[生成的PDF文件];
    F -- 自定义参数 (高级版) --> G;

    H[内购模块 (Google Play Billing)] --> I{用户订阅状态};
    I -- 免费用户 --> J[广告模块 (AdMob)];
    I -- 高级用户 --> K[解锁高级功能];

    L[权限管理 (相机, 存储等)] --> A;
    L --> B;

    subgraph "核心处理流程"
        C; D; E; F; G;
    end

    subgraph "用户交互与支持"
        A; B; L;
    end

    subgraph "商业化与扩展"
        H; I; J; K;
    end
```

## 5. 下一步计划

1.  **详细功能拆解：** 将每个模块的功能点进一步细化到具体的用户故事或任务。
2.  **UI/UX 原型设计：** 使用Figma或类似工具创建关键页面的线框图和高保真原型，遵循Material You指南。
3.  **技术选型确认：** 针对PDF生成库、图像处理库等关键技术点进行更深入的调研和最终选择。
4.  **数据库设计（如果需要）：** 明确是否需要本地数据库存储PDF元数据、用户设置等，并设计表结构。
5.  **API接口定义（云存储）：** 明确与云存储服务交互所需的API端点和数据格式。
6.  **开发任务分解与排期：** 将开发任务分配到各个迭代周期，并预估时间。
7.  **测试策略制定：** 包括单元测试、集成测试和UI测试。

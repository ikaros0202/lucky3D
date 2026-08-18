# Static Caibao Page Implementation Plan

> **历史执行说明（2026-08-12 更新）：** 原 Superpowers 子技能依赖已退役。本文仅作为历史实施计划保留；复用时遵循仓库根 `AGENTS.md`、当前 Skill 基线和任务状态，不调用未安装 Skill。

**Goal:** Add “彩报” as the fifth bottom-navigation destination and display one bundled, read-only newspaper placeholder image without any update or action capability.

**Architecture:** A pure `MainTab` model defines the five stable destinations. The app root uses Navigation 3 for the root destination and saveable tab selection inside a Material 3 `NavigationBar`; the彩报 content is a focused stateless composable backed only by an APK resource.

**Tech Stack:** Kotlin 2.4.10, Jetpack Compose Material 3, Navigation 3 1.1.4, Android resources, JUnit 4, Truth.

## Global Constraints

- Package: `com.lucky3d.app`.
- Platform: Android phone, `minSdk 26`, `compileSdk 37`, `targetSdk 37`.
- Bottom destinations in order: 首页、走势、选号、方案、彩报.
- 彩报 must never perform a network request or expose refresh, paging, favorite, share, download, purchase, or recommendation actions.
- The bundled image is a static placeholder sourced from Swello on Unsplash under the Unsplash License.
- All bottom targets are at least `48dp`; labels remain visible and TalkBack receives meaningful descriptions.

---

### Task 1: Lock the five-destination contract

**Files:**
- Create: `app/src/main/java/com/lucky3d/app/app/navigation/MainTab.kt`
- Test: `app/src/test/java/com/lucky3d/app/app/navigation/MainTabTest.kt`

**Interfaces:**
- Produces: `enum class MainTab(val label: String)` with entries `HOME`, `TREND`, `PICK`, `PLANS`, `CAIBAO`.

- [x] **Step 1: Write the failing contract test**

```kotlin
assertThat(MainTab.entries.map(MainTab::label))
    .containsExactly("首页", "走势", "选号", "方案", "彩报")
    .inOrder()
```

- [x] **Step 2: Run the test and verify it fails because `MainTab` does not exist**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*.MainTabTest"
```

- [x] **Step 3: Add the minimal enum with the exact ordered labels**

```kotlin
enum class MainTab(val label: String) {
    HOME("首页"), TREND("走势"), PICK("选号"), PLANS("方案"), CAIBAO("彩报")
}
```

- [x] **Step 4: Re-run the focused test and require PASS**

- [x] **Step 5: Review the files for the next repository commit; leave staging to the user**

### Task 2: Bundle the licensed static placeholder

**Files:**
- Create: `app/src/main/res/drawable-nodpi/caibao_placeholder.jpg`
- Create: `docs/third-party-assets.md`

**Interfaces:**
- Produces: drawable resource `R.drawable.caibao_placeholder`.

- [x] **Step 1: Download the fixed-resolution Unsplash image URL**

```powershell
Invoke-WebRequest "https://images.unsplash.com/photo-1643967254338-475e748b6488?auto=format&fit=max&fm=jpg&q=80&w=1200" -OutFile app/src/main/res/drawable-nodpi/caibao_placeholder.jpg
```

- [x] **Step 2: Record author, source page, license URL, purpose, and download URL**

- [x] **Step 3: Verify the file is a decodable JPEG and record its SHA-256**

### Task 3: Implement the root shell and static page

**Files:**
- Create: `app/src/main/java/com/lucky3d/app/app/navigation/AppNavigation.kt`
- Create: `app/src/main/java/com/lucky3d/app/feature/caibao/CaibaoScreen.kt`
- Modify: `app/src/main/java/com/lucky3d/app/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/com/lucky3d/app/app/navigation/AppNavigationTest.kt`

**Interfaces:**
- Consumes: `MainTab.entries`, `R.drawable.caibao_placeholder`.
- Produces: `@Composable fun AppNavigation()` and `@Composable fun CaibaoScreen(modifier: Modifier = Modifier)`.

- [x] **Step 1: Add a Compose test that selects “彩报” and asserts the static disclaimer and image semantics**

```kotlin
composeRule.onNodeWithText("彩报").performClick()
composeRule.onNodeWithText("当前为静态示意内容，不会自动更新").assertIsDisplayed()
composeRule.onNodeWithContentDescription("彩报静态占位图：桌面上的报纸").assertExists()
```

- [x] **Step 2: Run the Android test compilation and verify failure because the navigation/page does not exist**

```powershell
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

- [x] **Step 3: Implement one Navigation 3 root, saveable selected-tab state, five labeled `NavigationBarItem`s, four neutral placeholder screens, and `CaibaoScreen`**

- [x] **Step 4: Re-run unit tests, Android test compilation, lint, and debug assembly**

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
```

- [x] **Step 5: Review implementation and evidence for the next repository commit; leave staging to the user**

### Task 4: Device acceptance

**Files:**
- Modify: `tasks/todo.md`

**Interfaces:**
- Consumes: debug APK and AVDs `XinYue_API26`, `XinYue_API37`.
- Produces: verified five-tab and offline彩报 behavior.

- [x] **Step 1: Install and launch the debug APK on API 26**
- [x] **Step 2: Switch through all five tabs and verify彩报 remains visible in airplane/offline conditions**
- [x] **Step 3: Repeat the navigation and large-font check on API 37**
- [x] **Step 4: Confirm there are no彩报 refresh/action controls and no crash in logcat**
- [x] **Step 5: Mark only the verified彩报 acceptance items complete**

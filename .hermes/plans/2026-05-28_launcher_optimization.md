# Daemon Launcher Optimization Plan

**Date:** 2026-05-28  
**Project:** IDK-launcher (Daemon)  
**Goal:** Pure optimization without breaking features or compromising functionality

---

## Executive Summary

This plan outlines safe, non-breaking optimizations for the Daemon Android launcher. The focus is on performance improvements, code quality, and maintainability while preserving all existing features.

---

## Current Architecture Overview

### Tech Stack
- **Language:** Kotlin
- **Min SDK:** 21, **Target SDK:** 35
- **Architecture:** Single-activity with fragments
- **UI:** View system (not Compose), ViewBinding
- **Preferences:** Custom preference system with serialization
- **Build:** Gradle with version catalogs

### Key Components
1. **HomeActivity** - Main launcher UI, gesture handling
2. **SettingsActivity** - Tabbed settings with carousels and ribbons
3. **UIObject** - Interface for theme/window flag handling
4. **PremiumRibbonSlider** - Custom slider view
5. **ModernColorPickerBottomSheet** - HSB+Alpha color picker

---

## Optimization Strategy

### Phase 1: Code Quality & Maintainability

#### 1.1 Preference System Optimization
**Files:** `preferences/*.kt`

- **Issue:** Preference system uses string-based keys, no compile-time safety
- **Optimization:** Add type-safe preference delegates
- **Risk:** Low - additive changes

```kotlin
// Before: String-based keys
LauncherPreferences.theme().font()

// After: Type-safe delegates
LauncherPreferences.theme.font { 
    get() // returns Font
    set(value) // type-safe
}
```

#### 1.2 View Binding Consistency
**Files:** All UI files

- **Issue:** Mixed usage of findViewById and ViewBinding
- **Optimization:** Standardize on ViewBinding
- **Risk:** Low - requires audit but no behavior change

#### 1.3 String Resource Optimization
**Files:** `res/values/strings.xml`

- **Issue:** May have duplicated or non-optimized strings
- **Optimization:** Consolidate, add quantity strings for plurals
- **Risk:** Low - resource-only changes

---

### Phase 2: Performance Optimizations

#### 2.1 RecyclerView Performance
**Files:** `ui/list/*.kt`, widgets

- **Current:** Uses RecyclerView with adapters
- **Optimization:**
  - Add `setHasFixedSize(true)` where applicable
  - Use `DiffUtil` for list updates
  - Consider view holder pooling for frequently updated lists

#### 2.2 Bitmap & Image Handling
**Files:** `apps/AppIconFetcher.kt`, widgets

- **Issue:** Icons loaded repeatedly
- **Optimization:**
  - Implement L1 (memory) + L2 (disk) cache with size limits
  - Use `LruCache` for icon cache
  - Pre-scale bitmaps to required dimensions

#### 2.3 Background Thread Management
**Files:** `actions/*.kt`, `widgets/*.kt`

- **Issue:** Potential main thread blocking
- **Optimization:**
  - Ensure icon loading runs on background threads
  - Use `CoroutineScope(Dispatchers.IO)` for file operations
  - Consider `ExecutorService` for parallel operations

---

### Phase 3: Memory & Resource Optimization

#### 3.1 Drawable Resources
**Files:** `res/drawable/*`

- **Issue:** May have oversized drawables
- **Optimization:**
  - Audit all drawables for proper sizing (xxxhdpi only for launcher icons)
  - Use `drawable-anydpi` for vector drawables
  - Remove unused drawables

#### 3.2 String Allocation Reduction
**Files:** `ui/views/PremiumRibbonSlider.kt`, `ui/settings/*.kt`

- **Issue:** String formatting in draw methods
- **Optimization:**
  - Cache formatted strings when possible
  - Use `SparseArray` for indexed lookups
  - Avoid object allocation in `onDraw()`

---

### Phase 4: UI/UX Improvements (Safe)

#### 4.1 Slider Value Formatting
**Files:** `ui/views/PremiumRibbonSlider.kt`

- **Issue:** No value labels on sliders
- **Optimization:** Add optional value TextView that updates with slider
- **Risk:** Low - additive feature

#### 4.2 Color Picker UX
**Files:** `ui/settings/ModernColorPickerBottomSheet.kt`

- **Issue:** Hex input doesn't handle # prefix gracefully
- **Optimization:** Auto-format hex output, handle paste events
- **Risk:** Low - UX improvement

---

### Phase 5: Build & Build Time Optimization

#### 5.1 Gradle Configuration
**Files:** `app/build.gradle.kts`, `gradle.properties`

- **Optimization:**
  - Enable build cache
  - Configure `kotlin.incremental=true`
  - Use `gradle build --configuration-cache`

```kotlin
// gradle.properties
kotlin.incremental=true
kotlin.incremental.js=true
android.enableBuildCache=true
```

#### 5.2 Dependency Optimization
**Files:** `libs.versions.toml`

- **Issue:** May have unused transitive dependencies
- **Optimization:**
  - Run dependency analysis
  - Remove unused dependencies
  - Enable ProGuard/R8 optimization

---

## Files Likely to Change

### High Priority (Low Risk)
| File | Change Type | Risk |
|------|-------------|------|
| `gradle.properties` | Add build flags | Low |
| `res/values/strings.xml` | Consolidate strings | Low |
| `ui/views/PremiumRibbonSlider.kt` | Minor UX | Low |
| `ui/settings/ModernColorPickerBottomSheet.kt` | Hex formatting | Low |

### Medium Priority
| File | Change Type | Risk |
|------|-------------|------|
| `preferences/*.kt` | Add delegates | Medium |
| `apps/AppIconFetcher.kt` | Cache implementation | Medium |
| `ui/list/*.kt` | DiffUtil | Medium |

### Low Priority (Consider)
| File | Change Type | Risk |
|------|-------------|------|
| `ui/HomeActivity.kt` | Minor cleanup | Low |
| `ui/settings/SettingsActivity.kt` | Extract helpers | Low |

---

## Verification Steps

1. **Unit Tests:** Run existing tests (if any)
2. **Manual Testing:**
   - App launch time
   - Scrolling performance
   - Memory usage (Android Studio Profiler)
   - Theme switching
   - Color picker workflow
3. **Build Verification:**
   - `./gradlew assembleDebug --configuration-cache`
   - `./gradlew lint`
   - APK size comparison

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Preference migration breaks | Use backward-compatible preference readers |
| Cache causes memory issues | Set size limits, clear on low memory |
| Build cache corruption | Clear build cache if issues occur |
| String resource changes break translations | Keep all existing keys, only add new ones |

---

## Non-Goals (Out of Scope)

- No feature additions
- No UI redesign
- No architecture rewrite
- No Compose migration
- No new dependencies

---

## Estimated Effort

- **Phase 1:** 2-3 hours
- **Phase 2:** 3-4 hours
- **Phase 3:** 1-2 hours
- **Phase 4:** 1-2 hours
- **Phase 5:** 1 hour

**Total:** ~8-12 hours of development time

---

## Next Steps

1. Review this plan
2. Start with Phase 1 (lowest risk)
3. Proceed incrementally, verifying after each change
4. Measure performance before/after each phase
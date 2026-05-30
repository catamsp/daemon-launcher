package com.catamsp.Daemon.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import org.xmlpull.v1.XmlPullParser

data class IconPack(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

data class IconPackIcon(
    val drawableName: String,
    val thumbnail: Drawable?
)

class IconPackManager private constructor(private val context: Context) {

    private var activePackPackageName: String? = null
    private var iconPackResources: Resources? = null
    private var iconPackContext: Context? = null
    private var componentToDrawable: MutableMap<ComponentName, String> = mutableMapOf()
    private val drawableCache = LruCache<String, Drawable>(200)

    // Fallback compositing layers from appfilter.xml
    private var iconBackName: String? = null
    private var iconMaskName: String? = null
    private var iconUponName: String? = null
    private var scaleFactor: Float = 1.0f

    fun getInstalledIconPacks(): List<IconPack> {
        val pm = context.packageManager
        val packs = mutableListOf<IconPack>()

        val intentActions = listOf(
            "org.adw.ActivityStarter.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme"
        )

        val resolvedPackages = mutableSetOf<String>()

        for (action in intentActions) {
            val intent = android.content.Intent(action)
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (ri in resolveInfos) {
                val pkgName = ri.activityInfo?.packageName ?: continue
                if (resolvedPackages.add(pkgName)) {
                    val label = ri.loadLabel(pm).toString()
                    val icon = try { ri.loadIcon(pm) } catch (_: Exception) { null }
                    packs.add(IconPack(label, pkgName, icon))
                }
            }
        }

        return packs.sortedBy { it.label }
    }

    fun loadIconPack(packageName: String): Boolean {
        return try {
            val packCtx = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            val packRes = packCtx.resources

            val appfilterId = packRes.getIdentifier("appfilter", "xml", packageName)
            if (appfilterId == 0) {
                android.util.Log.w("IconPackManager", "appfilter.xml not found in $packageName")
                return false
            }

            componentToDrawable.clear()
            drawableCache.evictAll()
            iconBackName = null
            iconMaskName = null
            iconUponName = null
            scaleFactor = 1.0f

            val parser = packRes.getXml(appfilterId)
            parseAppFilter(parser)

            activePackPackageName = packageName
            iconPackResources = packRes
            iconPackContext = packCtx

            android.util.Log.i("IconPackManager", "Loaded icon pack: $packageName (${componentToDrawable.size} icons, fallback: back=$iconBackName mask=$iconMaskName upon=$iconUponName scale=$scaleFactor)")
            true
        } catch (e: Exception) {
            android.util.Log.e("IconPackManager", "Failed to load icon pack: $packageName", e)
            false
        }
    }

    fun getIcon(componentName: ComponentName, originalIcon: Drawable? = null): Drawable? {
        val packRes = iconPackResources ?: return null
        val packPkg = activePackPackageName ?: return null

        // 1. Exact match
        val exact = componentToDrawable[componentName]
        if (exact != null) return getDrawableByName(exact)

        // 2. Fallback compositing
        if (originalIcon != null && hasFallbackLayers()) {
            return getFallbackIcon(originalIcon)
        }

        return null
    }

    fun getDrawableByName(drawableName: String): Drawable? {
        val packRes = iconPackResources ?: return null
        val packPkg = activePackPackageName ?: return null

        drawableCache.get(drawableName)?.let { return it }

        return try {
            val resId = packRes.getIdentifier(drawableName, "drawable", packPkg)
            if (resId == 0) return null
            val drawable = packRes.getDrawable(resId, null) ?: return null
            drawableCache.put(drawableName, drawable)
            drawable
        } catch (e: Exception) {
            null
        }
    }

    fun getAvailableIcons(): List<IconPackIcon> {
        val packRes = iconPackResources ?: return emptyList()
        val packPkg = activePackPackageName ?: return emptyList()

        val drawableId = packRes.getIdentifier("drawable", "xml", packPkg)
        if (drawableId == 0) return emptyList()

        return try {
            val parser = packRes.getXml(drawableId)
            parseDrawableXml(parser)
        } catch (e: Exception) {
            android.util.Log.e("IconPackManager", "Failed to parse drawable.xml", e)
            emptyList()
        }
    }

    fun isLoaded(): Boolean = activePackPackageName != null && iconPackResources != null

    fun getActivePackPackage(): String? = activePackPackageName

    fun clearCache() {
        drawableCache.evictAll()
        componentToDrawable.clear()
        activePackPackageName = null
        iconPackResources = null
        iconPackContext = null
        iconBackName = null
        iconMaskName = null
        iconUponName = null
        scaleFactor = 1.0f
    }

    private fun hasFallbackLayers(): Boolean {
        return iconBackName != null || iconMaskName != null
    }

    private fun getFallbackIcon(originalIcon: Drawable): Drawable? {
        val packRes = iconPackResources ?: return null
        val packPkg = activePackPackageName ?: return null

        val size = 192
        val sizef = size.toFloat()
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1. Draw background layer (iconback)
        val backDrawable = iconBackName?.let { getDrawableByName(it) }
        if (backDrawable != null) {
            backDrawable.setBounds(0, 0, size, size)
            backDrawable.draw(canvas)
        } else {
            // Fallback: fill with white if no background defined
            val bgPaint = android.graphics.Paint().apply { color = Color.WHITE; style = android.graphics.Paint.Style.FILL }
            canvas.drawRect(0f, 0f, sizef, sizef, bgPaint)
        }

        // 2. Draw original icon scaled and masked
        val iconSize = (size * scaleFactor).toInt().coerceAtLeast(1)
        val left = (size - iconSize) / 2
        val top = (size - iconSize) / 2

        val maskDrawable = iconMaskName?.let { getDrawableByName(it) }

        if (maskDrawable != null) {
            // Save layer for masking compositing
            val saveCount = canvas.saveLayer(0f, 0f, sizef, sizef, null)

            // Draw original icon
            originalIcon.setBounds(left, top, left + iconSize, top + iconSize)
            originalIcon.draw(canvas)

            // Apply mask using DST_IN (keeps icon pixels where mask is opaque)
            maskDrawable.setBounds(0, 0, size, size)
            maskDrawable.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.DST_IN)
            maskDrawable.draw(canvas)

            canvas.restoreToCount(saveCount)
        } else {
            // No mask — draw icon as-is, centered
            originalIcon.setBounds(left, top, left + iconSize, top + iconSize)
            originalIcon.draw(canvas)
        }

        // 3. Draw overlay layer (iconupon)
        val uponDrawable = iconUponName?.let { getDrawableByName(it) }
        if (uponDrawable != null) {
            uponDrawable.setBounds(0, 0, size, size)
            uponDrawable.draw(canvas)
        }

        return BitmapDrawable(context.resources, result)
    }

    private fun parseAppFilter(parser: XmlPullParser) {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "item" -> {
                        val component = parser.getAttributeValue(null, "component")
                        val drawableName = parser.getAttributeValue(null, "drawable")
                        if (component != null && drawableName != null) {
                            val flatComponent = component.removePrefix("ComponentInfo{").removeSuffix("}")
                            val cn = ComponentName.unflattenFromString(flatComponent)
                            if (cn != null) {
                                componentToDrawable[cn] = drawableName
                            }
                        }
                    }
                    "iconback" -> {
                        // Try img, img1, img2, etc. — use the first one found
                        if (iconBackName == null) {
                            iconBackName = parser.getAttributeValue(null, "img")
                                ?: parser.getAttributeValue(null, "img1")
                        }
                    }
                    "iconmask" -> {
                        if (iconMaskName == null) {
                            iconMaskName = parser.getAttributeValue(null, "img")
                                ?: parser.getAttributeValue(null, "img1")
                        }
                    }
                    "iconupon" -> {
                        if (iconUponName == null) {
                            iconUponName = parser.getAttributeValue(null, "img")
                                ?: parser.getAttributeValue(null, "img1")
                        }
                    }
                    "scale" -> {
                        scaleFactor = parser.getAttributeValue(null, "factor")?.toFloatOrNull() ?: 1.0f
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseDrawableXml(parser: XmlPullParser): List<IconPackIcon> {
        val icons = mutableListOf<IconPackIcon>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name == "item") {
                    val drawableName = parser.getAttributeValue(null, "drawable")
                    if (drawableName != null) {
                        // Don't load thumbnails eagerly — too many icons causes OOM
                        icons.add(IconPackIcon(drawableName, null))
                    }
                }
            }
            eventType = parser.next()
        }
        return icons
    }

    companion object {
        @Volatile
        private var instance: IconPackManager? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = IconPackManager(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(context: Context): IconPackManager {
            return instance ?: throw IllegalStateException("IconPackManager not initialized. Call init() first.")
        }
    }
}

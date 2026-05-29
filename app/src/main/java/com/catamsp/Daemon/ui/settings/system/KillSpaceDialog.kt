package com.catamsp.Daemon.ui.settings.system

import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catamsp.Daemon.R
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.preferences.LauncherPreferences
import kotlinx.coroutines.*

data class RunningProcess(
    val packageName: String,
    val name: String,
    val ram: String,
    val cpu: String,
    val isPersistent: Boolean = false
)

class KillSpaceDialog(context: Context) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar) {

    private lateinit var recycler: RecyclerView
    private lateinit var loaderText: TextView
    private val processList = mutableListOf<RunningProcess>()
    private lateinit var adapter: KillSpaceAdapter
    private val dialogScope = CoroutineScope(Dispatchers.Main + Job())
    private var totalRamKb: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.dialog_kill_space, null)
        setContentView(view)

        window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            it.setDimAmount(0f)
            it.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            LauncherPreferences.theme().background().applyToWindow(it)
        }

        // Find the root container of the Activity to hide everything beneath the dialog
        val activity = (context as? android.app.Activity)
            ?: (context as? android.view.ContextThemeWrapper)?.baseContext as? android.app.Activity

        val rootContainer = activity?.findViewById<View>(R.id.home_container)
        rootContainer?.visibility = View.INVISIBLE

        setOnDismissListener {
            rootContainer?.visibility = View.VISIBLE
        }

        (context as? UIObject)?.applyFont(view)

        recycler = view.findViewById(R.id.kill_space_recycler)
        loaderText = view.findViewById(R.id.kill_space_loader_text)

        // Apply theme accent color to the loader
        val typedValue = android.util.TypedValue()
        if (context.theme.resolveAttribute(androidx.appcompat.R.attr.colorAccent, typedValue, true)) {
            loaderText.setTextColor(typedValue.data)
        }

        adapter = KillSpaceAdapter(processList) { pkg -> killProcess(pkg) }
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter

        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        totalRamKb = memInfo.totalMem / 1024

        startBrailleAnimation()
        fetchProcesses(isInitialLoad = true)
    }

    private fun startBrailleAnimation() {
        val brailleChars = arrayOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
        dialogScope.launch {
            var i = 0
            while (isActive) {
                loaderText.text = brailleChars[i]
                i = (i + 1) % brailleChars.size
                delay(80)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        dialogScope.cancel()
    }

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    private fun executeShell(command: String): String {
        return try {
            val cmdArray = arrayOf("sh", "-c", command)
            val newProcessMethod = rikka.shizuku.Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(
                null,
                cmdArray,
                null,
                null
            ) as rikka.shizuku.ShizukuRemoteProcess

            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val output = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseRamToKb(ramStr: String): Double {
        return try {
            val cleanStr = ramStr.trim().uppercase()
            when {
                cleanStr.endsWith("G") -> cleanStr.dropLast(1).toDouble() * 1024 * 1024
                cleanStr.endsWith("M") -> cleanStr.dropLast(1).toDouble() * 1024
                cleanStr.endsWith("K") -> cleanStr.dropLast(1).toDouble()
                else -> cleanStr.toDouble()
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private fun fetchProcesses(isInitialLoad: Boolean = false) {
        if (isInitialLoad) {
            loaderText.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        }

        dialogScope.launch(Dispatchers.IO) {
            // Using top to get CPU and Memory (RSS)
            val output = executeShell("top -b -n 1")
            val lines = output.split("\n")
            val pm = context.packageManager
            val apps = mutableListOf<RunningProcess>()

            val persistentPrefixes = listOf(
                "com.vivo.",
                "com.iqoo.",
                "com.bbk.",
                "com.samsung.",
                "com.sec.",
                "com.miui.",
                "com.coloros.",
                "com.oppo.",
                "com.oneplus.",
                "com.google.android.gms"
            )

            for (line in lines) {
                val trimmed = line.trim().replace(Regex("\\s+"), " ")
                val parts = trimmed.split(" ")
                if (parts.size >= 12 && parts.last().contains(".")) {
                    val pkg = parts.last()
                    val cpu = parts[8] // %CPU is usually 9th col
                    val ramRaw = parts[5] // RES/RAM is usually 6th col

                    if (pkg == context.packageName || pkg.startsWith("com.android.") || pkg.startsWith(
                            "android."
                        )
                    ) continue

                    try {
                        val info = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                        val label = pm.getApplicationLabel(info).toString()

                        val ramKb = parseRamToKb(ramRaw)
                        val ramPercent = if (totalRamKb > 0) (ramKb / totalRamKb) * 100 else 0.0
                        val ramDisplay =
                            if (ramRaw.contains(Regex("[A-Za-z]"))) ramRaw else "${ramRaw}K"
                        val ramWithPercent = String.format("%s (%.1f%%)", ramDisplay, ramPercent)

                        val isPersistent = persistentPrefixes.any { pkg.startsWith(it) }

                        // Avoid duplicates from multiple threads
                        if (apps.none { it.packageName == pkg }) {
                            apps.add(RunningProcess(pkg, label, ramWithPercent, "$cpu%", isPersistent))
                        }
                    } catch (e: Exception) {
                    }
                }
            }

            apps.sortBy { it.name }

            withContext(Dispatchers.Main) {
                processList.clear()
                processList.addAll(apps)
                adapter.notifyDataSetChanged()
                if (isInitialLoad) {
                    loaderText.visibility = View.GONE
                    recycler.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun killProcess(packageName: String) {
        dialogScope.launch(Dispatchers.IO) {
            executeShell("am force-stop $packageName")
            withContext(Dispatchers.Main) {
                // Refresh silently without showing the loader
                fetchProcesses(isInitialLoad = false)
            }
        }
    }

    inner class KillSpaceAdapter(
        private val items: List<RunningProcess>,
        private val onKill: (String) -> Unit
    ) : RecyclerView.Adapter<KillSpaceAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.app_name)
            val stats: TextView = view.findViewById(R.id.app_stats)
            val btnKill: ImageView = view.findViewById(R.id.btn_kill)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_kill_space_row, parent, false)
            (context as? UIObject)?.applyFont(view)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            holder.stats.text = "RAM: ${item.ram}  |  CPU: ${item.cpu}"
            
            if (item.isPersistent) {
                holder.btnKill.visibility = View.INVISIBLE
            } else {
                holder.btnKill.visibility = View.VISIBLE
                holder.btnKill.setOnClickListener { 
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onKill(item.packageName) 
                }
            }
        }

        override fun getItemCount() = items.size
    }
}

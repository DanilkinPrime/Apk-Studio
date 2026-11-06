package com.example.composeworkbench

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                WorkbenchApp()
            }
        }
    }
}

// ───────────────────────── Data ─────────────────────────

data class Project(
    val appName: String,
    val packageName: String,
    val minSdk: Int
)

data class DesignItem(
    val id: Int,
    val type: String,
    val name: String,
    val text: String,
    val fontSize: Int = 18,
    val colorHex: String = "#222222",
    val padding: Int = 8,
    val x: Int = 20,
    val y: Int = 20,
    val width: Int = 220,
    val height: Int = 64
)

/** One row in the Properties panel: Name + editable Value. */
class Prop(
    val name: String,
    val value: String,
    val numeric: Boolean,
    val apply: (DesignItem, String) -> DesignItem?
)

val MIN_VERSIONS = listOf(
    "Android 7.0" to 24,
    "Android 8.0" to 26,
    "Android 9" to 28,
    "Android 10" to 29,
    "Android 11" to 30,
    "Android 12" to 31
)

private val HEX_REGEX = Regex("^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

fun propsFor(item: DesignItem): List<Prop> = buildList {
    add(Prop("Name", item.name, false) { i, v -> i.copy(name = v) })
    add(Prop("Text", item.text, false) { i, v -> i.copy(text = v) })

    if (item.type == "Text") {
        add(Prop("FontSize", item.fontSize.toString(), true) { i, v ->
            v.toIntOrNull()?.takeIf { it in 1..200 }?.let { i.copy(fontSize = it) }
        })
        add(Prop("Color", item.colorHex, false) { i, v ->
            if (HEX_REGEX.matches(v)) i.copy(colorHex = v) else null
        })
    }

    add(Prop("X", item.x.toString(), true) { i, v ->
        v.toIntOrNull()?.takeIf { it >= 0 }?.let { i.copy(x = it) }
    })
    add(Prop("Y", item.y.toString(), true) { i, v ->
        v.toIntOrNull()?.takeIf { it >= 0 }?.let { i.copy(y = it) }
    })
    add(Prop("Width", item.width.toString(), true) { i, v ->
        v.toIntOrNull()?.takeIf { it >= 1 }?.let { i.copy(width = it) }
    })
    add(Prop("Height", item.height.toString(), true) { i, v ->
        v.toIntOrNull()?.takeIf { it >= 1 }?.let { i.copy(height = it) }
    })
    add(Prop("Padding", item.padding.toString(), true) { i, v ->
        v.toIntOrNull()?.takeIf { it >= 0 }?.let { i.copy(padding = it) }
    })
}

// ───────────────────────── Helpers ─────────────────────────

fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)

        when (clean.length) {
            6 -> Color(0xFF000000 or value)
            8 -> Color(value)
            else -> Color.Black
        }
    } catch (_: Exception) {
        Color.Black
    }
}

fun String.toColorLiteral(): String {
    val hex = removePrefix("#")

    return when (hex.length) {
        6 -> "0xFF$hex"
        8 -> "0x$hex"
        else -> "0xFFFFFFFF"
    }
}

fun kotlinString(value: String): String {
    return "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("$", "\\$")
        .replace("\n", "\\n") + "\""
}

fun xmlEscape(value: String): String {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
}

fun sanitizeId(name: String): String {
    val cleaned = name.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }
    return when {
        cleaned.isEmpty() -> "app"
        cleaned[0].isDigit() -> "app$cleaned"
        else -> cleaned
    }
}

fun sanitizeFileName(name: String): String {
    val cleaned = name.trim().map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }
        .joinToString("")
    return cleaned.ifEmpty { "project" }
}

fun makePackageName(appName: String): String = "com.example." + sanitizeId(appName)

// ───────────────────────── Code generation ─────────────────────────

fun generateScreenSource(
    packageName: String,
    items: List<DesignItem>,
    background: String
): String = buildString {
    appendLine("package $packageName")
    appendLine()
    appendLine("import androidx.compose.foundation.background")
    appendLine("import androidx.compose.foundation.layout.*")
    appendLine("import androidx.compose.material3.*")
    appendLine("import androidx.compose.runtime.Composable")
    appendLine("import androidx.compose.ui.Modifier")
    appendLine("import androidx.compose.ui.graphics.Color")
    appendLine("import androidx.compose.ui.unit.dp")
    appendLine("import androidx.compose.ui.unit.sp")
    appendLine()
    appendLine("@Composable")
    appendLine("fun MyScreen() {")
    appendLine("    Box(")
    appendLine("        modifier = Modifier")
    appendLine("            .fillMaxSize()")
    appendLine("            .background(Color(${background.toColorLiteral()}))")
    appendLine("    ) {")

    items.forEach { item ->
        val comment = item.name.replace("\n", " ")
        appendLine("        // $comment")
        appendLine("        Box(")
        appendLine("            modifier = Modifier")
        appendLine("                .offset(${item.x}.dp, ${item.y}.dp)")
        appendLine("                .size(${item.width}.dp, ${item.height}.dp)")
        appendLine("                .padding(${item.padding}.dp)")
        appendLine("        ) {")

        when (item.type) {
            "Text" -> {
                appendLine("            Text(")
                appendLine("                text = ${kotlinString(item.text)},")
                appendLine("                fontSize = ${item.fontSize}.sp,")
                appendLine("                color = Color(${item.colorHex.toColorLiteral()})")
                appendLine("            )")
            }

            "Button" -> {
                appendLine("            Button(onClick = {}) {")
                appendLine("                Text(${kotlinString(item.text)})")
                appendLine("            }")
            }

            "TextField" -> {
                appendLine("            OutlinedTextField(")
                appendLine("                value = \"\",")
                appendLine("                onValueChange = {},")
                appendLine("                placeholder = { Text(${kotlinString(item.text)}) },")
                appendLine("                modifier = Modifier.fillMaxSize()")
                appendLine("            )")
            }
        }

        appendLine("        }")
        appendLine()
    }

    appendLine("    }")
    appendLine("}")
}

fun buildProjectFiles(
    project: Project,
    items: List<DesignItem>,
    background: String
): Map<String, String> {
    val pkg = project.packageName
    val rootName = sanitizeFileName(project.appName)
    val files = LinkedHashMap<String, String>()

    files["settings.gradle.kts"] = """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "$rootName"
include(":app")
""".trimStart()

    files["build.gradle.kts"] = """
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
""".trimStart()

    files["gradle.properties"] = """
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
""".trimStart()

    files["app/build.gradle.kts"] = """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "$pkg"
    compileSdk = 34

    defaultConfig {
        applicationId = "$pkg"
        minSdk = ${project.minSdk}
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
""".trimStart()

    files["app/src/main/AndroidManifest.xml"] = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
""".trimStart()

    files["app/src/main/res/values/strings.xml"] = """
<resources>
    <string name="app_name">${xmlEscape(project.appName)}</string>
</resources>
""".trimStart()

    val srcDir = "app/src/main/java/" + pkg.replace('.', '/')

    files["$srcDir/MainActivity.kt"] = """
package $pkg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MyScreen()
            }
        }
    }
}
""".trimStart()

    files["$srcDir/MyScreen.kt"] = generateScreenSource(pkg, items, background)

    files[".github/workflows/build.yml"] = """
name: Build APK

on:
  push:
    branches: [ main, master ]
  pull_request:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 8.9

      - name: Build debug APK
        run: gradle assembleDebug --stacktrace

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: $rootName-debug-apk
          path: app/build/outputs/apk/debug/*.apk
""".trimStart()

    files[".gitignore"] = """
*.iml
.gradle/
local.properties
.idea/
build/
captures/
.externalNativeBuild/
.cxx/
""".trimStart()

    files["README.md"] = """
# ${project.appName}

Generated by Compose Workbench.

- Package: `$pkg`
- Min SDK: ${project.minSdk}

## Build with GitHub Actions
1. Create a new GitHub repository and push this folder to the `main` branch.
2. Open the **Actions** tab and wait for **Build APK** to finish
   (or start it manually with **Run workflow**).
3. Download the APK from the **Artifacts** section of the run.

## Build locally
Open the folder in Android Studio, or run `gradle assembleDebug`.
""".trimStart()

    return files
}

fun writeProjectZip(
    out: OutputStream,
    project: Project,
    items: List<DesignItem>,
    background: String
) {
    ZipOutputStream(out).use { zip ->
        buildProjectFiles(project, items, background).forEach { (path, content) ->
            zip.putNextEntry(ZipEntry(path))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
}

// ───────────────────────── UI: root ─────────────────────────

@Composable
fun WorkbenchApp() {
    var project by remember { mutableStateOf<Project?>(null) }

    val current = project
    if (current == null) {
        HomeScreen(onCreate = { project = it })
    } else {
        BackHandler { project = null }
        EditorScreen(project = current, onClose = { project = null })
    }
}

// ───────────────────────── UI: home ─────────────────────────

@Composable
fun HomeScreen(onCreate: (Project) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101318)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "COMPOSE WORKBENCH",
                color = Color(0xFF9DB7FF),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                "Visual UI designer",
                color = Color.LightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
            Button(onClick = { showDialog = true }) {
                Text("Create project")
            }
        }
    }

    if (showDialog) {
        CreateProjectDialog(
            onDismiss = { showDialog = false },
            onCreate = {
                showDialog = false
                onCreate(it)
            }
        )
    }
}

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (Project) -> Unit
) {
    var appName by remember { mutableStateOf("My App") }
    var minSdk by remember { mutableIntStateOf(28) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New project") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("APK / app name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Package: ${makePackageName(appName)}",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Text("Minimum Android version", fontWeight = FontWeight.Bold)

                MIN_VERSIONS.forEach { (label, api) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = minSdk == api,
                            onClick = { minSdk = api }
                        )
                        Text("$label (API $api)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = appName.isNotBlank(),
                onClick = {
                    onCreate(
                        Project(
                            appName = appName.trim(),
                            packageName = makePackageName(appName),
                            minSdk = minSdk
                        )
                    )
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ───────────────────────── UI: properties ─────────────────────────

@Composable
fun PropertyRow(
    prop: Prop,
    ownerId: Int,
    onValue: (String) -> Unit
) {
    // Numeric values follow external changes (dragging on canvas);
    // text values keep their own state so the cursor doesn't jump.
    var text by remember(ownerId, prop.name, if (prop.numeric) prop.value else "") {
        mutableStateOf(prop.value)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            prop.name,
            color = Color(0xFF9DB7FF),
            fontSize = 13.sp,
            modifier = Modifier.weight(0.4f)
        )

        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onValue(it)
            },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (prop.numeric) KeyboardType.Number else KeyboardType.Text
            ),
            modifier = Modifier
                .weight(0.6f)
                .background(Color(0xFF262D3A), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF3A4458), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

// ───────────────────────── UI: editor ─────────────────────────

@Composable
fun EditorScreen(project: Project, onClose: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val items = remember { mutableStateListOf<DesignItem>() }
    var nextId by remember { mutableIntStateOf(1) }
    var selectedId by remember { mutableIntStateOf(-1) }
    var showSource by remember { mutableStateOf(false) }
    var screenBackground by remember { mutableStateOf("#FFFFFF") }

    val selected = items.find { it.id == selectedId }

    fun addItem(type: String) {
        val id = nextId
        items.add(
            DesignItem(
                id = id,
                type = type,
                name = "$type$id",
                text = when (type) {
                    "Button" -> "Button"
                    "TextField" -> "Enter text"
                    else -> "New text"
                },
                x = 30,
                y = 30 + (items.size % 8) * 20
            )
        )
        selectedId = id
        nextId = id + 1
    }

    fun updateItem(item: DesignItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) items[index] = item
    }

    val source = remember(items.toList(), screenBackground) {
        generateScreenSource(project.packageName, items.toList(), screenBackground)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    writeProjectZip(out, project, items.toList(), screenBackground)
                }
                Toast.makeText(context, "Project exported", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101318))
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) { Text("←") }

            Column(Modifier.weight(1f)) {
                Text(
                    project.appName,
                    color = Color(0xFF9DB7FF),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    "${project.packageName} · min API ${project.minSdk}",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            TextButton(onClick = { showSource = !showSource }) {
                Text(if (showSource) "Designer" else "Source")
            }

            Button(
                onClick = {
                    exportLauncher.launch(sanitizeFileName(project.appName) + ".zip")
                }
            ) {
                Text("Export ZIP", fontSize = 12.sp)
            }
        }

        if (showSource) {
            Text(
                "MyScreen.kt (generated)",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Text(
                source,
                color = Color(0xFFB7D8FF),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            )
        } else {
            // Object palette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Text", "Button", "TextField").forEach { type ->
                    Button(
                        onClick = { addItem(type) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ $type", fontSize = 11.sp)
                    }
                }
            }

            Text(
                "Tap an object to select it. Drag to move, drag the blue handle to resize.",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
            )

            // Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(parseHexColor(screenBackground))
            ) {
                val canvasWidth = this.maxWidth
                val canvasHeight = this.maxHeight

                items.forEach { item ->
                    val isSelected = item.id == selectedId

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    with(density) { item.x.dp.roundToPx() },
                                    with(density) { item.y.dp.roundToPx() }
                                )
                            }
                            .size(width = item.width.dp, height = item.height.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        2.dp,
                                        Color(0xFF4B8BFF),
                                        RoundedCornerShape(6.dp)
                                    )
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        Color(0x33000000),
                                        RoundedCornerShape(6.dp)
                                    )
                                }
                            )
                            .pointerInput(item.id) {
                                detectDragGestures(
                                    onDragStart = { selectedId = item.id }
                                ) { change, dragAmount ->
                                    change.consume()

                                    val cur = items.find { it.id == item.id }
                                        ?: return@detectDragGestures

                                    val dx = with(density) { dragAmount.x.toDp().value }
                                    val dy = with(density) { dragAmount.y.toDp().value }

                                    val maxX = (canvasWidth.value - cur.width)
                                        .coerceAtLeast(0f).toInt()
                                    val maxY = (canvasHeight.value - cur.height)
                                        .coerceAtLeast(0f).toInt()

                                    updateItem(
                                        cur.copy(
                                            x = (cur.x + dx).roundToInt().coerceIn(0, maxX),
                                            y = (cur.y + dy).roundToInt().coerceIn(0, maxY)
                                        )
                                    )
                                }
                            }
                            .padding(item.padding.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (item.type) {
                            "Text" -> Text(
                                text = item.text,
                                fontSize = item.fontSize.sp,
                                color = parseHexColor(item.colorHex)
                            )

                            "Button" -> Button(onClick = { selectedId = item.id }) {
                                Text(item.text)
                            }

                            "TextField" -> OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                placeholder = { Text(item.text) },
                                enabled = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .background(Color(0xFF4B8BFF), RoundedCornerShape(4.dp))
                                    .pointerInput(item.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()

                                            val cur = items.find { it.id == item.id }
                                                ?: return@detectDragGestures

                                            val dx = with(density) { dragAmount.x.toDp().value }
                                            val dy = with(density) { dragAmount.y.toDp().value }

                                            val maxW = (canvasWidth.value - cur.x)
                                                .coerceAtLeast(40f).toInt()
                                            val maxH = (canvasHeight.value - cur.y)
                                                .coerceAtLeast(32f).toInt()

                                            updateItem(
                                                cur.copy(
                                                    width = (cur.width + dx).roundToInt()
                                                        .coerceIn(40, maxW),
                                                    height = (cur.height + dy).roundToInt()
                                                        .coerceIn(32, maxH)
                                                )
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("↘", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // Properties panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .background(Color(0xFF1B2029))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Properties",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected != null) {
                        TextButton(
                            onClick = {
                                items.removeAll { it.id == selected.id }
                                selectedId = -1
                            }
                        ) {
                            Text("Delete", color = Color(0xFFFF7777))
                        }
                    }
                }

                Text(
                    "Name",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(2.dp))

                if (selected == null) {
                    Text(
                        "Select an object to edit its properties.",
                        color = Color.LightGray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        "${selected.type} · ID ${selected.id}",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    propsFor(selected).forEach { prop ->
                        PropertyRow(prop = prop, ownerId = selected.id) { newValue ->
                            val cur = items.find { it.id == selected.id }
                            if (cur != null) {
                                prop.apply(cur, newValue)?.let { updateItem(it) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text("Screen", color = Color.White, fontWeight = FontWeight.Bold)

                PropertyRow(
                    prop = Prop("Background", screenBackground, false) { i, _ -> i },
                    ownerId = -100
                ) { v ->
                    if (HEX_REGEX.matches(v)) screenBackground = v
                }
            }
        }
    }
}

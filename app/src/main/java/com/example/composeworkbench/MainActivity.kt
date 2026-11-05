package com.example.composeworkbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                DesignerApp()
            }
        }
    }
}

data class DesignItem(
    val id: Int,
    val type: String,
    val text: String,
    val fontSize: Int = 18,
    val colorHex: String = "#222222",
    val padding: Int = 8,
    val x: Int = 20,
    val y: Int = 20,
    val width: Int = 220,
    val height: Int = 64
)

fun parseHexColor(hex: String): Color {
    return try {
        val value = hex.removePrefix("#").toLong(16)

        when (hex.removePrefix("#").length) {
            6 -> Color(0xFF000000 or value)
            8 -> Color(value)
            else -> Color.Black
        }
    } catch (_: Exception) {
        Color.Black
    }
}

fun kotlinString(value: String): String {
    return "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n") + "\""
}

@Composable
fun DesignerApp() {
    val items = remember {
        mutableStateListOf(
            DesignItem(
                id = 1,
                type = "Text",
                text = "Hello, Compose!",
                fontSize = 28,
                x = 30,
                y = 30,
                width = 280,
                height = 70
            ),
            DesignItem(
                id = 2,
                type = "Button",
                text = "Get started",
                x = 60,
                y = 120
            )
        )
    }

    var nextId by remember { mutableIntStateOf(3) }
    var selectedId by remember { mutableIntStateOf(1) }
    var showSource by remember { mutableStateOf(false) }
    var screenBackground by remember {
        mutableStateOf("#FFFFFF")
    }

    val selected = items.find { it.id == selectedId }

    fun addItem(type: String) {
        val item = DesignItem(
            id = nextId,
            type = type,
            text = when (type) {
                "Button" -> "Button"
                "TextField" -> "Enter text"
                else -> "New text"
            },
            x = 30,
            y = 200
        )

        items.add(item)
        selectedId = nextId
        nextId++
    }

    fun updateItem(item: DesignItem) {
        val index = items.indexOfFirst {
            it.id == item.id
        }

        if (index >= 0) {
            items[index] = item
        }
    }

    val source = remember(
        items.toList(),
        screenBackground
    ) {
        buildString {
            appendLine("package com.example.myapp")
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
            appendLine("            .background(Color(${screenBackground.toColorLiteral()}))")
            appendLine("    ) {")

            items.forEach { item ->
                appendLine("        Box(")
                appendLine("            modifier = Modifier")
                appendLine("                .offset(${item.x}.dp, ${item.y}.dp)")
                appendLine("                .size(${item.width}.dp, ${item.height}.dp)")
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
                        appendLine("                placeholder = { Text(${kotlinString(item.text)}) }")
                        appendLine("            )")
                    }
                }

                appendLine("        }")
                appendLine()
            }

            appendLine("    }")
            appendLine("}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101318))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "COMPOSE WORKBENCH",
                    color = Color(0xFF9DB7FF),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Visual UI designer",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            TextButton(
                onClick = {
                    showSource = !showSource
                }
            ) {
                Text(
                    if (showSource) "Designer" else "Source"
                )
            }
        }

        if (showSource) {
            Text(
                "Generated Kotlin Compose source",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Text",
                    "Button",
                    "TextField"
                ).forEach { type ->
                    Button(
                        onClick = {
                            addItem(type)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "+ $type",
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Text(
                "Drag objects to move them. Drag the blue handle to resize.",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp)
            )

            val density = LocalDensity.current

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(parseHexColor(screenBackground))
            ) {
                val canvasWidth = maxWidth
                val canvasHeight = maxHeight

                items.forEach { item ->
                    val isSelected = item.id == selectedId

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    with(density) {
                                        item.x.dp.roundToPx()
                                    },
                                    with(density) {
                                        item.y.dp.roundToPx()
                                    }
                                )
                            }
                            .size(
                                width = item.width.dp,
                                height = item.height.dp
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        2.dp,
                                        Color(0xFF4B8BFF),
                                        RoundedCornerShape(6.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .pointerInput(item.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        selectedId = item.id
                                    }
                                ) { change, dragAmount ->
                                    change.consume()

                                    val current = items.find {
                                        it.id == item.id
                                    } ?: return@detectDragGestures

                                    val dx = with(density) {
                                        dragAmount.x.toDp().value
                                    }

                                    val dy = with(density) {
                                        dragAmount.y.toDp().value
                                    }

                                    val maxX = (
                                        canvasWidth.value -
                                            current.width
                                        ).coerceAtLeast(0f)

                                    val maxY = (
                                        canvasHeight.value -
                                            current.height
                                        ).coerceAtLeast(0f)

                                    updateItem(
                                        current.copy(
                                            x = (
                                                current.x + dx
                                                ).roundToInt()
                                                .coerceIn(0, maxX.toInt()),
                                            y = (
                                                current.y + dy
                                                ).roundToInt()
                                                .coerceIn(0, maxY.toInt())
                                        )
                                    )
                                }
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (item.type) {
                            "Text" -> {
                                Text(
                                    text = item.text,
                                    fontSize = item.fontSize.sp,
                                    color = parseHexColor(item.colorHex)
                                )
                            }

                            "Button" -> {
                                Button(
                                    onClick = {
                                        selectedId = item.id
                                    }
                                ) {
                                    Text(item.text)
                                }
                            }

                            "TextField" -> {
                                OutlinedTextField(
                                    value = "",
                                    onValueChange = {},
                                    placeholder = {
                                        Text(item.text)
                                    },
                                    enabled = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .background(
                                        Color(0xFF4B8BFF),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .pointerInput(item.id) {
                                        detectDragGestures {
                                                change,
                                                dragAmount ->
                                            change.consume()

                                            val current = items.find {
                                                it.id == item.id
                                            } ?: return@detectDragGestures

                                            val dx = with(density) {
                                                dragAmount.x.toDp().value
                                            }

                                            val dy = with(density) {
                                                dragAmount.y.toDp().value
                                            }

                                            val maxWidth = (
                                                canvasWidth.value -
                                                    current.x
                                                ).coerceAtLeast(40f)

                                            val maxHeight = (
                                                canvasHeight.value -
                                                    current.y
                                                ).coerceAtLeast(32f)

                                            updateItem(
                                                current.copy(
                                                    width = (
                                                        current.width + dx
                                                        ).roundToInt()
                                                        .coerceIn(
                                                            40,
                                                            maxWidth.toInt()
                                                        ),
                                                    height = (
                                                        current.height + dy
                                                        ).roundToInt()
                                                        .coerceIn(
                                                            32,
                                                            maxHeight.toInt()
                                                        )
                                                )
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "↘",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .background(Color(0xFF1B2029))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "Properties",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (selected == null) {
                    Text(
                        "Select an object to edit it.",
                        color = Color.LightGray
                    )
                } else {
                    Text(
                        "${selected.type} · ID ${selected.id}",
                        color = Color(0xFF9DB7FF),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    OutlinedTextField(
                        value = selected.text,
                        onValueChange = {
                            updateItem(
                                selected.copy(text = it)
                            )
                        },
                        label = {
                            Text("Text / placeholder")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (selected.type == "Text") {
                        Text(
                            "Font size: ${selected.fontSize} sp",
                            color = Color.White
                        )

                        Slider(
                            value = selected.fontSize.toFloat(),
                            onValueChange = {
                                updateItem(
                                    selected.copy(
                                        fontSize = it.toInt()
                                    )
                                )
                            },
                            valueRange = 10f..48f
                        )

                        OutlinedTextField(
                            value = selected.colorHex,
                            onValueChange = {
                                updateItem(
                                    selected.copy(colorHex = it)
                                )
                            },
                            label = {
                                Text("Text color (hex)")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Text(
                        "Position X: ${selected.x} dp",
                        color = Color.White
                    )

                    Slider(
                        value = selected.x.toFloat(),
                        onValueChange = {
                            updateItem(
                                selected.copy(x = it.toInt())
                            )
                        },
                        valueRange = 0f..500f
                    )

                    Text(
                        "Position Y: ${selected.y} dp",
                        color = Color.White
                    )

                    Slider(
                        value = selected.y.toFloat(),
                        onValueChange = {
                            updateItem(
                                selected.copy(y = it.toInt())
                            )
                        },
                        valueRange = 0f..800f
                    )

                    Text(
                        "Width: ${selected.width} dp",
                        color = Color.White
                    )

                    Slider(
                        value = selected.width.toFloat(),
                        onValueChange = {
                            updateItem(
                                selected.copy(width = it.toInt())
                            )
                        },
                        valueRange = 40f..500f
                    )

                    Text(
                        "Height: ${selected.height} dp",
                        color = Color.White
                    )

                    Slider(
                        value = selected.height.toFloat(),
                        onValueChange = {
                            updateItem(
                                selected.copy(height = it.toInt())
                            )
                        },
                        valueRange = 32f..500f
                    )

                    Text(
                        "Padding: ${selected.padding} dp",
                        color = Color.White
                    )

                    Slider(
                        value = selected.padding.toFloat(),
                        onValueChange = {
                            updateItem(
                                selected.copy(padding = it.toInt())
                            )
                        },
                        valueRange = 0f..48f
                    )

                    TextButton(
                        onClick = {
                            items.removeAll {
                                it.id == selected.id
                            }

                            selectedId =
                                items.firstOrNull()?.id ?: -1
                        }
                    ) {
                        Text(
                            "Delete selected object",
                            color = Color(0xFFFF7777)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Screen background",
                    color = Color.White
                )

                OutlinedTextField(
                    value = screenBackground,
                    onValueChange = {
                        screenBackground = it
                    },
                    label = {
                        Text("Background color")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

fun String.toColorLiteral(): String {
    val hex = removePrefix("#")

    return try {
        when (hex.length) {
            6 -> "0xFF$hex"
            8 -> "0x$hex"
            else -> "0xFFFFFFFF"
        }
    } catch (_: Exception) {
        "0xFFFFFFFF"
    }
}

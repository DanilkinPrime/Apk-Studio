package com.example.composeworkbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Workbench() } }
    }
}

@Composable
fun Workbench() {
    var projectName by remember { mutableStateOf("My Compose App") }
    var screenTitle by remember { mutableStateOf("Hello, Compose!") }
    var buttonText by remember { mutableStateOf("Get started") }
    var backgroundHex by remember { mutableStateOf("#F3F4F6") }
    var corner by remember { mutableFloatStateOf(18f) }
    var padding by remember { mutableFloatStateOf(24f) }
    var tab by remember { mutableIntStateOf(0) }
    var source by remember { mutableStateOf(defaultSource) }

    Column(Modifier.fillMaxSize().background(Color(0xFF101318)).padding(16.dp)) {
        Text("COMPOSE WORKBENCH", color = Color(0xFF9DB7FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(projectName, { projectName = it }, label = { Text("Project name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Design") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Kotlin source") })
        }
        Spacer(Modifier.height(12.dp))
        if (tab == 0) {
            Text("Live preview", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            val previewColor = parseColor(backgroundHex)
            Column(
                Modifier.fillMaxWidth().weight(1f).background(previewColor, RoundedCornerShape(16.dp)).padding(padding.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(screenTitle, color = Color(0xFF1B1B1F), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {}, shape = RoundedCornerShape(corner.dp)) { Text(buttonText) }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(screenTitle, { screenTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(buttonText, { buttonText = it }, label = { Text("Button text") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(backgroundHex, { backgroundHex = it }, label = { Text("Background hex (e.g. #F3F4F6)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("Corner radius: ${corner.toInt()} dp", color = Color.White)
            Slider(value = corner, onValueChange = { corner = it }, valueRange = 0f..36f)
            Text("Content padding: ${padding.toInt()} dp", color = Color.White)
            Slider(value = padding, onValueChange = { padding = it }, valueRange = 8f..40f)
        } else {
            Text("Editable Kotlin starter", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = source, onValueChange = { source = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Compose source (local editor)") },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White)
            )
            Text("This edits the starter text inside the app; copy it into your Android project to build.", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

fun parseColor(value: String): Color = try {
    val hex = value.trim().removePrefix("#")
    val parsed = hex.toLong(16)
    when (hex.length) {
        6 -> Color(0xFF000000 or parsed)
        8 -> Color(parsed)
        else -> Color(0xFFF3F4F6)
    }
} catch (_: Exception) { Color(0xFFF3F4F6) }

private val defaultSource = """@Composable
fun ExampleScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Hello, Compose!", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { /* TODO */ }) {
            Text("Get started")
        }
    }
}"""

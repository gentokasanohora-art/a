package com.emeraldrng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emeraldrng.viewmodel.RngViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF3B82F6), background = Color(0xFF0A1628), surface = Color(0xFF12243D))) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A1628)) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(vm: RngViewModel = viewModel()) {
    val results by vm.results.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val timer by vm.timerState.collectAsState()

    var tid by remember { mutableStateOf("0") }
    var sid by remember { mutableStateOf("0") }
    var onlyShiny by remember { mutableStateOf(false) }
    var targetFrame by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) } // 0:Search 1:Timer

    Column(Modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Emerald RNG Complete", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("全機能完全実装版", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = tab, containerColor = Color(0xFF12243D)) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("検索") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("タイマー") })
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (tab == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = tid, onValueChange = { tid = it }, label = { Text("TID") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = sid, onValueChange = { sid = it }, label = { Text("SID") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = onlyShiny, onCheckedChange = { onlyShiny = it })
                Text("色違いのみ", color = Color.White)
            }
            Button(
                onClick = { vm.search(tid = tid.toIntOrNull() ?: 0, sid = sid.toIntOrNull() ?: 0, onlyShiny = onlyShiny) },
                modifier = Modifier.fillMaxWidth(), enabled = !isLoading
            ) { Text(if (isLoading) "検索中..." else "検索開始") }

            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { s ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF12243D))) {
                        Column(Modifier = Modifier.padding(12.dp)) {
                            Text("Frame: ${s.frame}", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                            Text("${s.natureName} / 特性${s.ability} / ${s.ivString()}", color = Color.White)
                            Text("めざパ: ${s.hpType} ${s.hpPower}  色違い: ${s.shinyType}",
                                color = if (s.isShiny) Color.Yellow else Color.LightGray)
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = targetFrame, onValueChange = { targetFrame = it },
                label = { Text("目標フレーム") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { targetFrame.toLongOrNull()?.let { vm.setTargetFrame(it) } }) { Text("セット") }
                Button(onClick = { vm.startTimer() }, enabled = !timer.isRunning) { Text("開始") }
                Button(onClick = { vm.stopTimer() }, enabled = timer.isRunning) { Text("停止") }
                Button(onClick = { vm.resetTimer() }) { Text("リセット") }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(timer.formattedTime, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("残りフレーム: ${timer.remainingFrames}", color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(progress = { timer.progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
        }
    }
}

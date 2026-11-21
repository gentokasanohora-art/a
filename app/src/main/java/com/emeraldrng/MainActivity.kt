package com.emeraldrng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF3B82F6),
                    background = Color(0xFF0A1628),
                    surface = Color(0xFF12243D)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A1628)
                ) {
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
    var tab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Emerald RNG Complete",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "全機能完全実装版",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = tab,
            containerColor = Color(0xFF12243D)
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("検索") }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("タイマー") }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (tab == 0) {
            SearchTab(
                tid = tid,
                sid = sid,
                onlyShiny = onlyShiny,
                isLoading = isLoading,
                results = results,
                onTidChange = { tid = it },
                onSidChange = { sid = it },
                onOnlyShinyChange = { onlyShiny = it },
                onSearch = {
                    vm.search(
                        tid = tid.toIntOrNull() ?: 0,
                        sid = sid.toIntOrNull() ?: 0,
                        onlyShiny = onlyShiny
                    )
                }
            )
        } else {
            TimerTab(
                targetFrame = targetFrame,
                timer = timer,
                onTargetFrameChange = { targetFrame = it },
                onSet = { targetFrame.toLongOrNull()?.let { vm.setTargetFrame(it) } },
                onStart = { vm.startTimer() },
                onStop = { vm.stopTimer() },
                onReset = { vm.resetTimer() }
            )
        }
    }
}

@Composable
private fun SearchTab(
    tid: String,
    sid: String,
    onlyShiny: Boolean,
    isLoading: Boolean,
    results: List<com.emeraldrng.rng.EmeraldRNG.Spread>,
    onTidChange: (String) -> Unit,
    onSidChange: (String) -> Unit,
    onOnlyShinyChange: (Boolean) -> Unit,
    onSearch: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = tid,
            onValueChange = onTidChange,
            label = { Text("TID") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = sid,
            onValueChange = onSidChange,
            label = { Text("SID") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = onlyShiny, onCheckedChange = onOnlyShinyChange)
        Text(text = "色違いのみ", color = Color.White)
    }
    Button(
        onClick = onSearch,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    ) {
        Text(if (isLoading) "検索中..." else "検索開始")
    }

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(results) { s ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12243D))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Frame: ${s.frame}",
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${s.natureName} / 特性${s.ability} / ${s.ivString()}",
                        color = Color.White
                    )
                    Text(
                        text = "めざパ: ${s.hpType} ${s.hpPower}  色違い: ${s.shinyType}",
                        color = if (s.isShiny) Color.Yellow else Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerTab(
    targetFrame: String,
    timer: RngViewModel.TimerState,
    onTargetFrameChange: (String) -> Unit,
    onSet: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    OutlinedTextField(
        value = targetFrame,
        onValueChange = onTargetFrameChange,
        label = { Text("目標フレーム") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onSet) { Text("セット") }
        Button(onClick = onStart, enabled = !timer.isRunning) { Text("開始") }
        Button(onClick = onStop, enabled = timer.isRunning) { Text("停止") }
        Button(onClick = onReset) { Text("リセット") }
    }
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = timer.formattedTime,
        color = Color.White,
        fontSize = 42.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "残りフレーム: ${timer.remainingFrames}",
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(12.dp))
    LinearProgressIndicator(
        progress = { timer.progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
    )
}

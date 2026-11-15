package com.skinscan.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinscan.app.MainViewModel
import com.skinscan.app.data.HistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val DISCLAIMER =
    "Результат создан нейросетью и носит справочный характер. Это не диагноз и не замена консультации врача. " +
        "При ухудшении состояния или сомнениях обратитесь к специалисту."

@Composable
private fun Header(title: String, subtitle: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HealthAndSafety, null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
        }
    }
}

// ───────────────────────── Главный экран ─────────────────────────

@Composable
fun HomeScreen(vm: MainViewModel, onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("SkinScan", "ИИ-оценка состояния кожи по фото")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().height(280.dp)
            ) {
                val bmp = vm.bitmap
                if (bmp != null) {
                    Image(
                        bmp.asImageBitmap(), "Выбранное фото",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Сделайте или выберите фото", fontWeight = FontWeight.Medium)
                        Text(
                            "Хорошее освещение, участок в фокусе",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onCamera, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Камера")
                }
                FilledTonalButton(onClick = onGallery, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Галерея")
                }
            }

            OutlinedTextField(
                value = vm.note,
                onValueChange = { vm.note = it },
                label = { Text("Жалобы и симптомы (необязательно)") },
                placeholder = { Text("Например: зуд, появилось 3 дня назад") },
                shape = RoundedCornerShape(16.dp),
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            vm.error?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SeverityHigh.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, tint = SeverityHigh)
                        Spacer(Modifier.width(10.dp))
                        Text(msg, color = SeverityHigh, fontSize = 14.sp)
                    }
                }
            }

            Button(
                onClick = { vm.analyze() },
                enabled = vm.bitmap != null && !vm.loading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) {
                if (vm.loading) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Анализируем…", fontSize = 17.sp)
                } else {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Анализировать", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                DISCLAIMER, fontSize = 12.sp, lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ───────────────────────── Результат ─────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
fun ResultScreen(vm: MainViewModel) {
    val a = vm.result ?: return
    BackHandler { vm.newAnalysis() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Результат", "Предварительная оценка нейросети")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TextButton(onClick = { vm.newAnalysis() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Spacer(Modifier.width(6.dp))
                Text("Новый анализ")
            }

            vm.bitmap?.let {
                Image(
                    it.asImageBitmap(), null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp))
                )
            }

            val sc = severityColor(a.severity)
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Surface(shape = RoundedCornerShape(50), color = sc.copy(alpha = 0.15f)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(sc))
                            Spacer(Modifier.width(8.dp))
                            Text(severityLabel(a.severity), color = sc, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Что видно на фото", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(a.summary, lineHeight = 21.sp)
                }
            }

            if (a.seeDoctor) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SeverityMedium.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, null, tint = SeverityMedium)
                        Spacer(Modifier.width(12.dp))
                        Text("Рекомендуется показаться врачу (дерматологу)", fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (a.conditions.isNotEmpty()) {
                SectionTitle("Возможные состояния")
                a.conditions.forEach { c ->
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                if (c.likelihood.isNotBlank()) {
                                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text(
                                            c.likelihood, fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            if (c.description.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(c.description, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (a.recommendations.isNotEmpty()) {
                SectionTitle("Рекомендации")
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        a.recommendations.forEach {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(it, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }

            if (a.urgentSigns.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SeverityHigh.copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = SeverityHigh)
                            Spacer(Modifier.width(8.dp))
                            Text("Срочно к врачу, если:", fontWeight = FontWeight.Bold, color = SeverityHigh)
                        }
                        a.urgentSigns.forEach { Text("•  $it", fontSize = 14.sp, lineHeight = 20.sp) }
                    }
                }
            }

            Text(DISCLAIMER, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { vm.newAnalysis() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Новый анализ") }
        }
    }
}

// ───────────────────────── История ─────────────────────────

@Composable
fun HistoryScreen(vm: MainViewModel, onOpen: (HistoryItem) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("История", "Ваши последние анализы")
        if (vm.history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("Пока пусто", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vm.history, key = { it.id }) { item ->
                    val thumb = remember(item.id) {
                        BitmapFactory.decodeFile(vm.thumbPath(item.id))?.asImageBitmap()
                    }
                    val sc = severityColor(item.analysis.severity)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(item) }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                if (thumb != null) Image(thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(9.dp).clip(CircleShape).background(sc))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(item.id)),
                                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.analysis.conditions.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: "Без явного состояния",
                                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.analysis.summary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { vm.deleteHistory(item.id) }) {
                                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

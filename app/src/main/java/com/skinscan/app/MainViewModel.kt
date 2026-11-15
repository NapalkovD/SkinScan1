package com.skinscan.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skinscan.app.data.AiClient
import com.skinscan.app.data.Analysis
import com.skinscan.app.data.HistoryItem
import com.skinscan.app.data.HistoryStore
import com.skinscan.app.data.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HistoryStore(app)

    var cameraUri: Uri? = null
    var bitmap by mutableStateOf<Bitmap?>(null)
    var note by mutableStateOf("")
    var loading by mutableStateOf(false)
    var result by mutableStateOf<Analysis?>(null)
    var error by mutableStateOf<String?>(null)
    var history by mutableStateOf(store.load())

    fun setImage(uri: Uri) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) { ImageUtils.load(getApplication(), uri) }
            if (bmp == null) {
                error = "Не удалось открыть изображение."
            } else {
                bitmap = bmp
                result = null
                error = null
            }
        }
    }

    fun analyze() {
        val bmp = bitmap ?: return
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val analysis = AiClient.analyze(bmp, note)
                history = withContext(Dispatchers.IO) { store.add(bmp, note, analysis) }
                result = analysis
            } catch (e: Exception) {
                error = e.message ?: "Неизвестная ошибка"
            } finally {
                loading = false
            }
        }
    }

    fun newAnalysis() {
        result = null
        bitmap = null
        note = ""
        error = null
    }

    fun openHistory(item: HistoryItem) {
        bitmap = BitmapFactory.decodeFile(store.thumbFile(item.id).path)
        note = item.note
        result = item.analysis
        error = null
    }

    fun thumbPath(id: Long): String = store.thumbFile(id).path

    fun deleteHistory(id: Long) { history = store.remove(id) }
    fun clearHistory() { history = store.clear() }
}

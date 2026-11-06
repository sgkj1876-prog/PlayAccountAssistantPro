package com.playaccount.assistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Panel = Color(0xFF10263D)
private val Cyan = Color(0xFF35D9F4)
private val Mint = Color(0xFF67E6B5)
data class ChatMessage(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("play_account_assistant", MODE_PRIVATE) }
    private fun openUrl(url: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var backendUrl by remember { mutableStateOf(prefs.getString("backend_url", "") ?: "") }
            var language by remember { mutableStateOf(prefs.getString("language", "ar") ?: "ar") }
            var emailDraft by remember { mutableStateOf("") }
            var accounts by remember { mutableStateOf(prefs.getStringSet("accounts", emptySet())?.toList()?.sorted() ?: emptyList()) }
            var settings by remember { mutableStateOf(false) }
            var showAccounts by remember { mutableStateOf(false) }
            var input by remember { mutableStateOf("") }
            var busy by remember { mutableStateOf(false) }
            var status by remember { mutableStateOf("") }
            val messages = remember { mutableStateListOf(ChatMessage(
                if (language == "ar") "مرحباً! أنا مساعدك الذكي. أشرح إنشاء حساب Google وإعداد متجر Play وأساعدك في فهم رسائل الخطأ. لا ترسل كلمة مرورك أو رمز التحقق." else "Hello! I can guide you through Google signup and Play Store setup. Never share your password or verification code.", false
            )) }
            val scope = rememberCoroutineScope()

            MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Mint, surface = Panel, onPrimary = Color(0xFF06111F))) {
                Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF050D18), Color(0xFF0C2843)))).padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("PLAYACCOUNT", color = Cyan, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Text("ASSISTANT PRO • GEMINI", color = Mint, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { language = if (language == "ar") "en" else "ar"; prefs.edit().putString("language", language).apply() }) { Text(if (language == "ar") "EN" else "عربي", color = Mint) }
                            IconButton(onClick = { settings = !settings }) { Icon(Icons.Default.Settings, null, tint = Cyan) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        MiniAction("تسجيل Google", Icons.Default.PersonAdd, Modifier.weight(1f)) { openUrl("https://accounts.google.com/signup") }
                        MiniAction("حساباتي", Icons.Default.ManageAccounts, Modifier.weight(1f)) { showAccounts = !showAccounts }
                        MiniAction("مساعدة Play", Icons.Default.Public, Modifier.weight(1f)) { openUrl("https://support.google.com/googleplay/answer/7431675?hl=ar") }
                    }
                    if (settings) {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("إعداد اتصال Gemini", color = Mint, fontWeight = FontWeight.Bold)
                                Text("أدخل رابط الخادم المنشور. لا تضع مفتاح Gemini هنا.", color = Color.LightGray, fontSize = 12.sp)
                                OutlinedTextField(value = backendUrl, onValueChange = { backendUrl = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("https://your-service.example.com") })
                                Button(onClick = { prefs.edit().putString("backend_url", backendUrl.trim().trimEnd('/')).apply(); status = "تم حفظ رابط الخادم."; settings = false }, modifier = Modifier.fillMaxWidth()) { Text("حفظ") }
                            }
                        }
                    }
                    if (showAccounts) {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("الحسابات المضافة يدوياً", color = Mint, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = emailDraft, onValueChange = { emailDraft = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("name@gmail.com") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                                    IconButton(onClick = {
                                        val e = emailDraft.trim()
                                        if (e.matches(Regex("^[A-Za-z0-9._%+-]+@gmail\\.com$")) && e !in accounts) {
                                            accounts = (accounts + e).sorted(); prefs.edit().putStringSet("accounts", accounts.toSet()).apply(); emailDraft = ""; status = "تمت إضافة الحساب محلياً."
                                        } else status = "تحقق من عنوان Gmail أو أنه مضاف مسبقاً."
                                    }) { Icon(Icons.Default.AddCircle, null, tint = Cyan) }
                                }
                                accounts.forEach { email ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(email, color = Color.White, fontSize = 13.sp)
                                        IconButton(onClick = { accounts = accounts - email; prefs.edit().putStringSet("accounts", accounts.toSet()).apply() }) { Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFFF8A8A)) }
                                    }
                                }
                                Text("تُحفظ عناوين البريد فقط على الجهاز؛ لا تُحفظ كلمات المرور.", color = Color.LightGray, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.weight(1f).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xCC0C2035)), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Cyan); Spacer(Modifier.width(8.dp))
                                Text("مساعد Gemini الذكي", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f)); Text("PRO", color = Mint, fontSize = 11.sp)
                            }
                            HorizontalDivider(color = Color(0xFF24445F), modifier = Modifier.padding(vertical = 8.dp))
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(messages) { msg ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start) {
                                        Surface(color = if (msg.fromUser) Color(0xFF14506A) else Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 300.dp)) {
                                            Text(msg.text, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                                        }
                                    }
                                }
                                if (busy) item { Text("Gemini يفكر...", color = Mint, fontSize = 12.sp) }
                            }
                            if (status.isNotBlank()) Text(status, color = Mint, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("اكتب سؤالك...") }, maxLines = 4)
                                IconButton(enabled = !busy && input.isNotBlank(), onClick = {
                                    val prompt = input.trim(); input = ""; messages.add(ChatMessage(prompt, true)); busy = true; status = ""
                                    scope.launch { messages.add(ChatMessage(GeminiClient.ask(backendUrl, prompt, language), false)); busy = false }
                                }) { Icon(Icons.Default.Send, null, tint = Cyan) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("التسجيل والتحقق يتمان عبر Google رسمياً. لا تشارك كلمة المرور أو رموز التحقق.", color = Color(0xFF9CB1C5), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun MiniAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
        Icon(icon, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text(label, fontSize = 9.sp)
    }
}

private object GeminiClient {
    suspend fun ask(baseUrl: String, prompt: String, language: String): String {
        if (baseUrl.isBlank()) return "لم يتم ربط خادم Gemini بعد. افتح الإعدادات وأدخل رابط الخادم بعد نشره."
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder().callTimeout(45, java.util.concurrent.TimeUnit.SECONDS).build()
                val json = org.json.JSONObject().put("message", prompt).put("language", language)
                val body = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), json.toString())
                val request = okhttp3.Request.Builder().url(baseUrl.trimEnd('/') + "/api/chat").post(body).build()
                client.newCall(request).execute().use { response ->
                    val raw = response.body()?.string().orEmpty()
                    if (!response.isSuccessful) "تعذر الاتصال بخدمة Gemini (HTTP ${response.code()}). تحقق من إعدادات الخادم."
                    else org.json.JSONObject(raw).optString("reply", "لم يصل رد مفهوم من الخادم.")
                }
            } catch (e: Exception) { "حدث خطأ في الاتصال: ${e.localizedMessage ?: "تحقق من الإنترنت ورابط الخادم."}" }
        }
    }
}

package com.vpntester.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var editUrl: EditText
    private lateinit var textStatus: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ConfigAdapter

    private val configs = mutableListOf<VpnConfig>()
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editUrl = findViewById(R.id.editSubscriptionUrl)
        textStatus = findViewById(R.id.textStatus)
        recycler = findViewById(R.id.recyclerConfigs)

        adapter = ConfigAdapter(configs)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnLoad).setOnClickListener { loadSubscription() }
        findViewById<Button>(R.id.btnTestAll).setOnClickListener { testAll() }
    }

    private fun loadSubscription() {
        val url = editUrl.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, "Вставьте ссылку на подписку", Toast.LENGTH_SHORT).show()
            return
        }

        textStatus.text = "Загрузка..."
        scope.launch {
            try {
                val body = withContext(Dispatchers.IO) { fetchUrl(url) }
                val parsed = ConfigParser.parseSubscriptionBody(body)

                configs.clear()
                configs.addAll(parsed)
                adapter.notifyDataSetChanged()

                textStatus.text = "Загружено конфигов: ${parsed.size}"
                if (parsed.isEmpty()) {
                    textStatus.text = "Не удалось найти ни одного vless/ss конфига в подписке"
                }
            } catch (e: Exception) {
                textStatus.text = "Ошибка загрузки: ${e.message}"
            }
        }
    }

    private fun fetchUrl(urlString: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun testAll() {
        if (configs.isEmpty()) {
            Toast.makeText(this, "Сначала загрузите подписку", Toast.LENGTH_SHORT).show()
            return
        }

        textStatus.text = "Проверка ${configs.size} конфигов..."
        scope.launch {
            val deferred = configs.map { cfg ->
                async(Dispatchers.IO) { ConfigTester.test(cfg) }
            }
            deferred.awaitAll()

            adapter.sortByLatency()
            val working = configs.count { it.latencyMs >= 0 }
            textStatus.text = "Готово. Рабочих: $working из ${configs.size}"
        }
    }
}

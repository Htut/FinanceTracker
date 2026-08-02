package com.financetracker.evolva.data.rates

import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.prefs.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches live FX rates via Frankfurter (ECB-based, no API key).
 * Returns map: foreign code → units of [home] per 1 foreign unit.
 */
object ExchangeRateFetcher {
    suspend fun fetchRatesToHome(home: AppCurrency): Result<Map<String, Double>> =
        withContext(Dispatchers.IO) {
            try {
                val symbols = AppCurrency.entries
                    .filter { it != home }
                    .joinToString(",") { it.code }
                val url = URL(
                    "https://api.frankfurter.app/latest?from=${home.code}&to=$symbols"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 12_000
                    requestMethod = "GET"
                }
                conn.inputStream.bufferedReader().use { reader ->
                    val body = reader.readText()
                    val ratesObj = JSONObject(body).getJSONObject("rates")
                    // API: 1 home = rates[foreign] foreign units
                    // We store: 1 foreign = (1 / rates[foreign]) home units
                    val out = mutableMapOf<String, Double>()
                    ratesObj.keys().forEach { code ->
                        val foreignPerHome = ratesObj.getDouble(code)
                        if (foreignPerHome > 0) {
                            out[code] = 1.0 / foreignPerHome
                        }
                    }
                    // Currencies Frankfurter doesn't cover keep seed defaults
                    val merged = SettingsDataStore.defaultRatesFor(home) + out
                    Result.success(merged.filterKeys { it != home.code })
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

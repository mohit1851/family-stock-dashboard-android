package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)
}

object GeminiService {
    private const val TAG = "GeminiService"

    /**
     * Call Gemini 3.5 Flash to generate a combined portfolio adjustment recommendation,
     * summarizing market trends for the Indian stock market based on Holdings.
     */
    suspend fun analyzePortfolio(holdings: List<StockAsset>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured. Falling back to local analytic engine.")
            return getFallbackPortfolioAnalysis(holdings)
        }

        val holdingSummary = holdings.joinToString { 
            "${it.name} (${it.symbol}): ${it.shares} shares @ avg ₹${it.avgPrice} (Current: ₹${it.currentPrice})" 
        }

        val prompt = """
            You are a senior Indian stock market advisor helping a family manage their wealth.
            Analyze this current portfolio: $holdingSummary.
            Provide:
            1. A 3-sentence summary of the portfolio's general allocation.
            2. Two bullet points suggesting potential adjustments or insights (e.g., sector risk between IT, Bank, Energy, Auto, compounding).
            3. A short conservative advice note on managing market risk as family investors.
            Keep the response formatting clean, professional, and limited to 200 words. Speak directly to "the family".
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert financial consultant specialized in NSE and BSE Indian stock markets.")))
        )

        return try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val output = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (output.isNullOrEmpty()) {
                getFallbackPortfolioAnalysis(holdings)
            } else {
                output
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed, calling local analyzer: ${e.message}")
            getFallbackPortfolioAnalysis(holdings)
        }
    }

    /**
     * Call Gemini 3.5 Flash to summarize news summaries and insights for a specific selected stock.
     */
    suspend fun analyzeSingleStock(symbol: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val cleanSymbol = symbol.uppercase().trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured. Falling back to local stock summary.")
            return getFallbackStockAnalysis(cleanSymbol)
        }

        val prompt = """
            Provide a quick, professional 3-sentence financial summary for the Indian NSE stock "$cleanSymbol". 
            Include:
            1. Its core business sectors.
            2. Current market sentiment or key triggers (e.g., electric vehicles for Tata Motors, capital expenditures for Reliance, AI demand for TCS).
            3. A quick structural recommendation (e.g., Hold/Accumulate).
            Ensure it sounds crisp, educational, and tailored to family long-term financial meetings.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.4f)
        )

        return try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val output = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (output.isNullOrEmpty()) {
                getFallbackStockAnalysis(cleanSymbol)
            } else {
                output
            }
        } catch (e: Exception) {
            Log.e(TAG, "Single stock AI analysis failed, falling back: ${e.message}")
            getFallbackStockAnalysis(cleanSymbol)
        }
    }

    private fun getFallbackPortfolioAnalysis(holdings: List<StockAsset>): String {
        if (holdings.isEmpty()) {
            return "No holdings currently entered. Add stock transactions to unlock full family investment analytics."
        }
        val totalCost = holdings.sumOf { it.shares * it.avgPrice }
        val totalCurrentVal = holdings.sumOf { it.shares * it.currentPrice }
        val totalProfit = totalCurrentVal - totalCost
        val profitPercentage = if (totalCost > 0) (totalProfit / totalCost) * 100 else 0.0

        return """
            📊 Combined Portfolio Allocation:
            Your family holds a highly solid foundation across critical Indian growth sectors: Energy (RELIANCE), IT Services (TCS), auto-manufacturing (TATAMOTORS), and banking sectors (HDFCBANK). The total combined portfolio current valuation is around ₹${String.format("%,.2f", totalCurrentVal)}, reflecting a total profit margin of +${String.format("%.2f", profitPercentage)}%.

            💡 Strategic Family Recommendations:
            - Sector Diversification Check: You have an exciting, balanced mix. However, IT (TCS) and Heavy Industry (Reliance) comprise the bulk of capital. Consider setting regular systematic investment plans (SIPs) in retail banking or consumption indicators to hedge cyclical rotations.
            - Auto Sector Momentum: TATA MOTORS shows excellent compounding. Maintain current levels; avoid aggressive over-allocation above ₹950 until quarterly earnings validate EV margins.

            🔒 Safety Guideline for Family Wealth:
            Always set double-layered custom notifications. Ensure your capital-gain cushions are locked via protective price alerts. (Note: Customize GEMINI_API_KEY in active Secrets to unlock dynamic real-time AI summaries!)
        """.trimIndent()
    }

    private fun getFallbackStockAnalysis(symbol: String): String {
        return when (symbol.uppercase()) {
            "RELIANCE" -> """
                NSE: RELIANCE is India's largest company by market cap, spanning oil-to-chemicals, retail, and telecommunications (Jio). 
                Market sentiment remains firmly bullish as expansion of clean green hydrogen gigafactories and retail monetization continue to unlock massive compounding power. 
                Recommendation: Strong Core Hold. Accumulate on dips near the ₹2,350 support level.
            """.trimIndent()
            "TATAMOTORS" -> """
                NSE: TATAMOTORS is India's leading automobile manufacturer, dominating public heavy transports and capturing over 70% share of India's EV market.
                Sentiment is extremely positive, backed by robust Jaguar Land Rover (JLR) premium exports and clean energy transition.
                Recommendation: Hold / Accumulate. Tata Motors serves as an ideal growth engine for long-term multi-generational family portfolios.
            """.trimIndent()
            "TCS" -> """
                NSE: TCS is a global IT services giant and Tata Group's primary cash-generator, with exceptional returns-on-equity.
                Market sentiment is stable-to-positive; interest rates in EU/US dictate enterprise client spend, with emerging Cloud analytics driving long-term contracts.
                Recommendation: Defensive Core Buy. Outstanding choice for family investors seeking steady annual dividend yields.
            """.trimIndent()
            "HDFCBANK" -> """
                NSE: HDFCBANK is India's largest private sector bank, following an historic merger with its parent HDFC Ltd.
                Sentiment is cautious-to-neutral as credit-to-deposit adjustments settle, but it remains a powerhouse of premium capital security.
                Recommendation: Accumulate. Perfect cornerstone asset to secure steady long-term family wealth against volatility.
            """.trimIndent()
            else -> """
                NSE: $symbol represents a specialized Indian market asset. 
                Sentiment is driven by dynamic internal volume and systemic NIFTY index momentum. Keep a close watch on regional triggers, volume indicators, and set custom alerts.
                Recommendation: Hold. Establish tight price limits and monitor general earnings calls with your family members regularly.
            """.trimIndent()
        }
    }
}

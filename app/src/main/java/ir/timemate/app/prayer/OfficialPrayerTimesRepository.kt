package ir.timemate.app.prayer

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.ByteArrayInputStream

class OfficialPrayerTimesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cache = PrayerTimesCache(appContext)

    suspend fun refreshYear(city: PrayerCity): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val pdfUrl = resolveOfficialPdfUrl(city)
            val response = Jsoup.connect(pdfUrl)
                .ignoreContentType(true)
                .maxBodySize(MAX_PDF_BYTES)
                .timeout(NETWORK_TIMEOUT_MS)
                .execute()
            val bytes = response.bodyAsBytes()
            require(bytes.size in 1..MAX_PDF_BYTES) { "فایل رسمی خالی یا بیش از حد بزرگ است." }
            require(bytes.take(5).toByteArray().toString(Charsets.ISO_8859_1).startsWith("%PDF-")) {
                "پاسخ دریافتی فایل PDF معتبر نیست."
            }

            PDFBoxResourceLoader.init(appContext)
            val extractedText = PDDocument.load(ByteArrayInputStream(bytes)).use { document ->
                require(document.numberOfPages in 10..14) { "تعداد صفحات فایل رسمی غیرمنتظره است." }
                PDFTextStripper().getText(document)
            }
            val days = OfficialPrayerTimesPdfParser.parseText(extractedText, city, pdfUrl)
            require(days.size in 365..366) { "پوشش کامل سال ۱۴۰۵ از فایل رسمی استخراج نشد." }
            require(cache.saveAll(days)) { "ذخیره داده رسمی در حافظه ناموفق بود." }
            true
        }.getOrDefault(false)
    }

    fun cachedDay(city: PrayerCity, dateIso: String): CachedPrayerTimes? = cache.get(city, dateIso)

    private fun resolveOfficialPdfUrl(city: PrayerCity): String {
        val document = Jsoup.connect(PrayerTimesSource.OFFICIAL_PAGE)
            .timeout(NETWORK_TIMEOUT_MS)
            .get()
        val wanted = normalize(city.title)
        val link = document.select("a[href]").firstOrNull { normalize(it.text()) == wanted }
            ?: error("پیوند رسمی PDF برای شهر ${city.title} در صفحه دانشگاه پیدا نشد.")
        val url = link.absUrl("href")
        val uri = java.net.URI(url)
        require(uri.scheme == "https" && uri.host == OFFICIAL_HOST && uri.path.contains(".pdf", ignoreCase = true)) {
            "پیوند شهر به فایل PDF رسمی دانشگاه تهران اشاره نمی‌کند."
        }
        return url
    }

    private fun normalize(value: String): String = value
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace("\u200c", "")
        .replace(" ", "")
        .trim()

    companion object {
        private const val OFFICIAL_HOST = "calendar.ut.ac.ir"
        private const val NETWORK_TIMEOUT_MS = 25_000
        private const val MAX_PDF_BYTES = 8 * 1024 * 1024
    }
}

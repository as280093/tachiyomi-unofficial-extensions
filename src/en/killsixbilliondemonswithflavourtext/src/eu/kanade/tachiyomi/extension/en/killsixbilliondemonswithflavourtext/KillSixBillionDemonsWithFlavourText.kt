package eu.kanade.tachiyomi.extension.en.killsixbilliondemonswithflavourtext

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.util.Base64
import eu.kanade.tachiyomi.lib.dataimage.DataImageInterceptor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.text.WordUtils
import org.jsoup.nodes.Element
import rx.Observable
import java.io.ByteArrayOutputStream

/**
 *  @author Aria Moradi <aria.moradi007@gmail.com>
 */

class KillSixBillionDemonsWithFlavourText : KillSixBillionDemons() {
    override val name = "KSBD: with flavour text"

    override val client: OkHttpClient = OkHttpClient().newBuilder()
        .addInterceptor(DataImageInterceptor())
        .build()

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            title = element.text().substringBefore(" (") + "\nwith flavour text"
            thumbnail_url = "https://dummyimage.com/768x994/000/ffffff.jpg&text=$title"
            artist = "Abbadon"
            author = "Abbadon"
            status = SManga.UNKNOWN
            url = title // this url is not useful at all but must set to something unique or the app breaks!
        }
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val wordpressPages = fetchChapterWordpressPagesList(chapter)

        val chapterPages = mutableListOf<Page>()

        var pageNum = 0
        wordpressPages.forEach { wordpressPage ->
            wordpressPage.select(".post-content .entry a:has(img)").forEach { postImage ->
                chapterPages.add(
                    Page(pageNum, "", postImage.select("img").attr("src"))
                )
                pageNum++
                chapterPages.add(
                    Page(pageNum, postImage.attr("href"))
                )
                pageNum++
            }
        }
        return Observable.just(chapterPages)
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

    override fun imageUrlParse(response: Response): String {
        return flavourTextAsImageURL(response)
    }

    private fun flavourTextAsImageURL(response: Response): String {
        val document = response.asJsoup()
        var flavourTextParagraphs = document.select(".entry").first().select("p").map { el -> el.text() }

        if (flavourTextParagraphs.isEmpty())
            flavourTextParagraphs = listOf("No flavour text for the previous page.")

        val paint = Paint().apply {
            textAlign = Align.CENTER
            color = Color.parseColor("#ea2f45")
            isAntiAlias = true
            textSize = 42f
        }
        val lineSpacing = paint.descent() - paint.ascent()

        val stringBuilder = StringBuilder()

        for (paragraph in flavourTextParagraphs) {
            val p = WordUtils.wrap(paragraph, 50, "\n", true)
            stringBuilder.append(p)
            stringBuilder.append("\n\n")
        }
        val linesToDraw = stringBuilder.split("\n")

        val imageHeight = imageVerticalMargin * 2 + linesToDraw.size * lineSpacing

        val bitmap = Bitmap.createBitmap(imageWidth, imageHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)

        val x = (imageWidth / 2).toFloat()
        var y = imageVerticalMargin.toFloat()
        canvas.save()
        canvas.restore()
        for (line in linesToDraw) {
            canvas.drawText(line, x, y, paint)
            y += lineSpacing
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()

        val encoded = Base64.encodeToString(byteArray, Base64.DEFAULT)

        return "https://127.0.0.1/?image/png;base64,$encoded"
    }

    companion object {
        const val imageWidth = 1500
        const val imageVerticalMargin = 200
    }
}

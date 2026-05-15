package com.example.myapplication.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.dto.post.PostType
import com.example.myapplication.dto.trip.StartingTime
import com.example.myapplication.dto.trip.TransportMode
import com.example.myapplication.dto.trip.TripFeedItemDto
import com.example.myapplication.dto.trip.TripStepDetailDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.tan

@SuppressLint("SetJavaScriptEnabled")
fun Fragment.exportTripToPdf(trip: TripFeedItemDto) {
    val ctx = requireContext()
    val html = buildTripHtml(ctx, trip)
    val jobName = "TravelPath – ${trip.username}"

    val dp16 = (16 * ctx.resources.displayMetrics.density).toInt()
    val progress = ProgressBar(ctx).apply { setPadding(dp16, dp16, dp16, dp16) }
    val loadingDialog = AlertDialog.Builder(ctx)
        .setTitle(R.string.trip_generating)
        .setView(progress)
        .setCancelable(false)
        .create()
    loadingDialog.show()

    val webView = WebView(ctx)
    webView.settings.javaScriptEnabled = true

    webView.addJavascriptInterface(object : Any() {
        @JavascriptInterface
        fun onMapReady() {
            Handler(Looper.getMainLooper()).post {
                loadingDialog.dismiss()
                val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val attrs = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, printAdapter, attrs)
            }
        }
    }, "Android")

    webView.loadDataWithBaseURL("https://tile.openstreetmap.org", html, "text/html", "UTF-8", null)
}

private fun buildTripHtml(ctx: Context, trip: TripFeedItemDto): String {
    val primary = "#E2725B"
    val textPrimary = "#2A2A1A"
    val textSecondary = "#6A6A5A"
    val background = "#F5F5DC"
    val surface = "#FAFAF0"
    val divider = "#DEDED0"

    val weather = "${trip.weather.toWeatherEmoji()} ${trip.weather.toWeatherLabel(ctx)}"
    val duration = trip.totalDuration.toTripDuration(ctx)
    val startingTime = StartingTime.fromApiValue(trip.startingTime)
        ?.let { ctx.getString(it.labelRes) } ?: trip.startingTime
    val transportMode = TransportMode.fromApiValue(trip.transportMode)
        ?.let { ctx.getString(it.labelRes) } ?: trip.transportMode

    val stepsHtml = buildString {
        trip.steps.forEachIndexed { i, step ->
            append(buildStepHtml(ctx, i + 1, step, i < trip.steps.size - 1, primary, textPrimary, textSecondary, surface, divider))
        }
    }

    val startLocHtml = trip.startLocalisation?.let {
        """<div style="display:flex;align-items:center;gap:6px;margin-bottom:20px;">
            <span style="font-size:13px;color:$textSecondary;">📍 ${ctx.getString(R.string.pdf_label_departure)} :</span>
            <span style="font-size:13px;font-weight:600;color:$primary;">${LocalisationFormat.display(it.name).escapeHtml()}</span>
           </div>"""
    } ?: ""

    val exportedOn = ctx.getString(R.string.pdf_exported_on, currentDateHtml())
    val stepsCount = ctx.getString(R.string.trip_result_n_steps, trip.totalStep)
    val cost = ctx.getString(R.string.trip_total_cost, trip.totalCost)
    val footerDetails = ctx.getString(R.string.pdf_footer_details, startingTime, trip.totalStep, trip.totalCost)

    return """<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background:#fff; color:$textPrimary; }
  .page { padding:40px 48px; max-width:794px; margin:0 auto; }

  .header { display:flex; align-items:center; justify-content:space-between; margin-bottom:32px; padding-bottom:20px; border-bottom:2px solid $primary; }
  .brand { font-size:22px; font-weight:800; color:$primary; letter-spacing:-0.5px; }
  .brand span { color:$textPrimary; }
  .trip-author { text-align:right; }
  .trip-author .username { font-size:14px; font-weight:700; color:$textPrimary; }
  .trip-author .date { font-size:12px; color:$textSecondary; margin-top:2px; }

  .meta-grid { display:grid; grid-template-columns:1fr 1fr 1fr 1fr; gap:12px; margin-bottom:28px; }
  .meta-card { background:$background; border-radius:10px; padding:12px 14px; }
  .meta-card .label { font-size:10px; color:$textSecondary; letter-spacing:0.5px; text-transform:uppercase; margin-bottom:4px; }
  .meta-card .value { font-size:14px; font-weight:700; color:$textPrimary; }

  .section-title { font-size:13px; font-weight:700; color:$textSecondary; letter-spacing:0.8px; text-transform:uppercase; margin-bottom:16px; }

  .step { display:flex; gap:16px; margin-bottom:0; position:relative; }
  .step-left { display:flex; flex-direction:column; align-items:center; width:32px; flex-shrink:0; }
  .step-number { width:32px; height:32px; border-radius:50%; background:$primary; color:#fff; font-size:13px; font-weight:800; display:flex; align-items:center; justify-content:center; flex-shrink:0; }
  .step-line { width:2px; background:$divider; flex:1; margin-top:4px; min-height:20px; }
  .step-body { flex:1; background:$surface; border-radius:12px; padding:14px 16px; margin-bottom:12px; border:1px solid $divider; }
  .step-title { font-size:15px; font-weight:700; color:$textPrimary; margin-bottom:4px; }
  .step-location { font-size:12px; color:$primary; font-weight:600; margin-bottom:8px; }
  .step-tags { display:flex; flex-wrap:wrap; gap:6px; margin-bottom:8px; }
  .tag { font-size:11px; background:$background; color:$textSecondary; border-radius:6px; padding:3px 8px; font-weight:500; }
  .step-details { display:flex; gap:16px; margin-top:6px; }
  .step-detail { font-size:11px; color:$textSecondary; }
  .step-detail span { font-weight:600; color:$textPrimary; }

  .travel-connector { display:flex; align-items:center; gap:8px; padding:6px 0 6px 48px; }
  .travel-time { font-size:11px; color:$textSecondary; font-style:italic; }

  .footer { margin-top:32px; padding-top:16px; border-top:1px solid $divider; display:flex; justify-content:space-between; align-items:center; }
  .footer-brand { font-size:11px; color:$textSecondary; }
  .footer-steps { font-size:11px; color:$textSecondary; }
</style>
</head>
<body>
<div class="page">

  <div class="header">
    <div class="brand">Travel<span>Path</span></div>
    <div class="trip-author">
      <div class="username">${trip.username.escapeHtml()}</div>
      <div class="date">$exportedOn</div>
    </div>
  </div>

  <div class="meta-grid">
    <div class="meta-card">
      <div class="label">${ctx.getString(R.string.pdf_label_weather)}</div>
      <div class="value">$weather</div>
    </div>
    <div class="meta-card">
      <div class="label">${ctx.getString(R.string.pdf_label_duration)}</div>
      <div class="value">$duration</div>
    </div>
    <div class="meta-card">
      <div class="label">${ctx.getString(R.string.pdf_label_budget)}</div>
      <div class="value">$cost</div>
    </div>
    <div class="meta-card">
      <div class="label">${ctx.getString(R.string.pdf_label_transport)}</div>
      <div class="value">$transportMode</div>
    </div>
  </div>

  $startLocHtml

  ${buildStaticMapSection(ctx, trip, primary, divider)}

  <div class="section-title">$stepsCount</div>

  $stepsHtml

  <div class="footer">
    <div class="footer-brand">${ctx.getString(R.string.pdf_footer_generated)}</div>
    <div class="footer-steps">$footerDetails</div>
  </div>

</div>
</body>
</html>"""
}

private fun buildStaticMapSection(ctx: Context, trip: TripFeedItemDto, primary: String, divider: String): String {
    data class MapPoint(val lat: Double, val lon: Double, val label: String)

    val points = mutableListOf<MapPoint>()
    trip.startLocalisation?.let { points.add(MapPoint(it.lat, it.long, "D")) }
    trip.steps.forEachIndexed { i, step ->
        points.add(MapPoint(step.localisation.lat, step.localisation.long, "${i + 1}"))
    }
    if (points.isEmpty()) return ""

    val imgW = 700
    val imgH = 300

    fun tileX(lon: Double, z: Int) = (lon + 180.0) / 360.0 * (1 shl z)
    fun tileY(lat: Double, z: Int): Double {
        val r = lat * PI / 180.0
        return (1.0 - ln(tan(r) + 1.0 / cos(r)) / PI) / 2.0 * (1 shl z)
    }

    val minLat = points.minOf { it.lat }
    val maxLat = points.maxOf { it.lat }
    val minLon = points.minOf { it.lon }
    val maxLon = points.maxOf { it.lon }
    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0

    var zoom = 15
    while (zoom > 2) {
        val pxW = (tileX(maxLon, zoom) - tileX(minLon, zoom)) * 256
        val pxH = (tileY(minLat, zoom) - tileY(maxLat, zoom)) * 256
        if (pxW <= imgW - 100 && pxH <= imgH - 100) break
        zoom--
    }

    val cTX = tileX(centerLon, zoom)
    val cTY = tileY(centerLat, zoom)
    fun px(lon: Double) = (imgW / 2.0 + (tileX(lon, zoom) - cTX) * 256).roundToInt()
    fun py(lat: Double) = (imgH / 2.0 + (tileY(lat, zoom) - cTY) * 256).roundToInt()

    val tileCount = 1 shl zoom
    val floorCTX = cTX.toInt()
    val floorCTY = cTY.toInt()
    val tilesHtml = buildString {
        for (ty in (floorCTY - 1)..(floorCTY + 1)) {
            if (ty < 0 || ty >= tileCount) continue
            for (tx in (floorCTX - 2)..(floorCTX + 2)) {
                val wrappedTX = ((tx % tileCount) + tileCount) % tileCount
                val leftPx = ((tx - cTX) * 256 + imgW / 2.0).roundToInt()
                val topPx = ((ty - cTY) * 256 + imgH / 2.0).roundToInt()
                append("""<img class="maptile" src="https://tile.openstreetmap.org/$zoom/$wrappedTX/$ty.png" style="position:absolute;left:${leftPx}px;top:${topPx}px;width:256px;height:256px;"/>""")
            }
        }
    }

    val polylineSvg = if (points.size >= 2) {
        val pts = points.joinToString(" ") { "${px(it.lon)},${py(it.lat)}" }
        """<polyline points="$pts" fill="none" stroke="$primary" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" opacity="0.85"/>"""
    } else ""

    val markersSvg = buildString {
        points.forEach { pt ->
            val x = px(pt.lon)
            val y = py(pt.lat)
            append("""<circle cx="$x" cy="$y" r="11" fill="$primary" stroke="white" stroke-width="2.5"/>""")
            append("""<text x="$x" y="$y" text-anchor="middle" dominant-baseline="central" fill="white" font-size="9" font-weight="bold" font-family="Helvetica,sans-serif">${pt.label}</text>""")
        }
    }

    return """<div style="margin-bottom:28px;">
      <div class="section-title">${ctx.getString(R.string.pdf_map_section)}</div>
      <div style="position:relative;width:${imgW}px;height:${imgH}px;overflow:hidden;border-radius:12px;border:1px solid $divider;">
        $tilesHtml
        <svg viewBox="0 0 $imgW $imgH" style="position:absolute;top:0;left:0;width:${imgW}px;height:${imgH}px;" xmlns="http://www.w3.org/2000/svg">
          $polylineSvg
          $markersSvg
        </svg>
      </div>
    </div>
    <script>
    (function() {
      var imgs = document.querySelectorAll('.maptile');
      if (!imgs.length) { if (window.Android) Android.onMapReady(); return; }
      var done = 0, total = imgs.length;
      function check() { if (++done >= total && window.Android) Android.onMapReady(); }
      imgs.forEach(function(img) { img.onload = img.onerror = check; });
      setTimeout(function() { if (window.Android) Android.onMapReady(); }, 10000);
    })();
    </script>"""
}

private fun buildStepHtml(
    ctx: Context,
    index: Int,
    step: TripStepDetailDto,
    hasNext: Boolean,
    primary: String,
    textPrimary: String,
    textSecondary: String,
    surface: String,
    divider: String
): String {
    val postType = PostType.fromApiValue(step.post.type)
    val typeLabel = postType?.let { ctx.getString(it.labelRes) } ?: step.post.type
    val visitDuration = DateUtils.formatMinutes(ctx, step.visitDuration)
    val visitLabel = ctx.getString(R.string.trip_step_visit_duration, visitDuration)

    val priceHtml = if (step.post.minPrice != null && step.post.maxPrice != null) {
        if (step.post.minPrice == 0 && step.post.maxPrice == 0)
            ctx.getString(R.string.price_free)
        else
            ctx.getString(R.string.price_range_format, step.post.minPrice, step.post.maxPrice)
    } else null

    val tagsHtml = buildString {
        append("""<span class="tag">${typeLabel.escapeHtml()}</span>""")
        if (priceHtml != null) append("""<span class="tag">${priceHtml.escapeHtml()}</span>""")
    }

    val travelHtml = if (hasNext && step.travelTimeFromPrevious > 0) {
        val travelTime = DateUtils.formatMinutes(ctx, step.travelTimeFromPrevious)
        val travelLabel = ctx.getString(R.string.pdf_travel_arrow, travelTime)
        """<div class="travel-connector">
            <div class="travel-time">$travelLabel</div>
           </div>"""
    } else ""

    return """
    <div class="step">
      <div class="step-left">
        <div class="step-number">$index</div>
        ${if (hasNext) """<div class="step-line"></div>""" else ""}
      </div>
      <div style="flex:1;">
        <div class="step-body">
          <div class="step-title">${step.post.title.escapeHtml()}</div>
          <div class="step-location">📍 ${LocalisationFormat.display(step.localisation.name).escapeHtml()}</div>
          <div class="step-tags">$tagsHtml</div>
          <div class="step-details">
            <div class="step-detail">$visitLabel</div>
          </div>
        </div>
        $travelHtml
      </div>
    </div>"""
}

private fun currentDateHtml(): String =
    SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date())

private fun String.escapeHtml(): String = this
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

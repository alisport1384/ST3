package com.alisport.goldpin.core

import kotlin.math.abs
import kotlin.math.max

// ═══════════════════════════════════════════════════════════════════════════════
//  بهینه‌ساز خودکار (نسخهٔ ۱.۴.۴۹)
//
//  چرا: تا امروز «تطبیق» در اپ فقط یک جدول دستی از اعداد آزموده‌شده بود؛ اگر کارمزد
//  کاربر، مود مالی، استراتژی یا بازهٔ داده عوض می‌شد، آن اعداد دیگر بهینه نبودند.
//  این کلاس همان کاری را می‌کند که ما بیرون از اپ با جاروب پارامتر انجام می‌دادیم، ولی
//  **داخل خود اپ** و روی دادهٔ همان گوشی: چند بک‌تست کامل روی بازهٔ انتخابی، با معیار
//  صریح انتخاب برنده.
//
//  معیار انتخاب (score) — عمداً «بیشترین سود» نیست:
//    score = ۱۰۰×انتظار پس‌از‌کارمزد (R)
//            − ۰٫۳۵ × بیشترین افت ٪
//            + ۸ × (سهم ماه‌های مثبت − ۰٫۵)
//            + ۳ اگر بدترین پنجرهٔ ۱۲ماهه منفی نباشد
//            − ۵۰ اگر تعداد معامله کمتر از حد کافی باشد (نتیجهٔ آماری بی‌معنا)
//  یعنی: انتظار مثبت مهم‌تر است، ولی افت عمیق و منحنی ناهموار جریمه می‌شوند؛ دقیقاً همان
//  چیزی که برای «منحنی صعودی» لازم است. عددهای وزنی در [WEIGHTS] مستند شده‌اند.
//
//  جست‌وجو **مختصات‌به‌مختصات** است (نه شبکهٔ کامل) تا روی گوشی قابل اجرا بماند:
//  چند مرحلهٔ کوچک به‌جای ۱۰۰ ترکیب.
// ═══════════════════════════════════════════════════════════════════════════════

object Optimizer {

    /** یک نتیجهٔ کامل بک‌تست — همان سنجه‌هایی که پنل اپ نشان می‌دهد. */
    class Row(
        val label: String,
        /** تنظیماتِ دقیقی که این ردیف با آن اجرا شد (برای «اعمال برنده»). */
        val cfg: Settings,
        val trades: Int,
        val wins: Int,
        val gross: Double,
        val comm: Double,
        val netCost: Double,
        val avgRCost: Double,
        val pf: Double,
        val maxDD: Double,
        val finalEquity: Double,
        val posMonths: Int,
        val months: Int,
        val worst12: Double,
        val score: Double,
        val riskDollars: Double
    ) {
        val winRate: Double get() = if (trades > 0) wins * 100.0 / trades else 0.0
        val ddPct: Double get() = if (finalEquity.isFinite() && riskDollars.isFinite()) 0.0 else 0.0
    }

    /** گزارش یک دور بهینه‌سازی؛ [rows] به ترتیب امتیاز نزولی. */
    class Report(
        val rows: List<Row>,
        val evaluated: Int,
        val seconds: Double,
        val candles: Int,
        val fromT: Long,
        val toT: Long,
        val cmsn: Double,
        val minTrades: Int,
        val cancelled: Boolean,
        /** ارزیابی **خارج از نمونه**: برنده روی ۳۰٪ آخر داده (که در انتخاب دخالت نداشته). */
        val oos: Row? = null,
        /** همان پنجرهٔ خارج از نمونه، ولی با تنظیمات پایهٔ کاربر (برای مقایسه). */
        val oosBase: Row? = null
    ) {
        /** برندهٔ جست‌وجو = بهترین ردیفِ مرحله‌ها (ردیف‌های «خارج از نمونه» جدا هستند). */
        val best: Row? get() = rows.filter { !it.label.contains("خارج از نمونه") }.firstOrNull()

        /** خلاصهٔ متنی برای نمایش در اپ/لاگ (فارسی، بدون قالب‌بندی گرافیکی). */
        fun summary(): String {
            val b = best ?: return "هیچ ترکیبی اجرا نشد"
            return "بهترین: ${b.label} · معاملات ${b.trades} · نرخ برد ${"%.1f".format(b.winRate)}٪ · " +
                "انتظار ${"%.3f".format(b.avgRCost)}R · خالص ${"%.0f".format(b.netCost)} · " +
                "PF ${"%.2f".format(b.pf)} · افت ${"%.0f".format(b.maxDD)} · ماه مثبت ${b.posMonths}/${b.months}" +
                (oos?.let { "\nخارج از نمونه: انتظار ${"%.3f".format(it.avgRCost)}R · خالص ${"%.0f".format(it.netCost)}" +
                    " (تنظیم فعلی: ${oosBase?.let { o -> "%.3f".format(o.avgRCost) + "R / " + "%.0f".format(o.netCost) } ?: "-"})" } ?: "")
        }
    }

    // ── وزن‌های معیار انتخاب (یک‌جا، تا تغییرشان قابل ردیابی باشد) ──────────────
    private const val W_EXPECTANCY = 100.0     // ضریب انتظار پس‌از‌کارمزد بر حسب R
    private const val W_DD = 0.35              // جریمهٔ هر درصد بیشترین افت
    private const val W_CONSISTENCY = 8.0      // پاداش سهم ماه‌های مثبت (منهای ۰٫۵)
    private const val BONUS_NO_BAD_YEAR = 3.0  // پاداش وقتی بدترین پنجرهٔ ۱۲ماهه منفی نباشد
    private const val PENALTY_LOW_SAMPLE = 50.0

    /** حداقل تعداد معامله برای اینکه نتیجه «آماری» حساب شود. */
    fun minTradesFor(candles: Int): Int = when {
        candles > 1_500_000 -> 120
        candles > 600_000 -> 60
        candles > 200_000 -> 25
        else -> 12
    }

    /**
     * اجرای یک بک‌تست کامل و برگرداندن سنجه‌ها.
     *
     * @param cfg تنظیمات (از قبل کپی‌شده؛ این تابع خودش چیزی را عوض نمی‌کند)
     */
    fun evaluate(cfg: Settings, candles: List<Candle>, label: String = ""): Row {
        val eng = Engine(cfg)
        val broker = PaperBroker(cfg)
        eng.broker = broker
        broker.engine = eng
        eng.barCommitHook = { cd -> broker.onBar(cd, live = false) }
        // مثل خود اپ: هر تکه با آخرین کندل بسته پردازش می‌شود تا رفتار یکسان بماند.
        var upto = 0
        while (upto < candles.size) {
            val next = minOf(upto + 25_000, candles.size)
            eng.feed(candles.subList(0, next), lastIsClosed = true)
            upto = next
        }
        val closed = broker.trades.filter { !it.open }.sortedBy { it.exitT }
        var gross = 0.0
        var comm = 0.0
        var riskDollars = 0.0
        var swap = 0.0
        var wins = 0
        val monthly = LinkedHashMap<String, Double>()
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        for (t in closed) {
            val c = t.commission
            gross += t.grossPnl()
            comm += c
            swap += t.swap
            riskDollars += t.risk * t.qty * t.contractSize
            if (t.grossPnl() - c + t.swap > 0.0) wins++
            cal.timeInMillis = t.exitT!!
            val key = "%04d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
            monthly[key] = (monthly[key] ?: 0.0) + (t.grossPnl() - c + t.swap)
        }
        val netCost = gross - comm + swap
        val avgRCost = if (riskDollars > 0.0) netCost / riskDollars else 0.0
        var worst12 = 0.0
        val vals = monthly.values.toList()
        if (vals.size >= 12) {
            worst12 = Double.MAX_VALUE
            for (i in 0..vals.size - 12) {
                var s = 0.0
                for (j in i until i + 12) s += vals[j]
                if (s < worst12) worst12 = s
            }
        } else {
            worst12 = vals.sum()
        }
        val sum = Reports.summary(broker.trades)
        val minTrades = minTradesFor(candles.size)
        val consistency = if (vals.isEmpty()) 0.0 else vals.count { it > 0 }.toDouble() / vals.size
        val ddPct = if (cfg.initialEquity > 0.0) sum.maxDD / cfg.initialEquity * 100.0 else 0.0
        var score = W_EXPECTANCY * avgRCost - W_DD * ddPct + W_CONSISTENCY * (consistency - 0.5)
        if (worst12 >= 0.0) score += BONUS_NO_BAD_YEAR
        if (sum.trades < minTrades) score -= PENALTY_LOW_SAMPLE
        return Row(
            label, cfg, sum.trades, wins, gross, comm, netCost, avgRCost, sum.profitFactor, sum.maxDD,
            broker.equity, vals.count { it > 0 }, vals.size, worst12, score, riskDollars
        )
    }

    /** کارمزد واقعی همان معامله — مستقل از مود مالی (مبنای مقایسهٔ منصفانه). */
    private fun commissionOf(cfg: Settings, t: Trade): Double {
        val c = if (cfg.cmsnAuto && cfg.cmsnAutoValue > 0.0) cfg.cmsnAutoValue else max(0.0, cfg.cmsnManual)
        return c * t.qty * t.contractSize
    }

    private fun baseCopy(base: Settings): Settings = base.copy()

    /**
     * جست‌وجوی مختصات‌به‌مختصات.
     *
     * مرحله‌ها (هر مرحله فقط روی برندهٔ مرحلهٔ قبل می‌چرخد):
     *   ۱) جهت معامله (فقط استراتژی ۱): هم‌جهت یا معکوس
     *   ۲) هندسهٔ حد ضرر: ATR چند برابر (۰٫۷۵ · ۱ · ۱٫۵ · ۲٫۵) در برابر بافر ثابت فعلی
     *   ۳) هدف: k (استراتژی ۱) یا واحد حد سود (استراتژی ۲)
     *   ۴) نگهبان منحنی: خاموش / کاهش ریسک در افت ۱۲٪ / کاهش + توقف در افت ۲۵٪
     *
     * @param progress (شماره، کل، برچسب) — برای نمایش پیشرفت در اپ
     * @param cancel اگر true برگردد، ادامه نمی‌دهد
     */
    fun optimize(
        base: Settings,
        candles: List<Candle>,
        progress: ((Int, Int, String) -> Unit)? = null,
        cancel: () -> Boolean = { false }
    ): Report {
        val t0 = System.currentTimeMillis()
        val split = if (candles.size > 20_000) (candles.size * 0.7).toInt() else candles.size
        val training = candles.subList(0, split)
        val minTrades = minTradesFor(training.size)
        val plans = ArrayList<Pair<String, (Settings) -> Unit>>()
        val baseBuf = base.slBufTicks
        val baseMaxSl = base.maxSlUnits
        val baseTarget = base.trigger2TargetR
        val baseTp = base.tpUnits

        // ── مرحلهٔ ۱: جهت ──────────────────────────────────────────────────────
        val stage1 = ArrayList<Pair<String, (Settings) -> Unit>>()
        if (base.strategy == Strategy.DOC) {
            stage1.add("هم‌جهت ساختار" to { c: Settings -> c.trigger2Fade = false })
            stage1.add("تریگر ۲ معکوس" to { c: Settings -> c.trigger2Fade = true })
        } else {
            stage1.add("بدون روند" to { _: Settings -> })
        }
        // ── مرحلهٔ ۲: هندسه ────────────────────────────────────────────────────
        val stage2 = ArrayList<Pair<String, (Settings) -> Unit>>()
        stage2.add("بافر ثابت ${baseBuf.toInt()} تیک" to { c: Settings ->
            c.slBufMode = 0; c.maxSlMode = 0; c.slBufTicks = baseBuf; c.maxSlUnits = baseMaxSl
        })
        for (m in listOf(0.75, 1.0, 1.5, 2.5)) {
            stage2.add("بافر ${m}×ATR" to { c: Settings ->
                c.slBufMode = 1; c.slAtrMult = m; c.slAtrPeriod = base.slAtrPeriod
                c.maxSlMode = 1; c.maxSlAtrMult = max(2.0, m * 2.5)
            })
        }
        // ── مرحلهٔ ۳: هدف ──────────────────────────────────────────────────────
        val stage3 = ArrayList<Pair<String, (Settings) -> Unit>>()
        if (base.strategy == Strategy.DOC) {
            for (k in listOf(3.0, 5.0, 7.0, 10.0)) {
                stage3.add("هدف ${k.toInt()}R" to { c: Settings -> c.trigger2TargetR = k })
            }
        } else {
            // ⚠ واحدِ «حد سود» هم‌واحد قیمتِ نماد است: ۱۰/۲۵/۵۰ دلار برای طلای ۴٬۰۰۰
            // دلاری درست است ولی روی پوند ۱٫۳ دلاری یعنی ۷۷۰٪ قیمت. پس شبکه **نسبی به
            // مقدار پایهٔ خودِ نماد** ساخته می‌شود؛ برای طلا دقیقاً همان ۱۰/۲۵/۵۰ قدیمی
            // درمی‌آید چون `base.tpUnits` طلا ۱۰ است.
            val tpBase = if (base.tpUnits > 0.0) base.tpUnits else 10.0
            for (k in listOf(1.0, 2.5, 5.0)) {
                val tp = Math.round(tpBase * k * 1e6) / 1e6
                stage3.add("حد سود ${Fa2.n(tp, if (tp < 1.0) 5 else 0)} واحد" to { c: Settings -> c.tpUnits = tp })
            }
        }
        // ── مرحلهٔ ۵: گیت نوسان (ATR/قیمت) ────────────────────────────────────
        val stage5 = ArrayList<Pair<String, (Settings) -> Unit>>()
        // ⚠ آستانهٔ ATR٪ هم به مقیاس نوسانِ خودِ نماد وابسته است (میانهٔ ATR٪ پوند
        // ۰٫۱۱٪ است در برابر ۰٫۴۱٪ طلا). شبکه نسبت به مقدار پایه ساخته می‌شود؛ برای طلا
        // عیناً همان ۰٫۲۰/۰٫۲۵ درمی‌آید چون `Tuned.MIN_ATR_PCT` = ۰٫۲۵ است.
        val volBase = if (base.minAtrPct > 0.0) base.minAtrPct else Tuned.MIN_ATR_PCT
        stage5.add("گیت نوسان خاموش" to { c: Settings -> c.minAtrPct = 0.0 })
        for (k in listOf(0.8, 1.0)) {
            val g = Math.round(volBase * k * 1000.0) / 1000.0
            stage5.add("گیت نوسان ${Fa2.n(g, 3)}٪" to { c: Settings -> c.minAtrPct = g })
        }
        // ── مرحلهٔ ۴: نگهبان منحنی ─────────────────────────────────────────────
        val stage4 = ArrayList<Pair<String, (Settings) -> Unit>>()
        stage4.add("نگهبان خاموش" to { c: Settings -> c.guardMode = 0 })
        stage4.add("کاهش ریسک در افت ۱۲٪" to { c: Settings -> c.guardMode = 1; c.guardDDPct = 12.0 })
        stage4.add("کاهش + توقف در افت ۲۵٪" to { c: Settings -> c.guardMode = 2; c.guardDDPct = 12.0 })

        // ── مرحلهٔ ۶ (۱٫۴٫۵۱): سیستم مدیریت معاملهٔ باز ──────────────────────────
        // کلاسیک سند، هوشمند با سه آستانهٔ دروازهٔ نوسان، و نسخهٔ بدون تریلینگ.
        // ترتیب: بعد از مرحله‌های ورود و ریسک، چون مدیریت فقط روی معاملاتی اثر دارد که
        // ورودشان قبلاً انتخاب شده است.
        val stage6 = ArrayList<Pair<String, (Settings) -> Unit>>()
        stage6.add("مدیریت کلاسیک سند" to { c: Settings ->
            c.mgmtMode = 0
        })
        // همان قاعدهٔ مرحلهٔ ۵: دروازهٔ نوسانِ مدیریت هم نسبت به پایهٔ نماد. برای طلا
        // عیناً ۰٫۲۵/۰٫۳۰/۰٫۳۵ درمی‌آید چون `Tuned.MGMT_VOL_GATE_PCT` = ۰٫۳۰ است.
        val baseGateMgmt = if (base.mgmtVolGatePct > 0.0) base.mgmtVolGatePct else volBase * 1.2
        for (k in listOf(0.833, 1.0, 1.167)) {
            val g = Math.round(baseGateMgmt * k * 1000.0) / 1000.0
            stage6.add("هوشمند · دروازه ${Fa2.n(g, 3)}٪" to { c: Settings ->
                c.mgmtMode = 1; c.mgmtVolGatePct = g
                c.mgmtHoldMinR = Tuned.MGMT_HOLD_MIN_R; c.mgmtHoldMaxDays = Tuned.MGMT_HOLD_DAYS
                c.mgmtTrailStartR = Tuned.MGMT_TRAIL_START_R; c.mgmtTrailDistR = Tuned.MGMT_TRAIL_DIST_R
            })
        }
        stage6.add("هوشمند · بدون تریلینگ" to { c: Settings ->
            c.mgmtMode = 1; c.mgmtVolGatePct = 0.0; c.mgmtTrailStartR = 0.0
        })

        // اجرای زنجیره‌ای مرحله‌ها: برندهٔ هر مرحله مبنای مرحلهٔ بعد است.
        val rows = ArrayList<Row>()
        var current = baseCopy(base).also { it.guardMode = 0 }
        var evaluated = 0
        val stages = listOf(stage1, stage2, stage3, stage5, stage4, stage6)
        var cancelled = false
        for ((si, stage) in stages.withIndex()) {
            var bestCfg: Settings? = null
            var bestScore = -Double.MAX_VALUE
            var bestLabel = ""
            for ((li, plan) in stage.withIndex()) {
                if (cancel()) { cancelled = true; break }
                val c = current.copy()
                plan.second(c)
                val label = "م${si + 1}· ${plan.first}"
                progress?.invoke(evaluated, stages.sumOf { it.size }, label)
                val r = evaluate(c, training, label)
                evaluated++
                rows.add(r)
                if (r.score > bestScore) { bestScore = r.score; bestCfg = c; bestLabel = plan.first }
            }
            if (cancelled) break
            if (bestCfg != null) current = bestCfg
            progress?.invoke(evaluated, stages.sumOf { it.size }, "برنده تا اینجا: $bestLabel")
        }
        // گزارش و اعتبارسنجی باید دقیقاً همان ردیفی را استفاده کنند که بیشترین امتیاز
        // آموزش را دارد، نه لزوماً آخرین برندهٔ مرحله‌ای را.
        rows.maxByOrNull { it.score }?.let { current = it.cfg.copy() }
        // هدف R و حد سودِ «پایه» اگر در مرحلهٔ ۳ آزموده نشده باشد، به‌عنوان مرجع بماند
        if (baseTarget > 0 && base.strategy == Strategy.DOC) current.trigger2TargetR = current.trigger2TargetR
        if (base.strategy == Strategy.NO_TREND) current.tpUnits = current.tpUnits.coerceAtLeast(baseTp)
        // ── اعتبارسنجی خارج از نمونه ──────────────────────────────────────────
        // انتخاب فقط روی ۷۰٪ ابتدایی است؛ ۳۰٪ آخر کنار گذاشته شده و در انتخاب
        // هیچ دخالتی ندارد. برنده و پایه هر دو روی همان بازهٔ کنارگذاشته‌شده اجرا می‌شوند تا معلوم شود
        // بهبود واقعی است یا برازش بیش‌ازحد به گذشته.
        var oos: Row? = null
        var oosBase: Row? = null
        if (!cancelled && candles.size > 20_000) {
            val cut = split
            val tail = candles.subList(cut, candles.size)
            progress?.invoke(evaluated, evaluated + 2, "اعتبارسنجی خارج از نمونه…")
            oos = evaluate(current.copy(), tail, "خارج از نمونه · برنده")
            oosBase = evaluate(baseCopy(base), tail, "خارج از نمونه · تنظیم فعلی")
            evaluated += 2
            rows.add(oos); rows.add(oosBase)
        }
        return Report(
            rows = rows.sortedByDescending { it.score },
            evaluated = evaluated,
            seconds = (System.currentTimeMillis() - t0) / 1000.0,
            candles = candles.size,
            fromT = candles.firstOrNull()?.t ?: 0L,
            toT = candles.lastOrNull()?.t ?: 0L,
            cmsn = if (base.cmsnAuto && base.cmsnAutoValue > 0.0) base.cmsnAutoValue else base.cmsnManual,
            minTrades = minTrades,
            cancelled = cancelled,
            oos = oos,
            oosBase = oosBase
        )
    }

    /**
     * تنظیمات برندهٔ گزارش را روی [target] می‌نشیند.
     *
     * فقط کلیدهای هندسی/مدیریتی جابه‌جا می‌شوند؛ کارمزد، سرمایه، ریسک، تایم‌فریم، `cmsn`
     * و هر تنظیم دیگرِ کاربر دست‌نخورده می‌ماند (همان قاعدهٔ پیش‌تنظیم‌های تطبیق).
     */
    fun applyWinner(winner: Row, target: Settings): String {
        val s = winner.cfg
        target.strategy = s.strategy
        target.trigger2Fade = s.trigger2Fade
        target.trigger2TargetR = s.trigger2TargetR
        target.tpUnits = s.tpUnits
        target.slBufMode = s.slBufMode
        target.slAtrMult = s.slAtrMult
        target.slAtrPeriod = s.slAtrPeriod
        target.slBufTicks = s.slBufTicks
        target.slAtrMinUnits = s.slAtrMinUnits
        target.slAtrMaxUnits = s.slAtrMaxUnits
        target.maxSlMode = s.maxSlMode
        target.maxSlAtrMult = s.maxSlAtrMult
        target.maxSlUnits = s.maxSlUnits
        target.guardMode = s.guardMode
        target.guardDDPct = s.guardDDPct
        target.guardHardDDPct = s.guardHardDDPct
        target.guardRiskScale = s.guardRiskScale
        target.guardResetAtHigh = s.guardResetAtHigh
        target.minAtrPct = s.minAtrPct
        target.entryHourFrom = s.entryHourFrom
        target.entryHourTo = s.entryHourTo
        return winner.label
    }
}

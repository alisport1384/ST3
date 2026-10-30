package com.alisport.goldpin.core

// ═══════════════════════════════════════════════════════════════════════════════
//  نمادهای قابل معامله — طلا (XAUUSD) و نفت WTI (USOIL)
//
//  چرا این فایل: نسخهٔ ۱٫۴٫۵۲ فقط یک نماد می‌شناخت و همهٔ عددهای پیش‌فرض
//  (بافر حد ضرر بر حسب تیک، سقف N، گیت نوسان، اسپرد، پنجرهٔ ساعت ورود) برای
//  طلای ۲٬۰۰۰–۴٬۰۰۰ دلاری کالیبره شده‌اند. نفت حدود ۴۰ تا ۱۵۰ دلار است و
//  نوسان نسبی‌اش ۲ تا ۳ برابر طلا؛ با عددهای طلا، حد ضرر مطلق ۱۸ دلاری روی نفت
//  می‌شد ۲۵٪ قیمت. پس هر نماد «پروفایل عددی» خودش را دارد.
//
//  قاعدهٔ طلایی این نسخه: انتخاب نماد **فقط نمای برنامه** را عوض می‌کند؛
//  تشخیص و اجرا برای هر دو نماد هم‌زمان و مستقل ادامه دارد (Hub).
//
//  نسخهٔ ۱٫۵٫۰۲ — کارمزد از پروفایل بروکر می‌آید ([Brokers])، نه از عدد ثابت:
//  پیش‌فرض = آلپاری Pro ECN. هر پروفایل عدد رسمی خودش را دارد و واحدش هم همان واحد
//  `cmsnManual` اپ است (دلار بر اونس/بشکه در رفت‌وبرگشت)، پس هیچ تبدیلی در رابط لازم نیست.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * مشخصات یک نماد معاملاتی.
 *
 * @param id شناسهٔ پایدار (برای نام فایل ذخیره و کلید نقشهٔ نمادها — هرگز عوض نشود)
 * @param label نام فارسی برای رابط
 * @param feedSymbol نماد یاهو فایننس برای فید زنده و تاریخچهٔ آنلاین
 * @param unit واحد قیمت («اونس» / «بشکه»)
 * @param decimals تعداد رقم اعشار نمایش قیمت
 * @param tick اندازهٔ یک تیک قیمت (واحد پول)
 * @param oil پروفایل نفتی؟ (برای انتخاب پیش‌فرض‌ها و مسیر قیمت لحظه‌ای)
 */
enum class SymbolKind { METAL, OIL, FX }

data class SymbolSpec(
    val id: String,
    val label: String,
    val feedSymbol: String,
    val unit: String,
    val decimals: Int,
    val tick: Double,
    val kind: SymbolKind = SymbolKind.METAL,
    /**
     * نماد یاهو برای **قیمت نقدی** وقتی [feedSymbol] یک قرارداد آتی است و حجم واقعی ندارد.
     * `null` ⇒ خودِ [feedSymbol] (رفتار قدیمی طلا و نفت، بدون هیچ تغییر).
     */
    val spotSymbol: String? = null
) {
    /** پروفایل نفتی؟ (کلیدهای قدیمی همه‌جا همین را می‌خوانند) */
    val oil: Boolean get() = kind == SymbolKind.OIL

    /** عنوان کوتاه برای تب مدیریت */
    val short: String get() = when (kind) {
        SymbolKind.OIL -> "نفت"
        SymbolKind.FX -> "پوند"
        SymbolKind.METAL -> "طلا"
    }
}

object Symbols {

    val GOLD = SymbolSpec(
        id = "XAUUSD",
        label = "طلا — XAUUSD (دلار/اونس)",
        feedSymbol = "GC=F",
        unit = "اونس",
        decimals = 2,
        tick = 0.01,
        kind = SymbolKind.METAL
    )

    /**
     * نفت WTI با نماد یاهو `CL=F` (فیوچرز سبک شیرین) — همان مسیری که طلا با `GC=F` می‌رود.
     * قیمت‌ها دلار بر بشکه؛ یک تیک = ۰٫۰۱ دلار.
     */
    val OIL = SymbolSpec(
        id = "USOIL",
        label = "نفت WTI — USOIL (دلار/بشکه)",
        feedSymbol = "CL=F",
        unit = "بشکه",
        decimals = 2,
        tick = 0.01,
        kind = SymbolKind.OIL
    )

    /**
     * پوند/دلار — نماد سوم، با همان ساختار طلا و نفت.
     *
     *  • `feedSymbol = "6B=F"` (فیوچرز پوند CME) چون تنها نماد یاهو است که **حجم واقعی**
     *    دارد؛ استراتژی‌های این اپ (باکس ولوم کم/زیاد) بدون حجم کار نمی‌کنند، درست مثل
     *    طلایی که با `GC=F` می‌رود و نه با `XAUUSD=X`.
     *  • `spotSymbol = "GBPUSD=X"` شناسهٔ نقدی است؛ دادهٔ کندل و قیمت زنده از فیوچرز
     *    می‌آیند و این دو ابزار یکسان نیستند. قیمت نقدی هرگز به کندل آتی تزریق نمی‌شود.
     *    قیمت و حجم آتی الزاماً معادل جریان نقدی بروکر نیست.
     *  • یک تیک = ۰٫۰۰۰۰۱ (یک «پوینت» در قرارداد رسمی آلپاری) و قیمت با ۵ رقم اعشار.
     */
    val GBPUSD = SymbolSpec(
        id = "GBPUSD",
        label = "پوند/دلار — GBPUSD",
        feedSymbol = "6B=F",
        unit = "پوند",
        decimals = 5,
        tick = 0.00001,
        kind = SymbolKind.FX,
        spotSymbol = "GBPUSD=X"
    )

    val ALL: List<SymbolSpec> = listOf(GOLD, OIL, GBPUSD)

    fun byId(id: String?): SymbolSpec = ALL.firstOrNull { it.id == id } ?: GOLD

    fun indexOf(id: String?): Int = ALL.indexOfFirst { it.id == id }.coerceAtLeast(0)

    /**
     * پروفایل عددی نفت — همهٔ عددها از جاروب پارامتر روی دادهٔ **واقعی M1 نفت**
     * (۶٬۸۲۹٬۵۶۰ کندل · ۲۰۰۵-۰۱-۰۳ تا ۲۰۲۶-۱۰-۲۲ · اسپرد ۰٫۰۳ دلار بر بشکه) آمده‌اند:
     *
     *  • بافر حد ضرر ۶۰ تیک = ۰٫۶۰ دلار ≈ ۱٫۷× ATR ساعتی نفت (ATR% میانهٔ نفت ۰٫۵۱٪
     *    در برابر ۰٫۲۶٪ طلا). عدد طلا (۱۸۰۰ تیک = ۱۸ دلار) روی نفت می‌شد ۲۵٪ قیمت.
     *  • سقف N = ۲ واحد قیمت (طلا: ۸۰) — همان نسبت به ATR.
     *  • گیت نوسان ورود ۰٫۶۰٪ (طلا: ۰٫۲۵٪) — چون ATR٪ نفت ۲٫۳ برابر طلاست؛ با ۰٫۲۵
     *    نفت عملاً همیشه مجاز بود و نتیجه منفی می‌شد.
     *  • هدف تریگر ۲ = ۱۰R (طلا: ۷R) — نفت روندهای بلندتر و بازگشت‌های تندتر دارد.
     *  • اسپرد ۰٫۳۰ دلار بر بشکه (تصمیم مالک؛ طلا: ۰٫۷ دلار بر اونس). این «کارمزد» است، نه عدد ثابت:
     *    در کد کمیسیون = اسپرد × حجم × اندازهٔ قرارداد، پس عدد باید هم‌واحد قیمت نماد باشد.
     *    ⚠ صادقانه: ۰٫۳ دلار روی قیمت ۷۰ دلار یعنی ۰٫۴۳٪ قیمت — حدود ۱۸ برابر گران‌تر از
     *    نسبت طلا. با ۰٫۳ هر ترکیبی که حد ضرر باریک دارد شکست می‌خورد؛ به همین دلیل
     *    بافر ۲۰۰ تیک، سقف N=۸، فیلتر هزینه ۸× و هدف ۱۲R انتخاب شده‌اند.
     *    (این اعداد از جاروب ۳۸ ترکیب روی دادهٔ ۲۰ ساله بیرون آمده‌اند.)
     */
    fun applyOilDefaults(s: Settings, cmsn: Double = DEFAULT_OIL_CMSN) {
        s.strategy = Strategy.DOC
        s.mmMode = MoneyMode.MIXED
        s.trigger2Fade = true          // ✅ معکوس‌سازی تریگر ۲: تنها خانواده‌ای که روی نفت لبه دارد
        s.trigger2TargetR = 12.0       // ✅ هدف ۱۲R — با اسپرد ۰٫۳، فقط هدف‌های بزرگ زنده می‌مانند
        s.tpUnits = 10.0
        s.minTPunits = 1.0
        s.maxSlUnits = 8.0             // ✅ سقف N = ۸ واحد قیمت (طلا: ۸۰)
        s.maxSlMode = 0
        s.slBufMode = 0
        s.slBufTicks = 200.0           // ✅ بافر = ۲٬۰ دلار (طلا: ۱۸ دلار)
        s.entryBufTicks = -2.0
        s.minSlUnits = 0.0
        s.minSlCostMult = 8.0          // ✅ کلید اصلی: R باید ≥ ۸ × اسپرد باشد (≥ ۲٫۴ دلار)
        //  ↑ نگه‌داشتن ۸ فقط برای کارمزد مرجع ۰٫۳۰ درست است؛ [applyBrokerCost] همین را با
        //    کارمزد واقعی بروکر نرمال می‌کند تا آستانهٔ مطلق همان [OIL_COST_FLOOR_UNITS] بماند.
        s.mintick = 0.01
        s.slipTicks = 1.0
        s.minAtrPct = 0.70             // ✅ ورود فقط در نوسان ≥ ۰٫۷۰٪ (طلا: ۰٫۲۵٪)
        s.entryHourFrom = 5
        s.entryHourTo = 15
        s.mgmtMode = 1
        s.mgmtHoldMinR = Tuned.MGMT_HOLD_MIN_R
        s.mgmtHoldMaxDays = Tuned.MGMT_HOLD_DAYS
        s.mgmtTrailStartR = Tuned.MGMT_TRAIL_START_R
        s.mgmtTrailDistR = Tuned.MGMT_TRAIL_DIST_R
        s.mgmtVolGatePct = Tuned.MGMT_VOL_GATE_PCT
        // ── کارمزد واقعی بروکر ────────────────────────────────────────────────
        //  ۱٫۵٫۰۱ با فرض «۰٫۳ دلار بر بشکه» تنظیم شد؛ آن فرض از جدول رسمی بروکر
        //  بیرون نیامده بود. جدول واقعی آلپاری روی نفت ۰٫۰۱۵ (Pro ECN) تا ۰٫۰۷
        //  (Standard MT4) دلار بر بشکه است — ۴ تا ۲۰ برابر ارزان‌تر.
        //  از ۱٫۵٫۰۲ عدد کارمزد از [Brokers] می‌آید و پارامترهای زیر (که با فرض
        //  گران‌ترین حالت انتخاب شده بودند) عمداً دست‌نخورده مانده‌اند تا مبنای
        //  آزمون ۲۰سالهٔ ۱٫۵٫۰۱ حفظ شود؛ با کارمزد ارزان‌تر فقط بهتر عمل می‌کنند.
        s.cmsnAuto = false
        s.cmsnManual = cmsn
        // موتور ترکیبی روی نفت: فقط لایهٔ گیت رژیم سود می‌دهد
        s.cxMode = 1
        s.cxRegimeGate = 0.60
        s.cxRegimeGateCalm = 0.65
        s.cxBoxMinUnits = 0.10
        s.cxSizeMode = 1
        s.cxCompoundCap = 1.5
        s.cxStressFloor = 1.0
        s.cxSlWidenMax = 1.15
        s.cxTrailRegime2 = 1.10
        s.useRiskPct = true
        s.riskPct = 1.0
        s.initialEquity = 100000.0
        s.contractSize = 1.0
    }

    /** کارمزد مرجع نفتی که پارامترهای ۱٫۵٫۰۱ با آن آزموده شدند (فقط برای مقایسه/گزارش). */
    const val DEFAULT_OIL_CMSN = 0.30

    /**
     * کف فاصلهٔ حد ضرر نفت بر حسب **واحد قیمت** که پارامترهای ۱٫۵٫۰۱ با آن آزموده شدند
     * (= ضریب فیلتر ۸ × کارمزد مرجع ۰٫۳۰).
     *
     * چرا این عدد ثابت می‌ماند: اندازه‌گیری روی ۲۰۲۲–۲۰۲۶ نشان داد وقتی فیلتر کارمزد با
     * کارمزد ارزان باز می‌شود، ۲۷۸ معاملهٔ کم‌کیفیت وارد می‌شوند و سود خالص به صفر می‌رسد
     * (سود ناخالص ≈ هزینه). پس با هر بروکری، *آستانهٔ مطلق* ۲٫۴ دلار حفظ می‌شود و ضریب
     * فیلتر فقط از نو نرمال می‌شود: `ضریب = ۲٫۴ ÷ کارمزد بروکر`.
     * یعنی کارمزد ارزان‌تر **هزینه** را کم می‌کند، نه اینکه اجازهٔ معامله‌های بد بدهد.
     */
    const val OIL_COST_FLOOR_UNITS = 2.4

    /**
     * پیش‌فرض طلا = همان تنظیم آزموده‌شدهٔ ۲۰سالهٔ [Tuned] — تنها کارمزد از بروکر می‌آید.
     * (پارامترهای طلا با کارمزد ۰٫۷ بهینه شده‌اند؛ با کارمزد کمتر آلپاری همان پارامترها
     * هزینهٔ کمتری می‌دهند، پس دست‌نخورده می‌مانند.)
     */
    fun applyGoldDefaults(s: Settings, cmsn: Double = Tuned.CMSN) {
        Tuned.apply(s, cmsn)
        s.cmsnAuto = false
        s.cmsnManual = cmsn
    }

    /**
     * اعمال پیش‌فرض‌های یک نماد **با کارمزد بروکر**.
     *
     * @param price قیمت مرجع (آخرین قیمت کندل/فید)؛ اگر ≤ ۰ باشد از [Brokers.fallbackPrice] استفاده می‌شود.
     * @param brokerId شناسهٔ پروفایل؛ `null` ⇒ همین [Settings.brokerId] و اگر آن هم ناشناس بود، پیش‌فرض آلپاری.
     * @return کارمزدی که واقعاً اعمال شد (دلار بر واحد قیمت، رفت‌وبرگشت).
     */
    fun applyDefaults(s: Settings, spec: SymbolSpec, price: Double = 0.0, brokerId: String? = null): Double {
        val profile = Brokers.byId(brokerId ?: s.brokerId) ?: Brokers.default
        val px = if (price > 0.0) price else Brokers.fallbackPrice(spec.id)
        val cost = Brokers.costPerUnit(profile, spec.id, px) ?: fallbackCost(spec)
        applyKindDefaults(s, spec, cost)
        // سواپ شبانه هم در مسیر «نصب تازه» اعمال می‌شود. پیش‌تر فقط [applyBrokerCost]
        // آن را می‌گذاشت، پس روی نصب تازه `swapEnabled=true` می‌ماند با `swapLong/Short = 0`
        // — یعنی سواپ **بی‌صدا** صفر حساب می‌شد تا وقتی کاربر دستی بروکر را ست کند.
        if (spec.kind == SymbolKind.FX) applySwap(s, spec, profile)
        // ⚠ باگ مسیر «نصب تازه» که در ۱٫۵٫۰۲ نیمه‌کاره ماند: `applyBrokerCost` آستانهٔ
        // **مطلق** فیلتر کارمزد نفت را از نو نرمال می‌کرد ولی اینجا ضریب روی همان ۸٫۰
        // ثابت رها می‌شد. با کارمزد رسمی نفت (۰٫۰۱۵۰۶) آستانه به ۰٫۱۲ دلار می‌افتاد،
        // یعنی فیلتر عملاً خاموش — همان چیزی که آزمون «آستانهٔ فیلتر کارمزد همان ۲٫۴
        // دلار می‌ماند» ردش می‌کرد. طلا و پوند چون `Tuned.COST_MULT = 0` دارند (فیلتر
        // خاموش) دست‌نخورده می‌مانند.
        if (spec.oil && cost > 0.0) {
            s.minSlCostMult = Math.round(OIL_COST_FLOOR_UNITS / cost * 10.0) / 10.0
        }
        s.brokerId = profile.id
        s.brokerRefPrice = px
        s.brokerAppliedAt = System.currentTimeMillis()
        return cost
    }

    /**
     * ست کردن پروفایل بروکر روی تنظیمات **بدون** دست‌زدن به پارامترهای استراتژی.
     * فقط کارمزد و فیلدهای بروکر عوض می‌شوند؛ بقیهٔ تنظیم دستی کاربر محترم می‌ماند.
     * @return کارمزد اعمال‌شده یا `null` اگر بروکر برای این نماد عدد رسمی نداشته باشد.
     */
    fun applyBrokerCost(s: Settings, spec: SymbolSpec, profile: BrokerProfile, price: Double): Double? {
        val px = if (price > 0.0) price else Brokers.fallbackPrice(spec.id)
        val cost = Brokers.costPerUnit(profile, spec.id, px) ?: return null
        // آستانهٔ مطلق فعلی فیلتر کارمزد (واحد قیمت) — قبل از عوض‌کردن کارمزد نگه داشته می‌شود
        val oldCost = if (s.cmsnAuto && s.cmsnAutoValue > 0.0) s.cmsnAutoValue else Math.max(0.0, s.cmsnManual)
        val oldFloor = if (oldCost > 0.0) s.minSlCostMult * oldCost else 0.0

        s.brokerId = profile.id
        s.brokerRefPrice = px
        s.brokerAppliedAt = System.currentTimeMillis()
        s.cmsnAuto = false
        s.cmsnManual = cost
        applySwap(s, spec, profile)

        // ── نرمال‌کردن ضریب فیلتر کارمزد تا آستانهٔ **مطلق** تغییر نکند ─────────────
        //  چرا لازم است: اندازه‌گیری روی نفت ۲۰۲۲–۲۰۲۶ نشان داد اگر فقط کارمزد کم شود و
        //  ضریب ۸ بماند، آستانه از ۲٫۴ دلار به ۰٫۱۲ می‌آید، ۲۷۸ معاملهٔ کم‌کیفیت وارد
        //  می‌شوند و سود خالص به ≈صفر می‌رسد (۲۷۸ معامله · PF ۱٫۰۰ · افت ۴۴۴$).
        //  پس آستانهٔ آزموده‌شده دست‌نخورده می‌ماند و ضریب بازمحاسبه می‌شود:
        //      نفت: آستانهٔ ثابت [OIL_COST_FLOOR_UNITS] (۲٫۴ دلار)
        //      طلا: اگر کاربر فیلتر را روشن کرده باشد، همان آستانهٔ فعلی خودش
        if (cost > 0.0) {
            val floor = if (spec.oil) OIL_COST_FLOOR_UNITS else oldFloor
            if (floor > 0.0) s.minSlCostMult = Math.round(floor / cost * 10.0) / 10.0
        }
        return cost
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  پروفایل عددی پوند/دلار (GBPUSD) — نسخهٔ ۱٫۵٫۰۴
    //
    //  خواستهٔ مالک: GBPUSD باید کپی بی‌کم‌وکاست طلا و همهٔ تنظیم‌هایش باشد. پس اینجا
    //  [Tuned] طلا **عیناً** صدا زده می‌شود و هیچ کلید دیگری دست نمی‌خورد؛ تنها عددهایی
    //  بازمقیاس می‌شوند که واحدشان «واحد قیمت» یا «درصد نوسان» است و با کپی عددی روی
    //  قیمت ۱٫۳ دلاری بی‌معنا می‌شدند (بافر ۱۸ دلاری روی پوند می‌شد ۱٬۳۰۰٪ قیمت).
    //
    //  قاعدهٔ بازمقیاس‌کردن: «همان ضریب ATR روی همان داده».
    //  اندازه‌گیری با همان تعریف موتور (`Engine.structureAtr` = میانگین TR روی ۱۴ کندل
    //  بستهٔ H1 · `tools/measure_fx_atr.py` · جدول `docs/fx_atr_calibration.json`):
    //
    //      بازه    میانهٔ ATR٪ طلا   میانهٔ ATR٪ پوند   نسبت
    //      ۱ سال        ۰٫۴۰۸۴            ۰٫۱۱۰۹        ۰٫۲۷۱۵
    //      ۳ سال        ۰٫۲۸۶۵            ۰٫۱۱۴۸        ۰٫۴۰۰۷
    //      ۲۰ سال       ۰٫۲۵۷۸            ۰٫۱۳۸۷        ۰٫۵۳۸۲
    //
    //  ⚠ مبنای محاسبه **۱ سال** است، نه ۲۰ سال. دلیلش اندازه‌گیری مستقیم است، نه سلیقه:
    //  نوسان طلا در یک سال گذشته ۵۸٪ **بزرگ** شده (۰٫۲۵۷۸ → ۰٫۴۰۸۴) و نوسان پوند ۲۰٪
    //  **کوچک** (۰٫۱۳۸۷ → ۰٫۱۱۰۹). اعداد [Tuned] طلا ثابتِ مطلق‌اند، پس «ضریب ATR»ِ خودِ
    //  طلا هم در این بیست سال جابه‌جا شده: بافر ۱۸ دلاری روی دادهٔ یک‌ساله فقط ۱٫۰۲×ATR
    //  است، نه ۵٫۱۹×ATRِ میانگین بیست‌ساله. بک‌تست تأیید کرد همین ۱٫۰۲ ضریبِ مؤثر است
    //  (حد ضرر واقعیِ معاملات طلا روی یک سال = ۱٫۳۱×ATR که ۱٫۰۲ آن بافر است).
    //  اگر پوند با مبنای ۲۰ سال بسته می‌شد، بافرش ۶٫۷×ATR می‌شد؛ آنگاه حد ضرر هرگز
    //  نمی‌خورد (۰ خروج با SL در کل سال)، حد سود ۴۸×ATR هرگز نمی‌رسید و تب عملاً مرده
    //  بود: ۸ معامله در سال به‌جای ۱۳۴. پس هر عدد زیر، ضریب ATRِ **دادهٔ یک‌سالهٔ** طلا
    //  را نگه می‌دارد:
    //
    //      مقدار            طلا (۱ سال)        ضریب ATR طلا   پوند
    //      بافر حد ضرر      ۱۸٫۰۰ دلار          ۱٫۰۲×        ۰٫۰۰۱۵ (۱۵۰ تیک / ۱٫۰۲×ATR)
    //      سقف N            ۸۰ دلار             ۴٫۵۲×        ۰٫۰۰۶۷ (۴٫۵۲×ATR)
    //      حد سود جایگزین   ۱۰ دلار             ۰٫۵۷×        ۰٫۰۰۰۸۴
    //
    //  و چون بافر و سقف N **نسبی به ATR** بسته شده‌اند (`slBufMode=1` و `maxSlMode=1` —
    //  همان مکانیزمی که [Optimizer] جارویش می‌کند)، این ضریب‌ها روی هر پنج بازهٔ داده
    //  درست می‌مانند و با تغییر نوسان پوند کهنه نمی‌شوند. ثابت‌های تیکی فقط جانشینِ
    //  دورانِ «ATR هنوز گرم نشده» هستند.
    //
    //  هر آستانهٔ درصدی هم با همان نسبت ۰٫۲۷۱۵ مقیاس شده تا **صدکِ** خودش در توزیع
    //  ATR٪ِ یک‌ساله حفظ شود:
    //      گیت نوسان     ۰٫۲۵ (زیر صدک ۱۰ طلا)  →  ۰٫۰۶۸ (زیر صدک ۱۰ پوند)
    //      دروازهٔ مدیریت ۰٫۳۰                  →  ۰٫۰۸۱
    //      دروازهٔ ترکیبی ۰٫۳۳                  →  ۰٫۰۹۰
    //      آرامِ ترکیبی   ۰٫۴۰                  →  ۰٫۱۰۹
    //
    //  چیزهایی که **عمداً** کپی عددی شده‌اند: بافر سطح سفارش ‎−۲ تیک‎ و لغزش ۱ تیک.
    //  هر دو زیر یک‌درصد ATR هستند (در طلا و پوند) و واحدشان «تیکِ خودِ نماد» است،
    //  پس معنایشان با عوض‌شدن اندازهٔ تیک خودش حفظ می‌شود.
    // ═══════════════════════════════════════════════════════════════════════════

    /** یک تیک پوند = یک «پوینت» در قرارداد رسمی آلپاری. */
    const val FX_MINTICK = 0.00001

    /** بافر حد ضرر پوند به تیک ⇒ ۰٫۰۰۱۵ (۱۵ پیپ). فقط تا گرم‌شدن ATR کاربرد دارد. */
    const val FX_SL_BUF_TICKS = 150.0

    /** ضریب ATRِ بافر حد ضرر — دقیقاً همان ضریبی که ۱۸ دلار طلا روی دادهٔ یک‌ساله دارد. */
    const val FX_SL_ATR_MULT = 1.02

    /** سقف N پوند (واحد قیمت) ⇒ ۶۷ پیپ؛ جانشینِ ثابت تا گرم‌شدن ATR. */
    const val FX_MAX_SL_UNITS = 0.0067

    /** ضریب ATRِ سقف N — همان ضریب طلای ۸۰ دلاری روی دادهٔ یک‌ساله. */
    const val FX_MAX_SL_ATR_MULT = 4.52

    /** حد سود استراتژی ۲ و کف حد سود فیبویی (واحد قیمت) ≈ ۰٫۵۷× ATR. */
    const val FX_TP_UNITS = 0.00084

    /** گیت نوسان ورود (ATR/قیمت×۱۰۰) — هم‌صدک با ۰٫۲۵٪ طلا. */
    const val FX_MIN_ATR_PCT = 0.068

    /** دروازهٔ نوسان مدیریت هوشمند — هم‌صدک با ۰٫۳۰٪ طلا. */
    const val FX_MGMT_VOL_GATE_PCT = 0.081

    /** دروازه‌های رژیم لایهٔ ترکیبی — هم‌صدک با ۰٫۳۳٪ و ۰٫۴۰٪ طلا. */
    const val FX_CX_REGIME_GATE = 0.090
    const val FX_CX_REGIME_GATE_CALM = 0.109

    /**
     * کارمزد مرجع پوند: هزینهٔ رفت‌وبرگشت آلپاری Pro ECN MT5 بر حسب دلار به‌ازای هر
     * **پوند** (= ۷ دلار در لات ۱۰۰٬۰۰۰ پوندی: اسپرد ۲ پوینت + ۲٫۵ دلار کمیسیون هر طرف).
     * فقط وقتی استفاده می‌شود که پروفایل انتخابی عدد رسمی منتشرنکرده داشته باشد.
     */
    const val DEFAULT_FX_CMSN = 0.00007

    /**
     * پیش‌فرض پوند = **کپی کامل [Tuned] طلا** + عددهای بازمقیاس‌شدهٔ بالا.
     *
     * هیچ استراتژی، مود مدیریت، لایهٔ ترکیبی، پنجرهٔ ساعتی یا تنظیم ریسکی اینجا
     * عوض نمی‌شود؛ اگر روزی [Tuned] طلا تغییر کند، پوند هم همان را می‌گیرد.
     */
    fun applyFxDDefaults(s: Settings, cmsn: Double = DEFAULT_FX_CMSN) {
        Tuned.apply(s, cmsn)                    // ← عیناً همان تنظیم آزموده‌شدهٔ طلا
        // ── فقط عددهای وابسته به مقیاس قیمت ────────────────────────────────────
        s.mintick = FX_MINTICK
        // بافر و سقف N **نسبی به ATR**: ضریبِ ثابتِ طلا روی دادهٔ یک‌ساله، ولی خودش با
        // نوسانِ لحظه‌ای پوند بزرگ و کوچک می‌شود. ثابت‌های تیکی فقط جانشینِ گرم‌نشدن ATR.
        s.slBufMode = 1
        s.slAtrMult = FX_SL_ATR_MULT
        s.slBufTicks = FX_SL_BUF_TICKS
        s.maxSlMode = 1
        s.maxSlAtrMult = FX_MAX_SL_ATR_MULT
        s.maxSlUnits = FX_MAX_SL_UNITS
        s.tpUnits = FX_TP_UNITS
        s.minTPunits = FX_TP_UNITS
        s.minAtrPct = FX_MIN_ATR_PCT
        s.mgmtVolGatePct = FX_MGMT_VOL_GATE_PCT
        s.cxRegimeGate = FX_CX_REGIME_GATE
        s.cxRegimeGateCalm = FX_CX_REGIME_GATE_CALM
        s.cmsnAuto = false
        s.cmsnManual = cmsn
    }

    /**
     * سواپ شبانهٔ رسمی پروفایل بروکر → [Settings].
     * اگر پروفایل برای این نماد سواپی منتشر نکرده باشد، تنظیم کاربر دست‌نخورده می‌ماند.
     */
    private fun applySwap(s: Settings, spec: SymbolSpec, profile: BrokerProfile) {
        profile.symbol(spec.id)?.let { bs ->
            val sl = bs.swapLong ?: 0.0
            val ss = bs.swapShort ?: 0.0
            if (sl != 0.0 || ss != 0.0) {
                s.swapEnabled = true
                s.swapLong = sl
                s.swapShort = ss
            }
        }
    }

    /** پیش‌فرض‌های یک نماد بر اساس نوعش. */
    fun applyKindDefaults(s: Settings, spec: SymbolSpec, cost: Double) = when (spec.kind) {
        SymbolKind.OIL -> applyOilDefaults(s, cost)
        SymbolKind.FX -> applyFxDDefaults(s, cost)
        SymbolKind.METAL -> applyGoldDefaults(s, cost)
    }

    /**
     * کارمزد جانشین هر نماد وقتی پروفایل بروکر عدد رسمی ندارد.
     * (طلا/نفت همان اعداد ۱٫۵٫۰۲ · پوند هزینهٔ Pro ECN MT5.)
     */
    fun fallbackCost(spec: SymbolSpec): Double = when (spec.kind) {
        SymbolKind.OIL -> DEFAULT_OIL_CMSN
        SymbolKind.FX -> DEFAULT_FX_CMSN
        SymbolKind.METAL -> Tuned.CMSN
    }

    /**
     * ضریب‌های بازمقیاس‌کردن مقادیر **دستی کاربر/پیش‌تنظیم** از طلا به یک نماد دیگر.
     *
     *   [unit]  ضربدرِ مقدارهای «واحد قیمت» (tpUnits · minSl · maxSl · حد فاصله‌ها)
     *   [ticks] ضربدرِ مقدارهای تیکی، تا همان فاصلهٔ مطلقِ معادل به‌دست آید
     *   [pct]   ضربدرِ آستانه‌های «ATR/قیمت×۱۰۰» تا همان **صدک** از توزیع نوسان بماند
     *   [money] ضربدرِ مقدارهای پولیِ هم‌واحد قیمت (کارمزد/اسپرد دستی)
     *
     * ⚠ نفت عمداً ضریب ۱ دارد: پروفایل نفت از اول مستقل از طلا جارو و تنظیم شده
     * ([applyOilDefaults]) و جدول تطبیق هم برای نفت همان‌طور که در ۱٫۵٫۰۲ آزموده شد
     * می‌ماند؛ اینجا فقط نماد تازه (پوند) مقیاس می‌گیرد تا رفتار هیچ نماد موجودی عوض نشود.
     */
    data class Scale(val unit: Double, val ticks: Double, val pct: Double, val money: Double) {
        val isIdentity: Boolean get() = unit == 1.0 && ticks == 1.0 && pct == 1.0 && money == 1.0
    }

    val SCALE_NONE = Scale(1.0, 1.0, 1.0, 1.0)

    /**
     * پوند — از اندازه‌گیری ATR٪ **یک‌ساله** (همان مبنای ثابت‌های بالا):
     * نسبت میانهٔ ATR٪ ۰٫۱۱۰۹ ÷ ۰٫۴۰۸۴ = ۰٫۲۷۱۵ و نسبت ATR مطلق
     * ۰٫۰۰۱۴۸۸۹ ÷ ۱۷٫۶۹۷۵ = ۰٫۰۰۰۰۸۴۱.
     *
     * [ticks] = ۱۵۰ ÷ ۱۸۰۰ است تا بافر پیش‌تنظیم ستاره‌دارِ طلا دقیقاً همان
     * [FX_SL_BUF_TICKS] شود. [money] نسبت کارمزد مرجع دو نماد است (۰٫۰۰۰۰۷ ÷ ۰٫۷).
     */
    val SCALE_FX = Scale(unit = 0.0000841, ticks = 0.0833, pct = 0.2715, money = 0.0001)

    fun scale(spec: SymbolSpec): Scale = when (spec.kind) {
        SymbolKind.FX -> SCALE_FX
        else -> SCALE_NONE
    }

    /** گرد کردن یک عدد بازمقیاس‌شده به رقم اعشار نماد (بدون آن، ۰٫۰۰۰۵۶۱۸۴ می‌شود). */
    fun roundUnits(spec: SymbolSpec, v: Double): Double {
        val f = Math.pow(10.0, (spec.decimals + 1).toDouble())
        return Math.round(v * f) / f
    }
}

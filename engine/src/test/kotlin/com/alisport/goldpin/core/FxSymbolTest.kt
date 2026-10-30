package com.alisport.goldpin.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * پوند/دلار (GBPUSD) — نماد سوم.
 *
 * این آزمون سه چیز را قفل می‌کند:
 *  ۱) **هویت نماد**: پوند در [Symbols.ALL] هست، تب و مسیر داده‌اش از همان‌جا می‌آید، و
 *     اضافه‌شدنش هیچ رفتاری از طلا و نفت را عوض نکرده است.
 *  ۲) **هزینهٔ رسمی آلپاری**: اسپرد ۲ پوینت + کمیسیون ۲٫۵ دلار در لات هر طرف + سواپ
 *     −۳٫۲۹/−۱٫۴۴ پوینت، همه از ویجت Contract Specifications خودِ آلپاری. این عددها
 *     دست‌ساز نیستند؛ اگر روزی عدد عوض شد، آزمون باید قرمز شود نه اینکه بی‌صدا جابه‌جا شود.
 *  ۳) **کپی‌بودن از طلا**: [Symbols.applyFxDDefaults] اول [Tuned.apply] را عیناً صدا می‌زند
 *     و بعد **فقط** عددهای وابسته به مقیاس قیمت را عوض می‌کند. هر کلید دیگری که فرق کند
 *     یعنی «کپی بی‌کم‌وکاست» نقض شده است.
 */
class FxSymbolTest {

    private val fx = Symbols.GBPUSD
    private val fxId = Symbols.GBPUSD.id
    /** قیمت مرجع محاسبهٔ هزینه — همان قیمتی که در `docs/ALPARI-KARMOZD-VA-SPREAD.md` آمده. */
    private val pxFx = 1.36429

    private fun near(expected: Double, actual: Double, eps: Double = 1e-9) =
        assertTrue(kotlin.math.abs(expected - actual) <= eps,
            "انتظار $expected بود، $actual به دست آمد (اختلاف ${kotlin.math.abs(expected - actual)})")

    @Test
    fun `پوند عضو فهرست نمادهاست و تب و مسیر داده از همان‌جا می‌آید`() {
        assertEquals(3, Symbols.ALL.size, "باید سه نماد باشد: طلا، نفت، پوند")
        assertEquals("GBPUSD", fx.id)
        assertEquals("GBPUSD", Symbols.byId("GBPUSD").id)
        assertEquals(Symbols.GOLD, Symbols.byId("gbpusd-unknown"))
        assertEquals(2, Symbols.indexOf("GBPUSD"), "پوند بعد از طلا و نفت می‌آید")
        assertEquals(SymbolKind.FX, fx.kind)
        assertEquals("پوند", fx.short)
        // ⚠ فیوچرز برای **حجم** (استراتژی بدون حجم کار نمی‌کند) و نقدی برای **هزینه**.
        assertEquals("6B=F", fx.feedSymbol)
        assertEquals("GBPUSD=X", fx.spotSymbol)
        assertEquals(5, fx.decimals)
        near(0.00001, fx.tick, 1e-12)
        // طلا و نفت دقیقاً مثل قبل: `spotSymbol` نداشتند و نباید پیدا کنند
        assertNull(Symbols.GOLD.spotSymbol, "طلا نباید مسیر قیمتش عوض شود")
        assertNull(Symbols.OIL.spotSymbol)
        assertEquals("GC=F", Symbols.GOLD.feedSymbol)
        assertEquals("CL=F", Symbols.OIL.feedSymbol)
        assertTrue(Symbols.OIL.oil && !Symbols.GOLD.oil && !fx.oil)
    }

    @Test
    fun `هزینهٔ رسمی آلپاری روی پوند — اسپرد ۲ پوینت و کمیسیون ۲٫۵ دلار در لات هر طرف`() {
        val bs = Brokers.ALPARI_PRO_ECN_MT5.symbol(fxId)
        assertNotNull(bs, "پروفایل Pro ECN MT5 باید GBPUSD داشته باشد")
        assertEquals("GBPUSD", bs!!.ticker)
        near(2.0, bs.spreadPoints!!, 1e-12)
        near(0.00001, bs.pointSize, 1e-12)
        near(100000.0, bs.contractSize, 1e-9)
        // اسپرد ۲ پوینت = ۰٫۰۰۰۰۲ دلار بر پوند = ۲ دلار در لات
        near(0.00002, bs.spreadPerUnit!!, 1e-12)
        // کمیسیون ۲ × ۲٫۵ ÷ ۱۰۰٬۰۰۰ — و **مستقل از قیمت**، چون آلپاری جفت‌ارز را
        // «دلار در لات» اعلام می‌کند نه «دلار بر میلیون».
        near(0.00005, bs.commissionPerUnit(pxFx), 1e-12)
        near(0.00005, bs.commissionPerUnit(1.10), 1e-12, )
        near(0.00005, bs.commissionPerUnit(1.90), 1e-12)
        // کل = ۰٫۰۰۰۰۷ دلار بر پوند = ۷ دلار در لات رفت‌وبرگشت
        near(0.00007, bs.costPerUnit(pxFx)!!, 1e-12)
        near(7.0, bs.costPerLot(pxFx)!!, 1e-9)
        // حدود ۰٫۰۰۵٪ قیمت — هم‌خانوادهٔ طلای آلپاری (۰٫۰۰۶٪)، نه چند برابر آن
        assertTrue(bs.costPct(pxFx)!! < 0.01, "هزینهٔ پوند نباید از ۰٫۰۱٪ قیمت بیشتر شود")
    }

    @Test
    fun `کمیسیون جفت‌ارز فقط روی Pro ECN است و چهار پروفایل دیگر اسپرد رسمی ندارند`() {
        // صفحهٔ رسمی خودِ آلپاری: «آلپاری فقط روی حساب Pro ECN کمیسیون می‌گیرد».
        for (p in Brokers.ALL) {
            val bs = p.symbol(fxId)
            assertNotNull(bs, "پروفایل ${p.id} باید ردیف GBPUSD داشته باشد (حداقل سواپ)")
            if (p.id == Brokers.DEFAULT_ID || p.id == "alpari-pro-ecn-mt4") {
                near(0.00005, bs!!.commissionPerUnit(pxFx), 1e-12)
            } else {
                near(0.0, bs!!.commissionPerUnit(pxFx), 1e-15)
            }
            // ⚠ فقط `proEcnMt5` سمت سرور رندر می‌شود؛ بقیه حداقل اسپرد رسمی GBPUSD را
            // منتشر نکرده‌اند. **اسپرد حدس نمی‌زنیم** — هزینهٔ اشتباه کل فیلتر کارمزد را
            // جابه‌جا می‌کند، پس `null` می‌ماند و `costPerUnit` هم `null` می‌دهد.
            if (p.id != Brokers.DEFAULT_ID) {
                assertNull(bs.spreadPoints, "اسپرد ${p.id} باید null بماند")
                assertNull(Brokers.costPerUnit(p, fxId, pxFx), "هزینهٔ ${p.id} باید null بماند")
            }
            if (p.id == Brokers.DEFAULT_ID) {
                near(-0.0000329, bs.swapLong!!, 1e-12)
                near(-0.0000144, bs.swapShort!!, 1e-12)
            } else {
                assertNull(bs.swapLong); assertNull(bs.swapShort)
            }
        }
        assertEquals(5, Brokers.ALL.size, "تعداد پروفایل‌ها نباید عوض شود")
    }

    @Test
    fun `پیش‌فرض پوند همان تنظیم طلاست و فقط عددهای مقیاس‌دار عوض می‌شوند`() {
        val g = Settings(); Symbols.applyGoldDefaults(g, Tuned.CMSN)
        val f = Settings(); Symbols.applyFxDDefaults(f, Symbols.DEFAULT_FX_CMSN)

        // ── کلیدهایی که **باید** عیناً یکی باشند (کپی بی‌کم‌وکاست) ──────────────
        assertEquals(g.strategy, f.strategy, "استراتژی")
        assertEquals(g.mmMode, f.mmMode, "مود مالی")
        assertEquals(g.trigger2Fade, f.trigger2Fade, "معکوس‌سازی تریگر ۲")
        near(g.trigger2TargetR, f.trigger2TargetR, 1e-12)
        assertEquals(g.entryBufTicks, f.entryBufTicks, "بافر سطح سفارش")
        assertEquals(g.slipTicks, f.slipTicks, "لغزش")
        assertEquals(g.minSlUnits, f.minSlUnits)
        assertEquals(g.minSlCostMult, f.minSlCostMult)
        assertEquals(g.mgmtMode, f.mgmtMode)
        near(g.mgmtHoldMinR, f.mgmtHoldMinR, 1e-12)
        assertEquals(g.mgmtHoldMaxDays, f.mgmtHoldMaxDays)
        near(g.mgmtTrailStartR, f.mgmtTrailStartR, 1e-12)
        near(g.mgmtTrailDistR, f.mgmtTrailDistR, 1e-12)
        assertEquals(g.entryHourFrom, f.entryHourFrom)
        assertEquals(g.entryHourTo, f.entryHourTo)
        assertEquals(g.cxMode, f.cxMode, "لایهٔ ترکیبی باید مثل طلا روشن باشد")
        near(g.cxBoxMinUnits, f.cxBoxMinUnits, 1e-12, )
        assertEquals(g.guardMode, f.guardMode)
        assertEquals(g.useRiskPct, f.useRiskPct)
        near(g.riskPct, f.riskPct, 1e-12)
        near(g.initialEquity, f.initialEquity, 1e-9)

        // ── کلیدهایی که **عمداً** فرق دارند (واحدشان وابسته به مقیاس قیمت است) ────
        assertNotEquals(g.mintick, f.mintick)
        near(0.00001, f.mintick, 1e-12)
        // بافر و سقف N برای پوند **نسبی به ATR** بسته شده‌اند تا روی هر بازهٔ داده درست
        // بمانند؛ طلا همان ثابتِ آزموده‌شدهٔ خودش را دارد (و آزمون طلا دست‌نخورده است).
        assertEquals(0, g.slBufMode)
        assertEquals(1, f.slBufMode)
        near(Symbols.FX_SL_ATR_MULT, f.slAtrMult, 1e-12)
        assertEquals(0, g.maxSlMode)
        assertEquals(1, f.maxSlMode)
        near(Symbols.FX_MAX_SL_ATR_MULT, f.maxSlAtrMult, 1e-12)
        assertTrue(f.minAtrPct > 0.0 && f.minAtrPct < g.minAtrPct)
        assertTrue(f.cxRegimeGate > 0.0 && f.cxRegimeGate < g.cxRegimeGate)
        assertTrue(f.cxRegimeGateCalm > f.cxRegimeGate)
    }

    @Test
    fun `پیش‌فرض نصب تازهٔ پوند با کارمزد و سواپ رسمی بروکر ساخته می‌شود`() {
        val s = Settings()
        val cost = Symbols.applyDefaults(s, fx, pxFx, Brokers.DEFAULT_ID)
        near(0.00007, cost, 1e-12)
        near(0.00007, s.cmsnManual, 1e-12)
        assertEquals(Brokers.DEFAULT_ID, s.brokerId)
        near(pxFx, s.brokerRefPrice, 1e-12)
        near(0.00001, s.mintick, 1e-12)
        assertEquals(1, s.slBufMode)
        assertEquals(1, s.maxSlMode)
        // ⚠ سواپ باید در **همین** مسیر هم اعمال شود. پیش‌تر فقط `applyBrokerCost` آن را
        // می‌گذاشت، پس روی نصب تازه `swapEnabled=true` با سواپ صفر می‌ماند و سواپ بی‌صدا
        // حساب نمی‌شد.
        assertTrue(s.swapEnabled)
        near(-0.0000329, s.swapLong, 1e-12)
        near(-0.0000144, s.swapShort, 1e-12)
        // فیلتر کارمزد طلا/پوند خاموش است (Tuned.COST_MULT = 0) و نباید روشن شود
        near(Tuned.COST_MULT, s.minSlCostMult, 1e-12)
    }

    @Test
    fun `ست کردن بروکر روی پوند کارمزد و سواپ رسمی را می‌گذارد و هندسه را دست نمی‌زند`() {
        val s = Settings()
        Symbols.applyFxDDefaults(s, Symbols.DEFAULT_FX_CMSN)
        val applied = Symbols.applyBrokerCost(s, fx, Brokers.ALPARI_PRO_ECN_MT5, pxFx)
        assertNotNull(applied)
        near(0.00007, applied!!, 1e-12)
        near(0.00007, s.cmsnManual, 1e-12)
        assertEquals(Brokers.DEFAULT_ID, s.brokerId)
        assertTrue(s.swapEnabled)
        near(-0.0000329, s.swapLong, 1e-12)
        // هندسهٔ استراتژی دست‌نخورده می‌ماند
        assertEquals(1, s.slBufMode)
        near(Symbols.FX_SL_ATR_MULT, s.slAtrMult, 1e-12)
        near(Symbols.FX_MAX_SL_UNITS, s.maxSlUnits, 1e-12)
    }

    @Test
    fun `پروفایلی که اسپرد رسمی پوند ندارد کارمزد دستی را عوض نمی‌کند`() {
        val s = Settings()
        Symbols.applyFxDDefaults(s, 0.0001)
        val before = s.cmsnManual
        assertNull(Symbols.applyBrokerCost(s, fx, Brokers.ALPARI_STANDARD_MT4, pxFx))
        near(before, s.cmsnManual, 1e-15)
        assertNotEquals(Brokers.ALPARI_STANDARD_MT4.id, s.brokerId)
    }

    @Test
    fun `قیمت جانشین و رقم اعشار نمایش هزینه برای پوند`() {
        near(1.35, Brokers.fallbackPrice(fxId), 1e-12)
        // طلا و نفت مثل قبل
        near(4000.0, Brokers.fallbackPrice(Symbols.GOLD.id), 1e-9)
        near(100.0, Brokers.fallbackPrice(Symbols.OIL.id), 1e-9)
        assertEquals(5, Brokers.costDecimals(fxId))
        assertEquals(4, Brokers.costDecimals(Symbols.GOLD.id))
        assertEquals(8, Brokers.swapDecimals(fxId))
        assertEquals(4, Brokers.swapDecimals(Symbols.OIL.id))

        val sum = Brokers.summaryFa(Brokers.ALPARI_PRO_ECN_MT5, fxId, pxFx)
        // `Fa2` رقم‌ها را لاتین می‌نویسد؛ تبدیل به فارسی کار لایهٔ رابط اپ است.
        assertTrue("GBPUSD" in sum, sum)
        assertTrue("0.00002" in sum, "اسپرد باید با پنج رقم نشان داده شود: $sum")
        assertTrue("0.00007" in sum, "کل هزینه باید با پنج رقم نشان داده شود: $sum")
        assertTrue("7.00 دلار در لات" in sum, "هزینهٔ هر لات باید در متن باشد: $sum")
        assertTrue("1.36429" in sum, "قیمت مرجع باید با پنج رقم اعشار باشد: $sum")
        // بدون رقم اضافه، سواپ پوند «0.0000» نمایش داده می‌شد
        val sw = Brokers.swapWarningFa(Brokers.ALPARI_PRO_ECN_MT5, fxId)
        assertTrue("0.00003290" in sw, "سواپ باید با هشت رقم باشد: $sw")
        assertTrue("0.00001440" in sw, sw)
        // متن طلا/نفت با همان چهار رقم قدیمی می‌ماند (رگرسیون نمایشی)
        val goldSum = Brokers.summaryFa(Brokers.ALPARI_PRO_ECN_MT5, Symbols.GOLD.id, 4000.0)
        assertTrue("XAUUSD @ 4000.00" in goldSum, goldSum)
    }

    @Test
    fun `ضریب‌های بازمقیاس‌کردن پوند از اندازه‌گیری یک‌ساله آمده‌اند`() {
        val sc = Symbols.scale(fx)
        assertEquals(Symbols.SCALE_FX, sc)
        // طلا و نفت عمداً ضریب ۱ دارند تا رفتار هیچ نماد موجودی عوض نشود
        assertTrue(Symbols.scale(Symbols.GOLD).isIdentity)
        assertTrue(Symbols.scale(Symbols.OIL).isIdentity)
        // نسبت ATR مطلق یک‌ساله: ۰٫۰۰۱۴۸۸۹ ÷ ۱۷٫۶۹۸۲
        assertTrue(sc.unit > 0.00007 && sc.unit < 0.0001, "unit=${sc.unit}")
        // نسبت ATR٪ یک‌ساله: ۰٫۱۱۰۹ ÷ ۰٫۴۰۸۴
        assertTrue(sc.pct > 0.26 && sc.pct < 0.29, "pct=${sc.pct}")
        // ضریب تیک باید بافر پیش‌تنظیم ستاره‌دارِ طلا را دقیقاً به بافر پوند ببرد
        near(Symbols.FX_SL_BUF_TICKS, Tuned.SL_BUF_TICKS * sc.ticks, 0.6, )
        // نسبت کارمزد مرجع دو نماد
        near(Symbols.DEFAULT_FX_CMSN / Tuned.CMSN, sc.money, 1e-9)
        // گرد کردن باید رقم اعشار نماد را رعایت کند، وگرنه ۰٫۰۰۰۵۶۱۸۴ در رابط می‌ماند
        near(0.000841, Symbols.roundUnits(fx, 0.000841234), 1e-9)
        near(0.001, Symbols.roundUnits(Symbols.GOLD, 0.000841234), 1e-9)
    }

    @Test
    fun `اندازهٔ بافر و سقف N پوند با ضریب ATR طلای یک‌ساله برابر است`() {
        // این آزمون **دلیل** انتخاب ضریب‌ها را قفل می‌کند: بافر ۱۸ دلاری طلا روی دادهٔ
        // یک‌ساله ۱٫۰۲×ATR است (نه ۵٫۱۹×ATRِ میانگین بیست‌ساله). اگر کسی ضریب پوند را
        // با مبنای بیست‌ساله ببندد، بافر ۶٫۷×ATR می‌شود و حد ضرر هرگز نمی‌خورد.
        val goldAtr1y = 17.6982   // ۰٫۴۰۸۴٪ × میانهٔ کلوز ۴٬۳۳۳٫۳۸
        val fxAtr1y = 0.0014889   // ۰٫۱۱۰۹٪ × میانهٔ کلوز ۱٫۳۴۲۷۶
        val goldBufMult = (Tuned.SL_BUF_TICKS * 0.01) / goldAtr1y
        assertTrue(goldBufMult in 0.95..1.10, "بافر طلا روی دادهٔ یک‌ساله باید ≈۱×ATR باشد: $goldBufMult")
        near(goldBufMult, Symbols.FX_SL_ATR_MULT, 0.06)
        val goldMaxMult = Tuned.MAX_SL_UNITS / goldAtr1y
        assertTrue(goldMaxMult in 4.3..4.8, "سقف N طلا روی دادهٔ یک‌ساله: $goldMaxMult")
        near(goldMaxMult, Symbols.FX_MAX_SL_ATR_MULT, 0.25)
        // ثابت تیکیِ جانشین هم باید همان ≈۱×ATRِ امروز باشد
        val fallbackMult = (Symbols.FX_SL_BUF_TICKS * Symbols.FX_MINTICK) / fxAtr1y
        assertTrue(fallbackMult in 0.9..1.15, "جانشین تیکی پوند: $fallbackMult")
    }
}

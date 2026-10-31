# Qaf (قاف) — UX & Accessibility Changelog (`UX_CHANGES.md`)

## English Summary (User-Facing Release Notes)

### 1. Clearer Home Screen & Progress Hierarchy
- **Faster Access to Reviews on Compact Phones (`360dp`)**: Moved the **"Keep your memory strong"** review queue card directly underneath **Today's Study Plan** so due and waiting reviews are immediately visible without scrolling past secondary analytics.
- **Crystal-Clear Progress Scope (`CEFR Vocab` vs. `Vocabulary Lifecycle Stages`)**: Clarified that the **CEFR Readiness Vocab counter** (e.g., `B1 Vocab (50%) — 10/500`) tracks words at your current CEFR level, while **Vocabulary Lifecycle Stages (`All Selected Books`)** tracks all words across your chosen books (including ungraded specialty expressions and idioms).
- **Collapsible Lifecycle Grid**: Tap the **Vocabulary Lifecycle Stages** header anytime to collapse or expand the 6-stage breakdown (`New`, `Seen`, `Learning`, `Familiar`, `Mature`, `Conquered`).
- **Bilingual Waiting-Queue Labels**: Added Arabic status labels (`في الانتظار` / `مستحقة الآن`) and non-breaking timer units (`~7 د`) so waiting review counts never wrap awkwardly.

### 2. Flashcards & Light Mode Readability
- **High-Contrast Light & Dark Mode Typography**: Replaced hardcoded dark-mode gray colors on the flipped flashcard's Arabic definition and example translation with reactive semantic theme tokens (`TextSecondary` and `TextSubtle`), ensuring crisp WCAG AA readability in both Light and Dark themes.
- **Balanced 4-Block Flipped Card & Long-IPA Guard**: Preserved even vertical spacing across the 4 back-of-card blocks while preventing long phonetic transcriptions (such as `accidentally` and `be derived from sth`) from squishing the slow-audio Turtle and Favorite Heart buttons.
- **Actionable Missing-Translation Prompt**: Replaced the vague `"No Arabic yet"` badge with a clickable bilingual prompt that opens **Word Details** for instant dictionary/AI translation.

### 3. Cross-Device Layout Unification, Audio Feedback & Clearer Dialogs (v3.1.0)
- **Canonical `411dp` Viewport & Equal-Height Grid Unification**: Normalized handset density to the canonical `411dp` design viewport (`MIN_NORMALIZED_WIDTH_DP = 411f` via exact pixel width) and locked `fontScale = 1.0f` across `MainActivity.attachBaseContext` and Compose `Density`. Multi-column grids (Reading Topics, Daily Study Time, Study Days/Week, and Lifecycle Funnel chips) use `IntrinsicSize.Min` + `fillMaxHeight()` so paired cards always match height and titles like `"Science & Environment"` stay on a single line across all physical phones.
- **Reduced Motion Support**: Flipped flashcards now respect Android's **Remove Animations / Animator Duration Scale** accessibility setting, switching to an instant flip when motion is disabled.
- **Helpful Audio / TTS Alerts**: If your device does not have an English Text-to-Speech voice installed, tapping a pronunciation button now displays a clear bilingual tip explaining how to enable **Google Speech Services** instead of failing silently.
- **Specific Safe-Exit Dialog Buttons**: Replaced generic `"Cancel"` buttons in Backup Restore and Plan Reset confirmation dialogs with explicit labels (`"Keep Current Plan"`, `"Keep Current Data"`).

### 4. Full Arabic RTL Experience, Adaptive Quiz Mastery & Speed Blitz (v4.0.0)
- **Natural Arabic Reading Flow with Protected English Lexical Elements**: Comprehensive RTL layout mirroring with dedicated LTR guardrails for English phonetic IPA transcriptions, sample sentences, vocabulary definitions, and grammar examples.
- **Adaptive Daily Mini-Quiz with 70% Mastery Threshold**: Tasks require >= 70% (7/10) to complete, remaining actionable with clear 'Needs Retry' indicators and one-tap retests, with missed questions immediately routed to SM-2 and Mistake Notebook.
- **Integrated Speed Blitz & Lexical Association Webs**: Quick 60-second vocabulary sprint and radial mind-map explore word families with zero lag and offline fallbacks.
- **In-App Direct Updates**: One-tap background download, SHA-256 verification, and immediate Android package installer launch.

---

## ملخص التحديثات باللغة العربية (سجل التغييرات للمستخدم — v3.1.0)

### ١. تنظيم الشاشة الرئيسية ووضوح عدادات التقدم
- **وصول أسرع للمراجعات على الشاشات الصغيرة (`360dp`)**: تم تقديم بطاقة المراجعات المتباعدة (**Keep your memory strong**) لتظهر مباشرة أسفل **خطتك اليومية** دون الحاجة للتمرير الطويل.
- **توضيح الفرق بين عداد كلمات المستوى (`CEFR Vocab`) ومراحل إتقان المفردات**: أصبح عداد المفردات في بطاقة الجاهزية يوضّح أنه يقيس كلمات مستواك الحالي فقط (مثل `B1 Vocab (50%)`)، بينما تقيس شبكة **مراحل إتقان المفردات (`كل الكتب المختارة`)** جميع الكلمات التي درستها في كتبك المختارة (بما فيها المصطلحات والعبارات التخصصية غير المصنفة بمستوى CEFR).
- **إمكانية طيّ وتوسيع شبكة المراحل الست**: يمكنك الآن الضغط على عنوان **Vocabulary Lifecycle Stages** لطيّ أو إظهار صناديق المراحل الست لتوفير مساحة أكبر على الشاشة.
- **ثبات عرض مؤقت الانتظار**: تحسين عرض عداد الانتظار (`في الانتظار` / `مستحقة الآن`) وربط الرقم بوحدة الدقائق (`~7 د`) لمنع انقسام النص في سطر منفصل.

### ٢. البطاقات التعليمية (Flashcards) ووضوح الوضع الفاتح (Light Mode)
- **تباين ألوان مثالي في الوضعين الفاتح والداكن**: استبدال الألوان الرمادية الثابتة في ظهر البطاقة التعليمية بألوان ديناميكية عالية التباين (`TextSecondary` و `TextSubtle`) لضمان وضوح التعريف العربي وترجمة المثال في الوضع الفاتح (Light Mode) والداكن (Dark Mode).
- **توزيع عمودي متوازن وحماية الرموز الصوتية الطويلة**: الحفاظ على التوزيع المتوازن لظهر البطاقة من ٤ أقسام مع ضمان عدم ضغط أزرار السلحفاة والقلب عند عرض كلمات ذات نطق صوتي (IPA) طويل.
- **رسالة تفاعلية عند غياب الترجمة**: استبدال عبارة `"No Arabic yet"` برسالة واضحة (`لا توجد ترجمة محفوظة بعد • اضغط تفاصيل الكلمة`) تفتح نافذة التفاصيل مباشرة عند الضغط عليها.

### ٣. توحيد التخطيط عبر الهواتف والتنبيهات الصوتية (v3.1.0)
- **توحيد العرض القياسي (`411dp`) وتساوي ارتفاع البطاقات**: ضبط العرض القياسي وتثبيت معامل تحجيم الخط (`fontScale = 1.0f`) لضمان تطابق شكل الواجهة والبطاقات على جميع الهواتف الذكية مع محاكي التصميم، مع توحيد ارتفاع البطاقات المتجاورة (`IntrinsicSize.Min`) ومنع انقسام عناوين مواضيع القراءة المركبة.
- **احترام إعداد تقليل الحركة (Reduce Motion)**: عند تفعيل خيار إيقاف التأثيرات الحركية في جهازك، تنقلب البطاقات التعليمية فوراً بدون دوران ثلاثي الأبعاد.
- **تنبيه واضح عند غياب محرك النطق (TTS)**: في حال عدم وجود صوت إنجليزي مثبّت على الجهاز، يظهر تنبيه ثنائي اللغة يوضح كيفية تفعيل **Google Speech Services** بدلاً من الصمت غير المفهوم.
- **أزرار تأكيد أوضح في النوافذ الحوارية**: استبدال زر `"إلغاء"` العام بعبارات محددة ومطمئنة (`"الاحتفاظ بخطتي"` و `"إلغاء الاستعادة"`).

### ٤. الواجهة العربية المتكاملة، اختبار الإتقان التكيفي وتحدي السرعة (v4.0.0)
- **انسيابية الواجهة العربية مع حماية الكلمات والمقاطع الإنجليزية**: دعم اتجاه القراءة من اليمين لليار (RTL) لكامل شاشات التطبيق مع عزل وحماية اتجاه النصوص والرموز الصوتية الإنجليزية (IPA) وجمل الأمثلة لمنع أي انعكاس بصري غير مرغوب فيه.
- **إعادة هندسة الاختبار اليومي القصير بنسبة نجاح 70%**: اشتراط تحقيق 7 من 10 على الأقل لاحتساب المهمة مع إتاحة إعادة المحاولة فوراً بأسئلة جديدة، وتحويل الكلمات المتعثرة إلى مراجعات الغد في SM-2 ودفتر التصحيحات.
- **الخريطة الذهنية للمفردات وتحدي الـ 60 ثانية السريع**: إمكانية استكشاف المترادفات والمتلازمات وتحدي سرعة استدعاء الكلمات وممارسة الإملاء الصوتي.
- **تحديث التطبيق المباشر بضغطة زر**: فحص الإصدارات الجديدة من داخل التطبيق وتحميلها وتثبيتها مباشرة مع فحص بصمة الأمان (SHA-256).

---

## Deferred / Recommended Next (توصيات للمراحل القادمة)

1. **Modal Stage Preview Sheet Paging (`Deferred`)**:
   - The main Word Books screen already virtualizes all 21,624 entries smoothly. Migrating the secondary modal stage-preview sheet to `PagingData` was deferred to avoid modifying DAO method signatures per Working Rule 2.
2. **Bundled Offline Neural TTS Fallback (`Deferred`)**:
   - Bundling an in-APK neural speech synthesis model would increase download size by ~45 MB. The new observable TTS status alert + Google Speech Services (`com.google.android.tts`) voice selector resolves silent audio failures with zero APK size overhead.

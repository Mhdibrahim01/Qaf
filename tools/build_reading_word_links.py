#!/usr/bin/env python3
"""
Builds deterministic A1/A2 micro-stories (r001-r016) in app/src/main/assets/reading_texts.json
and generates app/src/main/assets/reading_word_links.json linking every reading text (r001-r090)
to its matched vocabulary headwords, normalized macro-topic, and content tokens for readability scoring.
"""
import json
import os
import re

ASSETS_DIR = "app/src/main/assets"
READING_JSON_PATH = os.path.join(ASSETS_DIR, "reading_texts.json")
LINKS_JSON_PATH = os.path.join(ASSETS_DIR, "reading_word_links.json")
DATA_IMPORT_DIR = os.path.join(ASSETS_DIR, "data_import")

STOPWORDS = {
    "a", "an", "the", "and", "or", "but", "if", "because", "as", "until", "while",
    "of", "at", "by", "for", "with", "about", "against", "between", "into", "through",
    "during", "before", "after", "above", "below", "to", "from", "up", "down", "in",
    "out", "on", "off", "over", "under", "again", "further", "then", "once", "here",
    "there", "when", "where", "why", "how", "all", "any", "both", "each", "few",
    "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own",
    "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don",
    "should", "now", "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
    "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself",
    "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their",
    "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these",
    "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has",
    "had", "having", "do", "does", "did", "doing", "would", "could", "ought", "im",
    "youre", "hes", "shes", "its", "were", "theyre", "ive", "youve", "weve", "theyve",
    "id", "youd", "hed", "shed", "wed", "theyd", "ill", "youll", "hell", "shell",
    "well", "theyll", "isnt", "arent", "wasnt", "werent", "hasnt", "havent", "hadnt",
    "doesnt", "dont", "didnt", "wont", "wouldnt", "shant", "shouldnt", "cant", "cannot",
    "couldnt", "mustnt", "lets", "thats", "whos", "whats", "heres", "theres", "whens",
    "wheres", "whys", "hows", "one", "two", "three", "four", "five", "first", "second",
    "also", "many", "much", "every", "day", "time", "year", "years", "people", "way"
}

MACRO_TOPIC_KEYWORDS = {
    "technology_science": [
        "tech", "ai", "digital", "science", "space", "future", "innovation", "cyber",
        "computer", "software", "internet", "data", "robot", "automation", "smart", "algorithm"
    ],
    "business_career": [
        "work", "career", "business", "interview", "leadership", "economy", "finance",
        "management", "office", "remote", "job", "company", "corporate", "entrepreneur", "productivity"
    ],
    "daily_life_culture": [
        "food", "culture", "daily", "family", "routine", "tradition", "home", "hobby",
        "cooking", "festival", "art", "music", "lifestyle", "community", "friendship", "shopping"
    ],
    "health_psychology": [
        "health", "mind", "psychology", "sleep", "stress", "fitness", "medical", "emotion",
        "habit", "wellbeing", "brain", "memory", "exercise", "nutrition", "mental"
    ],
    "environment_nature": [
        "environment", "nature", "climate", "park", "ocean", "green", "energy", "wildlife",
        "water", "planet", "pollution", "conservation", "weather", "sustainability", "forest"
    ],
    "travel_global": [
        "travel", "city", "journey", "tourism", "global", "transport", "airport", "adventure",
        "country", "urban", "exploration", "flight", "hotel"
    ],
    "education_language": [
        "education", "learning", "language", "study", "school", "book", "reading", "student",
        "university", "communication", "writing", "knowledge", "skill"
    ],
    "history_society": [
        "history", "society", "ancient", "media", "ethics", "law", "philosophy", "civilization",
        "change", "justice", "journalism", "architecture", "heritage"
    ]
}


def normalize_macro_topic(raw_topic: str, title_en: str = "") -> str:
    combined = f"{raw_topic} {title_en}".lower()
    for macro, keywords in MACRO_TOPIC_KEYWORDS.items():
        for kw in keywords:
            if kw in combined:
                return macro
    return "daily_life_culture"


A1_A2_STORIES = [
    # --- A1 MICRO-STORIES (r001 - r008, 50-80 words each) ---
    {
        "id": "r001",
        "level": "A1",
        "topic": "daily routine",
        "title_en": "A Busy Morning at Home",
        "title_ar": "صباح مزدحم في المنزل",
        "grammar_ids": ["g001"],
        "target_words": ["morning", "early", "breakfast", "coffee", "kitchen", "ready"],
        "sentences": [
            {"en": "Salma wakes up early every morning at six o'clock.", "ar": "تستيقظ سلمى مبكرًا كل صباح في الساعة السادسة.", "grammar_id": "g001"},
            {"en": "She goes to the kitchen and makes hot coffee.", "ar": "تذهب إلى المطبخ وتعد قهوة ساخنة.", "grammar_id": "g001"},
            {"en": "Her brother eats healthy breakfast before school.", "ar": "يأكل أخوها فطورًا صحيًا قبل المدرسة."},
            {"en": "They check the weather on the phone together.", "ar": "يتحققان من الطقس على الهاتف معًا."},
            {"en": "Salma puts her notebook and pen in her bag.", "ar": "تضع سلمى دفترها وقلمها في حقيبتها."},
            {"en": "She is ready to start a new day with a smile.", "ar": "هي مستعدة لبدء يوم جديد بابتسامة.", "grammar_id": "g001"}
        ],
        "questions": [
            {
                "prompt": "What does Salma make in the kitchen?",
                "options": ["Cold juice", "Hot coffee", "Green soup", "Sweet cake"],
                "answer_index": 1,
                "explanation_ar": "النص يذكر أن سلمى تذهب إلى المطبخ وتعد قهوة ساخنة (hot coffee)."
            },
            {
                "prompt": "What does Salma put in her bag?",
                "options": ["Her notebook and pen", "Her shoes and coat", "Her lunch box only", "Her camera"],
                "answer_index": 0,
                "explanation_ar": "الجملة تقول: Salma puts her notebook and pen in her bag."
            }
        ]
    },
    {
        "id": "r002",
        "level": "A1",
        "topic": "technology",
        "title_en": "My First Computer Class",
        "title_ar": "درسي الأول في الحاسوب",
        "grammar_ids": ["g002"],
        "target_words": ["computer", "screen", "keyboard", "message", "password", "helpful"],
        "sentences": [
            {"en": "Today is my first computer class at the local center.", "ar": "اليوم هو درسي الأول في الحاسوب في المركز المحلي.", "grammar_id": "g002"},
            {"en": "The teacher shows us the screen and the keyboard.", "ar": "يشرح لنا المعلم الشاشة ولوحة المفاتيح."},
            {"en": "I learn how to write a short email message.", "ar": "أتعلم كيف أكتب رسالة بريد إلكتروني قصيرة.", "grammar_id": "g002"},
            {"en": "We also create a strong password for safety.", "ar": "نحن أيضًا ننشئ كلمة مرور قوية للأمان."},
            {"en": "The teacher is very patient and helpful with everyone.", "ar": "المعلم صبور جدًا ومتعاون مع الجميع."},
            {"en": "I practice typing English words every evening at home.", "ar": "أتدرب على كتابة الكلمات الإنجليزية كل مساء في المنزل."}
        ],
        "questions": [
            {
                "prompt": "What does the student learn to write in class?",
                "options": ["A long book", "A short email message", "A paper letter", "A music song"],
                "answer_index": 1,
                "explanation_ar": "النص يقول: I learn how to write a short email message."
            },
            {
                "prompt": "Why do the students create a strong password?",
                "options": ["For safety", "For games", "To print photos", "To turn off the screen"],
                "answer_index": 0,
                "explanation_ar": "الجملة توضح أن كلمة المرور القوية هي للأمان (for safety)."
            }
        ]
    },
    {
        "id": "r003",
        "level": "A1",
        "topic": "work and office",
        "title_en": "A New Job at the Office",
        "title_ar": "وظيفة جديدة في المكتب",
        "grammar_ids": ["g003"],
        "target_words": ["office", "manager", "meeting", "team", "desk", "schedule"],
        "sentences": [
            {"en": "Karim starts his new job at a clean modern office.", "ar": "يبدأ كريم وظيفته الجديدة في مكتب حديث ونظيف.", "grammar_id": "g003"},
            {"en": "His desk is near a large window with bright light.", "ar": "مكتبه قريب من نافذة كبيرة ذات إضاءة ساطعة."},
            {"en": "The manager welcomes him and introduces the friendly team.", "ar": "يرحب به المدير ويعرفه على الفريق الودود.", "grammar_id": "g003"},
            {"en": "They have a short morning meeting at nine o'clock.", "ar": "لديهم اجتماع صباحي قصير في الساعة التاسعة."},
            {"en": "Karim writes his daily schedule in a small notebook.", "ar": "يكتب كريم جدوله اليومي في دفتر صغير."},
            {"en": "He feels happy and excited about his new career.", "ar": "يشعر بالسعادة والحماس تجاه مسيرته المهنية الجديدة."}
        ],
        "questions": [
            {
                "prompt": "Where is Karim's desk located?",
                "options": ["Near the kitchen door", "Near a large window", "In the basement", "Outside the building"],
                "answer_index": 1,
                "explanation_ar": "النص يذكر أن مكتبه قريب من نافذة كبيرة (near a large window)."
            },
            {
                "prompt": "When is the short morning meeting?",
                "options": ["At seven o'clock", "At nine o'clock", "At noon", "At five o'clock"],
                "answer_index": 1,
                "explanation_ar": "الاجتماع الصباحي القصير يبدأ في الساعة التاسعة (at nine o'clock)."
            }
        ]
    },
    {
        "id": "r004",
        "level": "A1",
        "topic": "health",
        "title_en": "Walking for Good Health",
        "title_ar": "المشي من أجل صحة جيدة",
        "grammar_ids": ["g004"],
        "target_words": ["health", "exercise", "water", "energy", "doctor", "healthy"],
        "sentences": [
            {"en": "Doctor Nadia always says that simple exercise is important.", "ar": "تقول الدكتورة نادية دائمًا إن التمرين البسيط مهم.", "grammar_id": "g004"},
            {"en": "Walking for thirty minutes every day improves our health.", "ar": "المشي لمدة ثلاثين دقيقة كل يوم يحسن صحتنا."},
            {"en": "I walk in the neighborhood with my neighbor after work.", "ar": "أمشي في الحي مع جاري بعد العمل.", "grammar_id": "g004"},
            {"en": "We carry a bottle of cold water with us.", "ar": "نحمل معنا زجاجة من الماء البارد."},
            {"en": "Fresh air gives us positive energy and better sleep.", "ar": "الهواء النقي يمنحنا طاقة إيجابية ونومًا أفضل."},
            {"en": "A healthy habit is easy when you start slowly.", "ar": "العادة الصحية تكون سهلة عندما تبدأ ببطء."}
        ],
        "questions": [
            {
                "prompt": "How many minutes of walking every day improves health?",
                "options": ["Five minutes", "Thirty minutes", "Three hours", "Ten seconds"],
                "answer_index": 1,
                "explanation_ar": "النص يذكر المشي لمدة ثلاثين دقيقة كل يوم (thirty minutes every day)."
            },
            {
                "prompt": "What does fresh air give the walkers?",
                "options": ["Positive energy and better sleep", "Heavy bags", "More homework", "Loud noise"],
                "answer_index": 0,
                "explanation_ar": "الهواء النقي يمنحهم طاقة إيجابية ونومًا أفضل (positive energy and better sleep)."
            }
        ]
    },
    {
        "id": "r005",
        "level": "A1",
        "topic": "environment",
        "title_en": "Planting Trees in Our Garden",
        "title_ar": "زراعة الأشجار في حديقتنا",
        "grammar_ids": ["g005"],
        "target_words": ["garden", "tree", "plant", "nature", "clean", "flower"],
        "sentences": [
            {"en": "Our family loves nature and green spaces around the house.", "ar": "تحب عائلتنا الطبيعة والمساحات الخضراء حول المنزل.", "grammar_id": "g005"},
            {"en": "Every spring, we plant new flowers in the small garden.", "ar": "في كل ربيع، نزرع زهورًا جديدة في الحديقة الصغيرة."},
            {"en": "Today, my father is planting a young lemon tree.", "ar": "اليوم، يزرع والدي شجرة ليمون صغيرة.", "grammar_id": "g005"},
            {"en": "Trees keep the air clean and give cool shade in summer.", "ar": "الأشجار تحافظ على نظافة الهواء وتعطي ظلًا باردًا في الصيف."},
            {"en": "My sister gives water to the plants every evening.", "ar": "تسقي أختي النباتات بالماء كل مساء."},
            {"en": "Small actions at home help protect our environment.", "ar": "الأفعال الصغيرة في المنزل تساعد في حماية بيئتنا."}
        ],
        "questions": [
            {
                "prompt": "What kind of tree is the father planting today?",
                "options": ["An apple tree", "A young lemon tree", "A tall pine tree", "A palm tree"],
                "answer_index": 1,
                "explanation_ar": "النص يقول: Today, my father is planting a young lemon tree."
            },
            {
                "prompt": "How do trees help in summer?",
                "options": ["They give cool shade and clean air", "They make the weather hotter", "They block the road", "They use electricity"],
                "answer_index": 0,
                "explanation_ar": "الأشجار تحافظ على نظافة الهواء وتعطي ظلًا باردًا في الصيف."
            }
        ]
    },
    {
        "id": "r006",
        "level": "A1",
        "topic": "travel",
        "title_en": "A Train Trip to the Coast",
        "title_ar": "رحلة بالقطار إلى الساحل",
        "grammar_ids": ["g006"],
        "target_words": ["train", "ticket", "station", "travel", "window", "beach"],
        "sentences": [
            {"en": "On Saturday, we travel by train to the coastal city.", "ar": "يوم السبت، نسافر بالقطار إلى المدينة الساحلية.", "grammar_id": "g006"},
            {"en": "We buy our train tickets online before going to the station.", "ar": "نشتري تذاكر القطار عبر الإنترنت قبل الذهاب إلى المحطة."},
            {"en": "The station is busy, but our train arrives on time.", "ar": "المحطة مزدحمة، لكن قطارنا يصل في الوقت المحدد.", "grammar_id": "g006"},
            {"en": "I sit near the window and watch the green fields.", "ar": "أجلس بالقرب من النافذة وأشاهد الحقول الخضراء."},
            {"en": "After two hours, we see the blue sea and the beach.", "ar": "بعد ساعتين، نرى البحر الأزرق والشاطئ."},
            {"en": "Traveling by train is comfortable, safe, and relaxing.", "ar": "السفر بالقطار مريح وآمن ويبعث على الاسترخاء."}
        ],
        "questions": [
            {
                "prompt": "How do they buy their train tickets?",
                "options": ["Online before going to the station", "Inside the train", "At the beach", "From a taxi driver"],
                "answer_index": 0,
                "explanation_ar": "النص يذكر أنهم يشترون التذاكر عبر الإنترنت قبل الذهاب إلى المحطة."
            },
            {
                "prompt": "What do they see after two hours?",
                "options": ["High snow mountains", "The blue sea and the beach", "A desert", "A factory"],
                "answer_index": 1,
                "explanation_ar": "الجملة تقول: After two hours, we see the blue sea and the beach."
            }
        ]
    },
    {
        "id": "r007",
        "level": "A1",
        "topic": "education",
        "title_en": "Reading English Stories Every Night",
        "title_ar": "قراءة القصص الإنجليزية كل ليلة",
        "grammar_ids": ["g007"],
        "target_words": ["story", "read", "dictionary", "notebook", "sentence", "learn"],
        "sentences": [
            {"en": "Tariq wants to speak and read English with confidence.", "ar": "يريد طارق أن يتحدث ويقرأ الإنجليزية بثقة.", "grammar_id": "g007"},
            {"en": "Every night, he reads one short story before bed.", "ar": "كل ليلة، يقرأ قصة قصيرة واحدة قبل النوم."},
            {"en": "When he sees a new word, he checks his offline dictionary.", "ar": "عندما يرى كلمة جديدة، يتحقق من قاموسه دون اتصال.", "grammar_id": "g007"},
            {"en": "He writes the word and a clear sentence in his notebook.", "ar": "يكتب الكلمة وجملة واضحة في دفتره."},
            {"en": "In the morning, he reviews his cards for five minutes.", "ar": "في الصباح، يراجع بطاقاته لمدة خمس دقائق."},
            {"en": "Step by step, his vocabulary grows stronger every week.", "ar": "خطوة بخطوة، تنمو مفرداته وتزداد قوة كل أسبوع."}
        ],
        "questions": [
            {
                "prompt": "What does Tariq do when he sees a new word?",
                "options": ["He closes the book", "He checks his offline dictionary", "He skips the page", "He calls his friend"],
                "answer_index": 1,
                "explanation_ar": "عندما يرى كلمة جديدة يتحقق من قاموسه (checks his offline dictionary)."
            },
            {
                "prompt": "How long does he review his cards in the morning?",
                "options": ["For five minutes", "For two hours", "All day", "Once a month"],
                "answer_index": 0,
                "explanation_ar": "يراجع بطاقاته في الصباح لمدة خمس دقائق (for five minutes)."
            }
        ]
    },
    {
        "id": "r008",
        "level": "A1",
        "topic": "society and community",
        "title_en": "The Neighborhood Library",
        "title_ar": "مكتبة الحي",
        "grammar_ids": ["g008"],
        "target_words": ["library", "quiet", "book", "history", "borrow", "student"],
        "sentences": [
            {"en": "There is a public library in the center of our town.", "ar": "توجد مكتبة عامة في وسط مدينتنا.", "grammar_id": "g008"},
            {"en": "The building is old, but the rooms are bright and quiet.", "ar": "المبنى قديم، لكن الغرف مضيئة وهادئة."},
            {"en": "Many students come here to study and read history books.", "ar": "يأتي العديد من الطلاب إلى هنا للدراسة وقراءة كتب التاريخ.", "grammar_id": "g008"},
            {"en": "You can borrow three books for two weeks for free.", "ar": "يمكنك استعارة ثلاثة كتب لمدة أسبوعين مجانًا."},
            {"en": "The librarian helps visitors find useful information quickly.", "ar": "يساعد أمين المكتبة الزوار في العثور على معلومات مفيدة بسرعة."},
            {"en": "A good library brings the whole community together.", "ar": "المكتبة الجيدة تجمع المجتمع بأكمله معًا."}
        ],
        "questions": [
            {
                "prompt": "How many books can you borrow for free?",
                "options": ["One book only", "Three books for two weeks", "Ten books for a year", "None"],
                "answer_index": 1,
                "explanation_ar": "النص يذكر أنه يمكنك استعارة ثلاثة كتب لمدة أسبوعين مجانًا."
            },
            {
                "prompt": "How are the rooms inside the library described?",
                "options": ["Dark and noisy", "Bright and quiet", "Closed and cold", "Crowded and loud"],
                "answer_index": 1,
                "explanation_ar": "النص يصف الغرف بأنها مضيئة وهادئة (bright and quiet)."
            }
        ]
    },
    # --- A2 MICRO-STORIES (r009 - r016, 80-120 words each) ---
    {
        "id": "r009",
        "level": "A2",
        "topic": "technology and software",
        "title_en": "Fixing a Website Bug Before Launch",
        "title_ar": "إصلاح خطأ برمجي في موقع قبل الإطلاق",
        "grammar_ids": ["g016"],
        "target_words": ["software", "developer", "system", "update", "error", "solution", "network"],
        "sentences": [
            {"en": "Omar works as a junior software developer at a technology company.", "ar": "يعمل عمر كمطور برمجيات مبتدئ في شركة تقنية.", "grammar_id": "g016"},
            {"en": "Yesterday evening, the team tested a new update for their mobile application.", "ar": "مساء أمس، اختبر الفريق تحديثًا جديدًا لتطبيق الهاتف الخاص بهم.", "grammar_id": "g016"},
            {"en": "Suddenly, an unexpected error appeared on the login screen.", "ar": "فجأة، ظهر خطأ غير متوقع على شاشة تسجيل الدخول."},
            {"en": "Users could not connect to the database when the network was slow.", "ar": "لم يتمكن المستخدمون من الاتصال بقاعدة البيانات عندما كانت الشبكة بطيئة."},
            {"en": "Omar carefully checked the code and found the exact problem in ten minutes.", "ar": "فحص عمر الكود بعناية ووجد المشكلة بالضبط في عشر دقائق.", "grammar_id": "g016"},
            {"en": "He wrote a clean solution and tested the system again with his colleague.", "ar": "كتب حلًا نظيفًا واختبر النظام مرة أخرى مع زميله."},
            {"en": "This morning, the application launched smoothly and received great feedback.", "ar": "هذا الصباح، انطلق التطبيق بسلاسة وتلقى تقييمات رائعة."}
        ],
        "questions": [
            {
                "prompt": "What problem appeared during the test yesterday evening?",
                "options": [
                    "The office lost electricity",
                    "An error appeared on the login screen when the network was slow",
                    "The computers were stolen",
                    "The designer deleted the logo"
                ],
                "answer_index": 1,
                "explanation_ar": "ظهر خطأ غير متوقع على شاشة تسجيل الدخول ولم يتمكن المستخدمون من الاتصال عندما كانت الشبكة بطيئة."
            },
            {
                "prompt": "How did Omar solve the issue?",
                "options": [
                    "He checked the code, wrote a clean solution, and tested the system again",
                    "He canceled the application launch",
                    "He bought a new phone",
                    "He waited until next month"
                ],
                "answer_index": 0,
                "explanation_ar": "فحص عمر الكود بعناية وكتب حلًا نظيفًا ثم اختبر النظام مجددًا."
            }
        ]
    },
    {
        "id": "r010",
        "level": "A2",
        "topic": "career and interview",
        "title_en": "Preparing for a First Tech Interview",
        "title_ar": "الاستعداد لأول مقابلة عمل تقنية",
        "grammar_ids": ["g017"],
        "target_words": ["interview", "experience", "project", "skill", "confident", "company", "prepare"],
        "sentences": [
            {"en": "Next Tuesday, Lina has an important job interview with an international company.", "ar": "يوم الثلاثاء القادم، لدى لينا مقابلة عمل مهمة مع شركة دولية.", "grammar_id": "g017"},
            {"en": "To prepare well, she researched the company's recent projects and goals.", "ar": "للاستعداد جيدًا، بحثت عن مشاريع الشركة الأخيرة وأهدافها."},
            {"en": "She also practiced explaining her technical skills in clear English sentences.", "ar": "كما تدربت على شرح مهاراتها التقنية بجمل إنجليزية واضحة.", "grammar_id": "g017"},
            {"en": "Her older brother asked her common interview questions for thirty minutes.", "ar": "طرح عليها أخوها الأكبر أسئلة المقابلات الشائعة لمدة ثلاثين دقيقة."},
            {"en": "At first, she spoke too fast because she felt a little nervous.", "ar": "في البداية، تحدثت بسرعة كبيرة لأنها شعرت ببعض التوتر."},
            {"en": "After three practice rounds, her answers became calm, organized, and confident.", "ar": "بعد ثلاث جولات تدريبية، أصبحت إجاباتها هادئة ومنظمة وواثقة."}
        ],
        "questions": [
            {
                "prompt": "How did Lina prepare for her interview?",
                "options": [
                    "She researched the company's projects and practiced answering in English",
                    "She memorized a dictionary without speaking",
                    "She stayed awake all night",
                    "She did not prepare at all"
                ],
                "answer_index": 0,
                "explanation_ar": "بحثت لينا عن مشاريع الشركة وتدربت على شرح مهاراتها التقنية بالإنجليزية مع أخيها."
            },
            {
                "prompt": "How did Lina's answers change after three practice rounds?",
                "options": [
                    "They became shorter and quieter",
                    "They became calm, organized, and confident",
                    "She forgot her technical skills",
                    "She decided not to attend"
                ],
                "answer_index": 1,
                "explanation_ar": "بعد ثلاث جولات تدريبية أصبحت إجاباتها هادئة ومنظمة وواثقة."
            }
        ]
    },
    {
        "id": "r011",
        "level": "A2",
        "topic": "food and culture",
        "title_en": "Friday Family Lunch at Grandmother's House",
        "title_ar": "غداء العائلة يوم الجمعة في بيت الجدة",
        "grammar_ids": ["g018"],
        "target_words": ["tradition", "recipe", "delicious", "conversation", "prepare", "together"],
        "sentences": [
            {"en": "Every Friday afternoon, our whole family gathers at my grandmother's house.", "ar": "بعد ظهر كل يوم جمعة، تجتمع عائلتنا بأكملها في منزل جدتي.", "grammar_id": "g018"},
            {"en": "This weekly meeting is a warm tradition that everyone enjoys.", "ar": "هذا اللقاء الأسبوعي هو تقليد دافئ يستمتع به الجميع."},
            {"en": "My grandmother prepares a delicious meal using a traditional family recipe.", "ar": "تحضر جدتي وجبة لذيذة باستخدام وصفة عائلية تقليدية.", "grammar_id": "g018"},
            {"en": "The younger cousins help set the table and serve fresh mint tea.", "ar": "يساعد الأبناء الأصغر سنًا في تجهيز الطاولة وتقديم شاي النعناع الطازج."},
            {"en": "During lunch, we share stories about our week and laugh together.", "ar": "خلال الغداء، نتبادل القصص حول أسبوعنا ونضحك معًا."},
            {"en": "Spending quality time with family makes us feel supported and grateful.", "ar": "قضاء وقت مميز مع العائلة يجعلنا نشعر بالدعم والامتنان."}
        ],
        "questions": [
            {
                "prompt": "How does the grandmother prepare the Friday meal?",
                "options": [
                    "She orders fast food from a restaurant",
                    "She uses a traditional family recipe",
                    "She asks the neighbors to cook",
                    "She only serves cold sandwiches"
                ],
                "answer_index": 1,
                "explanation_ar": "تحضر الجدة وجبة لذيذة باستخدام وصفة عائلية تقليدية (traditional family recipe)."
            },
            {
                "prompt": "How do the younger cousins help?",
                "options": [
                    "They set the table and serve fresh mint tea",
                    "They wash the cars outside",
                    "They go shopping during lunch",
                    "They repair the television"
                ],
                "answer_index": 0,
                "explanation_ar": "يساعدون في تجهيز الطاولة وتقديم شاي النعناع الطازج."
            }
        ]
    },
    {
        "id": "r012",
        "level": "A2",
        "topic": "health and psychology",
        "title_en": "Why Good Sleep Matters for Memory",
        "title_ar": "لماذا يهم النوم الجيد للذاكرة",
        "grammar_ids": ["g019"],
        "target_words": ["memory", "brain", "focus", "habit", "screen", "rest"],
        "sentences": [
            {"en": "Many students stay up late when they study for important exams.", "ar": "يسهر العديد من الطلاب لوقت متأخر عندما يدرسون للامتحانات المهمة.", "grammar_id": "g019"},
            {"en": "However, scientists explain that the brain needs deep rest to remember new information.", "ar": "ومع ذلك، يوضح العلماء أن الدماغ يحتاج إلى راحة عميقة لتذكر المعلومات الجديدة."},
            {"en": "While you sleep, your brain organizes the words and rules you learned during the day.", "ar": "أثناء نومك، ينظم دماغك الكلمات والقواعد التي تعلمتها خلال اليوم.", "grammar_id": "g019"},
            {"en": "Without enough sleep, it becomes difficult to focus and solve problems the next morning.", "ar": "بدون نوم كافٍ، يصبح من الصعب التركيز وحل المشكلات في صباح اليوم التالي."},
            {"en": "Turning off phone screens one hour before bed builds a healthy sleep habit.", "ar": "إيقاف شاشات الهاتف قبل النوم بساعة يبني عادة نوم صحية."},
            {"en": "Seven to eight hours of rest can improve both your mood and your memory.", "ar": "سبع إلى ثماني ساعات من الراحة يمكن أن تحسن مزاجك وذاكرتك معًا."}
        ],
        "questions": [
            {
                "prompt": "What does your brain do while you sleep?",
                "options": [
                    "It forgets everything from the day",
                    "It organizes the words and rules you learned during the day",
                    "It stops working completely",
                    "It makes you feel hungrier"
                ],
                "answer_index": 1,
                "explanation_ar": "ينظم الدماغ أثناء النوم الكلمات والقواعد التي تعلمتها خلال النهار."
            },
            {
                "prompt": "What habit helps improve sleep quality?",
                "options": [
                    "Drinking coffee at midnight",
                    "Turning off phone screens one hour before bed",
                    "Studying until four in the morning",
                    "Keeping bright lights on"
                ],
                "answer_index": 1,
                "explanation_ar": "إيقاف شاشات الهاتف قبل النوم بساعة يبني عادة نوم صحية."
            }
        ]
    },
    {
        "id": "r013",
        "level": "A2",
        "topic": "environment",
        "title_en": "Using Solar Energy in Desert Towns",
        "title_ar": "استخدام الطاقة الشمسية في المدن الصحراوية",
        "grammar_ids": ["g020"],
        "target_words": ["energy", "modern", "electricity", "environment", "reduce", "future"],
        "sentences": [
            {"en": "Sunny desert regions receive bright sunlight for most of the year.", "ar": "تتلقى المناطق الصحراوية المشمسة ضوء شمس ساطعًا معظم أيام السنة.", "grammar_id": "g020"},
            {"en": "Today, many towns are building solar panels to produce clean electricity.", "ar": "اليوم، تقوم العديد من المدن ببناء ألواح شمسية لإنتاج كهرباء نظيفة."},
            {"en": "Solar energy does not pollute the air or harm the natural environment.", "ar": "الطاقة الشمسية لا تلوث الهواء ولا تضر بالبيئة الطبيعية.", "grammar_id": "g020"},
            {"en": "In addition, it helps families and schools reduce their monthly electricity bills.", "ar": "بالإضافة إلى ذلك، فإنها تساعد العائلات والمدارس على تقليل فواتير الكهرباء الشهرية."},
            {"en": "Local engineers train young technicians to maintain the modern solar systems.", "ar": "يدرب المهندسون المحليون الفنيين الشباب على صيانة الأنظمة الشمسية الحديثة."},
            {"en": "Investing in renewable energy creates jobs and protects the future of our planet.", "ar": "الاستثمار في الطاقة المتجددة يخلق فرص عمل ويحمي مستقبل كوكبنا."}
        ],
        "questions": [
            {
                "prompt": "Why are desert regions ideal for solar panels?",
                "options": [
                    "Because they receive bright sunlight for most of the year",
                    "Because it rains every day",
                    "Because they have many rivers",
                    "Because the wind is cold"
                ],
                "answer_index": 0,
                "explanation_ar": "المناطق الصحراوية تتلقى ضوء شمس ساطعًا معظم أيام السنة."
            },
            {
                "prompt": "How does solar energy help families and schools?",
                "options": [
                    "It makes electricity bills higher",
                    "It reduces their monthly electricity bills and produces clean power",
                    "It replaces water pipes",
                    "It only works in winter"
                ],
                "answer_index": 1,
                "explanation_ar": "تساعد الطاقة الشمسية في تقليل فواتير الكهرباء الشهرية وإنتاج طاقة نظيفة."
            }
        ]
    },
    {
        "id": "r014",
        "level": "A2",
        "topic": "travel",
        "title_en": "Navigating an International Airport",
        "title_ar": "التنقل داخل مطار دولي",
        "grammar_ids": ["g021"],
        "target_words": ["airport", "flight", "passport", "passenger", "luggage", "information"],
        "sentences": [
            {"en": "Traveling through a large international airport for the first time can feel overwhelming.", "ar": "قد يبدو السفر عبر مطار دولي كبير لأول مرة أمرًا مربكًا.", "grammar_id": "g021"},
            {"en": "It is smart to arrive three hours before your flight departure time.", "ar": "من الذكاء الوصول قبل موعد إقلاع رحلتك بثلاث ساعات."},
            {"en": "First, you show your passport and ticket at the check-in desk to drop off your luggage.", "ar": "أولًا، تبرز جواز سفرك وتذكرتك عند مكتب التسجيل لتسليم أمتعتك.", "grammar_id": "g021"},
            {"en": "Next, passengers walk through security and follow the clear English signs to their gate.", "ar": "بعد ذلك، يمر المسافرون عبر الأمن ويتبعون اللافتات الإنجليزية الواضحة إلى بوابتهم."},
            {"en": "If your gate number changes, the information screens display the update immediately.", "ar": "إذا تغير رقم بوابتك، تعرض شاشات المعلومات التحديث فورًا."},
            {"en": "Knowing basic travel vocabulary makes every international journey much smoother.", "ar": "معرفة مفردات السفر الأساسية تجعل كل رحلة دولية أكثر سلاسة."}
        ],
        "questions": [
            {
                "prompt": "When should you arrive at the airport for an international flight?",
                "options": [
                    "Ten minutes before takeoff",
                    "Three hours before your flight departure time",
                    "One day after the flight",
                    "Only when the gate closes"
                ],
                "answer_index": 1,
                "explanation_ar": "ينصح النص بالوصول قبل موعد إقلاع الرحلة بثلاث ساعات."
            },
            {
                "prompt": "Where can passengers see updates if their gate number changes?",
                "options": [
                    "On the information screens",
                    "Inside their luggage",
                    "On their passport cover",
                    "In the taxi"
                ],
                "answer_index": 0,
                "explanation_ar": "شاشات المعلومات (information screens) تعرض التحديث فورًا."
            }
        ]
    },
    {
        "id": "r015",
        "level": "A2",
        "topic": "education and language",
        "title_en": "Learning From Mistakes in a Notebook",
        "title_ar": "التعلم من الأخطاء في دفتر الملاحظات",
        "grammar_ids": ["g022"],
        "target_words": ["mistake", "improve", "practice", "correct", "progress", "language"],
        "sentences": [
            {"en": "Many language learners feel shy when they make a mistake in English.", "ar": "يشعر العديد من متعلمي اللغة بالخجل عندما يرتكبون خطأً في الإنجليزية.", "grammar_id": "g022"},
            {"en": "In reality, mistakes are the most valuable part of the learning process.", "ar": "في الواقع، الأخطاء هي الجزء الأكثر قيمة في عملية التعلم."},
            {"en": "Whenever Youssef misses a grammar rule, he saves the sentence in his correction notebook.", "ar": "كلما أخطأ يوسف في قاعدة نحوية، يحفظ الجملة في دفتر التصحيح الخاص به.", "grammar_id": "g022"},
            {"en": "He compares his original sentence with the correct form and reads the Arabic explanation.", "ar": "يقارن جملته الأصلية بالصيغة الصحيحة ويقرأ الشرح العربي."},
            {"en": "Three days later, the app reminds him to practice that exact rule again.", "ar": "بعد ثلاثة أيام، يذكره التطبيق بالتدرب على تلك القاعدة بالذات مرة أخرى."},
            {"en": "Turning errors into active review is the fastest way to make real progress.", "ar": "تحويل الأخطاء إلى مراجعة نشطة هو أسرع طريقة لتحقيق تقدم حقيقي."}
        ],
        "questions": [
            {
                "prompt": "What does Youssef do when he misses a grammar rule?",
                "options": [
                    "He stops studying for a week",
                    "He saves the sentence in his correction notebook and reviews it",
                    "He deletes the app",
                    "He ignores the explanation"
                ],
                "answer_index": 1,
                "explanation_ar": "يحفظ الجملة في دفتر التصحيح ويقارنها بالصيغة الصحيحة."
            },
            {
                "prompt": "According to the passage, why are mistakes valuable?",
                "options": [
                    "Because turning errors into active review is the fastest way to make real progress",
                    "Because grammar rules never matter",
                    "Because they make tests longer",
                    "Because nobody notices them"
                ],
                "answer_index": 0,
                "explanation_ar": "تحويل الأخطاء إلى مراجعة نشطة هو أسرع طريقة لتحقيق تقدم حقيقي."
            }
        ]
    },
    {
        "id": "r016",
        "level": "A2",
        "topic": "history and science",
        "title_en": "How Ancient Scholars Preserved Knowledge",
        "title_ar": "كيف حفظ العلماء القدماء المعرفة",
        "grammar_ids": ["g023"],
        "target_words": ["history", "knowledge", "science", "translate", "library", "discover"],
        "sentences": [
            {"en": "Over a thousand years ago, Baghdad had one of the greatest libraries in history.", "ar": "قبل أكثر من ألف عام، كان في بغداد واحدة من أعظم المكتبات في التاريخ.", "grammar_id": "g023"},
            {"en": "Scholars traveled from many countries to study mathematics, astronomy, and medicine.", "ar": "سافر العلماء من بلدان عديدة لدراسة الرياضيات والفلك والطب."},
            {"en": "They translated important scientific books from Greek, Persian, and Sanskrit into Arabic.", "ar": "ترجموا كتبًا علمية مهمة من اليونانية والفارسية والسنسكريتية إلى العربية.", "grammar_id": "g023"},
            {"en": "In addition to translating, they tested ideas through careful experiments and observations.", "ar": "وبالإضافة إلى الترجمة، اختبروا الأفكار من خلال التجارب والملاحظات الدقيقة."},
            {"en": "Their discoveries later helped European universities develop modern science.", "ar": "ساعدت اكتشافاتهم لاحقًا الجامعات الأوروبية على تطوير العلوم الحديثة."},
            {"en": "Sharing knowledge across languages always builds a stronger human civilization.", "ar": "إن مشاركة المعرفة عبر اللغات تبني دائمًا حضارة إنسانية أقوى."}
        ],
        "questions": [
            {
                "prompt": "What subjects did scholars study in the great library of Baghdad?",
                "options": [
                    "Mathematics, astronomy, and medicine",
                    "Modern computer programming",
                    "Aviation and space rockets",
                    "Photography and cinema"
                ],
                "answer_index": 0,
                "explanation_ar": "سافر العلماء لدراسة الرياضيات والفلك والطب."
            },
            {
                "prompt": "How did the scholars test scientific ideas?",
                "options": [
                    "Through careful experiments and observations",
                    "By guessing without reading",
                    "By keeping books closed",
                    "By avoiding translations"
                ],
                "answer_index": 0,
                "explanation_ar": "اختبروا الأفكار من خلال التجارب والملاحظات الدقيقة (careful experiments and observations)."
            }
        ]
    }
]


def load_dictionary_headwords():
    """Loads all headwords and their levels/decks from assets/data_import."""
    headword_to_info = {}
    oxford_levels = {}

    files = [
        ("oxford5000_part1.json", "oxford5000"),
        ("oxford5000_part2.json", "oxford5000"),
        ("oxford5000_part3.json", "oxford5000"),
        ("oxford_opal.json", "oxford_opal"),
        ("oxford_phrases.json", "oxford_phrases"),
        ("everyday_expressions.json", "everyday_expressions"),
        ("essential_idioms.json", "essential_idioms"),
        ("ielts_core.json", "ielts_core"),
        ("it_industry.json", "it_industry"),
        ("cefr_c1.json", "cefr_c1"),
        ("pte_academic.json", "pte_academic"),
        ("business_english.json", "business_english"),
        ("career_interview.json", "career_interview"),
        ("medical_healthcare.json", "medical_healthcare"),
    ]

    for fname, default_deck in files:
        fpath = os.path.join(DATA_IMPORT_DIR, fname)
        if not os.path.exists(fpath):
            continue
        with open(fpath, "r", encoding="utf-8") as f:
            data = json.load(f)
        entries = data if isinstance(data, list) else data.get("entries", data.get("words", []))
        for item in entries:
            word = (item.get("text") or item.get("word") or "").strip().lower()
            if not word:
                continue
            deck = (item.get("deck") or default_deck).strip()
            lvl = (item.get("level") or "").strip().upper()
            if deck == "oxford5000" and lvl in {"A1", "A2", "B1", "B2", "C1"}:
                if word not in oxford_levels:
                    oxford_levels[word] = lvl
            if word not in headword_to_info:
                headword_to_info[word] = {
                    "word": word,
                    "decks": set(),
                    "level": lvl if lvl in {"A1", "A2", "B1", "B2", "C1"} else None
                }
            headword_to_info[word]["decks"].add(deck)

    for word, info in headword_to_info.items():
        if word in oxford_levels:
            info["level"] = oxford_levels[word]

    return headword_to_info


def stem_candidates(token: str):
    cands = [token]
    if token.endswith("ies") and len(token) > 4:
        cands.append(token[:-3] + "y")
    if token.endswith("es") and len(token) > 3:
        cands.append(token[:-2])
    if token.endswith("s") and len(token) > 3:
        cands.append(token[:-1])
    if token.endswith("ing") and len(token) > 5:
        cands.append(token[:-3])
        cands.append(token[:-3] + "e")
    if token.endswith("ed") and len(token) > 4:
        cands.append(token[:-2])
        cands.append(token[:-1])
    return cands


def extract_story_tokens(story_obj, headword_map):
    sentences = story_obj.get("sentences", [])
    full_text = " ".join(s.get("en", "") for s in sentences)
    raw_tokens = re.findall(r"[A-Za-z]+(?:'[A-Za-z]+)?", full_text.lower())

    content_tokens = []
    seen_tokens = set()
    matched_headwords = []
    seen_headwords = set()

    # First include explicit target_words if they match dictionary
    for tw in story_obj.get("target_words", []):
        clean_tw = tw.strip().lower()
        if clean_tw in headword_map and clean_tw not in seen_headwords:
            seen_headwords.add(clean_tw)
            matched_headwords.append(clean_tw)

    for tok in raw_tokens:
        if len(tok) <= 2 or tok in STOPWORDS:
            continue
        resolved = None
        for cand in stem_candidates(tok):
            if cand in headword_map:
                resolved = cand
                break
        norm_tok = resolved or tok
        if norm_tok not in seen_tokens and norm_tok not in STOPWORDS:
            seen_tokens.add(norm_tok)
            content_tokens.append(norm_tok)
        if resolved and resolved not in seen_headwords and resolved not in STOPWORDS:
            seen_headwords.add(resolved)
            matched_headwords.append(resolved)

    return content_tokens, matched_headwords


def main():
    with open(READING_JSON_PATH, "r", encoding="utf-8") as f:
        existing_texts = json.load(f)

    by_id = {item["id"]: item for item in existing_texts}
    for story in A1_A2_STORIES:
        by_id[story["id"]] = story

    # Sort stories by numeric id (r001 .. r090)
    def story_sort_key(item):
        digits = "".join(ch for ch in item.get("id", "") if ch.isdigit())
        return int(digits) if digits else 9999

    all_stories = sorted(by_id.values(), key=story_sort_key)
    headword_map = load_dictionary_headwords()

    links_payload = []
    for story in all_stories:
        content_tokens, matched_headwords = extract_story_tokens(story, headword_map)
        story_level = (story.get("level") or "B1").upper()

        # Prioritize headwords at or near the story's CEFR level for pre-teaching
        level_order = {
            "A1": ["A1", "A2", "B1"],
            "A2": ["A2", "A1", "B1"],
            "B1": ["B1", "B2", "A2"],
            "B2": ["B2", "C1", "B1"],
            "C1": ["C1", "B2", "B1"]
        }.get(story_level, ["B1", "B2"])

        def hw_rank(hw):
            lvl = headword_map.get(hw, {}).get("level")
            if lvl in level_order:
                return level_order.index(lvl)
            return 99

        prioritized_headwords = sorted(matched_headwords, key=hw_rank)
        if not story.get("target_words"):
            story["target_words"] = prioritized_headwords[:8]

        macro_topic = normalize_macro_topic(story.get("topic", ""), story.get("title_en", ""))
        links_payload.append({
            "text_id": story["id"],
            "level": story_level,
            "raw_topic": story.get("topic", ""),
            "macro_topic": macro_topic,
            "grammar_ids": story.get("grammar_ids", []),
            "target_words": story.get("target_words", [])[:8],
            "linked_headwords": prioritized_headwords[:20],
            "content_tokens": content_tokens
        })

    with open(READING_JSON_PATH, "w", encoding="utf-8") as f:
        json.dump(all_stories, f, ensure_ascii=False, indent=2)

    with open(LINKS_JSON_PATH, "w", encoding="utf-8") as f:
        json.dump(links_payload, f, ensure_ascii=False, indent=2)

    print(f"Updated {READING_JSON_PATH}: {len(all_stories)} total stories (A1={sum(1 for s in all_stories if s['level']=='A1')}, A2={sum(1 for s in all_stories if s['level']=='A2')}, B1={sum(1 for s in all_stories if s['level']=='B1')}, B2={sum(1 for s in all_stories if s['level']=='B2')}, C1={sum(1 for s in all_stories if s['level']=='C1')})")
    print(f"Generated {LINKS_JSON_PATH}: {len(links_payload)} linked story records.")


if __name__ == "__main__":
    main()

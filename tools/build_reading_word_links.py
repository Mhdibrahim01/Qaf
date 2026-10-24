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
    {
        "id": "r001",
        "level": "A1",
        "topic": "daily routine",
        "title_en": "A Busy Morning at Home",
        "title_ar": "صباح مزدحم في المنزل",
        "grammar_ids": [
            "g001"
        ],
        "target_words": [
            "morning",
            "early",
            "breakfast",
            "coffee",
            "kitchen",
            "ready"
        ],
        "sentences": [
            {
                "en": "Salma is an early riser who loves the peaceful morning.",
                "ar": "سلمى تستيقظ مبكرًا وتحب الصباح الهادئ.",
                "grammar_id": "g001"
            },
            {
                "en": "Her kitchen is warm and smells of freshly brewed coffee.",
                "ar": "مطبخها دافئ وتفوح منه رائحة القهوة الطازجة.",
                "grammar_id": "g001"
            },
            {
                "en": "Her two younger brothers are still asleep in their room.",
                "ar": "أخواها الأصغر سنًا لا يزالان نائمين في غرفتهما.",
                "grammar_id": "g001"
            },
            {
                "en": "I am always glad to have a quiet moment before work.",
                "ar": "أنا سعيد دائمًا بالحصول على لحظة هادئة قبل العمل.",
                "grammar_id": "g001"
            },
            {
                "en": "Salma puts her notebook and pen in her bag.",
                "ar": "تضع سلمى دفترها وقلمها في حقيبتها."
            },
            {
                "en": "She is ready to start a productive day with a bright smile.",
                "ar": "هي مستعدة لبدء يوم مثمر بابتسامة مشرقة.",
                "grammar_id": "g001"
            }
        ],
        "questions": [
            {
                "prompt": "What is the kitchen like in the morning?",
                "options": [
                    "Cold and dark",
                    "Warm and smells of fresh coffee",
                    "Very noisy",
                    "Empty without food"
                ],
                "answer_index": 1,
                "explanation_ar": "النص يذكر أن المطبخ دافئ وتفوح منه رائحة القهوة الطازجة."
            },
            {
                "prompt": "Who is still asleep in the room?",
                "options": [
                    "Her two younger brothers",
                    "Her father",
                    "Her neighbors",
                    "Her teacher"
                ],
                "answer_index": 0,
                "explanation_ar": "النص يوضح أن أخويها الأصغر سنًا لا يزالان نائمين."
            }
        ]
    },
    {
        "id": "r002",
        "level": "A1",
        "topic": "technology",
        "title_en": "My First Computer Class",
        "title_ar": "درسي الأول في الحاسوب",
        "grammar_ids": [
            "g002"
        ],
        "target_words": [
            "computer",
            "screen",
            "keyboard",
            "message",
            "password",
            "helpful"
        ],
        "sentences": [
            {
                "en": "Today is my first computer class, and I am very excited.",
                "ar": "اليوم هو درسي الأول في الحاسوب، وأنا متحمس جدًا.",
                "grammar_id": "g002"
            },
            {
                "en": "Our teacher introduces her syllabus on our shared screen.",
                "ar": "معلمتنا تقدم خطتها الدراسية على شاشتنا المشتركة.",
                "grammar_id": "g002"
            },
            {
                "en": "He explains how computers process our digital messages.",
                "ar": "يشرح كيف تعالج الحواسيب رسائلنا الرقمية.",
                "grammar_id": "g002"
            },
            {
                "en": "We write short emails to our friends to practice our typing.",
                "ar": "نكتب رسائل بريد إلكتروني قصيرة لأصدقائنا للتدرب على كتابتنا.",
                "grammar_id": "g002"
            },
            {
                "en": "They are happy to help their students with difficult passwords.",
                "ar": "هم سعداء بمساعدة طلابهم في كلمات المرور الصعبة.",
                "grammar_id": "g002"
            },
            {
                "en": "My computer skills improve noticeably every single week.",
                "ar": "مهاراتي في الحاسوب تتحسن بشكل ملحوظ كل أسبوع.",
                "grammar_id": "g002"
            }
        ],
        "questions": [
            {
                "prompt": "What do the students write to practice typing?",
                "options": [
                    "A thick novel",
                    "Short emails to their friends",
                    "A paper letter",
                    "A song"
                ],
                "answer_index": 1,
                "explanation_ar": "النص يقول: We write short emails to our friends."
            },
            {
                "prompt": "How does the student feel about the class?",
                "options": [
                    "Very excited",
                    "Bored",
                    "Angry",
                    "Tired"
                ],
                "answer_index": 0,
                "explanation_ar": "الجملة الأولى تقول: I am very excited."
            }
        ]
    },
    {
        "id": "r003",
        "level": "A1",
        "topic": "work and office",
        "title_en": "A New Job at the Office",
        "title_ar": "وظيفة جديدة في المكتب",
        "grammar_ids": [
            "g003"
        ],
        "target_words": [
            "office",
            "manager",
            "meeting",
            "team",
            "desk",
            "schedule"
        ],
        "sentences": [
            {
                "en": "Karim starts a new job at an international office.",
                "ar": "يبدأ كريم وظيفة جديدة في مكتب دولي.",
                "grammar_id": "g003"
            },
            {
                "en": "A friendly manager welcomes him and introduces the team.",
                "ar": "مدير ودود يرحب به ويعرفه على الفريق.",
                "grammar_id": "g003"
            },
            {
                "en": "The modern office has a bright cafeteria and a quiet library.",
                "ar": "المكتب الحديث يحتوي على مقصف مشرق ومكتبة هادئة.",
                "grammar_id": "g003"
            },
            {
                "en": "They hold an important morning meeting at nine o'clock.",
                "ar": "يعقدون اجتماعًا صباحيًا مهمًا في الساعة التاسعة.",
                "grammar_id": "g003"
            },
            {
                "en": "Karim places a laptop and a notebook on the wooden desk.",
                "ar": "يضع كريم حاسوبًا محمولًا ودفترًا على المكتب الخشبي.",
                "grammar_id": "g003"
            },
            {
                "en": "He feels happy with the warm atmosphere in the company.",
                "ar": "يشعر بالسعادة مع الأجواء الدافئة في الشركة.",
                "grammar_id": "g003"
            }
        ],
        "questions": [
            {
                "prompt": "Where does Karim start his new job?",
                "options": [
                    "At an international office",
                    "On a ship",
                    "In a hospital",
                    "At a farm"
                ],
                "answer_index": 0,
                "explanation_ar": "النص يوضح: Karim starts a new job at an international office."
            },
            {
                "prompt": "What time is the important morning meeting?",
                "options": [
                    "At seven",
                    "At nine o'clock",
                    "At midnight",
                    "At four"
                ],
                "answer_index": 1,
                "explanation_ar": "الاجتماع يبدأ في الساعة التاسعة (at nine o'clock)."
            }
        ]
    },
    {
        "id": "r004",
        "level": "A1",
        "topic": "health",
        "title_en": "Walking for Good Health",
        "title_ar": "المشي من أجل صحة جيدة",
        "grammar_ids": [
            "g004"
        ],
        "target_words": [
            "health",
            "exercise",
            "water",
            "energy",
            "doctor",
            "healthy"
        ],
        "sentences": [
            {
                "en": "Doctor Nadia reminds her patients that daily walks protect our hearts.",
                "ar": "تذكر الدكتورة نادية مرضاها بأن المشي اليومي يحمي قلوبنا.",
                "grammar_id": "g004"
            },
            {
                "en": "Many doctors recommend thirty minutes of exercise in green parks.",
                "ar": "ينصح العديد من الأطباء بثلاثين دقيقة من التمارين في الحدائق الخضراء.",
                "grammar_id": "g004"
            },
            {
                "en": "My neighbors and I walk between tall trees after work.",
                "ar": "أمشي أنا وجيراني بين الأشجار العالية بعد العمل.",
                "grammar_id": "g004"
            },
            {
                "en": "We carry two cold water bottles in our backpacks.",
                "ar": "نحمل زجاجتين من الماء البارد في حقائب الظهر الخاصة بنا.",
                "grammar_id": "g004"
            },
            {
                "en": "These healthy habits give us positive thoughts and better sleep.",
                "ar": "هذه العادات الصحية تمنحنا أفكارًا إيجابية ونومًا أفضل.",
                "grammar_id": "g004"
            },
            {
                "en": "Regular outdoor activities help adults build stronger bodies.",
                "ar": "الأنشطة الخارجية المنتظمة تساعد البالغين على بناء أجساد أقوى.",
                "grammar_id": "g004"
            }
        ],
        "questions": [
            {
                "prompt": "Where do the doctors recommend walking?",
                "options": [
                    "In green parks",
                    "On crowded highways",
                    "Inside basements",
                    "In dark rooms"
                ],
                "answer_index": 0,
                "explanation_ar": "الأطباء ينصحون بالمشي في الحدائق الخضراء (in green parks)."
            },
            {
                "prompt": "What do the walkers carry in their backpacks?",
                "options": [
                    "Two cold water bottles",
                    "Heavy metal tools",
                    "Old magazines",
                    "Nothing"
                ],
                "answer_index": 0,
                "explanation_ar": "يحملون زجاجتين من الماء البارد (two cold water bottles)."
            }
        ]
    },
    {
        "id": "r005",
        "level": "A1",
        "topic": "environment",
        "title_en": "Planting Trees in Our Garden",
        "title_ar": "زراعة الأشجار في حديقتنا",
        "grammar_ids": [
            "g005"
        ],
        "target_words": [
            "garden",
            "tree",
            "plant",
            "nature",
            "clean",
            "flower"
        ],
        "sentences": [
            {
                "en": "This garden behind our house is our favorite family space.",
                "ar": "هذه الحديقة خلف منزلنا هي المكان العائلي المفضل لدينا.",
                "grammar_id": "g005"
            },
            {
                "en": "Look at that tall palm tree over there across the fence.",
                "ar": "انظر إلى تلك النخلة الطويلة هناك عبر السياج.",
                "grammar_id": "g005"
            },
            {
                "en": "My father is planting these young lemon seedlings in the soil.",
                "ar": "والدي يزرع شتلات الليمون الصغيرة هذه في التربة.",
                "grammar_id": "g005"
            },
            {
                "en": "Those ancient olive trees produce sweet fruit every autumn.",
                "ar": "أشجار الزيتون القديمة تلك تنتج ثمارًا حلوة كل خريف.",
                "grammar_id": "g005"
            },
            {
                "en": "This peaceful activity connects us closely with the earth.",
                "ar": "هذا النشاط الهادئ يربطنا بشكل وثيق بالأرض.",
                "grammar_id": "g005"
            },
            {
                "en": "These small green actions help protect our natural environment.",
                "ar": "هذه الأفعال الخضراء الصغيرة تساعد في حماية بيئتنا الطبيعية.",
                "grammar_id": "g005"
            }
        ],
        "questions": [
            {
                "prompt": "What is the father planting in the soil?",
                "options": [
                    "These young lemon seedlings",
                    "Old plastic flowers",
                    "Dry stones",
                    "Tall fences"
                ],
                "answer_index": 0,
                "explanation_ar": "يزرع والده شتلات الليمون الصغيرة هذه (these young lemon seedlings)."
            },
            {
                "prompt": "Where is the tall palm tree located?",
                "options": [
                    "Across the fence",
                    "Inside the kitchen",
                    "Under the table",
                    "On the roof"
                ],
                "answer_index": 0,
                "explanation_ar": "توجد النخلة الطويلة عبر السياج (across the fence)."
            }
        ]
    },
    {
        "id": "r006",
        "level": "A1",
        "topic": "travel",
        "title_en": "A Train Trip to the Coast",
        "title_ar": "رحلة بالقطار إلى الساحل",
        "grammar_ids": [
            "g006"
        ],
        "target_words": [
            "train",
            "ticket",
            "station",
            "travel",
            "window",
            "beach"
        ],
        "sentences": [
            {
                "en": "There is a fast passenger train departing at eight o'clock.",
                "ar": "يوجد قطار ركاب سريع يغادر في الساعة الثامنة.",
                "grammar_id": "g006"
            },
            {
                "en": "There are many excited travelers waiting on the station platform.",
                "ar": "يوجد العديد من المسافرين المتحمسين ينتظرون على رصيف المحطة.",
                "grammar_id": "g006"
            },
            {
                "en": "There is a comfortable seat near the window with a seaside view.",
                "ar": "يوجد مقعد مريح بالقرب من النافذة مع إطلالة بحرية.",
                "grammar_id": "g006"
            },
            {
                "en": "There are green rolling hills and small farmhouses along the route.",
                "ar": "توجد تلال خضراء ومزارع صغيرة على طول المسار.",
                "grammar_id": "g006"
            },
            {
                "en": "There is a modern cafe car where passengers can buy warm drinks.",
                "ar": "يوجد عربة مقهى حديثة حيث يمكن للركاب شراء مشروبات ساخنة.",
                "grammar_id": "g006"
            },
            {
                "en": "There are beautiful sandy beaches waiting for us at our destination.",
                "ar": "توجد شواطئ رملية جميلة بانتظارنا في وجهتنا.",
                "grammar_id": "g006"
            }
        ],
        "questions": [
            {
                "prompt": "What time does the fast passenger train depart?",
                "options": [
                    "At eight o'clock",
                    "At noon",
                    "At five o'clock",
                    "At midnight"
                ],
                "answer_index": 0,
                "explanation_ar": "يوجد قطار ركاب سريع يغادر في الساعة الثامنة (at eight o'clock)."
            },
            {
                "prompt": "Where can passengers buy warm drinks?",
                "options": [
                    "In a modern cafe car",
                    "At home",
                    "On the train roof",
                    "In the luggage area"
                ],
                "answer_index": 0,
                "explanation_ar": "يمكنهم شراء المشروبات في عربة المقهى الحديثة (in a modern cafe car)."
            }
        ]
    },
    {
        "id": "r007",
        "level": "A1",
        "topic": "daily life",
        "title_en": "Reading English Stories Every Night",
        "title_ar": "قراءة القصص الإنجليزية كل ليلة",
        "grammar_ids": [
            "g007"
        ],
        "target_words": [
            "story",
            "dictionary",
            "card",
            "confidence",
            "vocabulary",
            "review"
        ],
        "sentences": [
            {
                "en": "Tariq wants to speak and read English with great confidence.",
                "ar": "يريد طارق أن يتحدث ويقرأ الإنجليزية بثقة كبيرة.",
                "grammar_id": "g007"
            },
            {
                "en": "Every night, he reads one graded story before he sleeps.",
                "ar": "كل ليلة، يقرأ قصة متدرجة واحدة قبل أن ينام.",
                "grammar_id": "g007"
            },
            {
                "en": "When he discovers a difficult word, he checks an offline dictionary.",
                "ar": "عندما يكتشف كلمة صعبة، يتحقق من قاموس دون اتصال بالإنترنت.",
                "grammar_id": "g007"
            },
            {
                "en": "He writes the new definition in his personal vocabulary notebook.",
                "ar": "يكتب التعريف الجديد في دفتر مفرداته الشخصي.",
                "grammar_id": "g007"
            },
            {
                "en": "In the morning, he reviews his flashcards for ten minutes.",
                "ar": "في الصباح، يراجع بطاقاته التعليمية لمدة عشر دقائق.",
                "grammar_id": "g007"
            },
            {
                "en": "He does not waste his study time on social media distractions.",
                "ar": "هو لا يضيع وقت دراسته في مشتتات وسائل التواصل الاجتماعي.",
                "grammar_id": "g007"
            }
        ],
        "questions": [
            {
                "prompt": "How often does Tariq read a graded story?",
                "options": [
                    "Every night before he sleeps",
                    "Once a year",
                    "Only on holidays",
                    "Never"
                ],
                "answer_index": 0,
                "explanation_ar": "يقرأ طارق قصة كل ليلة قبل النوم (Every night before he sleeps)."
            },
            {
                "prompt": "What does Tariq do when he discovers a difficult word?",
                "options": [
                    "He checks an offline dictionary",
                    "He closes the book",
                    "He throws his notebook",
                    "He goes to sleep immediately"
                ],
                "answer_index": 0,
                "explanation_ar": "يتحقق من قاموس دون اتصال (checks an offline dictionary)."
            }
        ]
    },
    {
        "id": "r008",
        "level": "A1",
        "topic": "community",
        "title_en": "The Neighborhood Library",
        "title_ar": "مكتبة الحي",
        "grammar_ids": [
            "g008"
        ],
        "target_words": [
            "library",
            "building",
            "book",
            "librarian",
            "quiet",
            "student"
        ],
        "sentences": [
            {
                "en": "Right now, many university students are studying quietly in the library.",
                "ar": "في الوقت الحالي، يدرس العديد من طلاب الجامعة بهدوء في المكتبة.",
                "grammar_id": "g008"
            },
            {
                "en": "The chief librarian is helping an elderly visitor find an old atlas.",
                "ar": "أمينة المكتبة الرئيسية تساعد زائرًا مسنًا في العثور على أطلس قديم.",
                "grammar_id": "g008"
            },
            {
                "en": "Two young researchers are typing their project notes on laptops.",
                "ar": "باحثان شابان يكتبان ملاحظات مشروعهما على الحواسيب المحمولة.",
                "grammar_id": "g008"
            },
            {
                "en": "Outside the window, yellow autumn leaves are falling on the pavement.",
                "ar": "خارج النافذة، تتساقط أوراق الخريف الصفراء على الرصيف.",
                "grammar_id": "g008"
            },
            {
                "en": "I am reading a fascinating chapter about modern technology and history.",
                "ar": "أنا أقرأ فصلاً رائعاً عن التكنولوجيا الحديثة والتاريخ.",
                "grammar_id": "g008"
            },
            {
                "en": "Everyone is enjoying the calm and welcoming atmosphere of the study hall.",
                "ar": "الجميع يستمتعون بالأجواء الهادئة والمرحبة في قاعة الدراسة.",
                "grammar_id": "g008"
            }
        ],
        "questions": [
            {
                "prompt": "What are the university students doing right now in the library?",
                "options": [
                    "They are studying quietly",
                    "They are shouting loudly",
                    "They are running fast",
                    "They are sleeping"
                ],
                "answer_index": 0,
                "explanation_ar": "الطلاب يدرسون بهدوء في المكتبة (are studying quietly)."
            },
            {
                "prompt": "Who is the chief librarian helping?",
                "options": [
                    "An elderly visitor",
                    "A police officer",
                    "A famous pilot",
                    "No one"
                ],
                "answer_index": 0,
                "explanation_ar": "أمينة المكتبة تساعد زائرًا مسنًا (an elderly visitor)."
            }
        ]
    },
    {
        "id": "r009",
        "level": "A2",
        "topic": "technology",
        "title_en": "Fixing a Website Bug Before Launch",
        "title_ar": "إصلاح خلل برمجي قبل إطلاق الموقع",
        "grammar_ids": [
            "g016"
        ],
        "target_words": [
            "software",
            "developer",
            "database",
            "update",
            "screen",
            "launch"
        ],
        "sentences": [
            {
                "en": "Yesterday evening, Omar was working late at the technology company.",
                "ar": "مساء أمس، كان عمر يعمل حتى وقت متأخر في شركة التكنولوجيا.",
                "grammar_id": "g016"
            },
            {
                "en": "While the developers were testing the update, a sudden error occurred.",
                "ar": "بينما كان المطورون يختبرون التحديث، حدث خطأ مفاجئ.",
                "grammar_id": "g016"
            },
            {
                "en": "Users were experiencing delays while the server was restarting.",
                "ar": "كان المستخدمون يواجهون تأخيرات بينما كان الخادم يعيد التشغيل.",
                "grammar_id": "g016"
            },
            {
                "en": "Omar was carefully analyzing the database logs when he found the bug.",
                "ar": "كان عمر يحلل سجلات قاعدة البيانات بعناية عندما وجد الخلل.",
                "grammar_id": "g016"
            },
            {
                "en": "His colleagues were collaborating actively to fix the network code.",
                "ar": "كان زملاؤه يتعاونون بنشاط لإصلاح كود الشبكة.",
                "grammar_id": "g016"
            },
            {
                "en": "By midnight, the application was functioning smoothly without interruptions.",
                "ar": "بحلول منتصف الليل، كان التطبيق يعمل بسلاسة دون انقطاع.",
                "grammar_id": "g016"
            }
        ],
        "questions": [
            {
                "prompt": "What was Omar doing yesterday evening?",
                "options": [
                    "He was working late at the tech company",
                    "He was watching television",
                    "He was sleeping",
                    "He was playing tennis"
                ],
                "answer_index": 0,
                "explanation_ar": "كان عمر يعمل حتى وقت متأخر في شركة التكنولوجيا (was working late)."
            },
            {
                "prompt": "When did the unexpected error occur?",
                "options": [
                    "While the developers were testing the update",
                    "After three years",
                    "Before the company opened",
                    "During breakfast"
                ],
                "answer_index": 0,
                "explanation_ar": "حدث الخطأ بينما كان المطورون يختبرون التحديث (were testing the update)."
            }
        ]
    },
    {
        "id": "r010",
        "level": "A2",
        "topic": "career and interview",
        "title_en": "Preparing for a First Tech Interview",
        "title_ar": "الاستعداد لمقابلة العمل التقنية الأولى",
        "grammar_ids": [
            "g017"
        ],
        "target_words": [
            "interview",
            "international",
            "skills",
            "question",
            "nervous",
            "confident"
        ],
        "sentences": [
            {
                "en": "Lina has prepared thoroughly for her international job interview.",
                "ar": "استعدت لينا بدقة لمقابلة عملها الدولية.",
                "grammar_id": "g017"
            },
            {
                "en": "She has researched the software company's recent artificial intelligence projects.",
                "ar": "بحثت في مشاريع الذكاء الاصطناعي الأخيرة لشركة البرمجيات.",
                "grammar_id": "g017"
            },
            {
                "en": "Her older brother has helped her practice common technical questions.",
                "ar": "ساعدها شقيقها الأكبر في ممارسة الأسئلة التقنية الشائعة.",
                "grammar_id": "g017"
            },
            {
                "en": "She has improved her English pronunciation through daily audio drills.",
                "ar": "حسّنت نطقها للغة الإنجليزية من خلال التدريبات الصوتية اليومية.",
                "grammar_id": "g017"
            },
            {
                "en": "Lina has already answered three mock interview sessions with confidence.",
                "ar": "أجابت لينا بالفعل على ثلاث جلسات مقابلات تجريبية بثقة.",
                "grammar_id": "g017"
            },
            {
                "en": "Her dedicated efforts have created a calm and positive mindset.",
                "ar": "خلقت جهودها المخلصة عقلية هادئة وإيجابية.",
                "grammar_id": "g017"
            }
        ],
        "questions": [
            {
                "prompt": "What has Lina researched before her interview?",
                "options": [
                    "The company's recent artificial intelligence projects",
                    "Ancient Greek history",
                    "Fashion magazines",
                    "Restaurant menus"
                ],
                "answer_index": 0,
                "explanation_ar": "بحثت في مشاريع الذكاء الاصطناعي الأخيرة للشركة."
            },
            {
                "prompt": "How has Lina improved her English pronunciation?",
                "options": [
                    "Through daily audio drills",
                    "By remaining silent",
                    "By avoiding speaking",
                    "Through cooking lessons"
                ],
                "answer_index": 0,
                "explanation_ar": "حسنت نطقها من خلال تدريبات صوتية يومية (through daily audio drills)."
            }
        ]
    },
    {
        "id": "r011",
        "level": "A2",
        "topic": "family and traditions",
        "title_en": "Friday Family Lunch at Grandmother's House",
        "title_ar": "غداء العائلة يوم الجمعة في منزل الجدة",
        "grammar_ids": [
            "g018"
        ],
        "target_words": [
            "family",
            "tradition",
            "delicious",
            "cousin",
            "support",
            "recipe"
        ],
        "sentences": [
            {
                "en": "My grandmother's house is warmer and cozier than any modern apartment.",
                "ar": "منزل جدتي أكثر دفئًا وراحة من أي شقة حديثة.",
                "grammar_id": "g018"
            },
            {
                "en": "She prepares the most delicious traditional meals in our entire city.",
                "ar": "هي تعد أشهى الوجبات التقليدية في مدينتنا بأكملها.",
                "grammar_id": "g018"
            },
            {
                "en": "Today, our family dining table is much fuller than usual with happy relatives.",
                "ar": "اليوم، مائدة طعام عائلتنا أكثر امتلاءً بكثير من المعتاد بأقارب سعداء.",
                "grammar_id": "g018"
            },
            {
                "en": "The younger cousins are more energetic and playful than the adults.",
                "ar": "أبناء العم الأصغر سنًا أكثر حيوية ومرحًا من البالغين.",
                "grammar_id": "g018"
            },
            {
                "en": "Friday family lunch is the most special and heartwarming tradition of our week.",
                "ar": "غداء العائلة يوم الجمعة هو التقليد الأكثر تميزًا ودفئًا في أسبوعنا.",
                "grammar_id": "g018"
            },
            {
                "en": "Being together makes our relationships stronger and deeper over time.",
                "ar": "التواجد معًا يجعل علاقاتنا أقوى وأعمق مع مرور الوقت.",
                "grammar_id": "g018"
            }
        ],
        "questions": [
            {
                "prompt": "How does grandmother's house compare to modern apartments?",
                "options": [
                    "It is warmer and cozier",
                    "It is colder and darker",
                    "It is smaller and noisier",
                    "It is empty"
                ],
                "answer_index": 0,
                "explanation_ar": "المنزل أكثر دفئًا وراحة (warmer and cozier)."
            },
            {
                "prompt": "Who is more energetic than the adults?",
                "options": [
                    "The younger cousins",
                    "The neighbor's cat",
                    "The grandfather",
                    "No one"
                ],
                "answer_index": 0,
                "explanation_ar": "أبناء العم الأصغر سنًا أكثر حيوية (The younger cousins are more energetic)."
            }
        ]
    },
    {
        "id": "r012",
        "level": "A2",
        "topic": "health and science",
        "title_en": "Why Good Sleep Matters for Memory",
        "title_ar": "أهمية النوم الجيد للذاكرة",
        "grammar_ids": [
            "g019"
        ],
        "target_words": [
            "memory",
            "sleep",
            "brain",
            "habit",
            "focus",
            "rest"
        ],
        "sentences": [
            {
                "en": "Scientists explain that deep sleep will significantly strengthen your memory.",
                "ar": "يوضح العلماء أن النوم العميق سيقوي ذاكرتك بشكل كبير.",
                "grammar_id": "g019"
            },
            {
                "en": "Tonight, I am going to turn off my digital screens an hour before bed.",
                "ar": "الليلة، سأقوم بإيقاف شاشاتي الرقمية قبل النوم بساعة.",
                "grammar_id": "g019"
            },
            {
                "en": "If you get eight hours of rest, your brain will organize newly learned facts easily.",
                "ar": "إذا حصلت على ثماني ساعات من الراحة، سينظم دماغك الحقائق المتعلمة حديثًا بسهولة.",
                "grammar_id": "g019"
            },
            {
                "en": "Our biology teacher is going to give an interesting presentation on sleep cycles.",
                "ar": "سيقدم معلم الأحياء لدينا عرضًا تقديميًا ممتعًا حول دورات النوم.",
                "grammar_id": "g019"
            },
            {
                "en": "Consistent sleep schedules will help students perform better in difficult exams.",
                "ar": "جداول النوم المنتظمة ستساعد الطلاب على الأداء بشكل أفضل في الامتحانات الصعبة.",
                "grammar_id": "g019"
            },
            {
                "en": "We are going to adopt healthier bedtime routines starting this evening.",
                "ar": "سنعتمد روتين نوم أكثر صحة بدءًا من هذا المساء.",
                "grammar_id": "g019"
            }
        ],
        "questions": [
            {
                "prompt": "What is the speaker going to do tonight before bed?",
                "options": [
                    "Turn off digital screens an hour before bed",
                    "Drink strong black coffee",
                    "Play video games all night",
                    "Watch loud action movies"
                ],
                "answer_index": 0,
                "explanation_ar": "سيقوم بإيقاف الشاشات الرقمية قبل النوم بساعة."
            },
            {
                "prompt": "How does deep sleep help the brain?",
                "options": [
                    "It will strengthen memory and organize facts",
                    "It makes people forget everything",
                    "It damages focus",
                    "It causes exam stress"
                ],
                "answer_index": 0,
                "explanation_ar": "يقوي الذاكرة وينظم الحقائق (will significantly strengthen your memory)."
            }
        ]
    },
    {
        "id": "r013",
        "level": "A2",
        "topic": "environment and technology",
        "title_en": "Using Solar Energy in Desert Towns",
        "title_ar": "استخدام الطاقة الشمسية في بلدات الصحراء",
        "grammar_ids": [
            "g020"
        ],
        "target_words": [
            "solar",
            "energy",
            "clean",
            "electricity",
            "engineer",
            "future"
        ],
        "sentences": [
            {
                "en": "Tomorrow morning, our engineering team is visiting the desert solar facility.",
                "ar": "صباح الغد، يزور فريقنا الهندسي منشأة الطاقة الشمسية في الصحراء.",
                "grammar_id": "g020"
            },
            {
                "en": "Next month, the local town council is installing modern solar streetlights.",
                "ar": "الشهر المقبل، يركب المجلس البلدي المحلي أعمدة إنارة شمسية حديثة في الشوارع.",
                "grammar_id": "g020"
            },
            {
                "en": "The university is hosting a green energy conference this coming Thursday.",
                "ar": "تستضيف الجامعة مؤتمرًا للطاقة الخضراء يوم الخميس القادم.",
                "grammar_id": "g020"
            },
            {
                "en": "Two foreign specialists are arriving on Friday to train young technicians.",
                "ar": "يصل متخصصان أجنبيان يوم الجمعة لتدريب الفنيين الشباب.",
                "grammar_id": "g020"
            },
            {
                "en": "Our department is launching a community solar initiative next week.",
                "ar": "يطلق قسمنا مبادرة مجتمعية للطاقة الشمسية الأسبوع المقبل.",
                "grammar_id": "g020"
            },
            {
                "en": "We are meeting with regional environmental leaders next Monday afternoon.",
                "ar": "نحن نجتمع مع قادة البيئة الإقليميين بعد ظهر يوم الإثنين المقبل.",
                "grammar_id": "g020"
            }
        ],
        "questions": [
            {
                "prompt": "When is the engineering team visiting the solar facility?",
                "options": [
                    "Tomorrow morning",
                    "Next year",
                    "Three years ago",
                    "Last night"
                ],
                "answer_index": 0,
                "explanation_ar": "يزور الفريق المنشأة صباح الغد (Tomorrow morning)."
            },
            {
                "prompt": "What is the university hosting this coming Thursday?",
                "options": [
                    "A green energy conference",
                    "A sports race",
                    "A magic show",
                    "A cooking contest"
                ],
                "answer_index": 0,
                "explanation_ar": "تستضيف الجامعة مؤتمرًا للطاقة الخضراء (a green energy conference)."
            }
        ]
    },
    {
        "id": "r014",
        "level": "A2",
        "topic": "travel",
        "title_en": "Navigating an International Airport",
        "title_ar": "التنقل في مطار دولي",
        "grammar_ids": [
            "g021"
        ],
        "target_words": [
            "airport",
            "passport",
            "ticket",
            "security",
            "gate",
            "journey"
        ],
        "sentences": [
            {
                "en": "International travelers must hold a valid passport and travel visa before departure.",
                "ar": "يجب على المسافرين الدوليين حمل جواز سفر وتأشيرة سفر ساريين قبل المغادرة.",
                "grammar_id": "g021"
            },
            {
                "en": "You have to arrive at the airport three hours early for international flights.",
                "ar": "عليك الوصول إلى المطار قبل ثلاث ساعات من موعد الرحلات الدولية.",
                "grammar_id": "g021"
            },
            {
                "en": "Passengers don't have to print paper tickets if they use the digital airline app.",
                "ar": "لا يضطر المسافرون لطباعة تذاكر ورقية إذا استخدموا تطبيق شركة الطيران الرقمي.",
                "grammar_id": "g021"
            },
            {
                "en": "Everyone must pass through security gates and follow airport safety procedures.",
                "ar": "يجب على الجميع المرور عبر بوابات الأمن واتباع إجراءات سلامة المطار.",
                "grammar_id": "g021"
            },
            {
                "en": "You have to check your flight gate number on the overhead screens.",
                "ar": "يتعين عليك التحقق من رقم بوابة رحلتك على الشاشات المعلقة.",
                "grammar_id": "g021"
            },
            {
                "en": "You must not leave your personal bags unattended anywhere in the terminal.",
                "ar": "يجب ألا تترك حقائبك الشخصية دون مراقبة في أي مكان في مبنى المطار.",
                "grammar_id": "g021"
            }
        ],
        "questions": [
            {
                "prompt": "When do passengers not have to print paper tickets?",
                "options": [
                    "If they use the digital airline app",
                    "If they lose their passport",
                    "If they arrive late",
                    "Never"
                ],
                "answer_index": 0,
                "explanation_ar": "لا يضطرون لطباعة التذاكر إذا استخدموا تطبيق الطيران الرقمي."
            },
            {
                "prompt": "What must travelers hold before international departure?",
                "options": [
                    "A valid passport and travel visa",
                    "A toy camera",
                    "A heavy television",
                    "Old coins"
                ],
                "answer_index": 0,
                "explanation_ar": "يجب حمل جواز سفر وتأشيرة ساريين (a valid passport and travel visa)."
            }
        ]
    },
    {
        "id": "r015",
        "level": "A2",
        "topic": "education and language",
        "title_en": "Learning From Mistakes in a Notebook",
        "title_ar": "التعلم من الأخطاء في دفتر الملاحظات",
        "grammar_ids": [
            "g022"
        ],
        "target_words": [
            "mistake",
            "notebook",
            "rule",
            "review",
            "correct",
            "progress"
        ],
        "sentences": [
            {
                "en": "Language learners should write down their grammatical mistakes in a dedicated notebook.",
                "ar": "ينبغي لمتعلمي اللغة تدوين أخطائهم النحوية في دفتر مخصص.",
                "grammar_id": "g022"
            },
            {
                "en": "You shouldn't feel embarrassed when you make an error in spoken English.",
                "ar": "لا ينبغي أن تشعر بالحرج عندما ترتكب خطأ في التحدث بالإنجليزية.",
                "grammar_id": "g022"
            },
            {
                "en": "Students should review their saved correction cards every few days for retention.",
                "ar": "يجب على الطلاب مراجعة بطاقات التصحيح المحفوظة كل بضعة أيام لتثبيت المعلومات.",
                "grammar_id": "g022"
            },
            {
                "en": "You shouldn't memorize isolated words without understanding their sentence context.",
                "ar": "لا ينبغي أن تحفظ كلمات معزولة دون فهم سياق جملتها.",
                "grammar_id": "g022"
            },
            {
                "en": "You should compare your original draft with the teacher's corrected version.",
                "ar": "عليك مقارنة مسودتك الأصلية بالنسخة المصححة للمعلم.",
                "grammar_id": "g022"
            },
            {
                "en": "Learners shouldn't give up when grammar rules seem challenging at first.",
                "ar": "لا ينبغي للمتعلمين الاستسلام عندما تبدو القواعد النحوية صعبة في البداية.",
                "grammar_id": "g022"
            }
        ],
        "questions": [
            {
                "prompt": "What should language learners do when they make grammatical mistakes?",
                "options": [
                    "Write them down in a dedicated notebook",
                    "Stop studying completely",
                    "Hide their books",
                    "Delete their mobile apps"
                ],
                "answer_index": 0,
                "explanation_ar": "ينبغي تدوين الأخطاء في دفتر مخصص (write down their mistakes in a dedicated notebook)."
            },
            {
                "prompt": "How should learners feel when making spoken errors?",
                "options": [
                    "They shouldn't feel embarrassed",
                    "They should be terrified",
                    "They should feel angry",
                    "They should feel hopeless"
                ],
                "answer_index": 0,
                "explanation_ar": "النص يوضح: You shouldn't feel embarrassed."
            }
        ]
    },
    {
        "id": "r016",
        "level": "A2",
        "topic": "history and science",
        "title_en": "How Ancient Scholars Preserved Knowledge",
        "title_ar": "كيف حفظ العلماء القدماء المعرفة",
        "grammar_ids": [
            "g023"
        ],
        "target_words": [
            "history",
            "knowledge",
            "science",
            "translate",
            "library",
            "discover"
        ],
        "sentences": [
            {
                "en": "Scholars in ancient Baghdad always gathered at the House of Wisdom to study.",
                "ar": "كان العلماء في بغداد القديمة يجتمعون دائمًا في بيت الحكمة للدراسة.",
                "grammar_id": "g023"
            },
            {
                "en": "They usually translated classical scientific manuscripts with extreme precision.",
                "ar": "كانوا يترجمون عادة المخطوطات العلمية الكلاسيكية بدقة متناهية.",
                "grammar_id": "g023"
            },
            {
                "en": "Astronomers often recorded planetary movements from their desert observatories.",
                "ar": "كان علماء الفلك يسجلون غالبًا حركات الكواكب من مراصدهم الصحراوية.",
                "grammar_id": "g023"
            },
            {
                "en": "Philosophers sometimes debated logical theories late into the starry night.",
                "ar": "كان الفلاسفة يناقشون أحيانًا النظريات المنطقية حتى وقت متأخر من الليل.",
                "grammar_id": "g023"
            },
            {
                "en": "Dedicated researchers rarely accepted scientific claims without practical experiments.",
                "ar": "نادرًا ما كان الباحثون المخلصون يقبلون الادعاءات العلمية دون تجارب عملية.",
                "grammar_id": "g023"
            },
            {
                "en": "Great scholars never ignored valuable wisdom from neighboring civilizations.",
                "ar": "لم يتجاهل كبار العلماء أبدًا الحكمة القيمة من الحضارات المجاورة.",
                "grammar_id": "g023"
            }
        ],
        "questions": [
            {
                "prompt": "How often did scholars gather at the House of Wisdom?",
                "options": [
                    "They always gathered to study",
                    "They never met",
                    "Only once in a century",
                    "Rarely"
                ],
                "answer_index": 0,
                "explanation_ar": "كانوا يجتمعون دائمًا في بيت الحكمة (always gathered)."
            },
            {
                "prompt": "Did great scholars ignore knowledge from neighboring civilizations?",
                "options": [
                    "They never ignored valuable wisdom",
                    "They always threw books away",
                    "They refused all translations",
                    "They forgot ancient books"
                ],
                "answer_index": 0,
                "explanation_ar": "لم يتجاهلوا أبدًا الحكمة القيمة (never ignored valuable wisdom)."
            }
        ]
    },
    {
        "id": "r017",
        "level": "A1",
        "topic": "sports and abilities",
        "title_en": "Sports and Special Talents",
        "title_ar": "الرياضات والمواهب الخاصة",
        "grammar_ids": [
            "g009"
        ],
        "target_words": [
            "swim",
            "piano",
            "language",
            "practice",
            "talent",
            "confidence"
        ],
        "sentences": [
            {
                "en": "Sami can swim very fast in the Olympic pool, but he cannot dive.",
                "ar": "يستطيع سامي السباحة بسرعة كبيرة في المسبح الأولمبي، لكنه لا يستطيع الغوص.",
                "grammar_id": "g009"
            },
            {
                "en": "His talented sister can play classical piano melodies beautifully.",
                "ar": "تستطيع أخته الموهوبة عزف ألحان البيانو الكلاسيكية بشكل جميل.",
                "grammar_id": "g009"
            },
            {
                "en": "I cannot play any musical instruments yet.",
                "ar": "أنا لا أستطيع عزف أي آلة موسيقية حتى الآن.",
                "grammar_id": "g009"
            },
            {
                "en": "However, I can speak three different languages with ease and confidence.",
                "ar": "ومع ذلك، أستطيع التحدث بثلاث لغات مختلفة بسهولة وثقة.",
                "grammar_id": "g009"
            },
            {
                "en": "We can practice our sports skills together every Saturday afternoon.",
                "ar": "يمكننا ممارسة مهاراتنا الرياضية معًا بعد ظهر كل سبت.",
                "grammar_id": "g009"
            },
            {
                "en": "With patient dedication, anyone can develop their own unique abilities.",
                "ar": "بالتفاني والصبر، يمكن لأي شخص تطوير قدراته الفريدة.",
                "grammar_id": "g009"
            }
        ],
        "questions": [
            {
                "prompt": "What can Sami do in the Olympic pool?",
                "options": [
                    "He can swim very fast",
                    "He can dive from high platforms",
                    "He can sleep in water",
                    "He cannot swim"
                ],
                "answer_index": 0,
                "explanation_ar": "يستطيع سامي السباحة بسرعة كبيرة (can swim very fast)."
            },
            {
                "prompt": "How many languages can the speaker speak?",
                "options": [
                    "Three different languages",
                    "Ten languages",
                    "Only one word",
                    "None"
                ],
                "answer_index": 0,
                "explanation_ar": "يستطيع التحدث بثلاث لغات مختلفة (can speak three different languages)."
            }
        ]
    },
    {
        "id": "r018",
        "level": "A1",
        "topic": "education and science",
        "title_en": "A Curious Student in Science Class",
        "title_ar": "طالب فضولي في درس العلوم",
        "grammar_ids": [
            "g010"
        ],
        "target_words": [
            "planet",
            "question",
            "teacher",
            "curious",
            "science",
            "discover"
        ],
        "sentences": [
            {
                "en": "What is the most fascinating planet in our solar system?",
                "ar": "ما هو الكوكب الأكثر إثارة للاهتمام في نظامنا الشمسي؟",
                "grammar_id": "g010"
            },
            {
                "en": "Where do migratory birds fly when the cold winter arrives?",
                "ar": "إلى أين تطير الطيور المهاجرة عندما يأتي فصل الشتاء البارد؟",
                "grammar_id": "g010"
            },
            {
                "en": "Adam asks his teacher why the evening sky turns bright orange at sunset.",
                "ar": "يسأل آدم معلمه لماذا تتحول سماء المساء إلى اللون البرتقالي الساطع عند الغروب.",
                "grammar_id": "g010"
            },
            {
                "en": "The teacher demonstrates how white clouds produce heavy rain.",
                "ar": "يشرح المعلم كيف تنتج السحب البيضاء أمطارًا غزيرة.",
                "grammar_id": "g010"
            },
            {
                "en": "Who invented the first astronomical telescope in human history?",
                "ar": "من اخترع أول تلسكوب فلكي في تاريخ البشرية؟",
                "grammar_id": "g010"
            },
            {
                "en": "Asking thoughtful questions helps students understand scientific wonders every day.",
                "ar": "طرح الأسئلة الذكية يساعد الطلاب على فهم العجائب العلمية كل يوم.",
                "grammar_id": "g010"
            }
        ],
        "questions": [
            {
                "prompt": "What does Adam ask his teacher about the evening sky?",
                "options": [
                    "Why the sky turns orange at sunset",
                    "Where the moon hides in the morning",
                    "Who painted the clouds",
                    "How fast fish swim"
                ],
                "answer_index": 0,
                "explanation_ar": "يسأل لماذا تتحول السماء إلى اللون البرتقالي عند الغروب (why the evening sky turns bright orange)."
            },
            {
                "prompt": "What does asking thoughtful questions help students do?",
                "options": [
                    "Understand scientific wonders every day",
                    "Forget their homework",
                    "Break lab equipment",
                    "Sleep in class"
                ],
                "answer_index": 0,
                "explanation_ar": "يساعدهم على فهم العجائب العلمية كل يوم."
            }
        ]
    },
    {
        "id": "r019",
        "level": "A1",
        "topic": "food and shopping",
        "title_en": "Shopping for Family Dinner",
        "title_ar": "التسوق لإعداد عشاء العائلة",
        "grammar_ids": [
            "g012"
        ],
        "target_words": [
            "vegetable",
            "milk",
            "cheese",
            "bread",
            "supermarket",
            "dinner"
        ],
        "sentences": [
            {
                "en": "We already have some fresh vegetables and tomatoes in the kitchen basket.",
                "ar": "لدينا بالفعل بعض الخضار الطازجة والطماطم في سلة المطبخ.",
                "grammar_id": "g012"
            },
            {
                "en": "However, there isn't any milk left in the refrigerator for breakfast.",
                "ar": "ومع ذلك، لا يوجد أي حليب متبقٍ في الثلاجة لتناول الإفطار.",
                "grammar_id": "g012"
            },
            {
                "en": "Do we need any white cheese for the pasta dish tonight?",
                "ar": "هل نحتاج إلى أي جبن أبيض لطبق المعكرونة الليلة؟",
                "grammar_id": "g012"
            },
            {
                "en": "My mother buys some red apples and two loaves of warm bread at the bakery.",
                "ar": "تشتري والدتي بعض التفاح الأحمر ورغيفين من الخبز الدافئ من المخبز.",
                "grammar_id": "g012"
            },
            {
                "en": "The grocery store does not have any olive oil on the shelves today.",
                "ar": "البقالة لا تحتوي على أي زيت زيتون على الرفوف اليوم.",
                "grammar_id": "g012"
            },
            {
                "en": "Preparing a delicious home-cooked meal brings the entire family together.",
                "ar": "إعداد وجبة شهية مطبوخة في المنزل يجمع العائلة بأكملها معًا."
            }
        ],
        "questions": [
            {
                "prompt": "Is there any milk left in the refrigerator?",
                "options": [
                    "No, there isn't any milk left",
                    "Yes, ten bottles",
                    "Only warm chocolate milk",
                    "The fridge is full of milk"
                ],
                "answer_index": 0,
                "explanation_ar": "النص يذكر: there isn't any milk left in the refrigerator."
            },
            {
                "prompt": "What does the mother buy at the bakery?",
                "options": [
                    "Some red apples and two loaves of warm bread",
                    "Ten boxes of pizza",
                    "Canned fish",
                    "Bottles of soda"
                ],
                "answer_index": 0,
                "explanation_ar": "تشتري بعض التفاح الأحمر ورغيفين من الخبز الدافئ."
            }
        ]
    },
    {
        "id": "r020",
        "level": "A1",
        "topic": "science and safety",
        "title_en": "Laboratory Safety Guidelines",
        "title_ar": "إرشادات السلامة في المختبر",
        "grammar_ids": [
            "g013"
        ],
        "target_words": [
            "safety",
            "goggles",
            "glass",
            "listen",
            "wash",
            "rule"
        ],
        "sentences": [
            {
                "en": "Always wear your protective goggles before touching any chemical substances.",
                "ar": "ارتدِ دائمًا نظاراتك الواقية قبل لمس أي مواد كيميائية.",
                "grammar_id": "g013"
            },
            {
                "en": "Do not touch the heated glass beakers with your bare hands.",
                "ar": "لا تلمس الكؤوس الزجاجية الساخنة بيديك العاريتين.",
                "grammar_id": "g013"
            },
            {
                "en": "Listen attentively to your science instructor's safety directions.",
                "ar": "استمع بانتباه لإرشادات السلامة الصادرة عن معلم العلوم.",
                "grammar_id": "g013"
            },
            {
                "en": "Wash your hands thoroughly with soap and warm water after each experiment.",
                "ar": "اغسل يديك جيدًا بالماء الدافئ والصابون بعد كل تجربة.",
                "grammar_id": "g013"
            },
            {
                "en": "Never run or consume food and sugary drinks inside the laboratory room.",
                "ar": "إياك أن تركض أو تتناول الطعام والمشروبات السكرية داخل غرفة المختبر.",
                "grammar_id": "g013"
            },
            {
                "en": "Follow these clear safety guidelines to prevent accidents and protect your classmates.",
                "ar": "اتبع هذه الإرشادات الواضحة لمنع الحوادث وحماية زملائك.",
                "grammar_id": "g013"
            }
        ],
        "questions": [
            {
                "prompt": "What should students wear before touching chemical substances?",
                "options": [
                    "Protective goggles",
                    "Heavy winter coats",
                    "Sunglasses",
                    "Woolen hats"
                ],
                "answer_index": 0,
                "explanation_ar": "ارتدِ دائمًا نظاراتك الواقية (Always wear your protective goggles)."
            },
            {
                "prompt": "What should students never do inside the laboratory room?",
                "options": [
                    "Never run or consume food and drinks",
                    "Never listen to the teacher",
                    "Never read books",
                    "Never write notes"
                ],
                "answer_index": 0,
                "explanation_ar": "إياك أن تركض أو تتناول الطعام داخل المختبر (Never run or consume food)."
            }
        ]
    },
    {
        "id": "r021",
        "level": "A2",
        "topic": "work and crafts",
        "title_en": "The Master Woodworker",
        "title_ar": "النجار الماهر",
        "grammar_ids": [
            "g024"
        ],
        "target_words": [
            "workshop",
            "patiently",
            "carefully",
            "wood",
            "smooth",
            "skill"
        ],
        "sentences": [
            {
                "en": "Uncle Hani carves traditional oak furniture patiently in his busy workshop.",
                "ar": "ينحت العم هاني الأثاث التقليدي المصنوع من خشب البلوط بصبر في ورشته المزدحمة.",
                "grammar_id": "g024"
            },
            {
                "en": "He measures each wooden board carefully before making any cuts.",
                "ar": "يقيس كل لوح خشبي بعناية قبل إجراء أي قطع.",
                "grammar_id": "g024"
            },
            {
                "en": "His young apprentice works quietly and observes his master's hand movements.",
                "ar": "يعمل متدربه الشاب بهدوء ويراقب حركات يد معلمه.",
                "grammar_id": "g024"
            },
            {
                "en": "When visitors arrive, Uncle Hani greets them warmly and politely.",
                "ar": "عندما يصل الزوار، يرحب بهم العم هاني بحرارة ولباقة.",
                "grammar_id": "g024"
            },
            {
                "en": "He smooths the polished table gently with fine sandpaper.",
                "ar": "يصقل الطاولة الملساء بلطف بورق زجاج ناعم.",
                "grammar_id": "g024"
            },
            {
                "en": "Working skillfully with natural materials creates timeless handmade art.",
                "ar": "العمل بمهارة مع المواد الطبيعية يخلق فنًا يدويًا خالدًا.",
                "grammar_id": "g024"
            }
        ],
        "questions": [
            {
                "prompt": "How does Uncle Hani measure each wooden board?",
                "options": [
                    "Carefully before making any cuts",
                    "Quickly without looking",
                    "With closed eyes",
                    "Carelessly"
                ],
                "answer_index": 0,
                "explanation_ar": "يقيس كل لوح خشبي بعناية (carefully before making any cuts)."
            },
            {
                "prompt": "How does he greet visitors when they arrive?",
                "options": [
                    "Warmly and politely",
                    "Angrily and loudly",
                    "Silently with a frown",
                    "Rudely"
                ],
                "answer_index": 0,
                "explanation_ar": "يرحب بهم بحرارة ولباقة (warmly and politely)."
            }
        ]
    },
    {
        "id": "r022",
        "level": "A2",
        "topic": "community",
        "title_en": "Organizing the Neighborhood Festival",
        "title_ar": "تنظيم مهرجان الحي",
        "grammar_ids": [
            "g025"
        ],
        "target_words": [
            "volunteer",
            "festival",
            "fruit",
            "water",
            "ticket",
            "community"
        ],
        "sentences": [
            {
                "en": "There are many enthusiastic volunteers helping in the community hall today.",
                "ar": "يوجد العديد من المتطوعين المتحمسين يساعدون في قاعة المجتمع اليوم.",
                "grammar_id": "g025"
            },
            {
                "en": "We do not have much time remaining before the cultural festival opens.",
                "ar": "ليس لدينا الكثير من الوقت المتبقي قبل افتتاح المهرجان الثقافي.",
                "grammar_id": "g025"
            },
            {
                "en": "A few generous neighbors brought a lot of fresh seasonal fruit and drinks.",
                "ar": "أحضر عدد قليل من الجيران الكرماء الكثير من الفواكه والمشروبات الموسمية الطازجة.",
                "grammar_id": "g025"
            },
            {
                "en": "We only need a little clean water to prepare the display flower vases.",
                "ar": "نحتاج فقط إلى القليل من الماء النظيف لتجهيز مزهريات الزهور المعروضة.",
                "grammar_id": "g025"
            },
            {
                "en": "How many entry tickets have the students sold so far this morning?",
                "ar": "كم عدد تذاكر الدخول التي باعها الطلاب حتى الآن هذا الصباح؟",
                "grammar_id": "g025"
            },
            {
                "en": "Sharing a lot of responsibilities among neighbors makes large events successful.",
                "ar": "مشاركة الكثير من المسؤوليات بين الجيران تجعل الفعاليات الكبيرة ناجحة.",
                "grammar_id": "g025"
            }
        ],
        "questions": [
            {
                "prompt": "How much time is remaining before the festival opens?",
                "options": [
                    "We do not have much time remaining",
                    "Three whole months",
                    "Ten years",
                    "An infinite amount of time"
                ],
                "answer_index": 0,
                "explanation_ar": "ليس لدينا الكثير من الوقت المتبقي (do not have much time remaining)."
            },
            {
                "prompt": "What did a few generous neighbors bring to the hall?",
                "options": [
                    "A lot of fresh fruit and drinks",
                    "Old broken chairs",
                    "Heavy bags of cement",
                    "Cold stones"
                ],
                "answer_index": 0,
                "explanation_ar": "أحضروا الكثير من الفواكه والمشروبات الطازجة (a lot of fresh seasonal fruit and drinks)."
            }
        ]
    },
    {
        "id": "r023",
        "level": "A2",
        "topic": "friendship and school",
        "title_en": "Returning the Lost Backpack",
        "title_ar": "إعادة الحقيبة المفقودة",
        "grammar_ids": [
            "g026"
        ],
        "target_words": [
            "backpack",
            "notebook",
            "return",
            "friend",
            "smile",
            "homework"
        ],
        "sentences": [
            {
                "en": "Tariq found a blue student backpack under the park bench and opened it.",
                "ar": "وجد طارق حقيبة ظهر زرقاء تحت مقعد الحديقة وفتحها.",
                "grammar_id": "g026"
            },
            {
                "en": "He recognized the handwriting on the math notebook and knew it was hers.",
                "ar": "تعرف على خط اليد في دفتر الرياضيات وعرف أنه ملكها.",
                "grammar_id": "g026"
            },
            {
                "en": "Tariq immediately messaged Nadia and offered to return the bag to her.",
                "ar": "راسل طارق نادية على الفور وعرض إعادة الحقيبة إليها.",
                "grammar_id": "g026"
            },
            {
                "en": "She met him at the school gate and thanked him with a bright smile.",
                "ar": "التقت به عند بوابة المدرسة وشكرته بابتسامة مشرقة.",
                "grammar_id": "g026"
            },
            {
                "en": "Nadia said, \"This blue backpack is mine, and you saved my project!\"",
                "ar": "قالت نادية: \"حقيبة الظهر الزرقاء هذه ملكي، وأنت أنقذت مشروعي!\"",
                "grammar_id": "g026"
            },
            {
                "en": "He answered, \"Friends should always help them and protect their belongings.\"",
                "ar": "أجاب: \"ينبغي للأصدقاء دائمًا مساعدتهم وحماية ممتلكاتهم.\"",
                "grammar_id": "g026"
            }
        ],
        "questions": [
            {
                "prompt": "Whose backpack was found under the park bench?",
                "options": [
                    "It was hers (Nadia's bag)",
                    "It belonged to the teacher",
                    "It was Tariq's brother's bag",
                    "It was nobody's"
                ],
                "answer_index": 0,
                "explanation_ar": "عرف أنه ملكها (knew it was hers) وتعود لنادية."
            },
            {
                "prompt": "What did Nadia say to Tariq when she received the bag?",
                "options": [
                    "This blue backpack is mine, and you saved my project!",
                    "Why did you open it?",
                    "Leave me alone",
                    "I don't know you"
                ],
                "answer_index": 0,
                "explanation_ar": "قالت نادية إن الحقيبة ملكها وشكرته (This blue backpack is mine)."
            }
        ]
    },
    {
        "id": "r024",
        "level": "A2",
        "topic": "career and design",
        "title_en": "Choosing a Career in Design",
        "title_ar": "اختيار مهنة في التصميم",
        "grammar_ids": [
            "g027"
        ],
        "target_words": [
            "design",
            "career",
            "drawing",
            "logo",
            "creative",
            "ambition"
        ],
        "sentences": [
            {
                "en": "Zaid enjoys drawing colorful graphic illustrations on his digital tablet.",
                "ar": "يستمتع زيد برسم الرسوم التوضيحية الملونة على جهازه اللوحي الرقمي.",
                "grammar_id": "g027"
            },
            {
                "en": "He wants to become an internationally recognized user interface designer.",
                "ar": "هو يريد أن يصبح مصمم واجهات مستخدم معترفاً به دولياً.",
                "grammar_id": "g027"
            },
            {
                "en": "His supportive parents decided to invest in his specialized art courses.",
                "ar": "قرر والداه الداعمان الاستثمار في دوراته الفنية المتخصصة.",
                "grammar_id": "g027"
            },
            {
                "en": "Zaid loves creating modern logos and visual identities for local startups.",
                "ar": "يحب زيد إنشاء شعارات وهويات بصرية حديثة للشركات الناشئة المحلية.",
                "grammar_id": "g027"
            },
            {
                "en": "He hopes to join a prominent creative design agency after graduation.",
                "ar": "يأمل في الانضمام إلى وكالة تصميم إبداعية بارزة بعد التخرج.",
                "grammar_id": "g027"
            },
            {
                "en": "Practicing creative crafts regularly develops profound professional confidence.",
                "ar": "ممارسة الحرف الإبداعية بانتظام يطور ثقة مهنية عميقة.",
                "grammar_id": "g027"
            }
        ],
        "questions": [
            {
                "prompt": "What does Zaid enjoy doing on his digital tablet?",
                "options": [
                    "He enjoys drawing colorful graphic illustrations",
                    "Playing video games",
                    "Watching cooking shows",
                    "Reading stock news"
                ],
                "answer_index": 0,
                "explanation_ar": "يستمتع برسم الرسوم التوضيحية الملونة (enjoys drawing colorful graphic illustrations)."
            },
            {
                "prompt": "What does Zaid want to become in the future?",
                "options": [
                    "A user interface designer",
                    "A submarine captain",
                    "A medical doctor",
                    "An airline pilot"
                ],
                "answer_index": 0,
                "explanation_ar": "يريد أن يصبح مصمم واجهات مستخدم (wants to become a user interface designer)."
            }
        ]
    },
    {
        "id": "r025",
        "level": "A2",
        "topic": "nature and travel",
        "title_en": "Planning the Mountain Hike",
        "title_ar": "التخطيط لرحلة المشي في الجبل",
        "grammar_ids": [
            "g028"
        ],
        "target_words": [
            "mountain",
            "weather",
            "hike",
            "temperature",
            "guide",
            "adventure"
        ],
        "sentences": [
            {
                "en": "If the weather is sunny on Saturday, we will hike to the highest mountain peak.",
                "ar": "إذا كان الطقس مشمسًا يوم السبت، فسنمشي إلى أعلى قمة جبلية.",
                "grammar_id": "g028"
            },
            {
                "en": "If it rains heavily tomorrow morning, we will explore the local geology museum instead.",
                "ar": "إذا هطل المطر بغزارة صباح الغد، فسنستكشف متحف الجيولوجيا المحلي بدلاً من ذلك.",
                "grammar_id": "g028"
            },
            {
                "en": "I will pack a thermal jacket if the alpine temperature drops below freezing.",
                "ar": "سأحزم سترة حرارية إذا انخفضت درجة حرارة الجبال إلى ما دون الصفر.",
                "grammar_id": "g028"
            },
            {
                "en": "If our trail guide arrives early, we will start our ascent before sunrise.",
                "ar": "إذا وصل مرشدنا في وقت مبكر، فسنبدأ صعودنا قبل شروق الشمس.",
                "grammar_id": "g028"
            },
            {
                "en": "We will take magnificent panoramic photos if the morning sky remains clear.",
                "ar": "سنلتقط صورًا بانورامية رائعة إذا بقيت سماء الصباح صافية.",
                "grammar_id": "g028"
            },
            {
                "en": "Careful preparation guarantees that every mountain adventure will remain safe and memorable.",
                "ar": "الإعداد الدقيق يضمن أن تظل كل مغامرة جبلية آمنة ولا تُنسى.",
                "grammar_id": "g028"
            }
        ],
        "questions": [
            {
                "prompt": "What will the hikers do if the weather is sunny on Saturday?",
                "options": [
                    "They will hike to the highest mountain peak",
                    "They will stay in bed all day",
                    "They will cancel the trip",
                    "They will buy new winter coats"
                ],
                "answer_index": 0,
                "explanation_ar": "سيمشون إلى أعلى قمة جبلية إذا كان الطقس مشمسًا (will hike to the highest mountain peak)."
            },
            {
                "prompt": "When will they pack a thermal jacket?",
                "options": [
                    "If the alpine temperature drops below freezing",
                    "If it is too hot",
                    "If they forget food",
                    "Never"
                ],
                "answer_index": 0,
                "explanation_ar": "سيحزم سترة إذا انخفضت درجة الحرارة لما دون الصفر (if the alpine temperature drops)."
            }
        ]
    },
    {
        "id": "r026",
        "level": "A2",
        "topic": "housing and urban life",
        "title_en": "Searching for the Right Apartment",
        "title_ar": "البحث عن الشقة المناسبة",
        "grammar_ids": [
            "g029"
        ],
        "target_words": [
            "apartment",
            "rent",
            "spacious",
            "neighborhood",
            "lease",
            "contract"
        ],
        "sentences": [
            {
                "en": "The first downtown apartment was too small for our growing family.",
                "ar": "كانت شقة وسط المدينة الأولى صغيرة جدًا بالنسبة لعائلتنا المتنامية.",
                "grammar_id": "g029"
            },
            {
                "en": "The second suburban flat was lovely, but the monthly rent was too expensive.",
                "ar": "كانت الشقة الثانية في الضواحي جميلة، لكن الإيجار الشهري كان باهظًا للغاية.",
                "grammar_id": "g029"
            },
            {
                "en": "Fortunately, the third residential apartment was spacious enough for all of us.",
                "ar": "لحسن الحظ، كانت الشقة السكنية الثالثة واسعة بما يكفي لجميعنا.",
                "grammar_id": "g029"
            },
            {
                "en": "The surrounding neighborhood is quiet enough for students to study peacefully.",
                "ar": "الحي المحيط هادئ بما يكفي ليدرس الطلاب بسلام.",
                "grammar_id": "g029"
            },
            {
                "en": "The building was close enough to public transit stations for easy commuting.",
                "ar": "كان المبنى قريبًا بما يكفي من محطات النقل العام لتسهيل التنقل.",
                "grammar_id": "g029"
            },
            {
                "en": "We were fortunate enough to sign the annual lease agreement yesterday afternoon.",
                "ar": "كنا محظوظين بما يكفي لتوقيع عقد الإيجار السنوي بعد ظهر أمس.",
                "grammar_id": "g029"
            }
        ],
        "questions": [
            {
                "prompt": "Why did the family reject the first downtown apartment?",
                "options": [
                    "It was too small for their family",
                    "It had no windows",
                    "It was too far from Earth",
                    "It had green doors"
                ],
                "answer_index": 0,
                "explanation_ar": "كانت صغيرة جدًا بالنسبة للعائلة (too small for our growing family)."
            },
            {
                "prompt": "What made the third apartment suitable?",
                "options": [
                    "It was spacious enough for all of them",
                    "It was completely free",
                    "It had no roof",
                    "It was in another country"
                ],
                "answer_index": 0,
                "explanation_ar": "كانت واسعة بما يكفي لجميعهم (spacious enough for all of us)."
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

package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.importer.DataImportRepository
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.ReadingQuestionItem
import com.example.data.local.ReadingTextEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDatabase
import com.example.data.plan.LocalQuizEngine
import com.example.data.plan.StudyPlanRepository
import com.example.data.plan.TestTimeProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyMiniQuizIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: WordBooksDatabase
    private lateinit var repo: DataImportRepository
    private lateinit var planRepo: StudyPlanRepository
    private lateinit var quizEngine: LocalQuizEngine
    private lateinit var timeProvider: TestTimeProvider
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repo = DataImportRepository(context, db)
        timeProvider = TestTimeProvider(
            todayDate = "2026-10-22",
            currentMonth = "2026-10",
            nowMillis = 1792670400000L
        )
        planRepo = StudyPlanRepository(context, db.wordBooksDao(), timeProvider)
        quizEngine = LocalQuizEngine(db.wordBooksDao(), timeProvider, planRepo)

        repo.importBuiltInDictionary()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testGenerateAlignedDailyQuiz_ProducesExact10QuestionsWith5Vocab3Grammar2Reading() = runBlocking {
        val checklist = planRepo.saveOnboardingGoalAndGeneratePlan(
            level = "B2",
            goal = "it_career",
            minutesPerDay = 30,
            studyDaysPerWeek = 6
        )

        val profile = db.wordBooksDao().getStudyPlanProfile()
        assertNotNull(profile)

        val result = quizEngine.generateAlignedDailyQuiz(checklist, profile, isTtsAvailable = true)
        val questions = result.questions

        assertEquals(10, questions.size)

        val vocabCount = questions.count { it.skill == "vocabulary" }
        val grammarCount = questions.count { it.skill == "grammar" }
        val readingCount = questions.count { it.skill == "reading" }

        assertEquals(5, vocabCount)
        assertEquals(3, grammarCount)
        assertEquals(2, readingCount)
    }

    @Test
    fun testGrammarQuestions_StrictlyDerivedFromAssignedLesson() = runBlocking {
        // Insert a custom target grammar lesson with 3 specific exercises
        val targetLesson = GrammarLessonEntity(
            id = "g_test_conditional",
            topic = "Conditionals",
            level = "B2",
            titleEn = "Third Conditional",
            titleAr = "الحالة الشرطية الثالثة",
            summaryEn = "If + past perfect, would have + past participle",
            summaryAr = "لو كنت فعلت لكان حدث",
            ruleEn = "Used for past unreal situations",
            ruleAr = "قاعدة التمني والندم في الماضي",
            exercisesJson = json.encodeToString(
                listOf(
                    GrammarExerciseItem(
                        id = "ex1",
                        type = "mcq",
                        prompt = "If I had known, I _____ called you.",
                        options = listOf("would have", "will have", "would", "had"),
                        answer = "would have",
                        explanationAr = "في الحالة الشرطية الثالثة نستخدم would have"
                    ),
                    GrammarExerciseItem(
                        id = "ex2",
                        type = "mcq",
                        prompt = "She wouldn't have missed the flight if she _____ earlier.",
                        options = listOf("had left", "left", "leaves", "would leave"),
                        answer = "had left",
                        explanationAr = "جملة if تأخذ past perfect"
                    ),
                    GrammarExerciseItem(
                        id = "ex3",
                        type = "reorder",
                        prompt = "Arrange: would / have / succeeded / they",
                        options = listOf("they", "would", "have", "succeeded"),
                        answer = "they would have succeeded",
                        explanationAr = "الترتيب الصحيح"
                    )
                )
            ),
            isCompleted = false
        )
        db.wordBooksDao().insertGrammarLesson(targetLesson)

        val tasks = listOf(
            DailyTaskItem(
                id = "task_grammar_g_test_conditional",
                type = "grammar",
                titleEn = "Grammar: Third Conditional",
                titleAr = "قواعد: الحالة الشرطية الثالثة",
                subtitleEn = "Third Conditional",
                targetCount = 1,
                completedCount = 0,
                isCompleted = false,
                linkedRoute = "grammar",
                linkedId = "g_test_conditional"
            )
        )
        val checklist = DailyChecklistEntity(
            date = "2026-10-22",
            tasksJson = json.encodeToString(tasks),
            completedCount = 0,
            totalCount = 1,
            isCompleted = false
        )
        val profile = StudyPlanProfileEntity(id = 1, level = "B2", goal = "it_career")

        val result = quizEngine.generateAlignedDailyQuiz(checklist, profile)
        val grammarQuestions = result.questions.filter { it.skill == "grammar" }

        assertEquals(3, grammarQuestions.size)
        grammarQuestions.forEach { q ->
            assertTrue(q.promptAr.contains("الحالة الشرطية الثالثة"))
            assertTrue(q.explanationAr.contains("الحالة الشرطية الثالثة") || q.explanationAr.contains("would have") || q.explanationAr.contains("past perfect"))
        }
    }

    @Test
    fun testReadingQuestions_StrictlyDerivedFromAssignedStory() = runBlocking {
        val targetStory = ReadingTextEntity(
            id = "r_test_interview",
            level = "B2",
            titleEn = "Interview with a Tech Founder",
            titleAr = "مقابلة مع مؤسس تقني",
            topic = "Technology",
            contentEn = "The founder discussed AI disruption...",
            contentAr = "ناقش المؤسس أثر الذكاء الاصطناعي...",
            questionsJson = json.encodeToString(
                listOf(
                    ReadingQuestionItem(
                        id = "rq1",
                        prompt = "What was the main topic discussed by the founder?",
                        options = listOf("AI disruption", "Cooking", "Space travel", "Weather"),
                        answerIndex = 0,
                        explanationAr = "الموضوع الرئيسي هو الذكاء الاصطناعي"
                    ),
                    ReadingQuestionItem(
                        id = "rq2",
                        prompt = "Why did the founder emphasize continuous learning?",
                        options = listOf("Rapid tech shifts", "Boredom", "Lower costs", "Routine"),
                        answerIndex = 0,
                        explanationAr = "بسبب التغيرات التكنولوجية السريعة"
                    )
                )
            ),
            isCompleted = false
        )
        db.wordBooksDao().insertReadingText(targetStory)

        val tasks = listOf(
            DailyTaskItem(
                id = "task_reading_r_test_interview",
                type = "reading",
                titleEn = "Reading: Interview with a Tech Founder",
                titleAr = "قراءة: مقابلة مع مؤسس تقني",
                subtitleEn = "Tech Founder",
                targetCount = 1,
                completedCount = 0,
                isCompleted = false,
                linkedRoute = "reading",
                linkedId = "r_test_interview"
            )
        )
        val checklist = DailyChecklistEntity(
            date = "2026-10-22",
            tasksJson = json.encodeToString(tasks),
            completedCount = 0,
            totalCount = 1,
            isCompleted = false
        )
        val profile = StudyPlanProfileEntity(id = 1, level = "B2", goal = "it_career")

        val result = quizEngine.generateAlignedDailyQuiz(checklist, profile)
        val readingQuestions = result.questions.filter { it.skill == "reading" }

        assertEquals(2, readingQuestions.size)
        readingQuestions.forEach { q ->
            assertTrue(q.promptEn.contains("Interview with a Tech Founder"))
            assertTrue(q.promptAr.contains("مقابلة مع مؤسس تقني"))
        }
    }

    @Test
    fun testScopeInfoPreview_CorrectlyFlagsCompletionState() = runBlocking {
        val tasks = listOf(
            DailyTaskItem(
                id = "task_new_words",
                type = "new_words",
                titleEn = "Learn 10 New Words (B2)",
                titleAr = "تعلم ١٠ مفردات",
                subtitleEn = "Focus words",
                targetCount = 10,
                completedCount = 10,
                isCompleted = true,
                linkedRoute = "word_books",
                linkedId = "oxford5000|B2"
            ),
            DailyTaskItem(
                id = "task_grammar_g001",
                type = "grammar",
                titleEn = "Present Perfect",
                titleAr = "المضارع التام",
                subtitleEn = "Grammar",
                targetCount = 1,
                completedCount = 0,
                isCompleted = false,
                linkedRoute = "grammar",
                linkedId = "g001"
            )
        )
        val checklist = DailyChecklistEntity(
            date = "2026-10-22",
            tasksJson = json.encodeToString(tasks),
            completedCount = 1,
            totalCount = 2,
            isCompleted = false
        )
        val profile = StudyPlanProfileEntity(id = 1, level = "B2", goal = "it_career")

        val result = quizEngine.generateAlignedDailyQuiz(checklist, profile)
        val scope = result.scopeInfo

        assertTrue(scope.vocabCompleted)
        assertFalse(scope.grammarCompleted)
        assertFalse(scope.allCompleted)
    }

    @Test
    fun testRecordQuizSubmission_SavesResultsAndAttempts() = runBlocking {
        val checklist = planRepo.saveOnboardingGoalAndGeneratePlan(
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6
        )
        val profile = db.wordBooksDao().getStudyPlanProfile()
        val result = quizEngine.generateAlignedDailyQuiz(checklist, profile)
        val questions = result.questions

        val answers = mutableMapOf<String, Int>()
        questions.forEachIndexed { index, q ->
            // Answer all correct except 1
            answers[q.id] = if (index == 0) (q.answerIndex + 1) % q.options.size else q.answerIndex
        }

        val accuracyMap = quizEngine.recordQuizSubmission("daily_mini", questions, answers, "2026-10-22")
        assertNotNull(accuracyMap["vocabulary"])
        assertNotNull(accuracyMap["grammar"])
        assertNotNull(accuracyMap["reading"])

        val savedResults = db.wordBooksDao().getQuizResultsForDate("2026-10-22")
        assertTrue(savedResults.isNotEmpty())

        val savedAttempts = db.wordBooksDao().getAllAttempts().filter { it.activityType == "quiz_question" }
        assertEquals(10, savedAttempts.size)
    }
}

import { useState, type ReactNode } from "react";

type IconName =
  | "home"
  | "books"
  | "cards"
  | "sentence"
  | "grammar"
  | "more"
  | "settings"
  | "calendar"
  | "fire"
  | "check"
  | "arrow";

const paths: Record<IconName, ReactNode> = {
  home: <><path d="m3 11 9-8 9 8" /><path d="M5 10v10h14V10M9 20v-6h6v6" /></>,
  books: <><path d="M4 4h6v16H4zM14 4h6v16h-6z" /><path d="M7 8h0M17 8h0" /></>,
  cards: <><rect x="4" y="5" width="14" height="16" rx="2" /><path d="m8 5 .8-2h11.4v15H18M8 11h6M8 15h4" /></>,
  sentence: <><path d="M4 5h16M4 10h12M4 15h16M4 20h9" /></>,
  grammar: <><path d="M5 19 10.5 5h3L19 19M7 14h10" /></>,
  more: <><circle cx="5" cy="12" r="1" fill="currentColor" /><circle cx="12" cy="12" r="1" fill="currentColor" /><circle cx="19" cy="12" r="1" fill="currentColor" /></>,
  settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1a1.7 1.7 0 0 0 1.9.3A1.7 1.7 0 0 0 10 3v-.2h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1Z" /></>,
  calendar: <><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M8 3v4M16 3v4M3 10h18M8 14h0M12 14h0M16 14h0M8 18h0M12 18h0" /></>,
  fire: <path d="M13 3s1 4-2 6c-2-3-5-1-5 3a6 6 0 0 0 12 0c0-3-2-6-5-9Zm-1 16c-2 0-3-1.5-2.5-3.1.4-1.2 1.5-1.8 2.2-2.9.2 1.2 1.3 1.8 1.8 2.8C14.4 17.5 13.3 19 12 19Z" />,
  check: <path d="m5 12 4 4L19 6" />,
  arrow: <><path d="M5 12h14M14 7l5 5-5 5" /></>,
};

function Icon({ name, size = 22 }: { name: IconName; size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name]}
    </svg>
  );
}

const navItems: { id: string; label: string; icon: IconName }[] = [
  { id: "Home", label: "Home", icon: "home" },
  { id: "Word Books", label: "Books", icon: "books" },
  { id: "Flashcards", label: "Cards", icon: "cards" },
  { id: "Sentences", label: "Sentences", icon: "sentence" },
  { id: "More", label: "More", icon: "more" },
];

const decks = [
  { title: "Oxford 5000", arabic: "أهم ٥٠٠٠ كلمة في الإنجليزية", entries: "5,024", progress: 68, color: "emerald" },
  { title: "Everyday Expressions", arabic: "تعبيرات الحياة اليومية", entries: "428", progress: 31, color: "sky" },
  { title: "Essential Idioms", arabic: "التعابير الاصطلاحية الأساسية", entries: "312", progress: 14, color: "violet" },
];

const pageData: Record<string, { eyebrow: string; title: string; subtitle: string; stats: [string, string][]; items: { title: string; detail: string; meta: string; badge: string }[] }> = {
  "Word Books": {
    eyebrow: "8,426 SAVED WORDS",
    title: "Your word books",
    subtitle: "Browse vocabulary by collection and learning level.",
    stats: [["8,426", "Total words"], ["1,247", "Mastered"], ["184", "Favorites"]],
    items: [
      { title: "Oxford 5000", detail: "Core vocabulary • A1–C1", meta: "5,024 words • 68% complete", badge: "Continue" },
      { title: "Everyday Expressions", detail: "Useful phrases for daily conversations", meta: "428 expressions • 31% complete", badge: "Study" },
      { title: "Essential Idioms", detail: "Common idioms with Arabic explanations", meta: "312 idioms • 14% complete", badge: "Study" },
      { title: "Academic Word List", detail: "Vocabulary for university and research", meta: "570 words • Not started", badge: "Start" },
    ],
  },
  Flashcards: {
    eyebrow: "SPACED REPETITION",
    title: "Flashcards",
    subtitle: "Review cards at the right time to remember more.",
    stats: [["24", "Due today"], ["18", "New cards"], ["92%", "Retention"]],
    items: [
      { title: "Due today", detail: "A mixed review from all your books", meta: "24 cards • About 8 minutes", badge: "Review" },
      { title: "Oxford 5000", detail: "Continue from “accomplish”", meta: "Card 342 of 5,024", badge: "Continue" },
      { title: "Difficult words", detail: "Words answered incorrectly twice or more", meta: "16 cards", badge: "Practice" },
    ],
  },
  Sentences: {
    eyebrow: "SAVED EXAMPLES",
    title: "My sentences",
    subtitle: "Learn vocabulary naturally through useful examples.",
    stats: [["126", "Saved"], ["38", "Practiced"], ["12", "This week"]],
    items: [
      { title: "She accomplished everything she set out to do.", detail: "لقد أنجزت كل ما خططت للقيام به.", meta: "accomplish • Oxford 5000", badge: "Practice" },
      { title: "The weather can be unpredictable in spring.", detail: "قد يكون الطقس غير متوقع في فصل الربيع.", meta: "unpredictable • B2", badge: "Practice" },
      { title: "I'd rather stay home this evening.", detail: "أفضل البقاء في المنزل هذا المساء.", meta: "would rather • Expression", badge: "Practice" },
    ],
  },
  Grammar: {
    eyebrow: "B2 LEARNING PATH",
    title: "Grammar lessons",
    subtitle: "Short explanations, examples, and targeted exercises.",
    stats: [["18", "Completed"], ["72%", "Accuracy"], ["6", "In progress"]],
    items: [
      { title: "Present perfect vs. past simple", detail: "Choose the correct tense in context", meta: "12 min • Intermediate", badge: "Continue" },
      { title: "Conditionals: zero to third", detail: "Build accurate real and unreal conditions", meta: "18 min • Upper intermediate", badge: "Start" },
      { title: "Reported speech", detail: "Report statements, questions, and commands", meta: "15 min • Intermediate", badge: "Start" },
      { title: "Articles: a, an, and the", detail: "Review common article patterns", meta: "10 min • Pre-intermediate", badge: "Review" },
    ],
  },
  Reading: {
    eyebrow: "GRADED READING",
    title: "Reading library",
    subtitle: "Build comprehension with stories matched to your level.",
    stats: [["14", "Completed"], ["6.2h", "Reading time"], ["B2", "Current level"]],
    items: [
      { title: "The Future of Remote Work", detail: "How technology is reshaping where we work", meta: "B2 • 6 min • 820 words", badge: "Read" },
      { title: "A City Beneath the Sand", detail: "An archaeological discovery in the desert", meta: "B1 • 5 min • 640 words", badge: "Read" },
      { title: "Why We Form Habits", detail: "The science behind everyday routines", meta: "B2 • 8 min • 1,040 words", badge: "Continue" },
    ],
  },
  "Study plan": {
    eyebrow: "WEEKLY PROGRESS",
    title: "Study plan",
    subtitle: "A balanced schedule adapted to your learning activity.",
    stats: [["4/7", "Days active"], ["82", "Minutes"], ["68%", "Weekly goal"]],
    items: [
      { title: "Wednesday • Today", detail: "20 reviews, 10 new words, reading, grammar", meta: "2 of 4 tasks complete • 18 min left", badge: "Continue" },
      { title: "Thursday", detail: "15 reviews, sentence practice, short reading", meta: "Estimated 24 minutes", badge: "Preview" },
      { title: "Friday", detail: "Weekly revision and progress quiz", meta: "Estimated 30 minutes", badge: "Preview" },
    ],
  },
};

function SectionPage({ section, onStartStudy }: { section: string; onStartStudy: () => void }) {
  const page = pageData[section] ?? pageData["Word Books"];
  const [completed, setCompleted] = useState<string[]>([]);

  return (
    <main className="content section-page">
      <section className="page-intro">
        <p className="eyebrow">{page.eyebrow}</p>
        <h1>{page.title}</h1>
        <p>{page.subtitle}</p>
      </section>
      <section className="stats-grid">
        {page.stats.map(([value, label]) => <article key={label}><strong>{value}</strong><span>{label}</span></article>)}
      </section>
      <section className="section-heading list-heading">
        <div><p className="eyebrow">SAMPLE DATA</p><h2>{section === "Study plan" ? "Upcoming schedule" : "Recommended for you"}</h2></div>
        <button className="text-button">Filter <Icon name="more" size={17} /></button>
      </section>
      <div className="content-list">
        {page.items.map((item, index) => {
          const id = `${section}-${index}`;
          const isComplete = completed.includes(id);
          return (
            <article className="content-row" key={id}>
              <span className="row-number">{String(index + 1).padStart(2, "0")}</span>
              <div><h3>{item.title}</h3><p>{item.detail}</p><small>{item.meta}</small></div>
              <button className={isComplete ? "row-action complete" : "row-action"} onClick={() => {
                if (section === "Flashcards") onStartStudy();
                else setCompleted((current) => current.includes(id) ? current.filter((value) => value !== id) : [...current, id]);
              }}>
                {isComplete ? <><Icon name="check" size={15} /> Done</> : item.badge}
              </button>
            </article>
          );
        })}
      </div>
      <p className="test-note">Sample data is stored locally and resets when the preview reloads.</p>
    </main>
  );
}

function Home({ onStartStudy, onNavigate }: { onStartStudy: () => void; onNavigate: (section: string) => void }) {
  const [tasks, setTasks] = useState([true, true, false, false]);
  const completed = tasks.filter(Boolean).length;
  const toggleTask = (index: number) => setTasks((current) => current.map((value, i) => i === index ? !value : value));

  return (
    <main className="content">
      <section className="welcome">
        <div>
          <p className="eyebrow">Wednesday, May 22</p>
          <h1>Good evening, Ibrahim</h1>
          <p className="arabic" dir="rtl">جاهز لجلسة تعلم جديدة؟</p>
        </div>
        <span className="streak"><Icon name="fire" size={17} /> 12 day streak</span>
      </section>

      <section className="plan-card">
        <div className="plan-head">
          <div className="title-lockup">
            <span className="icon-tile"><Icon name="calendar" /></span>
            <div><h2>Today's Study Plan</h2><p className="arabic" dir="rtl">خطتك اليومية الذكية</p></div>
          </div>
          <strong>{completed}/4</strong>
        </div>
        <div className="progress" aria-label={`${completed} of 4 tasks completed`}><span style={{ width: `${completed * 25}%` }} /></div>
        <div className="tasks">
          {["Review 20 due cards", "Learn 10 new words", "Read a short story", "Practice grammar"].map((task, index) => (
            <button className={`task ${tasks[index] ? "done" : ""}`} onClick={() => toggleTask(index)} key={task}>
              <span className="task-check">{tasks[index] && <Icon name="check" size={15} />}</span>
              <span>{task}</span>
              <small>{[8, 10, 12, 10][index]} min</small>
            </button>
          ))}
        </div>
      </section>

      <section className="section-heading">
        <div><p className="eyebrow">SPACED REPETITION</p><h2>Ready for review</h2></div>
        <button className="text-button" onClick={() => onNavigate("Flashcards")}>View all <Icon name="arrow" size={16} /></button>
      </section>

      <section className="review-card">
        <div className="review-count"><strong>24</strong><span>cards due today</span></div>
        <div className="review-copy"><h3>Keep your memory strong</h3><p>Your shortest review is ready. About 8 minutes.</p></div>
        <button className="primary" onClick={onStartStudy}>Start review <Icon name="arrow" size={18} /></button>
      </section>

      <section className="section-heading decks-heading">
        <div><p className="eyebrow">YOUR LIBRARY</p><h2>Continue learning</h2></div>
        <button className="text-button" onClick={() => onNavigate("Word Books")}>All books <Icon name="arrow" size={16} /></button>
      </section>
      <div className="deck-grid">
        {decks.map((deck) => (
          <button className="deck-card" key={deck.title} onClick={() => onNavigate("Word Books")}>
            <span className={`book-cover ${deck.color}`}><Icon name="books" size={28} /></span>
            <span className="deck-info">
              <strong>{deck.title}</strong><span className="arabic" dir="rtl">{deck.arabic}</span>
              <span className="deck-meta">{deck.entries} words <i>•</i> {deck.progress}% complete</span>
              <span className="mini-progress"><i style={{ width: `${deck.progress}%` }} /></span>
            </span>
            <Icon name="arrow" size={18} />
          </button>
        ))}
      </div>
    </main>
  );
}

const reviewCards = [
  { word: "accomplish", type: "verb • B2", meaning: "يُنجز، يُتم", example: "She accomplished everything she set out to do." },
  { word: "reliable", type: "adjective • B1", meaning: "موثوق، جدير بالاعتماد", example: "We need a reliable source of information." },
  { word: "perspective", type: "noun • B2", meaning: "وجهة نظر، منظور", example: "Try to see the issue from a different perspective." },
];

function StudySession({ onClose }: { onClose: () => void }) {
  const [index, setIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const card = reviewCards[index % reviewCards.length];
  const answer = (rating: string) => {
    setRevealed(false);
    setIndex((current) => current + 1);
    if (rating === "Finish") onClose();
  };

  return (
    <main className="study-session">
      <div className="study-top">
        <button className="session-close" onClick={onClose} aria-label="Close study session">×</button>
        <div><strong>Daily review</strong><small>{Math.min(index + 1, 24)} of 24</small></div>
        <span>8 min</span>
      </div>
      <div className="session-progress"><i style={{ width: `${Math.min((index + 1) / 24 * 100, 100)}%` }} /></div>
      <div className="card-stage">
        <button className={`flashcard ${revealed ? "revealed" : ""}`} onClick={() => setRevealed(true)}>
          <span className="card-label">{revealed ? "ANSWER" : "WORD"}</span>
          <strong>{card.word}</strong>
          <small>{card.type}</small>
          {revealed ? <><div className="card-rule" /><p className="arabic" dir="rtl">{card.meaning}</p><q>{card.example}</q></> : <span className="tap-hint">Tap to reveal meaning</span>}
        </button>
      </div>
      <div className="study-controls">
        {revealed ? (
          <div className="ratings">
            {[["Again", "< 1m"], ["Hard", "2d"], ["Good", "4d"], ["Easy", "8d"]].map(([label, time]) => (
              <button key={label} className={label.toLowerCase()} onClick={() => answer(index >= 23 ? "Finish" : label)}><small>{time}</small><strong>{label}</strong></button>
            ))}
          </div>
        ) : <button className="reveal-button" onClick={() => setRevealed(true)}>Show answer</button>}
      </div>
    </main>
  );
}

function SettingsSheet({ onClose }: { onClose: () => void }) {
  const [sound, setSound] = useState(true);
  const [arabic, setArabic] = useState(true);
  return (
    <div className="scrim" onClick={onClose}>
      <aside className="more-sheet settings-sheet" onClick={(event) => event.stopPropagation()}>
        <span className="handle" /><p className="eyebrow">PREFERENCES</p><h2>Learning settings</h2>
        {[["Auto-play pronunciation", "Hear each word when a card opens", sound, setSound], ["Show Arabic translation", "Display Arabic support across lessons", arabic, setArabic]].map(([label, detail, value, setter]) => (
          <button className="setting-row" key={label as string} onClick={() => (setter as (value: boolean) => void)(!(value as boolean))}>
            <span><strong>{label as string}</strong><small>{detail as string}</small></span><i className={value ? "switch on" : "switch"}><b /></i>
          </button>
        ))}
        <button className="settings-done" onClick={onClose}>Save preferences</button>
      </aside>
    </div>
  );
}

function App() {
  const [active, setActive] = useState("Home");
  const [moreOpen, setMoreOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [studying, setStudying] = useState(false);

  const navigate = (id: string) => {
    if (id === "More") setMoreOpen(true);
    else { setActive(id); setMoreOpen(false); }
  };

  if (studying) return <StudySession onClose={() => { setStudying(false); setActive("Flashcards"); }} />;

  return (
    <div className="app-shell">
      <header>
        <div className="brand"><span>WB</span><div><strong>WordBooks</strong><small>{active}</small></div></div>
        <div className="header-actions"><span className="daily-goal"><i /> 2 / 4 today</span><button className="icon-button" aria-label="Settings" onClick={() => setSettingsOpen(true)}><Icon name="settings" /></button></div>
      </header>

      {active === "Home" ? <Home onStartStudy={() => setStudying(true)} onNavigate={setActive} /> : <SectionPage section={active} onStartStudy={() => setStudying(true)} />}

      {moreOpen && (
        <div className="scrim" onClick={() => setMoreOpen(false)}>
          <aside className="more-sheet" onClick={(event) => event.stopPropagation()}>
            <span className="handle" /><h2>More learning tools</h2>
            {[["Grammar", "grammar"], ["Reading", "sentence"], ["Study plan", "calendar"]].map(([label, icon]) => (
              <button key={label} onClick={() => { setActive(label); setMoreOpen(false); }}><span className="icon-tile"><Icon name={icon as IconName} /></span><strong>{label}</strong><Icon name="arrow" size={18} /></button>
            ))}
          </aside>
        </div>
      )}

      {settingsOpen && <SettingsSheet onClose={() => setSettingsOpen(false)} />}

      <nav aria-label="Main navigation">
        {navItems.map((item) => (
          <button className={active === item.id || (item.id === "More" && moreOpen) ? "active" : ""} onClick={() => navigate(item.id)} key={item.id}>
            <span><Icon name={item.icon} /></span><small>{item.label}</small>
          </button>
        ))}
      </nav>
    </div>
  );
}

export default App;

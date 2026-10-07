package com.eve.app.uistudio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.eve.app.R
import com.eve.app.data.model.*
import com.eve.app.ui.admin.*
import com.eve.app.ui.home.ExamTestsAdapter
import com.eve.app.ui.home.ExamTestsListItem
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.TestAttempt
import com.eve.app.data.model.LeaderboardEntry
import com.eve.app.ui.result.AnswerAdapter
import com.eve.app.ui.history.HistoryAdapter
import com.eve.app.ui.leaderboard.LeaderboardAdapter
import com.eve.app.ui.admin.QuestionManageAdapter
import com.eve.app.ui.home.ExamAdapter
import com.eve.app.ui.home.HomeListItem
import com.eve.app.ui.notifications.NotificationAdapter
import com.eve.app.ui.syllabus.SyllabusAdapter
import com.eve.app.ui.test.QuestionAdapter
import com.eve.app.util.StoredNotification

/** Coordinates come from transformed view bounds; selection never mutates view foregrounds. */
class StudioPreview(context: Context) : FrameLayout(context) {
    var editing = true
    var selected: View? = null
    var onSelect: (View?) -> Unit = {}
    var candidates: () -> List<StudioRenderer.Element> = { emptyList() }
    private var x = 0f
    private var y = 0f
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.rgb(0,122,255); strokeWidth=3 * resources.displayMetrics.density; style=Paint.Style.STROKE }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (!editing) return super.dispatchTouchEvent(event)
        if (event.actionMasked==MotionEvent.ACTION_DOWN) { x=event.rawX; y=event.rawY;return true }
        if (event.actionMasked==MotionEvent.ACTION_UP && kotlin.math.hypot(event.rawX-x,event.rawY-y) < ViewConfiguration.get(context).scaledTouchSlop) {
            val rect=Rect()
            val hit=candidates().filter{it.view.getGlobalVisibleRect(rect) && rect.contains(event.rawX.toInt(),event.rawY.toInt())}.maxWithOrNull(Comparator{a,b->drawOrder(a.view,b.view)})
            onSelect(hit?.view); invalidate(); return true
        }
        // Ancestor scroll views can intercept movement; edit taps never trigger native actions.
        return true
    }
    private fun drawOrder(a:View,b:View):Int {
        val left=generateSequence(a){it.parent as? View}.toList().asReversed()
        val right=generateSequence(b){it.parent as? View}.toList().asReversed()
        for(i in 0 until minOf(left.size,right.size))if(left[i]!==right[i]){
            val z=left[i].z.compareTo(right[i].z)
            if(z!=0)return z
            val parent=left[i].parent as? android.view.ViewGroup
            return (parent?.indexOfChild(left[i]) ?: 0).compareTo(parent?.indexOfChild(right[i]) ?: 0)
        }
        return left.size.compareTo(right.size)
    }
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val rect=Rect(); val origin=IntArray(2); getLocationOnScreen(origin)
        if (editing && selected?.getGlobalVisibleRect(rect)==true) {
            rect.offset(-origin[0],-origin[1]); canvas.drawRect(rect,pen)
        }
    }
}

/** Production adapters, deterministic in-memory data, no student Activity or API mutations. */
object StudioFixtures {
    fun bind(root: View, screen: String, config: () -> com.eve.app.data.model.uistudio.UiStudioConfig, navigate: (String)->Unit) {
        val exam=Exam(id="studio-example",examName="General Studies",questionCount=20,category="Practice",syllabusUrl="https://example.com/syllabus.pdf")
        fun rv(id: Int, adapter: RecyclerView.Adapter<*>) { root.findViewById<RecyclerView>(id)?.apply { layoutManager=LinearLayoutManager(context); this.adapter=adapter; visibility=View.VISIBLE } }
        listOf("progressGroup","messageGroup","shimmerSkeletonHome","shimmerSkeletonTest").forEach {
            val id=root.resources.getIdentifier(it,"id",root.context.packageName)
            root.findViewById<View>(id)?.visibility=View.GONE
        }
        val question=Question(id="preview",questionText="Which number is a prime number?",optionA="21",optionB="23",optionC="25",optionD="27")
        val answer=AnswerItem(number=1,questionText=question.questionText,selected="B",selectedText="23",correct="B",correctText="23",explanation="23 has exactly two positive divisors.",optionA="21",optionB="23",optionC="25",optionD="27")
        fun text(name: String,value: String) { root.findViewById<TextView>(root.resources.getIdentifier(name,"id",root.context.packageName))?.text=value }
        fun tabs(id: Int,labels: List<String>,sections: List<Int>) {
            root.findViewById<com.google.android.material.tabs.TabLayout>(id)?.apply {
                if(tabCount==0)labels.forEach { addTab(newTab().setText(it)) }
                addOnTabSelectedListener(object: com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) { sections.forEachIndexed { i,section -> root.findViewById<View>(section)?.visibility=if(i==tab.position)View.VISIBLE else View.GONE } }
                    override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
                    override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
                })
            }
        }
        if(screen=="result") {
            root.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutResult)?.apply {
                if(tabCount==0)listOf("Overview","Review","Leaderboard").forEach { addTab(newTab().setText(it)) }
                addOnTabSelectedListener(object: com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                        root.findViewById<View>(R.id.scrollResultContent)?.visibility=if(tab.position==0)View.VISIBLE else View.GONE
                        root.findViewById<View>(R.id.sectionReview)?.visibility=if(tab.position==1)View.VISIBLE else View.GONE
                        root.findViewById<View>(R.id.scrollLeaderboard)?.visibility=if(tab.position==2)View.VISIBLE else View.GONE
                    }
                    override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
                    override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
                })
            }
        }
        if(screen=="admin") tabs(R.id.tabLayoutAdmin,listOf("Modules","Questions","Admins"),listOf(R.id.scrollTabCreateExam,R.id.scrollTabManageQuestions,R.id.scrollTabAdmins))
        if(screen in listOf("test","result")) {
            val adapter=com.eve.app.ui.common.QuestionPaletteAdapter {}
            adapter.submit(listOf(com.eve.app.ui.common.PaletteItem(1,com.eve.app.ui.common.PaletteState.ANSWERED,true),com.eve.app.ui.common.PaletteItem(2),com.eve.app.ui.common.PaletteItem(3,com.eve.app.ui.common.PaletteState.MARKED)))
            root.findViewById<RecyclerView>(if(screen=="test")R.id.rvQuestionPalette else R.id.rvResultPalette)?.apply { layoutManager=LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false);this.adapter=adapter }
        }
        when(screen) {
            "home" -> {
                rv(R.id.rvExams,ExamAdapter({ _,_,_ -> navigate("test") }).apply { submit(listOf(HomeListItem.Header("Your next practice"),HomeListItem.ExamRow(exam),HomeListItem.ExamRow(exam.copy(id="studio-2",examName="Quantitative Aptitude")))) })
                root.findViewById<View>(R.id.layoutStreakPill)?.visibility=View.VISIBLE
                root.findViewById<TextView>(R.id.tvStreakSummary)?.apply { text="5 day streak";visibility=View.VISIBLE }
                root.findViewById<View>(R.id.panelHomeBanner)?.visibility=View.VISIBLE
                root.findViewById<ViewPager2>(R.id.vpHomeBanners)?.adapter=com.eve.app.ui.home.HomeBannerAdapter().apply { submitList(listOf(HomeBanner(id="preview-banner",imageUrl="android.resource://${root.context.packageName}/${R.drawable.ic_launcher}"))) }
            }
            "test" -> {
                var answer="B"
                val adapter=QuestionAdapter(listOf(Question(id="preview",questionText="Which number is a prime number?",optionA="21",optionB="23",optionC="25",optionD="27")),{answer},{_,a -> answer=a},{false},{},{false},{},{42},studioConfig=config)
                root.findViewById<ViewPager2>(R.id.viewPager)?.adapter=adapter
                root.findViewById<TextView>(R.id.tvTestTitle)?.text="General Studies · practice preview"
                root.findViewById<View>(R.id.btnNext)?.setOnClickListener { navigate("result") }
                root.findViewById<View>(R.id.btnPrev)?.setOnClickListener { answer="";adapter.notifyDataSetChanged() }
                root.findViewById<View>(R.id.btnClear)?.setOnClickListener { answer="";adapter.notifyDataSetChanged() }
            }
            "result", "result_detail" -> {
                rv(R.id.rvAnswers,AnswerAdapter().apply { submit(listOf(answer,answer.copy(number=2,selected="A",selectedText="21"))) })
                text("tvScore","18 / 20");text("tvScorePercentage","90%");text("tvCorrectCount","18");text("tvWrongCount","1");text("tvUnattemptedCount","1")
                root.findViewById<View>(R.id.btnReattempt)?.setOnClickListener { navigate("test") }
            }
            "history" -> rv(R.id.rvHistory,HistoryAdapter { navigate("result") }.apply { submit(listOf(TestAttempt(id="preview",examName="General Studies",category="Practice",score=18.0,total=20,correct=18,wrong=1,unattempted=1,timestamp=1791331200000L))) })
            "leaderboard" -> rv(R.id.rvLeaderboard,LeaderboardAdapter("preview").apply { submit(listOf(LeaderboardEntry(userId="preview",displayName="Alex Student",score=18.0,total=20),LeaderboardEntry(userId="preview-2",displayName="Sam Student",score=17.0,total=20))) })
            "admin", "edit_exam" -> {
                rv(R.id.rvQuestions,QuestionManageAdapter({},{}).apply { submit(listOf(question)) })
                rv(R.id.rvAdmins,AdminEmailAdapter {}.apply { submit(listOf("admin@example.com")) })
            }
            "activity_log" -> rv(R.id.rvAuditLogs,AuditLogAdapter().apply { submit(listOf(AdminAuditLog(id="preview",actionType="UI_STUDIO_PUBLISH",description="Published visual configuration",adminEmail="admin@example.com",timestamp=1791331200000L))) })
            "admin_analytics" -> {
                rv(R.id.rvExams,AnalyticsExamAdapter().apply { submit(listOf(ExamAnalytics(examName=exam.examName,attemptCount=120,uniqueUsers=80,averageScore=16.5))) })
                rv(R.id.rvQuestions,AnalyticsQuestionAdapter().apply { submit(listOf(QuestionAnalytics(questionText=question.questionText,questionNumber=1,attempts=120,correct=90,wrong=20,unattempted=10))) })
            }
            "manage_existing_exams" -> rv(R.id.rvExams,ExistingExamsAdapter({navigate("exam_tests")},{navigate("edit_exam")},{navigate("test")},{}).apply { submitList(listOf(ExamWithStats(exam,120))) })
            "generated_tests", "manage_exams" -> {
                val adapter=GeneratedTestAdapter({_,_->},{navigate("test")},{})
                adapter.submit(listOf(GeneratedTest(id="preview",examName=exam.examName,title="General Studies · Practice 1",questionCount=20,status="live",generatedAt=1791331200000L)))
                rv(if(screen=="generated_tests")R.id.rvGeneratedTests else R.id.rvGeneratedTestsForExam,adapter)
            }
            "exam_tests" -> rv(R.id.rvTests,ExamTestsAdapter({navigate("exam_tests")},{_,_->navigate("test")}).apply { submitList(listOf(ExamTestsListItem.HeaderItem("Practice tests"),ExamTestsListItem.TestItem(GeneratedTest(id="preview",examName=exam.examName,status="live",questionCount=20),"General Studies · Practice 1","20 questions · 20 minutes",false))) })
            "manage_users" -> rv(R.id.rvUsers,UserAdminAdapter({},{_,_->}).apply { submitList(listOf(AdminUser(id="preview",displayName="Alex Student",email="student@example.com",createdAt=1791331200000L))) })
            "manage_polls" -> rv(R.id.rvPolls,PollsAdminAdapter({},{}).apply { submitList(listOf(Poll(id="preview",question="Which subject will you practice next?",options=listOf("General Studies","Arithmetic"),createdAt=1791331200000L,voteCounts=mapOf("0" to 12L,"1" to 8L)))) })
            "feedback_messages" -> rv(R.id.rvFeedbackMessages,FeedbackMessagesAdapter({},{},{}).apply { submitList(listOf(FeedbackMessage(id="preview",message="The practice explanations are helpful.",userName="Alex Student",userEmail="student@example.com",timestamp=1791331200000L))) })
            "send_notification" -> rv(R.id.rvSentBroadcasts,SentBroadcastAdapter({},{},{}).apply { submitList(listOf(BroadcastMessage(id="preview",title="Practice is ready",message="Try today's General Studies practice.",sentBy="admin@example.com",sentAt=1791331200000L))) })
            "flagged_questions" -> rv(R.id.rvFlaggedQuestions,FlaggedQuestionsAdapter({},{}).apply { submit(listOf(AggregatedFlaggedQuestion("preview","studio-example",exam.examName,question.questionText,2,listOf("Explanation"),listOf("Please explain further"),listOf("preview-flag"),1791331200000L))) })
            "manage_premium" -> {
                rv(R.id.rvPremiumUsers,AdminPremiumUserAdapter(onExtend={},onRevoke={}).apply { submitList(listOf(AdminPremiumUserDto("preview","student@example.com","Alex Student","monthly","Monthly","preview-order","preview-payment",1791331200000L,1793923200000L,false,"active","preview",1791331200000L,1791331200000L))) })
                rv(R.id.rvTransactions,AdminTransactionAdapter().apply { submitList(listOf(AdminTransactionDto("preview-order","preview","student@example.com","monthly","Monthly",9900,"INR",30,false,"sandbox","preview-provider","preview-payment","PAID",1791331200000L,1793923200000L,1791331200000L))) })
            }
            "bookmarks" -> rv(R.id.rvBookmarks,com.eve.app.ui.bookmarks.BookmarkAdapter({navigate("result")},{},{false}).apply { submitList(listOf(com.eve.app.data.model.BookmarkedQuestion(questionId="preview",examName="General Studies",questionNumber=1,questionText=question.questionText,optionA="21",optionB="23",optionC="25",optionD="27",correctAnswer="B",explanation=answer.explanation))) })
            "mistakes" -> rv(R.id.rvMistakes,com.eve.app.ui.mistakes.MistakesAdapter({},{false},{false}).apply { submitList(listOf(com.eve.app.data.remote.MistakeItem(questionId="preview",questionText=question.questionText,selected="A",selectedText="21",correct="B",correctText="23",explanation=answer.explanation))) })
            "performance" -> rv(R.id.rvTopics,com.eve.app.ui.performance.TopicStatAdapter().apply { submit(listOf(com.eve.app.data.model.TopicStat("Arithmetic",18,20),com.eve.app.data.model.TopicStat("General Studies",14,20))) })
            "profile" -> { text("tvName","Alex Student");text("tvEmail","student@example.com");text("tvEveId","EVE-PREVIEW") }
            "notifications" -> rv(R.id.rvNotifications,NotificationAdapter().apply { submit(listOf(StoredNotification("Practice is ready","Your next General Studies test is available.",1791331200000L,false))) })
            "manage_syllabus" -> rv(R.id.rvSyllabuses,AdminSyllabusAdapter({},{},{}).apply { submit(listOf(exam)) })
            "syllabus" -> rv(R.id.rvSyllabus,SyllabusAdapter({_,_->navigate("syllabus")},{navigate("syllabus")}).apply { submitList(listOf(exam)) })
        }
    }
}

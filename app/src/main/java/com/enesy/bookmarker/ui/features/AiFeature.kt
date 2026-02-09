package com.enesy.bookmarker.ui.features

import androidx.annotation.DrawableRes
import com.enesy.bookmarker.R

enum class AiFeature(
    val title: String,
    @DrawableRes val iconRes: Int,
    val systemPrompt: String
) {
    QUICK_RECAP(
        title = "Quick Recap",
        iconRes = R.drawable.ic_ai_recap,
        systemPrompt = "Summarize the key themes and takeaways of the selected items in one clear paragraph."
    ),
    ACTION_PLAN(
        title = "Action Plan",
        iconRes = R.drawable.ic_ai_plan,
        systemPrompt = "Analyze the selected content and extract concrete, actionable steps. Format the output as a Checklist."
    ),
    ELI5(
        title = "ELI5",
        iconRes = R.drawable.ic_ai_simple,
        systemPrompt = "Explain the concepts in the selected text simply, as if I am 5 years old. Use analogies."
    ),
    IDEA_GENERATOR(
        title = "Idea Generator",
        iconRes = R.drawable.ic_ai_idea,
        systemPrompt = "Generate 5 creative content ideas (Blog posts, Tweets, Videos) inspired by the selected notes/bookmarks."
    ),
    CONNECT_DOTS(
        title = "Connect Dots",
        iconRes = R.drawable.ic_ai_connect,
        systemPrompt = "Find hidden connections, patterns, or synthesizing insights between these separate items."
    ),
    QUIZ_ME(
        title = "Quiz Me",
        iconRes = R.drawable.ic_ai_quiz,
        systemPrompt = "Create 3 multiple-choice questions to test my understanding of this content. Provide the correct answers at the very end."
    ),
    DEBATE_MASTER(
        title = "Debate Master",
        iconRes = R.drawable.ic_ai_debate,
        systemPrompt = "Act as a critic. Logically challenge the arguments or ideas presented in the selected text."
    )
}
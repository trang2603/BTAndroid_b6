package com.demo

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.demo.databinding.ActivityMainBinding
import com.demo.model.Question
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var questions: List<Question>
    private var currentQuestionIndex = 0
    private lateinit var answerButtons: MutableList<Button>
    private var countHeart = 5
    private var mark = 0

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        questions = getDataQuestion(this)
        startGame()

        binding.btnNext.setOnClickListener {
            currentQuestionIndex++
            showQuestionAndAnswer()
            binding.btnNext.visibility = View.INVISIBLE
            binding.notify.text = ""
        }

    }

    private fun clearGridLayout(suggestAnswer: GridLayout) {
        suggestAnswer.removeAllViews()
    }

    private fun startGame() {
        countHeart = 5
        binding.countHeart.text = countHeart.toString()
        binding.mark.text = "0"
        currentQuestionIndex = 0
        showQuestionAndAnswer()
    }

    fun getAnswerForQuestion(questions: List<Question>, index: Int): String? {
        return if (index in questions.indices) {
            questions[index].answer
        } else {
            null
        }
    }

    private fun generateLetters(answer: String, numberOfButtons: Int): List<String> {
        val answerLetters = answer.toUpperCase().filter { it.isLetter() }.toList()
        val allLetters = answerLetters.toMutableList()

        Log.e("", "${answerLetters.size}")
        Log.e("", "$numberOfButtons")
        val random = ('A'..'Z').filterNot { it in answerLetters }.shuffled().take(numberOfButtons - answerLetters.size)
        Log.e("", "${random.size}")
        allLetters.addAll(random)

        //tron cac chu cai
        allLetters.shuffle()
        return allLetters.map { it.toString() }

    }

    private fun layoutAnswer(answer: String) {
        val numberOfButtons = answer.length
        val columnCount = 8

        binding.answer.removeAllViews()
        answerButtons = mutableListOf()

        for (i in 0 until numberOfButtons) {
            val button = Button(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dpToPx(50)
                    height = dpToPx(60)
                    gravity = Gravity.CENTER
                    rowSpec = GridLayout.spec(i / columnCount)
                    columnSpec = GridLayout.spec(i % columnCount)
                }
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_tile)
                textSize = 24f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                text = ""
                setOnClickListener { onAnswerButtonClick(this) }
            }
            answerButtons.add(button)
            binding.answer.addView(button)
        }
    }

    private fun onAnswerButtonClick(button: Button) {
        val letter = button.text.toString()
        if (letter.isNotEmpty()) {
            //dat lai gia tri cho button
            button.text = ""
            button.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_tile)
            val suggestButton = button.tag as? Button
            suggestButton?.visibility = View.VISIBLE
            button.tag = null
        }
    }

    private fun layoutSuggestAnswer(answer: String) {
        val columnCount = 8
        val numberOfButtons = 16

        //tao danh sach chu cai cho cac button
        val letters = generateLetters(answer, numberOfButtons)

        for (i in 0 until numberOfButtons) {
            val button = Button(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dpToPx(50)
                    height = dpToPx(60)
                    gravity = Gravity.CENTER
                    rowSpec = GridLayout.spec(i / columnCount)
                    Log.e("", "$rowSpec")
                    columnSpec = GridLayout.spec(i % columnCount)
                    Log.e("", "$columnSpec")

                }
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_tile_hover)
                textSize = 24f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                text = letters[i]
                setOnClickListener { onLetterButtonClick(this) }
            }
            binding.suggestAnswer.addView(button)
        }
    }

    private fun getDataQuestion(context: Context): List<Question> {
        val json = context.resources.openRawResource(R.raw.questions)
            .bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Question>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun showQuestionAndAnswer() {
        val question = questions[currentQuestionIndex]
        val resId = resources.getIdentifier(question.img, "drawable", packageName)
        binding.imgQuestion.setImageResource(resId)
        val answer = getAnswerForQuestion(questions, currentQuestionIndex) ?: return
        clearGridLayout(binding.suggestAnswer)
        layoutAnswer(answer)
        layoutSuggestAnswer(answer)
    }

    private fun isAnswerCorrect(): Boolean {
        val userAnswer = answerButtons.joinToString(separator = "") { it.text.toString() }
        val correctAnswer = questions[currentQuestionIndex].answer
        return userAnswer == correctAnswer
    }

    private fun onLetterButtonClick(button: Button) {
        val letter = button.text.toString()

        if (letter.isNotEmpty()) {
            //cap nhat nut trong dau tien voi chu cai
            for (answerButton in answerButtons) {
                if (answerButton.text.isEmpty()) {
                    answerButton.text = letter
                    answerButton.tag = button
                    button.visibility = View.INVISIBLE
                    break
                }
            }
        }
        if (isAnswerComplete()) {
            if (isAnswerCorrect()) {
                changeAnswerButtonBackground(R.drawable.ic_tile_true)
                binding.btnNext.visibility = View.VISIBLE
                binding.notify.text = "Bạn đã trả lời đúng rồi!!!"
                mark += 100
                binding.mark.text = mark.toString()
            } else {
                changeAnswerButtonBackground(R.drawable.ic_tile_false)
                binding.notify.text = "Bạn đã trả lời sai rồi!!!"
                countHeart--
                binding.countHeart.text = countHeart.toString()
                if (countHeart <= 0) {
                    showGameOverDialog()
                }
            }
        }
    }

    private fun showGameOverDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Game over")
        dialog.setMessage("Bạn đã hết tim, bạn có muốn chơi lại không?")
        dialog.setPositiveButton("Có") { _, _ ->
            startGame()
        }
        dialog.setNegativeButton("Không") { _, _ ->
            finish()
        }
        dialog.create().show()
    }

    private fun isAnswerComplete(): Boolean {
        for (answerButton in answerButtons) {
            if (answerButton.text.isEmpty()) {
                changeAnswerButtonBackground(R.drawable.ic_tile)
                return false
            }
        }
        return true
    }

    private fun changeAnswerButtonBackground(icTileTrue: Int) {
        for (answerButton in answerButtons) {
            answerButton.background = ContextCompat.getDrawable(this, icTileTrue)
        }
    }
}
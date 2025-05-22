package com.example.mp3player
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mp3player.R

class Calc3 : AppCompatActivity() {
    private lateinit var resultTextView: TextView
    private var currentNumber = ""
    private var storedNumber = ""
    private var currentOperation = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calc3)

        resultTextView = findViewById(R.id.resultTextView)

        val numberButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        numberButtons.forEach { buttonId ->
            findViewById<Button>(buttonId).setOnClickListener {
                currentNumber += (it as Button).text
                updateDisplay()
            }
        }

        val operationButtons = listOf(
            R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide
        )

        operationButtons.forEach { buttonId ->
            findViewById<Button>(buttonId).setOnClickListener {
                if (currentNumber.isNotEmpty()) {
                    performCalculation()
                    currentOperation = (it as Button).text.toString()
                    storedNumber = currentNumber
                    currentNumber = ""
                }
            }
        }


        findViewById<Button>(R.id.btnClear).setOnClickListener {
            currentNumber = ""
            storedNumber = ""
            currentOperation = ""
            updateDisplay()
        }


        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            if (currentNumber.isNotEmpty() && storedNumber.isNotEmpty()) {
                performCalculation()
                currentOperation = ""
            }
        }
    }

    private fun performCalculation() {
        if (storedNumber.isNotEmpty() && currentNumber.isNotEmpty()) {
            val firstNum = storedNumber.toDouble()
            val secondNum = currentNumber.toDouble()
            val result = when (currentOperation) {
                "+" -> firstNum + secondNum
                "-" -> firstNum - secondNum
                "×" -> firstNum * secondNum
                "/" -> firstNum / secondNum
                else -> return
            }
            currentNumber = if (result % 1 == 0.0) {
                result.toInt().toString()
            } else {
                result.toString()
            }

            storedNumber = ""
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        resultTextView.text = currentNumber.ifEmpty { "0" }
    }
}
package com.example.myapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var number1: EditText
    private lateinit var number2: EditText
    private lateinit var plus: Button
    private lateinit var minus: Button
    private lateinit var umnoj: Button
    private lateinit var del: Button
    private lateinit var result: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        result = findViewById(R.id.result)
        number1 = findViewById(R.id.number1)
        number2 = findViewById(R.id.number2)
        plus = findViewById(R.id.plus)
        minus = findViewById(R.id.minus)
        umnoj = findViewById(R.id.umnoj)
        del = findViewById(R.id.del)

        plus.setOnClickListener(this)
        minus.setOnClickListener(this)
        umnoj.setOnClickListener(this)
        del.setOnClickListener(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    @SuppressLint("SetTextI18n")
    override fun onClick(view: View) {
        val num1 = number1.text.toString().toFloatOrNull() ?: 0f
        val num2 = number2.text.toString().toFloatOrNull() ?: 0f

        if (view.id == R.id.plus) {
            var res = num1 + num2
            result.text = res.toString()
        } else if (view.id == R.id.minus) {
            var res = num1 - num2
            result.text = res.toString()
        } else if (view.id == R.id.umnoj) {
            var res = num1 * num2
            result.text = res.toString()
        } else if (view.id == R.id.del) {
            if (num2 != 0f) {
                var res = num1 / num2
                result.text = res.toString()
            } else {
                result.text = "Ошибка-деление на 0"
            }
        }
    }
}
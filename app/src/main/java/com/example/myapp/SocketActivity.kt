package com.example.myapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ


class SocketActivity : AppCompatActivity() {
    private var log_tag: String = "ZMQ_CLIENT"
    private lateinit var tvSockets: TextView
    private lateinit var btnSendToServer: Button
    private lateinit var btnClearLog: Button
    private lateinit var tvStatus: TextView

    private var isConnected = false
    private var messageCounter = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socket)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        tvSockets = findViewById(R.id.tvSockets)
        btnSendToServer = findViewById(R.id.SendToServer)
        tvStatus = findViewById(R.id.tvStatus)

        // Добавьте кнопку очистки в ваш layout
        btnClearLog = findViewById(R.id.btnClearLog)
    }

    private fun setupClickListeners() {
        btnSendToServer.setOnClickListener {
            if (!isConnected) {
                startClient()
            } else {
                appendToLog("Клиент уже запущен")
            }
        }

        btnClearLog.setOnClickListener {
            tvSockets.text = "Лог соединения:\n"
            messageCounter = 0
        }
    }

    private fun startClient() {
        Thread {
            try {
                runOnUiThread {
                    tvStatus.text = "Статус: Подключение"
                    btnSendToServer.isEnabled = false
                }

                val context = ZContext()
                val socket = context.createSocket(SocketType.REQ)

               // val serverAddress = "tcp://192.168.1.103:2222"
                val serverAddress = "tcp://10.48.236.49:2222"

                socket.connect(serverAddress)
                socket.setReceiveTimeOut(5000) // Таймаут 5 секунд

                isConnected = true

                runOnUiThread {
                    tvStatus.text = "Статус: Подключено к $serverAddress"
                    appendToLog("Успешное подключение к серверу")
                }

                for (i in 1..10) {
                    messageCounter++
                    val request = "Hello from Android! Сообщение #$messageCounter"

                    try {
                        val sendResult = socket.send(request.toByteArray(ZMQ.CHARSET), 0)

                        if (sendResult) {
                            appendToLog("Отправлено: $request")

                            val reply = socket.recv(0)
                            if (reply != null) {
                                val replyStr = String(reply, ZMQ.CHARSET)
                                appendToLog("Получено: $replyStr")
                            } else {
                                appendToLog("Таймаут ожидания ответа")
                                break
                            }

                            Thread.sleep(1000)

                        } else {
                            appendToLog("Ошибка отправки сообщения")
                            break
                        }

                    } catch (e: Exception) {
                        Log.e(log_tag, "Ошибка при отправке/получении", e)
                        appendToLog("Ошибка: ${e.message}")
                        break
                    }
                }

                isConnected = false
                runOnUiThread {
                    tvStatus.text = "Статус: Отключено"
                    btnSendToServer.isEnabled = true
                    appendToLog("Сессия завершена")
                }

                socket.close()
                context.close()

            } catch (e: Exception) {
                Log.e(log_tag, "Ошибка подключения", e)
                runOnUiThread {
                    tvStatus.text = "Статус: Ошибка подключения"
                    btnSendToServer.isEnabled = true
                    appendToLog("Ошибка подключения: ${e.message}")
                }
                isConnected = false
            }
        }.start()
    }

    private fun appendToLog(message: String) {
        runOnUiThread {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
            val logEntry = "[$timestamp] $message\n"
            tvSockets.append(logEntry)
        }
    }
}
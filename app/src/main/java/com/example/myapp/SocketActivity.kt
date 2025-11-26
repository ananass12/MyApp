package com.example.myapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.telephony.*
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.text.SimpleDateFormat
import java.util.*

class SocketActivity : AppCompatActivity() {

    private val TAG = "ZMQ_CLIENT"
    private lateinit var tvStatus: TextView
    private lateinit var btnSend: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnWrite: Button
    private lateinit var btnStopWrite: Button

    private lateinit var latVal: TextView
    private lateinit var lonVal: TextView
    private lateinit var altVal: TextView
    private lateinit var timeVal: TextView

    private lateinit var typeVal: TextView
    private lateinit var operVal: TextView
    private lateinit var tacVal: TextView
    private lateinit var pciVal: TextView
    private lateinit var cidVal: TextView
    private lateinit var rsrpVal: TextView
    private lateinit var asuVal: TextView
    private var isConnected = false
    private var messageCount = 0
    private var isWriting = false
    private var writingThread: Thread? = null

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socket)

        initUI()
        requestPermissions()

        btnSend.setOnClickListener { if (!isConnected) startClient() }
        btnUpdate.setOnClickListener { collectData() }
        btnWrite.setOnClickListener { startWriting() }
        btnStopWrite.setOnClickListener { stopWriting() }
    }

    private fun initUI() {
        tvStatus = findViewById(R.id.tvStatus)
        btnSend = findViewById(R.id.SendToServer)
        btnUpdate = findViewById(R.id.btnUpdateAll)
        btnWrite = findViewById(R.id.btnWrite)
        btnStopWrite = findViewById(R.id.btnStopWrite)

        latVal = findViewById(R.id.znachLatitude)
        lonVal = findViewById(R.id.znachLongitude)
        altVal = findViewById(R.id.znachAltitude)
        timeVal = findViewById(R.id.znachTime)

        typeVal = findViewById(R.id.znachNetworkType)
        operVal = findViewById(R.id.znachOperator)
        tacVal = findViewById(R.id.znachLacTac)
        pciVal = findViewById(R.id.znachPci)
        cidVal = findViewById(R.id.znachCid)
        rsrpVal = findViewById(R.id.znachRsrp)
        asuVal = findViewById(R.id.znachAsu)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            ),
            100
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(): Location? {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        return lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun collectData(): JSONObject? {
        try {
            val tele = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            val loc = getLocation() ?: return null

            updateLocationUI(loc)

            val root = JSONObject()
            val locObj = JSONObject()

            locObj.put("Latitude", loc.latitude)
            locObj.put("Longitude", loc.longitude)
            locObj.put("Altitude", loc.altitude)
            locObj.put("Timestamp", System.currentTimeMillis())
            root.put("Location", locObj)

            val cellsArr = JSONArray()
            tele.allCellInfo?.forEach { ci ->
                when (ci) {
                    is CellInfoLte -> cellsArr.put(buildLte(ci))
                    is CellInfoGsm -> cellsArr.put(buildGsm(ci))
                }
            }

            root.put("CellInfo", cellsArr)
            return root

        } catch (e: Exception) {
            Log.e(TAG, "collectData", e)
            return null
        }
    }

    private fun buildLte(ci: CellInfoLte): JSONObject {
        val d = JSONObject()

        val id = ci.cellIdentity
        val s = ci.cellSignalStrength

        d.put("type", "LTE")
        d.put("MCC", id.mcc)
        d.put("MNC", id.mnc)
        d.put("PCI", id.pci)
        d.put("TAC", id.tac)
        d.put("CI", id.ci)
        d.put("RSRP", s.rsrp)
        d.put("ASU", s.asuLevel)

        updateNetworkUI(d)
        return d
    }

    private fun buildGsm(ci: CellInfoGsm): JSONObject {
        val d = JSONObject()

        val id = ci.cellIdentity
        val s = ci.cellSignalStrength

        d.put("type", "GSM")
        d.put("MCC", id.mcc)
        d.put("MNC", id.mnc)
        d.put("LAC", id.lac)
        d.put("CID", id.cid)
        d.put("ASU", s.asuLevel)
        d.put("Dbm", s.dbm)

        updateNetworkUI(d)
        return d
    }

    private fun updateLocationUI(loc: Location) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        runOnUiThread {
            latVal.text = loc.latitude.toString()
            lonVal.text = loc.longitude.toString()
            altVal.text = loc.altitude.toString()
            timeVal.text = sdf.format(Date())
        }
    }

    private fun updateNetworkUI(d: JSONObject) {
        runOnUiThread {
            typeVal.text = d.optString("type", "—")
            operVal.text = "${d.optString("MCC", "")}/${d.optString("MNC", "")}"
            tacVal.text = d.optString("TAC", d.optString("LAC", "—"))
            cidVal.text = d.optString("CI", d.optString("CID", "—"))
            pciVal.text = d.optString("PCI", "—")
            rsrpVal.text = d.optString("RSRP", "—")
            asuVal.text = d.optString("ASU", "—")
        }
    }

    private fun readOneLine(): String? {
        val dir = getExternalFilesDir("MyAppLogs") ?: return null
        val file = java.io.File(dir, "log.json")

        if (!file.exists() || file.length() == 0L) return null

        val lines = file.readLines()
        if (lines.isEmpty()) return null

        return lines.first()
    }

    private fun removeFirstLine() {
        val dir = getExternalFilesDir("MyAppLogs") ?: return
        val file = java.io.File(dir, "log.json")

        if (!file.exists()) return

        val lines = file.readLines()
        if (lines.size <= 1) {
            file.writeText("")
            return
        }

        val newText = lines.drop(1).joinToString("\n")
        file.writeText(newText)
    }

    private fun startClient() {
        Thread {
            try {
                runOnUiThread {
                    tvStatus.text = "Статус: подключение…"
                    btnSend.isEnabled = false
                }

                val context = ZContext()
                val socket = context.createSocket(SocketType.REQ)
                //val address = "tcp://10.48.236.49:2222"
                val address = "tcp://10.147.163.49:2222"

                socket.connect(address)
                socket.setReceiveTimeOut(5000)
                isConnected = true

                runOnUiThread { tvStatus.text = "Статус: подключено" }

                repeat(10) {
                    val line = readOneLine() ?: return@repeat  //читаем 1 строку из файла
                    socket.send(line.toByteArray(ZMQ.CHARSET), 0)

                    val reply = socket.recv()
                    if (reply != null) {
                        removeFirstLine()  //удаляем только если сервер ответил
                    }

                    Thread.sleep(1000)
                }

                socket.close()
                context.close()
                isConnected = false

                runOnUiThread {
                    tvStatus.text = "Статус: отключено"
                    btnSend.isEnabled = true
                }

            } catch (e: Exception) {
                Log.e(TAG, "startClient", e)
                runOnUiThread {
                    tvStatus.text = "Ошибка подключения"
                    btnSend.isEnabled = true
                }
            }
        }.start()
    }

    private fun startWriting() {
        if (isWriting) return
        isWriting = true

        tvStatus.text = "Статус: запись..."
        messageCount = 0

        writingThread = Thread {
            while (isWriting) {
                val json = collectData()?.toString() ?: continue
                writeToFile(json)
                Thread.sleep(10_000)
            }
        }
        writingThread!!.start()
    }

    private fun stopWriting() {
        isWriting = false
        writingThread = null
        tvStatus.text = "Статус: запись остановлена"
    }

    private fun writeToFile(text: String) {
        try {
            val dir = getExternalFilesDir("MyAppLogs")!!
            if (!dir.exists()) dir.mkdirs()

            val file = java.io.File(dir, "log.json")
            messageCount++

            file.appendText("$text\n")

            runOnUiThread {
                tvStatus.text = "Статус: записано $messageCount строк"
            }

        } catch (e: Exception) {
            Log.e(TAG, "writeToFile", e)
            runOnUiThread { tvStatus.text = "Ошибка записи" }
        }
    }
}
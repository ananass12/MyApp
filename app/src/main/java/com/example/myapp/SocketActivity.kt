package com.example.myapp

import android.Manifest
import android.annotation.SuppressLint
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
    private val SERVER_ADDRESS = "tcp://192.168.1.103:2222"
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

    private var lastLocation: Location? = null
    private var locationCallback: com.google.android.gms.location.LocationCallback? = null

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val telephonyManager by lazy { getSystemService(TELEPHONY_SERVICE) as TelephonyManager }
    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private val dateFormatter by lazy {
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        )
    }

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
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun collectData() {
        val loc = getLocation()
        if (loc == null) {
            tvStatus.text = "Нет данных о местоположении"
            return
        }

        updateLocationUI(loc)

        telephonyManager.allCellInfo?.firstOrNull()?.let { ci ->
            when (ci) {
                is CellInfoLte -> updateNetworkUI(createLteJson(ci))
                is CellInfoGsm -> updateNetworkUI(createGsmJson(ci))
            }
        }

        tvStatus.text = "Данные обновлены"
    }

    private fun createLteJson(cellInfo: CellInfoLte): JSONObject {
        return JSONObject().apply {
            put("type", "LTE")
            put("MCC", cellInfo.cellIdentity.mcc)
            put("MNC", cellInfo.cellIdentity.mnc)
            put("PCI", cellInfo.cellIdentity.pci)
            put("TAC", cellInfo.cellIdentity.tac)
            put("CI", cellInfo.cellIdentity.ci)
            put("RSRP", cellInfo.cellSignalStrength.rsrp)
            put("ASU", cellInfo.cellSignalStrength.asuLevel)
        }
    }

    private fun createGsmJson(cellInfo: CellInfoGsm): JSONObject {
        return JSONObject().apply {
            put("type", "GSM")
            put("MCC", cellInfo.cellIdentity.mcc)
            put("MNC", cellInfo.cellIdentity.mnc)
            put("LAC", cellInfo.cellIdentity.lac)
            put("CI", cellInfo.cellIdentity.cid)
            put("ASU", cellInfo.cellSignalStrength.asuLevel)
            put("Dbm", cellInfo.cellSignalStrength.dbm)
        }
    }

    private fun updateLocationUI(loc: Location) {
        runOnUiThread {
            latVal.text = loc.latitude.toString()
            lonVal.text = loc.longitude.toString()
            altVal.text = loc.altitude.toString()
            timeVal.text = dateFormatter.format(Date())
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

                socket.connect(SERVER_ADDRESS)
                socket.setReceiveTimeOut(5000)
                isConnected = true

                runOnUiThread { tvStatus.text = "Статус: подключено" }

                while (true) {
                    val line = readOneLine() ?: break   //если строк нет
                    socket.send(line.toByteArray(ZMQ.CHARSET), 0)

                    val reply = socket.recv()
                    if (reply != null) {
                        removeFirstLine()
                    } else {
                        Log.w(TAG, "Сервер не ответил на сообщение")
                    }

                    Thread.sleep(2000)
                }

                socket.close()
                context.close()
                isConnected = false

                runOnUiThread {
                    tvStatus.text = "Статус: все строки отправлены"
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

    @SuppressLint("MissingPermission")
    private fun startWriting() {
        if (isWriting) return

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            tvStatus.text = "Нет разрешений на локацию"
            return
        }

        isWriting = true
        messageCount = 0
        tvStatus.text = "Статус: запись..."

        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                for (loc in result.locations) {
                    if (lastLocation != null &&
                        lastLocation!!.latitude == loc.latitude &&
                        lastLocation!!.longitude == loc.longitude
                    ) continue

                    lastLocation = loc
                    val json = buildJsonFromLocation(loc)
                    writeToFile(json.toString())
                }
            }
        }

        val request = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        )
            .setMinUpdateDistanceMeters(1f)
            .build()

        fused.requestLocationUpdates(request, locationCallback!!, mainLooper)
    }

    @SuppressLint("MissingPermission")
    private fun buildJsonFromLocation(loc: Location): JSONObject {
        val root = JSONObject()

        val locObj = JSONObject()
        locObj.put("Latitude", loc.latitude)
        locObj.put("Longitude", loc.longitude)
        locObj.put("Altitude", loc.altitude)
        locObj.put("Timestamp", System.currentTimeMillis())
        root.put("Location", locObj)

        val cellsArr = JSONArray()
        telephonyManager.allCellInfo?.forEach { ci ->
            when (ci) {
                is CellInfoLte -> {
                    val cellJson = createLteJson(ci)
                    cellsArr.put(cellJson)
                    updateNetworkUI(cellJson)
                }
                is CellInfoGsm -> {
                    val cellJson = createGsmJson(ci)
                    cellsArr.put(cellJson)
                    updateNetworkUI(cellJson)
                }
            }
        }
        root.put("CellInfo", cellsArr)

        updateLocationUI(loc)
        return root
    }

    private fun stopWriting() {
        if (!isWriting) return
        isWriting = false
        locationCallback?.let { fused.removeLocationUpdates(it) }
        locationCallback = null
        lastLocation = null
        tvStatus.text = "Статус: запись остановлена"
    }

    private fun writeToFile(text: String) {
        try {
            val dir = getExternalFilesDir("MyAppLogs")!!
            if (!dir.exists()) dir.mkdirs()

            val file = java.io.File(dir, "log.json")
            messageCount++

            file.appendText("$text\n")

            runOnUiThread { tvStatus.text = "Статус: записано $messageCount строк" }

        } catch (e: Exception) {
            Log.e(TAG, "writeToFile", e)
            runOnUiThread { tvStatus.text = "Ошибка записи" }
        }
    }
}
package com.example.cs388_mobile_application_part_2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.GRAVITY_EARTH
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import kotlin.math.abs
import kotlin.math.log10

@SuppressLint("SetTextI18n")
class ToolsFragment : Fragment(), SensorEventListener {

    private var mediaRecorder: MediaRecorder? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false
    private val ORANGE_THRESHOLD = 20.0
    private val RED_THRESHOLD = 30.0
    private lateinit var tvDecibelLevel: TextView
    private lateinit var tvDecibelLabel: TextView
    private lateinit var btnStartDecibel: Button
    private lateinit var btnStopDecibel: Button

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isMeasuringHeight = false
    private var verticalVelocity = 0f
    private var heightTraveled = 0f
    private var lastTimestamp = 0L
    private lateinit var tvHeightResult: TextView
    private lateinit var tvHeightLabel: TextView
    private lateinit var btnStartHeight: Button
    private lateinit var btnStopHeight: Button

    private var isDetectingShake = false
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private val SHAKE_THRESHOLD_LIGHT = 5f
    private val SHAKE_THRESHOLD_MODERATE = 10f
    private val SHAKE_THRESHOLD_HEAVY = 15f
    private lateinit var tvShakeLevel: TextView
    private lateinit var tvShakeLabel: TextView
    private lateinit var btnStartShake: Button
    private lateinit var btnStopShake: Button

    private var isLevelMeterActive = false
    private val LEVEL_THRESHOLD_PERFECT = 2f
    private val LEVEL_THRESHOLD_GOOD = 5f
    private lateinit var tvLevelAngle: TextView
    private lateinit var tvLevelLabel: TextView
    private lateinit var btnStartLevel: Button
    private lateinit var btnStopLevel: Button

    private var lightSensor: Sensor? = null
    private var isLightMeterActive = false
    private val LIGHT_THRESHOLD_DIM = 50f
    private val LIGHT_THRESHOLD_MODERATE = 200f
    private val LIGHT_THRESHOLD_BRIGHT = 1000f
    private lateinit var tvLightLevel: TextView
    private lateinit var tvLightLabel: TextView
    private lateinit var btnStartLight: Button
    private lateinit var btnStopLight: Button

    private lateinit var spinnerDiceType: android.widget.Spinner
    private lateinit var spinnerDiceCount: android.widget.Spinner
    private lateinit var btnRollDice: Button
    private lateinit var tvDiceResults: TextView
    private lateinit var tvDiceTotal: TextView

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else tvDecibelLabel.text = "Microphone permission denied"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_tools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvDecibelLevel = view.findViewById(R.id.tvDecibelLevel)
        tvDecibelLabel = view.findViewById(R.id.tvDecibelLabel)
        btnStartDecibel = view.findViewById(R.id.btnStartDecibel)
        btnStopDecibel = view.findViewById(R.id.btnStopDecibel)

        tvHeightResult = view.findViewById(R.id.tvHeightResult)
        tvHeightLabel = view.findViewById(R.id.tvHeightLabel)
        btnStartHeight = view.findViewById(R.id.btnStartHeight)
        btnStopHeight = view.findViewById(R.id.btnStopHeight)

        tvShakeLevel = view.findViewById(R.id.tvShakeLevel)
        tvShakeLabel = view.findViewById(R.id.tvShakeLabel)
        btnStartShake = view.findViewById(R.id.btnStartShake)
        btnStopShake = view.findViewById(R.id.btnStopShake)

        tvLevelAngle = view.findViewById(R.id.tvLevelAngle)
        tvLevelLabel = view.findViewById(R.id.tvLevelLabel)
        btnStartLevel = view.findViewById(R.id.btnStartLevel)
        btnStopLevel = view.findViewById(R.id.btnStopLevel)

        tvLightLevel = view.findViewById(R.id.tvLightLevel)
        tvLightLabel = view.findViewById(R.id.tvLightLabel)
        btnStartLight = view.findViewById(R.id.btnStartLight)
        btnStopLight = view.findViewById(R.id.btnStopLight)

        spinnerDiceType = view.findViewById(R.id.spinnerDiceType)
        spinnerDiceCount = view.findViewById(R.id.spinnerDiceCount)
        btnRollDice = view.findViewById(R.id.btnRollDice)
        tvDiceResults = view.findViewById(R.id.tvDiceResults)
        tvDiceTotal = view.findViewById(R.id.tvDiceTotal)

        val diceTypes = arrayOf("d4", "d6", "d8", "d10", "d12", "d20")
        spinnerDiceType.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, diceTypes)
        spinnerDiceType.setSelection(1)

        val diceCounts = (1..10).map { it.toString() }.toTypedArray()
        spinnerDiceCount.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, diceCounts)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        btnStartDecibel.setOnClickListener { checkPermissionAndStart() }
        btnStopDecibel.setOnClickListener { stopListening() }
        btnStartHeight.setOnClickListener { startHeightMeasure() }
        btnStopHeight.setOnClickListener { stopHeightMeasure() }
        btnStartShake.setOnClickListener { startShakeDetection() }
        btnStopShake.setOnClickListener { stopShakeDetection() }
        btnStartLevel.setOnClickListener { startLevelMeter() }
        btnStopLevel.setOnClickListener { stopLevelMeter() }
        btnStartLight.setOnClickListener { startLightMeter() }
        btnStopLight.setOnClickListener { stopLightMeter() }
        btnRollDice.setOnClickListener { rollDice() }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }


    private fun startListening() {
        try {
            mediaRecorder = MediaRecorder(requireContext()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(requireContext().cacheDir.absolutePath + "/noise_temp.3gp")
                prepare()
                start()
            }
            isListening = true
            tvDecibelLabel.text = "Listening..."
            btnStartDecibel.isEnabled = false
            btnStopDecibel.isEnabled = true
            handler.post(decibelRunnable)
        } catch (_: Exception) {
            tvDecibelLabel.text = "Error starting microphone"
            isListening = false
            btnStartDecibel.isEnabled = true
            btnStopDecibel.isEnabled = false
            try { mediaRecorder?.release(); mediaRecorder = null } catch (_: Exception) {}
        }
    }

    private fun stopListening() {
        isListening = false
        handler.removeCallbacks(decibelRunnable)
        try { mediaRecorder?.stop(); mediaRecorder?.release(); mediaRecorder = null } catch (_: Exception) {}
        tvDecibelLevel.text = "-- dB"
        tvDecibelLevel.setTextColor(Color.BLACK)
        tvDecibelLabel.text = "Press Start to begin"
        btnStartDecibel.isEnabled = true
        btnStopDecibel.isEnabled = false
    }

    private val decibelRunnable = object : Runnable {
        override fun run() {
            if (!isListening) return
            val amplitude = mediaRecorder?.maxAmplitude ?: 0
            if (amplitude > 0) {
                val db = 20 * log10(amplitude.toDouble())
                tvDecibelLevel.text = "%.1f dB".format(db)
                when {
                    db >= RED_THRESHOLD -> {
                        tvDecibelLevel.setTextColor(Color.RED)
                        tvDecibelLabel.text = "⚠️ TOO LOUD! Keep it down!"
                    }
                    db >= ORANGE_THRESHOLD -> {
                        tvDecibelLevel.setTextColor("#FF6600".toColorInt())
                        tvDecibelLabel.text = "Getting loud..."
                    }
                    else -> {
                        tvDecibelLevel.setTextColor("#2E7D32".toColorInt())
                        tvDecibelLabel.text = "✅ Nice and quiet"
                    }
                }
            }
            handler.postDelayed(this, 300)
        }
    }

    private fun startHeightMeasure() {
        if (accelerometer == null) { tvHeightLabel.text = "No accelerometer found"; return }
        isMeasuringHeight = true
        verticalVelocity = 0f
        heightTraveled = 0f
        lastTimestamp = 0L
        tvHeightResult.text = "-- cm"
        tvHeightLabel.text = "Move phone upward..."
        btnStartHeight.isEnabled = false
        btnStopHeight.isEnabled = true
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun stopHeightMeasure() {
        isMeasuringHeight = false
        sensorManager.unregisterListener(this)
        btnStartHeight.isEnabled = true
        btnStopHeight.isEnabled = false
        val cm = abs(heightTraveled * 100f)
        tvHeightResult.text = "%.1f cm".format(cm)
        tvHeightLabel.text = "%.1f cm  (%.3f m)".format(cm, cm / 100f)
    }

    private fun startShakeDetection() {
        if (accelerometer == null) { tvShakeLabel.text = "No accelerometer found"; return }
        isDetectingShake = true
        lastX = 0f
        lastY = 0f
        lastZ = 0f
        tvShakeLevel.text = "0.0"
        tvShakeLevel.setTextColor("#2E7D32".toColorInt())
        tvShakeLabel.text = "Monitoring table..."
        btnStartShake.isEnabled = false
        btnStopShake.isEnabled = true
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun stopShakeDetection() {
        isDetectingShake = false
        sensorManager.unregisterListener(this)
        tvShakeLevel.text = "--"
        tvShakeLevel.setTextColor(Color.BLACK)
        tvShakeLabel.text = "Press Start to begin"
        btnStartShake.isEnabled = true
        btnStopShake.isEnabled = false
    }

    private fun startLevelMeter() {
        if (accelerometer == null) { tvLevelLabel.text = "No accelerometer found"; return }
        isLevelMeterActive = true
        tvLevelAngle.text = "0.0°"
        tvLevelAngle.setTextColor("#2E7D32".toColorInt())
        tvLevelLabel.text = "Measuring..."
        btnStartLevel.isEnabled = false
        btnStopLevel.isEnabled = true
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    private fun stopLevelMeter() {
        isLevelMeterActive = false
        sensorManager.unregisterListener(this)
        tvLevelAngle.text = "--"
        tvLevelAngle.setTextColor(Color.BLACK)
        tvLevelLabel.text = "Press Start to begin"
        btnStartLevel.isEnabled = true
        btnStopLevel.isEnabled = false
    }

    private fun startLightMeter() {
        if (lightSensor == null) { tvLightLabel.text = "No light sensor found"; return }
        isLightMeterActive = true
        tvLightLevel.text = "0 lux"
        tvLightLevel.setTextColor("#2E7D32".toColorInt())
        tvLightLabel.text = "Measuring..."
        btnStartLight.isEnabled = false
        btnStopLight.isEnabled = true
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun stopLightMeter() {
        isLightMeterActive = false
        sensorManager.unregisterListener(this, lightSensor)
        tvLightLevel.text = "-- lux"
        tvLightLevel.setTextColor(Color.BLACK)
        tvLightLabel.text = "Press Start to begin"
        btnStartLight.isEnabled = true
        btnStopLight.isEnabled = false
    }

    private fun rollDice() {
        val diceType = spinnerDiceType.selectedItem.toString().substring(1).toInt()
        val diceCount = spinnerDiceCount.selectedItem.toString().toInt()
        
        val results = mutableListOf<Int>()
        repeat(diceCount) {
            results.add((1..diceType).random())
        }
        
        val total = results.sum()
        tvDiceResults.text = results.joinToString(" + ")
        tvDiceTotal.text = "Total: $total"
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if(isMeasuringHeight){
                    if (lastTimestamp == 0L) {
                        lastTimestamp = event.timestamp
                        return
                    }

                    val dt = (event.timestamp - lastTimestamp) / 1_000_000_000f
                    lastTimestamp = event.timestamp

                    val netVertical = event.values[1] - GRAVITY_EARTH

                    if (abs(netVertical) < 0.5f) return

                    verticalVelocity += netVertical * dt
                    heightTraveled += verticalVelocity * dt
                    tvHeightResult.text = "%.1f".format(abs(heightTraveled * 100f))
                } else if (isDetectingShake) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    if (lastX != 0f || lastY != 0f || lastZ != 0f) {
                        val deltaX = abs(x - lastX)
                        val deltaY = abs(y - lastY)
                        val deltaZ = abs(z - lastZ)
                        val shakeForce = deltaX + deltaY + deltaZ

                        tvShakeLevel.text = "%.1f".format(shakeForce)

                        when {
                            shakeForce >= SHAKE_THRESHOLD_HEAVY -> {
                                tvShakeLevel.setTextColor(Color.RED)
                                tvShakeLabel.text = "⚠️ HEAVY SHAKE! Careful!"
                            }
                            shakeForce >= SHAKE_THRESHOLD_MODERATE -> {
                                tvShakeLevel.setTextColor("#FF6600".toColorInt())
                                tvShakeLabel.text = "Moderate shaking detected"
                            }
                            shakeForce >= SHAKE_THRESHOLD_LIGHT -> {
                                tvShakeLevel.setTextColor("#FFA500".toColorInt())
                                tvShakeLabel.text = "Light shake detected"
                            }
                            else -> {
                                tvShakeLevel.setTextColor("#2E7D32".toColorInt())
                                tvShakeLabel.text = "✅ Table is stable"
                            }
                        }
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                } else if (isLevelMeterActive) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val pitch = Math.toDegrees(kotlin.math.atan2(x.toDouble(), kotlin.math.sqrt((y * y + z * z).toDouble())))
                    val roll = Math.toDegrees(kotlin.math.atan2(y.toDouble(), kotlin.math.sqrt((x * x + z * z).toDouble())))
                    
                    val totalTilt = kotlin.math.sqrt(pitch * pitch + roll * roll)

                    tvLevelAngle.text = "%.1f°".format(totalTilt)

                    when {
                        totalTilt <= LEVEL_THRESHOLD_PERFECT -> {
                            tvLevelAngle.setTextColor("#2E7D32".toColorInt())
                            tvLevelLabel.text = "✅ Perfectly level!"
                        }
                        totalTilt <= LEVEL_THRESHOLD_GOOD -> {
                            tvLevelAngle.setTextColor("#FFA500".toColorInt())
                            tvLevelLabel.text = "Nearly level"
                        }
                        else -> {
                            tvLevelAngle.setTextColor(Color.RED)
                            tvLevelLabel.text = "⚠️ Surface is tilted"
                        }
                    }
                }
            }
            Sensor.TYPE_LIGHT -> {
                if (isLightMeterActive) {
                    val lux = event.values[0]
                    tvLightLevel.text = "%.0f lux".format(lux)

                    when {
                        lux < LIGHT_THRESHOLD_DIM -> {
                            tvLightLevel.setTextColor("#1565C0".toColorInt())
                            tvLightLabel.text = "🌙 Too dark - turn on lights!"
                        }
                        lux < LIGHT_THRESHOLD_MODERATE -> {
                            tvLightLevel.setTextColor("#FFA500".toColorInt())
                            tvLightLabel.text = "💡 Dim lighting"
                        }
                        lux < LIGHT_THRESHOLD_BRIGHT -> {
                            tvLightLevel.setTextColor("#2E7D32".toColorInt())
                            tvLightLabel.text = "✅ Good lighting for games"
                        }
                        else -> {
                            tvLightLevel.setTextColor("#FF6600".toColorInt())
                            tvLightLabel.text = "☀️ Very bright"
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        if (isListening) {
            handler.removeCallbacks(decibelRunnable)
            mediaRecorder?.release()
            mediaRecorder = null
        }
        if (isMeasuringHeight || isDetectingShake || isLevelMeterActive) sensorManager.unregisterListener(this, accelerometer)
        if (isLightMeterActive) sensorManager.unregisterListener(this, lightSensor)
    }

    override fun onResume() {
        super.onResume()
        if (isListening) startListening()
        if (isMeasuringHeight) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        if (isDetectingShake) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        if (isLevelMeterActive) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        if (isLightMeterActive) sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
}

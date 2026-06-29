package com.namaanda.investmentcalculator

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InvestmentCalculatorApp()
                }
            }
        }
    }
}

data class HasilInvestasi(
    val modalAwal: Double,
    val totalProfit: Double,
    val totalAkhir: Double
)

fun hitungInvestasi(
    modalAwalStr: String,
    persenStr: String,
    startDateStr: String,
    endDateStr: String
): Triple<HasilInvestasi, List<Float>, List<String>> {
    val modal = modalAwalStr.toDoubleOrNull() ?: 0.0
    val persen = persenStr.toDoubleOrNull() ?: 0.0

    if (startDateStr.isEmpty() || endDateStr.isEmpty()) return Triple(HasilInvestasi(modal, 0.0, modal), emptyList(), emptyList())

    val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
    val startDate = sdf.parse(startDateStr) ?: Date()
    val endDate = sdf.parse(endDateStr) ?: Date()

    val diffInMillis = endDate.time - startDate.time
    val days = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

    if (days <= 0) return Triple(HasilInvestasi(modal, 0.0, modal), emptyList(), emptyList())

    val chartData = mutableListOf<Float>()
    for (i in 0..days) {
        val value = modal * (1 + persen / 100).pow(i)
        chartData.add(value.toFloat())
    }

    val totalAkhir = modal * (1 + persen / 100).pow(days)
    val totalProfit = totalAkhir - modal
    val hasil = HasilInvestasi(modal, totalProfit, totalAkhir)

    val hasilBulanan = mutableListOf<String>()
    val cal = Calendar.getInstance()
    cal.time = startDate
    var monthCounter = 1

    val df = DecimalFormat("#,##0.###")
    val formatCur = { v: Double -> "Rp " + df.format(v).replace(',', 'X').replace('.', ',').replace('X', '.') }

    while (cal.timeInMillis < endDate.time) {
        cal.add(Calendar.MONTH, 1)
        if (cal.timeInMillis > endDate.time) {
            cal.time = endDate
        }
        val currentDays = ((cal.timeInMillis - startDate.time) / (1000 * 60 * 60 * 24)).toInt()
        val value = modal * (1 + persen / 100).pow(currentDays)
        hasilBulanan.add("Bulan $monthCounter -> ${formatCur(value)}")
        monthCounter++
    }

    return Triple(hasil, chartData, hasilBulanan)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentCalculatorApp() {
    var modalAwal by remember { mutableStateOf("3000000") }
    var profitHarian by remember { mutableStateOf("5") }
    var startDate by remember { mutableStateOf("1/5/2026") }
    var endDate by remember { mutableStateOf("30/9/2026") }

    var hasil by remember { mutableStateOf<HasilInvestasi?>(null) }
    var chartData by remember { mutableStateOf<List<Float>>(emptyList()) }
    var hasilBulanan by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Kalkulator Investasi",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = modalAwal,
            onValueChange = { modalAwal = it },
            label = { Text("Modal Awal") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = profitHarian,
            onValueChange = { profitHarian = it },
            label = { Text("Profit Harian (%)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    context,
                    { _, y, m, d -> startDate = "$d/${m + 1}/$y" },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mulai: $startDate")
        }
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    context,
                    { _, y, m, d -> endDate = "$d/${m + 1}/$y" },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sampai: $endDate")
        }
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val result = hitungInvestasi(modalAwal, profitHarian, startDate, endDate)
                hasil = result.first
                chartData = result.second
                hasilBulanan = result.third
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Hitung")
        }
        Spacer(Modifier.height(16.dp))

        if (chartData.isNotEmpty()) {
            Text("📊 Grafik Pertumbuhan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            AndroidView(
                factory = { ctx ->
                    LineChart(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            500
                        )
                    }
                },
                update = { chart ->
                    val entries = chartData.mapIndexed { i, v ->
                        Entry(i.toFloat(), v)
                    }
                    val dataSet = LineDataSet(entries, "Growth").apply {
                        valueTextSize = 10f
                    }
                    chart.data = LineData(dataSet)
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        hasil?.let {
            val df = DecimalFormat("#,##0.###")
            val formatCur = { v: Double -> "Rp " + df.format(v).replace(',', 'X').replace('.', ',').replace('X', '.') }

            Text("Modal Awal: ${formatCur(it.modalAwal)}")
            Text("Total Profit: ${formatCur(it.totalProfit)}")
            Text("Total Akhir: ${formatCur(it.totalAkhir)}")
            Spacer(Modifier.height(16.dp))

            Text("Detail Per Bulan:", fontWeight = FontWeight.Bold)
            hasilBulanan.forEach { detail ->
                Text(detail)
            }
        }
    }
}
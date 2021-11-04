package de.wiomoc.mystudid.activities

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.paging.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.database.HistoryDatabase
import de.wiomoc.mystudid.databinding.ActivityHistoryBinding
import de.wiomoc.mystudid.fragments.LoginDialogFragment
import de.wiomoc.mystudid.fragments.LoginDialogFragment.Companion.closeLoginDialog
import de.wiomoc.mystudid.fragments.LoginDialogFragment.Companion.showLoginDialog
import de.wiomoc.mystudid.services.HistoryManager
import de.wiomoc.mystudid.services.OnlineCardClient
import de.wiomoc.mystudid.services.PreferencesManager
import kotlinx.coroutines.Dispatchers
import org.jetbrains.anko.find
import org.jetbrains.anko.intentFor
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HistoryActivity : AppCompatActivity(), LoginDialogFragment.LoginDialogCallback {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class Adapter : PagingDataAdapter<HistoryDatabase.Transaction, ViewHolder>(DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_transaction, parent, false)
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            getItem(position).let {
                holder.itemView.apply {
                    find<TextView>(R.id.tv_transaction_amount).text = it?.amount?.let { String.format("%.2f€", it) }
                    find<TextView>(R.id.tv_transaction_date).text = it?.date.toString()
                    find<TextView>(R.id.tv_transaction_location).text = it?.location
                }
            }
        }

        companion object {
            private val DIFF_CALLBACK = object :
                DiffUtil.ItemCallback<HistoryDatabase.Transaction>() {
                override fun areItemsTheSame(
                    oldTransaction: HistoryDatabase.Transaction,
                    newTransaction: HistoryDatabase.Transaction
                ): Boolean =
                    oldTransaction.onlineID == newTransaction.onlineID

                override fun areContentsTheSame(
                    oldTransaction: HistoryDatabase.Transaction,
                    newTransaction: HistoryDatabase.Transaction
                ): Boolean =
                    oldTransaction == newTransaction
            }
        }
    }

    lateinit var binding: ActivityHistoryBinding
    var loginCallback: ((loginCredentials: OnlineCardClient.LoginCredentials) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        syncDatabase()
        binding.rwHistory.adapter = Adapter().also { adapter ->
            Pager(
                PagingConfig(10),
                this.initialLoadKey,
                HistoryDatabase.dao.getAllTransactions().asPagingSourceFactory(Dispatchers.IO)
            ).liveData.build()
                .observe(this) {
                    adapter.submitData(lifecycle, PagingData.from(it.snapshot()))
                }
        }

        binding.rwHistory.layoutManager = LinearLayoutManager(this)

        binding.srHistory.setOnRefreshListener {
            syncDatabase()
        }
    }

    private fun syncDatabase() {
        binding.srHistory.isRefreshing = true
        HistoryManager.syncDatabase(object : OnlineCardClient.ResponseCallback<Boolean> {
            override fun onSuccess(response: Boolean) {
                binding.srHistory.isRefreshing = false
                closeLoginDialog()
            }

            override fun onCredentialsRequired(cb: (loginCredentials: OnlineCardClient.LoginCredentials) -> Unit) {
                binding.srHistory.isRefreshing = false
                showLoginDialog(PreferencesManager.cardNumber)
                loginCallback = cb
            }

            override fun onFailure(t: Throwable) {
                binding.srHistory.isRefreshing = false
                Snackbar.make(binding.historyCoordinatorLayout, "Error loading new Transactions", Snackbar.LENGTH_LONG)
                    .show()
            }
        })
    }

    fun setupChart(transactions: PagedList<HistoryDatabase.Transaction>) {
        binding.lcHistory.setTouchEnabled(true)
        binding.lcHistory.isDragEnabled = true
        binding.lcHistory.setViewPortOffsets(0f, 0f, 0f, 0f)
        binding.lcHistory.setVisibleXRangeMaximum(TimeUnit.DAYS.toSeconds(30).toFloat())
        binding.lcHistory.setVisibleXRangeMinimum(TimeUnit.DAYS.toSeconds(30).toFloat())
        binding.lcHistory.description.isEnabled = false
        binding.lcHistory.extraBottomOffset = 80f

        val xAxis = binding.lcHistory.xAxis
        xAxis.position = XAxis.XAxisPosition.TOP_INSIDE
        xAxis.textSize = 10f
        xAxis.textColor = Color.WHITE
        xAxis.setDrawAxisLine(false)
        xAxis.setDrawGridLines(true)
        xAxis.textColor = Color.rgb(255, 192, 56)
        xAxis.setCenterAxisLabels(true)
        xAxis.granularity = 1f // one hour
        xAxis.valueFormatter = object : ValueFormatter() {

            private val mFormat = SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH)

            override fun getFormattedValue(value: Float): String {

                val millis = TimeUnit.HOURS.toMillis(value.toLong())
                return mFormat.format(Date(millis))
            }
        }

        xAxis.isEnabled = false

        val leftAxis = binding.lcHistory.axisLeft
        leftAxis.isEnabled = false
        /* leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
         leftAxis.setTextColor(ColorTemplate.getHoloBlue())
         leftAxis.setDrawGridLines(true)
         leftAxis.setGranularityEnabled(true)
         leftAxis.setAxisMinimum(0f)
         leftAxis.setAxisMaximum(170f)
         leftAxis.setYOffset(-9f)
         leftAxis.setTextColor(Color.rgb(255, 192, 56))*/

        val rightAxis = binding.lcHistory.axisRight
        rightAxis.isEnabled = false

        var current = 0f
        println(transactions.take(45).reversed()
            .filter { it != null })

        val values = transactions.take(50)
            .reversed()
            .filter { it != null }
            .map {
                if (it.credit == null) {
                    current += it.amount!!
                    current = Math.max(current, 0f)
                } else {
                    current = it.credit!!
                }; Entry((it.date.time / 10000).toFloat(), current)
            }

        println(values)

        // create a dataset and give it a type
        val set1 = LineDataSet(values, null)
        set1.mode = LineDataSet.Mode.STEPPED
        set1.cubicIntensity = 0.2f
        set1.setDrawFilled(true)
        set1.setDrawCircles(false)
        set1.lineWidth = 2f
        set1.circleRadius = 4f
        set1.color = ContextCompat.getColor(this, R.color.colorLineChart)
        set1.fillDrawable = ContextCompat.getDrawable(this, R.drawable.chart_fade)

        set1.setDrawHorizontalHighlightIndicator(false)

        // create a data object with the data sets
        val data = LineData(set1)
        data.setDrawValues(false)

        // set data
        binding.lcHistory.data = data
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(intentFor<SettingsActivity>())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onLoginAction(credentials: OnlineCardClient.LoginCredentials) {
        loginCallback?.invoke(credentials)
    }

}

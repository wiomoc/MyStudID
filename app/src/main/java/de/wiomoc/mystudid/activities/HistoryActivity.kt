package de.wiomoc.mystudid.activities

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import de.wiomoc.mystudid.R
import de.wiomoc.mystudid.database.HistoryDatabase
import de.wiomoc.mystudid.fragments.LoginDialogFragment
import de.wiomoc.mystudid.fragments.LoginDialogFragment.Companion.showLoginDialog
import de.wiomoc.mystudid.services.HistoryManager
import de.wiomoc.mystudid.services.OnlineCardClient
import kotlinx.android.synthetic.main.activity_history.*
import org.jetbrains.anko.find
import org.jetbrains.anko.intentFor

class HistoryActivity : AppCompatActivity(), LoginDialogFragment.LoginDialogCallback {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class Adapter : PagedListAdapter<HistoryDatabase.Transaction, ViewHolder>(DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            getItem(position).let {
                holder.itemView.apply {
                    find<TextView>(R.id.tv_transaction_amount).text = "" + it?.amount
                    find<TextView>(R.id.tv_transaction_date).text = it?.date.toString()
                    find<TextView>(R.id.tv_transaction_location).text = it?.location
                }
            }
        }

        companion object {
            private val DIFF_CALLBACK = object :
                    DiffUtil.ItemCallback<HistoryDatabase.Transaction>() {
                override fun areItemsTheSame(oldTransaction: HistoryDatabase.Transaction,
                                             newTransaction: HistoryDatabase.Transaction): Boolean =
                        oldTransaction.onlineID == newTransaction.onlineID

                override fun areContentsTheSame(oldTransaction: HistoryDatabase.Transaction,
                                                newTransaction: HistoryDatabase.Transaction): Boolean =
                        oldTransaction == newTransaction
            }
        }
    }

    var loginCallback: ((loginCredentials: OnlineCardClient.LoginCredentials) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        syncDatabase()
        rw_history.adapter = Adapter().also { adapter ->
            LivePagedListBuilder(HistoryDatabase.dao.getAllTransactions(), 10).build()
                    .observe(this, androidx.lifecycle.Observer {
                        adapter.submitList(it)
                    })
        }

        rw_history.layoutManager = LinearLayoutManager(this)
        sr_history.setOnRefreshListener {
            syncDatabase()
        }

    }

    private fun syncDatabase() {
        sr_history.isRefreshing = true
        HistoryManager.syncDatabase(object : OnlineCardClient.ResponseCallback<Boolean> {
            override fun onSuccess(response: Boolean) {
                sr_history.isRefreshing = false
            }

            override fun onCredentialsRequired(cb: (loginCredentials: OnlineCardClient.LoginCredentials) -> Unit) {
                sr_history.isRefreshing = false
                showLoginDialog()
                loginCallback = cb
            }

            override fun onFailure(t: Throwable) {
                sr_history.isRefreshing = false
                Snackbar.make(history_coordinator_layout, "Error loading new Transactions", Snackbar.LENGTH_LONG).show()
            }

        })
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

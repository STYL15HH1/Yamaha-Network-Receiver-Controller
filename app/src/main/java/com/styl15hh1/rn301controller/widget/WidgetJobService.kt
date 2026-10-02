package com.styl15hh1.rn301controller.widget

import android.app.job.*
import android.content.*
import com.styl15hh1.rn301controller.data.repository.ReceiverRuntime
import kotlinx.coroutines.*

/** Finite user-requested work. No periodic jobs and no automatic command retries. */
class WidgetJobService : JobService() {
    private var work: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        work = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            try {
                while (true) {
                    val item = params.dequeueWork() ?: break
                    try { execute(item.intent) }
                    finally { params.completeWork(item) }
                }
            } finally { jobFinished(params, false) }
        }
        return true
    }
    private suspend fun execute(intent: Intent) {
        val action = WidgetAction.entries.firstOrNull { it.name == intent.action } ?: return
        val runtime = ReceiverRuntime.get(this)
        val repo = runtime.repository
        // Consume before dispatch: a killed process must not replay a power toggle.
        val token = intent.getStringExtra("request") ?: return
        val consumed = getSharedPreferences("widget_actions", MODE_PRIVATE)
        val previous = consumed.getString("consumed", "").orEmpty().split(',')
        if (token in previous) return
        val editor = consumed.edit()
        editor.putString("consumed", (previous.takeLast(31) + token).joinToString(","))
        // Require durable storage before a non-idempotent action; KTX edit does not return commit success.
        if (!withContext(Dispatchers.IO) { editor.commit() }) return
        if (System.currentTimeMillis() - intent.getLongExtra("created", 0) > 60_000) return
        WidgetActions.execute(repo, runtime.settings, action, intent.getStringExtra("source"))
        ReceiverWidget.capture(this, com.styl15hh1.rn301controller.widget.WidgetSnapshot(repo.status.value, repo.tuner.value, repo.player.value, runtime.settings.state.value))
        ReceiverWidget.updateAll(this)
    }
    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return false // Never replay an interrupted toggle or source command.
    }
    companion object {
        fun enqueue(context: Context, intent: Intent) {
            val job = JobInfo.Builder(1200, ComponentName(context, WidgetJobService::class.java))
                .setOverrideDeadline(0).build()
            context.getSystemService(JobScheduler::class.java).enqueue(job, JobWorkItem(Intent(intent).putExtra("request", java.util.UUID.randomUUID().toString()).putExtra("created", System.currentTimeMillis())))
        }
    }
}

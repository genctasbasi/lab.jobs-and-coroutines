package com.escmobile.lab.jobsandcoroutines

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

const val JOB_TIME = 5000
const val PROGRESS_MIN: Int = 0
const val PROGRESS_MAX: Int = JOB_TIME

class MainActivity : AppCompatActivity() {

    lateinit var job1: CompletableJob

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initJob()
    }

    private fun initViews() {
        progressBar.min = PROGRESS_MIN
        progressBar.max = PROGRESS_MAX
        progressBar.progress = PROGRESS_MIN

        buttonStart.setOnClickListener {

            GlobalScope.launch {
                doJob1()
                log(jobState())
            }
        }

        buttonCancel.setOnClickListener {
            log(jobState())
            job1.cancel(CancellationException("Cancelled by the user."))
        }
    }

    private fun initJob() {

        job1 = Job()

        job1.invokeOnCompletion {
            log("Job1 completed. ${jobState()}. Exception: ${it?.message ?: "None"}")
        }
    }

    private suspend fun doJob1() {

        CoroutineScope(Dispatchers.IO + job1).launch {

            for (progress in 0..JOB_TIME) {
                delay(1)
                withContext(Dispatchers.Main) {
                    progressBar.progress = progress
                }
            }

            job1.complete()
        }
    }

    private fun Job.state(): String = when {
        isActive -> "Active / Completing"
        isCompleted && isCancelled -> "Cancelled"
        isCancelled -> "Cancelling"
        isCompleted -> "Completed"
        else -> "New"
    }

    private fun jobState() = "Job state: ${job1.state()}"

    private fun log(message: String) {
        GlobalScope.launch(Dispatchers.Main) {
            logView.text = "${logView.text}\n$message"
        }
    }
}
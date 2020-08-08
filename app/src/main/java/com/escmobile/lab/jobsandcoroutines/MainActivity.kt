package com.escmobile.lab.jobsandcoroutines

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

const val JOB_TIME = 5000
const val PROGRESS_MIN: Int = 0
const val PROGRESS_MAX: Int = JOB_TIME
const val TAG = "J&C"

class MainActivity : AppCompatActivity() {

    lateinit var job1: CompletableJob

    private lateinit var parentJob: Job

    private val childJobs = mutableListOf<Job>()

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

        buttonStartSeveral.setOnClickListener {

            // if initialized as supervisor, then cancelling a child job won't affect
            // the rest of the children or the parent job.
            // if initialized as a normal job, then cancelling a child job will cancel all children
            // and the parent
            parentJob = SupervisorJob() // or try parentJob = Job()

            val coroutineScope = CoroutineScope(Dispatchers.Default + parentJob + exceptionHandler)

            for (index in 0..99) {

                val childJob = coroutineScope.launch {
                    // val childJob = Job(parentJob)
                    someCPUHeavyWork(index)
                }

                childJobs.add(childJob)
            }

            Log.d(TAG, "Completed creating child jobs: ${parentJob.children.count()} child jobs.")
        }


        cancelAChild.setOnClickListener {
            Log.d(TAG, "Cancelling the job #10")
            childJobs[10].cancel("Only the 10th job will be cancelled, siblings and the parent should be fine.")
        }
    }

    private fun initJob() {

        job1 = Job()

        job1.invokeOnCompletion {
            log("Job1 completed. ${jobState()}. Exception: ${it?.message ?: "None"}")
        }

    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "Exception : $throwable")
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

    private suspend fun someCPUHeavyWork(workNo: Int) {

        // Fake an exception in coroutine #25
        // If our job is SupervisorJob(), then only the 25th job will be incomplete.
        // If our job is just Job(), then all of the siblings / children should cancel.
        if (workNo == 25) throw java.lang.IllegalArgumentException()
        delay(2000)
        Log.d(TAG, "Work $workNo is done on ${Thread.currentThread().name}")
    }

    private fun Job.state(): String = when {
        isActive -> "Active / Completing"
        isCompleted && isCancelled -> "Cancelled"
        isCancelled -> "Cancelling"
        isCompleted -> "Completed"
        else -> "New"
    }

    private fun jobState(job: Job? = null) = "Job state: ${(job ?: job1).state()}"

    private fun log(message: String) {
        GlobalScope.launch(Dispatchers.Main) {
            logView.text = "${logView.text}\n$message"
        }
    }
}
package moe.matthew.homesnap;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/**
 * Restarts this app so it can reconnect the helper. Shizuku sends its binder when this process
 * starts or changes state, but its own wake-up at server start did not reach this app after a reboot.
 */
public final class HelperCheckJob extends JobService {
    private static final int JOB_ID = 1;
    private static final long DEADLINE_SLACK_MS = 30_000;

    static void schedule(Context context, long delayMs) {
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(context, HelperCheckJob.class))
                .setMinimumLatency(delayMs)
                // Without a deadline the system's flexibility rule can hold this back for much longer.
                .setOverrideDeadline(delayMs + DEADLINE_SLACK_MS)
                .setPersisted(true) // A fallback if ColorOS blocks the boot broadcast.
                .build();
        context.getSystemService(JobScheduler.class).schedule(job);
    }

    static void cancel(Context context) {
        context.getSystemService(JobScheduler.class).cancel(JOB_ID);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        ((App) getApplication()).sync();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}

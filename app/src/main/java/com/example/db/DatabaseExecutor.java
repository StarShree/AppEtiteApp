package com.example.db;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous executor utility for database operations.
 * Executes JDBC queries off the main UI thread via ExecutorService,
 * safely delivering results or exceptions to the Android main thread Handler
 * to prevent NetworkOnMainThreadException and ANR.
 */
public class DatabaseExecutor {
    private static final String TAG = "DatabaseExecutor";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Executes a callable database task off the UI thread and dispatches result to main thread.
     */
    public static <T> void execute(Callable<T> task, Callback<T> callback) {
        executorService.execute(() -> {
            try {
                T result = task.call();
                mainThreadHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Database operation failed: " + e.getMessage(), e);
                mainThreadHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
    }

    /**
     * Executes a fire-and-forget task in background.
     */
    public static void execute(Runnable task) {
        executorService.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                Log.e(TAG, "Background task execution error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Post a runnable directly to the main thread.
     */
    public static void runOnUiThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainThreadHandler.post(runnable);
        }
    }
}

package com.example.gymclient.util;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Wykonuje zapytania do API w tle, żeby nie blokować interfejsu. */
public final class Async {

    private Async() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> Dialogs.error(task.getException()));
        Thread thread = new Thread(task, "api-call");
        thread.setDaemon(true);
        thread.start();
    }

    /** Wariant bez wyniku. */
    public static void run(ThrowingRunnable work, Runnable onSuccess) {
        run(() -> {
            work.run();
            return null;
        }, ignored -> onSuccess.run());
    }

    public static void later(Runnable r) {
        Platform.runLater(r);
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}

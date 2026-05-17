package net.sleepy;

import net.sleepy.jobs.DateZipJob;
import net.sleepy.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Test4 {
    private static final Logger log = LoggerFactory.getLogger(Test4.class);

    static {
        System.loadLibrary("price_stat");
    }


    public static void main (String[] args) throws IOException, InterruptedException {

        var list = DateUtils.getDatesInRange(
                LocalDate.of(2025, 11, 1),
                LocalDate.now()
        );

        for (int i = 0; i < list.size(); i += 2) {
            list.set(i, null);
        }

        Semaphore connectionSemaphore = new Semaphore(5);
        Semaphore rustSemaphore = new Semaphore(2);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            list.forEach(date -> executor.submit(() -> {

                if (date == null)
                    return;

                try {
                    rustSemaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                try {
                    new DateZipJob(date, connectionSemaphore).run();
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                } finally {
                    rustSemaphore.release();
                }
            }));
        }
    }
}
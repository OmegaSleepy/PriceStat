package net.sleepy.jobs;

import net.sleepy.RustFFM;
import net.sleepy.net.NetworkingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import java.util.concurrent.Semaphore;

public class DateZipJob {
    private final String date;
    private final Semaphore semaphore; // Add this
    private final String BASEURL = "https://kolkostruva.bg/opendata_files/%s.zip";
    private final Logger log = LoggerFactory.getLogger(DateZipJob.class);

    // Update constructor
    public DateZipJob (String date, Semaphore semaphore) {
        this.date = date;
        this.semaphore = semaphore;
    }

    public void run () throws InterruptedException {
        Path path = Path.of("download/zip/" + date + ".zip");

        if (!Files.exists(path)) {
            byte[] rawZip = null;

            // Acquire a permit before hitting the network
            semaphore.acquire();
            try {
                rawZip = NetworkingClient.get(String.format(BASEURL, date)).orElseThrow();
                Files.createDirectories(path.getParent());
                Files.write(path, rawZip);
            } catch (IOException e) {
                log.error(e.getMessage());
                return;
            } finally {
                // ALWAYS release the permit in a finally block
                semaphore.release();
            }
        }

        var start = Instant.now();
        RustFFM.parseZip(path);
        var end = Instant.now();
        log.info("Took {}ms in rust", (Duration.between(start, end).toMillis()));
    }
}


package net.sleepy.jobs;

import net.sleepy.RustFFM;
import net.sleepy.net.NetworkingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DateZipJob {

    private final String date;
    private final String BASEURL = "https://kolkostruva.bg/opendata_files/%s.zip";
    private final Logger log = LoggerFactory.getLogger(DateZipJob.class);

    public DateZipJob (String date) {
        this.date = date;
    }

    public void run () throws IOException, InterruptedException {
        Path path = Path.of("download/zip/" + date + ".zip");

        if (!Files.exists(path)) {
            var rawZip = NetworkingClient.get(String.format(BASEURL, date));
            Files.createDirectories(path.getParent());
            Files.write(path, rawZip);
        }

        RustFFM.parseZip(path);

    }

    public String getDate () {
        return date;
    }
}


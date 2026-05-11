package net.sleepy.main.java.jobs;

import net.sleepy.main.java.io.csv.CsvPipeline;
import net.sleepy.main.java.io.csv.Normalization;
import net.sleepy.main.java.io.sql.CopyInserter;
import net.sleepy.main.java.io.sql.DataBaseUtil;
import net.sleepy.main.java.io.zip.ZipReader;
import net.sleepy.main.java.net.NetworkingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public class DownloadDateAndInsert {

    private final String date;
    private final String BASEURL = "https://kolkostruva.bg/opendata_files/%s.zip";
    private final Logger log = LoggerFactory.getLogger(DownloadDateAndInsert.class);

    public DownloadDateAndInsert (String date) {
        this.date = date;
    }

    public void run () throws IOException, InterruptedException {
        Path path = Path.of("download/zip/" + date + ".zip");

        if (!Files.exists(path)) {
            var rawZip = NetworkingClient.get(String.format(BASEURL, date));
            Files.createDirectories(path.getParent());
            Files.write(path, rawZip);
        }

        File file = new File(path.toString());
        ZipReader zipReader = new ZipReader(file);
        log.info("Loading {}", zipReader);

        try (Connection conn = DataBaseUtil.getConnection()) {

            CopyInserter inserter = new CopyInserter(conn, date);

            zipReader.getZipAsNameContentsMap().forEach((firmName, content) -> {

                String firm = Normalization.normBussName(firmName);

                CsvPipeline.process(content, firm, inserter);
            });

            inserter.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDate () {
        return date;
    }
}


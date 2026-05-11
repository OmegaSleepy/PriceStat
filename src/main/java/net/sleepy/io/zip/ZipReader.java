package net.sleepy.io.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipReader {

    private final File file;

    public ZipReader (File file) {
        this.file = file;
    }

    public Map<String, String> getZipAsNameContentsMap() {

        Map<String, String> result = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(file);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                result.put(entry.getName(), new String(zis.readAllBytes()));

                entry = zis.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public List<ZipEntry> getZipEntries() {
        List<ZipEntry> result = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                result.add(entry);
                entry = zis.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }



}

package org.akvo.flow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

public class TempTestFileFactory {

    private Queue<File> files = new ArrayDeque<>();

    public File generateTempFile() throws IOException {
        File tempFile = File.createTempFile("temp_", ".txt");
        files.add(tempFile);
        return tempFile;
    }

    public void deleteTempFiles() {
        for (File tempFile : files) {
            tempFile.delete();
        }
    }
}
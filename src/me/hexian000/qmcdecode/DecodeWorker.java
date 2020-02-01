package me.hexian000.qmcdecode;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CancellationException;

public class DecodeWorker extends SwingWorker<String, Void> {
    public String getInput() {
        return input;
    }

    private String input, output;
    private long length;

    public DecodeWorker(String input, String output) {
        this.input = input;
        this.output = output;
        length = new File(input).length();
    }

    private void decode() throws IOException {
        setProgress(0);
        long pos = 0;

        try (var in = new QmcInputStream(new FileInputStream(input));
             var out = new FileOutputStream(output)) {
            var buffer = new byte[65536];
            for (var read = in.read(buffer); read != -1; read = in.read(buffer)) {
                out.write(buffer, 0, read);
                pos += read;
                setProgress((int) (pos * 100 / length));
                if (isCancelled()) {
                    throw new CancellationException();
                }
            }
        }
        setProgress(100);
    }

    private void cleanup() {
        try {
            var outFile = new File(output);
            //noinspection ResultOfMethodCallIgnored
            outFile.delete();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected String doInBackground() {
        try {
            decode();
        } catch (Exception e) {
            e.printStackTrace();
            cleanup();
        }
        return input;
    }
}

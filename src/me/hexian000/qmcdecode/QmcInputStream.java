package me.hexian000.qmcdecode;

import java.io.IOException;
import java.io.InputStream;

public class QmcInputStream extends InputStream {
    private InputStream base;
    private KeyGenerator key;

    public QmcInputStream(InputStream base) {
        this.base = base;
        key = new KeyGenerator();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = base.read(b, off, len);
        if (read <= 0) {
            return read;
        }
        for (int i = 0; i < read; i++) {
            b[off + i] ^= key.next();
        }
        return read;
    }

    @Override
    public int read() throws IOException {
        var data = base.read();
        if (data == -1) {
            return data;
        }
        return data ^ key.next();
    }

    @Override
    public void close() throws IOException {
        base.close();
    }
}

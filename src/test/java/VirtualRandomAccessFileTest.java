import exceptions.*;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

class VirtualRandomAccessFileTest {
    private String fileName = "test_file";

    @Rule
    public TemporaryFolder folder = TemporaryFolder.builder().assureDeletion().build();

    @Test
    void read() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        OutputStream out = new FileOutputStream(sourceFile);

        byte[] data = new byte[] {
                0, 0, 0, 0, 0, 0, 0, 32, // Размер блока = 32
                0, 0, 0, 0, 0, 0, 0, (byte) 0x2f,
                -1, -1, -1, -1, -1, -1, -1, -1, // Конец данных
                0, 0, 0, 0, 0, 0, 0, 8, // Размер даных
                0, 0, 0, 0, 0, 0, 0, 8, // Последний блок
                1, 2, 3, 4, 5, 6, 7, 8,
                0, 0, 0, 0, 0, 0, 0, 0
        };

        byte[] content = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        byte[] readContent = new byte[content.length];

        out.write(data);
        out.close();

        VirtualRandomAccessFile randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw", 8);
        randomAccessFile.read(readContent);

        assertArrayEquals(content, readContent);
    }

    @Test
    void write() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");

        String content = "hello";

        randomAccessFile.write(content.getBytes());
        randomAccessFile.close();
        long startPosition = randomAccessFile.getFirstBlockPosition();

        randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw", startPosition);

        assertEquals(content, randomAccessFile.readLine());
    }
}
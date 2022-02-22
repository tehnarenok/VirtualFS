import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

class VirtualRandomAccessFileTest {
    private final String fileName = "test_file";

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
                0, 0, 0, 0, 0, 0, 0, 8, // Размер данных
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

    @Test
    void length() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");

        String content = "hello";

        randomAccessFile.write(content.getBytes());
        randomAccessFile.flush();

        assertEquals(content.getBytes().length, randomAccessFile.length());
    }

    @Test
    void setLengthMore() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");

        String content = "hello";

        randomAccessFile.write(content.getBytes());
        randomAccessFile.flush();

        assertEquals(content.getBytes().length, randomAccessFile.length());

        randomAccessFile.setLength(content.getBytes().length + 10);

        assertEquals(content.getBytes().length + 10, randomAccessFile.length());
    }

    @Test
    void setLengthLess() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");

        String content = "hello";

        randomAccessFile.write(content.getBytes());
        randomAccessFile.flush();

        assertEquals(content.getBytes().length, randomAccessFile.length());

        randomAccessFile.setLength(content.getBytes().length - 2);

        assertEquals(content.getBytes().length - 2, randomAccessFile.length());
    }

    @Test
    void seek() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");

        String content = "hello";
        String newContent = "llo";

        randomAccessFile.write(content.getBytes());
        randomAccessFile.flush();

        randomAccessFile.seek(2);

        assertEquals(2, randomAccessFile.getFilePointer());
        assertEquals(newContent, randomAccessFile.readLine());
    }

    @Test
    void seekLength() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");

        String content = "hello";

        randomAccessFile.write(content.getBytes());
        randomAccessFile.flush();

        randomAccessFile.seek(randomAccessFile.length());
        assertEquals(randomAccessFile.length(), randomAccessFile.getFilePointer());
    }

    @Test
    void seekNegative() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");

        String content = "hello";

        randomAccessFile.write(content.getBytes());
        randomAccessFile.flush();

        assertThrows(IOException.class, () -> randomAccessFile.seek(-1));
    }

    @Test
    void seekMoreThanLength() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");

        String content = "hello";

        randomAccessFile.write(content.getBytes());
        randomAccessFile.flush();

        assertDoesNotThrow(() -> randomAccessFile.seek(randomAccessFile.length() + 10));
        assertEquals(randomAccessFile.length(), randomAccessFile.getFilePointer());
    }

    @Test
    void rewrite() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");

        String content = "hello";
        String newContent = "my first program";

        randomAccessFile.write(content.getBytes());
        randomAccessFile.flush();

        randomAccessFile.seek(0);
        randomAccessFile.write(newContent.getBytes());
        randomAccessFile.flush();

        randomAccessFile.seek(0);

        assertEquals(newContent, randomAccessFile.readLine());
    }

    @Test
    void writeTwoFiles() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile_1 = new VirtualRandomAccessFile(sourceFile, "rw");
        VirtualRandomAccessFile randomAccessFile_2 = new VirtualRandomAccessFile(sourceFile, "rw");

        String content_1 = "hello";
        String content_2 = "))))))";

        randomAccessFile_1.write(content_1.getBytes());
        randomAccessFile_2.write(content_2.getBytes());

        randomAccessFile_1.close();
        randomAccessFile_2.close();

        long position_1 = randomAccessFile_1.getFirstBlockPosition();
        long position_2 = randomAccessFile_2.getFirstBlockPosition();

        randomAccessFile_1 = new VirtualRandomAccessFile(sourceFile, "r", position_1);
        randomAccessFile_2 = new VirtualRandomAccessFile(sourceFile, "r", position_2);

        assertEquals(content_1, randomAccessFile_1.readLine());
        assertEquals(content_2, randomAccessFile_2.readLine());
    }

    @Test
    void deleteFile() throws IOException {
        folder.create();
        File sourceFile = folder.newFile(fileName);

        VirtualRandomAccessFile randomAccessFile_1 = new VirtualRandomAccessFile(sourceFile, "rw");
        VirtualRandomAccessFile randomAccessFile_2 = new VirtualRandomAccessFile(sourceFile, "rw");

        String content_1 = "hello";
        String content_2 = "))))))";

        randomAccessFile_1.write(content_1.getBytes());

        randomAccessFile_1.close();

        long position_1 = randomAccessFile_1.getFirstBlockPosition();

        randomAccessFile_1 = new VirtualRandomAccessFile(sourceFile, "rw", position_1);
        randomAccessFile_1.setLength(0);

        assertEquals(-1, randomAccessFile_1.getFirstBlockPosition());

        randomAccessFile_2.write(content_2.getBytes());
        randomAccessFile_2.close();

        long position_2 = randomAccessFile_2.getFirstBlockPosition();

        randomAccessFile_2 = new VirtualRandomAccessFile(sourceFile, "r", position_2);

        assertEquals(content_2, randomAccessFile_2.readLine());
        assertEquals(position_1, position_2);
    }
}
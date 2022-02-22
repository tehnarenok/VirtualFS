import exceptions.*;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class VirtualFileTest {
    final String name = "test_name";
    final String newName = "name_test";

    @Rule
    public TemporaryFolder folder = TemporaryFolder.builder().assureDeletion().build();

    private VirtualFS virtualFS;
    private File sourceFile;

    @BeforeEach
    public void setup() throws IOException, ClassNotFoundException {
        folder.create();
        sourceFile = folder.newFile(name);
        virtualFS = new VirtualFS(sourceFile);
    }

    @Test
    void createFile() {
        VirtualFile virtualFile = new VirtualFile(name);
        Date createdAt = new Date();

        assertEquals(name, virtualFile.getName());

        assertEquals(createdAt, virtualFile.getCreatedAt());

        assertEquals(createdAt, virtualFile.getModifiedAt());
    }

    @Test
    void renameFile() throws InterruptedException, VFSException {
        VirtualFile virtualFile;
        virtualFile = new VirtualFile(name);

        Thread.sleep(1000);

        virtualFile.rename(newName);
        Date modifiedAt = new Date();

        assertEquals(
                newName,
                virtualFile.getName()
        );

        assertEquals(
                modifiedAt,
                virtualFile.getModifiedAt()
        );
    }

    @Test
    void uniqueName() throws VFSException {
        virtualFS.touch(name);

        assertThrows(NotUniqueName.class, () -> virtualFS.touch(name));
        assertDoesNotThrow(() -> virtualFS.touch(name + name));
    }

    @Test
    void uniqueNameRename() throws VFSException {
        String name_1 = "name_1";
        String name_2 = "name_2";
        String name_3 = "name_3";
        VirtualFile virtualFile = virtualFS.touch(name_1);
        virtualFS.touch(name_2);

        assertThrows(NotUniqueName.class, () -> virtualFile.rename(name_2));
        assertDoesNotThrow(() -> virtualFile.rename(name_1));
        assertDoesNotThrow(() -> virtualFile.rename(name_3));
    }

    @Test
    void modifyContent() throws InterruptedException, IOException, VFSException {
        VirtualFile virtualFile = virtualFS.touch(name);
        Date modifiedAt = virtualFile.getModifiedAt();

        Thread.sleep(1000);

        VirtualRandomAccessFile randomAccessFile = virtualFile.open("rw");
        randomAccessFile.write("Hello".getBytes());
        randomAccessFile.close();

        assertNotEquals(
                modifiedAt,
                virtualFile.getModifiedAt()
        );
    }

    @Test
    void removeWithNullRootDirectory() {
        VirtualFile virtualFile = new VirtualFile(name);

        UnremovableVirtualNode exception = assertThrows(UnremovableVirtualNode.class, virtualFile::remove);

        assertEquals(
                "This node cannot be deleted",
                exception.getMessage()
        );
    }

    @Test
    void removeSelf() throws VFSException {
        VirtualFile virtualFile = virtualFS.touch(name);

        assertDoesNotThrow(() -> virtualFile.remove());

        assertArrayEquals(
                new VirtualFile[]{},
                virtualFS.getFiles().toArray()
        );

        assertNull(virtualFile.getRootDirectory());
    }

    @Test
    void remove() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualFile virtualFile = rootDirectory.touch(name);

        assertDoesNotThrow(() -> rootDirectory.remove(virtualFile));

        assertArrayEquals(
                new VirtualFile[]{},
                rootDirectory.getFiles().toArray()
        );

        assertNull(virtualFile.getRootDirectory());
    }

    @Test
    void removeDeleted() throws VFSException {
        VirtualFile virtualFile = virtualFS.touch(name);

        assertDoesNotThrow(() -> virtualFile.remove());

        assertThrows(VirtualFSNodeIsDeleted.class, virtualFile::remove);
    }

    @Test
    void move() throws VFSException {
        VirtualDirectory rootDirectory = virtualFS.getRootDirectory();
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualFile virtualFile = rootDirectory.touch(name);

        virtualFile.move(destinationDirectory);

        assertArrayEquals(
                new VirtualFile[]{},
                rootDirectory.getFiles().toArray()
        );

        assertArrayEquals(
                new VirtualFile[]{virtualFile},
                destinationDirectory.getFiles().toArray()
        );

        assertEquals(destinationDirectory, virtualFile.getRootDirectory());
    }

    @Test
    void copy() throws VFSException, IOException {
        VirtualDirectory rootDirectory = virtualFS.getRootDirectory();
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualFile virtualFile = rootDirectory.touch(name);

        VirtualFile copiedVirtualFile = virtualFile.copy(destinationDirectory);
        Date createdAtCopy = new Date();

        assertArrayEquals(
                new VirtualFile[]{virtualFile},
                rootDirectory.getFiles().toArray()
        );

        assertArrayEquals(
                new VirtualFile[]{copiedVirtualFile},
                destinationDirectory.getFiles().toArray()
        );

        assertEquals(destinationDirectory, copiedVirtualFile.getRootDirectory());
        assertEquals(rootDirectory, virtualFile.getRootDirectory());

        assertEquals(
                virtualFile.getName(),
                copiedVirtualFile.getName()
        );

        assertEquals(
                createdAtCopy.toString(),
                copiedVirtualFile.getCreatedAt().toString()
        );

        assertNotEquals(virtualFile, copiedVirtualFile);
    }

    @Test
    void copyWithContent() throws VFSException, IOException {
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        byte[] content;
        content = "Hello".getBytes();

        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualRandomAccessFile randomAccessFile = virtualFile.open("rw");
        randomAccessFile.write(content);
        randomAccessFile.close();

        VirtualFile copiedVirtualFile = virtualFile.copy(destinationDirectory);

        randomAccessFile = copiedVirtualFile.open("r");
        byte[] copiedContent = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(copiedContent);
        randomAccessFile.close();

        assertArrayEquals(
                content,
                copiedContent
        );

        randomAccessFile = virtualFile.open("rw");
        randomAccessFile.write(content);
        randomAccessFile.write(content);
        randomAccessFile.close();

        randomAccessFile = copiedVirtualFile.open("r");
        copiedContent = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(copiedContent);
        randomAccessFile.close();

        assertArrayEquals(
                content,
                copiedContent
        );
    }

    @Test
    void copyDirectoryWithContent() throws VFSException, IOException {
        String destinationDirectoryName = "destinationDirectoryName";
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(destinationDirectoryName);

        byte[] content;
        content = "Hello world".getBytes();

        VirtualFile virtualFile = directory.touch(name);
        VirtualRandomAccessFile randomAccessFile = virtualFile.open("rw");
        randomAccessFile.write(content);
        randomAccessFile.close();

        VirtualDirectory copiedDirectory = directory.copy(destinationDirectory);

        assertEquals(
                1,
                copiedDirectory.getFiles().size()
        );

        VirtualFile copiedFile = copiedDirectory.find(name).next();
        randomAccessFile = copiedFile.open("r");
        byte[] copiedContent = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(copiedContent);
        randomAccessFile.close();

        assertArrayEquals(
                content,
                copiedContent
        );

        randomAccessFile = virtualFile.open("rw");
        randomAccessFile.write(content);
        randomAccessFile.write(content);
        randomAccessFile.close();

        randomAccessFile = copiedFile.open("r");
        copiedContent = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(copiedContent);
        randomAccessFile.close();

        assertArrayEquals(
                content,
                copiedContent
        );
    }

    @Test
    void testReadWrite() throws IOException, VFSException, ClassNotFoundException {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        String content = "hello world";

        VirtualFile file = virtualFS.touch(name);

        VirtualRandomAccessFile randomAccessFile = file.open("rw");
        randomAccessFile.write(content.getBytes());
        randomAccessFile.close();

        randomAccessFile = file.open("r");
        byte[] bytes = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(bytes);

        assertEquals(content, new String(bytes));
    }

    @Test
    void testReadWriteLong() throws IOException, VFSException {
        long content = 10;

        VirtualFile file = virtualFS.touch(name);

        VirtualRandomAccessFile randomAccessFile = file.open("rw");
        randomAccessFile.writeLong(10);
        randomAccessFile.close();

        randomAccessFile = file.open("r");
        long readContent = randomAccessFile.readLong();

        assertEquals(content, readContent);
    }

    @Test
    void testReadWrite_2() throws IOException, VFSException, ClassNotFoundException {
        String content = "hello world";

        VirtualFile file = virtualFS.touch(name);

        VirtualRandomAccessFile randomAccessFile = file.open("rw");
        randomAccessFile.write(content.getBytes());
        randomAccessFile.close();

        virtualFS.save();

        VirtualFS virtualFS_2 = new VirtualFS(sourceFile);
        VirtualFile file_1 = virtualFS_2.find(name).next();

        randomAccessFile = file_1.open("r");
        byte[] bytes = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(bytes);

        assertEquals(content, new String(bytes));
    }

    private static class RunnableWriter implements Runnable {
        public VirtualFile file;
        public String content;

        public RunnableWriter(VirtualFile file, String content) {
            this.file = file;
            this.content = content;
        }

        @Override
        public void run() {
            try {
                VirtualRandomAccessFile randomAccessFile = file.open("rw");
                randomAccessFile.write(content.getBytes());
                randomAccessFile.close();
            } catch (IOException | OverlappingVirtualFileLockException | NullVirtualFS | LockedVirtualFSNode e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    void testReadWriteParrarel() throws IOException, ClassNotFoundException, VFSException, InterruptedException {
        String name_1 = "name_1";
        String name_2 = "name_2";

        String content_1 = "hello world";
        String content_2 = "hello tehnarenok";

        VirtualFile file_1 = virtualFS.touch(name_1);
        VirtualFile file_2 = virtualFS.touch(name_2);

        VirtualRandomAccessFile randomAccessFile;

        Thread thread_0 = new Thread(new RunnableWriter(file_1, content_1));
        Thread thread_1 = new Thread(new RunnableWriter(file_2, content_2));

        thread_0.start();
        thread_1.start();

        thread_0.join();
        thread_1.join();

        virtualFS.save();

        VirtualFS virtualFS_2 = new VirtualFS(sourceFile);
        VirtualFile readedFile1 = virtualFS_2.find(name_1).next();
        VirtualFile readedFile2 = virtualFS_2.find(name_2).next();

        byte[] bytes;

        randomAccessFile = readedFile1.open("r");
        bytes = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(bytes);

        assertEquals(content_1, new String(bytes));

        randomAccessFile = readedFile2.open("r");
        bytes = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(bytes);

        assertEquals(content_2, new String(bytes));
    }

    @Test
    void testReadLock() throws VFSException, IOException {
        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        virtualFile.open("r");

        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.open("rw"));
        assertThrows(LockedVirtualFSNode.class, virtualFile::remove);
        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.move(destinationDirectory));

        assertDoesNotThrow(() -> virtualFile.copy(destinationDirectory));
        assertDoesNotThrow(() -> virtualFile.open("r"));
    }

    @Test
    void testWriteLock() throws VFSException, IOException {
        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        virtualFile.open("rw");

        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.open("rw"));
        assertThrows(LockedVirtualFSNode.class, virtualFile::remove);
        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.move(destinationDirectory));
        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.open("rw"));
        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.copy(destinationDirectory));
    }
}
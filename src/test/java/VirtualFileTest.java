import exceptions.*;
import org.junit.Rule;
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

    @Test
    void createFile() {
        VirtualFile virtualFile = new VirtualFile(name);
        Date createdAt = new Date();

        assertEquals(
                createdAt,
                virtualFile.getCreatedAt()
        );

        assertEquals(
                createdAt,
                virtualFile.getModifiedAt()
        );
    }

    @Test
    void renameFile() throws InterruptedException, LockedVirtualFSNode, VirtualFSNodeIsDeleted {
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
    void modifyContent() throws InterruptedException, IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

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
    void removeSelf() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);


        VirtualFile virtualFile = virtualFS.touch(name);

        assertDoesNotThrow(() -> virtualFile.remove());

        assertArrayEquals(
                new VirtualFile[]{},
                virtualFS.getFiles().toArray()
        );

        assertNull(virtualFile.getRootDirectory());
    }

    @Test
    void remove() throws LockedVirtualFSNode {
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
    void move() throws LockedVirtualFSNode, VirtualFSNodeIsDeleted {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory destinationDirectory = new VirtualDirectory(name);

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
    void copy() throws NullVirtualFS, LockedVirtualFSNode, OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeleted {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory destinationDirectory = new VirtualDirectory(name);

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
                createdAtCopy,
                copiedVirtualFile.getCreatedAt()
        );

        assertNotEquals(virtualFile, copiedVirtualFile);
    }

    @Test
    void copyWithContent() throws NullVirtualFS, LockedVirtualFSNode, OverlappingVirtualFileLockException, IOException, ClassNotFoundException, VirtualFSNodeIsDeleted {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        byte[] content;
        content = new byte[]{1, 2, 4};

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
    void copyDirectoryWithContent() throws NullVirtualFS, LockedVirtualFSNode, OverlappingVirtualFileLockException, IOException, ClassNotFoundException, VirtualFSNodeIsDeleted {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        byte[] content;
        content = new byte[]{1, 2, 4};

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

//    @Test
//    void read() throws IOException {
//        byte[] content;
//        content = new byte[]{1, 2, 4, 5, 6};
//
//        VirtualFile virtualFile = new VirtualFile(name, null, content);
//
//        InputStream inputStream = virtualFile.read();
//
//        byte[] readContent = new byte[3];
//        inputStream.read(readContent, 0, 3);
//
//        assertArrayEquals(
//                new byte[]{1, 2, 4},
//                readContent
//        );
//    }

    @Test
    void testReadWrite_1() throws IOException, ClassNotFoundException, OverlappingVirtualFileLockException, NullVirtualFS, LockedVirtualFSNode {
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
    void testReadWriteLong() throws IOException, ClassNotFoundException, OverlappingVirtualFileLockException, NullVirtualFS, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

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
    void testReadWrite_2() throws IOException, ClassNotFoundException, OverlappingVirtualFileLockException, NullVirtualFS, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

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

    private class RunnableWriter implements Runnable {
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
            } catch (IOException e) {
                e.printStackTrace();
            } catch (OverlappingVirtualFileLockException e) {
                e.printStackTrace();
            } catch (NullVirtualFS e) {
                e.printStackTrace();
            } catch (LockedVirtualFSNode e) {
                e.printStackTrace();
            }
        }
    };

    @Test
    void testReadWrite_3() throws IOException, ClassNotFoundException, OverlappingVirtualFileLockException, NullVirtualFS, InterruptedException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

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
        VirtualFile _file_1 = virtualFS_2.find(name_1).next();
        VirtualFile _file_2 = virtualFS_2.find(name_2).next();

        byte[] bytes;

        randomAccessFile = _file_1.open("r");
        bytes = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(bytes);

        assertEquals(content_1, new String(bytes));

        randomAccessFile = _file_2.open("r");
        bytes = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(bytes);

        assertEquals(content_2, new String(bytes));
    }
}
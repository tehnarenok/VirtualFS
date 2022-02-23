import exceptions.*;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class VirtualDirectoryTest {
    final String name = "test_name";

    @Rule
    public TemporaryFolder folder = TemporaryFolder.builder().assureDeletion().build();

    private VirtualFS virtualFS;

    @BeforeEach
    public void setup() throws VFSException, IOException, ClassNotFoundException {
        folder.create();
        File sourceFile = folder.newFile(name);
        virtualFS = new VirtualFS(sourceFile);
    }

    @Test
     void createDirectory() throws VFSException {
        VirtualDirectory directory = new VirtualDirectory(name);

        assertNull(directory.getRootDirectory());

        assertEquals(name, directory.getName());

        assertArrayEquals(
                new VirtualDirectory[]{},
                directory.getDirectories().toArray()
        );

        assertArrayEquals(
                new VirtualFile[]{},
                directory.getFiles().toArray()
        );
    }

    @Test
    void createDirectoryWithEmptyName() throws VFSException {
        assertThrows(EmptyNodeNameException.class, () -> new VirtualDirectory(""));
    }

    @Test
    void rename() throws VFSException {
        String newName = name + name;
        VirtualDirectory virtualDirectory = virtualFS.mkdir(name);
        virtualDirectory.rename(newName);

        assertEquals(newName, virtualDirectory.getName());
    }

    @Test
    void renameToEmpty() throws VFSException {
        VirtualDirectory virtualDirectory = virtualFS.mkdir(name);
        assertThrows(EmptyNodeNameException.class, () -> virtualDirectory.rename(""));
    }


    @Test
    void uniqueName() throws VFSException {
        virtualFS.mkdir(name);

        assertThrows(NotUniqueNameException.class, () -> virtualFS.mkdir(name));
        assertDoesNotThrow(() -> virtualFS.mkdir(name + name));
    }

    @Test
    void uniqueNameRename() throws VFSException {
        String name_1 = "name_1";
        String name_2 = "name_2";
        String name_3 = "name_3";
        VirtualDirectory virtualDirectory = virtualFS.mkdir(name_1);
        virtualFS.mkdir(name_2);

        assertThrows(NotUniqueNameException.class, () -> virtualDirectory.rename(name_2));
        assertDoesNotThrow(() -> virtualDirectory.rename(name_1));
        assertDoesNotThrow(() -> virtualDirectory.rename(name_3));
    }

    @Test
    void createDirectoryWithRoot() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory directory = new VirtualDirectory(name, rootDirectory);

        assertEquals(rootDirectory, directory.getRootDirectory());
    }

    @Test
    void mkdirAddingDirectoryToList() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory virtualDirectory_1 = rootDirectory.mkdir(name);

        assertArrayEquals(
                new VirtualDirectory[]{virtualDirectory_1},
                rootDirectory.getDirectories().toArray()
        );

        VirtualDirectory virtualDirectory_2 = rootDirectory.mkdir("virtualDirectory_2");

        assertArrayEquals(
                new VirtualDirectory[]{virtualDirectory_1, virtualDirectory_2},
                rootDirectory.getDirectories().toArray()
        );
    }

    @Test
    void touchAddFileToList() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualFile virtualFile_1 = rootDirectory.touch(name);

        assertArrayEquals(
                new VirtualFile[]{virtualFile_1},
                rootDirectory.getFiles().toArray()
        );

        VirtualFile virtualFile_2 = rootDirectory.touch(name + name);

        assertArrayEquals(
                new VirtualFile[]{virtualFile_1, virtualFile_2},
                rootDirectory.getFiles().toArray()
        );
    }

    @Test
    void mkdir() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        VirtualDirectory virtualDirectory = rootDirectory.mkdir(name);

        assertEquals(rootDirectory, virtualDirectory.rootDirectory);

        assertArrayEquals(
                new VirtualDirectory[]{virtualDirectory},
                rootDirectory.getDirectories().toArray()
        );
    }

    @Test
    void touch() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        VirtualFile virtualFile = rootDirectory.touch(name);

        assertEquals(rootDirectory, virtualFile.rootDirectory);

        assertArrayEquals(
                new VirtualFile[]{virtualFile},
                rootDirectory.getFiles().toArray()
        );
    }

    @Test
    void removeWithNullRootDirectory() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        UnremovableVirtualNodeException exception = assertThrows(UnremovableVirtualNodeException.class, rootDirectory::remove);

        assertTrue(exception.getMessage().contains("This node cannot be deleted"));
    }

    @Test
    void removeChildDirectory() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory virtualDirectory = rootDirectory.mkdir(name);

        assertDoesNotThrow(() -> rootDirectory.remove(virtualDirectory));

        assertArrayEquals(
                new VirtualDirectory[]{},
                rootDirectory.getDirectories().toArray()
        );
    }

    @Test
    void removeChildFile() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualFile virtualFile = rootDirectory.touch(name);

        assertDoesNotThrow(() -> rootDirectory.remove(virtualFile));

        assertArrayEquals(
                new VirtualFile[]{},
                rootDirectory.getFiles().toArray()
        );
    }

    @Test
    void removeSelf() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory virtualDirectory = rootDirectory.mkdir(name);

        assertDoesNotThrow(() -> virtualDirectory.remove());

        assertArrayEquals(
                new VirtualDirectory[]{},
                rootDirectory.getDirectories().toArray()
        );

        assertNull(virtualDirectory.getRootDirectory());
    }

    @Test
    void removeDeleted() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory virtualDirectory = rootDirectory.mkdir(name);

        assertDoesNotThrow(() -> virtualDirectory.remove());
        assertThrows(VirtualFSNodeIsDeletedException.class, virtualDirectory::remove);
    }

    @Test
    void removeRoot() {
        assertThrows(UnremovableVirtualNodeException.class, () -> virtualFS.getRootDirectory().remove());
    }

    @Test
    void move() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory destinationDirectory = new VirtualDirectory(name);

        VirtualDirectory virtualDirectory = rootDirectory.mkdir(name);

        virtualDirectory.move(destinationDirectory);

        assertArrayEquals(
                new VirtualDirectory[]{},
                rootDirectory.getDirectories().toArray()
        );

        assertArrayEquals(
                new VirtualDirectory[]{virtualDirectory},
                destinationDirectory.getDirectories().toArray()
        );

        assertEquals(destinationDirectory, virtualDirectory.getRootDirectory());
    }

    @Test
    void copy() throws VFSException, IOException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory destinationDirectory = new VirtualDirectory(name);

        VirtualDirectory virtualDirectory = rootDirectory.mkdir(name);

        VirtualDirectory copiedDirectory = virtualDirectory.copy(destinationDirectory);

        assertArrayEquals(
                new VirtualDirectory[]{virtualDirectory},
                rootDirectory.getDirectories().toArray()
        );

        assertArrayEquals(
                new VirtualDirectory[]{copiedDirectory},
                destinationDirectory.getDirectories().toArray()
        );

        assertEquals(rootDirectory, virtualDirectory.getRootDirectory());
        assertEquals(destinationDirectory, copiedDirectory.getRootDirectory());

        assertEquals(
                virtualDirectory.getName(),
                copiedDirectory.getName()
        );

        assertNotEquals(virtualDirectory, copiedDirectory);
    }

    @Test
    void copyWithChildren() throws VFSException, IOException {
        String fileName = "test file name";
        String directoryName = "test directory name";

        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory destinationDirectory = new VirtualDirectory(name);

        VirtualDirectory virtualDirectory = rootDirectory.mkdir(name);
        VirtualFile testFile = virtualDirectory.touch(fileName);
        VirtualDirectory testDirectory = virtualDirectory.mkdir(directoryName);

        VirtualDirectory copiedDirectory = virtualDirectory.copy(destinationDirectory);

        assertEquals(
                virtualDirectory.getFiles().size(),
                copiedDirectory.getFiles().size()
        );

        assertEquals(
                virtualDirectory.getDirectories().size(),
                copiedDirectory.getDirectories().size()
        );

        assertNotEquals(testFile, copiedDirectory.getFiles().get(0));
        assertNotEquals(testDirectory, copiedDirectory.getDirectories().get(0));
    }

    @Test
    void findSubNameInEmpty() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        Iterator<VirtualFile> emptyIterator = rootDirectory.find("123");

        assertFalse(emptyIterator.hasNext());
        assertThrows(NoSuchElementException.class, emptyIterator::next);
    }

    @Test
    void findSubName() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        VirtualFile virtualFile = rootDirectory.touch("123");
        rootDirectory.touch("456");

        Iterator<VirtualFile> iterator = rootDirectory.find("123");

        assertTrue(iterator.hasNext());
        assertEquals(virtualFile, iterator.next());
        assertFalse(iterator.hasNext());

        VirtualFile secondVirtualFile = rootDirectory.touch("123456");

        iterator = rootDirectory.find("123");

        assertTrue(iterator.hasNext());
        assertEquals(virtualFile, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(secondVirtualFile, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void findSubNameNotRecursive() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        VirtualFile virtualFile = rootDirectory.touch("123");
        rootDirectory.touch("456");

        Iterator<VirtualFile> iterator = rootDirectory.find("123");

        assertTrue(iterator.hasNext());
        assertEquals(virtualFile, iterator.next());
        assertFalse(iterator.hasNext());

        rootDirectory.mkdir(name).touch("123456");

        iterator = rootDirectory.find("123");

        assertTrue(iterator.hasNext());
        assertEquals(virtualFile, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void recursiveFindSubName() throws VFSException {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        VirtualFile virtualFile = rootDirectory.touch("123");
        rootDirectory.touch("456");

        VirtualFile secondVirtualFile = rootDirectory.mkdir("test directory").touch("451236");

        Iterator<VirtualFile> iterator = rootDirectory.find("123", true);

        assertTrue(iterator.hasNext());
        assertEquals(virtualFile, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(secondVirtualFile, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void findByPattern() throws VFSException {
        Pattern pattern = Pattern.compile("^test.*\\.java$");

        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        VirtualFile test123Java = rootDirectory.touch("test123.java");
        VirtualFile testJava = rootDirectory.touch("test.java");
        rootDirectory.touch("123test.java");
        rootDirectory.touch("123test.kt");

        Iterator<VirtualFile> iterator = rootDirectory.find(pattern, true);

        assertTrue(iterator.hasNext());
        assertEquals(test123Java, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(testJava, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void crashIterator() throws VFSException, IOException {
        VirtualFile file = virtualFS.touch(name);
        VirtualRandomAccessFile randomAccessFile = file.open("rw");

        Iterator<VirtualFile> iterator = virtualFS.find(name);

        assertThrows(ConcurrentModificationException.class, iterator::next);

        randomAccessFile.close();

        Iterator<VirtualFile> iterator_1 = virtualFS.find(name);

        assertDoesNotThrow(iterator_1::next);
    }

    @Test
    void testWriteLock() throws IOException, VFSException {
        String destinationDirectoryName = "destinationDirectoryName";
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(destinationDirectoryName);
        VirtualFile file = directory.touch(name);

        VirtualRandomAccessFile randomAccessFile = file.open("rw");

        assertThrows(LockedVirtualFSNodeException.class, () -> directory.move(destinationDirectory));
        assertThrows(LockedVirtualFSNodeException.class, () -> directory.copy(destinationDirectory));
        assertThrows(LockedVirtualFSNodeException.class, directory::remove);

        assertDoesNotThrow(() -> directory.rename(name + name));
        assertDoesNotThrow(() -> directory.touch(name + name));
        assertDoesNotThrow(() -> directory.mkdir(name + name));

        randomAccessFile.close();

        assertDoesNotThrow(() -> directory.move(destinationDirectory));
    }

    @Test
    void testReadLock() throws IOException, VFSException {
        String destinationDirectoryName = "destinationDirectoryName";
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(destinationDirectoryName);
        VirtualFile file = directory.touch(name);

        VirtualRandomAccessFile randomAccessFile = file.open("r");

        assertThrows(LockedVirtualFSNodeException.class, () -> directory.move(destinationDirectory));
        assertThrows(LockedVirtualFSNodeException.class, directory::remove);

        assertDoesNotThrow(() -> directory.rename(name + name));
        assertDoesNotThrow(() -> directory.touch(name + name));
        assertDoesNotThrow(() -> directory.mkdir(name + name));
        assertDoesNotThrow(() -> directory.copy(destinationDirectory).remove());

        randomAccessFile.close();

        assertDoesNotThrow(() -> directory.move(destinationDirectory));
    }

    @Test
    void importFromVirtualFS() throws IOException, VFSException, ClassNotFoundException {
        folder.create();
        File sourceFile_1 = folder.newFile(name + "1");
        File sourceFile_2 = folder.newFile(name + "2");
        VirtualFS virtualFS_1 = new VirtualFS(sourceFile_1);
        VirtualFS virtualFS_2 = new VirtualFS(sourceFile_2);

        virtualFS_1.mkdir(name).touch(name);
        virtualFS_1.touch(name);

        virtualFS_2.getRootDirectory().importContent(virtualFS_1.getRootDirectory());

        assertEquals(
                virtualFS_1.getFiles().size(),
                virtualFS_2.getFiles().size()
        );

        assertEquals(
                virtualFS_1.getDirectories().size(),
                virtualFS_2.getDirectories().size()
        );
    }

    @Test
    void importFromVirtualFSWithContent() throws IOException, ClassNotFoundException, VFSException {
        folder.create();
        File sourceFile_1 = folder.newFile(name + "1");
        File sourceFile_2 = folder.newFile(name + "2");
        VirtualFS virtualFS_1 = new VirtualFS(sourceFile_1);
        VirtualFS virtualFS_2 = new VirtualFS(sourceFile_2);

        VirtualFile file = virtualFS_1.touch(name);
        VirtualRandomAccessFile randomAccessFile = file.open("rw");
        randomAccessFile.write("Hello".getBytes());

        assertThrows(
                LockedVirtualFSNodeException.class,
                () -> virtualFS_2.getRootDirectory().importContent(virtualFS_1.getRootDirectory())
        );

        randomAccessFile.close();

        virtualFS_2.getRootDirectory().importContent(virtualFS_1.getRootDirectory());

        randomAccessFile = virtualFS_2.getFiles().get(0).open("r");

        assertEquals("Hello", randomAccessFile.readLine());
    }

    @Test
    void importFromRealFileSystem() throws IOException, VFSException {
        String content = "Hello world, I'm in virtual system)))";
        byte[] bytes = content.getBytes();

        File sourceFolder = folder.newFolder("test_folder");

        String fileName = "test_file";

        File file = new File(sourceFolder, fileName);
        FileOutputStream out = new FileOutputStream(file);
        out.write(bytes);
        out.close();

        virtualFS.getRootDirectory().importContent(sourceFolder);

        assertEquals(1, virtualFS.getFiles().size());

        VirtualFile virtualFile = virtualFS.getFiles().get(0);

        assertEquals(fileName, virtualFile.getName());

        VirtualRandomAccessFile virtualRandomAccessFile = virtualFile.open("r");

        assertEquals(content, virtualRandomAccessFile.readLine());
    }
}
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class VirtualFSTest {
    final String name = "test_name";

    @Test
    void initialization() {
        VirtualFS virtualFS = new VirtualFS();

        assertNull(virtualFS.getRootDirectory().getRootDirectory());
        assertEquals("", virtualFS.getRootDirectory().getName());
        assertArrayEquals(
                new VirtualDirectory[]{},
                virtualFS.getRootDirectory().getDirectories().toArray()
        );
        assertArrayEquals(
                new VirtualFile[]{},
                virtualFS.getRootDirectory().getFiles().toArray()
        );
    }

    @Test
    void mkdir() {
        VirtualFS virtualFS = new VirtualFS();

        assertArrayEquals(
                new VirtualDirectory[]{},
                virtualFS.getRootDirectory().getDirectories().toArray()
        );

        VirtualDirectory newDirectory = virtualFS.mkdir(name);

        assertArrayEquals(
                new VirtualDirectory[]{newDirectory},
                virtualFS.getRootDirectory().getDirectories().toArray()
        );

        assertEquals(newDirectory, virtualFS.getRootDirectory().getDirectories().get(0));
    }

    @Test
    void touch() {
        VirtualFS virtualFS = new VirtualFS();

        assertArrayEquals(
                new VirtualFile[]{},
                virtualFS.getRootDirectory().getFiles().toArray()
        );

        VirtualFile newFile = virtualFS.touch(name);

        assertArrayEquals(
                new VirtualFile[]{newFile},
                virtualFS.getRootDirectory().getFiles().toArray()
        );

        assertEquals(newFile, virtualFS.getRootDirectory().getFiles().get(0));
    }

    @Test
    void removeDirectory() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualDirectory virtualDirectory = virtualFS.mkdir(name);

        assertDoesNotThrow(() -> virtualFS.remove(virtualDirectory));

        assertArrayEquals(
                new VirtualDirectory[]{},
                virtualFS.getRootDirectory().getDirectories().toArray()
        );

        assertNull(virtualDirectory.getRootDirectory());
    }

    @Test
    void removeFile() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualFile virtualFile = virtualFS.touch(name);

        assertDoesNotThrow(() -> virtualFS.remove(virtualFile));

        assertArrayEquals(
                new VirtualDirectory[]{},
                virtualFS.getRootDirectory().getFiles().toArray()
        );

        assertNull(virtualFile.getRootDirectory());
    }

    @Test
    void moveFile() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        virtualFS.move(virtualFile, destinationDirectory);

        assertEquals(destinationDirectory, virtualFile.getRootDirectory());
    }

    @Test
    void moveDirectory() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualDirectory virtualDirectory  = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        virtualFS.move(virtualDirectory, destinationDirectory);

        assertEquals(destinationDirectory, virtualDirectory.getRootDirectory());
    }

    @Test
    void moveFileToRootDirectory() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualFile virtualFile = virtualFS.touch(name);

        virtualFS.move(virtualFile);

        assertEquals(virtualFile.getRootDirectory(), virtualFile.getRootDirectory());
    }

    @Test
    void moveDirectoryToRootDirectory() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualDirectory virtualDirectory  = virtualFS.mkdir(name).mkdir(name);

        virtualFS.move(virtualDirectory);

        assertEquals(virtualFS.getRootDirectory(), virtualDirectory.getRootDirectory());
    }

    @Test
    void copyFile() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualFile copiedFile = virtualFS.copy(virtualFile, destinationDirectory);

        assertEquals(virtualFS.getRootDirectory(), virtualFile.getRootDirectory());
        assertEquals(destinationDirectory, copiedFile.getRootDirectory());
    }

    @Test
    void copyDirectory() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualDirectory virtualDirectory  = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualDirectory copiedDirectory = virtualFS.copy(virtualDirectory, destinationDirectory);

        assertEquals(destinationDirectory, copiedDirectory.getRootDirectory());
        assertEquals(virtualFS.getRootDirectory(), virtualDirectory.getRootDirectory());
    }

    @Test
    void copyFileToRootDirectory() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualFile virtualFile = virtualFS.mkdir(name).touch(name);

        VirtualFile copiedFile = virtualFS.copy(virtualFile);

        assertEquals(virtualFS.getRootDirectory(), copiedFile.getRootDirectory());
        assertNotEquals(virtualFS.getRootDirectory(), virtualFile.getRootDirectory());
    }

    @Test
    void copyDirectoryToRootDirectory() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualDirectory virtualDirectory = virtualFS.mkdir(name).mkdir(name);

        VirtualDirectory copiedDirectory = virtualFS.copy(virtualDirectory);

        assertNotEquals(virtualFS.getRootDirectory(), virtualDirectory.getRootDirectory());
        assertEquals(virtualFS.getRootDirectory(), copiedDirectory.getRootDirectory());
    }

    @Test
    void findBySubName() {
        VirtualFS virtualFS = new VirtualFS();

        VirtualFile firstFile = virtualFS.touch("test_file");
        virtualFS.touch("123");
        VirtualFile secondFile = virtualFS.mkdir(name).touch("file_test");

        Iterator<VirtualFile> iterator = virtualFS.find("test");

        assertTrue(iterator.hasNext());
        assertEquals(firstFile, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(secondFile, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void findByPattern() {
        VirtualFS virtualFS = new VirtualFS();

        Pattern pattern = Pattern.compile("^.*test.*$");

        VirtualFile firstFile = virtualFS.touch("test_file");
        virtualFS.touch("123");
        VirtualFile secondFile = virtualFS.mkdir(name).touch("file_test");

        Iterator<VirtualFile> iterator = virtualFS.find(pattern);

        assertTrue(iterator.hasNext());
        assertEquals(firstFile, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(secondFile, iterator.next());
        assertFalse(iterator.hasNext());
    }
}
import exceptions.UnremovableVirtualNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VirtualDirectoryTest {
    final String name = "test_name";

    @Test
     void initialization() {
        VirtualDirectory directory = new VirtualDirectory(name);

        assertNull(directory.getRootDirectory());

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
    void initializationWithRootDirectory() {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory directory = new VirtualDirectory(name, rootDirectory);

        assertEquals(rootDirectory, directory.getRootDirectory());
    }

    @Test
    void getDirectories() {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory virtualDirectory_1 = rootDirectory.mkdir(name);

        assertArrayEquals(
                new VirtualDirectory[]{virtualDirectory_1},
                rootDirectory.getDirectories().toArray()
        );

        VirtualDirectory virtualDirectory_2 = rootDirectory.mkdir(name);

        assertArrayEquals(
                new VirtualDirectory[]{virtualDirectory_1, virtualDirectory_2},
                rootDirectory.getDirectories().toArray()
        );
    }

    @Test
    void getFiles() {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualFile virtualFile_1 = rootDirectory.touch(name);

        assertArrayEquals(
                new VirtualFile[]{virtualFile_1},
                rootDirectory.getFiles().toArray()
        );

        VirtualFile virtualFile_2 = rootDirectory.touch(name);

        assertArrayEquals(
                new VirtualFile[]{virtualFile_1, virtualFile_2},
                rootDirectory.getFiles().toArray()
        );
    }

    @Test
    void mkdir() {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        VirtualDirectory virtualDirectory = rootDirectory.mkdir(name);

        assertEquals(rootDirectory, virtualDirectory.rootDirectory);

        assertArrayEquals(
                new VirtualDirectory[]{virtualDirectory},
                rootDirectory.getDirectories().toArray()
        );
    }

    @Test
    void touch() {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        VirtualFile virtualFile = rootDirectory.touch(name);

        assertEquals(rootDirectory, virtualFile.rootDirectory);

        assertArrayEquals(
                new VirtualFile[]{virtualFile},
                rootDirectory.getFiles().toArray()
        );
    }

    @Test
    void removeWithNullRootDirectory() {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);

        Exception exception = assertThrows(UnremovableVirtualNode.class, rootDirectory::remove);

        assertTrue(exception.getMessage().contains("This node cannot be deleted"));
    }

    @Test
    void removeChildDirectory() {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory virtualDirectory = rootDirectory.mkdir(name);

        assertDoesNotThrow(() -> rootDirectory.remove(virtualDirectory));

        assertArrayEquals(
                new VirtualDirectory[]{},
                rootDirectory.getDirectories().toArray()
        );
    }

    @Test
    void removeChildFile() {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualFile virtualFile = rootDirectory.touch(name);

        assertDoesNotThrow(() -> rootDirectory.remove(virtualFile));

        assertArrayEquals(
                new VirtualFile[]{},
                rootDirectory.getFiles().toArray()
        );
    }

    @Test
    void removeSelf() {
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
    void move() {
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
    void copy() {
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
    void copyWithChildren() {
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
}
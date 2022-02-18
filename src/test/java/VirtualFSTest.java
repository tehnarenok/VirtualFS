import org.junit.jupiter.api.Test;

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
}
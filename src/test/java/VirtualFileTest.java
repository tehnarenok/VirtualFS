import exceptions.UnremovableVirtualNode;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class VirtualFileTest {
    final String name = "test_name";
    final String newName = "name_test";

    @Test
    void createFile() {
        VirtualFile virtualFile = new VirtualFile(name);
        Date createdAt = new Date();

        assertArrayEquals(
                new byte[]{},
                virtualFile.getContent()
        );

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
    void createFileWithContent() {
        byte[] content;
        content = new byte[]{1, 2, 4};

        VirtualFile virtualFile = new VirtualFile(name, null, content);

        assertArrayEquals(
                content,
                virtualFile.getContent()
        );
    }

    @Test
    void renameFile() throws InterruptedException {
        VirtualFile virtualFile;
        virtualFile = new VirtualFile(name);

        Thread.sleep(1000);

        virtualFile.rename(newName);
        Date modifiedAt = new Date();

        assertEquals(
                modifiedAt,
                virtualFile.getModifiedAt()
        );

        assertEquals(
                newName,
                virtualFile.getName()
        );
    }

    @Test
    void removeWithNullRootDirectory() {
        VirtualFile virtualFile = new VirtualFile(name);

        Exception exception = assertThrows(UnremovableVirtualNode.class, virtualFile::remove);

        assertEquals(
                "This node cannot be deleted",
                exception.getMessage()
        );
    }

    @Test
    void removeSelf() {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualFile virtualFile = rootDirectory.touch(name);

        assertDoesNotThrow(virtualFile::remove);

        assertArrayEquals(
                new VirtualFile[]{},
                rootDirectory.getFiles().toArray()
        );

        assertNull(virtualFile.getRootDirectory());
    }

    @Test
    void remove() {
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
    void move() {
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
    void copy() {
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
    void copyWithContent() {
        VirtualDirectory rootDirectory = new VirtualDirectory(name);
        VirtualDirectory destinationDirectory = new VirtualDirectory(name);

        byte[] content;
        content = new byte[]{1, 2, 4};

        VirtualFile virtualFile = new VirtualFile(name, rootDirectory, content);
        rootDirectory.paste(virtualFile);

        VirtualFile copiedVirtualFile = virtualFile.copy(destinationDirectory);

        assertArrayEquals(
                content,
                copiedVirtualFile.getContent()
        );
    }
}
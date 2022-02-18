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
}
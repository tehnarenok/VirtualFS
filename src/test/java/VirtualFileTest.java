import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VirtualFileTest {
    @Test
    void createFile() {
        String name = "test_name";

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
        String name = "test_name";
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
        String name = "test_name";
        String newName = "name_test";

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
}
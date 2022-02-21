import exceptions.*;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

public class MultiThreadFileTest {
    final String name = "test_name";

    @Rule
    public TemporaryFolder folder = TemporaryFolder.builder().assureDeletion().build();

    @Test
    void testReadAndDelete() throws IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);

        VirtualRandomAccessFile randomAccessFile = virtualFile.open("r");

        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.remove());

        randomAccessFile.close();

        assertDoesNotThrow(() -> virtualFile.remove());
    }

    @Test
    void testReadAndMove() throws IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualRandomAccessFile randomAccessFile = virtualFile.open("r");

        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.move(destinationDirectory));

        randomAccessFile.close();

        assertDoesNotThrow(() -> virtualFile.move(destinationDirectory));
    }

    @Test
    void testReadAndCopy() throws IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualRandomAccessFile randomAccessFile = virtualFile.open("r");

        assertDoesNotThrow(() -> virtualFile.copy(destinationDirectory));

        randomAccessFile.close();
    }

    @Test
    void testReadAndRename() throws IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);

        VirtualRandomAccessFile randomAccessFile = virtualFile.open("r");

        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.rename(name + name));

        randomAccessFile.close();

        assertDoesNotThrow(() -> virtualFile.rename(name + name));
    }

    @Test
    void testWriteAndDelete() throws IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);

        VirtualRandomAccessFile randomAccessFile = virtualFile.open("rw");

        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.remove());

        randomAccessFile.close();

        assertDoesNotThrow(() -> virtualFile.remove());
    }

    @Test
    void testWriteAndMove() throws IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualRandomAccessFile randomAccessFile = virtualFile.open("rw");

        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.move(destinationDirectory));

        randomAccessFile.close();

        assertDoesNotThrow(() -> virtualFile.move(destinationDirectory));
    }

    @Test
    void testWriteAndCopy() throws IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualRandomAccessFile randomAccessFile = virtualFile.open("rw");

        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.copy(destinationDirectory));

        randomAccessFile.close();

        assertDoesNotThrow(() -> virtualFile.copy(destinationDirectory));
    }

    @Test
    void testWriteAndRename() throws IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);

        VirtualRandomAccessFile randomAccessFile = virtualFile.open("rw");

        assertThrows(LockedVirtualFSNode.class, () -> virtualFile.rename(name + name));

        randomAccessFile.close();

        assertDoesNotThrow(() -> virtualFile.rename(name + name));
    }
}

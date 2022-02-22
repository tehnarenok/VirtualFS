import exceptions.*;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class MultiThreadVirtualDirectoryTest {
    final String name = "test_name";

    @Rule
    public TemporaryFolder folder = TemporaryFolder.builder().assureDeletion().build();

    private VirtualFS virtualFS;

    @BeforeEach
    public void setup() throws IOException, ClassNotFoundException {
        folder.create();
        File sourceFile = folder.newFile(name);
        virtualFS = new VirtualFS(sourceFile);
    }

    @Test
    public void writeFile_deleteDirectory() throws IOException, VFSException {
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualFile file = directory.touch(name);

        VirtualRandomAccessFile randomAccessFile = file.open("rw");

        assertThrows(LockedVirtualFSNode.class, directory::remove);

        randomAccessFile.close();

        assertDoesNotThrow(() -> directory.remove());
    }

    @Test
    public void writeFile_copyDirectory() throws IOException, VFSException {
        String destinationDirectoryName = "destinationDirectoryName";
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(destinationDirectoryName);
        VirtualFile file = directory.touch(name);

        VirtualRandomAccessFile randomAccessFile = file.open("rw");

        assertThrows(LockedVirtualFSNode.class, () -> directory.copy(destinationDirectory));

        randomAccessFile.close();

        assertDoesNotThrow(() -> directory.copy(destinationDirectory));
    }

    @Test
    public void writeFile_moveDirectory() throws IOException, VFSException {
        String destinationDirectoryName = "destinationDirectoryName";
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(destinationDirectoryName);
        VirtualFile file = directory.touch(name);

        VirtualRandomAccessFile randomAccessFile = file.open("rw");

        assertThrows(LockedVirtualFSNode.class, () -> directory.move(destinationDirectory));

        randomAccessFile.close();

        assertDoesNotThrow(() -> directory.move(destinationDirectory));
    }

    @Test
    public void openFile_touchNearby() throws IOException, VFSException {
        VirtualDirectory directory = virtualFS.mkdir(name);
        virtualFS.mkdir(name + name);
        VirtualFile file = directory.touch(name);

        file.open("rw");

        assertDoesNotThrow(() -> directory.touch(name + name));
    }

    @Test
    public void openFile_mkdirNearby() throws IOException, VFSException {
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualFile file = directory.touch(name);

        file.open("rw");

        assertDoesNotThrow(() -> directory.mkdir(name));
    }

    @Test
    public void readFile_deleteDirectory() throws IOException, VFSException {
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualFile file = directory.touch(name);

        VirtualRandomAccessFile randomAccessFile = file.open("r");

        assertThrows(LockedVirtualFSNode.class, directory::remove);

        randomAccessFile.close();

        assertDoesNotThrow(() -> directory.remove());
    }

    @Test
    public void readFile_copyDirectory() throws IOException, VFSException {
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name + name);
        VirtualFile file = directory.touch(name);

        file.open("r");

        assertDoesNotThrow(() -> directory.copy(destinationDirectory));
    }

    @Test
    public void readFile_moveDirectory() throws IOException, VFSException {
        String destinationDirectoryName = "destinationDirectoryName";
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(destinationDirectoryName);
        VirtualFile file = directory.touch(name);

        VirtualRandomAccessFile randomAccessFile = file.open("r");

        assertThrows(LockedVirtualFSNode.class, () -> directory.move(destinationDirectory));

        randomAccessFile.close();

        assertDoesNotThrow(() -> directory.move(destinationDirectory));
    }
}

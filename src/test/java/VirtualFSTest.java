
import exceptions.LockedVirtualFSNode;
import exceptions.NullVirtualFS;
import exceptions.OverlappingVirtualFileLockException;
import exceptions.VirtualFSNodeIsDeleted;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class VirtualFSTest {
    final String name = "test_name";

    @Rule
    public TemporaryFolder folder = TemporaryFolder.builder().assureDeletion().build();

    private VirtualFS virtualFS;
    private File sourceFile;

    @BeforeEach
    public void setup() throws IOException, ClassNotFoundException {
        folder.create();
        sourceFile = folder.newFile(name);
        virtualFS = new VirtualFS(sourceFile);
    }

    @Test
    public void creatingVirtualFS() throws LockedVirtualFSNode {
        assertNull(virtualFS.getRootDirectory().getRootDirectory());
        assertEquals("", virtualFS.getRootDirectory().getName());
        assertArrayEquals(
                new VirtualDirectory[]{},
                virtualFS.getDirectories().toArray()
        );
        assertArrayEquals(
                new VirtualFile[]{},
                virtualFS.getFiles().toArray()
        );
    }

    @Test
    void mkdir() throws LockedVirtualFSNode {
        VirtualDirectory newDirectory = virtualFS.mkdir(name);

        assertArrayEquals(
                new VirtualDirectory[]{newDirectory},
                virtualFS.getDirectories().toArray()
        );

        assertEquals(newDirectory, virtualFS.getDirectories().get(0));
    }

    @Test
    void touch() throws LockedVirtualFSNode {
        VirtualFile newFile = virtualFS.touch(name);

        assertArrayEquals(
                new VirtualFile[]{newFile},
                virtualFS.getFiles().toArray()
        );

        assertEquals(newFile, virtualFS.getFiles().get(0));
    }

    @Test
    void removeDirectory() throws LockedVirtualFSNode {
        VirtualDirectory virtualDirectory = virtualFS.mkdir(name);

        assertDoesNotThrow(() -> virtualFS.remove(virtualDirectory));

        assertArrayEquals(
                new VirtualDirectory[]{},
                virtualFS.getDirectories().toArray()
        );

        assertNull(virtualDirectory.getRootDirectory());
    }

    @Test
    void removeFile() throws LockedVirtualFSNode {
        VirtualFile virtualFile = virtualFS.touch(name);

        assertDoesNotThrow(() -> virtualFS.remove(virtualFile));

        assertArrayEquals(
                new VirtualDirectory[]{},
                virtualFS.getFiles().toArray()
        );

        assertNull(virtualFile.getRootDirectory());
    }

    @Test
    void moveFile() throws LockedVirtualFSNode, VirtualFSNodeIsDeleted {
        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        virtualFS.move(virtualFile, destinationDirectory);

        assertArrayEquals(new VirtualFile[]{}, virtualFS.getFiles().toArray());
        assertArrayEquals(new VirtualFile[]{virtualFile}, destinationDirectory.getFiles().toArray());
        assertEquals(destinationDirectory, virtualFile.getRootDirectory());
    }

    @Test
    void moveDirectory() throws LockedVirtualFSNode {
        VirtualDirectory virtualDirectory  = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        virtualFS.move(virtualDirectory, destinationDirectory);

        assertArrayEquals(new VirtualDirectory[]{destinationDirectory}, virtualFS.getDirectories().toArray());
        assertArrayEquals(new VirtualDirectory[]{virtualDirectory}, destinationDirectory.getDirectories().toArray());
        assertEquals(destinationDirectory, virtualDirectory.getRootDirectory());
    }

    @Test
    void moveFileToRootDirectory() throws LockedVirtualFSNode, VirtualFSNodeIsDeleted {
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualFile virtualFile = directory.touch(name);

        virtualFS.move(virtualFile);

        assertArrayEquals(new VirtualFile[]{}, directory.getFiles().toArray());
        assertArrayEquals(new VirtualFile[]{virtualFile}, virtualFS.getFiles().toArray());
        assertEquals(virtualFile.getRootDirectory(), virtualFile.getRootDirectory());
    }

    @Test
    void moveDirectoryToRootDirectory() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualDirectory virtualDirectory  = directory.mkdir(name);

        virtualFS.move(virtualDirectory);

        assertArrayEquals(new VirtualDirectory[]{}, directory.getDirectories().toArray());
        assertArrayEquals(new VirtualDirectory[]{directory, virtualDirectory}, virtualFS.getDirectories().toArray());
        assertEquals(virtualFS.getRootDirectory(), virtualDirectory.getRootDirectory());
    }

    @Test
    void copyFile() throws IOException, NullVirtualFS, LockedVirtualFSNode, OverlappingVirtualFileLockException, VirtualFSNodeIsDeleted {
        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualFile copiedFile = virtualFS.copy(virtualFile, destinationDirectory);

        assertArrayEquals(new VirtualFile[]{virtualFile}, virtualFS.getFiles().toArray());
        assertArrayEquals(new VirtualFile[]{copiedFile}, destinationDirectory.getFiles().toArray());

        assertEquals(virtualFS.getRootDirectory(), virtualFile.getRootDirectory());
        assertEquals(destinationDirectory, copiedFile.getRootDirectory());
    }

    @Test
    void copyDirectory()
            throws IOException, LockedVirtualFSNode,
            NullVirtualFS, OverlappingVirtualFileLockException, VirtualFSNodeIsDeleted {

        VirtualDirectory virtualDirectory  = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualDirectory copiedDirectory = virtualFS.copy(virtualDirectory, destinationDirectory);

        assertTrue(virtualFS.getDirectories().contains(virtualDirectory));
        assertArrayEquals(new VirtualDirectory[]{copiedDirectory}, destinationDirectory.getDirectories().toArray());

        assertEquals(destinationDirectory, copiedDirectory.getRootDirectory());
        assertEquals(virtualFS.getRootDirectory(), virtualDirectory.getRootDirectory());
    }

    @Test
    void copyFileToRootDirectory()
            throws IOException, NullVirtualFS, LockedVirtualFSNode,
            OverlappingVirtualFileLockException, VirtualFSNodeIsDeleted {
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualFile virtualFile = directory.touch(name);

        VirtualFile copiedFile = virtualFS.copy(virtualFile);

        assertArrayEquals(new VirtualFile[]{virtualFile}, directory.getFiles().toArray());
        assertArrayEquals(new VirtualFile[]{copiedFile}, virtualFS.getFiles().toArray());

        assertEquals(virtualFS.getRootDirectory(), copiedFile.getRootDirectory());
        assertNotEquals(virtualFS.getRootDirectory(), virtualFile.getRootDirectory());
    }

    @Test
    void copyDirectoryToRootDirectory()
            throws IOException, LockedVirtualFSNode, NullVirtualFS,
            OverlappingVirtualFileLockException, VirtualFSNodeIsDeleted {
        VirtualDirectory directory = virtualFS.mkdir(name);
        VirtualDirectory virtualDirectory = directory.mkdir(name);

        VirtualDirectory copiedDirectory = virtualFS.copy(virtualDirectory);

        assertTrue(directory.getDirectories().contains(virtualDirectory));
        assertTrue(virtualFS.getDirectories().contains(copiedDirectory));

        assertEquals(directory, virtualDirectory.getRootDirectory());
        assertEquals(virtualFS.getRootDirectory(), copiedDirectory.getRootDirectory());
    }

    @Test
    void findBySubName() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile firstFile = virtualFS.touch("test_file");
        virtualFS.touch("123");
        VirtualFile secondFile = virtualFS.mkdir(name).touch("file_test");

        Iterator<VirtualFile> iterator = virtualFS.find("test");

        List<VirtualFile> files = new ArrayList<>();
        iterator.forEachRemaining(files::add);

        assertArrayEquals(
                new VirtualFile[]{firstFile, secondFile},
                files.toArray()
        );
    }

    @Test
    void findByPattern() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        Pattern pattern = Pattern.compile("^.*test.*$");

        VirtualFile firstFile = virtualFS.touch("test_file");
        virtualFS.touch("123");
        VirtualFile secondFile = virtualFS.mkdir(name).touch("file_test");

        Iterator<VirtualFile> iterator = virtualFS.find(pattern);

        List<VirtualFile> files = new ArrayList<>();
        iterator.forEachRemaining(files::add);

        assertArrayEquals(
                new VirtualFile[]{firstFile, secondFile},
                files.toArray()
        );
    }

    @Test
    void readVirtualFSFromFile() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        VirtualFile originalFile = virtualFS.touch("test123");

        VirtualFS vfs = new VirtualFS(sourceFile);

        assertEquals(1, vfs.getFiles().size());

        assertEquals("test123", vfs.getFiles().get(0).getName());
        assertEquals(originalFile.getCreatedAt(), vfs.getFiles().get(0).getCreatedAt());
        assertEquals(originalFile.getModifiedAt(), vfs.getFiles().get(0).getModifiedAt());
    }

    @Test
    void testFileStructureSaveToFile() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        String directoryName1 = "directoryName1";
        String directoryName2 = "directoryName2";

        String fileName0 = "fileName0";
        String fileName1 = "fileName1";
        String fileName2 = "fileName2";

        virtualFS.touch(fileName0);
        VirtualDirectory originalDir1 = virtualFS.mkdir(directoryName1);
        VirtualDirectory originalDir2 = originalDir1.mkdir(directoryName2);
        originalDir1.touch(fileName1);
        originalDir2.touch(fileName2);

        VirtualFS vfs = new VirtualFS(sourceFile, 8);

        assertEquals(1, vfs.getFiles().size());
        assertEquals(1, vfs.getDirectories().size());

        assertEquals(fileName0, vfs.getFiles().get(0).getName());

        VirtualDirectory dir1 = vfs.getDirectories().get(0);

        assertEquals(1, dir1.getFiles().size());
        assertEquals(1, dir1.getDirectories().size());

        assertEquals(fileName1, dir1.getFiles().get(0).getName());

        VirtualDirectory dir2 = dir1.getDirectories().get(0);

        assertEquals(1, dir2.getFiles().size());
        assertEquals(0, dir2.getDirectories().size());

        assertEquals(fileName2, dir2.getFiles().get(0).getName());
    }
}
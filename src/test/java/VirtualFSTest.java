
import exceptions.LockedVirtualFSNode;
import exceptions.NullVirtualFS;
import exceptions.OverlappingVirtualFileLockException;
import exceptions.VirtualFSNodeIsDeleted;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class VirtualFSTest {
    final String name = "test_name";

    @Rule
    public TemporaryFolder folder = TemporaryFolder.builder().assureDeletion().build();

    @Test
    void initialization() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

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
    void mkdir() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        assertArrayEquals(
                new VirtualDirectory[]{},
                virtualFS.getDirectories().toArray()
        );

        VirtualDirectory newDirectory = virtualFS.mkdir(name);

        assertArrayEquals(
                new VirtualDirectory[]{newDirectory},
                virtualFS.getDirectories().toArray()
        );

        assertEquals(newDirectory, virtualFS.getDirectories().get(0));
    }

    @Test
    void touch() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        assertArrayEquals(
                new VirtualFile[]{},
                virtualFS.getFiles().toArray()
        );

        VirtualFile newFile = virtualFS.touch(name);

        assertArrayEquals(
                new VirtualFile[]{newFile},
                virtualFS.getFiles().toArray()
        );

        assertEquals(newFile, virtualFS.getFiles().get(0));
    }

    @Test
    void removeDirectory() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualDirectory virtualDirectory = virtualFS.mkdir(name);

        assertDoesNotThrow(() -> virtualFS.remove(virtualDirectory));

        assertArrayEquals(
                new VirtualDirectory[]{},
                virtualFS.getDirectories().toArray()
        );

        assertNull(virtualDirectory.getRootDirectory());
    }

    @Test
    void removeFile() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);

        assertDoesNotThrow(() -> virtualFS.remove(virtualFile));

        assertArrayEquals(
                new VirtualDirectory[]{},
                virtualFS.getFiles().toArray()
        );

        assertNull(virtualFile.getRootDirectory());
    }

    @Test
    void moveFile() throws IOException, ClassNotFoundException, LockedVirtualFSNode, VirtualFSNodeIsDeleted {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        virtualFS.move(virtualFile, destinationDirectory);

        assertEquals(destinationDirectory, virtualFile.getRootDirectory());
    }

    @Test
    void moveDirectory() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualDirectory virtualDirectory  = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        virtualFS.move(virtualDirectory, destinationDirectory);

        assertEquals(destinationDirectory, virtualDirectory.getRootDirectory());
    }

    @Test
    void moveFileToRootDirectory() throws IOException, ClassNotFoundException, LockedVirtualFSNode, VirtualFSNodeIsDeleted {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);

        virtualFS.move(virtualFile);

        assertEquals(virtualFile.getRootDirectory(), virtualFile.getRootDirectory());
    }

    @Test
    void moveDirectoryToRootDirectory() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualDirectory virtualDirectory  = virtualFS.mkdir(name).mkdir(name);

        virtualFS.move(virtualDirectory);

        assertEquals(virtualFS.getRootDirectory(), virtualDirectory.getRootDirectory());
    }

    @Test
    void copyFile() throws IOException, ClassNotFoundException, NullVirtualFS, LockedVirtualFSNode, OverlappingVirtualFileLockException, VirtualFSNodeIsDeleted {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.touch(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualFile copiedFile = virtualFS.copy(virtualFile, destinationDirectory);

        assertEquals(virtualFS.getRootDirectory(), virtualFile.getRootDirectory());
        assertEquals(destinationDirectory, copiedFile.getRootDirectory());
    }

    @Test
    void copyDirectory() throws IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException, VirtualFSNodeIsDeleted {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualDirectory virtualDirectory  = virtualFS.mkdir(name);
        VirtualDirectory destinationDirectory = virtualFS.mkdir(name);

        VirtualDirectory copiedDirectory = virtualFS.copy(virtualDirectory, destinationDirectory);

        assertEquals(destinationDirectory, copiedDirectory.getRootDirectory());
        assertEquals(virtualFS.getRootDirectory(), virtualDirectory.getRootDirectory());
    }

    @Test
    void copyFileToRootDirectory() throws IOException, ClassNotFoundException, NullVirtualFS, LockedVirtualFSNode, OverlappingVirtualFileLockException, VirtualFSNodeIsDeleted {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualFile virtualFile = virtualFS.mkdir(name).touch(name);

        VirtualFile copiedFile = virtualFS.copy(virtualFile);

        assertEquals(virtualFS.getRootDirectory(), copiedFile.getRootDirectory());
        assertNotEquals(virtualFS.getRootDirectory(), virtualFile.getRootDirectory());
    }

    @Test
    void copyDirectoryToRootDirectory() throws IOException, ClassNotFoundException, LockedVirtualFSNode, NullVirtualFS, OverlappingVirtualFileLockException, VirtualFSNodeIsDeleted {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);

        VirtualDirectory virtualDirectory = virtualFS.mkdir(name).mkdir(name);

        VirtualDirectory copiedDirectory = virtualFS.copy(virtualDirectory);

        assertNotEquals(virtualFS.getRootDirectory(), virtualDirectory.getRootDirectory());
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

        assertTrue(iterator.hasNext());
        assertEquals(firstFile, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(secondFile, iterator.next());
        assertFalse(iterator.hasNext());
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

        assertTrue(iterator.hasNext());
        assertEquals(firstFile, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(secondFile, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void createVFSInFile() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);
        virtualFS.touch("test123");
//        virtualFS.save();

        VirtualFS vfs = new VirtualFS(sourceFile, 8);

        assertEquals(1, vfs.getFiles().size());
        assertEquals("test123", vfs.getFiles().get(0).getName());
    }

    @Test
    void saveVFS_file_dates() throws IOException, ClassNotFoundException, LockedVirtualFSNode {
        folder.create();
        File sourceFile = folder.newFile(name);
        VirtualFS virtualFS = new VirtualFS(sourceFile);
        VirtualFile originalFile = virtualFS.touch("test123");
//        virtualFS.save();

        VirtualFS vfs = new VirtualFS(sourceFile, 8);

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

//        virtualFS.save();

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
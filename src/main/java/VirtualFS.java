import exceptions.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class VirtualFS {
    private VirtualDirectory rootDirectory;
    File sourceFile;
    private final VirtualRandomAccessFile virtualRandomAccessFile;

    public VirtualFS(@NotNull File sourceFile) throws IOException, ClassNotFoundException {
        this(sourceFile, 8);
    }

    public VirtualFS(
            @NotNull File sourceFile,
            long position)
            throws IOException, ClassNotFoundException {
        this.sourceFile = sourceFile;
        if(sourceFile.length() < 8) {
            this.rootDirectory = new VirtualDirectory("", null, this);
            this.virtualRandomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");
            this.save();
        } else {
            this.virtualRandomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw", position);
            load();
        }
    }

    public void save() throws IOException {
        // Серриализуем рутовую папку, и пишем в файл
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out;

        out = new ObjectOutputStream(bos);
        out.writeObject(this.rootDirectory);
        out.flush();
        byte[] bytes = bos.toByteArray();
        bos.close();

        if(virtualRandomAccessFile.length() < bytes.length) {
            this.virtualRandomAccessFile.setLength(bytes.length);
        }
        this.virtualRandomAccessFile.seek(0);
        this.virtualRandomAccessFile.write(bytes);
        this.virtualRandomAccessFile.flush();
    }

    private void load() throws IOException, ClassNotFoundException {
        // Читаем из файла и десерииалиуем в рут папку
        byte[] bytes = new byte[(int) virtualRandomAccessFile.length()];
        virtualRandomAccessFile.read(bytes);

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));

        rootDirectory = (VirtualDirectory) in.readObject();
        rootDirectory.virtualFS = this;
    }

    public void close() throws IOException, LockedVirtualFSNode {
        rootDirectory.tryWriteLockDown();
        virtualRandomAccessFile.close();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Throwable throwable) {

        }
        super.finalize();
    }

    public VirtualDirectory mkdir(@NotNull String name) throws LockedVirtualFSNode, NotUniqueName {
        return this.rootDirectory.mkdir(name);
    }

    public VirtualFile touch(@NotNull String name) throws LockedVirtualFSNode, NotUniqueName {
        return this.rootDirectory.touch(name);
    }

    public VirtualDirectory getRootDirectory() {
        return this.rootDirectory;
    }

    public List<VirtualFile> getFiles() throws LockedVirtualFSNode {
        return rootDirectory.getFiles();
    }

    public List<VirtualDirectory> getDirectories() throws LockedVirtualFSNode {
        return rootDirectory.getDirectories();
    }

    public void remove(
            @NotNull VirtualFile file)
            throws UnremovableVirtualNode, OverlappingVirtualFileLockException,
            IOException, NullVirtualFS,
            LockedVirtualFSNode, VirtualFSNodeIsDeleted {

        file.remove();
    }

    public void remove(@NotNull VirtualDirectory directory) throws UnremovableVirtualNode, OverlappingVirtualFileLockException,
            IOException, NullVirtualFS,
            LockedVirtualFSNode, VirtualFSNodeIsDeleted {
        directory.remove();
    }

    public void move(@NotNull VirtualFile virtualFile, @NotNull VirtualDirectory destinationDirectory)
            throws LockedVirtualFSNode, VirtualFSNodeIsDeleted, NotUniqueName {
        virtualFile.move(destinationDirectory);
    }

    public void move(
            @NotNull VirtualDirectory virtualDirectory,
            @NotNull VirtualDirectory destinationDirectory
    ) throws LockedVirtualFSNode, NotUniqueName, UnremovableVirtualNode {
        virtualDirectory.move(destinationDirectory);
    }

    public void move(@NotNull VirtualFile virtualFile) throws LockedVirtualFSNode, VirtualFSNodeIsDeleted, NotUniqueName {
        virtualFile.move(this.rootDirectory);
    }

    public void move(@NotNull VirtualDirectory virtualDirectory) throws LockedVirtualFSNode, NotUniqueName, UnremovableVirtualNode {
        virtualDirectory.move(this.rootDirectory);
    }

    public VirtualFile copy(
            @NotNull VirtualFile virtualFile,
            @NotNull VirtualDirectory destinationDirectory)
            throws NullVirtualFS, LockedVirtualFSNode,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeleted, NotUniqueName {
        return virtualFile.copy(destinationDirectory);
    }

    public VirtualFile copy(@NotNull VirtualFile virtualFile)
            throws NullVirtualFS, LockedVirtualFSNode,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeleted, NotUniqueName {
        return virtualFile.copy(this.rootDirectory);
    }

    public VirtualDirectory copy(
            @NotNull VirtualDirectory virtualDirectory,
            @NotNull VirtualDirectory destinationDirectory)
            throws LockedVirtualFSNode, NullVirtualFS,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeleted, NotUniqueName {
        return virtualDirectory.copy(destinationDirectory);
    }

    public VirtualDirectory copy(
            @NotNull VirtualDirectory virtualDirectory)
            throws LockedVirtualFSNode, NullVirtualFS,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeleted, NotUniqueName {
        return virtualDirectory.copy(this.rootDirectory);
    }

    public Iterator<VirtualFile> find(@NotNull String subName) {
        return this.rootDirectory.find(subName, true);
    }

    public Iterator<VirtualFile> find(@NotNull Pattern pattern) {
        return this.rootDirectory.find(pattern, true);
    }

    public void importContent(@NotNull File folder) throws NullVirtualFS, LockedVirtualFSNode,
            OverlappingVirtualFileLockException, IOException, NotUniqueName {
        rootDirectory.importContent(folder);
    }

    public void importContent(@NotNull VirtualFS virtualFS) throws NullVirtualFS, LockedVirtualFSNode,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeleted, NotUniqueName {
        rootDirectory.importContent(virtualFS.getRootDirectory());
    }

    public void importContent(@NotNull VirtualDirectory virtualDirectory) throws NullVirtualFS, LockedVirtualFSNode,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeleted, NotUniqueName {
        rootDirectory.importContent(virtualDirectory);
    }

    public void exportContent(@NotNull File folder) throws NullVirtualFS, LockedVirtualFSNode,
            OverlappingVirtualFileLockException, IOException {
        rootDirectory.exportContent(folder);
    }
}

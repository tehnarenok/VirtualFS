import exceptions.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class VirtualDirectory extends VirtualFSNode implements Serializable {
    AtomicBoolean isModifying = new AtomicBoolean(false);
    private List<VirtualDirectory> directories;
    private List<VirtualFile> files;

    transient private ReentrantReadWriteLock directoriesReadWriteLock = new ReentrantReadWriteLock();
    transient private ReentrantReadWriteLock filesReadWriteLock = new ReentrantReadWriteLock();
    transient private ReentrantReadWriteLock nameLock = new ReentrantReadWriteLock();

    public VirtualDirectory(String name) {
        this(name, null);
    }

    protected VirtualDirectory(
            @NotNull String name,
            VirtualDirectory rootDirectory) {
        this(name, rootDirectory, null);
    }

    protected VirtualDirectory(
            @NotNull String name,
            VirtualDirectory rootDirectory,
            VirtualFS virtualFS) {
        super(name, rootDirectory);
        this.directories = new ArrayList<>();
        this.files = new ArrayList<>();
        this.virtualFS = virtualFS;
    }

    public boolean isModifying() {
        return isModifying.get();
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        directoriesReadWriteLock = new ReentrantReadWriteLock();
        filesReadWriteLock = new ReentrantReadWriteLock();
        nameLock = new ReentrantReadWriteLock();
        isDeleted = false;
        isModifying = new AtomicBoolean(false);
    }

    @Override
    public void rename(@NotNull String name) throws LockedVirtualFSNode, VirtualFSNodeIsDeleted, NotUniqueName {
        List<Lock> locks = new ArrayList<>();
        try {
            locks.add(tryLockNameWrite());
            if(rootDirectory != null) locks.add(rootDirectory.tryWriteLockDirectories());
        } catch (LockedVirtualFSNode e) {
            locks.forEach(Lock::unlock);
            throw e;
        }
        isModifying.set(true);
        if(rootDirectory != null && !rootDirectory.checkForUniqueDirectoryName(this, name)) {
            isModifying.set(false);
            locks.forEach(Lock::unlock);
            throw new NotUniqueName();
        }

        super.rename(name);
        locks.forEach(Lock::unlock);
        save();
        isModifying.set(false);
    }

    public List<VirtualDirectory> getDirectories() throws LockedVirtualFSNode {
        Lock lock = tryReadLockDirectories();
        List<VirtualDirectory> directories = this.directories;
        lock.unlock();
        save();
        return directories;
    }

    public List<VirtualFile> getFiles() throws LockedVirtualFSNode {
        Lock lock = tryReadLockFiles();
        List<VirtualFile> files = this.files;
        lock.unlock();
        return files;
    }

    public VirtualDirectory mkdir(@NotNull String name) throws LockedVirtualFSNode, NotUniqueName {
        Lock lock = tryWriteLockDirectories();
        isModifying.set(true);
        if(!checkForUniqueDirectoryName(this, name)) {
            isModifying.set(false);
            lock.unlock();
            throw new NotUniqueName();
        }
        VirtualDirectory newDirectory;
        try {
            newDirectory = new VirtualDirectory(name, this);
            directories.add(newDirectory);
        } finally {
            isModifying.set(false);
            lock.unlock();
            save();
        }
        return newDirectory;
    }

    public VirtualFile touch(@NotNull String name) throws LockedVirtualFSNode, NotUniqueName {
        Lock lock = tryWriteLockFiles();
        isModifying.set(true);
        if(!checkForUniqueFileName(name)) {
            isModifying.set(false);
            lock.unlock();
            throw new NotUniqueName();
        }

        VirtualFile newFile;
        try {
            newFile = new VirtualFile(name, this);
            files.add(newFile);
        } finally {
            isModifying.set(false);
            lock.unlock();
            save();
        }
        return newFile;
    }

    @Override
    public void remove()
            throws NullVirtualFS, UnremovableVirtualNode, LockedVirtualFSNode,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeleted {
        remove(false, true);
    }

    void remove(boolean isLocked, boolean deleteFromRoot)
            throws UnremovableVirtualNode, OverlappingVirtualFileLockException,
            IOException, NullVirtualFS, LockedVirtualFSNode, VirtualFSNodeIsDeleted {
        super.remove();
        List<Lock> locks = new ArrayList<>();
        if(!isLocked) {
            locks = tryWriteLockDown();
            try {
                locks.add(rootDirectory.tryWriteLockDirectories());
            } catch (LockedVirtualFSNode e) {
                locks.forEach(Lock::unlock);
                throw e;
            }
        }
        isModifying.set(true);
        for (VirtualDirectory directory : this.directories) {
            directory.remove(true, false);
        }
        for (VirtualFile file : this.files) {
            file.remove(true, false);
        }

        directories = new ArrayList<>();
        files = new ArrayList<>();

        locks.forEach(Lock::unlock);

        if(deleteFromRoot) {
            rootDirectory.remove(this);
        }
        this.isDeleted = true;
        isModifying.set(false);
        save();
    }

    void remove(@NotNull VirtualFile file) {
        files.remove(file);
        file.rootDirectory = null;
    }

    void remove(@NotNull VirtualDirectory directory) {
        directories.remove(directory);
        directory.rootDirectory = null;
    }

    void paste(@NotNull VirtualDirectory virtualDirectory){
        directories.add(virtualDirectory);
        virtualDirectory.rootDirectory = this;
    }

    void paste(@NotNull VirtualFile virtualFile) {
        files.add(virtualFile);
        virtualFile.rootDirectory = this;
    }

    @Override
    public void move(@NotNull VirtualDirectory destinationDirectory)
            throws LockedVirtualFSNode, NotUniqueName, UnremovableVirtualNode {
        if(rootDirectory == null) {
            throw new UnremovableVirtualNode();
        }
        List<Lock> locks = tryWriteLockDown();
        try {
            locks.add(rootDirectory.tryWriteLockDirectories());
            locks.add(destinationDirectory.tryWriteLockDirectories());
        } catch (LockedVirtualFSNode e) {
            locks.forEach(Lock::unlock);
            throw e;
        }
        rootDirectory.isModifying.set(true);
        if(!destinationDirectory.checkForUniqueDirectoryName(this)) {
            locks.forEach(Lock::unlock);
            throw new NotUniqueName();
        }
        rootDirectory.remove(this);
        destinationDirectory.paste(this);
        locks.forEach(Lock::unlock);
        save();
    }

    VirtualDirectory clone(@NotNull VirtualDirectory destinationDirectory)
            throws NullVirtualFS, LockedVirtualFSNode, OverlappingVirtualFileLockException,
            IOException, VirtualFSNodeIsDeleted, NotUniqueName {
        VirtualDirectory clonedDirectory = new VirtualDirectory(
                name,
                destinationDirectory
        );

        for (VirtualDirectory directory : directories) {
            clonedDirectory.paste(directory.clone(clonedDirectory));
        }
        for (VirtualFile file : files) {
            clonedDirectory.paste(file.clone(clonedDirectory));
        }

        return clonedDirectory;
    }


    public VirtualDirectory copy(@NotNull VirtualDirectory destinationDirectory)
            throws LockedVirtualFSNode, NullVirtualFS,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeleted, NotUniqueName {
        List<Lock> locks = tryReadLockDown();
        destinationDirectory.isModifying.set(true);
        try {
            locks.add(tryLockNameRead());
            locks.add(rootDirectory.tryReadLockDirectories());
            locks.add(destinationDirectory.tryWriteLockDirectories());
        } catch (LockedVirtualFSNode e) {
            destinationDirectory.isModifying.set(false);
            locks.forEach(Lock::unlock);
            throw e;
        }
        VirtualDirectory copiedDirectory = clone(destinationDirectory);
        destinationDirectory.paste(copiedDirectory);
        locks.forEach(Lock::unlock);
        save();
        destinationDirectory.isModifying.set(false);
        return copiedDirectory;
    }

    Iterator<VirtualFile> find(
            @NotNull Predicate<VirtualFile> match,
            boolean isRecursive) {
        return new VirtualFileIterator(match, isRecursive, this);
    }

    public Iterator<VirtualFile> find(@NotNull String subName, boolean isRecursive) {
        return find((VirtualFile file) -> file.getName().contains(subName), isRecursive);
    }

    public Iterator<VirtualFile> find(@NotNull String subName) {
        return find(subName, false);
    }

    public Iterator<VirtualFile> find(@NotNull Pattern pattern, boolean isRecursive) {
        return find((VirtualFile file) -> pattern.matcher(file.getName()).matches(), isRecursive);
    }

    public  Iterator<VirtualFile> find(@NotNull Pattern pattern) {
        return find(pattern, false);
    }

    Lock tryWriteLockDirectories() throws LockedVirtualFSNode {
        if(directoriesReadWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNode();
        }
        Lock lock = directoriesReadWriteLock.writeLock();
        if(!lock.tryLock()) {
            throw new LockedVirtualFSNode();
        }
        return lock;
    }

    Lock tryReadLockDirectories() throws LockedVirtualFSNode {
        if(directoriesReadWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNode();
        }
        Lock lock = directoriesReadWriteLock.readLock();
        if(!lock.tryLock()) {
            throw new LockedVirtualFSNode();
        }
        return lock;
    }

    Lock tryWriteLockFiles() throws LockedVirtualFSNode {
        if(filesReadWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNode();
        }
        Lock lock = filesReadWriteLock.writeLock();
        if(!lock.tryLock()) {
            throw new LockedVirtualFSNode();
        }
        return lock;
    }

    Lock tryReadLockFiles() throws LockedVirtualFSNode {
        if(filesReadWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNode();
        }
        Lock lock = filesReadWriteLock.readLock();
        if(!lock.tryLock()) {
            throw new LockedVirtualFSNode();
        }
        return lock;
    }

    List<Lock> tryWriteLock() throws LockedVirtualFSNode {
        List<Lock> locks = new ArrayList<>();
        if(nameLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNode();
        }

        try {
            locks.add(tryLockNameWrite());
            locks.add(tryWriteLockFiles());
            locks.add(tryWriteLockDirectories());
        } catch (LockedVirtualFSNode e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        return locks;
    }

    List<Lock> tryReadLock() throws LockedVirtualFSNode {
        List<Lock> locks = new ArrayList<>();

        if(nameLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNode();
        }

        try {
            locks.add(tryLockNameRead());
            locks.add(tryReadLockFiles());
            locks.add(tryReadLockDirectories());
        } catch (LockedVirtualFSNode e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        return locks;
    }

    List<Lock> tryWriteLockDown() throws LockedVirtualFSNode {
        return tryWriteLockDown(new ArrayList<>());
    }

    List<Lock> tryWriteLockDown(List<Lock> locks) throws LockedVirtualFSNode {
        try {
            locks.addAll(tryWriteLock());
        } catch (LockedVirtualFSNode e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        for (VirtualDirectory directory : directories) {
            locks = directory.tryWriteLockDown(locks);
        }

        for (VirtualFile file : files) {
            try {
                locks.add(file.tryWriteLock());
            } catch (LockedVirtualFSNode e) {
                locks.forEach(Lock::unlock);
                throw e;
            }
        }

        return locks;
    }

    List<Lock> tryReadLockDown() throws LockedVirtualFSNode {
        return tryReadLockDown(new ArrayList<>());
    }

    List<Lock> tryReadLockDown(@NotNull List<Lock> locks) throws LockedVirtualFSNode {
        try {
            locks.addAll(tryReadLock());
        } catch (LockedVirtualFSNode e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        for (VirtualDirectory directory : directories) {
            locks = directory.tryReadLockDown(locks);
        }

        for (VirtualFile file : files) {
            try {
                locks.add(file.tryReadLock());
            } catch (LockedVirtualFSNode e) {
                locks.forEach(Lock::unlock);
                throw e;
            }
        }

        return locks;
    }

    Lock tryLockNameWrite() throws LockedVirtualFSNode {
        Lock lock = nameLock.writeLock();

        if(!lock.tryLock()) {
            throw new LockedVirtualFSNode();
        }

        return lock;
    }

    Lock tryLockNameRead() throws LockedVirtualFSNode {
        Lock lock = nameLock.readLock();

        if(!lock.tryLock()) {
            throw new LockedVirtualFSNode();
        }

        return lock;
    }

    List<Lock> tryLockWriteFilesDirectories(@NotNull List<Lock> locks) throws LockedVirtualFSNode {
        try {
            locks.add(tryWriteLockDirectories());
        } catch (LockedVirtualFSNode e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        try {
            locks.add(tryWriteLockFiles());
        } catch (LockedVirtualFSNode e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        return locks;
    }

    public void importContent(@NotNull VirtualDirectory directory)
            throws LockedVirtualFSNode, NullVirtualFS,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeleted, NotUniqueName {
        List<Lock> locks = directory.tryReadLockDown();
        locks = tryLockWriteFilesDirectories(locks);

        for (VirtualDirectory virtualDirectory : directory.directories) {
            if(!checkForUniqueDirectoryName(virtualDirectory.getName())) {
                throw new NotUniqueName();
            }
            paste(virtualDirectory.clone(this));
        }

        for (VirtualFile virtualFile : directory.files) {
            if(!checkForUniqueFileName(virtualFile.getName())) {
                throw new NotUniqueName();
            }
            paste(virtualFile.clone(this));
        }

        locks.forEach(Lock::unlock);
        save();
    }

    public void importContent(@NotNull File folder)
            throws LockedVirtualFSNode, NullVirtualFS,
            OverlappingVirtualFileLockException, IOException, NotUniqueName {
        if (!folder.isDirectory()) {
            throw new InvalidObjectException(String.format("File is not a directory: %s", folder.getAbsolutePath()));
        }
        List<Lock> locks = new ArrayList<>();
        locks = tryLockWriteFilesDirectories(locks);

        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                if(!checkForUniqueDirectoryName(fileEntry.getName())) {
                    throw new NotUniqueName();
                }
                VirtualDirectory directory = new VirtualDirectory(fileEntry.getName());
                paste(directory);
                directory.importContent(fileEntry);
            } else {
                if(!checkForUniqueFileName(fileEntry.getName())) {
                    throw new NotUniqueName();
                }
                VirtualFile file = new VirtualFile(fileEntry.getName());
                paste(file);
                VirtualRandomAccessFile virtualRandomAccessFile = file.open("rw");
                RandomAccessFile randomAccessFile = new RandomAccessFile(fileEntry,"r");
                byte[] b = new byte[(int) randomAccessFile.length()];
                randomAccessFile.read(b);
                randomAccessFile.close();
                virtualRandomAccessFile.write(b);
                virtualRandomAccessFile.close();
            }
        }

        locks.forEach(Lock::unlock);
        save();
    }

    public void exportContent(@NotNull File folder) throws IOException, LockedVirtualFSNode,
            NullVirtualFS, OverlappingVirtualFileLockException {
        if (!folder.isDirectory()) {
            throw new InvalidObjectException(String.format("File is not a directory: %s", folder.getAbsolutePath()));
        }

        List<Lock> locks = new ArrayList<>();
        locks = tryReadLockDown(locks);

        for (VirtualDirectory directory : directories) {
            File newDirectory = new File(folder, directory.getName());
            if(newDirectory.mkdir()) {
                directory.exportContent(newDirectory);
            }
        }

        for(VirtualFile file : files) {
            File newFile = new File(folder, file.getName());
            OutputStream out = new FileOutputStream(newFile);
            VirtualRandomAccessFile virtualRandomAccessFile = file.open("r");
            byte[] b = new byte[(int) virtualRandomAccessFile.length()];
            virtualRandomAccessFile.read(b);
            virtualRandomAccessFile.close();
            out.write(b);
            out.close();
        }

        locks.forEach(Lock::unlock);
    }

    protected void save() {
        try {
            if (rootDirectory != null) {
                rootDirectory.save();
                return;
            }
            if(virtualFS == null) {
                return;
            }

            List<Lock> locks = tryReadLockDown();

            getVirtualFS().save();

            locks.forEach(Lock::unlock);
        } catch (Throwable throwable) {
            return;
        }
    }

    boolean checkForUniqueDirectoryName(
            @NotNull String name) {
        for(VirtualDirectory directory : directories) {
            if(Objects.equals(directory.getName(), name)) {
                return false;
            }
        }
        return true;
    }

    boolean checkForUniqueDirectoryName(
            @NotNull VirtualDirectory virtualDirectory,
            @NotNull String name) {
        for(VirtualDirectory directory : directories) {
            if(directory != virtualDirectory && Objects.equals(directory.getName(), name)) {
                return false;
            }
        }
        return true;
    }

    boolean checkForUniqueDirectoryName(@NotNull VirtualDirectory virtualDirectory) {
        return checkForUniqueDirectoryName(virtualDirectory, virtualDirectory.getName());
    }

    boolean checkForUniqueFileName(
            @NotNull String name) {
        for(VirtualFile file : files) {
            if(Objects.equals(file.getName(), name)) {
                return false;
            }
        }
        return true;
    }

    boolean checkForUniqueFileName(
            @NotNull VirtualFile virtualFile,
            @NotNull String name) {
        for(VirtualFile file : files) {
            if(file != virtualFile && Objects.equals(file.getName(), name)) {
                return false;
            }
        }
        return true;
    }
}

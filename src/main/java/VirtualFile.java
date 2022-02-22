import exceptions.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VirtualFile extends VirtualFSNode implements Serializable {
    final private Date createdAt;
    private Date modifiedAt;
    private long contentPosition;

    transient private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public VirtualFile(@NotNull String name) {
        this(name, null, -1);
    }

    public VirtualFile(@NotNull String name, @NotNull VirtualDirectory rootDirectory) {
        this(name, rootDirectory, -1);
    }

    protected VirtualFile(
            @NotNull String name,
            VirtualDirectory rootDirectory,
            long contentPosition) {
        super(name, rootDirectory);
        this.contentPosition = contentPosition;
        this.createdAt = new Date();
        this.modifiedAt = this.createdAt;
    }

    private void readObject(@NotNull ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        readWriteLock = new ReentrantReadWriteLock();
        isDeleted = false;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    @Override
    public void rename(@NotNull String name) throws LockedVirtualFSNode, VirtualFSNodeIsDeleted, NotUniqueName {
        if(isDeleted) throw new VirtualFSNodeIsDeleted();
        List<Lock> locks = new ArrayList<>();
        try {
            locks.add(tryWriteLock());
            if (rootDirectory != null) {
                rootDirectory.isModifying.set(true);
                locks.add(rootDirectory.tryWriteLockFiles());
            }
        } catch (LockedVirtualFSNode e) {
            if (rootDirectory != null) {
                rootDirectory.isModifying.set(false);
            }
            locks.forEach(Lock::unlock);
            throw e;
        }
        if(rootDirectory != null && !rootDirectory.checkForUniqueFileName(this, name)) {
            rootDirectory.isModifying.set(false);
            locks.forEach(Lock::unlock);
            throw new NotUniqueName();
        }
        super.rename(name);
        modifiedAt = new Date();
        locks.forEach(Lock::unlock);
        if(rootDirectory != null) rootDirectory.isModifying.set(false);
        if(rootDirectory != null) rootDirectory.save();
    }

    @Override
    public void remove()
            throws UnremovableVirtualNode, OverlappingVirtualFileLockException,
            IOException, NullVirtualFS, LockedVirtualFSNode, VirtualFSNodeIsDeleted {
        remove(false, true);
    }

    void remove(boolean isLocked, boolean deleteFromRoot)
            throws UnremovableVirtualNode, OverlappingVirtualFileLockException,
            IOException, NullVirtualFS, LockedVirtualFSNode, VirtualFSNodeIsDeleted {
        super.remove();
        List<Lock> locks = new ArrayList<>();
        if(!isLocked) {
            locks.add(tryWriteLock());
            try {
                locks.add(rootDirectory.tryWriteLockFiles());
            } catch (LockedVirtualFSNode e) {
                locks.forEach(Lock::unlock);
                throw e;
            }
        }
        if(rootDirectory != null)rootDirectory.isModifying.set(true);
        VirtualRandomAccessFile randomAccessFile = openLocked("rw");
        randomAccessFile.setLength(0);
        randomAccessFile.close();
        if(deleteFromRoot) {
            rootDirectory.remove(this);
        }
        isDeleted = true;
        locks.forEach(Lock::unlock);
        if(rootDirectory != null) rootDirectory.isModifying.set(false);
        if(rootDirectory != null) rootDirectory.save();
    }

    @Override
    public void move(@NotNull VirtualDirectory destinationDirectory)
            throws LockedVirtualFSNode, VirtualFSNodeIsDeleted, NotUniqueName {
        if(isDeleted) throw new VirtualFSNodeIsDeleted();
        Lock lock = tryWriteLock();
        Lock directoryLock;
        try {
            directoryLock = destinationDirectory.tryWriteLockFiles();
        } catch (LockedVirtualFSNode e) {
            lock.unlock();
            throw e;
        }
        destinationDirectory.isModifying.set(true);
        if(rootDirectory != null) {
            rootDirectory.isModifying.set(true);
            if(!destinationDirectory.checkForUniqueFileName(name)) {
                rootDirectory.isModifying.set(false);
                destinationDirectory.isModifying.set(false);
                lock.unlock();
                directoryLock.unlock();
                throw new NotUniqueName();
            }
        }
        if(rootDirectory != null) rootDirectory.remove(this);
        destinationDirectory.paste(this);
        lock.unlock();
        directoryLock.unlock();
        if(rootDirectory != null) rootDirectory.isModifying.set(false);
        destinationDirectory.isModifying.set(false);
        if(rootDirectory != null) rootDirectory.save();
    }

    VirtualFile clone(@NotNull VirtualDirectory destinationDirectory)
            throws NullVirtualFS, LockedVirtualFSNode, OverlappingVirtualFileLockException,
            IOException, VirtualFSNodeIsDeleted {
        if(isDeleted) throw new VirtualFSNodeIsDeleted();
        VirtualFile clonedFile = new VirtualFile(
                this.name,
                destinationDirectory
        );

        if(contentPosition != -1) {
            VirtualRandomAccessFile randomAccessFile = this.open("r");
            byte[] bytes = new byte[(int) randomAccessFile.length()];
            randomAccessFile.read(bytes);
            randomAccessFile.close();

            randomAccessFile = clonedFile.open("rw");
            randomAccessFile.write(bytes);
            randomAccessFile.close();
        }

        return clonedFile;
    }

    public VirtualFile copy(@NotNull VirtualDirectory destinationDirectory)
            throws NullVirtualFS, LockedVirtualFSNode, OverlappingVirtualFileLockException,
            IOException, VirtualFSNodeIsDeleted, NotUniqueName {
        Lock lock = tryReadLock();
        Lock directoryLock;
        try {
            directoryLock = destinationDirectory.tryWriteLockFiles();
        } catch (LockedVirtualFSNode e) {
            lock.unlock();
            throw e;
        }
        destinationDirectory.isModifying.set(true);
        VirtualFile copiedFile = this.clone(destinationDirectory);
        destinationDirectory.paste(copiedFile);
        lock.unlock();
        directoryLock.unlock();
        destinationDirectory.isModifying.set(false);
        if(rootDirectory != null) rootDirectory.save();
        return copiedFile;
    }

    Lock tryWriteLock() throws LockedVirtualFSNode {
        if(readWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNode();
        }
        Lock lock = readWriteLock.writeLock();
        boolean isLocked = lock.tryLock();
        if(!isLocked) {
            throw new LockedVirtualFSNode();
        }
        return lock;
    }

    Lock tryReadLock() throws LockedVirtualFSNode {
        if(readWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNode();
        }
        Lock lock = readWriteLock.readLock();
        boolean isLocked = lock.tryLock();
        if(!isLocked) {
            throw new LockedVirtualFSNode();
        }
        return lock;
    }

    public VirtualRandomAccessFile open(@NotNull String mode) throws IOException, OverlappingVirtualFileLockException,
            NullVirtualFS, LockedVirtualFSNode {
        Lock lock;

        switch (mode) {
            case "r": {
                lock = tryReadLock();
                break;
            }
            case "rw": {
                lock = tryWriteLock();
                break;
            }
            default: {
                throw new IllegalArgumentException("Illegal mode \"" + mode + "\" must be one of " + "\"r\", \"rw\"");
            }
        }

        VirtualRandomAccessFileCloseListener onClose = (firstBlockPosition) -> {
            lock.unlock();
            contentPosition = firstBlockPosition;
            if(mode.equals("rw")) {
                rootDirectory.isModifying.set(false);
            }
        };

        ModifiedListener onModify = () -> {
            modifiedAt = new Date();
            rootDirectory.save();
        };

        if(mode.equals("rw")) {
            rootDirectory.isModifying.set(true);
        }

        return new VirtualRandomAccessFile(
                getSourceFile(),
                mode,
                contentPosition,
                onClose,
                onModify
        );
    }

    private VirtualRandomAccessFile openLocked(@NotNull String mode) throws IOException, NullVirtualFS {
        if(!mode.equals("r") && !mode.equals("rw")) {
            throw new IllegalArgumentException("Illegal mode \"" + mode + "\" must be one of " + "\"r\", \"rw\"");
        }

        VirtualRandomAccessFileCloseListener onClose = (firstBlockPosition) -> {
            contentPosition = firstBlockPosition;
            if(mode.equals("rw")) {
                rootDirectory.isModifying.set(false);
            }
        };

        ModifiedListener onModify = () -> modifiedAt = new Date();

        if(mode.equals("rw")) {
            rootDirectory.isModifying.set(true);
        }

        return new VirtualRandomAccessFile(
                getSourceFile(),
                mode,
                contentPosition,
                onClose,
                onModify
        );
    }
}

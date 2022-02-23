import exceptions.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
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

    public VirtualFile(@NotNull String name) throws EmptyNodeNameException {
        this(name, null, -1);
    }

    public VirtualFile(@NotNull String name, @NotNull VirtualDirectory rootDirectory) throws EmptyNodeNameException {
        this(name, rootDirectory, -1);
    }

    protected VirtualFile(@NotNull String name, VirtualDirectory rootDirectory, long contentPosition) throws EmptyNodeNameException {
        super(name, rootDirectory);
        this.contentPosition = contentPosition;
        this.createdAt = new Date();
        this.modifiedAt = this.createdAt;
    }

    protected VirtualFile(@NotNull String name, VirtualDirectory rootDirectory, long contentPosition, @NotNull Date createdAt, @NotNull Date modifiedAt) throws EmptyNodeNameException {
        super(name, rootDirectory);
        this.contentPosition = contentPosition;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    private void readObject(@NotNull ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        readWriteLock = new ReentrantReadWriteLock();
        isDeleted = false;
    }

    /**
     * Получение даты создания файла
     *
     * @return дата создания файла
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Получение даты последней модификации файла
     *
     * @return дата последней модификации файла
     */
    public Date getModifiedAt() {
        return modifiedAt;
    }

    /**
     * Переимернование файла
     *
     * @param name новое имя файла
     */
    @Override
    public void rename(@NotNull String name) throws VirtualFSNodeIsDeletedException, NotUniqueNameException, EmptyNodeNameException, LockedVirtualFSNodeException {
        if (isDeleted) throw new VirtualFSNodeIsDeletedException();
        List<Lock> locks = new ArrayList<>();
        try {
            locks.add(tryWriteLock());
            if (rootDirectory != null) {
                rootDirectory.isModifying.set(true);
                locks.add(rootDirectory.tryWriteLockFiles());
            }
        } catch (LockedVirtualFSNodeException e) {
            if (rootDirectory != null) {
                rootDirectory.isModifying.set(false);
            }
            locks.forEach(Lock::unlock);
            throw e;
        }
        if (rootDirectory != null && !rootDirectory.checkForUniqueFileName(this, name)) {
            rootDirectory.isModifying.set(false);
            locks.forEach(Lock::unlock);
            throw new NotUniqueNameException();
        }
        super.rename(name);
        modifiedAt = new Date();
        locks.forEach(Lock::unlock);
        if (rootDirectory != null) rootDirectory.isModifying.set(false);
        if (rootDirectory != null) rootDirectory.save();
    }

    /**
     * Удаление файла
     * Удаление данных файла из физического файла
     */
    @Override
    public void remove() throws UnremovableVirtualNodeException, OverlappingVirtualFileLockException, IOException, NullVirtualFSException, LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException {
        remove(false, true);
    }

    /**
     * Удаление файла
     * Удаление данных файла из физического файла
     */
    void remove(boolean isLocked, boolean deleteFromRoot) throws UnremovableVirtualNodeException, OverlappingVirtualFileLockException, IOException, NullVirtualFSException, LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException {
        super.remove();
        List<Lock> locks = new ArrayList<>();
        if (!isLocked) {
            locks.add(tryWriteLock());
            try {
                locks.add(rootDirectory.tryWriteLockFiles());
            } catch (LockedVirtualFSNodeException e) {
                locks.forEach(Lock::unlock);
                throw e;
            }
        }
        if (rootDirectory != null) rootDirectory.isModifying.set(true);
        VirtualRandomAccessFile randomAccessFile = openLocked("rw");
        randomAccessFile.setLength(0);
        randomAccessFile.close();
        if (deleteFromRoot) {
            rootDirectory.remove(this);
        }
        isDeleted = true;
        locks.forEach(Lock::unlock);
        if (rootDirectory != null) rootDirectory.isModifying.set(false);
        if (rootDirectory != null) rootDirectory.save();
    }

    /**
     * Перемещение файла в destinationDirectory
     */
    @Override
    public void move(@NotNull VirtualDirectory destinationDirectory) throws LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException, NotUniqueNameException {
        if (isDeleted) throw new VirtualFSNodeIsDeletedException();
        Lock lock = tryWriteLock();
        Lock directoryLock;
        try {
            directoryLock = destinationDirectory.tryWriteLockFiles();
        } catch (LockedVirtualFSNodeException e) {
            lock.unlock();
            throw e;
        }
        destinationDirectory.isModifying.set(true);
        if (rootDirectory != null) {
            rootDirectory.isModifying.set(true);
            if (!destinationDirectory.checkForUniqueFileName(name)) {
                rootDirectory.isModifying.set(false);
                destinationDirectory.isModifying.set(false);
                lock.unlock();
                directoryLock.unlock();
                throw new NotUniqueNameException();
            }
        }
        if (rootDirectory != null) rootDirectory.remove(this);
        destinationDirectory.paste(this);
        lock.unlock();
        directoryLock.unlock();
        if (rootDirectory != null) rootDirectory.isModifying.set(false);
        destinationDirectory.isModifying.set(false);
        if (rootDirectory != null) rootDirectory.save();
    }

    /**
     * Создание клона файла с указанием destinationDirectory в качетсве root директории
     */
    VirtualFile clone(@NotNull VirtualDirectory destinationDirectory) throws NullVirtualFSException, LockedVirtualFSNodeException, OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeletedException, EmptyNodeNameException {
        if (isDeleted) throw new VirtualFSNodeIsDeletedException();
        VirtualFile clonedFile = new VirtualFile(this.name, destinationDirectory, -1, createdAt, modifiedAt);

        if (contentPosition != -1) {
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

    /**
     * Копирование файла в destinationDirectory
     *
     * @return скопированный файл
     */
    public VirtualFile copy(@NotNull VirtualDirectory destinationDirectory) throws NullVirtualFSException, LockedVirtualFSNodeException, OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeletedException, NotUniqueNameException, EmptyNodeNameException {
        Lock lock = tryReadLock();
        Lock directoryLock;
        try {
            directoryLock = destinationDirectory.tryWriteLockFiles();
        } catch (LockedVirtualFSNodeException e) {
            lock.unlock();
            throw e;
        }
        destinationDirectory.isModifying.set(true);
        if (!destinationDirectory.checkForUniqueFileName(name)) {
            lock.unlock();
            destinationDirectory.isModifying.set(false);
            throw new NotUniqueNameException();
        }
        VirtualFile copiedFile = this.clone(destinationDirectory);
        destinationDirectory.paste(copiedFile);
        lock.unlock();
        directoryLock.unlock();
        destinationDirectory.isModifying.set(false);
        if (rootDirectory != null) rootDirectory.save();
        return copiedFile;
    }

    /**
     * Блокировка данного файла на запись
     *
     * @return полученная блокировка
     */
    Lock tryWriteLock() throws LockedVirtualFSNodeException {
        if (readWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNodeException();
        }
        Lock lock = readWriteLock.writeLock();
        boolean isLocked = lock.tryLock();
        if (!isLocked) {
            throw new LockedVirtualFSNodeException();
        }
        return lock;
    }

    /**
     * Блокировка данного файла на чтение
     *
     * @return полученная блокировка
     */
    Lock tryReadLock() throws LockedVirtualFSNodeException {
        if (readWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNodeException();
        }
        Lock lock = readWriteLock.readLock();
        boolean isLocked = lock.tryLock();
        if (!isLocked) {
            throw new LockedVirtualFSNodeException();
        }
        return lock;
    }

    /**
     * Открытие файла на чтение или чтение/запись
     */
    public VirtualRandomAccessFile open(@NotNull String mode) throws IOException, OverlappingVirtualFileLockException, NullVirtualFSException, LockedVirtualFSNodeException {
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

        VirtualRandomAccessFileListener onClose = new VirtualRandomAccessFileListener() {
            @Override
            public void onClose(long firstBlockPosition) {
                lock.unlock();
                contentPosition = firstBlockPosition;
                if (mode.equals("rw")) {
                    rootDirectory.isModifying.set(false);
                }
            }

            @Override
            public void onModify() {
                modifiedAt = new Date();
                rootDirectory.save();
            }
        };


        if (mode.equals("rw")) {
            rootDirectory.isModifying.set(true);
        }

        return new VirtualRandomAccessFile(getSourceFile(), mode, contentPosition, onClose);
    }

    /**
     * Открытие файла, файл уже заблокирован
     */
    private VirtualRandomAccessFile openLocked(@NotNull String mode) throws IOException, NullVirtualFSException {
        if (!mode.equals("r") && !mode.equals("rw")) {
            throw new IllegalArgumentException("Illegal mode \"" + mode + "\" must be one of " + "\"r\", \"rw\"");
        }

        VirtualRandomAccessFileListener onClose = new VirtualRandomAccessFileListener() {
            @Override
            public void onClose(long firstBlockPosition) {
                contentPosition = firstBlockPosition;
                if (mode.equals("rw")) {
                    rootDirectory.isModifying.set(false);
                }
            }

            @Override
            public void onModify() {
                modifiedAt = new Date();
            }
        };

        if (mode.equals("rw")) {
            rootDirectory.isModifying.set(true);
        }

        return new VirtualRandomAccessFile(getSourceFile(), mode, contentPosition, onClose);
    }
}

import exceptions.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class VirtualDirectory extends VirtualFSNode implements Serializable {
    transient AtomicBoolean isModifying = new AtomicBoolean(false);
    private List<VirtualDirectory> directories;
    private List<VirtualFile> files;

    transient private ReentrantReadWriteLock directoriesReadWriteLock = new ReentrantReadWriteLock();
    transient private ReentrantReadWriteLock filesReadWriteLock = new ReentrantReadWriteLock();
    transient private ReentrantReadWriteLock nameLock = new ReentrantReadWriteLock();

    public VirtualDirectory(String name) throws EmptyNodeNameException {
        this(name, null);
    }

    protected VirtualDirectory(@NotNull String name, VirtualDirectory rootDirectory) throws EmptyNodeNameException {
        this(name, rootDirectory, null);
    }

    protected VirtualDirectory(@NotNull String name, VirtualDirectory rootDirectory, VirtualFS virtualFS)
            throws EmptyNodeNameException {
        super(name, rootDirectory);
        this.directories = new ArrayList<>();
        this.files = new ArrayList<>();
        this.virtualFS = virtualFS;
    }

    boolean isModifying() {
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

    /**
     * Переименование директории
     */
    @Override
    public void rename(@NotNull String name) throws LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException,
            NotUniqueNameException, EmptyNodeNameException {
        List<Lock> locks = new ArrayList<>();
        try {
            locks.add(tryLockNameWrite());
            if (rootDirectory != null) locks.add(rootDirectory.tryWriteLockDirectories());
        } catch (LockedVirtualFSNodeException e) {
            locks.forEach(Lock::unlock);
            throw e;
        }
        isModifying.set(true);
        if (rootDirectory != null && !rootDirectory.checkForUniqueDirectoryName(this, name)) {
            isModifying.set(false);
            locks.forEach(Lock::unlock);
            throw new NotUniqueNameException();
        }
        try {
            super.rename(name);
        } finally {
            locks.forEach(Lock::unlock);
            save();
            isModifying.set(false);
        }
    }

    /**
     * Получение списка директорий в текущей директории
     */
    public List<VirtualDirectory> getDirectories() throws LockedVirtualFSNodeException {
        Lock lock = tryReadLockDirectories();
        List<VirtualDirectory> directories = this.directories;
        lock.unlock();
        save();
        return directories;
    }

    /**
     * Получение списка файлов в текущей директории
     */
    public List<VirtualFile> getFiles() throws LockedVirtualFSNodeException {
        Lock lock = tryReadLockFiles();
        List<VirtualFile> files = this.files;
        lock.unlock();
        return files;
    }

    /**
     * Создание директории в текущей директории
     *
     * @param name имя создаваемой директории
     * @return созданная директория
     */
    public VirtualDirectory mkdir(@NotNull String name) throws LockedVirtualFSNodeException,
            NotUniqueNameException, EmptyNodeNameException {
        Lock lock = tryWriteLockDirectories();
        isModifying.set(true);
        if (checkForUniqueDirectoryName(this, name)) {
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
        } else {
            isModifying.set(false);
            lock.unlock();
            throw new NotUniqueNameException();
        }
    }

    /**
     * Создание файла в текущей директории
     *
     * @param name имя создаваемого файла
     * @return созданный файл
     */
    public VirtualFile touch(@NotNull String name) throws LockedVirtualFSNodeException,
            NotUniqueNameException, EmptyNodeNameException {
        Lock lock = tryWriteLockFiles();
        isModifying.set(true);
        if (checkForUniqueFileName(name)) {
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
        } else {
            isModifying.set(false);
            lock.unlock();
            throw new NotUniqueNameException();
        }
    }

    /**
     * Удаление директории
     * Рекрсивное удаление
     * Удаляются все файлы, их данные удаляются из физического файла
     */
    @Override
    public void remove() throws NullVirtualFSException, UnremovableVirtualNodeException,
            LockedVirtualFSNodeException, OverlappingVirtualFileLockException, IOException,
            VirtualFSNodeIsDeletedException {
        remove(false, true);
    }

    /**
     * Удаление текущей директории
     * Рекрсивное удаление
     * Удаляются все файлы, их данные удаляются из физического файла
     */
    private void remove(boolean isLocked, boolean deleteFromRoot) throws UnremovableVirtualNodeException,
            OverlappingVirtualFileLockException, IOException, NullVirtualFSException,
            LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException {
        super.remove();
        List<Lock> locks = new ArrayList<>();
        if (!isLocked) {
            locks = tryWriteLockDown();
            try {
                locks.add(rootDirectory.tryWriteLockDirectories());
            } catch (LockedVirtualFSNodeException e) {
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

        if (deleteFromRoot) {
            rootDirectory.remove(this);
        }
        this.isDeleted = true;
        isModifying.set(false);
        save();
    }

    /**
     * Удаление файла file из текущей директории
     */
    void remove(@NotNull VirtualFile file) {
        files.remove(file);
        file.rootDirectory = null;
    }

    /**
     * Удаление директории directory из текущей директории
     */
    void remove(@NotNull VirtualDirectory directory) {
        directories.remove(directory);
        directory.rootDirectory = null;
    }

    /**
     * Вставка директории virtualDirectory в текущую диреторию
     */
    void paste(@NotNull VirtualDirectory virtualDirectory) {
        directories.add(virtualDirectory);
        virtualDirectory.rootDirectory = this;
    }

    /**
     * Вставка файла virtualFile в текущую диреторию
     */
    void paste(@NotNull VirtualFile virtualFile) {
        files.add(virtualFile);
        virtualFile.rootDirectory = this;
    }

    /**
     * Перемещение диектории в destinationDirectory
     */
    @Override
    public void move(@NotNull VirtualDirectory destinationDirectory) throws LockedVirtualFSNodeException,
            NotUniqueNameException, UnremovableVirtualNodeException {
        if (rootDirectory == null) {
            throw new UnremovableVirtualNodeException();
        }
        List<Lock> locks = tryWriteLockDown();
        try {
            locks.add(rootDirectory.tryWriteLockDirectories());
            locks.add(destinationDirectory.tryWriteLockDirectories());
        } catch (LockedVirtualFSNodeException e) {
            locks.forEach(Lock::unlock);
            throw e;
        }
        rootDirectory.isModifying.set(true);
        if (!destinationDirectory.checkForUniqueDirectoryName(this)) {
            locks.forEach(Lock::unlock);
            rootDirectory.isModifying.set(false);
            throw new NotUniqueNameException();
        }
        rootDirectory.remove(this);
        destinationDirectory.paste(this);
        rootDirectory.isModifying.set(false);
        locks.forEach(Lock::unlock);
        save();
    }

    /**
     * Создание клона директории с указанием destinationDirectory в качетсве root директории
     */
    VirtualDirectory clone(@NotNull VirtualDirectory destinationDirectory) throws NullVirtualFSException,
            LockedVirtualFSNodeException, OverlappingVirtualFileLockException, IOException,
            VirtualFSNodeIsDeletedException, EmptyNodeNameException {
        VirtualDirectory clonedDirectory = new VirtualDirectory(name, destinationDirectory);

        for (VirtualDirectory directory : directories) {
            clonedDirectory.paste(directory.clone(clonedDirectory));
        }
        for (VirtualFile file : files) {
            clonedDirectory.paste(file.clone(clonedDirectory));
        }

        return clonedDirectory;
    }

    /**
     * Копирование диетории в destinationDirectory
     */
    public VirtualDirectory copy(@NotNull VirtualDirectory destinationDirectory) throws LockedVirtualFSNodeException,
            NullVirtualFSException, OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeletedException,
            EmptyNodeNameException {
        List<Lock> locks = tryReadLockDown();
        destinationDirectory.isModifying.set(true);
        try {
            locks.add(tryLockNameRead());
            locks.add(rootDirectory.tryReadLockDirectories());
            locks.add(destinationDirectory.tryWriteLockDirectories());
        } catch (LockedVirtualFSNodeException e) {
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

    /**
     * Поиск файлов в данной директории
     *
     * @param isRecursive флаг, указывающий на рекрсивный поиск
     */
    Iterator<VirtualFile> find(@NotNull Predicate<VirtualFile> match, boolean isRecursive) {
        return new VirtualFileIterator(match, isRecursive, this);
    }

    /**
     * Поиск файлов по маске в данной директории
     *
     * @param isRecursive флаг, указывающий на рекрсивный поиск
     */
    public Iterator<VirtualFile> find(@NotNull String subName, boolean isRecursive) {
        return find((VirtualFile file) -> file.getName().contains(subName), isRecursive);
    }

    /**
     * Поиск файлов по маске в данной дирекотрии
     */
    public Iterator<VirtualFile> find(@NotNull String subName) {
        return find(subName, false);
    }

    /**
     * Поиск файлов по паттерну в данной дирекотрии
     *
     * @param isRecursive флаг, указывающий на рекрсивный поиск
     */
    public Iterator<VirtualFile> find(@NotNull Pattern pattern, boolean isRecursive) {
        return find((VirtualFile file) -> pattern.matcher(file.getName()).matches(), isRecursive);
    }

    /**
     * Поиск файлов по паттерну в данной дирекотрии
     */
    public Iterator<VirtualFile> find(@NotNull Pattern pattern) {
        return find(pattern, false);
    }

    /**
     * Блокировка на запись списка директорий
     */
    Lock tryWriteLockDirectories() throws LockedVirtualFSNodeException {
        if (directoriesReadWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNodeException();
        }
        Lock lock = directoriesReadWriteLock.writeLock();
        if (!lock.tryLock()) {
            throw new LockedVirtualFSNodeException();
        }
        return lock;
    }

    /**
     * Блокировка на чтение списка директорий
     */
    Lock tryReadLockDirectories() throws LockedVirtualFSNodeException {
        if (directoriesReadWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNodeException();
        }
        Lock lock = directoriesReadWriteLock.readLock();
        if (!lock.tryLock()) {
            throw new LockedVirtualFSNodeException();
        }
        return lock;
    }

    /**
     * Блокировка на запись списка файлов
     */
    Lock tryWriteLockFiles() throws LockedVirtualFSNodeException {
        if (filesReadWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNodeException();
        }
        Lock lock = filesReadWriteLock.writeLock();
        if (!lock.tryLock()) {
            throw new LockedVirtualFSNodeException();
        }
        return lock;
    }

    /**
     * Блокировка на чтение списка файлов
     */
    Lock tryReadLockFiles() throws LockedVirtualFSNodeException {
        if (filesReadWriteLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNodeException();
        }
        Lock lock = filesReadWriteLock.readLock();
        if (!lock.tryLock()) {
            throw new LockedVirtualFSNodeException();
        }
        return lock;
    }

    /**
     * Блокировка на запись списков директорий и файлов, имени директории
     */
    List<Lock> tryWriteLock() throws LockedVirtualFSNodeException {
        List<Lock> locks = new ArrayList<>();
        if (nameLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNodeException();
        }

        try {
            locks.add(tryLockNameWrite());
            locks.add(tryWriteLockFiles());
            locks.add(tryWriteLockDirectories());
        } catch (LockedVirtualFSNodeException e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        return locks;
    }

    /**
     * Блокировка на чтение списков директорий и файлов, имени директории
     */
    List<Lock> tryReadLock() throws LockedVirtualFSNodeException {
        List<Lock> locks = new ArrayList<>();

        if (nameLock.isWriteLockedByCurrentThread()) {
            throw new LockedVirtualFSNodeException();
        }

        try {
            locks.add(tryLockNameRead());
            locks.add(tryReadLockFiles());
            locks.add(tryReadLockDirectories());
        } catch (LockedVirtualFSNodeException e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        return locks;
    }

    /**
     * Блокировка на запись текущей директории, рекурсивная блокировка на запись всех поддиректорий и файлов
     */
    List<Lock> tryWriteLockDown() throws LockedVirtualFSNodeException {
        return tryWriteLockDown(new ArrayList<>());
    }

    /**
     * Блокировка на запись текущей директории, рекурсивная блокировка на запись всех поддиректорий и файлов
     */
    List<Lock> tryWriteLockDown(List<Lock> locks) throws LockedVirtualFSNodeException {
        try {
            locks.addAll(tryWriteLock());
        } catch (LockedVirtualFSNodeException e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        for (VirtualDirectory directory : directories) {
            locks = directory.tryWriteLockDown(locks);
        }

        for (VirtualFile file : files) {
            try {
                locks.add(file.tryWriteLock());
            } catch (LockedVirtualFSNodeException e) {
                locks.forEach(Lock::unlock);
                throw e;
            }
        }

        return locks;
    }

    /**
     * Блокировка на чтение текущей директории, рекурсивная блокировка на чтение всех поддиректорий и файлов
     */
    List<Lock> tryReadLockDown() throws LockedVirtualFSNodeException {
        return tryReadLockDown(new ArrayList<>());
    }

    /**
     * Блокировка на чтение текущей директории, рекурсивная блокировка на чтение всех поддиректорий и файлов
     */
    List<Lock> tryReadLockDown(@NotNull List<Lock> locks) throws LockedVirtualFSNodeException {
        try {
            locks.addAll(tryReadLock());
        } catch (LockedVirtualFSNodeException e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        for (VirtualDirectory directory : directories) {
            locks = directory.tryReadLockDown(locks);
        }

        for (VirtualFile file : files) {
            try {
                locks.add(file.tryReadLock());
            } catch (LockedVirtualFSNodeException e) {
                locks.forEach(Lock::unlock);
                throw e;
            }
        }

        return locks;
    }

    /**
     * Блокировка на запись имени директории
     */
    Lock tryLockNameWrite() throws LockedVirtualFSNodeException {
        Lock lock = nameLock.writeLock();

        if (!lock.tryLock()) {
            throw new LockedVirtualFSNodeException();
        }

        return lock;
    }

    /**
     * Блокировка на чтение имени директории
     */
    Lock tryLockNameRead() throws LockedVirtualFSNodeException {
        Lock lock = nameLock.readLock();

        if (!lock.tryLock()) {
            throw new LockedVirtualFSNodeException();
        }

        return lock;
    }

    /**
     * Блокировка на запись списка директорий и файлов
     */
    List<Lock> tryLockWriteFilesDirectories(@NotNull List<Lock> locks) throws LockedVirtualFSNodeException {
        try {
            locks.add(tryWriteLockDirectories());
        } catch (LockedVirtualFSNodeException e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        try {
            locks.add(tryWriteLockFiles());
        } catch (LockedVirtualFSNodeException e) {
            locks.forEach(Lock::unlock);
            throw e;
        }

        return locks;
    }

    /**
     * импорт данных из виртульной директории
     */
    public void importContent(@NotNull VirtualDirectory originalDirectory) throws LockedVirtualFSNodeException,
            NullVirtualFSException, OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeletedException,
            NotUniqueNameException, EmptyNodeNameException {
        List<Lock> locks = originalDirectory.tryReadLockDown();
        locks = tryLockWriteFilesDirectories(locks);

        for (VirtualDirectory directory : originalDirectory.directories) {
            if (checkForUniqueDirectoryName(directory.getName())) {
                paste(directory.clone(this));
            } else {
                for (VirtualDirectory virtualDirectory : directories) {
                    if (virtualDirectory.getName().equals(directory.getName())) {
                        virtualDirectory.importContent(directory);
                        break;
                    }
                }
            }
        }

        for (VirtualFile virtualFile : originalDirectory.files) {
            if (checkForUniqueFileName(virtualFile.getName())) {
                paste(virtualFile.clone(this));
            } else {
                locks.forEach(Lock::unlock);
                throw new NotUniqueNameException();
            }
        }

        locks.forEach(Lock::unlock);
        save();
    }

    /**
     * импорт данных из физической папки
     */
    public void importContent(@NotNull File folder) throws LockedVirtualFSNodeException, NullVirtualFSException, OverlappingVirtualFileLockException, IOException, NotUniqueNameException, EmptyNodeNameException {
        if (!folder.isDirectory()) {
            throw new InvalidObjectException(String.format("File is not a directory: %s", folder.getAbsolutePath()));
        }
        List<Lock> locks = new ArrayList<>();
        locks = tryLockWriteFilesDirectories(locks);

        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                VirtualDirectory directory = new VirtualDirectory(fileEntry.getName());
                if (checkForUniqueDirectoryName(fileEntry.getName())) {
                    paste(directory);
                } else {
                    for (VirtualDirectory virtualDirectory : directories) {
                        if (virtualDirectory.getName().equals(fileEntry.getName())) {
                            directory = virtualDirectory;
                            break;
                        }
                    }
                }
                directory.importContent(fileEntry);
            } else {
                importFile(fileEntry, true);
            }
        }

        locks.forEach(Lock::unlock);
        save();
    }

    public void importFile(@NotNull File file) throws LockedVirtualFSNodeException, NullVirtualFSException,
            OverlappingVirtualFileLockException, IOException, NotUniqueNameException, EmptyNodeNameException {
        importFile(file, false);
    }

    private void importFile(@NotNull File file, boolean isLocked) throws LockedVirtualFSNodeException, NullVirtualFSException,
            OverlappingVirtualFileLockException, IOException, NotUniqueNameException, EmptyNodeNameException {
        if (!file.isFile()) {
            throw new InvalidObjectException(String.format("File is not a file: %s", file.getAbsolutePath()));
        }
        Lock lock = null;
        if(!isLocked) {
            lock = tryWriteLockFiles();
        }

        if (!checkForUniqueFileName(file.getName())) {
            if(lock != null) lock.unlock();
            throw new NotUniqueNameException();
        }

        Date createdAt = new Date(((FileTime) Files.getAttribute(file.toPath(), "creationTime")).toMillis());
        Date modifiedAt = new Date(((FileTime) Files.getAttribute(file.toPath(), "lastModifiedTime")).toMillis());
        VirtualFile virtualFile = new VirtualFile(file.getName(), this, -1, createdAt, modifiedAt);
        paste(virtualFile);
        VirtualRandomAccessFile virtualRandomAccessFile = virtualFile.open("rw");
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        byte[] b = new byte[(int) randomAccessFile.length()];
        randomAccessFile.read(b);
        randomAccessFile.close();
        virtualRandomAccessFile.write(b);
        virtualRandomAccessFile.close();

        if(lock != null) lock.unlock();
        save();
    }

    /**
     * Экспорт данных в физическую папку
     */
    public void exportContent(@NotNull File folder) throws IOException, LockedVirtualFSNodeException, NullVirtualFSException, OverlappingVirtualFileLockException {
        if (!folder.isDirectory()) {
            throw new InvalidObjectException(String.format("File is not a directory: %s", folder.getAbsolutePath()));
        }

        List<Lock> locks = new ArrayList<>();
        locks = tryReadLockDown(locks);

        for (VirtualDirectory directory : directories) {
            File newDirectory = new File(folder, directory.getName());
            if (newDirectory.isDirectory() || newDirectory.mkdir()) {
                directory.exportContent(newDirectory);
            }
        }

        for (VirtualFile file : files) {
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

    /**
     * Попытка сохранения VFS в файл
     */
    protected void save() {
        try {
            if (rootDirectory != null) {
                rootDirectory.save();
                return;
            }
            if (virtualFS == null) {
                return;
            }

            List<Lock> locks = tryReadLockDown();

            getVirtualFS().save();

            locks.forEach(Lock::unlock);
        } catch (Throwable throwable) {
            return;
        }
    }

    /**
     * Проверка на то, что название директории не повторяется в списке дочерних директорий
     */
    boolean checkForUniqueDirectoryName(@NotNull String name) {
        for (VirtualDirectory directory : directories) {
            if (Objects.equals(directory.getName(), name)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверка на то, что название директории не повторяется в списке дочерних директорий
     */
    boolean checkForUniqueDirectoryName(@NotNull VirtualDirectory virtualDirectory, @NotNull String name) {
        for (VirtualDirectory directory : directories) {
            if (directory != virtualDirectory && Objects.equals(directory.getName(), name)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверка на то, что название директории не повторяется в списке дочерних директорий
     */
    boolean checkForUniqueDirectoryName(@NotNull VirtualDirectory virtualDirectory) {
        return checkForUniqueDirectoryName(virtualDirectory, virtualDirectory.getName());
    }

    /**
     * Проверка на то, что название файла не повторяется в списке дочерних файлов
     */
    boolean checkForUniqueFileName(@NotNull String name) {
        for (VirtualFile file : files) {
            if (Objects.equals(file.getName(), name)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверка на то, что название файла не повторяется в списке дочерних файлов
     */
    boolean checkForUniqueFileName(@NotNull VirtualFile virtualFile, @NotNull String name) {
        for (VirtualFile file : files) {
            if (file != virtualFile && Objects.equals(file.getName(), name)) {
                return false;
            }
        }
        return true;
    }
}

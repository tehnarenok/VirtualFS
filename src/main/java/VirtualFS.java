import exceptions.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

public class VirtualFS {
    private final VirtualRandomAccessFile virtualRandomAccessFile;
    File sourceFile;
    private VirtualDirectory rootDirectory;

    public VirtualFS(@NotNull File sourceFile) throws IOException, ClassNotFoundException,
            EmptyNodeNameException, LockedVirtualFSNodeException {
        this(sourceFile, 8);
    }

    public VirtualFS(
            @NotNull File sourceFile,
            long position)
            throws IOException, ClassNotFoundException, EmptyNodeNameException, LockedVirtualFSNodeException {
        this.sourceFile = sourceFile;
        if (sourceFile.length() < 8) {
            this.rootDirectory = new VirtualDirectory("root", null, this);
            this.virtualRandomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw");
            this.save();
        } else {
            this.virtualRandomAccessFile = new VirtualRandomAccessFile(sourceFile, "rw", position);
            load();
        }
    }

    /**
     * Сохранение данных в файл, если что-либо открыто на запись, то выдаётся ошибка LockedVirtualFSNodeException
     */
    public void save() throws IOException, LockedVirtualFSNodeException {
        List<Lock> locks = rootDirectory.tryReadLockDown();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out;

            out = new ObjectOutputStream(bos);
            out.writeObject(this.rootDirectory);
            out.flush();
            byte[] bytes = bos.toByteArray();
            bos.close();

            if (virtualRandomAccessFile.length() < bytes.length) {
                this.virtualRandomAccessFile.setLength(bytes.length);
            }
            this.virtualRandomAccessFile.seek(0);
            this.virtualRandomAccessFile.write(bytes);
            this.virtualRandomAccessFile.flush();
        } finally {
            locks.forEach(Lock::unlock);
        }
    }

    /**
     * Загрузка данных о VFS из файла
     */
    private void load() throws IOException, ClassNotFoundException {
        byte[] bytes = new byte[(int) virtualRandomAccessFile.length()];
        virtualRandomAccessFile.read(bytes);

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));

        rootDirectory = (VirtualDirectory) in.readObject();
        rootDirectory.virtualFS = this;
    }

    /**
     * Закртытие VFS, если что-либо открыто на чтение или запись, то будет выдана ошибка LockedVirtualFSNodeException
     *
     * @throws LockedVirtualFSNodeException when an object is open to read/write
     */
    public void close() throws IOException, LockedVirtualFSNodeException {
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

    /**
     * Создание в root директории новой директории
     *
     * @param name имя директории
     * @return созданная директория
     */
    public VirtualDirectory mkdir(@NotNull String name) throws LockedVirtualFSNodeException, NotUniqueNameException, EmptyNodeNameException {
        return this.rootDirectory.mkdir(name);
    }

    /**
     * Создание в root директории ноого файла
     *
     * @param name имя файла
     * @return созданный файл
     */
    public VirtualFile touch(@NotNull String name) throws LockedVirtualFSNodeException, NotUniqueNameException, EmptyNodeNameException {
        return this.rootDirectory.touch(name);
    }

    /**
     * Получение root директории
     *
     * @return root директория VFS
     */
    public VirtualDirectory getRootDirectory() {
        return this.rootDirectory;
    }

    /**
     * Получение файлов в root диектории
     *
     * @return файлы в root диектории
     */
    public List<VirtualFile> getFiles() throws LockedVirtualFSNodeException {
        return rootDirectory.getFiles();
    }

    /**
     * Получение директорий в root диектории
     *
     * @return диектории в root диектории
     */
    public List<VirtualDirectory> getDirectories() throws LockedVirtualFSNodeException {
        return rootDirectory.getDirectories();
    }

    public void remove(
            @NotNull VirtualFile file)
            throws UnremovableVirtualNodeException, OverlappingVirtualFileLockException,
            IOException, NullVirtualFSException,
            LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException {

        file.remove();
    }

    public void remove(@NotNull VirtualDirectory directory) throws UnremovableVirtualNodeException, OverlappingVirtualFileLockException,
            IOException, NullVirtualFSException,
            LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException {
        directory.remove();
    }

    public void move(@NotNull VirtualFile virtualFile, @NotNull VirtualDirectory destinationDirectory)
            throws LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException, NotUniqueNameException {
        virtualFile.move(destinationDirectory);
    }

    public void move(
            @NotNull VirtualDirectory virtualDirectory,
            @NotNull VirtualDirectory destinationDirectory
    ) throws LockedVirtualFSNodeException, NotUniqueNameException, UnremovableVirtualNodeException {
        virtualDirectory.move(destinationDirectory);
    }

    public void move(@NotNull VirtualFile virtualFile) throws LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException, NotUniqueNameException {
        virtualFile.move(this.rootDirectory);
    }

    public void move(@NotNull VirtualDirectory virtualDirectory) throws LockedVirtualFSNodeException, NotUniqueNameException, UnremovableVirtualNodeException {
        virtualDirectory.move(this.rootDirectory);
    }

    public VirtualFile copy(
            @NotNull VirtualFile virtualFile,
            @NotNull VirtualDirectory destinationDirectory)
            throws NullVirtualFSException, LockedVirtualFSNodeException,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeletedException, NotUniqueNameException, EmptyNodeNameException {
        return virtualFile.copy(destinationDirectory);
    }

    public VirtualFile copy(@NotNull VirtualFile virtualFile)
            throws NullVirtualFSException, LockedVirtualFSNodeException,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeletedException, NotUniqueNameException, EmptyNodeNameException {
        return virtualFile.copy(this.rootDirectory);
    }

    public VirtualDirectory copy(
            @NotNull VirtualDirectory virtualDirectory,
            @NotNull VirtualDirectory destinationDirectory)
            throws LockedVirtualFSNodeException, NullVirtualFSException,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeletedException, EmptyNodeNameException {
        return virtualDirectory.copy(destinationDirectory);
    }

    public VirtualDirectory copy(
            @NotNull VirtualDirectory virtualDirectory)
            throws LockedVirtualFSNodeException, NullVirtualFSException,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeletedException, EmptyNodeNameException {
        return virtualDirectory.copy(this.rootDirectory);
    }

    /**
     * Поиск файлов по имени во всей VFS
     *
     * @param subName строка, которая должна содержаться в имени
     */
    public Iterator<VirtualFile> find(@NotNull String subName) {
        return this.rootDirectory.find(subName, true);
    }

    /**
     * Поиск файлов по паттерну во всей VFS
     *
     * @param pattern паттерн для поиска файлов
     */
    public Iterator<VirtualFile> find(@NotNull Pattern pattern) {
        return this.rootDirectory.find(pattern, true);
    }

    /**
     * импорт данных из физической папки
     *
     * @param folder папка, из которой происходит импорт
     */
    public void importContent(@NotNull File folder) throws NullVirtualFSException, LockedVirtualFSNodeException,
            OverlappingVirtualFileLockException, IOException, NotUniqueNameException, EmptyNodeNameException {
        rootDirectory.importContent(folder);
    }

    /**
     * импорт данных из виртуальной файловой системы
     *
     * @param virtualFS виртуальной файловой системы
     */
    public void importContent(@NotNull VirtualFS virtualFS) throws NullVirtualFSException, LockedVirtualFSNodeException,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeletedException, NotUniqueNameException, EmptyNodeNameException {
        rootDirectory.importContent(virtualFS.getRootDirectory());
    }

    /**
     * импорт данных из виртульной директории
     */
    public void importContent(@NotNull VirtualDirectory virtualDirectory) throws NullVirtualFSException, LockedVirtualFSNodeException,
            OverlappingVirtualFileLockException, IOException, VirtualFSNodeIsDeletedException, NotUniqueNameException, EmptyNodeNameException {
        rootDirectory.importContent(virtualDirectory);
    }

    /**
     * Экспорт данных в физическую папку
     */
    public void exportContent(@NotNull File folder) throws NullVirtualFSException, LockedVirtualFSNodeException,
            OverlappingVirtualFileLockException, IOException {
        rootDirectory.exportContent(folder);
    }
}

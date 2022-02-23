import exceptions.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public abstract class VirtualFSNode implements Serializable {
    protected String name;
    protected VirtualDirectory rootDirectory;
    transient protected VirtualFS virtualFS;
    transient boolean isDeleted = false;

    protected VirtualFSNode(
            @NotNull String name,
            VirtualDirectory rootDirectory) throws EmptyNodeNameException {
        if (name.equals("")) {
            throw new EmptyNodeNameException();
        }
        this.name = name;
        this.rootDirectory = rootDirectory;
    }

    /**
     * Получение имени ноды
     */
    public String getName() {
        return this.name;
    }

    /**
     * Переимнование ноды
     */
    public void rename(@NotNull String name) throws LockedVirtualFSNodeException,
            VirtualFSNodeIsDeletedException, NotUniqueNameException, EmptyNodeNameException {
        if (name.equals("")) {
            throw new EmptyNodeNameException();
        }
        this.name = name;
    }

    /**
     * Получение root директории
     */
    public VirtualDirectory getRootDirectory() {
        return this.rootDirectory;
    }

    /**
     * Проверки на возможность удаления
     */
    public void remove()
            throws UnremovableVirtualNodeException, OverlappingVirtualFileLockException,
            IOException, NullVirtualFSException, LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException {
        if (this.isDeleted) {
            throw new VirtualFSNodeIsDeletedException();
        }
        if (this.rootDirectory == null) {
            throw new UnremovableVirtualNodeException();
        }
    }

    public void move(@NotNull VirtualDirectory destinationDirectory)
            throws LockedVirtualFSNodeException, VirtualFSNodeIsDeletedException, NotUniqueNameException, UnremovableVirtualNodeException {
    }

    /**
     * Получение физического файла, где хранится текущая VFS
     */
    protected File getSourceFile() throws NullVirtualFSException {
        return getVirtualFS().sourceFile;
    }

    /**
     * Поучение VFS в которой находится файл/директория
     */
    protected VirtualFS getVirtualFS() throws NullVirtualFSException {
        if (virtualFS != null) return virtualFS;
        if (rootDirectory != null) {
            virtualFS = rootDirectory.getVirtualFS();
            return virtualFS;
        }

        throw new NullVirtualFSException();
    }
}

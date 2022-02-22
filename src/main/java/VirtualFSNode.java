import exceptions.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public abstract class VirtualFSNode implements Serializable {
    protected String name;
    protected VirtualDirectory rootDirectory;
    transient boolean isDeleted = false;
    transient protected VirtualFS virtualFS;

    protected VirtualFSNode(
            @NotNull String name,
            VirtualDirectory rootDirectory) throws EmptyNodeName {
        if(name.equals("")) {
            throw new EmptyNodeName();
        }
        this.name = name;
        this.rootDirectory = rootDirectory;
    }

    public String getName() {
        return this.name;
    }

    public void rename(@NotNull String name) throws LockedVirtualFSNode,
            VirtualFSNodeIsDeleted, NotUniqueName, EmptyNodeName {
        if(name.equals("")) {
            throw new EmptyNodeName();
        }
        this.name = name;
    }

    public VirtualDirectory getRootDirectory() {
        return this.rootDirectory;
    }

    public void remove()
            throws UnremovableVirtualNode, OverlappingVirtualFileLockException,
            IOException, NullVirtualFS, LockedVirtualFSNode, VirtualFSNodeIsDeleted {
        if(this.isDeleted) {
            throw new VirtualFSNodeIsDeleted();
        }
        if(this.rootDirectory == null) {
            throw new UnremovableVirtualNode();
        }
    }

    public void move(@NotNull VirtualDirectory destinationDirectory)
            throws LockedVirtualFSNode, VirtualFSNodeIsDeleted, NotUniqueName, UnremovableVirtualNode {}

    protected File getSourceFile() throws NullVirtualFS {
        return getVirtualFS().sourceFile;
    }

    protected VirtualFS getVirtualFS() throws NullVirtualFS {
        if(virtualFS != null) return virtualFS;
        if(rootDirectory != null) {
            virtualFS = rootDirectory.getVirtualFS();
            return virtualFS;
        }

        throw new NullVirtualFS();
    }
}

import exceptions.UnremovableVirtualNode;

import java.util.Date;

public class VirtualFile extends VirtualFSNode {
    final private Date createdAt;
    private Date modifiedAt;
    private byte[] content;

    public VirtualFile(String name) {
        this(name, null, new byte[]{});
    }

    public VirtualFile(String name, VirtualDirectory rootDirectory) {
        this(name, rootDirectory, new byte[]{});
    }

    protected VirtualFile(String name, VirtualDirectory rootDirectory, byte[] content) {
        super(name, rootDirectory);
        this.content = content;
        this.createdAt = new Date();
        this.modifiedAt = this.createdAt;
    }

    public byte[] getContent() {
        return content;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    @Override
    public void rename(String name) {
        super.rename(name);
        this.modifiedAt = new Date();
    }

    @Override
    public void remove() throws UnremovableVirtualNode {
        super.remove();
        this.rootDirectory.remove(this);
    }

    @Override
    public void move(VirtualDirectory destinationDirectory) {
        this.rootDirectory.remove(this);
        destinationDirectory.paste(this);
    }

    protected VirtualFile clone() {
        VirtualFile clonedFile = new VirtualFile(
                this.name,
                this.rootDirectory,
                this.content
        );

        return clonedFile;
    }

    public VirtualFile copy(VirtualDirectory destinationDirectory) {
        VirtualFile copiedFile = this.clone();
        copiedFile.move(destinationDirectory);
        return copiedFile;
    }
}

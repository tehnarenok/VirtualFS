import exceptions.UnremovableVirtualNode;

import java.util.Vector;

public class VirtualDirectory extends VirtualFSNode {
    private final Vector<VirtualDirectory> directories;
    private final Vector<VirtualFile> files;

    public VirtualDirectory(String name) {
        this(name, null);
    }

    protected VirtualDirectory(String name, VirtualDirectory rootDirectory) {
        super(name, rootDirectory);
        this.directories = new Vector<>();
        this.files = new Vector<>();
    }

    public Vector<VirtualDirectory> getDirectories() {
        return this.directories;
    }

    public Vector<VirtualFile> getFiles() {
        return this.files;
    }

    public VirtualDirectory mkdir(String name) {
        VirtualDirectory newDirectory = new VirtualDirectory(name, this);
        this.directories.add(newDirectory);
        return newDirectory;
    }

    public VirtualFile touch(String name) {
        VirtualFile newFile = new VirtualFile(name, this);
        this.files.add(newFile);
        return newFile;
    }

    @Override
    public void remove() throws UnremovableVirtualNode {
        super.remove();
        for (VirtualDirectory directory : this.directories) {
            directory.remove();
        }
        for (VirtualFile file : this.files) {
            file.remove();
        }

        this.rootDirectory.remove(this);
    }

    public void remove(VirtualFile file) {
        this.files.remove(file);
        file.rootDirectory = null;
    }

    public void remove(VirtualDirectory directory) {
        this.directories.remove(directory);
        directory.rootDirectory = null;
    }

    protected void paste(VirtualDirectory virtualDirectory) {
        this.directories.add(virtualDirectory);
        virtualDirectory.rootDirectory = this;
    }

    protected void paste(VirtualFile virtualFile) {
        this.files.add(virtualFile);
        virtualFile.rootDirectory = this;
    }

    @Override
    public void move(VirtualDirectory destinationDirectory) {
        this.rootDirectory.remove(this);
        destinationDirectory.paste(this);
    }
}

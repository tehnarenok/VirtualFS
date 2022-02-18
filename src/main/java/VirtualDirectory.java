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
}
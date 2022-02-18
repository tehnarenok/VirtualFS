public class VirtualFSNode {
    protected String name;
    protected VirtualDirectory rootDirectory;

    public VirtualFSNode(String name) {
        this(name, null);
    }

    protected VirtualFSNode(String name, VirtualDirectory rootDirectory) {
        this.name = name;
        this.rootDirectory = rootDirectory;
    }

    public String getName() {
        return this.name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public VirtualDirectory getRootDirectory() {
        return this.rootDirectory;
    }
}

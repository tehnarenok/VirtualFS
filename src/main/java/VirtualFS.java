public class VirtualFS {
    private final VirtualDirectory rootDirectory;

    public VirtualFS() {
        this.rootDirectory = new VirtualDirectory("");
    }

    public VirtualDirectory mkdir(String name) {
        return this.rootDirectory.mkdir(name);
    }

    public VirtualFile touch(String name) {
        return this.rootDirectory.touch(name);
    }

    public VirtualDirectory getRootDirectory() {
        return this.rootDirectory;
    }
}
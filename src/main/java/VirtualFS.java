import exceptions.UnremovableVirtualNode;

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

    public void remove(VirtualFile file) throws UnremovableVirtualNode {
        file.remove();
    }

    public void remove(VirtualDirectory directory) throws UnremovableVirtualNode {
        directory.remove();
    }

    public void move(VirtualFile virtualFile, VirtualDirectory destinationDirectory) {
        virtualFile.move(destinationDirectory);
    }

    public void move(VirtualDirectory virtualDirectory, VirtualDirectory destinationDirectory) {
        virtualDirectory.move(destinationDirectory);
    }

    public void move(VirtualFile virtualFile) {
        virtualFile.move(this.rootDirectory);
    }

    public void move(VirtualDirectory virtualDirectory) {
        virtualDirectory.move(this.rootDirectory);
    }
}

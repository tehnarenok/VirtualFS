import exceptions.UnremovableVirtualNode;

import java.util.Iterator;
import java.util.regex.Pattern;

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

    public VirtualFile copy(VirtualFile virtualFile, VirtualDirectory destinationDirectory) {
        return virtualFile.copy(destinationDirectory);
    }

    public VirtualFile copy(VirtualFile virtualFile) {
        return virtualFile.copy(this.rootDirectory);
    }

    public VirtualDirectory copy(VirtualDirectory virtualDirectory, VirtualDirectory destinationDirectory) {
        return virtualDirectory.copy(destinationDirectory);
    }

    public VirtualDirectory copy(VirtualDirectory virtualDirectory) {
        return virtualDirectory.copy(this.rootDirectory);
    }

    public Iterator<VirtualFile> find(String subName) {
        return this.rootDirectory.find(subName, true);
    }

    public Iterator<VirtualFile> find(Pattern pattern) {
        return this.rootDirectory.find(pattern, true);
    }
}

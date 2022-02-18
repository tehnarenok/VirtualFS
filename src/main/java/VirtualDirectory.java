import exceptions.UnremovableVirtualNode;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

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

    protected VirtualDirectory clone() {
        VirtualDirectory clonedDirectory = new VirtualDirectory(
                this.name,
                this.rootDirectory
        );

        this.directories.forEach(directory -> clonedDirectory.paste(directory.clone()));
        this.files.forEach(file -> clonedDirectory.paste(file.clone()));

        return clonedDirectory;
    }

    public VirtualDirectory copy(VirtualDirectory destinationDirectory) {
        VirtualDirectory copiedDirectory = this.clone();
        copiedDirectory.move(destinationDirectory);
        return copiedDirectory;
    }

    private Iterator<VirtualFile> find(Match match, Boolean isRecursive) {
        Iterator<VirtualFile> it = new Iterator<>() {
            private Integer fileIdx = 0;
            private Integer directoryIdx = 0;
            private Iterator<VirtualFile> directoryIterator = null;

            @Override
            public boolean hasNext() {
                while (fileIdx < files.size()) {
                    if (match.match(files.get(fileIdx))) {
                        return true;
                    }
                    fileIdx++;
                }

                if (!isRecursive) {
                    return false;
                }

                if (directoryIterator != null) {
                    if (directoryIterator.hasNext()) {
                        return true;
                    }
                    directoryIdx++;
                }

                while (directoryIdx < directories.size()) {
                    directoryIterator = directories.get(directoryIdx).find(match, isRecursive);
                    if (directoryIterator.hasNext()) {
                        return true;
                    }
                    directoryIdx++;
                }

                return false;
            }

            @Override
            public VirtualFile next() {
                if (hasNext()) {
                    if (fileIdx < files.size()) {
                        return files.get(fileIdx++);
                    }
                    return directoryIterator.next();
                }

                throw new NoSuchElementException();
            }
        };

        return it;
    }

    public Iterator<VirtualFile> find(String subName, Boolean isRecursive) {
        return this.find((VirtualFile file) -> file.getName().contains(subName), isRecursive);
    }

    public Iterator<VirtualFile> find(String subName) {
        return this.find(subName, false);
    }

    public Iterator<VirtualFile> find(Pattern pattern, Boolean isRecursive) {
        return this.find((VirtualFile file) -> pattern.matcher(file.getName()).matches(), isRecursive);
    }

    public  Iterator<VirtualFile> find(Pattern pattern) {
        return this.find(pattern, false);
    }
}

import exceptions.LockedVirtualFSNode;
import org.jetbrains.annotations.NotNull;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

class VirtualFileIterator implements Iterator<VirtualFile> {
    private final Predicate<VirtualFile> match;
    private final boolean isRecursive;
    private Integer fileIdx;
    private Integer directoryIdx;
    private Iterator<VirtualFile> directoryIterator;

    private final VirtualDirectory directory;
    private final List<VirtualFile> files;
    private final List<VirtualDirectory> directories;

    public VirtualFileIterator(
            @NotNull Predicate<VirtualFile> match,
            boolean isRecursive,
            @NotNull VirtualDirectory directory
    ) throws ConcurrentModificationException {
        this.match = match;
        this.isRecursive = isRecursive;
        fileIdx = 0;
        directoryIdx = 0;
        directoryIterator = null;

        this.directory = directory;
        try {
            this.files = directory.getFiles();
            this.directories = directory.getDirectories();
        } catch (LockedVirtualFSNode e) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public boolean hasNext() {
        if (directory.isModifying()) {
            throw new ConcurrentModificationException();
        }
        while (fileIdx < files.size()) {
            if (match.test(files.get(fileIdx))) {
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
}

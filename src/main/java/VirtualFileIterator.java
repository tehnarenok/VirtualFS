import exceptions.LockedVirtualFSNodeException;
import org.jetbrains.annotations.NotNull;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

class VirtualFileIterator implements Iterator<VirtualFile> {
    private final Predicate<VirtualFile> match;
    private final boolean isRecursive;
    private final VirtualDirectory directory;
    private final List<VirtualFile> files;
    private List<VirtualDirectory> directories = null;
    private int fileIdx;
    private int directoryIdx;
    private Iterator<VirtualFile> directoryIterator;

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
            if(isRecursive) {
                this.directories = directory.getDirectories();
            }
        } catch (LockedVirtualFSNodeException e) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Проверка на то, что существует еще как минимум один файл, подходящий под маску
     */
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

    /**
     * Возвращает следующий найденный файл по маске
     */
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

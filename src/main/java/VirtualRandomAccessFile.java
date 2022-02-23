import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class VirtualRandomAccessFile extends RandomAccessFile {
    private static int BLOCK_SIZE = 1024;
    private final RandomAccessFile sourceFile;
    private final VirtualRandomAccessFileListener onClose;
    // meta information
    private long size;
    // information about real file
    private long emptyBlockPosition = 8;
    // information for navigation in real file
    private long currentBlockPosition;
    private long nextBlockPosition;
    private long firstBlockPosition;
    private boolean isReadFirstBlockData;
    // positions in virtual file
    private long position;
    private boolean isEOF;
    // buffer for read/write virtual file
    private byte[] buffer;
    private int bufferPosition;
    private long bufferStartPosition;
    // information about writing data
    private boolean isWriteData;

    /**
     * @param file     - физический файл
     * @param mode     - тип отрытия ("r" - для чтения, "rw" - для четния/записи)
     * @param position - номер байта, с которого начинается первый блок
     */
    public VirtualRandomAccessFile(
            @NotNull File file,
            @NotNull String mode,
            long position,
            VirtualRandomAccessFileListener onClose
    ) throws IOException {
        super(file, mode);
        this.sourceFile = new RandomAccessFile(file, mode);
        this.isEOF = true;
        this.firstBlockPosition = position;

        this.onClose = onClose;

        this.readFileInfo();
        this.readFirstBlock();
    }

    /**
     * Конструктор для виртуального файла, который записан в память
     *
     * @param file     - физический файл
     * @param mode     - тип отрытия ("r" - для чтения, "rw" - для четния/записи)
     * @param position - номер байта, с которого начинается первый блок
     */
    public VirtualRandomAccessFile(
            @NotNull File file,
            @NotNull String mode,
            long position) throws IOException {
        this(file, mode, position, null);
    }

    /**
     * Конструктор для виртуального файла, который еще не записан в память
     *
     * @param file - физический файл
     * @param mode - тип отрытия ("r" - для чтения, "rw" - для четния/записи)
     */
    public VirtualRandomAccessFile(@NotNull File file, @NotNull String mode) throws IOException {
        this(file, mode, -1, null);
    }

    public static int getBlockSize() {
        return BLOCK_SIZE;
    }

    private void readFileInfo() throws IOException {
        while (true) {
            if (sourceFile.length() < 8) {
                FileChannel fileChannel = sourceFile.getChannel();
                FileLock lock;
                try {
                    lock = fileChannel.lock();
                } catch (OverlappingFileLockException exception) {
                    continue;
                }
                sourceFile.setLength(0);
                sourceFile.seek(0);
                sourceFile.writeLong(BLOCK_SIZE);

                lock.release();
            } else {
                sourceFile.seek(0);
                BLOCK_SIZE = (int) sourceFile.readLong();
            }

            break;
        }
    }

    private VirtualBlockInfo readBlockInfo(long position) throws IOException {
        sourceFile.seek(position);
        VirtualBlockInfo blockInfo = new VirtualBlockInfo();
        blockInfo.lastByteInBlockPosition = sourceFile.readLong();
        blockInfo.nextBlockPosition = sourceFile.readLong();

        return blockInfo;
    }

    private VirtualFileMetaInformation readMetaInformation(long position) throws IOException {
        sourceFile.seek(position);
        VirtualFileMetaInformation metaInformation = new VirtualFileMetaInformation();

        metaInformation.size = sourceFile.readLong();
        metaInformation.lastBlockPosition = sourceFile.readLong();

        return metaInformation;
    }

    private VirtualFileMetaInformation readMetaInformation() throws IOException {
        return this.readMetaInformation(sourceFile.getFilePointer());
    }

    private ByteBuffer longToByteArray(long v) {
        byte[] b = new byte[8];
        b[0] = (byte) ((v >>> 56) & 0xFF);
        b[1] = (byte) ((v >>> 48) & 0xFF);
        b[2] = (byte) ((v >>> 40) & 0xFF);
        b[3] = (byte) ((v >>> 32) & 0xFF);
        b[4] = (byte) ((v >>> 24) & 0xFF);
        b[5] = (byte) ((v >>> 16) & 0xFF);
        b[6] = (byte) ((v >>> 8) & 0xFF);
        b[7] = (byte) ((v) & 0xFF);

        return ByteBuffer.wrap(b);
    }

    public long getFirstBlockPosition() {
        return firstBlockPosition;
    }

    /**
     * Поиск первого свободного блока, в который можно записать информацию
     */
    private long findFirstEmptyBlock() throws IOException {
        sourceFile.seek(emptyBlockPosition);

        if (sourceFile.getFilePointer() >= sourceFile.length()) {
            return emptyBlockPosition;
        }

        try {
            if (sourceFile.readLong() == -2) {
                return emptyBlockPosition;
            }
        } catch (IOException io) {
            sourceFile.skipBytes(BLOCK_SIZE + 16);
        }

        while (true) {
            sourceFile.skipBytes(BLOCK_SIZE + 8);
            emptyBlockPosition = sourceFile.getFilePointer();

            if (sourceFile.getFilePointer() >= sourceFile.length()) {
                return emptyBlockPosition;
            }

            try {
                if (sourceFile.readLong() == -2) {
                    return emptyBlockPosition;
                }
            } catch (IOException io) {
                throw new OverlappingFileLockException();
            }
        }
    }

    /**
     * @return длина данных виртульного файла
     */
    @Override
    public long length() {
        return size;
    }

    /**
     * @return текущая позиция в виртульном файле
     */
    @Override
    public long getFilePointer() {
        return position;
    }

    /**
     * Чтение первого блока
     */
    private void readFirstBlock() throws IOException {
        buffer = null;
        bufferPosition = 0;
        position = 0;
        isWriteData = false;

        // Проверка на существование виртуального файла в памяти
        if (firstBlockPosition == -1) {
            currentBlockPosition = -1;
            size = 0;
        } else {
            currentBlockPosition = firstBlockPosition;

            VirtualBlockInfo blockInfo = readBlockInfo(currentBlockPosition);
            VirtualFileMetaInformation metaInformation = readMetaInformation();

            nextBlockPosition = blockInfo.nextBlockPosition;

            size = metaInformation.size;

            isReadFirstBlockData = true;

            isEOF = false;
        }
    }

    /**
     * Чтение блока данных
     */
    private void readBlock() throws IOException {
        if (isWriteData) {
            writeBlock();
        }

        if (firstBlockPosition == -1) {
            buffer = new byte[BLOCK_SIZE];

            ByteBuffer target = ByteBuffer.wrap(buffer);
            target.put(longToByteArray(0));
            target.put(longToByteArray(-1));

            size = 0;
            bufferPosition = 8 * 2;
            bufferStartPosition = -1;
            return;
        }

        if (isEOF) {
            buffer = new byte[BLOCK_SIZE];
            bufferStartPosition = -1;
            bufferPosition = 0;
            return;
        }

        if (isReadFirstBlockData) {
            isReadFirstBlockData = false;
        } else {
            currentBlockPosition = nextBlockPosition;
        }

        if (currentBlockPosition != firstBlockPosition) {
            VirtualBlockInfo blockInfo = readBlockInfo(currentBlockPosition);
            nextBlockPosition = blockInfo.nextBlockPosition;
        }

        bufferStartPosition = sourceFile.getFilePointer();

        if (currentBlockPosition != firstBlockPosition) {
            buffer = new byte[BLOCK_SIZE];
        } else {
            buffer = new byte[BLOCK_SIZE - 8 * 2];
        }

        sourceFile.read(buffer);
        bufferPosition = 0;

        if (nextBlockPosition == -1) {
            isEOF = true;
        }
    }

    public int readNextByte() throws IOException {
        if (position >= size) {
            return -1;
        }
        if (buffer != null) {
            if (bufferPosition < buffer.length) {
                position++;
                return buffer[bufferPosition++];
            }
        }
        readBlock();
        return readNextByte();
    }

    /**
     * чтение байта в текущей позиции
     *
     * @return значение байта в текущей позиции
     */
    @Override
    public int read() throws IOException {
        return readNextByte();
    }

    /**
     * чтение байтов в массив байтов b с позиции off и длиной len
     */
    @Override
    public int read(
            byte[] b,
            int off,
            int len) throws IOException {
        for (int i = off; i < len; i++) {
            b[i] = (byte) read();
        }
        return b.length;
    }

    /**
     * чтение байтов в массив байтов b
     */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Запись буффера в физический файл
     */
    private void writeBlock() throws IOException {
        FileChannel fileChannel = sourceFile.getChannel();
        FileLock lockCurrentBlock;
        FileLock lockLastBlock = null;
        FileLock lockMetadata = null;

        boolean isWriteNewBlock;
        boolean isWriteFirstBlock;

        long writePosition;

        // Попытка получить лок на необходимые области файла
        while (true) {
            writePosition = bufferStartPosition;
            isWriteNewBlock = writePosition == -1;
            isWriteFirstBlock = firstBlockPosition == -1;

            if (isWriteNewBlock) {
                try {
                    writePosition = findFirstEmptyBlock();
                } catch (OverlappingFileLockException exception) {
                    continue;
                }
            }

            if (isWriteFirstBlock) {
                firstBlockPosition = writePosition;
            }

            try {
                lockCurrentBlock = fileChannel.tryLock(writePosition, buffer.length + 8 * 2, false);
            } catch (OverlappingFileLockException exception) {
                if (isWriteFirstBlock) firstBlockPosition = -1;
                continue;
            }

            try {
                if (!isWriteFirstBlock && isWriteNewBlock) {
                    lockLastBlock = fileChannel.tryLock(currentBlockPosition + 8, 8, false);
                    lockMetadata = fileChannel.tryLock(firstBlockPosition + 8 * 2, 8 * 2, false);
                }
            } catch (OverlappingFileLockException exception) {
                lockCurrentBlock.release();

                if (lockLastBlock != null) {
                    lockLastBlock.release();
                    lockLastBlock = null;
                }

                if (isWriteFirstBlock) firstBlockPosition = -1;
                continue;
            }
            break;
        }

        if (!isWriteNewBlock) {
            // Записываем в старый блок
            fileChannel.write(ByteBuffer.wrap(buffer), writePosition);

            //изменяем "ссылку" на конец в блоке, если записываем данные сверх
            sourceFile.seek(currentBlockPosition);
            if (sourceFile.readLong() < writePosition + bufferPosition - 1) {
                fileChannel.write(longToByteArray(writePosition + bufferPosition - 1), currentBlockPosition);
            }
        } else {
            // Записываем новый блок
            long saveWritingPosition = writePosition;

            if (!isWriteFirstBlock) {
                //изменяем в meta информации ссылку на последний блок
                // если пишем первый блок, то этого делать не надо, т.к. в буфер перезатрет эти изменения
                fileChannel.write(longToByteArray(saveWritingPosition), firstBlockPosition + 8 * 3);
            }

            if (!isWriteFirstBlock) {
                //изменяем ссылку на следующий блок, в предыдущем блоке
                // Если блок, который пишем первый, то предыдущего блока нет

                fileChannel.write(longToByteArray(saveWritingPosition), currentBlockPosition + 8);
            }

            // Записываем начало блока. Первые 8 байт - long "ссылка" на конец данных в этом блоке
            fileChannel.write(longToByteArray(saveWritingPosition + 8 * 2 + bufferPosition - 1), writePosition);
            writePosition += 8;

            // Записываем следующие 8 байт - long "ссылка" на сл блок, если блок последний, то пишем -1
            fileChannel.write(longToByteArray(-1), writePosition);
            writePosition += 8;

            // Пишем буфер в память
            fileChannel.write(ByteBuffer.wrap(buffer), writePosition);

            if (isWriteFirstBlock) {
                // Если пишем блок, то надо поменять мета информацию, ссылку на конечный блок = первому блоку
                fileChannel.write(longToByteArray(firstBlockPosition), firstBlockPosition + 8 * 3);
            }

            currentBlockPosition = saveWritingPosition;
        }

        //справляем мета информацию о файле - размер файла
        fileChannel.write(longToByteArray(size), firstBlockPosition + 8 * 2);

        if (lockCurrentBlock != null) lockCurrentBlock.release();
        if (lockLastBlock != null) lockLastBlock.release();
        if (lockMetadata != null) lockMetadata.release();

        isWriteData = false;
        readBlock();
    }

    /**
     * Запись одного байта
     * Зпись первоночально идет в буфер, при заполении буфера, буфер пишется в физический файл, ситаем сл блок
     */
    private void writeByte(byte b) throws IOException {
        if (onClose != null) onClose.onModify();
        if (buffer == null) {
            readBlock();
        }
        if (buffer != null && bufferPosition >= buffer.length) {
            readBlock();
        }
        isWriteData = true;
        assert buffer != null;
        buffer[bufferPosition++] = b;
        position++;

        if (position > size) {
            size = position;
        }
    }

    /**
     * запись байта в текущую позицию
     */
    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    /**
     * запись len байтов из массива b, начиная с позиции off
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int idx = off; idx < len; idx++) {
            write(b[idx]);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * закртытие виртульного файла
     */
    @Override
    public void close() throws IOException {
        if (isWriteData) {
            writeBlock();
        }
        sourceFile.close();
        if (onClose != null) onClose.onClose(firstBlockPosition);
        super.close();
    }

    /**
     * Удаление блока
     * Если полное удаление файла, то первые 8 байт = -2
     * иначе ставим в последние 8 байт номер конечного байта в блоке
     */
    private void deleteBlock(long position, int newBlockSize) throws IOException {
        if (newBlockSize == BLOCK_SIZE) return;

        FileChannel fileChannel = sourceFile.getChannel();
        FileLock lock = fileChannel.tryLock(position, 8, false);

        if (lock != null) {
            if (newBlockSize == 0) {
                fileChannel.write(longToByteArray(-2), position);
                if (position == firstBlockPosition) {
                    firstBlockPosition = -1;
                    readFirstBlock();
                }
                if (emptyBlockPosition > position) {
                    emptyBlockPosition = position;
                }
            } else {
                fileChannel.write(longToByteArray(position + 8 * 2 + newBlockSize - 1), position);
            }
        }

        assert lock != null;
        lock.release();
        sourceFile.seek(position + 8);
        long nextPosition = sourceFile.readLong();
        if (nextPosition != -1) {
            deleteBlock(nextPosition);
        }
    }

    /**
     * Удаление блока информаци
     */
    private void deleteBlock(long position) throws IOException {
        deleteBlock(position, 0);
    }

    /**
     * изменение длины файла, при увеличении в конец добисываются байты
     */
    @Override
    public void setLength(long newLength) throws IOException {
        if (newLength == size) {
            return;
        }

        if (newLength > size) {
            seek(size);
            write(new byte[(int) (newLength - size)]);
            if (isWriteData) {
                writeBlock();
            }
            if (onClose != null) onClose.onModify();
            seek(size);
        } else {
            seek(newLength);
            deleteBlock(currentBlockPosition, bufferPosition);
            if (onClose != null) onClose.onModify();
            size = newLength;
        }
    }

    /**
     * Перемещение в виртульном файле в позицию pos
     */
    @Override
    public void seek(long pos) throws IOException {
        if (isWriteData) {
            writeBlock();
        }

        if (pos < 0) throw new IOException();

        if (pos > size) {
            seek(size);
            return;
        }

        readFirstBlock();

        while (true) {
            readBlock();
            long bufferLength = buffer.length;
            if (currentBlockPosition == firstBlockPosition) {
                if (bufferLength == BLOCK_SIZE) bufferLength -= 8 * 2;
            }
            if (bufferLength + position <= pos) {
                position += bufferLength;
            } else {
                bufferPosition = (int) (pos - position);
                if (currentBlockPosition == firstBlockPosition) {
                    if (buffer.length == BLOCK_SIZE) {
                        bufferPosition += 8 * 2;
                        position -= 8 * 2;
                    }
                }
                position += bufferPosition;
                break;
            }
        }
    }

    /**
     * Принудительная запись в файл
     */
    public void flush() throws IOException {
        if (isWriteData) {
            writeBlock();
        }
    }

    private static class VirtualBlockInfo {
        public long lastByteInBlockPosition;
        public long nextBlockPosition;
    }

    private static class VirtualFileMetaInformation {
        public long size;
        public long lastBlockPosition;
    }
}
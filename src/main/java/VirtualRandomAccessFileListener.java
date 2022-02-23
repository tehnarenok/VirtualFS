public interface VirtualRandomAccessFileListener {
    default void onModify() {
    }

    default void onClose(long firstBlockPosition) {
    }
}

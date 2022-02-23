package exceptions;

public class UnremovableVirtualNodeException extends VFSException {
    public UnremovableVirtualNodeException() {
        super("This node cannot be deleted");
    }
}

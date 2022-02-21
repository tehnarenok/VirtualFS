package exceptions;

public class UnremovableVirtualNode extends VFSException {
    public UnremovableVirtualNode() {
        super("This node cannot be deleted");
    }
}

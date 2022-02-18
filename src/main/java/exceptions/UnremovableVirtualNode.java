package exceptions;

public class UnremovableVirtualNode extends Exception {
    public UnremovableVirtualNode() {
        this("This node cannot be deleted");
    }

    public UnremovableVirtualNode(String message) {
        super(message);
    }
}

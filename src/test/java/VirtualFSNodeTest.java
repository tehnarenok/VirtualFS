import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VirtualFSNodeTest {
    @Test
    void createNode() {
        String name = "test_name";

        VirtualFSNode virtualFSNode = new VirtualFSNode(name);

        assertEquals(name, virtualFSNode.name);
        assertNull(virtualFSNode.rootDirectory);
    }

    @Test
    void createNodeWithRootDirectory() {
        String name = "test_name";

        VirtualDirectory virtualDirectory = new VirtualDirectory(name);
        VirtualFSNode virtualFSNode = new VirtualFSNode(name, virtualDirectory);

        assertEquals(virtualDirectory, virtualFSNode.rootDirectory);
    }

    @Test
    void getName() {
        String name = "test_name";

        VirtualFSNode virtualFSNode = new VirtualFSNode(name);

        assertEquals(name, virtualFSNode.getName());
    }

    @Test
    void rename() {
        String name = "test_name";
        String newName = "name_test";

        VirtualFSNode virtualFSNode = new VirtualFSNode(name);
        virtualFSNode.rename(newName);

        assertEquals(newName, virtualFSNode.getName());
    }

    @Test
    void getNullRootDirectory() {
        String name = "test_name";

        VirtualFSNode virtualFSNode = new VirtualFSNode(name);

        assertNull(virtualFSNode.getRootDirectory());
    }

    @Test
    void getRootDirectory() {
        String name = "test_name";

        VirtualDirectory virtualDirectory = new VirtualDirectory(name);
        VirtualFSNode virtualFSNode = new VirtualFSNode(name, virtualDirectory);

        assertEquals(virtualDirectory, virtualFSNode.getRootDirectory());
    }
}
import exceptions.LockedVirtualFSNode;
import exceptions.VirtualFSNodeIsDeleted;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VirtualFSNodeTest {
    final String name = "test_name";
    final String newName = "name_test";

//    @Test
//    void createNode() {
//        VirtualFSNode virtualFSNode = new VirtualFSNode(name);
//
//        assertEquals(name, virtualFSNode.name);
//        assertNull(virtualFSNode.rootDirectory);
//    }
//
//    @Test
//    void createNodeWithRootDirectory() {
//        VirtualDirectory virtualDirectory = new VirtualDirectory(name);
//        VirtualFSNode virtualFSNode = new VirtualFSNode(name, virtualDirectory);
//
//        assertEquals(virtualDirectory, virtualFSNode.rootDirectory);
//    }
//
//    @Test
//    void getName() {
//        VirtualFSNode virtualFSNode = new VirtualFSNode(name);
//
//        assertEquals(name, virtualFSNode.getName());
//    }
//
//    @Test
//    void rename() throws LockedVirtualFSNode, VirtualFSNodeIsDeleted {
//        VirtualFSNode virtualFSNode = new VirtualFSNode(name);
//        virtualFSNode.rename(newName);
//
//        assertEquals(newName, virtualFSNode.getName());
//    }
//
//    @Test
//    void getNullRootDirectory() {
//        VirtualFSNode virtualFSNode = new VirtualFSNode(name);
//
//        assertNull(virtualFSNode.getRootDirectory());
//    }
//
//    @Test
//    void getRootDirectory() {
//        VirtualDirectory virtualDirectory = new VirtualDirectory(name);
//        VirtualFSNode virtualFSNode = new VirtualFSNode(name, virtualDirectory);
//
//        assertEquals(virtualDirectory, virtualFSNode.getRootDirectory());
//    }
}
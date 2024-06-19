import java.util.HashSet;
import java.util.Set;

public class TCPPortAllocator {
    private final Set<Integer> allocatedPorts;
    private final int startPort;
    private final int endPort;

    public TCPPortAllocator(int startPort, int endPort) {
        this.startPort = startPort;
        this.endPort = endPort;
        this.allocatedPorts = new HashSet<>();
    }

    public synchronized int allocatePort() throws NoAvailablePortsException {
        for (int port = startPort; port <= endPort; port++) {
            if (!allocatedPorts.contains(port)) {
                allocatedPorts.add(port);
                return port;
            }
        }
        throw new NoAvailablePortsException("No available ports in the range " + startPort + " to " + endPort);
    }

    public synchronized void releasePort(int port) {
        allocatedPorts.remove(port);
    }

    public static void main(String[] args) {
        TCPPortAllocator allocator = new TCPPortAllocator(10000, 10010);

        try {
            int port1 = allocator.allocatePort();
            System.out.println("Allocated port: " + port1);

            int port2 = allocator.allocatePort();
            System.out.println("Allocated port: " + port2);

            allocator.releasePort(port1);
            System.out.println("Released port: " + port1);

            int port3 = allocator.allocatePort();
            System.out.println("Allocated port: " + port3);
        } catch (NoAvailablePortsException e) {
            e.printStackTrace();
        }
    }
}

class NoAvailablePortsException extends Exception {
    public NoAvailablePortsException(String message) {
        super(message);
    }
}
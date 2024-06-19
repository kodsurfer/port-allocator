import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

public class TCPPortAllocator2 {
    private final Set<Integer> ports;
    private final boolean sync;
    private final int limit;
    private final Object lock = new Object();

    public TCPPortAllocator2(boolean sync, int limit) {
        this.ports = new HashSet<>();
        this.sync = sync;
        this.limit = limit;
    }

    public static final TCPPortAllocator2 SINGLETON = new TCPPortAllocator2(true, 65536);

    public int count() {
        return ports.size();
    }

    public boolean isEmpty() {
        return ports.isEmpty();
    }

    public int acquire(int total, int timeout) throws TimeoutException {
        long start = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() > start + timeout * 1000L) {
                throw new TimeoutException("Can't find a place in the pool of " + limit + " ports for " + total + " port(s), in " + (System.currentTimeMillis() - start) / 1000.0 + "s");
            }
            Integer[] opts = safe(() -> {
                if (ports.size() + total > limit) {
                    return null;
                }
                Integer[] result = new Integer[total];
                try {
                    for (int i = 0; i < total; i++) {
                        result[i] = i == 0 ? take() : take(result[i - 1] + 1);
                    }
                } catch (IOException e) {
                    return null;
                }
                for (int port : result) {
                    if (ports.contains(port)) {
                        return null;
                    }
                }
                int sum = 0;
                int min = Integer.MAX_VALUE;
                for (int port : result) {
                    sum += port;
                    if (port < min) {
                        min = port;
                    }
                }
                if (sum - (total * min) != total * (total - 1) / 2) {
                    return null;
                }
                for (int port : result) {
                    ports.add(port);
                }
                return result;
            });
            if (opts != null) {
                return opts[0];
            }
        }
    }

    public void release(int port) {
        safe(() -> ports.remove(port));
    }

    private int take() throws IOException {
        return take(0);
    }

    private int take(int opt) throws IOException {
        try (ServerSocket server = new ServerSocket(opt)) {
            return server.getLocalPort();
        }
    }

    private <T> T safe(SafeBlock<T> block) {
        if (sync) {
            synchronized (lock) {
                return block.run();
            }
        } else {
            return block.run();
        }
    }

    @FunctionalInterface
    private interface SafeBlock<T> {
        T run();
    }

    public static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        try {
            TCPPortAllocator2 allocator = TCPPortAllocator2.SINGLETON;
            int port = allocator.acquire(1, 4);
            System.out.println("Acquired port: " + port);
            allocator.release(port);
            System.out.println("Released port: " + port);
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
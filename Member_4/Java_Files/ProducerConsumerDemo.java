import java.util.LinkedList;
import java.util.Queue;

public class ProducerConsumerDemo {
    private final Queue<Integer> buffer = new LinkedList<>();
    private final int capacity = 3;

    public synchronized void produce(int value) throws InterruptedException {
        while (buffer.size() == capacity) {
            wait();
        }
        buffer.add(value);
        System.out.println("Producer produced: " + value);
        notifyAll();
    }

    public synchronized int consume() throws InterruptedException {
        while (buffer.isEmpty()) {
            wait();
        }
        int value = buffer.remove();
        System.out.println("Consumer consumed: " + value);
        notifyAll();
        return value;
    }

    public static void runDemo() {
        ProducerConsumerDemo demo = new ProducerConsumerDemo();
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    demo.produce(i);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    demo.consume();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}

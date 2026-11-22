import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

public final class CollectionConceptDemo {
    private CollectionConceptDemo() {
    }

    public static void run() {
        List<String> arrayList =
            new ArrayList<>(List.of("Aman", "Manveen", "BK"));
        LinkedList<String> linkedList = new LinkedList<>(arrayList);
        Set<String> hashSet = new HashSet<>(arrayList);
        TreeSet<String> treeSet = new TreeSet<>(arrayList);

        Map<String, Integer> hashMap = new HashMap<>();
        for (String name : arrayList) {
            hashMap.put(name, name.length());
        }

        Queue<String> queue = new ArrayDeque<>(arrayList);
        Deque<String> deque = new ArrayDeque<>(arrayList);
        PriorityQueue<Integer> priorityQueue =
            new PriorityQueue<>(List.of(5, 1, 3));

        System.out.println("ArrayList: " + arrayList);
        System.out.println("LinkedList: " + linkedList);
        System.out.println("HashSet: " + hashSet);
        System.out.println("TreeSet (sorted): " + treeSet);
        System.out.println("HashMap name lengths: " + hashMap);
        System.out.println("Queue poll: " + queue.poll());
        System.out.println("Deque first: " + deque.peekFirst());
        System.out.println("PriorityQueue poll (smallest): "
            + priorityQueue.poll());

        Iterator<String> iterator = arrayList.iterator();
        System.out.print("Iterator: ");
        while (iterator.hasNext()) {
            System.out.print(iterator.next() + " ");
        }
        System.out.println();

        arrayList.sort(Comparator.comparingInt(String::length)
            .thenComparing(String.CASE_INSENSITIVE_ORDER));
        System.out.println("Comparator sort: " + arrayList);
        System.out.println("Stream names starting with M: "
            + arrayList.stream().filter(name -> name.startsWith("M")).toList());

        int wrapped = Integer.valueOf("42");
        StringBuilder builder = new StringBuilder("Campus");
        builder.append("Hub AI");
        System.out.println("Wrapper Integer: " + wrapped
            + " | StringBuilder: " + builder);

        int[][] marks = {{80, 85}, {90, 95}};
        System.out.println("2D array sample mark: " + marks[1][0]);
    }
}

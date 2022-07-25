package org.fundacionjala.console;

import org.fundacionjala.console.services.CollectionsBenchmark;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class ApplicationRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        try {

            // Standard Collections List, Set, Queue Interfaces Time Benchmark
            CollectionsBenchmark collectionsBenchmark = new CollectionsBenchmark(15000, 100000);
            collectionsBenchmark.run(ArrayList.class);
            collectionsBenchmark.run(LinkedList.class);
            collectionsBenchmark.run(HashSet.class);
            collectionsBenchmark.run(LinkedHashSet.class);
            collectionsBenchmark.run(TreeSet.class);
            collectionsBenchmark.run(PriorityQueue.class);
            collectionsBenchmark.run(ArrayDeque.class);
//            collectionsBenchmark.displayCollectionsBenchmarkResults();

            // Standard Collections List, Set, Queue Interfaces Memory Benchmark
            List<Class<? extends Collection>> classes = new ArrayList<>();
            classes.add(ArrayList.class);
            classes.add(LinkedList.class);
            //classes.add(HashSet.class);
            //classes.add(LinkedHashSet.class);
            //classes.add(TreeSet.class);
            classes.add(PriorityQueue.class);
            classes.add(ArrayDeque.class);
            collectionsBenchmark.runMemoryBench(classes);
//            collectionsBenchmark.displayMemoryResults();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

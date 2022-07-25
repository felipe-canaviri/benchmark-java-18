package org.fundacionjala.console.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Collections Benchmark
 *
 * @author Leo Lewis & Kaan Keskin
 */
public class CollectionsBenchmark {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * Time in ms after which the CollectionsBenchmark task is considered timeout and is
     * stopped
     */
    private final long timeout;

    /**
     * Is the given CollectionsBenchmark task timeout
     */
    private volatile boolean isTimeout;

    /**
     * Number of elements to populate the collection on which the CollectionsBenchmark will
     * be launched
     */
    private final int populateSize;

    /**
     * Collection implementation to be tested
     */
    private Collection<String> collection;

    /**
     * List implementation to be tested
     */
//    private List<String> list;

    /**
     * Default context used for each Collections Benchmark test (will populate the tested
     * collection before launching the bench)
     */
    private final List<String> defaultListCtx;

    /**
     * Collections Benchmark results
     */
    private final Map<String, Map<Class<? extends Collection<?>>, Long>> colBenchResults;

    /**
     * Collections Memory results
     */
    private final Map<Class<? extends Collection<?>>, Long> colMemoryResults;

    /**
     * Constructor
     *
     * @param timeout Timeout for operation
     * @param populateSize Size of the data to populate
     */
    public CollectionsBenchmark(long timeout, int populateSize) {
        this.timeout = timeout;
        this.populateSize = populateSize;
        defaultListCtx = new ArrayList<>();
        for (int i = 0; i < populateSize; i++) {
            defaultListCtx.add(Integer.toString(i % 100));
        }
        colBenchResults = new HashMap<>();
        colMemoryResults = new HashMap<>();
    }

    /**
     * Run the Collections Interface Benchmark on the given collection
     *
     * @param collectionTested the collection (if it's a List, some additional
     *                         bench will be done)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void run(Class<? extends Collection> collectionTested) {
        try {
            long startTime = System.currentTimeMillis();
            Constructor<? extends Collection> constructor = collectionTested.getDeclaredConstructor((Class<?>[]) null);
            constructor.setAccessible(true);
            collection = (Collection<String>) constructor.newInstance();
            LOGGER.info("Performances of " + collection.getClass().getCanonicalName() + " populated with "
                    + populateSize + " element(s)");
            LOGGER.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            // Test List Collection used in Benchmark cases
            final Collection<String> colTest = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                colTest.add(Integer.toString(i % 30));
            }

            // Standard Collection List, Set and Queue Interface Benchmark
            if (collection instanceof List || collection instanceof Set || collection instanceof Queue) {

                execute(i -> collection.add(Integer.toString(i % 29)), populateSize, "add " + populateSize + " elements");

                execute(i -> collection.remove(Integer.toString(i)), Math.max(1, populateSize / 10), "remove " + Math.max(1, populateSize / 10) + " elements given Object");

                execute(i -> collection.addAll(colTest),
                        Math.min(populateSize, 1000),
                        "addAll " + Math.min(populateSize, 1000) + " times " + colTest.size() + " elements");

                execute(i -> collection.contains(collection.size() - i - 1),
                        Math.min(populateSize, 1000), "contains " + Math.min(populateSize, 1000) + " times");

                execute(i -> collection.removeAll(colTest),
                        Math.min(populateSize, 10), "removeAll " + Math.min(populateSize, 10) + " times " + colTest.size() + " elements");

                execute(i -> collection.iterator(), populateSize, "iterator " + populateSize + " times");

                execute(i -> collection.containsAll(colTest),
                        Math.min(populateSize, 5000), "containsAll " + Math.min(populateSize, 5000) + " times");

                execute(i -> collection.toArray(),
                        Math.min(populateSize, 5000), "toArray " + Math.min(populateSize, 5000) + " times");

                execute(i -> collection.clear(), 1, "clear");

                execute(i -> collection.retainAll(colTest),
                        Math.min(populateSize, 10), "retainAll " + Math.min(populateSize, 10) + " times");
            }

            LOGGER.info("Benchmark done in " + ((double) (System.currentTimeMillis() - startTime)) / 1000 + "s");
            LOGGER.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            // free memory
            collection.clear();
        } catch (Exception e) {
            System.err.println("Failed running Benchmark on class " + collectionTested.getCanonicalName());
            e.printStackTrace();
        }
        collection = null;
//        list = null;
        heavyGc();
    }

    /**
     * Execute the current run code loop times.
     *
     * @param run      code to run
     * @param loop     number of time to run the code
     * @param taskName name displayed at the end of the task
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void execute(BenchRunnable run, int loop, String taskName) {
        System.out.print(taskName + " ... ");
        // set default context
        collection.clear();
        collection.addAll(defaultListCtx);
        // warmup
        warmUp();
        isTimeout = false;
        // timeout timer
        javax.swing.Timer timer = new Timer((int) timeout, e -> {
            isTimeout = true;
            // to raise a ConcurrentModificationException or a
            // NoSuchElementException to interrupt internal work in the List
            collection.clear();
        });
        timer.setRepeats(false);
        timer.start();
        long startTime = System.nanoTime();
        int i;
        for (i = 0; i < loop && !isTimeout; i++) {
            try {
                run.run(i);
            } catch (Exception e) {
                // on purpose so ignore it
            }
        }
        timer.stop();
        long time = isTimeout ? timeout * 1000000 : System.nanoTime() - startTime;
        LOGGER.info((isTimeout ? "Timeout (>" + time + "ns) after " + i + " loop(s)" : time + "ns"));
        // restore default context,
        // the collection instance might have been
        // corrupted by the timeout so create a new instance
        try {
            Constructor<? extends Collection> constructor = collection.getClass().getDeclaredConstructor((Class<?>[]) null);
            constructor.setAccessible(true);
            collection = constructor.newInstance();
            // update the reference
//            if (collection instanceof List) {
//                list = (List<String>) collection;
//            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        // store the results for display
        Map<Class<? extends Collection<?>>, Long> currentBench = colBenchResults.computeIfAbsent(taskName, k -> new HashMap<>());
        currentBench.put((Class<? extends Collection<String>>) collection.getClass(), time);
        // little gc to clean up all the stuff
        System.gc();
    }

    /**
     * Display Benchmark results
     */
//    @SuppressWarnings("serial")
//    public void displayCollectionsBenchmarkResults() {
//        List<ChartPanel> chartPanels = new ArrayList<ChartPanel>();
//        // sort task by names
//        List<String> taskNames = new ArrayList<String>(colBenchResults.keySet());
//        Collections.sort(taskNames);
//        // browse task name, 1 chart per task
//        for (String taskName : taskNames) {
//            // time by class
//            Map<Class<? extends Collection<?>>, Long> clazzResult = colBenchResults.get(taskName);
//
//            ChartPanel chartPanel = createChart(taskName, "Time (ns)", clazzResult,
//                    new StandardCategoryItemLabelGenerator() {
//                        @Override
//                        public String generateLabel(CategoryDataset dataset, int row, int column) {
//                            String label = " " + dataset.getRowKey(row).toString();
//                            if (dataset.getValue(row, column).equals(timeout * 1000000)) {
//                                label += " (Timeout)";
//                            }
//                            return label;
//                        }
//                    });
//
//            chartPanels.add(chartPanel);
//        }
//        // display in a JFrame
//        JPanel mainPanel = new JPanel(new GridLayout(chartPanels.size() / 5, 5, 5, 5));
//        for (ChartPanel chart : chartPanels) {
//            mainPanel.add(chart);
//        }
//        JFrame frame = new JFrame("Collection Implementations Benchmark");
//        frame.getContentPane().add(
//                new JLabel("Collection Implementations Benchmark. Populate size : " + populateSize + ", timeout : "
//                        + timeout + "ms"), BorderLayout.NORTH);
//        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
//        frame.setSize(900, 500);
//        frame.setLocationRelativeTo(null);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setVisible(true);
//    }

    /**
     * Do some operation to be sure that the internal structure is allocated
     */
    @SuppressWarnings("rawtypes")
    private void warmUp() {
        collection.remove(collection.iterator().next());
        if (collection instanceof List) {
            collection.remove(0);
            ((List) collection).indexOf(((List) collection).get(0));
        }
        collection.iterator();
        collection.toArray();
    }

    /**
     * Create a chartpanel
     *
     * @param title                 title
     * @param dataName              name of the data
     * @param clazzResult           data mapped by classes
     * @param catItemLabelGenerator label generator
     * @return the chartPanel
     */
//    @SuppressWarnings("serial")
//    private ChartPanel createChart(String title, String dataName,
//                                   Map<Class<? extends Collection<?>>, Long> clazzResult,
//                                   AbstractCategoryItemLabelGenerator catItemLabelGenerator) {
//        // sort data by class name
//        List<Class<? extends Collection<?>>> clazzes = new ArrayList<>(
//                clazzResult.keySet());
//        Collections.sort(clazzes, new Comparator<>() {
//            @Override
//            public int compare(Class<? extends Collection<?>> o1, Class<? extends Collection<?>> o2) {
//                return o1.getCanonicalName().compareTo(o2.getCanonicalName());
//            }
//        });
//        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
//        // add the data to the dataset
//        for (Class<? extends Collection<?>> clazz : clazzes) {
//            dataSet.addValue(clazzResult.get(clazz), clazz.getName(), title.split(" ")[0]);
//        }
//        // create the chart
//        JFreeChart chart = ChartFactory.createBarChart(null, null, dataName, dataSet, PlotOrientation.HORIZONTAL,
//                false, true, false);
//        chart.addSubtitle(new TextTitle(title));
//        // some customization in the style
//        CategoryPlot plot = chart.getCategoryPlot();
//        plot.setBackgroundPaint(new Color(250, 250, 250));
//        plot.setDomainGridlinePaint(new Color(255, 200, 200));
//        plot.setRangeGridlinePaint(Color.BLUE);
//        plot.getDomainAxis().setVisible(false);
//        plot.getRangeAxis().setLabelFont(new Font("arial", Font.PLAIN, 10));
//        BarRenderer renderer = (BarRenderer) chart.getCategoryPlot().getRenderer();
//        // display the class name in the bar chart
//        for (int i = 0; i < clazzResult.size(); i++) {
//            renderer.setSeriesItemLabelGenerator(i, new StandardCategoryItemLabelGenerator() {
//                @Override
//                public String generateLabel(CategoryDataset dataset, int row, int column) {
//                    String label = " " + dataset.getRowKey(row).toString();
//                    if (dataset.getValue(row, column).equals(timeout * 1000000)) {
//                        label += " (Timeout)";
//                    }
//                    return label;
//                }
//            });
//            renderer.setSeriesItemLabelsVisible(i, true);
//            ItemLabelPosition itemPosition = new ItemLabelPosition();
//            renderer.setSeriesPositiveItemLabelPosition(i, itemPosition);
//            renderer.setSeriesNegativeItemLabelPosition(i, itemPosition);
//        }
//        ItemLabelPosition itemPosition = new ItemLabelPosition();
//        renderer.setPositiveItemLabelPositionFallback(itemPosition);
//        renderer.setNegativeItemLabelPositionFallback(itemPosition);
//        renderer.setShadowVisible(false);
//
//        // create the chartpanel
//        ChartPanel chartPanel = new ChartPanel(chart);
//        chart.setBorderVisible(true);
//        return chartPanel;
//    }

    /**
     * @param collectionTested
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void runMemoryBench(List<Class<? extends Collection>> collectionTested) {
        for (Class<? extends Collection> clazz : collectionTested) {
            try {
                // run some gc
                heavyGc();
                long usedMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
                Constructor<? extends Collection> constructor = clazz.getDeclaredConstructor((Class<?>[]) null);
                constructor.setAccessible(true);
                // do the test on 100 objects, to be more accurate
                for (int i = 0; i < 100; i++) {
                    this.collection = (Collection<String>) constructor.newInstance();
                    // polulate
                    collection.addAll(defaultListCtx);
                    warmUp();
                }
                // measure size
                long objectSize = (long) ((ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() - usedMemory) / 100f);
                LOGGER.info(clazz.getCanonicalName() + " Object size : " + objectSize + " bytes");
                colMemoryResults.put((Class<? extends Collection<?>>) clazz, objectSize);
                collection.clear();
                collection = null;
            } catch (Exception e) {
                System.err.println("Failed running Benchmark on class " + clazz.getCanonicalName());
                e.printStackTrace();
            }
        }
    }

    /**
     * Force (very) heavy GC
     */
    private void heavyGc() {
        try {
            System.gc();
            Thread.sleep(200);
//            System.runFinalization();
            Thread.sleep(200);
            System.gc();
            Thread.sleep(200);
//            System.runFinalization();
            Thread.sleep(1000);
            System.gc();
            Thread.sleep(200);
//            System.runFinalization();
            Thread.sleep(200);
            System.gc();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Display Memory results
     */
//    public void displayMemoryResults() {
//        ChartPanel chart = createChart("Memory usage of collections",
//                "Memory usage (bytes) of collections populated by " + populateSize + " element(s)", colMemoryResults,
//                new StandardCategoryItemLabelGenerator());
//        JFrame frame = new JFrame("Collection Implementations Benchmark");
//        frame.getContentPane().add(chart, BorderLayout.CENTER);
//        frame.setSize(900, 500);
//        frame.setLocationRelativeTo(null);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setVisible(true);
//    }

    /**
     * BenchRunnable
     *
     */
    private interface BenchRunnable {
        /**
         * Runnable that can exploit the current loop index
         *
         * @param loopIndex loop index
         */
        void run(int loopIndex);
    }
}

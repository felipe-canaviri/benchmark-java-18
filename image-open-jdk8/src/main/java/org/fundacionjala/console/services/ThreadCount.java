package org.fundacionjala.console.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadCount extends Thread {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public ThreadCount() {
        super("Overriding Thread Class");
        LOGGER.info("New thread created" + this);
        start();
    }

    public void run() { //Run Method
        try {
            for (int i = 0; i < 100; i++) {
                LOGGER.info("New thread created" + this);
                Thread.sleep(2);
            }
        } catch (InterruptedException e) {
            LOGGER.info("Currently executing thread is interrupted");
        }
        LOGGER.info("Currently executing thread run is terminated");
    }
}

/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.hugegraph.config.CoreOptions;
import org.slf4j.Logger;

import org.apache.hugegraph.HugeException;
import org.apache.hugegraph.task.TaskManager.ContextCallable;

public final class Consumers<V> {

    public static final int THREADS = 4 + CoreOptions.CPUS / 4;
    public static final int QUEUE_WORKER_SIZE = 1000;
    public static final long CONSUMER_WAKE_PERIOD = 1;

    private static final Logger LOG = Log.logger(Consumers.class);

    private final ExecutorService executor;
    private final Consumer<V> consumer;
    private final Runnable done;

    private final int workers;
    private final int queueSize;
    private final CountDownLatch latch;
    private final BlockingQueue<V> queue;

    private volatile boolean ending = false;
    private volatile Throwable exception = null;

    public Consumers(ExecutorService executor, Consumer<V> consumer) {
        this(executor, consumer, null);
    }

    public Consumers(ExecutorService executor,
                     Consumer<V> consumer, Runnable done) {
        this.executor = executor;
        this.consumer = consumer;
        this.done = done;

        int workers = THREADS;
        if (this.executor instanceof ThreadPoolExecutor) {
            workers = ((ThreadPoolExecutor) this.executor).getCorePoolSize();
        }
        this.workers = workers;
        this.queueSize = QUEUE_WORKER_SIZE * workers;
        this.latch = new CountDownLatch(workers);
        this.queue = new ArrayBlockingQueue<>(this.queueSize);
    }

    public void start(String name) {
        this.ending = false;
        this.exception = null;
        if (this.executor == null) {
            return;
        }
        LOG.info("Starting {} workers[{}] with queue size {}...",
                 this.workers, name, this.queueSize);
        for (int i = 0; i < this.workers; i++) {
            this.executor.submit(new ContextCallable<>(this::runAndDone));
        }
    }

    private Void runAndDone() {
        try {
            this.run();
        } catch (Throwable e) {
            // Only the first exception of one thread can be stored
            this.exception = e;
            if (!(e instanceof StopExecution)) {
                LOG.error("Error when running task", e);
            }
        } finally {
            this.done();
            this.latch.countDown();
        }
        return null;
    }

    private void run() {
        LOG.debug("Start to work...");
        while (!this.ending) {
            this.consume();
        }
        assert this.ending;
        while (this.consume()){
            // ignore
        }

        LOG.debug("Worker finished");
    }

    private boolean consume() {
        V elem;
        try {
            elem = this.queue.poll(CONSUMER_WAKE_PERIOD, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // ignore
            return true;
        }
        if (elem == null) {
            return false;
        }
        // do job
        this.consumer.accept(elem);
        return true;
    }

    private void done() {
        if (this.done == null) {
            return;
        }

        try {
            this.done.run();
        } catch (Throwable e) {
            if (this.exception == null) {
                this.exception = e;
            } else {
                LOG.warn("Error while calling done()", e);
            }
        }
    }

    private Throwable throwException() {
        assert this.exception != null;
        Throwable e = this.exception;
        this.exception = null;
        return e;
    }

    public void provide(V v) throws Throwable {
        if (this.executor == null) {
            assert this.exception == null;
            // do job directly if without thread pool
            this.consumer.accept(v);
        } else if (this.exception != null) {
            throw this.throwException();
        } else {
            try {
                this.queue.put(v);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while enqueue", e);
            }
        }
    }

    public void await() throws Throwable {
        this.ending = true;
        if (this.executor == null) {
            // call done() directly if without thread pool
            this.done();
        } else {
            try {
                this.latch.await();
            } catch (InterruptedException e) {
                String error = "Interrupted while waiting for consumers";
                this.exception = new HugeException(error, e);
                LOG.warn(error, e);
            }
        }

        if (this.exception != null) {
            throw this.throwException();
        }
    }

    public ExecutorService executor() {
        return this.executor;
    }

    public static void executeOncePerThread(ExecutorService executor,
                                            int totalThreads,
                                            Runnable callback)
                                            throws InterruptedException {
        // Ensure callback execute at least once for every thread
        final Map<Thread, Integer> threadsTimes = new ConcurrentHashMap<>();
        final List<Callable<Void>> tasks = new ArrayList<>();
        final Callable<Void> task = () -> {
            Thread current = Thread.currentThread();
            threadsTimes.putIfAbsent(current, 0);
            int times = threadsTimes.get(current);
            if (times == 0) {
                callback.run();
                // Let other threads run
                Thread.yield();
            } else {
                assert times < totalThreads;
                assert threadsTimes.size() < totalThreads;
                E.checkState(tasks.size() == totalThreads,
                             "Bad tasks size: %s", tasks.size());
                // Let another thread run and wait for it
                executor.submit(tasks.get(0)).get();
            }
            threadsTimes.put(current, ++times);
            return null;
        };

        // NOTE: expect each task thread to perform a close operation
        for (int i = 0; i < totalThreads; i++) {
            tasks.add(task);
        }
        executor.invokeAll(tasks);
    }

    public static ExecutorService newThreadPool(String prefix, int workers) {
        if (workers == 0) {
            return null;
        } else {
            if (workers < 0) {
                assert workers == -1;
                workers = Consumers.THREADS;
            } else if (workers > CoreOptions.CPUS * 2) {
                workers = CoreOptions.CPUS * 2;
            }
            String name = prefix + "-worker-%d";
            return ExecutorUtil.newFixedThreadPool(workers, name);
        }
    }

    public static ExecutorPool newExecutorPool(String prefix, int workers) {
        return new ExecutorPool(prefix, workers);
    }

    public static RuntimeException wrapException(Throwable e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new HugeException("Error when running task: %s",
                                HugeException.rootCause(e).getMessage(), e);
    }

    public static class ExecutorPool {

        private static final int POOL_CAPACITY = 2 * CoreOptions.CPUS;

        private final String threadNamePrefix;
        private final int executorWorkers;
        private final AtomicInteger count;

        private final Queue<ExecutorService> executors;

        public ExecutorPool(String prefix, int workers) {
            this.threadNamePrefix = prefix;
            this.executorWorkers = workers;
            this.count = new AtomicInteger();
            this.executors = new ArrayBlockingQueue<>(POOL_CAPACITY);
        }

        public synchronized ExecutorService getExecutor() {
            ExecutorService executor = this.executors.poll();
            if (executor == null) {
                int count = this.count.incrementAndGet();
                String prefix = this.threadNamePrefix + "-" + count;
                executor = newThreadPool(prefix, this.executorWorkers);
            }
            return executor;
        }

        public synchronized void returnExecutor(ExecutorService executor) {
            E.checkNotNull(executor, "executor");
            if (!this.executors.offer(executor)) {
                executor.shutdown();
            }
        }

        public synchronized void destroy() {
            for (ExecutorService executor : this.executors) {
                executor.shutdown();
            }
            this.executors.clear();
        }
    }

    public static class StopExecution extends HugeException {

        private static final long serialVersionUID = -371829356182454517L;

        public StopExecution(String message) {
            super(message);
        }

        public StopExecution(String message, Object... args) {
            super(message, args);
        }
    }
}

/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.common.pack;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class BoundedWorkQueue<K>
implements AutoCloseable {
    private final LinkedHashMap<K, Job<K>> waiting = new LinkedHashMap();
    private final Set<K> active = new HashSet<K>();
    private final ExecutorService workers;
    private final Consumer<Throwable> errors;
    private int limit;
    private boolean closed;

    public BoundedWorkQueue(int n, Consumer<Throwable> consumer) {
        this.limit = Math.max(1, n);
        this.errors = consumer;
        this.workers = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "Enorials-Pack-Builder");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void limit(int n) {
        this.limit = Math.max(1, n);
        this.pump();
    }

    public synchronized boolean submit(K k, Runnable runnable, IntConsumer intConsumer) {
        if (this.closed || this.waiting.containsKey(k) || this.active.contains(k)) {
            return false;
        }
        this.waiting.put(k, new Job<K>(k, runnable, intConsumer));
        this.pump();
        return true;
    }

    public synchronized boolean cancel(K k) {
        Job job = (Job)this.waiting.remove(k);
        if (job == null) {
            return false;
        }
        this.report(job, -2);
        this.positions();
        return true;
    }

    public synchronized int activeCount() {
        return this.active.size();
    }

    public synchronized int waitingCount() {
        return this.waiting.size();
    }

    private void report(Job<K> job, int n) {
        try {
            job.position.accept(n);
        }
        catch (Throwable throwable) {
            this.errors.accept(throwable);
        }
    }

    private void positions() {
        int n = 0;
        for (Job<K> job : this.waiting.values()) {
            this.report(job, ++n);
        }
    }

    private void pump() {
        if (this.closed) {
            return;
        }
        while (this.active.size() < this.limit && !this.waiting.isEmpty()) {
            Iterator<Job<K>> iterator = this.waiting.values().iterator();
            Job<K> job = iterator.next();
            iterator.remove();
            this.active.add(job.key);
            this.report(job, 0);
            this.workers.execute(() -> {
                try {
                    job.work.run();
                }
                catch (Throwable throwable) {
                    this.errors.accept(throwable);
                }
                finally {
                    BoundedWorkQueue boundedWorkQueue = this;
                    synchronized (boundedWorkQueue) {
                        this.active.remove(job.key);
                        this.report(job, -1);
                        this.pump();
                    }
                }
            });
        }
        this.positions();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() {
        BoundedWorkQueue boundedWorkQueue = this;
        synchronized (boundedWorkQueue) {
            this.closed = true;
            for (Job<K> job : this.waiting.values()) {
                this.report(job, -2);
            }
            this.waiting.clear();
        }
        this.workers.shutdownNow();
        try {
            if (!this.workers.awaitTermination(30L, TimeUnit.SECONDS)) {
                this.errors.accept(new IllegalStateException("Pack worker did not stop within 30 seconds"));
            }
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private record Job<K>(K key, Runnable work, IntConsumer position) {
    }
}


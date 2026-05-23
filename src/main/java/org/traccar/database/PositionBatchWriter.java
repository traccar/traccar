/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.database;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Position;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class PositionBatchWriter {

    private static final Request INSERT_REQUEST = new Request(new Columns.Exclude("id"));

    private record Entry(Position position, CompletableFuture<Long> future) {}

    private final Storage storage;
    private final ConcurrentLinkedQueue<Entry> queue;
    private final int batchSize;

    @Inject
    public PositionBatchWriter(Config config, Storage storage) {
        this.storage = storage;
        this.batchSize = config.getInteger(Keys.DATABASE_POSITION_BATCH_SIZE);
        long interval = config.getLong(Keys.DATABASE_POSITION_BATCH_INTERVAL);
        if (interval > 0) {
            queue = new ConcurrentLinkedQueue<>();
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "PositionBatchWriter");
                thread.setDaemon(true);
                return thread;
            });
            scheduler.scheduleWithFixedDelay(this::flush, interval, interval, TimeUnit.MILLISECONDS);
        } else {
            queue = null;
        }
    }

    public CompletableFuture<Long> submit(Position position) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        if (queue == null) {
            try {
                future.complete(storage.addObject(position, INSERT_REQUEST));
            } catch (StorageException error) {
                future.completeExceptionally(error);
            }
        } else {
            queue.offer(new Entry(position, future));
        }
        return future;
    }

    private void flush() {
        List<Entry> batch = new ArrayList<>(batchSize);
        Entry entry;
        while (batch.size() < batchSize && (entry = queue.poll()) != null) {
            batch.add(entry);
        }
        if (batch.isEmpty()) {
            return;
        }
        try {
            List<Position> positions = batch.stream().map(Entry::position).toList();
            List<Long> ids = storage.addObjects(positions, INSERT_REQUEST);
            for (int i = 0; i < batch.size(); i++) {
                batch.get(i).future().complete(ids.get(i));
            }
        } catch (Exception error) {
            batch.forEach(e -> e.future().completeExceptionally(error));
        }
    }

}

/*
 * Copyright 2025 Haven Madray (sgpublic2002@gmail.com)
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
package org.traccar.helper;

import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class BlockingBucketTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingBucketTest.class);

    private static final int QPS = 3;
    private static final int QPD = 5000;
    private static final int TEST_ROUND = 5;

    @Test
    public void test() throws InterruptedException {
        BlockingBucket bucket = Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(QPD)
                        .refillIntervally(QPD, Duration.ofDays(1)))
                .addLimit(limit -> limit
                        .capacity(QPS)
                        .refillGreedy(QPS, Duration.ofSeconds(1))
                        .initialTokens(0))
                .build()
                .asBlocking();

        long startTime = System.currentTimeMillis();
        long currentTime = startTime;
        for (int i = 0; i < TEST_ROUND * QPS + 1; i++) {
            LOGGER.info("Wait for consume token...");
            bucket.consume(1);
            LOGGER.info("Consume 1 token ({} in total) in {}ms", i + 1, System.currentTimeMillis() - currentTime);
            currentTime = System.currentTimeMillis();
        }
        assertTrue(System.currentTimeMillis() - startTime >= 1000 * TEST_ROUND);
    }
}

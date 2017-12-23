// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.client.retry;

import org.traccar.fcm.client.functional.Action0;
import org.traccar.fcm.client.functional.Func1;
import org.traccar.fcm.client.retry.strategy.IRetryStrategy;
import org.traccar.fcm.client.retry.strategy.SimpleRetryStrategy;

/**
 * This class implements RetryStrategies, for explicitly retrying requests to the FCM server.
 */
public final class RetryUtils {

    private RetryUtils() {

    }

    /**
     * Retries a method with the SimpleRetryStrategy and a maximum amount of retries.
     *
     * @param function   Function to retry.
     * @param maxRetries The Maximum Number of Retries.
     * @param <TResult>  The Result of the Method.
     * @return Result of the Method invocation.
     */
    public static <TResult> TResult getWithRetry(Func1<TResult> function, int maxRetries) {
        IRetryStrategy retryStrategy = new SimpleRetryStrategy(maxRetries);

        return getWithRetry(function, retryStrategy);
    }

    /**
     * Retries a method with the given Retry Strategy.
     *
     * @param function      Function to retry.
     * @param retryStrategy RetryStrategy to apply.
     * @param <TResult>     Result of the invocation.
     * @return Result of the Method invocation.
     */
    public static <TResult> TResult getWithRetry(Func1<TResult> function, IRetryStrategy retryStrategy) {
        return retryStrategy.getWithRetry(function);
    }

    /**
     * Retries a method with the SimpleRetryStrategy and a maximum amount of retries.
     *
     * @param action     Action to retry.
     * @param maxRetries The Maximum Number of Retries.
     */
    public static void doWithRetry(Action0 action, int maxRetries) {
        IRetryStrategy retryStrategy = new SimpleRetryStrategy(maxRetries);

        doWithRetry(action, retryStrategy);
    }

    /**
     * Retries a method with the given Retry Strategy.
     *
     * @param action        Action to retry.
     * @param retryStrategy RetryStrategy to apply.
     */
    public static void doWithRetry(Action0 action, IRetryStrategy retryStrategy) {
        retryStrategy.doWithRetry(action);
    }

}

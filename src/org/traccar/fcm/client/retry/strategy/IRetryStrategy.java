// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.client.retry.strategy;

import org.traccar.fcm.client.functional.Action0;
import org.traccar.fcm.client.functional.Func1;

/**
 * A Retry Strategy used to retry a function without a return value (@see {@link Action0}) and
 * functions with return values (@see {@link Func1}.
 */
public interface IRetryStrategy {

    /**
     * Retries a function without a return value.
     *
     * @param action Action to invoke.
     */
    void doWithRetry(Action0 action);

    /**
     * Retries a function with a return values.
     *
     * @param function  Function to invoke.
     * @param <TResult> Result of the invocation.
     * @return Result of the invocation.
     */
    <TResult> TResult getWithRetry(Func1<TResult> function);

}

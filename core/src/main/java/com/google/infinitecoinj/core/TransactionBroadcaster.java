/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.infinitecoinj.core;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A general interface which declares the ability to broadcast transactions. This is implemented
 * by {@link com.google.infinitecoinj.core.PeerGroup}.
 */
public interface TransactionBroadcaster {
    /** Broadcast the given transaction on the network */
    public ListenableFuture<Transaction> broadcastTransaction(final Transaction tx);
}

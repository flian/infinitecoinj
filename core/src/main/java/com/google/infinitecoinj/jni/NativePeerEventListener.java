/**
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

package com.google.infinitecoinj.jni;

import com.google.infinitecoinj.core.*;

import java.util.List;

/**
 * An event listener that relays events to a native C++ object. A pointer to that object is stored in
 * this class using JNI on the native side, thus several instances of this can point to different actual
 * native implementations.
 */
public class NativePeerEventListener implements PeerEventListener {
    public long ptr;

    @Override
    public native void onBlocksDownloaded(Peer peer, Block block, int blocksLeft);

    @Override
    public native void onChainDownloadStarted(Peer peer, int blocksLeft);

    @Override
    public native void onPeerConnected(Peer peer, int peerCount);

    @Override
    public native void onPeerDisconnected(Peer peer, int peerCount);

    @Override
    public native Message onPreMessageReceived(Peer peer, Message m);

    @Override
    public native void onTransaction(Peer peer, Transaction t);

    @Override
    public native List<Message> getData(Peer peer, GetDataMessage m);
}

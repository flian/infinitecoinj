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

package com.google.infinitecoinj.core;

import com.google.infinitecoinj.store.BlockStore;
import com.google.infinitecoinj.store.BlockStoreException;
import com.google.infinitecoinj.store.FullPrunedBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.*;

/**
 * <p>Vends hard-coded {@link StoredBlock}s for blocks throughout the chain. Checkpoints serve two purposes:</p>
 * <ol>
 *    <li>They act as a safety mechanism against huge re-orgs that could rewrite large chunks of history, thus
 *    constraining the block chain to be a consensus mechanism only for recent parts of the timeline.</li>
 *    <li>They allow synchronization to the head of the chain for new wallets/users much faster than syncing all
 *    headers from the genesis block.</li>
 * </ol>
 *
 * <p>Checkpoints are used by the SPV {@link BlockChain} to initialize fresh
 * {@link com.google.infinitecoinj.store.SPVBlockStore}s. They are not used by fully validating mode, which instead has a
 * different concept of checkpoints that are used to hard-code the validity of blocks that violate BIP30 (duplicate
 * coinbase transactions). Those "checkpoints" can be found in NetworkParameters.</p>
 *
 * <p>The file format consists of the string "CHECKPOINTS 1", followed by a uint32 containing the number of signatures
 * to read. The value may not be larger than 256 (so it could have been a byte but isn't for historical reasons).
 * If the number of signatures is larger than zero, each 65 byte ECDSA secp256k1 signature then follows. The signatures
 * sign the hash of all bytes that follow the last signature.</p>
 *
 * <p>After the signatures come an int32 containing the number of checkpoints in the file. Then each checkpoint follows
 * one after the other. A checkpoint is 12 bytes for the total work done field, 4 bytes for the height, 80 bytes
 * for the block header and then 1 zero byte at the end (i.e. number of transactions in the block: always zero).</p>
 */
public class CheckpointManager {
    private static final Logger log = LoggerFactory.getLogger(CheckpointManager.class);

    private static final int MAX_SIGNATURES = 256;

    // Map of block header time to data.
    protected final TreeMap<Long, StoredBlock> checkpoints = new TreeMap<Long, StoredBlock>();

    protected final NetworkParameters params;
    protected final Sha256Hash dataHash;

    public CheckpointManager(NetworkParameters params, InputStream inputStream) throws IOException {
        this.params = checkNotNull(params);
        checkNotNull(inputStream);
        DataInputStream dis = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);
            dis = new DataInputStream(digestInputStream);
            digestInputStream.on(false);
            String magic = "CHECKPOINTS 1";
            byte[] header = new byte[magic.length()];
            dis.readFully(header);
            if (!Arrays.equals(header, magic.getBytes("US-ASCII")))
                throw new IOException("Header bytes did not match expected version");
            int numSignatures = checkPositionIndex(dis.readInt(), MAX_SIGNATURES, "Num signatures out of range");
            for (int i = 0; i < numSignatures; i++) {
                byte[] sig = new byte[65];
                dis.readFully(sig);
                // TODO: Do something with the signature here.
            }
            digestInputStream.on(true);
            int numCheckpoints = dis.readInt();
            checkState(numCheckpoints > 0);
            final int size = StoredBlock.COMPACT_SERIALIZED_SIZE;
            ByteBuffer buffer = ByteBuffer.allocate(size);
            for (int i = 0; i < numCheckpoints; i++) {
                if (dis.read(buffer.array(), 0, size) < size)
                    throw new IOException("Incomplete read whilst loading checkpoints.");
                StoredBlock block = StoredBlock.deserializeCompact(params, buffer);
                buffer.position(0);
                checkpoints.put(block.getHeader().getTimeSeconds(), block);
            }
            dataHash = new Sha256Hash(digest.digest());
            log.info("Read {} checkpoints, hash is {}", checkpoints.size(), dataHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Cannot happen.
        } catch (ProtocolException e) {
            throw new IOException(e);
        } finally {
            if (dis != null) dis.close();
            inputStream.close();
        }
    }

    /**
     * Returns a {@link StoredBlock} representing the last checkpoint before the given time, for example, normally
     * you would want to know the checkpoint before the earliest wallet birthday.
     */
    public StoredBlock getCheckpointBefore(long time) {
        try {
            checkArgument(time > params.getGenesisBlock().getTimeSeconds());
            // This is thread safe because the map never changes after creation.
            Map.Entry<Long, StoredBlock> entry = checkpoints.floorEntry(time);
            if (entry != null) return entry.getValue();
            Block genesis = params.getGenesisBlock().cloneAsHeader();
            return new StoredBlock(genesis, genesis.getWork(), 0);
        } catch (VerificationException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }



    /** Returns the number of checkpoints that were loaded. */
    public int numCheckpoints() {
        return checkpoints.size();
    }

    /** Returns a hash of the concatenated checkpoint data. */
    public Sha256Hash getDataHash() {
        return dataHash;
    }

    /**
     * <p>Convenience method that creates a CheckpointManager, loads the given data, gets the checkpoint for the given
     * time, then inserts it into the store and sets that to be the chain head. Useful when you have just created
     * a new store from scratch and want to use configure it all in one go.</p>
     *
     * <p>Note that time is adjusted backwards by a week to account for possible clock drift in the block headers.</p>
     */
    //for testing of the forks
    long timeBegin = 0;
    long timeFork1 = 0;
    long timeFork2 = 0;
    long timeFork3 = 0;
    public static void checkpoint(NetworkParameters params, InputStream checkpoints, BlockStore store, long time)
            throws IOException, BlockStoreException {

        if(!CoinDefinition.checkpointFileSupport)
            return;

        checkNotNull(params);
        checkNotNull(store);
        checkArgument(!(store instanceof FullPrunedBlockStore), "You cannot use checkpointing with a full store.");

        time -= 86400 * 7;

        BufferedInputStream stream = new BufferedInputStream(checkpoints);
        CheckpointManager manager = new CheckpointManager(params, stream);
        StoredBlock checkpoint = manager.getCheckpointBefore(time);

        store.put(checkpoint);
        store.setChainHead(checkpoint);
    }
}

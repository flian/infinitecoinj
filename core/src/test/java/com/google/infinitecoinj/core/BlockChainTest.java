/**
 * Copyright 2011 Google Inc.
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

import com.google.infinitecoinj.core.Wallet.BalanceType;
import com.google.infinitecoinj.params.MainNetParams;
import com.google.infinitecoinj.params.TestNet2Params;
import com.google.infinitecoinj.params.UnitTestParams;
import com.google.infinitecoinj.store.BlockStore;
import com.google.infinitecoinj.store.MemoryBlockStore;
import com.google.infinitecoinj.utils.BriefLogFormatter;
import com.google.infinitecoinj.utils.TestUtils;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.infinitecoinj.utils.TestUtils.createFakeBlock;
import static com.google.infinitecoinj.utils.TestUtils.createFakeTx;
import static org.junit.Assert.*;

// Handling of chain splits/reorgs are in ChainSplitTests.

public class BlockChainTest {
    private BlockChain testNetChain;

    private Wallet wallet;
    private BlockChain chain;
    private BlockStore blockStore;
    private Address coinbaseTo;
    private NetworkParameters unitTestParams;
    private final StoredBlock[] block = new StoredBlock[1];
    private Transaction coinbaseTransaction;

    private static class TweakableTestNet2Params extends TestNet2Params {
        public void setProofOfWorkLimit(BigInteger limit) {
            proofOfWorkLimit = limit;
        }
    }
    private static final TweakableTestNet2Params testNet = new TweakableTestNet2Params();

    private void resetBlockStore() {
        blockStore = new MemoryBlockStore(unitTestParams);
    }

    @Before
    public void setUp() throws Exception {
        BriefLogFormatter.initVerbose();
        testNetChain = new BlockChain(testNet, new Wallet(testNet), new MemoryBlockStore(testNet));
        Wallet.SendRequest.DEFAULT_FEE_PER_KB = BigInteger.ZERO;

        unitTestParams = UnitTestParams.get();
        wallet = new Wallet(unitTestParams) {
            @Override
            public void receiveFromBlock(Transaction tx, StoredBlock block, BlockChain.NewBlockType blockType,
                                         int relativityOffset) throws VerificationException {
                super.receiveFromBlock(tx, block, blockType, relativityOffset);
                BlockChainTest.this.block[0] = block;
                if (tx.isCoinBase()) {
                    BlockChainTest.this.coinbaseTransaction = tx;
                }
            }
        };
        wallet.addKey(new ECKey());

        resetBlockStore();
        chain = new BlockChain(unitTestParams, wallet, blockStore);

        coinbaseTo = wallet.getKeys().get(0).toAddress(unitTestParams);
    }

    @After
    public void tearDown() {
        Wallet.SendRequest.DEFAULT_FEE_PER_KB = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
    }

    @Test
    public void testBasicChaining() throws Exception {
        // Check that we can plug a few blocks together and the futures work.
        ListenableFuture<StoredBlock> future = testNetChain.getHeightFuture(2);
        // Block 1 from the testnet.
        Block b1 = getBlock1();
        assertTrue(testNetChain.add(b1));
        assertFalse(future.isDone());
        // Block 2 from the testnet.
        Block b2 = getBlock2();

        // Let's try adding an invalid block.
        long n = b2.getNonce();
        try {
            b2.setNonce(12345);
            testNetChain.add(b2);
            fail();
        } catch (VerificationException e) {
            b2.setNonce(n);
        }

        // Now it works because we reset the nonce.
        assertTrue(testNetChain.add(b2));
        assertTrue(future.isDone());
        assertEquals(2, future.get().getHeight());
    }

    @Test
    public void receiveCoins() throws Exception {
        // Quick check that we can actually receive coins.
        Transaction tx1 = createFakeTx(unitTestParams,
                                       Utils.toNanoCoins(1, 0),
                                       wallet.getKeys().get(0).toAddress(unitTestParams));
        Block b1 = createFakeBlock(blockStore, tx1).block;
        chain.add(b1);
        assertTrue(wallet.getBalance().signum() > 0);
    }

    @Test
    public void merkleRoots() throws Exception {
        // Test that merkle root verification takes place when a relevant transaction is present and doesn't when
        // there isn't any such tx present (as an optimization).
        Transaction tx1 = createFakeTx(unitTestParams,
                                       Utils.toNanoCoins(1, 0),
                                       wallet.getKeys().get(0).toAddress(unitTestParams));
        Block b1 = createFakeBlock(blockStore, tx1).block;
        chain.add(b1);
        resetBlockStore();
        Sha256Hash hash = b1.getMerkleRoot();
        b1.setMerkleRoot(Sha256Hash.ZERO_HASH);
        try {
            chain.add(b1);
            fail();
        } catch (VerificationException e) {
            // Expected.
            b1.setMerkleRoot(hash);
        }
        // Now add a second block with no relevant transactions and then break it.
        Transaction tx2 = createFakeTx(unitTestParams, Utils.toNanoCoins(1, 0),
                                       new ECKey().toAddress(unitTestParams));
        Block b2 = createFakeBlock(blockStore, tx2).block;
        b2.getMerkleRoot();
        b2.setMerkleRoot(Sha256Hash.ZERO_HASH);
        b2.solve();
        chain.add(b2);  // Broken block is accepted because its contents don't matter to us.
    }

    @Test
    public void unconnectedBlocks() throws Exception {
        Block b1 = unitTestParams.getGenesisBlock().createNextBlock(coinbaseTo);
        Block b2 = b1.createNextBlock(coinbaseTo);
        Block b3 = b2.createNextBlock(coinbaseTo);
        // Connected.
        assertTrue(chain.add(b1));
        // Unconnected but stored. The head of the chain is still b1.
        assertFalse(chain.add(b3));
        assertEquals(chain.getChainHead().getHeader(), b1.cloneAsHeader());
        // Add in the middle block.
        assertTrue(chain.add(b2));
        assertEquals(chain.getChainHead().getHeader(), b3.cloneAsHeader());
    }

    @Test
    public void difficultyTransitions() throws Exception {
        // Add a bunch of blocks in a loop until we reach a difficulty transition point. The unit test params have an
        // artificially shortened period.
        Block prev = unitTestParams.getGenesisBlock();
        Utils.setMockClock(System.currentTimeMillis()/1000);
        for (int i = 0; i < unitTestParams.getInterval() - 1; i++) {
            Block newBlock = prev.createNextBlock(coinbaseTo, Utils.currentTimeMillis()/1000);
            assertTrue(chain.add(newBlock));
            prev = newBlock;
            // The fake chain should seem to be "fast" for the purposes of difficulty calculations.
            Utils.rollMockClock(2);
        }
        // Now add another block that has no difficulty adjustment, it should be rejected.
        try {
            chain.add(prev.createNextBlock(coinbaseTo, Utils.currentTimeMillis()/1000));
            fail();
        } catch (VerificationException e) {
        }
        // Create a new block with the right difficulty target given our blistering speed relative to the huge amount
        // of time it's supposed to take (set in the unit test network parameters).
        Block b = prev.createNextBlock(coinbaseTo, Utils.currentTimeMillis()/1000);
        b.setDifficultyTarget(0x201fFFFFL);
        b.solve();
        assertTrue(chain.add(b));
        // Successfully traversed a difficulty transition period.
    }

    @Test
    public void badDifficulty() throws Exception {
        assertTrue(testNetChain.add(getBlock1()));
        Block b2 = getBlock2();
        assertTrue(testNetChain.add(b2));
        Block bad = new Block(testNet);
        // Merkle root can be anything here, doesn't matter.
        bad.setMerkleRoot(new Sha256Hash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        // Nonce was just some number that made the hash < difficulty limit set below, it can be anything.
        bad.setNonce(140548933);
        bad.setTime(1279242649);
        bad.setPrevBlockHash(b2.getHash());
        // We're going to make this block so easy 50% of solutions will pass, and check it gets rejected for having a
        // bad difficulty target. Unfortunately the encoding mechanism means we cannot make one that accepts all
        // solutions.
        bad.setDifficultyTarget(Block.EASIEST_DIFFICULTY_TARGET);
        try {
            testNetChain.add(bad);
            // The difficulty target above should be rejected on the grounds of being easier than the networks
            // allowable difficulty.
            fail();
        } catch (VerificationException e) {
            assertTrue(e.getMessage(), e.getCause().getMessage().contains("Difficulty target is bad"));
        }

        // Accept any level of difficulty now.
        BigInteger oldVal = testNet.getProofOfWorkLimit();
        testNet.setProofOfWorkLimit(new BigInteger
                ("00ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16));
        try {
            testNetChain.add(bad);
            // We should not get here as the difficulty target should not be changing at this point.
            fail();
        } catch (VerificationException e) {
            assertTrue(e.getMessage(), e.getCause().getMessage().contains("Unexpected change in difficulty"));
        }
        testNet.setProofOfWorkLimit(oldVal);

        // TODO: Test difficulty change is not out of range when a transition period becomes valid.
    }

    @Test
    public void duplicates() throws Exception {
        // Adding a block twice should not have any effect, in particular it should not send the block to the wallet.
        Block b1 = unitTestParams.getGenesisBlock().createNextBlock(coinbaseTo);
        Block b2 = b1.createNextBlock(coinbaseTo);
        Block b3 = b2.createNextBlock(coinbaseTo);
        assertTrue(chain.add(b1));
        assertEquals(b1, block[0].getHeader());
        assertTrue(chain.add(b2));
        assertEquals(b2, block[0].getHeader());
        assertTrue(chain.add(b3));
        assertEquals(b3, block[0].getHeader());
        assertEquals(b3, chain.getChainHead().getHeader());
        assertTrue(chain.add(b2));
        assertEquals(b3, chain.getChainHead().getHeader());
        // Wallet was NOT called with the new block because the duplicate add was spotted.
        assertEquals(b3, block[0].getHeader());
    }

    @Test
    public void intraBlockDependencies() throws Exception {
        // Covers issue 166 in which transactions that depend on each other inside a block were not always being
        // considered relevant.
        Address somebodyElse = new ECKey().toAddress(unitTestParams);
        Block b1 = unitTestParams.getGenesisBlock().createNextBlock(somebodyElse);
        ECKey key = new ECKey();
        wallet.addKey(key);
        Address addr = key.toAddress(unitTestParams);
        // Create a tx that gives us some coins, and another that spends it to someone else in the same block.
        Transaction t1 = TestUtils.createFakeTx(unitTestParams, Utils.toNanoCoins(1, 0), addr);
        Transaction t2 = new Transaction(unitTestParams);
        t2.addInput(t1.getOutputs().get(0));
        t2.addOutput(Utils.toNanoCoins(2, 0), somebodyElse);
        b1.addTransaction(t1);
        b1.addTransaction(t2);
        b1.solve();
        chain.add(b1);
        assertEquals(BigInteger.ZERO, wallet.getBalance());
    }

    @Test
    public void coinbaseTransactionAvailability() throws Exception {
        // Check that a coinbase transaction is only available to spend after NetworkParameters.getSpendableCoinbaseDepth() blocks.

        // Create a second wallet to receive the coinbase spend.
        Wallet wallet2 = new Wallet(unitTestParams);
        ECKey receiveKey = new ECKey();
        wallet2.addKey(receiveKey);
        chain.addWallet(wallet2);

        Address addressToSendTo = receiveKey.toAddress(unitTestParams);

        // Create a block, sending the coinbase to the coinbaseTo address (which is in the wallet).
        Block b1 = unitTestParams.getGenesisBlock().createNextBlockWithCoinbase(wallet.getKeys().get(0).getPubKey());
        chain.add(b1);

        // Check a transaction has been received.
        assertNotNull(coinbaseTransaction);

        // The coinbase tx is not yet available to spend.
        assertEquals(BigInteger.ZERO, wallet.getBalance());
        assertEquals(wallet.getBalance(BalanceType.ESTIMATED), Utils.toNanoCoins(50, 0));
        assertTrue(!coinbaseTransaction.isMature());

        // Attempt to spend the coinbase - this should fail as the coinbase is not mature yet.
        try {
            wallet.createSend(addressToSendTo, Utils.toNanoCoins(49, 0));
            fail();
        } catch (InsufficientMoneyException e) {
        }

        // Check that the coinbase is unavailable to spend for the next spendableCoinbaseDepth - 2 blocks.
        for (int i = 0; i < unitTestParams.getSpendableCoinbaseDepth() - 2; i++) {
            // Non relevant tx - just for fake block creation.
            Transaction tx2 = createFakeTx(unitTestParams, Utils.toNanoCoins(1, 0),
                new ECKey().toAddress(unitTestParams));

            Block b2 = createFakeBlock(blockStore, tx2).block;
            chain.add(b2);

            // Wallet still does not have the coinbase transaction available for spend.
            assertEquals(BigInteger.ZERO, wallet.getBalance());
            assertEquals(wallet.getBalance(BalanceType.ESTIMATED), Utils.toNanoCoins(50, 0));

            // The coinbase transaction is still not mature.
            assertTrue(!coinbaseTransaction.isMature());

            // Attempt to spend the coinbase - this should fail.
            try {
                wallet.createSend(addressToSendTo, Utils.toNanoCoins(49, 0));
                fail();
            } catch (InsufficientMoneyException e) {
            }
        }

        // Give it one more block - should now be able to spend coinbase transaction. Non relevant tx.
        Transaction tx3 = createFakeTx(unitTestParams, Utils.toNanoCoins(1, 0), new ECKey().toAddress(unitTestParams));
        Block b3 = createFakeBlock(blockStore, tx3).block;
        chain.add(b3);

        // Wallet now has the coinbase transaction available for spend.
        assertEquals(wallet.getBalance(), Utils.toNanoCoins(50, 0));
        assertEquals(wallet.getBalance(BalanceType.ESTIMATED), Utils.toNanoCoins(50, 0));
        assertTrue(coinbaseTransaction.isMature());

        // Create a spend with the coinbase BTC to the address in the second wallet - this should now succeed.
        Transaction coinbaseSend2 = wallet.createSend(addressToSendTo, Utils.toNanoCoins(49, 0));
        assertNotNull(coinbaseSend2);

        // Commit the coinbaseSpend to the first wallet and check the balances decrement.
        wallet.commitTx(coinbaseSend2);
        assertEquals(wallet.getBalance(BalanceType.ESTIMATED), Utils.toNanoCoins(1, 0));
        // Available balance is zero as change has not been received from a block yet.
        assertEquals(wallet.getBalance(BalanceType.AVAILABLE), Utils.toNanoCoins(0, 0));

        // Give it one more block - change from coinbaseSpend should now be available in the first wallet.
        Block b4 = createFakeBlock(blockStore, coinbaseSend2).block;
        chain.add(b4);
        assertEquals(wallet.getBalance(BalanceType.AVAILABLE), Utils.toNanoCoins(1, 0));

        // Check the balances in the second wallet.
        assertEquals(wallet2.getBalance(BalanceType.ESTIMATED), Utils.toNanoCoins(49, 0));
        assertEquals(wallet2.getBalance(BalanceType.AVAILABLE), Utils.toNanoCoins(49, 0));
    }

    // Some blocks from the test net.
    private static Block getBlock2() throws Exception {
        Block b2 = new Block(testNet);
        b2.setMerkleRoot(new Sha256Hash("addc858a17e21e68350f968ccd384d6439b64aafa6c193c8b9dd66320470838b"));
        b2.setNonce(2642058077L);
        b2.setTime(1296734343L);
        b2.setPrevBlockHash(new Sha256Hash("000000033cc282bc1fa9dcae7a533263fd7fe66490f550d80076433340831604"));
        assertEquals("000000037b21cac5d30fc6fda2581cf7b2612908aed2abbcc429c45b0557a15f", b2.getHashAsString());
        b2.verifyHeader();
        return b2;
    }

    private static Block getBlock1() throws Exception {
        Block b1 = new Block(testNet);
        b1.setMerkleRoot(new Sha256Hash("0e8e58ecdacaa7b3c6304a35ae4ffff964816d2b80b62b58558866ce4e648c10"));
        b1.setNonce(236038445);
        b1.setTime(1296734340);
        b1.setPrevBlockHash(new Sha256Hash("00000007199508e34a9ff81e6ec0c477a4cccff2a4767a8eee39c11db367b008"));
        assertEquals("000000033cc282bc1fa9dcae7a533263fd7fe66490f550d80076433340831604", b1.getHashAsString());
        b1.verifyHeader();
        return b1;
    }

    @Test
    public void estimatedBlockTime() throws Exception {
        NetworkParameters params = MainNetParams.get();
        BlockChain prod = new BlockChain(params, new MemoryBlockStore(params));
        Date d = prod.estimateBlockTime(200000);
        // The actual date of block 200,000 was 2012-09-22 10:47:00
        assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse("2012-10-23T08:35:05.000-0700"), d);
    }

    @Test
    public void falsePositives() throws Exception {
        double decay = AbstractBlockChain.FP_ESTIMATOR_ALPHA;
        assertTrue(0 == chain.getFalsePositiveRate()); // Exactly
        chain.trackFalsePositives(55);
        assertEquals(decay * 55, chain.getFalsePositiveRate(), 1e-4);
        chain.trackFilteredTransactions(550);
        double rate1 = chain.getFalsePositiveRate();
        // Run this scenario a few more time for the filter to converge
        for (int i = 1 ; i < 10 ; i++) {
            chain.trackFalsePositives(55);
            chain.trackFilteredTransactions(550);
        }

        // Ensure we are within 10%
        assertEquals(0.1, chain.getFalsePositiveRate(), 0.01);

        // Check that we get repeatable results after a reset
        chain.resetFalsePositiveEstimate();
        assertTrue(0 == chain.getFalsePositiveRate()); // Exactly

        chain.trackFalsePositives(55);
        assertEquals(decay * 55, chain.getFalsePositiveRate(), 1e-4);
        chain.trackFilteredTransactions(550);
        assertEquals(rate1, chain.getFalsePositiveRate(), 1e-4);
    }
}

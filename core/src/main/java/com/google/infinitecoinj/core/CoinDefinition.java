package com.google.infinitecoinj.core;

import java.math.BigInteger;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: HashEngineering
 * Date: 12/13/13
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoinDefinition {


    public static final String coinName = "Infinitecoin";
    public static final String coinTicker = "IFC";
    public static final String coinURIScheme = "ifc";
    public static final String cryptsyMarketId = "60";
    public static final String cryptsyMarketCurrency = "LTC";
    public static final String PATTERN_PRIVATE_KEY_START = "6";

    public enum CoinPrecision {
        Coins,
        Millicoins,
    }
    public static final CoinPrecision coinPrecision = CoinPrecision.Coins;



    public static final String BLOCKEXPLORER_BASE_URL_PROD = "http://exploretheblocks.com:2750/";
    public static final String BLOCKEXPLORER_BASE_URL_TEST = "http://exploretheblocks.com:2750/";
    public static final String BLOCKEXPLORER_ADDRESS_PATH = "address/";             //blockr.io path
    public static final String BLOCKEXPLORER_TRANSACTION_PATH = "tx/";              //blockr.io path
    public static final String BLOCKEXPLORER_BLOCK_PATH = "block/";                 //blockr.io path

    public static final String DONATION_ADDRESS = "i3p7EagqTjB8F3w9N3D28KiHi2BtpFqMdR";  //HashEngineering donation IFC address

    enum CoinHash {
        SHA256,
        scrypt,
    };
    public static final CoinHash coinPOWHash = CoinHash.scrypt;

    public static boolean checkpointFileSupport = true;
    //Original Values

    public static final int TARGET_TIMESPAN = (int)(60 * 60);  // 60 minutes per difficulty cycle, on average.
    public static final int TARGET_SPACING = (int)(1 * 30);  // 30 seconds per block.


    public static final int INTERVAL = TARGET_TIMESPAN / TARGET_SPACING;  //120 blocks
	public static final int INTERVAL_PPC = 30;


    public static final int getInterval(int height, boolean testNet) {
        if(height < IFC_RETARGET_SWITCH_BLOCK)
            return INTERVAL;
        else if(height < IFC_RETARGET_SWITCH_BLOCK2)
            return INTERVAL;
        else if(height < IFC_RETARGET_SWITCH_BLOCK3)
            return INTERVAL;
        else return INTERVAL_PPC;
    }

    public static final int getTargetTimespan(int height, boolean testNet) {

            return TARGET_TIMESPAN;
    }

    public static int spendableCoinbaseDepth = 60; //main.h: static const int COINBASE_MATURITY
    public static final String MAX_MONEY_STRING = "210000000000";     //main.h:  MAX_MONEY
    public static final BigInteger MAX_MONEY = BigInteger.valueOf(210000000000L).multiply(Utils.COIN);                 //main.h:  MAX_MONEY



    public static final BigInteger DEFAULT_MIN_TX_FEE = BigInteger.valueOf(10000000L);   // MIN_TX_FEE 1 coin
    public static final BigInteger DUST_LIMIT =        BigInteger.valueOf(100000000000L);      //   1000 coins

    public static final int PROTOCOL_VERSION = 69003;          //version.h PROTOCOL_VERSION
    public static final int MIN_PROTOCOL_VERSION = 69001;        //version.h MIN_PROTO_VERSION


    public static final int BLOCK_CURRENTVERSION = 1;   //CBlock::CURRENT_VERSION
    public static final int MAX_BLOCK_SIZE = 1 * 1000 * 1000;


    public static final boolean supportsBloomFiltering = false; //Requires PROTOCOL_VERSION 70000 in the client
    public static boolean supportsIrcDiscovery() {
        return PROTOCOL_VERSION < 70000;
    }

    public static final int Port    = 9321;       //protocol.h GetDefaultPort(testnet=false)
    public static final int TestPort = 19321;     //protocol.h GetDefaultPort(testnet=true)

    //
    //  Production
    //

    public static final int AddressHeader = 102;             //base58.h CBitcoinAddress::PUBKEY_ADDRESS
    public static final int p2shHeader = 5;             //base58.h CBitcoinAddress::SCRIPT_ADDRESS
    public static final boolean allowBitcoinPrivateKey = true; //for backward compatibility with previous version of digitalcoin
    public static final int dumpedPrivateKeyHeader = 128;   //common to all coins
    public static final long PacketMagic = 0xfbc0b6db;      //0xfb, 0xc0, 0xb6, 0xdb

    //Genesis Block Information from main.cpp: LoadBlockIndex
    static public long genesisBlockDifficultyTarget = (0x1e0ffff0L);         //main.cpp: LoadBlockIndex
    static public long genesisBlockTime = 1370324666L;                       //main.cpp: LoadBlockIndex
    static public long genesisBlockNonce = (113458625);                         //main.cpp: LoadBlockIndex
    static public String genesisHash = "b10d5e83a5b2e62d9d872096bc20cae1a276ae6aacc02a71a5832b1fc9aeff85"; //main.cpp: hashGenesisBlock
    static public int genesisBlockValue = 524288;                                                              //main.cpp: LoadBlockIndex
    //taken from the raw data of the block explorer
    static public String genesisXInBytes = "04ffff001d01044b4d69616d69204865617420726f757420496e6469616e61205061636572732039392d37362c20616476616e636520746f204e42412046696e616c73206f6e204a756e6520332c2032303133";   //"Miami Heat rout Indiana Pacers 99-76, advance to NBA Finals on June 3, 2013"
    static public String genessiXOutBytes = "040184710fa689ad5023690c80f3a49c8f13f8d45b8c857fbcbc8bc4a8e4d3eb4b10f4d4604fa08dce601aaf0f470216fe1b51850b4acf21b179c45070ac7b03a9";

    //net.cpp strDNSSeed
    static public String[] dnsSeeds = new String[] {

            //"treasurequarry.com",
            //"exploretheblocks.com",
            //"94.23.16.150",
            "infinitecoin.com", "dnsseed.infinitecoin.com",
            "ifcf2pool.com", "dnsseed.ifcf2pool.com",
            "ifcp2pool.com", "dnsseed.ifcp2pool.com",
            "amxpool.com", "dnsseed.amxpool.com",
            "amzpool.com", "dnsseed.amzpool.com",
            "bmapool.com", "dnsseed.bmapool.com",
            "dyjykj.com", "dnsseed.dyjykj.com",
            "coinwk.com", "dnsseed.coinwk.com",
            "poolwk.com", "dnsseed.poolwk.com",
            "znjjsc.com", "dnsseed.znjjsc.com",
            "amxdns.com", "dnsseed.amxdns.com",
            "smpool.com", "dnsseed.smpool.com",
            "wxbkc.com", "dnsseed.wxbkc.com",
            "ifcqd.com", "dnsseed.ifcqd.com",
            "wwifc.com", "dnsseed.wwifc.com",
            "hchccoin.com", "dnsseed.hchccoin.com",
            "By5002.com", "dnsseed.By5002.com",
    };
    public static int minBroadcastConnections = 1;   //0 for default; we need more peers.

    //
    // TestNet - infinite - not tested
    //
    public static final boolean supportsTestNet = false;
    public static final int testnetAddressHeader = 111;             //base58.h CBitcoinAddress::PUBKEY_ADDRESS_TEST
    public static final int testnetp2shHeader = 196;             //base58.h CBitcoinAddress::SCRIPT_ADDRESS_TEST
    public static final long testnetPacketMagic = 0xfcc1b7dc;      //0xfc, 0xc1, 0xb7, 0xdc
    public static final String testnetGenesisHash = "47cfc1ea5d27a873dcc90fa8befc9378dbc793ca68dd608d4f6aa123437701ba";
    static public long testnetGenesisBlockDifficultyTarget = (0x1e0ffff0L);         //main.cpp: LoadBlockIndex
    static public long testnetGenesisBlockTime = 1367711671;                       //main.cpp: LoadBlockIndex
    static public long testnetGenesisBlockNonce = (111787711);                         //main.cpp: LoadBlockIndex


    public static int IFC_SWITCH_TIME = 1377993600;		// Sept 1, 2013 00:00:00 GMT
	public static int IFC_FEE_MULTIPLICATOR = 100;		// Transaction Fee Multiplicator


	public static int IFC_RETARGET_SWITCH_BLOCK		= 245000;		
	public static int IFC_RETARGET_SWITCH_BLOCK2	= 248000;		
	public static int IFC_RETARGET_SWITCH_BLOCK3	= 272000;	


    //main.cpp GetBlockValue(height, fee)
    public static final BigInteger GetBlockReward(int height)
    {
        BigInteger nSubsidy = Utils.toNanoCoins(524288, 0);

        return nSubsidy.shiftRight(height / subsidyDecreaseBlockCount);

    }

    public static int subsidyDecreaseBlockCount = 86400;     //main.cpp GetBlockValue(height, fee)

    public static BigInteger proofOfWorkLimit = Utils.decodeCompactBits(0x1e0fffffL);  //main.cpp bnProofOfWorkLimit (~Sha256Hash(0) >> 20); // Infinitecoin: starting difficulty is 1 / 2^12

    static public String[] testnetDnsSeeds = new String[] {
          "not supported"
    };
    //from main.h: CAlert::CheckSignature
    public static final String SATOSHI_KEY = "04494b7bde2ff420c3a1bb2b227ad0ecc00f56a9f475648670434b53d80a33904d0afa84d8259dafbfbebe23e43e28d4baa5677805cb6deb92a6de8c97436b243c";
    public static final String TESTNET_SATOSHI_KEY = "04494b7bde2ff420c3a1bb2b227ad0ecc00f56a9f475648670434b53d80a33904d0afa84d8259dafbfbebe23e43e28d4baa5677805cb6deb92a6de8c97436b243c";

    /** The string returned by getId() for the main, production network where people trade things. */

    public static final String ID_MAINNET = "org.infinitcoin.production";
    /** The string returned by getId() for the testnet. */
    public static final String ID_TESTNET = "org.infinitcoin.test";

    public static final String ID_REG_TEST = "org.infinitcoin.regtest";


    /** Unit test network. */
    public static final String ID_UNITTESTNET = "com.google.infinitcoin.unittest";


    //checkpoints.cpp Checkpoints::mapCheckpoints
    public static void initCheckpoints(Map<Integer, Sha256Hash> checkpoints)
    {

        checkpoints.put    (    99, new Sha256Hash("5d39f8648c612d6e01b953fcfc6e7c31a58f086ae4715ae3e5e828cd148052a9"));
        checkpoints.put    (   999, new Sha256Hash("18045133dedbed71aa49aaf1696b65818ca21b20263cd53cc9bd935c1c8be6ee"));
        checkpoints.put    (  9999, new Sha256Hash("d2f1a2f1b8862af96c5a750f3d99680ee96e7a4aac4e27f0587b1dbaa9b9207f"));
        checkpoints.put    ( 49999, new Sha256Hash("ef2a0653071708d6a41dff2bb671bb459879f2c361a06024fca17a2566b41225"));
        checkpoints.put    ( 99979, new Sha256Hash("f376177d849c75c6344fc93c9429f59df7d5b25b067447e694e087bb765128e0"));
        checkpoints.put    (139999, new Sha256Hash("ff9d5edf1661d8cd6fc53ffb9f583b16981874522044a760d8c8c004c312a41e"));
        checkpoints.put    (199999, new Sha256Hash("ec62c7700fd83c56f2013b1b97a7dbcc2aad1f065176ea18d9c47701ced164d5"));
        checkpoints.put    (228800, new Sha256Hash("6a2a329c5d21d6433cf9bda5ba43d66a732898bcd0c81150f1584d095edd5cd5"));
        checkpoints.put    (242388, new Sha256Hash("4c2dfd22435525519e89041420f6692e709da34f48243cebe1be14d43adb1c5c"));
        checkpoints.put    (265420, new Sha256Hash("9ef4ce8e7dab5c2f2b9142ceb285419ef0ea62021e5f613d4a81a3fc9d53453e"));
		checkpoints.put	(265497, new Sha256Hash("206aed8fb5b1ed94cf696bc5b964445380dd4c30c186e45ba5ed545866a242c7")); // keep
        checkpoints.put    (268559, new Sha256Hash("fd0ff0e0020f0ade68122c0fd82f648c7e6913e32cd6a3d8abc81694055daecc")); // keep
		checkpoints.put	(268560, new Sha256Hash("08c5337322ea40d3602b98ab9d9b1d43abd87dda19a4f8e282414a838ae3dbb8"));
        checkpoints.put   (282045, new Sha256Hash("271abe1c26daf5a684034529befb217e16f87e1af779c0e63bdd971def3d8ba5"));
		checkpoints.put	(380962, new Sha256Hash("a032a87b430091fbb4faa20f16c8247f93cfcc1854bd49a19c3c9fc3a0c43634"));
		checkpoints.put	(453211, new Sha256Hash("ea08eace1b78c5513d74750c7cfc01d0c1f3789fc650ccce197b85497405ce56"));

        checkpoints.put	(647000, new Sha256Hash("667fc19d6066d472176136f2a34dac9b2662e9d7212df3679e56042d8b198906"));
        checkpoints.put	(4333333, new Sha256Hash("8e4fcf3e003293882dec8943e42ed29e066cfc3782ff4d011c58c8b8ed9bb9a4"));
        checkpoints.put	(4368037, new Sha256Hash("b60d656dcc3fdd60b279a7de7ad8c21950c4cbae58698219785249e0174d4bcd"));
        checkpoints.put	(5781980, new Sha256Hash("a91e780de0b3f91f1b0515e56296b54804730cdfd21a45b4d2c433a3f92aebb7"));
        checkpoints.put	(6043693, new Sha256Hash("5518c133fead075e372027e69c2b245a01d5a6e95ef3c97d576bf42a62d1e6ef"));
        checkpoints.put	(6887217, new Sha256Hash("58edc430d6ecc35ae3fa4d8ccdfb8a42752ff771d2a08c8e6dc1f1b3b6782d4d"));
        checkpoints.put	(7081698, new Sha256Hash("4342f79249ffe1c4afe08fca143ee9945579528a9047afc0e79ef276ea6fde6b"));
        checkpoints.put	(7900916, new Sha256Hash("185d45e33bb5c8546b0923bd9c31c64c1b11eba2ae51afe549ce3e5aba63f70e"));
        checkpoints.put	(8578526, new Sha256Hash("a5d4766c2e9ba13efd942f2bfcb1849ce824b54130c9de3c15e80afec0f81aa5"));
        checkpoints.put	(9983739, new Sha256Hash("7ab96e7972bae3d3100fc2997fc8d9036955330e6a18c9c10a0c2c96d780c976"));
        checkpoints.put	(10096530, new Sha256Hash("320a5d057453df2c5e46d24a62b959bc1c4948dddf96f6f0d1489f86d517b565"));
    }

    //Unit Test Information
    public static final String UNITTEST_ADDRESS = "iF9mQhwCeqBkoQa5vZ3NAyLNT1vAhoB2fG";
    public static final String UNITTEST_ADDRESS_PRIVATE_KEY = "b8AF462o5QQZvgVNeU47HBKMZ9SgMBhPXrV52kizJbZJK8ngG4ah";

}

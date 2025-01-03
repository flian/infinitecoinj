/*
 * Copyright 2012 the original author or authors.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.google.infinitecoinj.uri;

import com.google.infinitecoinj.core.Address;
import com.google.infinitecoinj.core.Utils;
import com.google.infinitecoinj.params.MainNetParams;
import com.google.infinitecoinj.params.TestNet3Params;
import org.junit.Test;
import com.google.infinitecoinj.core.CoinDefinition;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import static org.junit.Assert.*;


public class BitcoinURITest {
    private BitcoinURI testObject = null;

    private static final String MAINNET_GOOD_ADDRESS = CoinDefinition.UNITTEST_ADDRESS; //"1KzTSfqjF2iKCduwz59nv2uqh1W2JsTxZH";

    @Test
    public void testConvertToBitcoinURI() throws Exception {
        Address goodAddress = new Address(MainNetParams.get(), MAINNET_GOOD_ADDRESS);
        
        // simple example
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello&message=AMessage", BitcoinURI.convertToBitcoinURI(goodAddress, Utils.toNanoCoins("12.34"), "Hello", "AMessage"));
        
        // example with spaces, ampersand and plus
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello%20World&message=Mess%20%26%20age%20%2B%20hope", BitcoinURI.convertToBitcoinURI(goodAddress, Utils.toNanoCoins("12.34"), "Hello World", "Mess & age + hope"));

        // no amount, label present, message present
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?label=Hello&message=glory", BitcoinURI.convertToBitcoinURI(goodAddress, null, "Hello", "glory"));
        
        // amount present, no label, message present
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?amount=0.1&message=glory", BitcoinURI.convertToBitcoinURI(goodAddress, Utils.toNanoCoins("0.1"), null, "glory"));
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?amount=0.1&message=glory", BitcoinURI.convertToBitcoinURI(goodAddress, Utils.toNanoCoins("0.1"), "", "glory"));

        // amount present, label present, no message
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello", BitcoinURI.convertToBitcoinURI(goodAddress, Utils.toNanoCoins("12.34"), "Hello", null));
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello", BitcoinURI.convertToBitcoinURI(goodAddress, Utils.toNanoCoins("12.34"), "Hello", ""));
              
        // amount present, no label, no message
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?amount=1000", BitcoinURI.convertToBitcoinURI(goodAddress, Utils.toNanoCoins("1000"), null, null));
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?amount=1000", BitcoinURI.convertToBitcoinURI(goodAddress, Utils.toNanoCoins("1000"), "", ""));
        
        // no amount, label present, no message
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?label=Hello", BitcoinURI.convertToBitcoinURI(goodAddress, null, "Hello", null));
        
        // no amount, no label, message present
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?message=Agatha", BitcoinURI.convertToBitcoinURI(goodAddress, null, null, "Agatha"));
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS + "?message=Agatha", BitcoinURI.convertToBitcoinURI(goodAddress, null, "", "Agatha"));
      
        // no amount, no label, no message
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS, BitcoinURI.convertToBitcoinURI(goodAddress, null, null, null));
        assertEquals(CoinDefinition.coinURIScheme + ":" + MAINNET_GOOD_ADDRESS, BitcoinURI.convertToBitcoinURI(goodAddress, null, "", ""));
    }

    @Test
    public void testGood_Simple() throws BitcoinURIParseException {
        testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS);
        assertNotNull(testObject);
        assertNull("Unexpected amount", testObject.getAmount());
        assertNull("Unexpected label", testObject.getLabel());
        assertEquals("Unexpected label", 20, testObject.getAddress().getHash160().length);
    }

    /**
     * Test a broken URI (bad scheme)
     */
    @Test
    public void testBad_Scheme() {
        try {
            testObject = new BitcoinURI(MainNetParams.get(), "blimpcoin:" + MAINNET_GOOD_ADDRESS);
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
        }
    }

    /**
     * Test a broken URI (bad syntax)
     */
    @Test
    public void testBad_BadSyntax() {
        // Various illegal characters
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + "|" + MAINNET_GOOD_ADDRESS);
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }

        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "\\");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }

        // Separator without field
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }
    }

    /**
     * Test a broken URI (missing address)
     */
    @Test
    public void testBad_Address() {
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME);
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
        }
    }

    /**
     * Test a broken URI (bad address type)
     */
    @Test
    public void testBad_IncorrectAddressType() {
        try {
            testObject = new BitcoinURI(TestNet3Params.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS);
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("Bad address"));
        }
    }

    /**
     * Handles a simple amount
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Amount() throws BitcoinURIParseException {
        // Test the decimal parsing
        testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210.12345678");
        assertEquals("654321012345678", testObject.getAmount().toString());

        // Test the decimal parsing
        testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=.12345678");
        assertEquals("12345678", testObject.getAmount().toString());

        // Test the integer parsing
        testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210");
        assertEquals("654321000000000", testObject.getAmount().toString());
    }

    /**
     * Handles a simple label
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Label() throws BitcoinURIParseException {
        testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?label=Hello%20World");
        assertEquals("Hello World", testObject.getLabel());
    }

    /**
     * Handles a simple label with an embedded ampersand and plus
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     * @throws UnsupportedEncodingException 
     */
    @Test
    public void testGood_LabelWithAmpersandAndPlus() throws Exception {
        String testString = "Hello Earth & Mars + Venus";
        String encodedLabel = BitcoinURI.encodeURLString(testString);
        testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "?label="
                + encodedLabel);
        assertEquals(testString, testObject.getLabel());
    }

    /**
     * Handles a Russian label (Unicode test)
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     * @throws UnsupportedEncodingException 
     */
    @Test
    public void testGood_LabelWithRussian() throws Exception {
        // Moscow in Russian in Cyrillic
        String moscowString = "\u041c\u043e\u0441\u043a\u0432\u0430";
        String encodedLabel = BitcoinURI.encodeURLString(moscowString); 
        testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "?label="
                + encodedLabel);
        assertEquals(moscowString, testObject.getLabel());
    }

    /**
     * Handles a simple message
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Message() throws BitcoinURIParseException {
        testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?message=Hello%20World");
        assertEquals("Hello World", testObject.getMessage());
    }

    /**
     * Handles various well-formed combinations
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Combinations() throws BitcoinURIParseException {
        testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210&label=Hello%20World&message=Be%20well");
        assertEquals(
                "BitcoinURI['amount'='654321000000000','label'='Hello World','message'='Be well','address'='"+CoinDefinition.UNITTEST_ADDRESS+"']",
                testObject.toString());
    }

    /**
     * Handles a badly formatted amount field
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_Amount() throws BitcoinURIParseException {
        // Missing
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?amount=");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("amount"));
        }

        // Non-decimal (BIP 21)
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?amount=12X4");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("amount"));
        }
    }

    /**
     * Handles a badly formatted label field
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_Label() throws BitcoinURIParseException {
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?label=");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("label"));
        }
    }

    /**
     * Handles a badly formatted message field
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_Message() throws BitcoinURIParseException {
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?message=");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("message"));
        }
    }

    /**
     * Handles duplicated fields (sneaky address overwrite attack)
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_Duplicated() throws BitcoinURIParseException {
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?address=aardvark");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("address"));
        }
    }

    /**
     * Handles case when there are too many equals
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_TooManyEquals() throws BitcoinURIParseException {
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?label=aardvark=zebra");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("cannot parse name value pair"));
        }
    }

    /**
     * Handles case when there are too many question marks
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_TooManyQuestionMarks() throws BitcoinURIParseException {
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?label=aardvark?message=zebra");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("Too many question marks"));
        }
    }
    
    /**
     * Handles unknown fields (required and not required)
     * 
     * @throws BitcoinURIParseException
     *             If something goes wrong
     */
    @Test
    public void testUnknown() throws BitcoinURIParseException {
        // Unknown not required field
        testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?aardvark=true");
        assertEquals("BitcoinURI['aardvark'='true','address'='"+CoinDefinition.UNITTEST_ADDRESS+"']", testObject.toString());

        assertEquals("true", (String) testObject.getParameterByName("aardvark"));

        // Unknown not required field (isolated)
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?aardvark");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("cannot parse name value pair"));
        }

        // Unknown and required field
        try {
            testObject = new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?req-aardvark=true");
            fail("Expecting BitcoinURIParseException");
        } catch (BitcoinURIParseException e) {
            assertTrue(e.getMessage().contains("req-aardvark"));
        }
    }

    @Test
    public void brokenURIs() throws BitcoinURIParseException {
        // Check we can parse the incorrectly formatted URIs produced by blockchain.info and its iPhone app.
        String str = CoinDefinition.coinURIScheme + "://"+CoinDefinition.UNITTEST_ADDRESS+"?amount=0.01000000";
        BitcoinURI uri = new BitcoinURI(str);
        assertEquals(CoinDefinition.UNITTEST_ADDRESS, uri.getAddress().toString());
        assertEquals(Utils.toNanoCoins(0, 1), uri.getAmount());
    }

    @Test(expected = BitcoinURIParseException.class)
    public void testBad_AmountTooPrecise() throws BitcoinURIParseException {
        new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=0.123456789");
    }

    @Test(expected = BitcoinURIParseException.class)
    public void testBad_NegativeAmount() throws BitcoinURIParseException {
        new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=-1");
    }

    @Test(expected = BitcoinURIParseException.class)
    public void testBad_TooLargeAmount() throws BitcoinURIParseException {
        new BitcoinURI(MainNetParams.get(), BitcoinURI.BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount="+(CoinDefinition.MAX_MONEY.multiply(BigInteger.valueOf(5))));
    }

    @Test
    public void testPaymentProtocolReq() throws Exception {
        // Non-backwards compatible form ...
        BitcoinURI uri = new BitcoinURI(TestNet3Params.get(), CoinDefinition.coinURIScheme + ":?r=https%3A%2F%2Fbitcoincore.org%2F%7Egavin%2Ff.php%3Fh%3Db0f02e7cea67f168e25ec9b9f9d584f9");
        assertEquals("https://bitcoincore.org/~gavin/f.php?h=b0f02e7cea67f168e25ec9b9f9d584f9", uri.getPaymentRequestUrl());
        assertNull(uri.getAddress());
    }
}

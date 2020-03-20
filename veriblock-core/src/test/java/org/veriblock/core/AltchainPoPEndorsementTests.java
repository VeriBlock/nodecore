// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core;

import org.veriblock.core.altchain.AltchainPoPEndorsement;
import org.junit.Assert;
import org.junit.Test;
import org.veriblock.core.utilities.Utility;

public class AltchainPoPEndorsementTests {

    @Test
    public void testMinimalEndorsement_1() {
        try {
            String identifierLength = "01";
            String identifier = "00";
            String lengthOfLengthOfHeader = "01"; // 1
            String lengthOfHeader = "01";
            String header = "FF";
            String noContextInfo = "00";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 0);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testMinimalEndorsement_2() {
        try {
            String identifierLength = "01";
            String identifier = "00";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "04"; // = 4
            String header = "AABBCCDD";
            String noContextInfo = "00";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 0);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testMinimalEndorsement_3() {
        try {
            String identifierLength = "01";
            String identifier = "00";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String noContextInfo = "00";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 0);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testMinimalEndorsement_4() {
        try {
            String identifierLength = "01";
            String identifier = "00";
            String lengthOfLengthOfHeader = "02";
            String lengthOfHeader = "0100"; // = 256
            StringBuilder headerSB = new StringBuilder();
            for (int i = 0; i < 256; i++) {
                headerSB.append("AB");
            }
            String header = headerSB.toString();
            String noContextInfo = "00";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 0);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testMinimalEndorsement_5() {
        try {
            String identifierLength = "01";
            String identifier = "00";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "FF"; // = 255
            StringBuilder headerSB = new StringBuilder();
            for (int i = 0; i < 255; i++) {
                headerSB.append("AB");
            }
            String header = headerSB.toString();
            String noContextInfo = "00";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 0);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testMinimalEndorsement_6() {
        try {
            String identifierLength = "01";
            String identifier = "00";
            String lengthOfLengthOfHeader = "02";
            String lengthOfHeader = "F000"; // = 61440
            StringBuilder headerSB = new StringBuilder();
            for (int i = 0; i < 61440; i++) {
                headerSB.append("AB");
            }
            String header = headerSB.toString();
            String noContextInfo = "00";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 0);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testMinimalEndorsement_7() {
        try {
            String identifierLength = "02";
            String identifier = "0000";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "06"; // = 6
            String header = "ABCDEF987654";
            String noContextInfo = "00";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 0);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testMinimalEndorsement_8() {
        try {
            String identifierLength = "07";
            String identifier = "00000000000001";
            String lengthOfLengthOfHeader = "02";
            String lengthOfHeader = "0006"; // = 6
            String header = "ABCDEF987654";
            String noContextInfo = "00";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 1);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testMinimalEndorsement_9() {
        try {
            String identifierLength = "08";
            String identifier = "3F00000000000000";
            String lengthOfLengthOfHeader = "02";
            String lengthOfHeader = "0006"; // = 6
            String header = "ABCDEF987654";
            String noContextInfo = "00";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4539628424389459968L);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContext_1() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContextInfo = "01";
            String lengthOfContextInfo = "01"; // = 1
            String contextInfo = "FF";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContextInfo,
                            lengthOfContextInfo,
                            contextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContext_2() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContextInfo = "01";
            String lengthOfContextInfo = "0F"; // = 15
            String contextInfo = "00112233445566778899AABBCCDDEE";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContextInfo,
                            lengthOfContextInfo,
                            contextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContext_3() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContextInfo = "01";
            String lengthOfContextInfo = "10"; // = 15
            String contextInfo = "00112233445566778899AABBCCDDEEFF";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContextInfo,
                            lengthOfContextInfo,
                            contextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContext_4() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContextInfo = "01";
            String lengthOfContextInfo = "FF"; // = 255
            StringBuilder contextSB = new StringBuilder();
            for (int i = 0; i < 255; i++) {
                contextSB.append("AB");
            }
            String contextInfo = contextSB.toString();
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContextInfo,
                            lengthOfContextInfo,
                            contextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContext_5() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContextInfo = "02";
            String lengthOfContextInfo = "0100"; // = 256
            StringBuilder contextSB = new StringBuilder();
            for (int i = 0; i < 256; i++) {
                contextSB.append("AB");
            }
            String contextInfo = contextSB.toString();
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContextInfo,
                            lengthOfContextInfo,
                            contextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContext_6() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContextInfo = "02";
            String lengthOfContextInfo = "1000"; // = 4096
            StringBuilder contextSB = new StringBuilder();
            for (int i = 0; i < 4096; i++) {
                contextSB.append("AB");
            }
            String contextInfo = contextSB.toString();
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContextInfo,
                            lengthOfContextInfo,
                            contextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContext_7() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContextInfo = "02";
            String lengthOfContextInfo = "0001"; // = 1
            String contextInfo = "AB";
            String noPayoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContextInfo,
                            lengthOfContextInfo,
                            contextInfo,
                            noPayoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertEquals(popEndorsement.getPayoutInfo().length, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithPayout_1() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String noContext = "00";
            String lengthOfLengthOfPayoutInfo = "01";
            String lengthOfPayoutInfo = "01"; // = 1
            String payoutInfo = "00";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContext,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithPayout_2() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String noContext = "00";
            String lengthOfLengthOfPayoutInfo = "01";
            String lengthOfPayoutInfo = "08"; // = 8
            String payoutInfo = "0011223344556677";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContext,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithPayout_3() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String noContext = "00";
            String lengthOfLengthOfPayoutInfo = "01";
            String lengthOfPayoutInfo = "FF"; // = 255
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 255; i++) {
                payoutSB.append("AB");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContext,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithPayout_4() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String noContext = "00";
            String lengthOfLengthOfPayoutInfo = "02";
            String lengthOfPayoutInfo = "0100"; // = 256
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 256; i++) {
                payoutSB.append("AB");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContext,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithPayout_5() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String noContext = "00";
            String lengthOfLengthOfPayoutInfo = "02";
            String lengthOfPayoutInfo = "0FFF"; // = 4095
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 4095; i++) {
                payoutSB.append("AB");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContext,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithPayout_6() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String noContext = "00";
            String lengthOfLengthOfPayoutInfo = "02";
            String lengthOfPayoutInfo = "1000"; // = 4096
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 4096; i++) {
                payoutSB.append("AB");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContext,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithPayout_7() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String noContext = "00";
            String lengthOfLengthOfPayoutInfo = "02";
            String lengthOfPayoutInfo = "0001"; // = 1
            String payoutInfo = "FF";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            noContext,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertEquals(popEndorsement.getContextInfo().length, 0);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContextAndPayout_1() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContext = "01";
            String lengthOfContext = "01"; // = 1
            String contextInfo = "00";
            String lengthOfLengthOfPayoutInfo = "01";
            String lengthOfPayoutInfo = "01"; // = 1
            String payoutInfo = "FF";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContext,
                            lengthOfContext,
                            contextInfo,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContextAndPayout_2() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContext = "02";
            String lengthOfContext = "0001"; // = 1
            String contextInfo = "00";
            String lengthOfLengthOfPayoutInfo = "02";
            String lengthOfPayoutInfo = "0001"; // = 1
            String payoutInfo = "FF";

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContext,
                            lengthOfContext,
                            contextInfo,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContextAndPayout_3() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContext = "01";
            String lengthOfContext = "FF"; // = 255
            StringBuilder contextSB = new StringBuilder();
            for (int i = 0; i < 255; i++) {
                contextSB.append("AB");
            }
            String contextInfo = contextSB.toString();
            String lengthOfLengthOfPayoutInfo = "01";
            String lengthOfPayoutInfo = "01"; // = 1
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 1; i++) {
                payoutSB.append("CD");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContext,
                            lengthOfContext,
                            contextInfo,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContextAndPayout_4() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContext = "02";
            String lengthOfContext = "0100"; // = 256
            StringBuilder contextSB = new StringBuilder();
            for (int i = 0; i < 256; i++) {
                contextSB.append("AB");
            }
            String contextInfo = contextSB.toString();
            String lengthOfLengthOfPayoutInfo = "01";
            String lengthOfPayoutInfo = "01"; // = 1
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 1; i++) {
                payoutSB.append("CD");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContext,
                            lengthOfContext,
                            contextInfo,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContextAndPayout_5() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContext = "02";
            String lengthOfContext = "1000"; // = 4096
            StringBuilder contextSB = new StringBuilder();
            for (int i = 0; i < 4096; i++) {
                contextSB.append("AB");
            }
            String contextInfo = contextSB.toString();
            String lengthOfLengthOfPayoutInfo = "01";
            String lengthOfPayoutInfo = "01"; // = 1
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 1; i++) {
                payoutSB.append("CD");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContext,
                            lengthOfContext,
                            contextInfo,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContextAndPayout_6() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContext = "02";
            String lengthOfContext = "0100"; // = 256
            StringBuilder contextSB = new StringBuilder();
            for (int i = 0; i < 256; i++) {
                contextSB.append("AB");
            }
            String contextInfo = contextSB.toString();
            String lengthOfLengthOfPayoutInfo = "02";
            String lengthOfPayoutInfo = "0001"; // = 1
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 1; i++) {
                payoutSB.append("CD");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContext,
                            lengthOfContext,
                            contextInfo,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContextAndPayout_7() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContext = "02";
            String lengthOfContext = "0100"; // = 256
            StringBuilder contextSB = new StringBuilder();
            for (int i = 0; i < 256; i++) {
                contextSB.append("AB");
            }
            String contextInfo = contextSB.toString();
            String lengthOfLengthOfPayoutInfo = "01";
            String lengthOfPayoutInfo = "FF"; // = 255
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 255; i++) {
                payoutSB.append("CD");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContext,
                            lengthOfContext,
                            contextInfo,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContextAndPayout_8() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContext = "02";
            String lengthOfContext = "1000"; // = 4096
            StringBuilder contextSB = new StringBuilder();
            for (int i = 0; i < 4096; i++) {
                contextSB.append("AB");
            }
            String contextInfo = contextSB.toString();
            String lengthOfLengthOfPayoutInfo = "02";
            String lengthOfPayoutInfo = "0001"; // = 1
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 1; i++) {
                payoutSB.append("CD");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContext,
                            lengthOfContext,
                            contextInfo,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEndorsementWithContextAndPayout_9() {
        try {
            String identifierLength = "04";
            String identifier = "FFFFFFFF";
            String lengthOfLengthOfHeader = "01";
            String lengthOfHeader = "10"; // = 16
            String header = "00112233445566778899AABBCCDDEEFF";
            String lengthOfLengthOfContext = "02";
            String lengthOfContext = "1001"; // = 4097
            StringBuilder contextSB = new StringBuilder();
            for (int i = 0; i < 4097; i++) {
                contextSB.append("FF");
            }
            String contextInfo = contextSB.toString();
            String lengthOfLengthOfPayoutInfo = "02";
            String lengthOfPayoutInfo = "1001"; // = 4097
            StringBuilder payoutSB = new StringBuilder();
            for (int i = 0; i < 4097; i++) {
                payoutSB.append("00");
            }
            String payoutInfo = payoutSB.toString();

            AltchainPoPEndorsement popEndorsement = new AltchainPoPEndorsement
                    (assembleHex(
                            identifierLength,
                            identifier,
                            lengthOfLengthOfHeader,
                            lengthOfHeader,
                            header,
                            lengthOfLengthOfContext,
                            lengthOfContext,
                            contextInfo,
                            lengthOfLengthOfPayoutInfo,
                            lengthOfPayoutInfo,
                            payoutInfo));

            Assert.assertEquals(Utility.bytesToHex(popEndorsement.getHeader()), header);
            Assert.assertEquals(popEndorsement.getIdentifier(), 4294967295L);
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getContextInfo(), Utility.hexToBytes(contextInfo)));
            Assert.assertTrue(Utility.byteArraysAreEqual(popEndorsement.getPayoutInfo(), Utility.hexToBytes(payoutInfo)));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_1() {
        String identifierLength = "01";
        String identifier = "0000"; // IDENTIFIER TOO LONG
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_2() {
        String identifierLength = "00"; // Can't have 0-length identifier

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_3() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "0100"; // Too large!
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_4() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "0000"; // Too large!
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_5() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "04"; // Too high of a value!
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_6() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "0001"; // Too long!
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_7() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "0000"; // Too long!
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_8() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "04"; // Value too high!
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_9() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "0001"; // Too long!
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_10() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "0000"; // Too long!

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_11() {
        String identifierLength = "09"; // Value too high!
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_12() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";
        String extraInfoAtEnd = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo,
                        extraInfoAtEnd));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_13() {
        String extraInfoAtBeginning = "01";
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        extraInfoAtBeginning,
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_14() {
        String identifierLength = "01";
        String extraInfoInMiddle = "00";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        extraInfoInMiddle,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_15() {
        String identifierLength = "01";
        String identifier = "00";
        String extraInfoInMiddle = "01";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        extraInfoInMiddle,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_16() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String extraInfoInMiddle = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        extraInfoInMiddle,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_17() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String extraInfoInMiddle = "00";
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        extraInfoInMiddle,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_18() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String extraInfoInMiddle = "01";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        extraInfoInMiddle,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_19() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String extraInfoInMiddle = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        extraInfoInMiddle,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_20() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String extraInfoInMiddle = "00";
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        extraInfoInMiddle,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_21() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String extraInfoInMiddle = "01";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        extraInfoInMiddle,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_22() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String extraInfoInMiddle = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        extraInfoInMiddle,
                        lengthOfPayoutInfo,
                        payoutInfo));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadEndorsement_23() {
        String identifierLength = "01";
        String identifier = "00";
        String lengthOfLengthOfHeader = "01";
        String lengthOfHeader = "01"; // = 1
        String header = "00";
        String lengthOfLengthOfContext = "01";
        String lengthOfContext = "01"; // = 1
        String contextInfo = "00";
        String lengthOfLengthOfPayoutInfo = "01";
        String lengthOfPayoutInfo = "01"; // = 1
        String extraInfoInMiddle = "01";
        String payoutInfo = "00";

        new AltchainPoPEndorsement
                (assembleHex(
                        identifierLength,
                        identifier,
                        lengthOfLengthOfHeader,
                        lengthOfHeader,
                        header,
                        lengthOfLengthOfContext,
                        lengthOfContext,
                        contextInfo,
                        lengthOfLengthOfPayoutInfo,
                        lengthOfPayoutInfo,
                        extraInfoInMiddle,
                        payoutInfo));

    }

    private static byte[] assembleHex(String... parts) {
        StringBuilder assembled = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            assembled.append(parts[i]);
        }

        return Utility.hexToBytes(assembled.toString());
    }
}

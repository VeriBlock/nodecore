package veriblock.admin.temp.grpc.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import veriblock.model.LedgerContext;
import veriblock.model.LedgerProofStatus;
import veriblock.model.LedgerValue;

public class LedgerContextTest {

    private LedgerContext addressDoesntExist;
    private LedgerContext addressIsInvalid;
    private LedgerContext addressIsExist;

    @Before
    public void setUp() {
        this.addressDoesntExist = new LedgerContext();
        addressDoesntExist.setLedgerProofStatus(LedgerProofStatus.ADDRESS_DOES_NOT_EXIST);

        this.addressIsInvalid= new LedgerContext();
        addressIsInvalid.setLedgerProofStatus(LedgerProofStatus.ADDRESS_IS_INVALID);

        this.addressIsExist= new LedgerContext();
        addressIsExist.setLedgerProofStatus(LedgerProofStatus.ADDRESS_EXISTS);
    }

    @Test
    public void compareToWhenStartValueIsNull() {
        LedgerContext ledgerContext = new LedgerContext();

        Assert.assertEquals(1, ledgerContext.compareTo(addressDoesntExist));
        Assert.assertEquals(1, ledgerContext.compareTo(addressIsInvalid));
        Assert.assertEquals(1, ledgerContext.compareTo(addressIsExist));
    }

    @Test
    public void compareToWhenStartValueAddressNotExist() {
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_DOES_NOT_EXIST);

        Assert.assertEquals(-1, ledgerContext.compareTo(addressDoesntExist));
        Assert.assertEquals(-1, ledgerContext.compareTo(addressIsInvalid));
        Assert.assertEquals(1, ledgerContext.compareTo(addressIsExist));
    }


    @Test
    public void compareToWhenStartValueAddressIsExist() {
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_EXISTS);

        Assert.assertEquals(-1, ledgerContext.compareTo(addressDoesntExist));
        Assert.assertEquals(-1, ledgerContext.compareTo(addressIsInvalid));
    }


    @Test
    public void compareToWhenStartValueAddressIsExistAndSignatureIndexLess() {
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_EXISTS);
        ledgerContext.setLedgerValue(new LedgerValue(0l, 0l, 1l));

        addressIsExist.setLedgerValue(new LedgerValue(0l, 0l, 0l));

        Assert.assertEquals(-1, ledgerContext.compareTo(addressIsExist));
    }

    @Test
    public void compareToWhenStartValueAddressIsExistAndSignatureIndexMore() {
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_EXISTS);
        ledgerContext.setLedgerValue(new LedgerValue(0l, 0l, 1l));

        addressIsExist.setLedgerValue(new LedgerValue(0l, 0l, 3l));

        Assert.assertEquals(1, ledgerContext.compareTo(addressIsExist));
    }

    @Test
    public void compareToWhenAddressIsExistAndSignatureIndexEqualButUnitsLess() {
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_EXISTS);
        ledgerContext.setLedgerValue(new LedgerValue(100l, 0l, 3l));

        addressIsExist.setLedgerValue(new LedgerValue(0l, 0l, 3l));

        Assert.assertEquals(-1, ledgerContext.compareTo(addressIsExist));
    }

    @Test
    public void compareToWhenAddressIsExistAndSignatureIndexEqualButUnitsMore() {
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_EXISTS);
        ledgerContext.setLedgerValue(new LedgerValue(100l, 0l, 3l));

        addressIsExist.setLedgerValue(new LedgerValue(101l, 0l, 3l));

        Assert.assertEquals(1, ledgerContext.compareTo(addressIsExist));
    }

    @Test
    public void compareToWhenAddressIsExistAndSignatureIndexEqual() {
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_EXISTS);
        ledgerContext.setLedgerValue(new LedgerValue(100l, 0l, 3l));

        addressIsExist.setLedgerValue(new LedgerValue(100l, 0l, 3l));

        Assert.assertEquals(-1, ledgerContext.compareTo(addressIsExist));
    }
}
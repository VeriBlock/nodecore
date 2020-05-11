package veriblock.admin.temp.grpc.model

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import veriblock.model.LedgerContext
import veriblock.model.LedgerProofStatus
import veriblock.model.LedgerValue

class LedgerContextTest {

    private var addressDoesntExist: LedgerContext? = null
    private var addressIsInvalid: LedgerContext? = null
    private var addressIsExist: LedgerContext? = null

    @Before
    fun setUp() {
        addressDoesntExist = LedgerContext().also {
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_DOES_NOT_EXIST
        }
        addressIsInvalid = LedgerContext().also {
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_IS_INVALID
        }
        addressIsExist = LedgerContext().also {
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
        }
    }

    @Test
    fun compareToWhenStartValueIsNull() {
        val ledgerContext = LedgerContext()
        Assert.assertEquals(1, ledgerContext.compareTo(addressDoesntExist!!).toLong())
        Assert.assertEquals(1, ledgerContext.compareTo(addressIsInvalid!!).toLong())
        Assert.assertEquals(1, ledgerContext.compareTo(addressIsExist!!).toLong())
    }

    @Test
    fun compareToWhenStartValueAddressNotExist() {
        val ledgerContext = LedgerContext().also {
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_DOES_NOT_EXIST
        }
        Assert.assertEquals(-1, ledgerContext.compareTo(addressDoesntExist!!).toLong())
        Assert.assertEquals(-1, ledgerContext.compareTo(addressIsInvalid!!).toLong())
        Assert.assertEquals(1, ledgerContext.compareTo(addressIsExist!!).toLong())
    }

    @Test
    fun compareToWhenStartValueAddressIsExist() {
        val ledgerContext = LedgerContext().also {
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
        }
        Assert.assertEquals(-1, ledgerContext.compareTo(addressDoesntExist!!).toLong())
        Assert.assertEquals(-1, ledgerContext.compareTo(addressIsInvalid!!).toLong())
    }

    @Test
    fun compareToWhenStartValueAddressIsExistAndSignatureIndexLess() {
        val ledgerContext = LedgerContext().also {
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
            it.ledgerValue = LedgerValue(0L, 0L, 1L)
        }
        addressIsExist!!.ledgerValue = LedgerValue(0L, 0L, 0L)
        Assert.assertEquals(-1, ledgerContext.compareTo(addressIsExist!!).toLong())
    }

    @Test
    fun compareToWhenStartValueAddressIsExistAndSignatureIndexMore() {
        val ledgerContext = LedgerContext().also {
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
            it.ledgerValue = LedgerValue(0L, 0L, 1L)
        }
        addressIsExist!!.ledgerValue = LedgerValue(0L, 0L, 3L)
        Assert.assertEquals(1, ledgerContext.compareTo(addressIsExist!!).toLong())
    }

    @Test
    fun compareToWhenAddressIsExistAndSignatureIndexEqualButUnitsLess() {
        val ledgerContext = LedgerContext().also {
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
            it.ledgerValue = LedgerValue(100L, 0L, 3L)
        }
        addressIsExist!!.ledgerValue = LedgerValue(0L, 0L, 3L)
        Assert.assertEquals(-1, ledgerContext.compareTo(addressIsExist!!).toLong())
    }

    @Test
    fun compareToWhenAddressIsExistAndSignatureIndexEqualButUnitsMore() {
        val ledgerContext = LedgerContext().also {
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
            it.ledgerValue = LedgerValue(100L, 0L, 3L)
        }
        addressIsExist!!.ledgerValue = LedgerValue(101L, 0L, 3L)
        Assert.assertEquals(1, ledgerContext.compareTo(addressIsExist!!).toLong())
    }

    @Test
    fun compareToWhenAddressIsExistAndSignatureIndexEqual() {
        val ledgerContext = LedgerContext().also {
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
            it.ledgerValue = LedgerValue(100L, 0L, 3L)
        }
        addressIsExist!!.ledgerValue = LedgerValue(100L, 0L, 3L)
        Assert.assertEquals(-1, ledgerContext.compareTo(addressIsExist!!).toLong())
    }
}

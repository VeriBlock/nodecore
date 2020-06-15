package org.veriblock.core

enum class VeriBlockError(
    val code: Int,
    val title: String
) {
    GENERIC_ERROR               ( 0, "Generic error"),
    INVALID_ADDRESS             ( 1, "Invalid address"),
    WALLET                      ( 2, "Wallet error"),
    ENCRYPT                     ( 3, "Encryption failed"),
    DECRYPT                     ( 4, "Decryption failed"),
    ADDRESS_CREATION            ( 5, "Address creation failed"),
    TRANSACTION_CREATION        ( 6, "Transaction creation failed"),
    SIGN_MESSAGE                ( 7, "Sign message failed"),
    TRANSACTION_SUBMISSION      ( 8, "Transaction submission failed"),
    POP                         ( 9, "PoP failed"),
    POOL                        (10, "Pool error"),
    PROTOCOL                    (11, "Unknown protocol command"),
    MINE                        (12, "Unable to mine"),
    COMMAND                     (13, "Unable to run the command"),
    FUNDS                       (14, "Insufficient funds"),
    TRANSACTION_LOCK            (15, "Temporarily unable to create Tx"),
    EXCEEDED_MAXTRANSACTION_FEE (16, "Exceeded max fee"),
    DUPLICATE_TRANSACTION       (17, "Duplicate transaction"),
    SEND_COINS                  (18, "Send failed"),
    EXPORT                      (19, "Export failed"),
    IMPORT                      (20, "Unable to import"),
    INPUT                       (21, "Invalid input"),
    CONFIGURATION               (22, "Failed to set config"),
    COMMUNICATION               (23, "Nodecore communication error"),
    ENDORSEMENT_CREATION        (24, "Endorsement creation failed"),
    WALLET_LOCKED               (25, "Wallet is locked"),
    WALLET_UNREADABLE           (26, "Unable to parse wallet file"),
    // HTTP codes
    NOT_FOUND                   (404, "Not found"),
    INTERNAL_ERROR              (500, "Internal error"),
    NOT_IMPLEMENTED             (999, "Not implemented")
}

open class VeriBlockException(
    val error: VeriBlockError,
    override val message: String
) : RuntimeException()

class GenericException(message: String) : VeriBlockException(VeriBlockError.GENERIC_ERROR, message)
class EndorsementCreationException(message: String) : VeriBlockException(VeriBlockError.ENDORSEMENT_CREATION, message)
class InvalidAddressException(message: String) : VeriBlockException(VeriBlockError.INVALID_ADDRESS, message)
class WalletException(message: String) : VeriBlockException(VeriBlockError.WALLET, message)
class EncryptException(message: String) : VeriBlockException(VeriBlockError.ENCRYPT, message)
class DecryptException(message: String) : VeriBlockException(VeriBlockError.DECRYPT, message)
class AddressCreationException(message: String) : VeriBlockException(VeriBlockError.ADDRESS_CREATION, message)
class TransactionCreationException(message: String) : VeriBlockException(VeriBlockError.TRANSACTION_CREATION, message)
class SignMessageException(message: String) : VeriBlockException(VeriBlockError.SIGN_MESSAGE, message)
class TransactionSubmissionException(message: String) : VeriBlockException(VeriBlockError.TRANSACTION_SUBMISSION, message)
class PopException(message: String) : VeriBlockException(VeriBlockError.POP, message)
class PoolException(message: String) : VeriBlockException(VeriBlockError.POOL, message)
class CommunicationException(message: String) : VeriBlockException(VeriBlockError.COMMUNICATION, message)
class ProtocolException(message: String) : VeriBlockException(VeriBlockError.PROTOCOL, message)
class MineException(message: String) : VeriBlockException(VeriBlockError.MINE, message)
class CommandException(message: String) : VeriBlockException(VeriBlockError.COMMAND, message)
class FundsException(message: String) : VeriBlockException(VeriBlockError.FUNDS, message)
class TransactionLockException(message: String) : VeriBlockException(VeriBlockError.TRANSACTION_LOCK, message)
class ExceededMaxTransactionFeeException(message: String) : VeriBlockException(VeriBlockError.EXCEEDED_MAXTRANSACTION_FEE, message)
class DuplicateTransactionException(message: String) : VeriBlockException(VeriBlockError.DUPLICATE_TRANSACTION, message)
class SendCoinsException(message: String) : VeriBlockException(VeriBlockError.SEND_COINS, message)
class ExportException(message: String) : VeriBlockException(VeriBlockError.EXPORT, message)
class ImportException(message: String) : VeriBlockException(VeriBlockError.IMPORT, message)
class InputException(message: String) : VeriBlockException(VeriBlockError.INPUT, message)
class ConfigurationException(message: String) : VeriBlockException(VeriBlockError.CONFIGURATION, message)
class WalletLockedException(message: String) : VeriBlockException(VeriBlockError.WALLET_LOCKED, message)
class WalletUnreadableException(message: String) : VeriBlockException(VeriBlockError.WALLET_UNREADABLE, message)

class NotFoundException(message: String) : VeriBlockException(VeriBlockError.NOT_FOUND, message)
class InternalErrorException(message: String) : VeriBlockException(VeriBlockError.INTERNAL_ERROR, message)
class NotImplementedException(message: String) : VeriBlockException(VeriBlockError.NOT_IMPLEMENTED, message)

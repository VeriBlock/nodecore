export enum OperationState {
  INITIAL = 'Initial',
  INSTRUCTION = 'Mining Instruction retrieved',
  ENDORSEMENT_TRANSACTION = 'Endorsement Transaction submitted',
  ENDORSEMENT_TX_CONFIRMED = 'Endorsement Transaction Confirmed',
  BLOCK_OF_PROOF = 'Block of Proof determined',
  PROVEN = 'Endorsement Transaction proven',
  SUBMITTED_POP_DATA = 'VBK Publications submitted',
  PAYOUT_DETECTED = 'Payout detected',
  COMPLETED = 'Completed',
}

export enum OperationStatus {
  PENDING = 'PENDING',
  CURRENT = 'CURRENT',
  DONE = 'DONE',
  FAILED = 'FAILED',
}

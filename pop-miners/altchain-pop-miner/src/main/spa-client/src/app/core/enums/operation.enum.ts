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

export enum OperationWorkflowState {
  'Initial',
  'Retrieve Mining Instruction',
  'Submit Endorsement Transaction',
  'Confirm Endorsement Transaction',
  'Determine Block of Proof',
  'Prove Endorsement Transaction',
  'Submit PoP Transaction',
  'Detect Payout',
  'Complete and save',
}

export enum OperationStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  DONE = 'DONE',
  FAILED = 'FAILED',
  EMPTY = '',
}

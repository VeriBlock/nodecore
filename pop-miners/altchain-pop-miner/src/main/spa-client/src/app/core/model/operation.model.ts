import { OperationStatus } from '@core/enums';

export class OperationSummaryResponse {
  chain: string;
  endorsedBlockHeight?: number;
  operationId: string;
  state: string;
  task: string;
}

export class OperationDetailResponse extends OperationSummaryResponse {
  stateDetail: StateDetail;
}

export class StateDetail {
  expectedRewardBlock?: string;
  chainIdentifier?: string;
  endorsedBlockHeight?: string;
  publicationDataHeader?: string;
  publicationDataContextInfo?: string;
  publicationDataPayoutInfo?: string;
  publicationDataPayoutInfoDisplay?: string;
  vbkContextBlockHashes?: string;
  btcContextBlockHashes?: string;
  vbkEndorsementTxId?: string;
  vbkEndorsementTxFee?: string;
  vbkBlockOfProof?: string;
  vbkBlockOfProofHeight?: string;
  merklePath?: string;
  altAtvId?: string;
  altAtvBlock?: string;
  altAtvCurrentConfirmations?: string;
  altAtvRequiredConfirmations?: string;
  payoutBlockHash?: string;
  payoutAmount?: string;
  failureReason?: string;
}

export class OperationSummaryListResponse {
  operations: OperationSummaryResponse[];
  totalCount: number;
}

export class OperationWorkflowStage {
  status: OperationStatus;
  taskName: string;
  extraInformation: string;
}

export class OperationWorkflow {
  operationId: string;
  stages: OperationWorkflowStage[];
}

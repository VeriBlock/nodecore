export class OperationSummaryResponse {
  chain: string;
  endorsedBlockHeight?: number;
  operationId: string;
  state: string;
  task: string;
}

export class OperationDetailResponse extends OperationSummaryResponse {
  stateDetail: any;
}

export class OperationSummaryListResponse {
  operations: OperationSummaryResponse[];
  totalCount: number;
}

export class OperationWorkflowStage {
  status: string;
  taskName: string;
  extraInformation: string;
}

export class OperationWorkflow {
  operationId: string;
  stages: OperationWorkflowStage[];
}

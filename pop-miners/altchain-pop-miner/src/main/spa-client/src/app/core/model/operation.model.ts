export class Operation {
  operationId: string;
  chain: string;
  state: string;
  endorsedBlockHeight: number;
  task: string;
  stateDetail: any;
}

export class OperationSummaryList {
  operations: Operation[];
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

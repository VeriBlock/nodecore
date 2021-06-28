export class MinerInfo {
  vbkAddress: string;
  vbkBalance: number;
  status: MinerStatus;
}

export class MinerStatus {
  isReady: boolean;
  reason?: string;
}

export class MineRequest {
  chainSymbol: string;
  height?: number;
}

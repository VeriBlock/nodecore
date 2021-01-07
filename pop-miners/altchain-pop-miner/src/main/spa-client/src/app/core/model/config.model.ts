export class AutoMineConfig {
  automineRounds: AutoMineRound[];
}

export class AutoMineRound {
  enabled: boolean;
  round: number;
}

export class VbkFeeConfig {
  feePerByte?: number;
  maxFee?: number;
}

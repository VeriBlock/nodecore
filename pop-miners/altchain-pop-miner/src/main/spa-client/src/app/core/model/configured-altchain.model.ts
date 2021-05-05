export class ConfiguredAltchainList {
  altchains: ConfiguredAltchain[];
}

export class ConfiguredAltchain {
  id: number;
  key: string;
  name: string;
  payoutDelay: number;
  readyStatus: AltChainReadyStatusResponse;

  hasLogo?: boolean;
  selectedFilter?: string;
}

export class AltChainReadyStatusResponse {
  isReady: boolean;
  reason?: string;
}

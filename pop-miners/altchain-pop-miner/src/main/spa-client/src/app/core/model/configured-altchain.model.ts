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
}

export class AltChainReadyStatusResponse {
  isReady: boolean;
  reason?: string;
}

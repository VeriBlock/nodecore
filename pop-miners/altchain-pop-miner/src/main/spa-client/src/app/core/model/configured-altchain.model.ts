export class ConfiguredAltchainList {
  altchains: ConfiguredAltchain[];
}

export class ConfiguredAltchain {
  id: number;
  key: string;
  name: string;
  payoutDelay: number;
  readyStatus: AltChainReadyStatusResponse;
  explorerBaseUrls: ExplorerBaseUrlsResponse;

  hasLogo?: boolean;
  selectedFilter?: string;
}

export class AltChainReadyStatusResponse {
  isReady: boolean;
  reason?: string;
}

export class ExplorerBaseUrlsResponse {
  atvById: string;
  blockByHash: string;
  blockByHeight: string;
  transactionById: string;
  address: string;
}

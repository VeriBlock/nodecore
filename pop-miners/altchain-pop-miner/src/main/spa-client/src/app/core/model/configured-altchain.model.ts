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
  syncStatus: AltChainSyncStatusResponse;

  hasLogo?: boolean;
  selectedFilter?: string;
}

export class AltChainSyncStatusResponse {
  localBlockchainHeight: number;
  networkHeight: number;
}

export class AltChainReadyStatusResponse {
  isReady: boolean;
  reason?: string;
}

export class ExplorerBaseUrlsResponse {
  atv: string;
  blockByHash: string;
  blockByHeight: string;
  transaction: string;
  address: string;
}

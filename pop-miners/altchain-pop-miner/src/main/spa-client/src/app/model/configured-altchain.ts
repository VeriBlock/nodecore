
export class ConfiguredAltchainList {
  altchains: ConfiguredAltchain[]
}

export class ConfiguredAltchain {
  id: number
  key: string
  name: string
  payoutDelay: number
}

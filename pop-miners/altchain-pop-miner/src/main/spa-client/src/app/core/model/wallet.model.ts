export class WithdrawRequest {
  amount: string;
  destinationAddress: string;
}

export class WithdrawResponse {
  ids: string[];
}

import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { ConfiguredAltchain, MinerStatus } from '@core/model';

@Injectable({
  providedIn: 'root',
})
export class DataShareService {
  public altChain = new BehaviorSubject(new ConfiguredAltchain());
  public globalMinerStatus = new BehaviorSubject(new MinerStatus());

  public currentAltChain = this.altChain.asObservable();
  public currentMinerStatus = this.globalMinerStatus.asObservable();

  constructor() {}

  public changeSelectedAltChain(data: ConfiguredAltchain) {
    this.altChain.next(data);
  }

  public changeMinerStatus(data: MinerStatus) {
    this.globalMinerStatus.next(data);
  }
}

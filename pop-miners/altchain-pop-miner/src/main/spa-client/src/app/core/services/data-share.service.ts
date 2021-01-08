import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs/internal/BehaviorSubject';

import { ConfiguredAltchain } from '@core/model';

@Injectable({
  providedIn: 'root',
})
export class DataShareService {
  public altChain = new BehaviorSubject(new ConfiguredAltchain());

  public currentAltChain = this.altChain.asObservable();

  constructor() {}

  public changeSelectedAltChain(data: ConfiguredAltchain) {
    this.altChain.next(data);
  }
}

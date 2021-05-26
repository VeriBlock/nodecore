import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';

import { AutoMineConfig, VbkFeeConfig } from '@core/model';

@Injectable({
  providedIn: 'root',
})
export class ConfigService {
  private baseUrl: string;

  constructor(private http: HttpClient) {
    this.baseUrl = `${environment.apiUrl}/config`;
  }

  getAutoMineConfig(id: string): Observable<AutoMineConfig> {
    return this.http.get<AutoMineConfig>(`${this.baseUrl}/automine/${id}`);
  }

  putAutoMineConfig(id: string, config: AutoMineConfig): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/automine/${id}`, config);
  }

  getVbkFee(): Observable<VbkFeeConfig> {
    return this.http.get<VbkFeeConfig>(`${this.baseUrl}/vbk-fee`);
  }

  putVbkFee(config: VbkFeeConfig): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/vbk-fee`, config);
  }
}

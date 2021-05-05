import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';

import { environment } from 'src/environments/environment';

import { NetworkInfoResponse } from '@core/model';

@Injectable({
  providedIn: 'root',
})
export class NetworkService {
  private baseUrl: string;

  constructor(private http: HttpClient) {
    this.baseUrl = `${environment.apiUrl}/network`;
  }

  getNetworkInfo(): Observable<NetworkInfoResponse> {
    return this.http.get<NetworkInfoResponse>(this.baseUrl);
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';

import { environment } from 'src/environments/environment';

import { WithdrawRequest, WithdrawResponse } from '@core/model';

@Injectable({
  providedIn: 'root',
})
export class WalletService {
  private baseUrl: string;

  constructor(private http: HttpClient) {
    this.baseUrl = `${environment.apiUrl}/wallet`;
  }

  postWithdraw(data: WithdrawRequest): Observable<WithdrawResponse> {
    return this.http.post<WithdrawResponse>(`${this.baseUrl}/withdraw`, data);
  }
}

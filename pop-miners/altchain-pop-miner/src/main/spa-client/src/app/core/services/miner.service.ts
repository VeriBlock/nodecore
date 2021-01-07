import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';

import { environment } from 'src/environments/environment';

import {
  OperationSummaryList,
  Operation,
  OperationWorkflow,
} from '@core/model/operation.model';
import { ConfiguredAltchainList } from '@core/model/configured-altchain.model';
import { MinerInfo, MineRequest } from '@core/model/miner.model';

/*
 * @author Pere
 * @since 09/13/2017
 */
@Injectable({
  providedIn: 'root',
})
export class MinerService {
  private baseUrl: string;

  constructor(private http: HttpClient) {
    this.baseUrl = `${environment.apiUrl}/miner`;
  }

  getMinerInfo(): Observable<MinerInfo> {
    return this.http.get<MinerInfo>(`${this.baseUrl}`);
  }

  getOperations(
    status: string,
    limit: number,
    offset: number
  ): Observable<OperationSummaryList> {
    return this.http.get<OperationSummaryList>(
      `${this.baseUrl}/operations?status=${status}&limit=${limit}&offset=${offset}`
    );
  }

  getOperation(id: string): Observable<Operation> {
    return this.http.get<Operation>(`${this.baseUrl}/operations/${id}`);
  }

  getOperationWorkflow(id: string): Observable<OperationWorkflow> {
    return this.http.get<OperationWorkflow>(
      `${this.baseUrl}/operations/${id}/workflow`
    );
  }

  getOperationLogs(id: string, level: string): Observable<string[]> {
    return this.http.get<string[]>(
      `${this.baseUrl}/operations/${id}/logs?level=${level}`
    );
  }

  postMine(request: MineRequest): Observable<Operation> {
    return this.http.post<Operation>(`${this.baseUrl}/mine`, request);
  }

  getConfiguredAltchains(): Observable<ConfiguredAltchainList> {
    return this.http.get<ConfiguredAltchainList>(
      `${this.baseUrl}/configured-altchains`
    );
  }
}

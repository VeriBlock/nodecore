import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';

import { environment } from 'src/environments/environment';

import { HttpUtilsService } from './http-utils.service';

import {
  ConfiguredAltchainList,
  MineRequest,
  MinerInfo,
  OperationDetailResponse,
  OperationGetListParams,
  OperationSummaryListResponse,
  OperationSummaryResponse,
  OperationWorkflow,
} from '@core/model';

/*
 * @author Pere
 * @since 09/13/2017
 */
@Injectable({
  providedIn: 'root',
})
export class MinerService {
  private baseUrl: string;

  constructor(
    private http: HttpClient,
    private httpUtilsService: HttpUtilsService
  ) {
    this.baseUrl = `${environment.apiUrl}/miner`;
  }

  getMinerInfo(): Observable<MinerInfo> {
    return this.http.get<MinerInfo>(`${this.baseUrl}`);
  }

  getOperationList(
    dto: OperationGetListParams
  ): Observable<OperationSummaryListResponse> {
    return this.http.get<OperationSummaryListResponse>(
      `${this.baseUrl}/operations`,
      {
        params: this.httpUtilsService.toHttpParams(dto),
      }
    );
  }

  getOperation(id: string): Observable<OperationDetailResponse> {
    return this.http.get<OperationDetailResponse>(
      `${this.baseUrl}/operations/${id}`
    );
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

  postMine(request: MineRequest): Observable<OperationSummaryResponse> {
    return this.http.post<OperationSummaryResponse>(
      `${this.baseUrl}/mine`,
      request
    );
  }

  getConfiguredAltchains(): Observable<ConfiguredAltchainList> {
    return this.http.get<ConfiguredAltchainList>(
      `${this.baseUrl}/configured-altchains`
    );
  }
}

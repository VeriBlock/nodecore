
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {OperationSummaryList, OperationWorkflow} from "../model/operation.model";
import {Operation} from "../model/operation.model";
import {MineRequest, MinerInfo} from "../model/miner.model";
import {environment} from "../../../environments/environment";
import {ConfiguredAltchainList} from "../model/configured-altchain.model";

/*
 * @author Pere
 * @since 09/13/2017
 */
@Injectable()
export class ApiService {

	constructor(
		private http: HttpClient
	) {
	}

  getMinerInfo(): Observable<MinerInfo> {
    return this.http.get<MinerInfo>(`${environment.apiUrl}/miner`);
  }

  getOperations(status: string, limit: number, offset: number): Observable<OperationSummaryList> {
    return this.http.get<OperationSummaryList>(`${environment.apiUrl}/miner/operations?status=${status}&limit=${limit}&offset=${offset}`);
  }

  getOperation(id: string): Observable<Operation> {
    return this.http.get<Operation>(`${environment.apiUrl}/miner/operations/${id}`);
  }

  getOperationWorkflow(id: string): Observable<OperationWorkflow> {
    return this.http.get<OperationWorkflow>(`${environment.apiUrl}/miner/operations/${id}/workflow`);
  }

  getOperationLogs(id: string, level: string): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiUrl}/miner/operations/${id}/logs?level=${level}`);
  }

  mine(request: MineRequest): Observable<Operation> {
    return this.http.post<Operation>(`${environment.apiUrl}/miner/mine`, request);
  }

  getConfiguredAltchains(): Observable<ConfiguredAltchainList> {
    return this.http.get<ConfiguredAltchainList>(`${environment.apiUrl}/miner/configured-altchains`);
  }
}

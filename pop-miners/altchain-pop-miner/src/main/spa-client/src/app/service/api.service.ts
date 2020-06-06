
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {OperationSummaryList, OperationWorkflow} from "../model/operation";
import {Operation} from "../model/operation";
import {MineRequest} from "../model/miner";

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

  getOperations(): Observable<OperationSummaryList> {
    return this.http.get<OperationSummaryList>(`http://localhost:8080/api/miner/operations`);
  }

  getOperation(id: string): Observable<Operation> {
    return this.http.get<Operation>(`http://localhost:8080/api/miner/operations/${id}`);
  }

  getOperationWorkflow(id: string): Observable<OperationWorkflow> {
    return this.http.get<OperationWorkflow>(`http://localhost:8080/api/miner/operations/${id}/workflow`);
  }

  mine(request: MineRequest): Observable<Operation> {
    return this.http.post<Operation>(`http://localhost:8080/api/miner/mine`, request);
  }
}

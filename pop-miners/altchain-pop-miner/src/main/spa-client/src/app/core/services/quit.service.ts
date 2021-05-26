import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';

import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root',
})
export class QuitService {
  private baseUrl: string;

  constructor(private http: HttpClient) {
    this.baseUrl = `${environment.apiUrl}/quit`;
  }

  postQuitRestart(restart: boolean): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}?restart=${restart}`, {});
  }
}

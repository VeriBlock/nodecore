import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class HttpUtilsService {
  constructor() {}

  public toHttpParams(obj: Object): HttpParams {
    let params = new HttpParams();

    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        const val = obj[key];
        if (val !== null && val !== undefined) {
          params = params.append(key, val.toString());
        }
      }
    }
    return params;
  }
}

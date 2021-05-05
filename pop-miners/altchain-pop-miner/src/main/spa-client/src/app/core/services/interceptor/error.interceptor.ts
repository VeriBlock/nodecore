import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AlertService } from '../alert.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(public alertService: AlertService) {}

  intercept(
    request: HttpRequest<any>,
    handler: HttpHandler
  ): Observable<HttpEvent<any>> {
    return handler.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = 'Unknown error!';
        if (error.error instanceof ErrorEvent) {
          // Client-side errors
          errorMessage = `Error: ${error.error.message}`;
        } else {
          // server status 0 or 5xx
          if (error?.status === 0 || error?.status >= 500) {
            errorMessage = `Something went wrong. Can't reach the server.`;
          } else if (error.error != null) {
            // Server-side errors
            errorMessage = `Error Code: ${error.status}\nMessage: ${error.error.message}`;
          } else {
            errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
          }
        }

        this.alertService.addWarning(errorMessage);

        return throwError(errorMessage);
      })
    );
  }
}

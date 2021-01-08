import { NgModule, Optional, SkipSelf } from '@angular/core';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

import { HttpUtilsService } from './services/http-utils.service';

import { ErrorInterceptor } from './services/interceptor/error.interceptor';

import { throwIfAlreadyLoaded } from './module-import-guard';

@NgModule({
  providers: [
    HttpUtilsService,
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
  ],
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    throwIfAlreadyLoaded(parentModule, 'CoreModule');
  }
}

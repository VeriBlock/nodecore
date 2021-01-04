import { NgModule } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule } from '@angular/forms';

import { ErrorInterceptor } from './core/service/interceptor/error.interceptor';
import { ApiService } from './core/service/api.service';

import { AppRoutingModule } from './app-routing.module';
import { MaterialModule } from '@material/material.module';

import { MineDialogComponent } from '@components/operations/mine-dialog/mine-dialog.component';
import { LogsDialogComponent } from '@components/operations/logs-dialog/logs-dialog.component';
import { OperationsComponent } from '@components/operations/operations.component';
import { AppComponent } from '@components/app/app.component';

@NgModule({
  declarations: [
    AppComponent,
    OperationsComponent,
    MineDialogComponent,
    LogsDialogComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    MaterialModule,
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    ApiService,
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}

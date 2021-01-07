import { NgModule } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { BrowserModule } from '@angular/platform-browser';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ErrorInterceptor } from '@core/services/interceptor/error.interceptor';
import { ConfigService } from '@core/services/config.service';
import { MinerService } from '@core/services/miner.service';

import { AppRoutingModule } from './app-routing.module';
import { SharedModule } from '@shared/shared.module';

import { OperationsTableComponent } from '@components/operations/operations-table/operations-table.component';
import { MineDialogComponent } from '@components/operations/mine-dialog/mine-dialog.component';
import { LogsDialogComponent } from '@components/operations/logs-dialog/logs-dialog.component';
import { OperationsComponent } from '@components/operations/operations.component';
import { AppComponent } from '@components/app/app.component';
import { CoinConfigurationDialogComponent } from './components/operations/coin-configuration-dialog/coin-configuration-dialog.component';
import { AppFeeSettingDialogComponent } from './components/app/app-fee-setting-dialog/app-fee-setting-dialog.component';

@NgModule({
  declarations: [
    AppFeeSettingDialogComponent,
    AppComponent,
    CoinConfigurationDialogComponent,
    OperationsTableComponent,
    OperationsComponent,
    MineDialogComponent,
    LogsDialogComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    BrowserAnimationsModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    SharedModule,
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    MinerService,
    ConfigService,
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}

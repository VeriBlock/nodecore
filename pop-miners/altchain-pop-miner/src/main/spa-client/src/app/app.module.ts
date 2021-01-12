import { NgModule } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MultiTranslateHttpLoader } from 'ngx-translate-multi-http-loader';
import { TranslateLoader } from '@ngx-translate/core';
import { TranslateModule } from '@ngx-translate/core';

import { AppRoutingModule } from './app-routing.module';
import { SharedModule } from '@shared/shared.module';
import { CoreModule } from '@core/core.module';

import { CoinConfigurationDialogComponent } from './components/operations/coin-configuration-dialog/coin-configuration-dialog.component';
import { AppTransactionDialogComponent } from './components/app/app-transaction-dialog/app-transaction-dialog.component';
import { AppFeeSettingDialogComponent } from './components/app/app-fee-setting-dialog/app-fee-setting-dialog.component';
import { OperationsTableComponent } from '@components/operations/operations-table/operations-table.component';
import { LogsDialogComponent } from '@components/operations/logs-dialog/logs-dialog.component';
import { OperationsComponent } from '@components/operations/operations.component';
import { AppComponent } from '@components/app/app.component';

// AoT requires an exported function for factories
export function HttpLoaderFactory(http: HttpClient) {
  return new MultiTranslateHttpLoader(http, [
    { prefix: './assets/i18n/ApmOperationExplainer/', suffix: '.json' },
  ]);
}

@NgModule({
  declarations: [
    AppFeeSettingDialogComponent,
    AppComponent,
    CoinConfigurationDialogComponent,
    OperationsTableComponent,
    OperationsComponent,
    LogsDialogComponent,
    AppTransactionDialogComponent,
  ],
  imports: [
    BrowserAnimationsModule,
    ReactiveFormsModule,
    AppRoutingModule,
    HttpClientModule,
    FlexLayoutModule,
    BrowserModule,
    FormsModule,
    CoreModule,
    SharedModule,
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [HttpClient],
      },
      useDefaultLang: true,
      defaultLanguage: 'en',
    }),
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}

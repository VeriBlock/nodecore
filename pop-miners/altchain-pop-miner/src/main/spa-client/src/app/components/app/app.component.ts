import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { startWith, switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin } from 'rxjs/internal/observable/forkJoin';
import { interval } from 'rxjs/internal/observable/interval';

import { DataShareService } from '@core/services/data-share.service';
import { NetworkService } from '@core/services/network.service';
import { ConfigService } from '@core/services/config.service';
import { AlertService } from '@core/services/alert.service';
import { MinerService } from '@core/services/miner.service';

import { AppTransactionDialogComponent } from './app-transaction-dialog/app-transaction-dialog.component';
import { AppFeeSettingDialogComponent } from './app-fee-setting-dialog/app-fee-setting-dialog.component';

import {
  ConfiguredAltchain,
  ConfiguredAltchainList,
  NetworkInfoResponse,
  VbkFeeConfig,
} from '@core/model';

@Component({
  selector: 'vbk-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  public networkInfo: string = null;
  public configuredAltchains: ConfiguredAltchain[] = [];
  public vbkAddress: string;
  public vbkBalance: string;
  public isLoadingSettings = false;

  public selectedAltChain = null;

  constructor(
    private dataShareService: DataShareService,
    private networkService: NetworkService,
    private configService: ConfigService,
    private alertService: AlertService,
    private minerService: MinerService,
    private route: ActivatedRoute,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit() {
    forkJoin([
      this.minerService.getConfiguredAltchains(),
      this.networkService.getNetworkInfo(),
    ]).subscribe((results) => {
      this.configuredAltchains =
        (results[0] as ConfiguredAltchainList)?.altchains || [];
      this.networkInfo = (results[1] as NetworkInfoResponse)?.name || null;
    });

    // Check the miner data API every 61 seconds
    interval(61000)
      .pipe(
        startWith(0),
        switchMap(() => this.minerService.getMinerInfo())
      )
      .subscribe((response) => {
        this.vbkAddress = response.vbkAddress;
        this.vbkBalance = (response.vbkBalance / 100_000_000).toString();
      });

    // Clear params if for some reason there are available when page loads
    if (!this.selectedAltChain) {
      const queryParams: Params = {};
      queryParams['selectedOperationId'] = null;
      queryParams['statusFilter'] = null;
      queryParams['pageLimit'] = null;
      queryParams['pageOffset'] = null;

      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: queryParams,
        queryParamsHandling: 'merge',
      });
    }
  }

  public changeAltChain(data: ConfiguredAltchain) {
    this.selectedAltChain = data?.key || null;
    this.dataShareService.changeSelectedAltChain(data);
  }

  public openFeeConfigurationDialog() {
    this.isLoadingSettings = true;

    this.configService.getVbkFee().subscribe((data) => {
      const dialogRef = this.dialog.open(AppFeeSettingDialogComponent, {
        minWidth: '350px',
        maxWidth: '500px',
        panelClass: 'dialog',
        data: data,
        closeOnNavigation: true,
      });
      this.isLoadingSettings = false;

      dialogRef
        .afterClosed()
        .subscribe((result: { save: boolean; feeConfig: VbkFeeConfig }) => {
          if (result?.save) {
            this.configService.putVbkFee(result?.feeConfig).subscribe(() => {
              this.alertService.addSuccess(
                'Configuration updated successfully!'
              );
            });
          }
        });
    });
  }

  public getAltChainLogo(key: string) {
    return `https://cryptoicons.org/api/icon/${
      key.includes('btc') ? 'btc' : key
    }/100`;
  }

  public showImg(chain: ConfiguredAltchain) {
    chain.hasLogo = true;
  }

  public openTransactionDialog(isDeposit: boolean) {
    this.dialog.open(AppTransactionDialogComponent, {
      minWidth: '500px',
      maxWidth: '800px',
      panelClass: 'dialog',
      data: {
        isDeposit,
        vbkAddress: this.vbkAddress,
        vbkBalance: this.vbkBalance,
        isTestnet: this.networkInfo === 'testnet',
      },
      closeOnNavigation: true,
    });
  }
}
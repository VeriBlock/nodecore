import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, Params } from '@angular/router';
import {
  delay,
  retryWhen,
  startWith,
  switchMap,
  takeWhile,
} from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin, interval } from 'rxjs';

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
  MinerStatus,
  NetworkInfoResponse,
  VbkFeeConfig,
} from '@core/model';

@Component({
  selector: 'vbk-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  public networkError = false;
  public networkInfo: NetworkInfoResponse = null;
  public configuredAltchains: ConfiguredAltchain[] = [];
  public vbkAddress: string;
  public vbkBalance: string;
  public isLoadingSettings = false;
  public minerStatus: MinerStatus = null;

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
    this.networkService
      .getNetworkInfo()
      .subscribe((data: NetworkInfoResponse) => {
        this.networkInfo = data || null;
      });

    // Check the miner data API every 9 seconds
    interval(9_000)
      .pipe(
        startWith(0),
        switchMap(() =>
          forkJoin([
            this.minerService.getConfiguredAltchains(),
            this.minerService.getMinerInfo(),
          ])
        ),
        retryWhen((errors) => {
          return errors.pipe(
            delay(15000),
            takeWhile((error) => {
              this.disableChains();
              return error.includes("APM Instance can't be reached");
            })
          );
        })
      )
      .subscribe((results) => {
        this.networkError = false;

        const returnedResults = [
          ...(results[0] as ConfiguredAltchainList)?.altchains?.map((alt) => {
            const newAltchain =
              this.configuredAltchains.find(
                (confAlt) => confAlt.id === alt.id
              ) || {};

            return { ...newAltchain, ...alt };
          }),
        ];

        if (
          JSON.stringify(this.configuredAltchains) !==
          JSON.stringify(returnedResults)
        ) {
          this.configuredAltchains = [...returnedResults];
          if (Boolean(this.selectedAltChain)) {
            this.changeAltChain(
              this.configuredAltchains.find(
                (alt) => alt.key === this.selectedAltChain
              )
            );
          }
        }

        this.vbkAddress = results[1].vbkAddress;
        this.vbkBalance = (results[1].vbkBalance / 100_000_000).toString();

        if (
          JSON.stringify(this.minerStatus) !==
          JSON.stringify(results[1]?.status)
        ) {
          this.minerStatus = results[1].status;
          this.dataShareService.changeMinerStatus(this.minerStatus);
        }

        if (this.configuredAltchains?.length > 0 && !this.selectedAltChain) {
          this.changeAltChain(this.configuredAltchains[0]);
        }
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

  private disableChains() {
    this.selectedAltChain = null;
    this.vbkBalance = null;

    this.configuredAltchains = [
      ...this.configuredAltchains.map((chain) => {
        chain.readyStatus.isReady = false;
        return chain;
      }),
    ];
    if (Boolean(this.selectedAltChain)) {
      this.changeAltChain(null);
    }
  }

  public changeAltChain(data: ConfiguredAltchain) {
    if (
      (this.networkError ||
        this.vbkBalance === null ||
        this.vbkBalance === undefined) &&
      !this.selectedAltChain
    ) {
      return;
    }

    if (this.selectedAltChain) {
      const index = this.configuredAltchains.findIndex(
        (alt) => alt.key === this.selectedAltChain
      );
      if (index > -1) {
        this.configuredAltchains[index].selectedFilter =
          this.route.snapshot.queryParamMap.get('statusFilter') || undefined;
      }
    }
    this.selectedAltChain = data?.key || null;
    this.dataShareService.changeSelectedAltChain(data);
  }

  public openFeeConfigurationDialog() {
    this.isLoadingSettings = true;

    this.configService
      .getVbkFee()
      .subscribe((data) => {
        const dialogRef = this.dialog.open(AppFeeSettingDialogComponent, {
          minWidth: '350px',
          maxWidth: '500px',
          panelClass: 'dialog',
          data: data,
          closeOnNavigation: true,
        });

        dialogRef
          .afterClosed()
          .subscribe(
            (result: {
              save: boolean;
              feeConfig: VbkFeeConfig;
              restart?: boolean;
            }) => {
              if (result?.save) {
                this.configService
                  .putVbkFee(result?.feeConfig)
                  .subscribe(() => {
                    this.alertService.addSuccess(
                      'Configuration updated successfully!'
                    );
                  });
              }

              if (result?.restart) {
                this.networkError = true;
                this.disableChains();
              }
            }
          );
      })
      .add(() => {
        this.isLoadingSettings = false;
      });
  }

  public getAltChainLogo(key: string) {
    return `https://cryptoicons.org/api/icon/${
      key.includes('btc') ? 'btc' : key
    }/100`;
  }

  public showImg(index: number) {
    this.configuredAltchains[index].hasLogo = true;
  }

  public openTransactionDialog(isDeposit: boolean) {
    this.dialog.open(AppTransactionDialogComponent, {
      width: '550px',
      panelClass: 'dialog',
      data: {
        isDeposit,
        vbkAddress: this.vbkAddress,
        vbkBalance: this.vbkBalance,
        isTestnet: this.networkInfo?.name !== 'mainnet',
      },
      closeOnNavigation: true,
    });
  }

  public getExplorerUrl(): string {
    return (
      this.networkInfo?.explorerBaseUrls?.transaction?.split('/tx')[0] ||
      undefined
    );
  }
}

import { Component, OnInit } from '@angular/core';
import { startWith, switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { interval } from 'rxjs';

import { DataShareService } from '@core/services/data-share.service';
import { ConfigService } from '@core/services/config.service';
import { AlertService } from '@core/services/alert.service';
import { MinerService } from '@core/services/miner.service';

import { AppFeeSettingDialogComponent } from './app-fee-setting-dialog/app-fee-setting-dialog.component';

import { ConfiguredAltchain, VbkFeeConfig } from '@core/model';

@Component({
  selector: 'vbk-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  public configuredAltchains: ConfiguredAltchain[] = [];
  public vbkAddress: string;
  public vbkBalance: string;
  public isLoadingSettings = false;

  public isAltChainSelected = false;

  constructor(
    private dataShareService: DataShareService,
    private configService: ConfigService,
    private alertService: AlertService,
    private minerService: MinerService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    // Get the configured altchains
    this.minerService
      .getConfiguredAltchains()
      .subscribe((configuredAltchains) => {
        this.configuredAltchains = configuredAltchains.altchains;
      });

    // Check the miner data API every 61 seconds
    interval(61_000)
      .pipe(
        startWith(0),
        switchMap(() => this.minerService.getMinerInfo())
      )
      .subscribe((response) => {
        this.vbkAddress = response.vbkAddress;
        this.vbkBalance = (response.vbkBalance / 100_000_000).toString();
      });
  }

  public changeAltChain(data: ConfiguredAltchain) {
    this.isAltChainSelected = Boolean(data?.key);
    this.dataShareService.changeSelectedAltChain(data);
  }

  public openFeeConfigurationDialog() {
    this.isLoadingSettings = true;

    this.configService.getVbkFee().subscribe((data) => {
      const dialogRef = this.dialog.open(AppFeeSettingDialogComponent, {
        minWidth: '30vw',
        maxWidth: '500px',
        panelClass: 'dialog',
        data: data,
        closeOnNavigation: true,
      });
      this.isLoadingSettings = false;

      dialogRef
        .afterClosed()
        .subscribe((result: { save: boolean; feeConfig: VbkFeeConfig }) => {
          console.log(result?.feeConfig);

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
    return `https://cryptoicons.org/api/icon/${key}/100`;
  }

  public showImg(chain: ConfiguredAltchain) {
    chain.hasLogo = true;
  }
}

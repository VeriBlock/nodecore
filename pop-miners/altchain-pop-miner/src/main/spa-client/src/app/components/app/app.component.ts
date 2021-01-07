import { Component, OnInit } from '@angular/core';
import { startWith, switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { interval } from 'rxjs';

import { ConfigService } from '@core/services/config.service';
import { MinerService } from '@core/services/miner.service';

import { AppFeeSettingDialogComponent } from './app-fee-setting-dialog/app-fee-setting-dialog.component';

import { ConfiguredAltchain } from '@core/model/configured-altchain.model';
import { VbkFeeConfig } from '@core/model/config.model';

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

  constructor(
    private minerService: MinerService,
    private configService: ConfigService,
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
        .subscribe((result: { save: boolean; data: VbkFeeConfig }) => {
          if (result?.save) {
            this.configService.putVbkFee(data).subscribe(() => {
              console.log('save successful');
            });
          }
        });
    });
  }
}

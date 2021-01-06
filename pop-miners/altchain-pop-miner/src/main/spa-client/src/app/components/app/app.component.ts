import { Component, OnInit } from '@angular/core';
import { startWith, switchMap } from 'rxjs/operators';
import { interval } from 'rxjs';

import { ApiService } from '@core/service/api.service';

import { ConfiguredAltchain } from '@core/model/configured-altchain.model';

@Component({
  selector: 'vbk-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {

  public configuredAltchains: ConfiguredAltchain[] = [];
  public vbkAddress: string;
  public vbkBalance: string;

  constructor(private apiService: ApiService) {}

  ngOnInit() {
    // Get the configured altchains
    this.apiService
      .getConfiguredAltchains()
      .subscribe((configuredAltchains) => {
        this.configuredAltchains = configuredAltchains.altchains;
      });

    // Check the miner data API every 61 seconds
    interval(61_000)
      .pipe(
        startWith(0),
        switchMap(() => this.apiService.getMinerInfo())
      )
      .subscribe((response) => {
        this.vbkAddress = response.vbkAddress;
        this.vbkBalance = (response.vbkBalance / 100_000_000).toString();
      });
  }

  openMineDialog() {
    // const dialogConfig = new MatDialogConfig();
    // dialogConfig.data = this.configuredAltchains;
    // this.dialog.open(MineDialogComponent, dialogConfig);
  }
}

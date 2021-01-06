import { Component, OnInit } from '@angular/core';

import { ApiService } from '@core/service/api.service';

import { ConfiguredAltchain } from '@core/model/configured-altchain.model';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  public configuredAltchains: ConfiguredAltchain[] = [];

  constructor(private apiService: ApiService) {}

  ngOnInit() {
    // Get the configured altchains
    this.apiService
      .getConfiguredAltchains()
      .subscribe((configuredAltchains) => {
        this.configuredAltchains = configuredAltchains.altchains;
      });
  }

  openMineDialog() {
    // const dialogConfig = new MatDialogConfig();
    // dialogConfig.data = this.configuredAltchains;
    // this.dialog.open(MineDialogComponent, dialogConfig);
  }
}

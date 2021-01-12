import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { MinerService } from '@core/services/miner.service';
import { AlertService } from '@core/services/alert.service';

import { ConfiguredAltchain, MineRequest } from '@core/model';

@Component({
  selector: 'vbk-mine-dialog',
  templateUrl: './mine-dialog.component.html',
  styleUrls: [],
})
export class MineDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<MineDialogComponent>,
    private minerService: MinerService,
    private alertService: AlertService,

    @Inject(MAT_DIALOG_DATA)
    public data: ConfiguredAltchain
  ) {}

  public onMine(): void {
    const request: MineRequest = {
      chainSymbol: this.data.key,
    };

    this.minerService.postMine(request).subscribe((response) => {
      this.alertService.addSuccess(
        'Mine request successful! Operation id: ' + response.operationId
      );
      this.dialogRef.close();
    });
  }

  public onCancel(): void {
    this.dialogRef.close();
  }
}

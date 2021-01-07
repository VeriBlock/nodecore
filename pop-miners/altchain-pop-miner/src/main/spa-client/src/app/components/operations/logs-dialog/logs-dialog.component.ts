import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { MinerService } from '@core/services/miner.service';

import { Operation } from '@core/model/operation.model';

@Component({
  selector: 'vbk-logs-dialog',
  templateUrl: './logs-dialog.component.html',
})
export class LogsDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<Operation>,
    private minerService: MinerService,
    @Inject(MAT_DIALOG_DATA) public logs: string[]
  ) {}
}

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { OperationSummaryResponse } from '@core/model';

@Component({
  selector: 'vbk-logs-dialog',
  templateUrl: './logs-dialog.component.html',
})
export class LogsDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<OperationSummaryResponse>,
    @Inject(MAT_DIALOG_DATA)
    public data: { logs: string[]; operationId: number; level: string }
  ) {}

  public capitalizeFirstLetter = (s: string): string => {
    if (typeof s !== 'string') return '';
    return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
  };
}

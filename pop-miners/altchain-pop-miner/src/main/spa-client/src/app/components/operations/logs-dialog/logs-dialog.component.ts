import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { Operation } from '@core/model/operation.model';
import { ApiService } from '@core/service/api.service';

@Component({
  selector: 'logs-dialog',
  templateUrl: './logs-dialog.component.html',
})
export class LogsDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<Operation>,
    private apiService: ApiService,
    @Inject(MAT_DIALOG_DATA) public logs: string[]
  ) {}
}

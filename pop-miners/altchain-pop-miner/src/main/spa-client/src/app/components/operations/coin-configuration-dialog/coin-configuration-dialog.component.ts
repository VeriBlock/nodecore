import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { AutoMineConfig, AutoMineRound } from '@core/model/config.model';

@Component({
  selector: 'vbk-coin-configuration-dialog',
  templateUrl: './coin-configuration-dialog.component.html',
})
export class CoinConfigurationDialogComponent implements OnInit {
  public automineRounds: AutoMineRound[] = [];

  constructor(
    public dialogRef: MatDialogRef<CoinConfigurationDialogComponent>,

    @Inject(MAT_DIALOG_DATA)
    public data: AutoMineConfig
  ) {}

  ngOnInit(): void {
    this.automineRounds = [
      ...this.data.automineRounds.sort((a, b) => a.round - b.round),
    ];
  }

  public onCancel(): void {
    this.dialogRef.close({ save: false });
  }

  public onSave(): void {
    this.dialogRef.close({ save: true, data: this.automineRounds });
  }
}

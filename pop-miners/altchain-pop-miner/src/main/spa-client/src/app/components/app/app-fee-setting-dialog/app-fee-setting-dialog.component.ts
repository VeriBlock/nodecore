import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { VbkFeeConfig } from '@core/model';

@Component({
  selector: 'vbk-app-fee-setting-dialog',
  templateUrl: './app-fee-setting-dialog.component.html',
})
export class AppFeeSettingDialogComponent implements OnInit {
  public form: FormGroup = this.formBuilder.group({
    feePerByte: [null, Validators.min(0)],
    maxFee: [null, Validators.min(0)],
  });

  constructor(
    public dialogRef: MatDialogRef<AppFeeSettingDialogComponent>,
    private formBuilder: FormBuilder,

    @Inject(MAT_DIALOG_DATA)
    public data: VbkFeeConfig
  ) {}

  ngOnInit(): void {
    this.form.patchValue({
      feePerByte: this.data.feePerByte,
      maxFee: this.data.maxFee,
    });
  }

  public onCancel(): void {
    this.dialogRef.close({ save: false });
  }

  public onSave(): void {
    this.dialogRef.close({ save: true, feeConfig: this.form.value });
  }
}

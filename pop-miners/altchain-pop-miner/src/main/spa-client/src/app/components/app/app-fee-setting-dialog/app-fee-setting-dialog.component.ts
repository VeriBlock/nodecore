import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AlertService } from '@core/services/alert.service';
import { QuitService } from '@core/services/quit.service';

import { VbkFeeConfig } from '@core/model';

@Component({
  selector: 'vbk-app-fee-setting-dialog',
  templateUrl: './app-fee-setting-dialog.component.html',
})
export class AppFeeSettingDialogComponent implements OnInit {
  public form: FormGroup = this.formBuilder.group({
    feePerByte: [
      null,
      Validators.compose([Validators.min(0), Validators.required]),
    ],
    maxFee: [
      null,
      Validators.compose([Validators.min(0), Validators.required]),
    ],
  });

  public isRestarting = false;

  constructor(
    public dialogRef: MatDialogRef<AppFeeSettingDialogComponent>,
    private alertService: AlertService,
    private formBuilder: FormBuilder,
    private quitService: QuitService,

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
    if (this.form.invalid) {
      this.alertService.addWarning(
        'Please check the form and fix the errors indicated'
      );
      return;
    }

    this.dialogRef.close({ save: true, feeConfig: this.form.value });
  }

  public onRestart(): void {
    this.isRestarting = true;

    this.quitService
      .postQuitRestart(true)
      .subscribe()
      .add(() => {
        setTimeout(() => {
          this.isRestarting = false;
        }, 15000);
      });
  }
}

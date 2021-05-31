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
  private maxFeeNumber = 99999999;

  public form: FormGroup = this.formBuilder.group({
    feePerByte: [
      null,
      Validators.compose([
        Validators.min(1),
        Validators.max(this.maxFeeNumber),
        Validators.required,
      ]),
    ],
    maxFee: [
      null,
      Validators.compose([
        Validators.min(1),
        Validators.max(this.maxFeeNumber),
        Validators.required,
      ]),
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
      .subscribe(() => {
        this.dialogRef.close({ save: false, restart: true });
      })
      .add(() => {
        this.isRestarting = false;
      });
  }

  public checkNumberFormat(e, name: string) {
    if (e.target?.value) {
      if (e.target.value.match(/[^0-9]/g)) {
        this.form.controls[name].patchValue(
          e.target.value.replace(/[^0-9]/g, '')
        );
      }
    }

    const selectedInputValue: number = parseFloat(e.target?.value);

    switch (name) {
      case 'feePerByte':
        const maxFeeLimit = parseFloat(this.form?.value?.maxFee);

        if (selectedInputValue > this.maxFeeNumber) {
          this.form.controls['feePerByte'].patchValue(this.maxFeeNumber);
          this.form.controls['maxFee'].patchValue(this.maxFeeNumber);
        } else if (selectedInputValue > maxFeeLimit) {
          this.form.controls['maxFee'].patchValue(selectedInputValue);
        }
        break;

      case 'maxFee':
        if (selectedInputValue > this.maxFeeNumber) {
          this.form.controls['maxFee'].patchValue(this.maxFeeNumber);
        }
        if (selectedInputValue < parseFloat(this.form?.value?.feePerByte)) {
          this.form.controls['feePerByte'].patchValue(selectedInputValue);
        }
        break;

      default:
        break;
    }
  }

  public disableNumberFormat(e: KeyboardEvent) {
    if (
      e.key === '.' ||
      e.key === ',' ||
      e.key === '-' ||
      e.key === '+' ||
      e.key === 'e'
    ) {
      e.preventDefault();
    }

    if (e.key === 'Enter' && this.form.valid) {
      this.onSave();
    }
  }
}

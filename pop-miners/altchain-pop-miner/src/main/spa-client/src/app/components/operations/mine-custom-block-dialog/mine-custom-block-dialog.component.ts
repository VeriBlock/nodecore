import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AlertService } from '@core/services/alert.service';

@Component({
  selector: 'vbk-mine-custom-block-dialog',
  templateUrl: './mine-custom-block-dialog.component.html',
})
export class MineCustomBlockDialogComponent implements OnInit {
  public maxNumber = 2147483647;

  public blockNumber: number;
  public form = this.formBuilder.group({
    blockNumber: null,
    feePerByte: [
      null,
      Validators.compose([Validators.min(0), Validators.required]),
    ],
    maxFee: [
      null,
      Validators.compose([Validators.min(0), Validators.required]),
    ],
  });

  constructor(
    public dialogRef: MatDialogRef<MineCustomBlockDialogComponent>,
    private alertService: AlertService,
    private formBuilder: FormBuilder,

    @Inject(MAT_DIALOG_DATA)
    public data: { maxHeight: number; feePerByte: number; maxFee: number }
  ) {}

  ngOnInit(): void {
    this.maxNumber = this.data?.maxHeight || this.maxNumber;

    this.form
      .get('blockNumber')
      .setValidators([
        Validators.min(0),
        Validators.max(this.maxNumber),
        Validators.required,
      ]);

    this.form.patchValue({
      blockNumber: this.data?.maxHeight || null,
      feePerByte: this.data?.feePerByte || null,
      maxFee: this.data?.maxFee || null,
    });

    this.form.controls['feePerByte'].disable();
    this.form.controls['maxFee'].disable();
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

    this.dialogRef.close({
      save: true,
      data: {
        maxHeight: this.form.value?.blockNumber,
        feePerByte: this.form.value?.feePerByte,
        maxFee: this.form.value?.maxFee,
      },
    });
  }

  public checkNumberFormat() {
    if (this.form.value?.blockNumber) {
      this.form.controls['blockNumber'].patchValue(
        String(this.form.value.blockNumber).replace(/[^0-9]/g, '')
      );
    }

    if (parseFloat(this.form.value?.blockNumber) > this.maxNumber) {
      this.form.controls['blockNumber'].patchValue(this.maxNumber - 1);
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

    if (e.key === 'Tab') {
      this.enableFeeInput();
    }
  }

  public enableFeeInput() {
    this.form.controls['feePerByte'].enable();
    this.form.controls['maxFee'].enable();
  }
}

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
  private maxFeeNumber = 99999999;

  public blockNumber: number;
  public form = this.formBuilder.group({
    blockNumber: null,
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
        Validators.min(1),
        Validators.max(this.maxNumber),
        Validators.required,
      ]);

    this.form.patchValue({
      blockNumber: this.data?.maxHeight || null,
      feePerByte: this.data?.feePerByte || null,
      maxFee: this.data?.maxFee || null,
    });

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
      case 'blockNumber':
        if (selectedInputValue > this.maxNumber) {
          this.form.controls['blockNumber'].patchValue(this.maxNumber - 1);
        }
        break;

      case 'feePerByte':
        const maxFeeLimit =
          parseFloat(this.form?.value?.maxFee) || this.data?.maxFee;

        if (this.form.controls['maxFee'].enabled) {
          if (selectedInputValue > this.maxFeeNumber) {
            this.form.controls['feePerByte'].patchValue(this.maxFeeNumber);
            this.form.controls['maxFee'].patchValue(this.maxFeeNumber);
          } else if (selectedInputValue > maxFeeLimit) {
            this.form.controls['maxFee'].patchValue(selectedInputValue);
          }
        } else if (selectedInputValue > maxFeeLimit) {
          this.form.controls['feePerByte'].patchValue(maxFeeLimit);
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

  public enableFeeInput() {
    this.form.controls['maxFee'].enable();
  }
}

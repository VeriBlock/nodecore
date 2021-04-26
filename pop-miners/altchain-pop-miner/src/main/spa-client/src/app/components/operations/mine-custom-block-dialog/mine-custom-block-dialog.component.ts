import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { AlertService } from '@core/services/alert.service';

@Component({
  selector: 'vbk-mine-custom-block-dialog',
  templateUrl: './mine-custom-block-dialog.component.html',
})
export class MineCustomBlockDialogComponent implements OnInit {
  public blockNumber: number;
  public form = this.formBuilder.group({
    blockNumber: [
      null,
      Validators.compose([Validators.min(0), Validators.required]),
    ],
  });

  constructor(
    public dialogRef: MatDialogRef<MineCustomBlockDialogComponent>,
    private alertService: AlertService,
    private formBuilder: FormBuilder
  ) {}

  ngOnInit(): void {}

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
      data: this.form.controls['blockNumber']?.value,
    });
  }
}

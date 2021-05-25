import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { WithdrawRequest, WithdrawResponse } from '@core/model';
import { AlertService } from '@core/services/alert.service';
import { WalletService } from '@core/services/wallet.service';

@Component({
  selector: 'vbk-app-transaction-dialog',
  templateUrl: './app-transaction-dialog.component.html',
  styleUrls: ['./app-transaction-dialog.component.scss'],
})
export class AppTransactionDialogComponent implements OnInit {
  public form = this.formBuilder.group({
    destinationAddress: [
      '',
      Validators.compose([Validators.maxLength(50), Validators.required]),
    ],
    amount: '',
  });

  public isDeposit = true;
  public vbkAddress: string;
  public vbkBalance: string;
  public isTestnet = false;

  public submitInProgress = false;
  public showAlert = false;
  public tx: string = null;

  constructor(
    public dialogRef: MatDialogRef<AppTransactionDialogComponent>,
    private walletService: WalletService,
    private alertService: AlertService,
    private formBuilder: FormBuilder,

    @Inject(MAT_DIALOG_DATA)
    public data: {
      isDeposit: boolean;
      vbkAddress: string;
      vbkBalance: string;
      isTestnet: boolean;
    }
  ) {}

  ngOnInit(): void {
    this.isDeposit = this.data.isDeposit || null;
    this.vbkAddress = this.data.vbkAddress || null;
    this.vbkBalance = this.data.vbkBalance || null;
    this.isTestnet = this.data.isTestnet || false;

    if (!this.isDeposit) {
      this.form
        .get('amount')
        .setValidators([
          Validators.max(parseFloat(this.vbkBalance) || 0),
          Validators.min(0),
          Validators.required,
        ]);
    }
  }

  public setMaxAmount() {
    if (parseFloat(this.vbkBalance) > 0) {
      this.form.controls['amount'].patchValue(this.vbkBalance);
    }
  }

  public onCancel(): void {
    this.dialogRef.close();
  }

  public onWithdraw(): void {
    if (this.form.invalid) {
      this.alertService.addWarning(
        'Please check the form and fix the errors indicated'
      );
      return;
    }

    if (this.form.value.amount === 0) {
      this.alertService.addWarning('Withdraw amount can not be zero');
      return;
    }

    const sendData: WithdrawRequest = {
      destinationAddress: this.form.value.destinationAddress || null,
      amount:
        typeof this.form.value.amount === 'number'
          ? this.form.value.amount.toString()
          : this.form.value.amount,
    };

    this.submitInProgress = true;

    this.walletService
      .postWithdraw(sendData)
      .subscribe((res: WithdrawResponse) => {
        this.alertService.addSuccess('Operation successful');

        this.tx = res.ids[0];
        this.showAlert = true;

        setTimeout(() => {
          this.tx = null;
          this.showAlert = false;
        }, 15000);
      })
      .add(() => {
        this.submitInProgress = false;
      });
  }

  public getTxLink(): string {
    return `https://${
      this.isTestnet ? 'testnet.' : ''
    }explore.veriblock.org/tx/${this.tx}`;
  }

  public showSuccess(): void {
    this.alertService.addSuccess('Address copied to clipboard successfully');
  }

  public checkNumberFormat() {
    if (this.form.value?.amount) {
      if (String(this.form.value.amount).match(/[^0-9.,]/g)) {
        this.form.controls['amount'].patchValue(
          String(this.form.value.amount).replace(/[^0-9.,]/g, '')
        );
      }

      const decimalPlaces: string[] = String(this.form.value?.amount)?.split(
        '.'
      );

      if (decimalPlaces?.length > 1 && decimalPlaces[1].length > 7) {
        this.form.controls['amount'].patchValue(
          parseFloat(`${decimalPlaces[0]}.${decimalPlaces[1].slice(0, 7)}`)
        );
      }
    }

    if (parseFloat(this.form.value?.amount) > parseFloat(this.vbkBalance)) {
      this.form.controls['amount'].patchValue(this.vbkBalance);
    }
  }

  public disableNumberFormat(e: KeyboardEvent) {
    if (e.key === '-' || e.key === '+' || e.key === 'e') {
      e.preventDefault();
    }
  }
}

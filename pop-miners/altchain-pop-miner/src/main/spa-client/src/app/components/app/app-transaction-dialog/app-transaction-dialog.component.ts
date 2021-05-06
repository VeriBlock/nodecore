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
    amount: ['', Validators.compose([Validators.min(0), Validators.required])],
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
        .setValidators(Validators.max(parseFloat(this.vbkBalance) || 0));
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
}

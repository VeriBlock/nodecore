import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

const DefaultDurationMs = 4000;
const ErrorDurationMs = 8000;

@Injectable({
  providedIn: 'root',
})
export class AlertService {
  constructor(private snackBar: MatSnackBar) {}

  // Generic alert
  public addAlert(text: string, panelClass?: string, duration?: number): void {
    setTimeout(() => {
      this.snackBar.open(text, null, {
        duration: duration || DefaultDurationMs,
        panelClass,
      });
    }, 0);
  }

  public addInfo(text: string): void {
    this.addAlert(text, 'alert-info');
  }

  public addSuccess(text: string): void {
    this.addAlert(text, 'alert-success');
  }

  public addWarning(text: string): void {
    this.addAlert(text, 'alert-warning', ErrorDurationMs);
  }
}

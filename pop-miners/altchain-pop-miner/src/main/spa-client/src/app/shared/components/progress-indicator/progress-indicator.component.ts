import { Component, Input } from '@angular/core';

@Component({
  selector: 'vbk-progress-indicator',
  templateUrl: './progress-indicator.component.html',
  styleUrls: ['./progress-indicator.component.scss'],
})
export class ProgressIndicatorComponent {
  @Input()
  public showSpinner: boolean;

  constructor() {}
}

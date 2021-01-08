import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup } from '@angular/forms';

import { MinerService } from '@core/services/miner.service';

import { Operation, ConfiguredAltchain, MineRequest } from '@core/model';

@Component({
  selector: 'vbk-mine-dialog',
  templateUrl: './mine-dialog.component.html',
  styleUrls: [],
})
export class MineDialogComponent {
  formGroup: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<Operation>,
    private minerService: MinerService,
    @Inject(MAT_DIALOG_DATA) public configuredAltchains: ConfiguredAltchain[]
  ) {
    this.formGroup = new FormGroup({
      chainSymbol: new FormControl(configuredAltchains[0].key, []),
    });
  }

  mine(): void {
    const request = new MineRequest();
    request.chainSymbol = this.formGroup.get('chainSymbol').value;
    this.minerService.postMine(request).subscribe((response) => {
      console.info(
        'Mine request successful! Operation id: ' + response.operationId
      );
      this.dialogRef.close(response);
    });
  }
}

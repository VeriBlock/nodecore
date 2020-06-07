
import {Component, Inject, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {HttpEventType, HttpResponse} from '@angular/common/http';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {MineRequest} from "../../model/miner";
import {Operation} from "../../model/operation";
import {ApiService} from "../../service/api.service";
import {ConfiguredAltchain} from "../../model/configured-altchain";

@Component({
	selector: 'mine-dialog',
	templateUrl: './mine-dialog.component.html',
	styleUrls: [],
})
export class MineDialogComponent {

  formGroup: FormGroup

	constructor(
		public dialogRef: MatDialogRef<Operation>,
		private apiService: ApiService,
    @Inject(MAT_DIALOG_DATA) public configuredAltchains: ConfiguredAltchain[]
	) {
    this.formGroup = new FormGroup({
      chainSymbol: new FormControl(configuredAltchains[0].key, [])
    });
	}

	mine(): void {
		const request = new MineRequest();
		request.chainSymbol = this.formGroup.get('chainSymbol').value
		this.apiService.mine(request).subscribe(response => {
      console.info('Mine request successful! Operation id: ' + response.operationId);
      this.dialogRef.close(response);
		});
	}
}

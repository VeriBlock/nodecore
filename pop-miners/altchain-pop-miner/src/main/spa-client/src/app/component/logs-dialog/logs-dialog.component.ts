
import {Component, Inject, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {HttpEventType, HttpResponse} from '@angular/common/http';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {MineRequest} from "../../model/miner";
import {Operation} from "../../model/operation";
import {ApiService} from "../../service/api.service";
import {ConfiguredAltchain} from "../../model/configured-altchain";

@Component({
	selector: 'logs-dialog',
	templateUrl: './logs-dialog.component.html',
	styleUrls: [],
})
export class LogsDialogComponent {


	constructor(
		public dialogRef: MatDialogRef<Operation>,
		private apiService: ApiService,
    @Inject(MAT_DIALOG_DATA) public logs: string[]
	) {
	}


}
